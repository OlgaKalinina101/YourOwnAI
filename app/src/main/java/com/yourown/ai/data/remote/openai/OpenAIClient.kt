package com.yourown.ai.data.remote.openai

import android.util.Log
import com.google.gson.Gson
import com.yourown.ai.data.remote.deepseek.ChatCompletionChunk
import com.yourown.ai.data.remote.deepseek.ChatCompletionRequest
import com.yourown.ai.data.remote.deepseek.ChatCompletionResponse
import com.yourown.ai.data.remote.deepseek.ChatMessage
import com.yourown.ai.data.remote.deepseek.ModelsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader

/**
 * OpenAI API Client (compatible with Deepseek models)
 * https://platform.openai.com/docs/api-reference/chat
 */
class OpenAIClient(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "OpenAIClient"
        private const val BASE_URL = "https://api.openai.com/v1"
        private const val RESPONSES_URL = "https://api.openai.com/v1/responses"
        
        // Models that require max_completion_tokens instead of max_tokens
        private val NEW_API_MODELS = setOf(
            "gpt-5", "gpt-5-2", "gpt-5-1", "gpt-5-fast",
            "o1", "o1-preview", "o1-mini", "o3", "o3-mini"
        )
        
        // Reasoning models that don't support temperature/top_p
        private val REASONING_MODELS = setOf(
            "o1", "o1-preview", "o1-mini", "o3", "o3-mini"
        )
        
        private fun shouldUseMaxCompletionTokens(model: String): Boolean {
            return NEW_API_MODELS.any { model.startsWith(it) }
        }
        
        private fun isReasoningModel(model: String): Boolean {
            return REASONING_MODELS.any { model.startsWith(it) }
        }
    }
    
    /**
     * List available models
     */
    suspend fun listModels(apiKey: String): Result<ModelsResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/models")
                .header("Authorization", "Bearer $apiKey")
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext Result.failure(
                    Exception("Empty response body")
                )
                val modelsResponse = gson.fromJson(body, ModelsResponse::class.java)
                Result.success(modelsResponse)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing models", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create chat completion (non-streaming)
     */
    suspend fun chatCompletion(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float? = null,
        topP: Float? = null,
        maxTokens: Int? = null
    ): Result<ChatCompletionResponse> = withContext(Dispatchers.IO) {
        try {
            val useNewApi = shouldUseMaxCompletionTokens(model)
            val isReasoning = isReasoningModel(model)
            
            val requestBody = ChatCompletionRequest(
                model = model,
                messages = messages,
                // Reasoning models don't support temperature/top_p
                temperature = if (!isReasoning) temperature else null,
                top_p = if (!isReasoning) topP else null,
                max_tokens = if (!useNewApi) maxTokens else null,
                max_completion_tokens = if (useNewApi) maxTokens else null,
                stream = false
            )
            
            val json = gson.toJson(requestBody)
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext Result.failure(
                    Exception("Empty response body")
                )
                val completionResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)
                Result.success(completionResponse)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Result.failure(Exception("HTTP ${response.code}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in chat completion", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create chat completion with streaming
     */
    fun chatCompletionStream(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float? = null,
        topP: Float? = null,
        maxTokens: Int? = null
    ): Flow<String> = callbackFlow {
        try {
            val useNewApi = shouldUseMaxCompletionTokens(model)
            val isReasoning = isReasoningModel(model)
            
            val requestBody = ChatCompletionRequest(
                model = model,
                messages = messages,
                // Reasoning models don't support temperature/top_p
                temperature = if (!isReasoning) temperature else null,
                top_p = if (!isReasoning) topP else null,
                max_tokens = if (!useNewApi) maxTokens else null,
                max_completion_tokens = if (useNewApi) maxTokens else null,
                stream = true
            )
            
            val json = gson.toJson(requestBody)
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                close(Exception("HTTP ${response.code}: $errorBody"))
                return@callbackFlow
            }
            
            val reader = response.body?.byteStream()?.bufferedReader()
            if (reader == null) {
                close(Exception("Empty response body"))
                return@callbackFlow
            }
            
            reader.use { 
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    
                    if (currentLine.startsWith("data: ")) {
                        val data = currentLine.substring(6).trim()
                        
                        if (data == "[DONE]") {
                            break
                        }
                        
                        if (data.isNotEmpty()) {
                            try {
                                val chunk = gson.fromJson(data, ChatCompletionChunk::class.java)
                                val content = chunk.choices.firstOrNull()?.delta?.content
                                if (!content.isNullOrEmpty()) {
                                    trySend(content)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse chunk: $data", e)
                            }
                        }
                    }
                }
            }
            
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Error in streaming chat completion", e)
            close(e)
        }
        
        awaitClose()
    }.flowOn(Dispatchers.IO)
    
    /**
     * Create chat completion with streaming and multimodal support (images)
     */
    fun chatCompletionStreamMultimodal(
        apiKey: String,
        model: String,
        messages: List<MultimodalChatMessage>,
        temperature: Float? = null,
        topP: Float? = null,
        maxTokens: Int? = null
    ): Flow<String> = callbackFlow {
        try {
            val useNewApi = shouldUseMaxCompletionTokens(model)
            val isReasoning = isReasoningModel(model)
            
            val requestBody = MultimodalChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = if (!isReasoning) temperature else null,
                top_p = if (!isReasoning) topP else null,
                max_tokens = if (!useNewApi) maxTokens else null,
                max_completion_tokens = if (useNewApi) maxTokens else null,
                stream = true
            )
            
            val json = gson.toJson(requestBody)
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                close(Exception("HTTP ${response.code}: $errorBody"))
                return@callbackFlow
            }
            
            val reader = response.body?.byteStream()?.bufferedReader()
            if (reader == null) {
                close(Exception("Empty response body"))
                return@callbackFlow
            }
            
            reader.use { 
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    
                    if (currentLine.startsWith("data: ")) {
                        val data = currentLine.substring(6).trim()
                        
                        if (data == "[DONE]") {
                            break
                        }
                        
                        if (data.isNotEmpty()) {
                            try {
                                val chunk = gson.fromJson(data, ChatCompletionChunk::class.java)
                                val content = chunk.choices.firstOrNull()?.delta?.content
                                if (!content.isNullOrEmpty()) {
                                    trySend(content)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse chunk: $data", e)
                            }
                        }
                    }
                }
            }
            
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Error in streaming multimodal chat completion", e)
            close(e)
        }
        
        awaitClose()
    }.flowOn(Dispatchers.IO)
    
    /**
     * Create chat completion with streaming using Responses API (for web search and other tools)
     */
    fun responsesApiStream(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        tools: List<Tool>,
        temperature: Float? = null,
        topP: Float? = null,
        maxTokens: Int? = null
    ): Flow<String> = callbackFlow {
        try {
            val isReasoning = isReasoningModel(model)
            
            val requestBody = ResponsesApiRequest(
                model = model,
                input = messages, // Responses API uses 'input' instead of 'messages'
                tools = tools,
                temperature = if (!isReasoning) temperature else null,
                top_p = if (!isReasoning) topP else null,
                max_output_tokens = maxTokens, // Responses API uses 'max_output_tokens'
                stream = true
            )
            
            val json = gson.toJson(requestBody)
            Log.d(TAG, "Responses API request: $json")
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(RESPONSES_URL)
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Responses API error: HTTP ${response.code}: $errorBody")
                close(Exception("HTTP ${response.code}: $errorBody"))
                return@callbackFlow
            }
            
            val reader = response.body?.byteStream()?.bufferedReader()
            if (reader == null) {
                close(Exception("Empty response body"))
                return@callbackFlow
            }
            
            reader.use { 
                var currentEvent: String? = null
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    
                    if (currentLine.startsWith("event: ")) {
                        currentEvent = currentLine.substring(7).trim()
                    } else if (currentLine.startsWith("data: ")) {
                        val data = currentLine.substring(6).trim()
                        
                        if (data == "[DONE]") {
                            break
                        }
                        
                        if (data.isNotEmpty() && currentEvent != null) {
                            try {
                                when (currentEvent) {
                                    "response.output_text.delta" -> {
                                        val event = gson.fromJson(data, ResponseStreamEvent::class.java)
                                        event.delta?.let { 
                                            trySend(it)
                                        }
                                    }
                                    "web_search_call.in_progress" -> {
                                        Log.d(TAG, "Web search in progress")
                                    }
                                    "web_search_call.searching" -> {
                                        Log.d(TAG, "Web search searching...")
                                    }
                                    "web_search_call.completed" -> {
                                        Log.d(TAG, "Web search completed: $data")
                                    }
                                    "response.completed" -> {
                                        Log.d(TAG, "Response completed")
                                        break
                                    }
                                    "response.failed" -> {
                                        Log.e(TAG, "Response failed: $data")
                                        close(Exception("Response failed: $data"))
                                        return@use
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse Responses API event: $currentEvent, data: $data", e)
                            }
                        }
                        
                        currentEvent = null
                    }
                }
            }
            
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Error in Responses API streaming", e)
            close(e)
        }
        
        awaitClose()
    }.flowOn(Dispatchers.IO)
    
    /**
     * Create chat completion with streaming using Responses API with multimodal support
     */
    fun responsesApiStreamMultimodal(
        apiKey: String,
        model: String,
        messages: List<MultimodalChatMessage>,
        tools: List<Tool>,
        temperature: Float? = null,
        topP: Float? = null,
        maxTokens: Int? = null
    ): Flow<String> = callbackFlow {
        try {
            val isReasoning = isReasoningModel(model)
            
            val requestBody = ResponsesApiMultimodalRequest(
                model = model,
                input = messages, // Responses API uses 'input' instead of 'messages'
                tools = tools,
                temperature = if (!isReasoning) temperature else null,
                top_p = if (!isReasoning) topP else null,
                max_output_tokens = maxTokens, // Responses API uses 'max_output_tokens'
                stream = true
            )
            
            val json = gson.toJson(requestBody)
            Log.d(TAG, "Responses API multimodal request: $json")
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(RESPONSES_URL)
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Responses API multimodal error: HTTP ${response.code}: $errorBody")
                close(Exception("HTTP ${response.code}: $errorBody"))
                return@callbackFlow
            }
            
            val reader = response.body?.byteStream()?.bufferedReader()
            if (reader == null) {
                close(Exception("Empty response body"))
                return@callbackFlow
            }
            
            reader.use { 
                var currentEvent: String? = null
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    
                    if (currentLine.startsWith("event: ")) {
                        currentEvent = currentLine.substring(7).trim()
                    } else if (currentLine.startsWith("data: ")) {
                        val data = currentLine.substring(6).trim()
                        
                        if (data == "[DONE]") {
                            break
                        }
                        
                        if (data.isNotEmpty() && currentEvent != null) {
                            try {
                                when (currentEvent) {
                                    "response.output_text.delta" -> {
                                        val event = gson.fromJson(data, ResponseStreamEvent::class.java)
                                        event.delta?.let { 
                                            trySend(it)
                                        }
                                    }
                                    "web_search_call.in_progress" -> {
                                        Log.d(TAG, "Web search in progress")
                                    }
                                    "web_search_call.searching" -> {
                                        Log.d(TAG, "Web search searching...")
                                    }
                                    "web_search_call.completed" -> {
                                        Log.d(TAG, "Web search completed: $data")
                                    }
                                    "response.completed" -> {
                                        Log.d(TAG, "Response completed")
                                        break
                                    }
                                    "response.failed" -> {
                                        Log.e(TAG, "Response failed: $data")
                                        close(Exception("Response failed: $data"))
                                        return@use
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse Responses API event: $currentEvent, data: $data", e)
                            }
                        }
                        
                        currentEvent = null
                    }
                }
            }
            
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Error in Responses API multimodal streaming", e)
            close(e)
        }
        
        awaitClose()
    }.flowOn(Dispatchers.IO)
    
    /**
     * Create embeddings for text(s)
     * https://platform.openai.com/docs/api-reference/embeddings
     */
    suspend fun createEmbeddings(
        apiKey: String,
        input: Any, // String or List<String>
        model: String = "text-embedding-3-small"
    ): Result<EmbeddingResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = EmbeddingRequest(
                input = input,
                model = model,
                encodingFormat = "float"
            )
            
            val json = gson.toJson(requestBody)
            Log.d(TAG, "Embedding request: model=$model, input type=${input::class.simpleName}")
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL/embeddings")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext Result.failure(
                    Exception("Empty response body")
                )
                val embeddingResponse = gson.fromJson(responseBody, EmbeddingResponse::class.java)
                Result.success(embeddingResponse)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Embedding error: HTTP ${response.code}: $errorBody")
                Result.failure(Exception("HTTP ${response.code}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating embeddings", e)
            Result.failure(e)
        }
    }
}
