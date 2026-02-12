package com.yourown.ai.data.service

import android.content.Context
import android.util.Log
import com.yourown.ai.data.llama.EmbeddingWrapper
import com.yourown.ai.data.repository.LocalEmbeddingModelRepository
import com.yourown.ai.domain.model.DownloadStatus
import com.yourown.ai.domain.model.LocalEmbeddingModel
import com.yourown.ai.domain.service.EmbeddingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of EmbeddingService using llama.cpp and API embeddings
 * Thread-safe singleton for embedding model operations
 * Supports both local models and API providers (OpenAI, OpenRouter)
 */
@Singleton
class EmbeddingServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val embeddingModelRepository: LocalEmbeddingModelRepository,
    private val aiConfigRepository: com.yourown.ai.data.repository.AIConfigRepository,
    private val apiKeyRepository: com.yourown.ai.data.repository.ApiKeyRepository,
    private val openAIClient: com.yourown.ai.data.remote.openai.OpenAIClient,
    private val openRouterClient: com.yourown.ai.data.remote.openrouter.OpenRouterClient
) : EmbeddingService {
    
    companion object {
        private const val TAG = "EmbeddingService"
    }
    
    private val embeddingWrapper = EmbeddingWrapper(context)
    private var currentModel: LocalEmbeddingModel? = null
    
    // Mutex to prevent concurrent operations (llama.cpp is NOT thread-safe!)
    private val loadMutex = Mutex()
    private val embeddingMutex = Mutex()
    
    /**
     * Automatically selects and loads the best available embedding model
     * Priority: MXBAI_EMBED_LARGE > ALL_MINILM_L6_V2
     * Returns Result.success if a model was loaded, Result.failure if no models available
     */
    private suspend fun autoLoadBestModel(): Result<Unit> {
        val models = embeddingModelRepository.models.value
        
        // Priority order: larger, better model first
        val preferredOrder = listOf(
            LocalEmbeddingModel.MXBAI_EMBED_LARGE,
            LocalEmbeddingModel.ALL_MINILM_L6_V2
        )
        
        for (model in preferredOrder) {
            val modelInfo = models[model]
            if (modelInfo?.status is DownloadStatus.Downloaded) {
                Log.i(TAG, "Auto-loading best available model: ${model.displayName}")
                return loadModel(model)
            }
        }
        
        Log.e(TAG, "No downloaded embedding models found")
        return Result.failure(IllegalStateException("No embedding models downloaded"))
    }
    
    override suspend fun loadModel(model: LocalEmbeddingModel): Result<Unit> = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            try {
                Log.d(TAG, "Attempting to load embedding model: ${model.displayName}")
                
                // If already loaded, skip
                if (currentModel == model && embeddingWrapper.isLoaded()) {
                    Log.i(TAG, "Embedding model ${model.displayName} already loaded")
                    return@withContext Result.success(Unit)
                }
                
                // Check if model is downloaded
                val models = embeddingModelRepository.models.value
                val modelInfo = models[model]
                
                val modelFile = getModelFile(model)
                Log.d(TAG, "Model file path: ${modelFile.absolutePath}")
                Log.d(TAG, "File exists: ${modelFile.exists()}")
                
                if (!modelFile.exists()) {
                    Log.e(TAG, "Embedding model file not found")
                    return@withContext Result.failure(
                        IllegalStateException("Model file not found. Please download the model first.")
                    )
                }
                
                if (modelInfo?.status !is DownloadStatus.Downloaded) {
                    Log.e(TAG, "Embedding model not downloaded: ${modelInfo?.status}")
                    return@withContext Result.failure(
                        IllegalStateException("Model ${model.displayName} is not downloaded")
                    )
                }
                
                // Unload previous model
                if (embeddingWrapper.isLoaded()) {
                    Log.i(TAG, "Unloading previous embedding model")
                    embeddingWrapper.unload()
                }
                
                Log.i(TAG, "Loading embedding model: ${model.displayName}")
                
                // Load model
                val success = embeddingWrapper.load(modelFile, model.dimensions)
                
                if (success) {
                    currentModel = model
                    Log.i(TAG, "Embedding model loaded successfully: ${model.displayName}")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to load embedding model"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading embedding model", e)
                Result.failure(e)
            }
        }
    }
    
    override suspend fun unloadModel(): Unit = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            try {
                Log.i(TAG, "Unloading embedding model")
                embeddingWrapper.unload()
                currentModel = null
            } catch (e: Exception) {
                Log.e(TAG, "Error unloading embedding model", e)
            }
        }
    }
    
    override fun isModelLoaded(): Boolean {
        return embeddingWrapper.isLoaded()
    }
    
    override fun getCurrentModel(): LocalEmbeddingModel? {
        return currentModel
    }
    
    override suspend fun generateEmbedding(text: String): Result<FloatArray> = withContext(Dispatchers.IO) {
        try {
            // Check if we should use API embeddings
            val config = aiConfigRepository.getAIConfig()
            
            if (config.useApiEmbeddings) {
                Log.d(TAG, "Using API embeddings: ${config.apiEmbeddingsProvider}/${config.apiEmbeddingsModel}")
                return@withContext generateApiEmbedding(
                    text = text,
                    provider = config.apiEmbeddingsProvider,
                    model = config.apiEmbeddingsModel
                )
            }
            
            // Use local model
            embeddingMutex.withLock {
                // Auto-load best available model if none loaded
                if (!embeddingWrapper.isLoaded()) {
                    val autoLoadResult = autoLoadBestModel()
                    if (autoLoadResult.isFailure) {
                        return@withContext Result.failure(
                            IllegalStateException("No embedding model available. Please download an embedding model first.")
                        )
                    }
                }
                
                if (text.isBlank()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Text cannot be blank")
                    )
                }
                
                Log.d(TAG, "Generating local embedding for text (${text.length} chars)")
                val embedding = embeddingWrapper.generateEmbedding(text)
                
                Result.success(embedding)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating embedding", e)
            Result.failure(e)
        }
    }
    
    override suspend fun generateEmbeddings(texts: List<String>): Result<List<FloatArray>> = withContext(Dispatchers.IO) {
        try {
            if (texts.isEmpty()) {
                return@withContext Result.success(emptyList())
            }
            
            // Check if we should use API embeddings
            val config = aiConfigRepository.getAIConfig()
            
            if (config.useApiEmbeddings) {
                Log.d(TAG, "Using API embeddings for ${texts.size} texts: ${config.apiEmbeddingsProvider}/${config.apiEmbeddingsModel}")
                return@withContext generateApiEmbeddings(
                    texts = texts,
                    provider = config.apiEmbeddingsProvider,
                    model = config.apiEmbeddingsModel
                )
            }
            
            // Use local model
            embeddingMutex.withLock {
                // Auto-load best available model if none loaded
                if (!embeddingWrapper.isLoaded()) {
                    val autoLoadResult = autoLoadBestModel()
                    if (autoLoadResult.isFailure) {
                        return@withContext Result.failure(
                            IllegalStateException("No embedding model available. Please download an embedding model first.")
                        )
                    }
                }
                
                Log.d(TAG, "Generating local embeddings for ${texts.size} texts")
                
                val embeddings = texts.map { text ->
                    if (text.isBlank()) {
                        FloatArray(currentModel?.dimensions ?: 0) { 0f }
                    } else {
                        embeddingWrapper.generateEmbedding(text)
                    }
                }
                
                Result.success(embeddings)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating embeddings", e)
            Result.failure(e)
        }
    }
    
    override fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        return embeddingWrapper.cosineSimilarity(embedding1, embedding2)
    }
    
    /**
     * Generate embedding using API (OpenAI or OpenRouter)
     */
    override suspend fun generateApiEmbedding(
        text: String,
        provider: String,
        model: String
    ): Result<FloatArray> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Text cannot be blank"))
            }
            
            // Get API key for the provider
            val apiKey = when (provider) {
                "openai" -> apiKeyRepository.getApiKey(com.yourown.ai.domain.model.AIProvider.OPENAI)
                "openrouter" -> apiKeyRepository.getApiKey(com.yourown.ai.domain.model.AIProvider.OPENROUTER)
                else -> null
            }
            
            if (apiKey.isNullOrBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("API key not found for provider: $provider")
                )
            }
            
            // Call appropriate API
            val result = when (provider) {
                "openai" -> {
                    openAIClient.createEmbeddings(
                        apiKey = apiKey,
                        input = text,
                        model = model
                    ).map { response ->
                        response.data.firstOrNull()?.embedding?.toFloatArray()
                            ?: throw Exception("No embedding returned")
                    }
                }
                "openrouter" -> {
                    openRouterClient.createEmbeddings(
                        apiKey = apiKey,
                        input = text,
                        model = model
                    ).map { response ->
                        response.data.firstOrNull()?.embedding?.toFloatArray()
                            ?: throw Exception("No embedding returned")
                    }
                }
                else -> Result.failure(IllegalArgumentException("Unknown provider: $provider"))
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error generating API embedding", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate embeddings using API for batch processing
     */
    override suspend fun generateApiEmbeddings(
        texts: List<String>,
        provider: String,
        model: String
    ): Result<List<FloatArray>> = withContext(Dispatchers.IO) {
        try {
            if (texts.isEmpty()) {
                return@withContext Result.success(emptyList())
            }
            
            // Filter out blank texts and remember their indices
            val nonBlankTexts = texts.filter { it.isNotBlank() }
            if (nonBlankTexts.isEmpty()) {
                return@withContext Result.success(List(texts.size) { FloatArray(0) })
            }
            
            // Get API key for the provider
            val apiKey = when (provider) {
                "openai" -> apiKeyRepository.getApiKey(com.yourown.ai.domain.model.AIProvider.OPENAI)
                "openrouter" -> apiKeyRepository.getApiKey(com.yourown.ai.domain.model.AIProvider.OPENROUTER)
                else -> null
            }
            
            if (apiKey.isNullOrBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("API key not found for provider: $provider")
                )
            }
            
            // Call appropriate API with batch
            val result = when (provider) {
                "openai" -> {
                    openAIClient.createEmbeddings(
                        apiKey = apiKey,
                        input = nonBlankTexts,
                        model = model
                    ).map { response ->
                        response.data.sortedBy { it.index }.map { it.embedding.toFloatArray() }
                    }
                }
                "openrouter" -> {
                    openRouterClient.createEmbeddings(
                        apiKey = apiKey,
                        input = nonBlankTexts,
                        model = model
                    ).map { response ->
                        response.data.sortedBy { it.index }.map { it.embedding.toFloatArray() }
                    }
                }
                else -> Result.failure(IllegalArgumentException("Unknown provider: $provider"))
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error generating API embeddings batch", e)
            Result.failure(e)
        }
    }
    
    private fun getModelFile(model: LocalEmbeddingModel): File {
        return embeddingModelRepository.getModelFile(model)
    }
}
