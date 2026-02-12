package com.yourown.ai.data.remote.openrouter

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OpenRouter API Client
 * OpenRouter provides unified access to 200+ AI models
 * https://openrouter.ai/docs/api-reference
 */
class OpenRouterClient(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "OpenRouterClient"
        private const val BASE_URL = "https://openrouter.ai/api/v1"
    }
    
    /**
     * Create chat completion with streaming
     * OpenRouter uses OpenAI-compatible streaming format
     */
    fun chatCompletionStream(
        apiKey: String,
        model: String,
        messages: List<OpenRouterMessage>,
        temperature: Float? = null,
        topP: Float? = null,
        maxTokens: Int? = null
    ): Flow<String> = callbackFlow {
        try {
            val requestBody = OpenRouterRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                top_p = topP,
                max_tokens = maxTokens,
                stream = true
            )
            
            val json = gson.toJson(requestBody)
            Log.d(TAG, "OpenRouter request: $json")
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://github.com/yourown/ai") // Optional: for better rate limits
                .header("X-Title", "YourOwnAI") // Optional: shows in OpenRouter dashboard
                .post(body)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "OpenRouter error: HTTP ${response.code}: $errorBody")
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
                            Log.d(TAG, "Stream completed")
                            break
                        }
                        
                        if (data.isNotEmpty()) {
                            try {
                                val chunk = gson.fromJson(data, OpenRouterChunk::class.java)
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
     * Create chat completion with streaming and multimodal support (images and files)
     * OpenRouter supports OpenAI-compatible multimodal format
     */
    fun chatCompletionStreamMultimodal(
        apiKey: String,
        model: String,
        messages: List<com.yourown.ai.data.remote.openai.MultimodalChatMessage>,
        temperature: Float? = null,
        topP: Float? = null,
        maxTokens: Int? = null
    ): Flow<String> = callbackFlow {
        try {
            val requestBody = com.yourown.ai.data.remote.openai.MultimodalChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                top_p = topP,
                max_tokens = maxTokens,
                stream = true
            )
            
            val json = gson.toJson(requestBody)
            Log.d(TAG, "OpenRouter multimodal request for model: $model")
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://github.com/yourown/ai")
                .header("X-Title", "YourOwnAI")
                .post(body)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "OpenRouter error: HTTP ${response.code}: $errorBody")
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
                            Log.d(TAG, "Stream completed")
                            break
                        }
                        
                        if (data.isNotEmpty()) {
                            try {
                                val chunk = gson.fromJson(data, OpenRouterChunk::class.java)
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
     * Create embeddings for text(s) via OpenRouter
     * https://openrouter.ai/docs/api-reference#embeddings
     */
    suspend fun createEmbeddings(
        apiKey: String,
        input: Any, // String or List<String>
        model: String
    ): Result<OpenRouterEmbeddingResponse> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val requestBody = OpenRouterEmbeddingRequest(
                input = input,
                model = model
            )
            
            val json = gson.toJson(requestBody)
            Log.d(TAG, "OpenRouter embedding request: model=$model, input type=${input::class.simpleName}")
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL/embeddings")
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://github.com/yourown/ai")
                .header("X-Title", "YourOwnAI")
                .post(body)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext Result.failure(
                    Exception("Empty response body")
                )
                val embeddingResponse = gson.fromJson(responseBody, OpenRouterEmbeddingResponse::class.java)
                Result.success(embeddingResponse)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "OpenRouter embedding error: HTTP ${response.code}: $errorBody")
                Result.failure(Exception("HTTP ${response.code}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating OpenRouter embeddings", e)
            Result.failure(e)
        }
    }
}
