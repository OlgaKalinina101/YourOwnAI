package com.yourown.ai.presentation.settings.managers

import com.yourown.ai.data.repository.ApiKeyRepository
import com.yourown.ai.data.remote.deepseek.DeepseekClient
import com.yourown.ai.data.remote.openai.OpenAIClient
import com.yourown.ai.data.remote.xai.XAIClient
import com.yourown.ai.domain.model.AIProvider
import javax.inject.Inject

/**
 * Manager for API Keys
 * Handles: Saving, deleting, and testing API keys for different providers
 */
class ApiKeyManager @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val deepseekClient: DeepseekClient,
    private val openAIClient: OpenAIClient,
    private val xaiClient: XAIClient
) {
    
    /**
     * Save API key for provider
     */
    suspend fun saveApiKey(provider: AIProvider, key: String) {
        apiKeyRepository.saveApiKey(provider, key)
    }
    
    /**
     * Delete API key for provider
     */
    suspend fun deleteApiKey(provider: AIProvider) {
        apiKeyRepository.deleteApiKey(provider)
    }
    
    /**
     * Test API key by listing models
     */
    suspend fun testApiKey(provider: AIProvider): Result<Unit> {
        val apiKey = apiKeyRepository.getApiKey(provider) ?: return Result.failure(Exception("No API key"))
        
        return when (provider) {
            AIProvider.DEEPSEEK -> {
                deepseekClient.listModels(apiKey).map { models ->
                    android.util.Log.i("ApiKeyManager", "Deepseek models: $models")
                }
            }
            AIProvider.OPENAI -> {
                openAIClient.listModels(apiKey).map { response ->
                    android.util.Log.i("ApiKeyManager", "OpenAI models: $response")
                }
            }
            AIProvider.XAI -> {
                xaiClient.listModels(apiKey).map { response ->
                    android.util.Log.i("ApiKeyManager", "x.ai models: $response")
                }
            }
            else -> {
                // TODO: Implement for OpenRouter
                Result.success(Unit)
            }
        }
    }
}
