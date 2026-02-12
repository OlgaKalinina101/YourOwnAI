package com.yourown.ai.domain.service

import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.domain.model.Message
import com.yourown.ai.domain.model.ModelProvider
import kotlinx.coroutines.flow.Flow

/**
 * Unified AI Service interface for both local and API-based models
 */
interface AIService {
    
    /**
     * Check if a model is available (downloaded for local, API key set for API)
     */
    suspend fun isModelAvailable(provider: ModelProvider): Boolean
    
    /**
     * Generate response from AI model
     */
    fun generateResponse(
        provider: ModelProvider,
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig,
        webSearchEnabled: Boolean = false,
        xSearchEnabled: Boolean = false
    ): Flow<String>
    
    /**
     * Stop ongoing generation
     */
    suspend fun stopGeneration()
}
