package com.yourown.ai.domain.model

/**
 * Persona - профиль с настройками AI, документами и memory scope
 * Расширение концепции SystemPrompt до полноценного профиля
 */
data class Persona(
    val id: String,
    val name: String,
    val description: String = "",
    val systemPromptId: String,              // Ссылка на SystemPrompt
    val systemPrompt: String,                 // Кеш текста промпта (для удобства)
    val isForApi: Boolean = true,
    
    // ===== AI Configuration =====
    // Копируются из глобального AIConfig при создании, затем можно изменить
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 4096,
    val deepEmpathy: Boolean = false,
    val memoryEnabled: Boolean = false,
    val ragEnabled: Boolean = false,
    val messageHistoryLimit: Int = 10,
    
    // ===== Prompts =====
    val deepEmpathyPrompt: String = AIConfig.DEFAULT_DEEP_EMPATHY_PROMPT,
    val deepEmpathyAnalysisPrompt: String = AIConfig.DEFAULT_DEEP_EMPATHY_ANALYSIS_PROMPT,
    val memoryExtractionPrompt: String = AIConfig.DEFAULT_MEMORY_EXTRACTION_PROMPT,
    val contextInstructions: String = AIConfig.DEFAULT_CONTEXT_INSTRUCTIONS,
    val memoryInstructions: String = AIConfig.DEFAULT_MEMORY_INSTRUCTIONS,
    val ragInstructions: String = AIConfig.DEFAULT_RAG_INSTRUCTIONS,
    val swipeMessagePrompt: String = AIConfig.DEFAULT_SWIPE_MESSAGE_PROMPT,
    
    // ===== Memory Configuration =====
    val memoryLimit: Int = 5,
    val memoryMinAgeDays: Int = 2,
    val memoryTitle: String = "Твои воспоминания",
    
    // ===== RAG Configuration =====
    val ragChunkSize: Int = 512,
    val ragChunkOverlap: Int = 64,
    val ragChunkLimit: Int = 5,
    val ragTitle: String = "Твоя библиотека текстов",
    
    // ===== Model Preference =====
    val preferredModelId: String? = null,
    val preferredProvider: String? = null,
    
    // ===== Document Links =====
    val linkedDocumentIds: List<String> = emptyList(),
    
    // ===== Memory Scope =====
    // Если true - использовать только воспоминания этой persona
    val useOnlyPersonaMemories: Boolean = false,
    // Если false - не делиться воспоминаниями с другими persona
    val shareMemoriesGlobally: Boolean = true,
    
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        /**
         * Создать Persona из SystemPrompt и глобального AIConfig
         */
        fun fromSystemPrompt(
            id: String,
            systemPromptId: String,
            systemPromptName: String,
            systemPromptContent: String,
            description: String = "",
            config: AIConfig,
            isForApi: Boolean = true
        ): Persona {
            return Persona(
                id = id,
                name = systemPromptName,
                description = description,
                systemPromptId = systemPromptId,
                systemPrompt = systemPromptContent,
                isForApi = isForApi,
                temperature = config.temperature,
                topP = config.topP,
                maxTokens = config.maxTokens,
                deepEmpathy = config.deepEmpathy,
                memoryEnabled = config.memoryEnabled,
                ragEnabled = config.ragEnabled,
                messageHistoryLimit = config.messageHistoryLimit,
                deepEmpathyPrompt = config.deepEmpathyPrompt,
                deepEmpathyAnalysisPrompt = config.deepEmpathyAnalysisPrompt,
                memoryExtractionPrompt = config.memoryExtractionPrompt,
                contextInstructions = config.contextInstructions,
                memoryInstructions = config.memoryInstructions,
                ragInstructions = config.ragInstructions,
                swipeMessagePrompt = config.swipeMessagePrompt,
                memoryLimit = config.memoryLimit,
                memoryMinAgeDays = config.memoryMinAgeDays,
                memoryTitle = config.memoryTitle,
                ragChunkSize = config.ragChunkSize,
                ragChunkOverlap = config.ragChunkOverlap,
                ragChunkLimit = config.ragChunkLimit,
                ragTitle = config.ragTitle,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Конвертировать Persona в AIConfig для использования в чате
     */
    fun toAIConfig(): AIConfig {
        return AIConfig(
            systemPrompt = if (isForApi) systemPrompt else AIConfig.DEFAULT_SYSTEM_PROMPT,
            localSystemPrompt = if (!isForApi) systemPrompt else AIConfig.DEFAULT_LOCAL_SYSTEM_PROMPT,
            memoryExtractionPrompt = memoryExtractionPrompt,
            temperature = temperature,
            topP = topP,
            maxTokens = maxTokens,
            deepEmpathy = deepEmpathy,
            deepEmpathyPrompt = deepEmpathyPrompt,
            deepEmpathyAnalysisPrompt = deepEmpathyAnalysisPrompt,
            memoryEnabled = memoryEnabled,
            memoryLimit = memoryLimit,
            memoryMinAgeDays = memoryMinAgeDays,
            memoryTitle = memoryTitle,
            memoryInstructions = memoryInstructions,
            ragEnabled = ragEnabled,
            ragChunkSize = ragChunkSize,
            ragChunkOverlap = ragChunkOverlap,
            ragChunkLimit = ragChunkLimit,
            ragTitle = ragTitle,
            ragInstructions = ragInstructions,
            contextInstructions = contextInstructions,
            swipeMessagePrompt = swipeMessagePrompt,
            messageHistoryLimit = messageHistoryLimit
        )
    }
}
