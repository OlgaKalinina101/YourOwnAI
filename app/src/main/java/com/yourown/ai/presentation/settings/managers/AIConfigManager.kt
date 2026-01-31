package com.yourown.ai.presentation.settings.managers

import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.data.repository.AIConfigRepository
import javax.inject.Inject

/**
 * Manager for AI Configuration settings
 * Handles: Temperature, Top-p, Deep Empathy, Memory, RAG, Message History, Max Tokens
 */
class AIConfigManager @Inject constructor(
    private val aiConfigRepository: AIConfigRepository
) {
    
    /**
     * Update temperature setting
     */
    suspend fun updateTemperature(config: AIConfig, value: Float): AIConfig {
        aiConfigRepository.updateTemperature(value)
        return config.copy(temperature = value)
    }
    
    /**
     * Update top-p setting
     */
    suspend fun updateTopP(config: AIConfig, value: Float): AIConfig {
        aiConfigRepository.updateTopP(value)
        return config.copy(topP = value)
    }
    
    /**
     * Toggle Deep Empathy feature
     */
    suspend fun toggleDeepEmpathy(config: AIConfig): AIConfig {
        val newValue = !config.deepEmpathy
        aiConfigRepository.setDeepEmpathy(newValue)
        return config.copy(deepEmpathy = newValue)
    }
    
    /**
     * Toggle Memory feature
     */
    suspend fun toggleMemory(config: AIConfig): AIConfig {
        val newValue = !config.memoryEnabled
        aiConfigRepository.setMemoryEnabled(newValue)
        return config.copy(memoryEnabled = newValue)
    }
    
    /**
     * Toggle RAG feature
     */
    suspend fun toggleRAG(config: AIConfig): AIConfig {
        val newValue = !config.ragEnabled
        aiConfigRepository.setRAGEnabled(newValue)
        return config.copy(ragEnabled = newValue)
    }
    
    /**
     * Update message history limit (API models only)
     */
    suspend fun updateMessageHistoryLimit(config: AIConfig, limit: Int): AIConfig {
        aiConfigRepository.updateMessageHistoryLimit(limit)
        return config.copy(messageHistoryLimit = limit)
    }
    
    /**
     * Update max tokens for AI responses
     */
    suspend fun updateMaxTokens(config: AIConfig, tokens: Int): AIConfig {
        aiConfigRepository.updateMaxTokens(tokens)
        return config.copy(maxTokens = tokens)
    }
    
    /**
     * Update RAG chunk size
     */
    suspend fun updateRAGChunkSize(config: AIConfig, value: Int): AIConfig {
        aiConfigRepository.updateRAGChunkSize(value)
        return config.copy(ragChunkSize = value)
    }
    
    /**
     * Update RAG chunk overlap
     */
    suspend fun updateRAGChunkOverlap(config: AIConfig, value: Int): AIConfig {
        aiConfigRepository.updateRAGChunkOverlap(value)
        return config.copy(ragChunkOverlap = value)
    }
    
    /**
     * Update memory limit
     */
    suspend fun updateMemoryLimit(config: AIConfig, value: Int): AIConfig {
        aiConfigRepository.updateMemoryLimit(value)
        return config.copy(memoryLimit = value)
    }
    
    /**
     * Update memory minimum age days filter
     */
    suspend fun updateMemoryMinAgeDays(config: AIConfig, value: Int): AIConfig {
        aiConfigRepository.updateMemoryMinAgeDays(value)
        return config.copy(memoryMinAgeDays = value)
    }
    
    /**
     * Update RAG chunk limit
     */
    suspend fun updateRAGChunkLimit(config: AIConfig, value: Int): AIConfig {
        aiConfigRepository.updateRAGChunkLimit(value)
        return config.copy(ragChunkLimit = value)
    }
    
    /**
     * Update context instructions
     */
    suspend fun updateContextInstructions(config: AIConfig, instructions: String): AIConfig {
        aiConfigRepository.updateContextInstructions(instructions)
        return config.copy(contextInstructions = instructions)
    }
    
    /**
     * Reset context instructions to default
     */
    suspend fun resetContextInstructions(config: AIConfig): AIConfig {
        aiConfigRepository.resetContextInstructions()
        return config.copy(contextInstructions = AIConfig.DEFAULT_CONTEXT_INSTRUCTIONS)
    }
    
    /**
     * Update swipe message prompt
     */
    suspend fun updateSwipeMessagePrompt(config: AIConfig, prompt: String): AIConfig {
        aiConfigRepository.updateSwipeMessagePrompt(prompt)
        return config.copy(swipeMessagePrompt = prompt)
    }
    
    /**
     * Reset swipe message prompt to default
     */
    suspend fun resetSwipeMessagePrompt(config: AIConfig): AIConfig {
        aiConfigRepository.resetSwipeMessagePrompt()
        return config.copy(swipeMessagePrompt = AIConfig.DEFAULT_SWIPE_MESSAGE_PROMPT)
    }
    
    /**
     * Update memory title
     */
    suspend fun updateMemoryTitle(config: AIConfig, title: String): AIConfig {
        aiConfigRepository.updateMemoryTitle(title)
        return config.copy(memoryTitle = title)
    }
    
    /**
     * Update memory instructions
     */
    suspend fun updateMemoryInstructions(config: AIConfig, instructions: String): AIConfig {
        aiConfigRepository.updateMemoryInstructions(instructions)
        return config.copy(memoryInstructions = instructions)
    }
    
    /**
     * Reset memory instructions to default
     */
    suspend fun resetMemoryInstructions(config: AIConfig): AIConfig {
        aiConfigRepository.resetMemoryInstructions()
        return config.copy(memoryInstructions = AIConfig.DEFAULT_MEMORY_INSTRUCTIONS)
    }
    
    /**
     * Update RAG title
     */
    suspend fun updateRAGTitle(config: AIConfig, title: String): AIConfig {
        aiConfigRepository.updateRAGTitle(title)
        return config.copy(ragTitle = title)
    }
    
    /**
     * Update RAG instructions
     */
    suspend fun updateRAGInstructions(config: AIConfig, instructions: String): AIConfig {
        aiConfigRepository.updateRAGInstructions(instructions)
        return config.copy(ragInstructions = instructions)
    }
    
    /**
     * Reset RAG instructions to default
     */
    suspend fun resetRAGInstructions(config: AIConfig): AIConfig {
        aiConfigRepository.resetRAGInstructions()
        return config.copy(ragInstructions = AIConfig.DEFAULT_RAG_INSTRUCTIONS)
    }
    
    /**
     * Update memory extraction prompt
     */
    suspend fun updateMemoryExtractionPrompt(config: AIConfig, prompt: String): AIConfig {
        aiConfigRepository.updateMemoryExtractionPrompt(prompt)
        return config.copy(memoryExtractionPrompt = prompt)
    }
    
    /**
     * Reset memory extraction prompt to default
     */
    suspend fun resetMemoryExtractionPrompt(config: AIConfig): AIConfig {
        aiConfigRepository.resetMemoryExtractionPrompt()
        return config.copy(memoryExtractionPrompt = AIConfig.DEFAULT_MEMORY_EXTRACTION_PROMPT)
    }
    
    /**
     * Update deep empathy prompt
     */
    suspend fun updateDeepEmpathyPrompt(config: AIConfig, prompt: String): AIConfig {
        aiConfigRepository.updateDeepEmpathyPrompt(prompt)
        return config.copy(deepEmpathyPrompt = prompt)
    }
    
    /**
     * Reset deep empathy prompt to default
     */
    suspend fun resetDeepEmpathyPrompt(config: AIConfig): AIConfig {
        aiConfigRepository.resetDeepEmpathyPrompt()
        return config.copy(deepEmpathyPrompt = AIConfig.DEFAULT_DEEP_EMPATHY_PROMPT)
    }
    
    /**
     * Update deep empathy analysis prompt
     */
    suspend fun updateDeepEmpathyAnalysisPrompt(config: AIConfig, prompt: String): AIConfig {
        aiConfigRepository.updateDeepEmpathyAnalysisPrompt(prompt)
        return config.copy(deepEmpathyAnalysisPrompt = prompt)
    }
    
    /**
     * Reset deep empathy analysis prompt to default
     */
    suspend fun resetDeepEmpathyAnalysisPrompt(config: AIConfig): AIConfig {
        aiConfigRepository.resetDeepEmpathyAnalysisPrompt()
        return config.copy(deepEmpathyAnalysisPrompt = AIConfig.DEFAULT_DEEP_EMPATHY_ANALYSIS_PROMPT)
    }
    
    /**
     * Update user context
     */
    suspend fun updateContext(config: AIConfig, context: String): AIConfig {
        aiConfigRepository.updateUserContext(context)
        return config
    }
}
