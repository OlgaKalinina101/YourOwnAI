package com.yourown.ai.data.service

import android.content.Context
import android.util.Log
import com.yourown.ai.data.llama.LlamaCppWrapper
import com.yourown.ai.data.repository.LocalModelRepository
import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.domain.model.DownloadStatus
import com.yourown.ai.domain.model.LocalModel
import com.yourown.ai.domain.model.Message
import com.yourown.ai.domain.model.MessageRole
import com.yourown.ai.domain.service.LlamaService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlamaServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localModelRepository: LocalModelRepository
) : LlamaService {
    
    companion object {
        private const val TAG = "LlamaService"
        private const val CONTEXT_SIZE = 2048
    }
    
    private val llamaWrapper = LlamaCppWrapper(context)
    private var currentModel: LocalModel? = null
    private var isGenerating = false
    
    // Mutex to prevent concurrent model loading (Llamatik is NOT thread-safe!)
    private val loadMutex = Mutex()
    private val generateMutex = Mutex()
    
    override suspend fun loadModel(model: LocalModel): Result<Unit> = withContext(Dispatchers.IO) {
        // Use mutex to prevent concurrent loading (Llamatik is NOT thread-safe!)
        loadMutex.withLock {
            try {
                Log.d(TAG, "Attempting to load model: ${model.displayName}")
                
                // If already loaded, skip
                if (currentModel == model && llamaWrapper.isLoaded()) {
                    Log.i(TAG, "Model ${model.displayName} already loaded, skipping")
                    return@withContext Result.success(Unit)
                }
                
                // Check if model is downloaded
                val models = localModelRepository.models.value
                val modelInfo = models[model]
                
                Log.d(TAG, "Model status: ${modelInfo?.status}")
                
                // Get model file
                val modelFile = getModelFile(model)
                Log.d(TAG, "Model file path: ${modelFile.absolutePath}")
                Log.d(TAG, "File exists: ${modelFile.exists()}, size: ${if (modelFile.exists()) modelFile.length() else 0}")
                
                // Check file exists FIRST (more reliable than status)
                if (!modelFile.exists()) {
                    Log.e(TAG, "Model file not found: ${modelFile.absolutePath}")
                    return@withContext Result.failure(
                        IllegalStateException("Model file not found. Please download the model first.")
                    )
                }
                
                // Check status
                if (modelInfo?.status !is DownloadStatus.Downloaded) {
                    Log.e(TAG, "Model status is not Downloaded: ${modelInfo?.status}")
                    return@withContext Result.failure(
                        IllegalStateException("Model ${model.displayName} is not downloaded")
                    )
                }
                
                // Unload previous model
                if (llamaWrapper.isLoaded()) {
                    Log.i(TAG, "Unloading previous model")
                    llamaWrapper.unload()
                }
                
                Log.i(TAG, "Loading model: ${model.displayName} from ${modelFile.absolutePath}")
                
                // Load model
                val success = llamaWrapper.load(modelFile, CONTEXT_SIZE)
                
                if (success) {
                    currentModel = model
                    Log.i(TAG, "Model loaded successfully: ${model.displayName}")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to load model"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model", e)
                Result.failure(e)
            }
        }
    }
    
    override suspend fun unloadModel() {
        withContext(Dispatchers.IO) {
            llamaWrapper.unload()
            currentModel = null
            Log.i(TAG, "Model unloaded")
        }
    }
    
    override fun isModelLoaded(): Boolean = llamaWrapper.isLoaded()
    
    override fun getCurrentModel(): LocalModel? = currentModel
    
    override fun generateResponse(
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig
    ): Flow<String> = flow {
        // Use mutex to prevent concurrent generation (Llamatik is NOT thread-safe!)
        generateMutex.withLock {
            if (!llamaWrapper.isLoaded()) {
                throw IllegalStateException("No model loaded")
            }
            
            isGenerating = true
            
            try {
                // Build prompt
                val prompt = buildPrompt(messages, systemPrompt, userContext, config)
                
                Log.d(TAG, "Generating response with prompt length: ${prompt.length}")
                Log.d(TAG, "Temperature: ${config.temperature}, Top-P: ${config.topP}")
                
                // Use non-streaming generation to avoid UTF-8 issues in native code
                val fullResponse = withContext(Dispatchers.IO) {
                    llamaWrapper.generateText(prompt)
                }
                
                Log.d(TAG, "Full response received, length: ${fullResponse.length}")
                
                // Simulate streaming by emitting words with small delays
                // This gives a nice UX while avoiding the native UTF-8 bug
                val words = fullResponse.split(" ")
                for ((index, word) in words.withIndex()) {
                    if (!isGenerating) break
                    
                    val toEmit = if (index < words.size - 1) "$word " else word
                    emit(toEmit)
                    
                    // Small delay to simulate streaming (30-50ms per word)
                    delay(35)
                }
                
                Log.d(TAG, "Generation completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating response", e)
                throw e
            } finally {
                isGenerating = false
            }
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun stopGeneration() {
        isGenerating = false
        llamaWrapper.cancelGeneration()
        Log.i(TAG, "Generation stopped")
    }
    
    /**
     * Build prompt from conversation history
     */
    private fun buildPrompt(
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig
    ): String {
        val builder = StringBuilder()
        
        // Add system prompt
        builder.append("System: $systemPrompt\n\n")
        
        // Add user context if provided
        if (!userContext.isNullOrBlank()) {
            builder.append("Context: $userContext\n\n")
        }
        
        // Add conversation history (limited by messageHistoryLimit)
        val historyLimit = config.messageHistoryLimit * 2 // pairs = user + assistant
        val relevantMessages = messages.takeLast(historyLimit)
        
        for (message in relevantMessages) {
            val role = when (message.role) {
                MessageRole.USER -> "User"
                MessageRole.ASSISTANT -> "Assistant"
                MessageRole.SYSTEM -> "System"
            }
            builder.append("$role: ${message.content}\n\n")
        }
        
        // Add prompt for assistant response
        builder.append("Assistant:")
        
        return builder.toString()
    }
    
    /**
     * Get model file location
     */
    private fun getModelFile(model: LocalModel): File {
        val modelsDir = File(context.filesDir, "models")
        return File(modelsDir, model.modelName) // modelName already includes .gguf
    }
}
