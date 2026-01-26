package com.yourown.ai.data.remote.deepseek

import android.util.Log
import com.google.gson.Gson
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepseekClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "DeepseekClient"
        private const val BASE_URL = "https://api.deepseek.com"
    }
    
    /**
     * Get available models
     */
    suspend fun listModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/models")
                .header("Authorization", "Bearer $apiKey")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }
            
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val modelsResponse = gson.fromJson(body, ModelsResponse::class.java)
            
            Result.success(modelsResponse.data.map { it.id })
        } catch (e: Exception) {
            Log.e(TAG, "Error listing models", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate chat completion (non-streaming)
     */
    suspend fun chatCompletion(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
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
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val chatResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)
            
            Result.success(chatResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error in chat completion", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate chat completion with streaming
     */
    fun chatCompletionStream(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
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
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                close(Exception("HTTP ${response.code}: ${response.message}"))
                return@callbackFlow
            }
            
            // Read SSE stream
            response.body?.byteStream()?.bufferedReader()?.use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        
                        if (data == "[DONE]") {
                            close()
                            return@callbackFlow
                        }
                        
                        try {
                            val chunk = gson.fromJson(data, ChatCompletionChunk::class.java)
                            val content = chunk.choices.firstOrNull()?.delta?.content
                            if (!content.isNullOrEmpty()) {
                                trySend(content)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing chunk: $data", e)
                        }
                    }
                    line = reader.readLine()
                }
            }
            
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Error in streaming", e)
            close(e)
        }
        
        awaitClose()
    }.flowOn(Dispatchers.IO)
}
