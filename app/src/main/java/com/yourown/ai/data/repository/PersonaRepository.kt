package com.yourown.ai.data.repository

import com.yourown.ai.data.local.dao.PersonaDao
import com.yourown.ai.data.mapper.toDomain
import com.yourown.ai.data.mapper.toEntity
import com.yourown.ai.domain.model.Persona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Persona management
 * 
 * Handles CRUD operations for personas and document/memory linking
 */
@Singleton
class PersonaRepository @Inject constructor(
    private val personaDao: PersonaDao,
    private val aiConfigRepository: AIConfigRepository
) {
    
    /**
     * Get all personas as Flow
     */
    fun getAllPersonas(): Flow<List<Persona>> {
        return personaDao.getAllPersonas().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get personas by type (API/Local)
     */
    fun getPersonasByType(isForApi: Boolean): Flow<List<Persona>> {
        return personaDao.getPersonasByType(isForApi).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get persona by ID
     */
    suspend fun getPersonaById(id: String): Persona? {
        return personaDao.getPersonaById(id)?.toDomain()
    }
    
    /**
     * Observe persona by ID
     */
    fun observePersonaById(id: String): Flow<Persona?> {
        return personaDao.observePersonaById(id).map { it?.toDomain() }
    }
    
    /**
     * Create new persona from SystemPrompt
     */
    suspend fun createPersonaFromSystemPrompt(
        systemPromptId: String,
        systemPromptName: String,
        systemPromptContent: String,
        description: String = "",
        isForApi: Boolean = true
    ): Persona {
        // Проверяем, существует ли уже persona для этого systemPromptId
        val existingPersona = getPersonaBySystemPromptId(systemPromptId)
        val id = existingPersona?.id ?: UUID.randomUUID().toString()
        
        val globalConfig = aiConfigRepository.aiConfig.first()
        
        val persona = Persona.fromSystemPrompt(
            id = id,
            systemPromptId = systemPromptId,
            systemPromptName = systemPromptName,
            systemPromptContent = systemPromptContent,
            description = description,
            config = globalConfig,
            isForApi = isForApi
        )
        
        personaDao.insertPersona(persona.toEntity())
        return persona
    }
    
    /**
     * Get persona by SystemPrompt ID
     */
    suspend fun getPersonaBySystemPromptId(systemPromptId: String): Persona? {
        val personas = personaDao.getAllPersonas().first()
        return personas.map { it.toDomain() }.find { it.systemPromptId == systemPromptId }
    }
    
    /**
     * Create persona from existing settings (for customization)
     */
    suspend fun createPersonaFromSettings(persona: Persona): String {
        personaDao.insertPersona(persona.toEntity())
        return persona.id
    }
    
    /**
     * Update persona
     */
    suspend fun updatePersona(persona: Persona) {
        val updated = persona.copy(updatedAt = System.currentTimeMillis())
        personaDao.updatePersona(updated.toEntity())
    }
    
    /**
     * Delete persona
     */
    suspend fun deletePersona(id: String) {
        personaDao.deletePersonaById(id)
    }
    
    /**
     * Link document to persona
     */
    suspend fun linkDocumentToPersona(personaId: String, documentId: String) {
        val persona = getPersonaById(personaId) ?: return
        
        if (!persona.linkedDocumentIds.contains(documentId)) {
            val updated = persona.copy(
                linkedDocumentIds = persona.linkedDocumentIds + documentId,
                updatedAt = System.currentTimeMillis()
            )
            updatePersona(updated)
        }
    }
    
    /**
     * Unlink document from persona
     */
    suspend fun unlinkDocumentFromPersona(personaId: String, documentId: String) {
        val persona = getPersonaById(personaId) ?: return
        
        val updated = persona.copy(
            linkedDocumentIds = persona.linkedDocumentIds.filter { it != documentId },
            updatedAt = System.currentTimeMillis()
        )
        updatePersona(updated)
    }
    
    /**
     * Get documents for persona
     */
    suspend fun getLinkedDocuments(personaId: String): List<String> {
        return getPersonaById(personaId)?.linkedDocumentIds ?: emptyList()
    }
    
    /**
     * Update persona's AI settings
     */
    suspend fun updateAISettings(
        personaId: String,
        temperature: Float? = null,
        topP: Float? = null,
        maxTokens: Int? = null,
        deepEmpathy: Boolean? = null,
        memoryEnabled: Boolean? = null,
        ragEnabled: Boolean? = null,
        messageHistoryLimit: Int? = null
    ) {
        val persona = getPersonaById(personaId) ?: return
        
        val updated = persona.copy(
            temperature = temperature ?: persona.temperature,
            topP = topP ?: persona.topP,
            maxTokens = maxTokens ?: persona.maxTokens,
            deepEmpathy = deepEmpathy ?: persona.deepEmpathy,
            memoryEnabled = memoryEnabled ?: persona.memoryEnabled,
            ragEnabled = ragEnabled ?: persona.ragEnabled,
            messageHistoryLimit = messageHistoryLimit ?: persona.messageHistoryLimit,
            updatedAt = System.currentTimeMillis()
        )
        
        updatePersona(updated)
    }
    
    /**
     * Update persona's memory settings
     */
    suspend fun updateMemorySettings(
        personaId: String,
        useOnlyPersonaMemories: Boolean? = null,
        shareMemoriesGlobally: Boolean? = null,
        memoryLimit: Int? = null,
        memoryMinAgeDays: Int? = null
    ) {
        val persona = getPersonaById(personaId) ?: return
        
        val updated = persona.copy(
            useOnlyPersonaMemories = useOnlyPersonaMemories ?: persona.useOnlyPersonaMemories,
            shareMemoriesGlobally = shareMemoriesGlobally ?: persona.shareMemoriesGlobally,
            memoryLimit = memoryLimit ?: persona.memoryLimit,
            memoryMinAgeDays = memoryMinAgeDays ?: persona.memoryMinAgeDays,
            updatedAt = System.currentTimeMillis()
        )
        
        updatePersona(updated)
    }
    
    /**
     * Update persona's model preference
     */
    suspend fun updateModelPreference(
        personaId: String,
        modelId: String?,
        provider: String?
    ) {
        val persona = getPersonaById(personaId) ?: return
        
        val updated = persona.copy(
            preferredModelId = modelId,
            preferredProvider = provider,
            updatedAt = System.currentTimeMillis()
        )
        
        updatePersona(updated)
    }
    
    /**
     * Check if personas exist
     */
    suspend fun hasPersonas(): Boolean {
        return personaDao.getPersonaCount() > 0
    }
}
