package com.yourown.ai.data.remote.xai

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

/**
 * x.ai (Grok) API Client - OpenAI compatible
 * https://docs.x.ai/docs/api-reference
 */
class XAIClient(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "XAIClient"
        private const val BASE_URL = "https://api.x.ai/v1"
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
            val requestBody = ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                top_p = topP,
                max_tokens = maxTokens,
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
            val requestBody = ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                top_p = topP,
                max_tokens = maxTokens,
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
     * Create chat completion with streaming and multimodal support (images and files)
     * x.ai Grok supports images via same format as OpenAI
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
}
