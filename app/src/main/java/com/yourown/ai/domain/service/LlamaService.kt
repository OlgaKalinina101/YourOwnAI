package com.yourown.ai.domain.service

import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.domain.model.LocalModel
import com.yourown.ai.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Service for local AI model inference
 * Handles loading models, generating responses, and managing model state
 */
interface LlamaService {
    
    /**
     * Load a model from local storage
     */
    suspend fun loadModel(model: LocalModel): Result<Unit>
    
    /**
     * Unload current model
     */
    suspend fun unloadModel()
    
    /**
     * Check if a model is currently loaded
     */
    fun isModelLoaded(): Boolean
    
    /**
     * Get currently loaded model
     */
    fun getCurrentModel(): LocalModel?
    
    /**
     * Generate response from the model
     * @param messages Conversation history (limited by messageHistoryLimit)
     * @param systemPrompt System prompt
     * @param userContext Additional user context
     * @param config AI configuration (temperature, top-p, etc.)
     * @return Flow of generated text chunks
     */
    fun generateResponse(
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig
    ): Flow<String>
    
    /**
     * Stop current generation
     */
    suspend fun stopGeneration()
}
