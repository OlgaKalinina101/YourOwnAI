package com.yourown.ai.data.remote.deepseek

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Deepseek API service
 * Compatible with OpenAI API format
 */
interface DeepseekApiService {
    
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
    
    @POST("chat/completions")
    @Streaming
    suspend fun chatCompletionStream(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>
    
    @GET("models")
    suspend fun listModels(
        @Header("Authorization") authorization: String
    ): Response<ModelsResponse>
}
