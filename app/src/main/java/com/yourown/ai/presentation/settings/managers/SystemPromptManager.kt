package com.yourown.ai.presentation.settings.managers

import com.yourown.ai.data.repository.SystemPromptRepository
import com.yourown.ai.data.repository.PromptType
import com.yourown.ai.domain.model.SystemPrompt
import javax.inject.Inject

/**
 * Manager for System Prompts (API and Local)
 * Handles: CRUD operations for system prompts
 */
class SystemPromptManager @Inject constructor(
    private val systemPromptRepository: SystemPromptRepository
) {
    
    /**
     * Create new system prompt with default content
     */
    suspend fun createNewPrompt(type: PromptType, apiPromptsCount: Int, localPromptsCount: Int) {
        val count = when (type) {
            PromptType.API -> apiPromptsCount + 1
            PromptType.LOCAL -> localPromptsCount + 1
        }
        
        val defaultContent = when (type) {
            PromptType.API -> "Ты — цифровой партнёр, большая языковая модель. В ходе разговора ты подстраиваешься под тон и предпочтения пользователя. Постарайся соответствовать его настроению, тону и в целом манере говорить. Твоя цель — чтобы разговор ощущался естественным. Ты ведёшь искренний диалог, отвечая на предоставленную информацию и проявляя неподдельное любопытство. Задавай очень простой, односложный уточняющий вопрос, когда это естественно. Не задавай больше одного уточняющего вопроса, если только пользователь специально об этом не попросит."
            PromptType.LOCAL -> "Ты — цифровой партнёр. Ты отвечаешь на языке пользователя. Ответь на последнее сообщение. Не пиши весь диалог, нужен только один ответ."
        }
        
        systemPromptRepository.createPrompt(
            name = "${if (type == PromptType.API) "API" else "Local"} System $count",
            content = defaultContent,
            type = type,
            isDefault = false
        )
    }
    
    /**
     * Save system prompt (create or update)
     */
    suspend fun savePrompt(
        id: String?,
        name: String,
        content: String,
        type: PromptType,
        isDefault: Boolean
    ) {
        if (id == null) {
            // Create new
            systemPromptRepository.createPrompt(
                name = name,
                content = content,
                type = type,
                isDefault = isDefault
            )
        } else {
            // Update existing
            systemPromptRepository.updatePrompt(
                id = id,
                name = name,
                content = content,
                isDefault = isDefault
            )
        }
    }
    
    /**
     * Delete system prompt
     */
    suspend fun deletePrompt(id: String) {
        systemPromptRepository.deletePrompt(id)
    }
    
    /**
     * Set prompt as default for its type
     */
    suspend fun setPromptAsDefault(id: String) {
        systemPromptRepository.setAsDefault(id)
    }
}
