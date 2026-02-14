package com.yourown.ai.data.repository

import com.yourown.ai.data.local.dao.SystemPromptDao
import com.yourown.ai.data.local.entity.SystemPromptEntity
import com.yourown.ai.domain.model.SystemPrompt
import com.yourown.ai.domain.prompt.PromptKey
import com.yourown.ai.domain.prompt.PromptTranslationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemPromptRepository @Inject constructor(
    private val systemPromptDao: SystemPromptDao,
    private val promptTranslationManager: PromptTranslationManager,
    private val settingsManager: com.yourown.ai.data.local.preferences.SettingsManager
) {
    
    /**
     * Get all prompts as Flow
     */
    fun getAllPrompts(): Flow<List<SystemPrompt>> {
        return systemPromptDao.getAllPrompts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Get prompts by type (api/local)
     */
    fun getPromptsByType(type: PromptType): Flow<List<SystemPrompt>> {
        return systemPromptDao.getPromptsByType(type.value).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Get prompt by ID
     */
    suspend fun getPromptById(id: String): SystemPrompt? {
        return systemPromptDao.getPromptById(id)?.toDomainModel()
    }
    
    /**
     * Get default API prompt
     */
    suspend fun getDefaultApiPrompt(): SystemPrompt? {
        return systemPromptDao.getDefaultApiPrompt()?.toDomainModel()
    }
    
    /**
     * Get default Local prompt
     */
    suspend fun getDefaultLocalPrompt(): SystemPrompt? {
        return systemPromptDao.getDefaultLocalPrompt()?.toDomainModel()
    }
    
    /**
     * Create new prompt
     */
    suspend fun createPrompt(
        name: String,
        content: String,
        type: PromptType,
        isDefault: Boolean = false
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        
        // If setting as default, clear other defaults for this type
        if (isDefault) {
            systemPromptDao.clearDefaultsForType(type.value)
        }
        
        val entity = SystemPromptEntity(
            id = id,
            name = name,
            content = content,
            promptType = type.value,
            isDefault = isDefault,
            createdAt = now,
            updatedAt = now,
            usageCount = 0
        )
        
        systemPromptDao.insertPrompt(entity)
        return id
    }
    
    /**
     * Update prompt
     */
    suspend fun updatePrompt(
        id: String,
        name: String? = null,
        content: String? = null,
        isDefault: Boolean? = null
    ) {
        val existing = systemPromptDao.getPromptById(id) ?: return
        
        // If setting as default, clear other defaults for this type
        if (isDefault == true && !existing.isDefault) {
            systemPromptDao.clearDefaultsForType(existing.promptType)
        }
        
        val updated = existing.copy(
            name = name ?: existing.name,
            content = content ?: existing.content,
            isDefault = isDefault ?: existing.isDefault,
            updatedAt = System.currentTimeMillis()
        )
        
        systemPromptDao.updatePrompt(updated)
    }
    
    /**
     * Delete prompt
     */
    suspend fun deletePrompt(id: String) {
        val prompt = systemPromptDao.getPromptById(id) ?: return
        systemPromptDao.deletePrompt(prompt)
    }
    
    /**
     * Set prompt as default
     */
    suspend fun setAsDefault(id: String) {
        val prompt = systemPromptDao.getPromptById(id) ?: return
        systemPromptDao.clearDefaultsForType(prompt.promptType)
        systemPromptDao.setAsDefault(id)
    }
    
    /**
     * Increment usage count
     */
    suspend fun incrementUsageCount(id: String) {
        systemPromptDao.incrementUsageCount(id)
    }
    
    /**
     * Initialize default prompts if database is empty
     */
    suspend fun initializeDefaultPrompts() {
        // Check if there are any prompts
        val apiPrompt = systemPromptDao.getDefaultApiPrompt()
        val localPrompt = systemPromptDao.getDefaultLocalPrompt()
        
        // Get current prompt language
        val promptLanguage = settingsManager.promptLanguage.first()
        
        // Create default API prompt if none exists
        if (apiPrompt == null) {
            createPrompt(
                name = "Default API",
                content = promptTranslationManager.getPrompt(PromptKey.SYSTEM_PROMPT, promptLanguage),
                type = PromptType.API,
                isDefault = true
            )
        }
        
        // Create default Local prompt if none exists
        if (localPrompt == null) {
            createPrompt(
                name = "Default Local",
                content = promptTranslationManager.getPrompt(PromptKey.LOCAL_SYSTEM_PROMPT, promptLanguage),
                type = PromptType.LOCAL,
                isDefault = true
            )
        }
    }
    
    /**
     * Update default prompts with new language
     * This is called when user changes prompt language
     */
    suspend fun updateDefaultPromptsLanguage() {
        val apiPrompt = systemPromptDao.getDefaultApiPrompt()
        val localPrompt = systemPromptDao.getDefaultLocalPrompt()
        
        // Get current prompt language
        val promptLanguage = settingsManager.promptLanguage.first()
        
        // Update API prompt if it exists
        apiPrompt?.let {
            updatePrompt(
                id = it.id,
                content = promptTranslationManager.getPrompt(PromptKey.SYSTEM_PROMPT, promptLanguage)
            )
        }
        
        // Update Local prompt if it exists
        localPrompt?.let {
            updatePrompt(
                id = it.id,
                content = promptTranslationManager.getPrompt(PromptKey.LOCAL_SYSTEM_PROMPT, promptLanguage)
            )
        }
    }
    
    /**
     * Get all system prompt entities (for syncing)
     */
    suspend fun getAllPromptsEntities(): List<SystemPromptEntity> = withContext(Dispatchers.IO) {
        systemPromptDao.getAllPromptsSync()
    }
    
    /**
     * Upsert system prompt (for cloud sync)
     */
    suspend fun upsertSystemPrompt(prompt: SystemPromptEntity): Unit = withContext(Dispatchers.IO) {
        systemPromptDao.insertPrompt(prompt)
    }
    
    /**
     * Upsert multiple system prompts (for cloud sync)
     */
    suspend fun upsertSystemPrompts(prompts: List<SystemPromptEntity>): Unit = withContext(Dispatchers.IO) {
        prompts.forEach { systemPromptDao.insertPrompt(it) }
    }
    
    private fun SystemPromptEntity.toDomainModel(): SystemPrompt {
        return SystemPrompt(
            id = id,
            name = name,
            content = content,
            type = PromptType.fromString(promptType),
            isDefault = isDefault,
            createdAt = createdAt,
            updatedAt = updatedAt,
            usageCount = usageCount
        )
    }
}

enum class PromptType(val value: String) {
    API("api"),
    LOCAL("local");
    
    companion object {
        fun fromString(value: String): PromptType {
            return when (value.lowercase()) {
                "api" -> API
                "local" -> LOCAL
                else -> API
            }
        }
    }
}
