package com.yourown.ai.data.local.dao

import androidx.room.*
import com.yourown.ai.data.local.entity.*
import com.yourown.ai.domain.model.UserBiography
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeConversationById(id: String): Flow<ConversationEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
    
    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)
    
    @Query("UPDATE conversations SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean)
    
    @Query("UPDATE conversations SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: String, isArchived: Boolean)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY createdAt ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesByConversation(conversationId: String): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesByConversationSync(conversationId: String): List<MessageEntity>
    
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    @Delete
    suspend fun deleteMessage(message: MessageEntity)
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: String)
    
    @Query("SELECT SUM(tokenCount) FROM messages WHERE conversationId = :conversationId")
    suspend fun getTotalTokensByConversation(conversationId: String): Int?
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY uploadedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>
    
    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: String): DocumentEntity?
    
    @Query("SELECT * FROM documents WHERE conversationId = :conversationId")
    fun getDocumentsByConversation(conversationId: String): Flow<List<DocumentEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)
    
    @Update
    suspend fun updateDocument(document: DocumentEntity)
    
    @Delete
    suspend fun deleteDocument(document: DocumentEntity)
    
    @Query("UPDATE documents SET isProcessed = :isProcessed, chunkCount = :chunkCount WHERE id = :id")
    suspend fun updateProcessingStatus(id: String, isProcessed: Boolean, chunkCount: Int)
}

@Dao
interface DocumentChunkDao {
    @Query("SELECT * FROM document_chunks WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    suspend fun getChunksByDocument(documentId: String): List<DocumentChunkEntity>
    
    @Query("SELECT * FROM document_chunks ORDER BY documentId, chunkIndex ASC")
    suspend fun getAllChunks(): List<DocumentChunkEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: DocumentChunkEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DocumentChunkEntity>)
    
    @Query("DELETE FROM document_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksByDocument(documentId: String)
}

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys WHERE isActive = 1 ORDER BY lastUsedAt DESC")
    fun getAllActiveKeys(): Flow<List<ApiKeyEntity>>
    
    @Query("SELECT * FROM api_keys WHERE provider = :provider")
    suspend fun getKeyByProvider(provider: String): ApiKeyEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKey(apiKey: ApiKeyEntity)
    
    @Update
    suspend fun updateApiKey(apiKey: ApiKeyEntity)
    
    @Delete
    suspend fun deleteApiKey(apiKey: ApiKeyEntity)
    
    @Query("UPDATE api_keys SET lastUsedAt = :timestamp WHERE provider = :provider")
    suspend fun updateLastUsed(provider: String, timestamp: Long)
}

@Dao
interface UsageStatsDao {
    @Query("SELECT * FROM usage_stats WHERE date = :date ORDER BY provider, model")
    suspend fun getStatsByDate(date: String): List<UsageStatsEntity>
    
    @Query("SELECT * FROM usage_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getStatsByDateRange(startDate: String, endDate: String): List<UsageStatsEntity>
    
    @Query("SELECT * FROM usage_stats WHERE provider = :provider ORDER BY date DESC")
    fun getStatsByProvider(provider: String): Flow<List<UsageStatsEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: UsageStatsEntity)
    
    @Query("SELECT SUM(totalTokens) FROM usage_stats WHERE date = :date")
    suspend fun getTotalTokensByDate(date: String): Int?
    
    @Query("SELECT SUM(estimatedCost) FROM usage_stats WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalCostByDateRange(startDate: String, endDate: String): Double?
}

@Dao
interface SystemPromptDao {
    @Query("SELECT * FROM system_prompts ORDER BY isDefault DESC, usageCount DESC, createdAt DESC")
    fun getAllPrompts(): Flow<List<SystemPromptEntity>>
    
    @Query("SELECT * FROM system_prompts ORDER BY isDefault DESC, usageCount DESC, createdAt DESC")
    suspend fun getAllPromptsSync(): List<SystemPromptEntity>
    
    @Query("SELECT * FROM system_prompts WHERE promptType = :type ORDER BY isDefault DESC, usageCount DESC, createdAt DESC")
    fun getPromptsByType(type: String): Flow<List<SystemPromptEntity>>
    
    @Query("SELECT * FROM system_prompts WHERE id = :id")
    suspend fun getPromptById(id: String): SystemPromptEntity?
    
    @Query("SELECT * FROM system_prompts WHERE isDefault = 1 AND promptType = 'api' LIMIT 1")
    suspend fun getDefaultApiPrompt(): SystemPromptEntity?
    
    @Query("SELECT * FROM system_prompts WHERE isDefault = 1 AND promptType = 'local' LIMIT 1")
    suspend fun getDefaultLocalPrompt(): SystemPromptEntity?
    
    @Query("SELECT * FROM system_prompts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultPrompt(): SystemPromptEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: SystemPromptEntity)
    
    @Update
    suspend fun updatePrompt(prompt: SystemPromptEntity)
    
    @Delete
    suspend fun deletePrompt(prompt: SystemPromptEntity)
    
    @Query("UPDATE system_prompts SET isDefault = 0 WHERE promptType = :type")
    suspend fun clearDefaultsForType(type: String)
    
    @Query("UPDATE system_prompts SET isDefault = 0")
    suspend fun clearAllDefaults()
    
    @Query("UPDATE system_prompts SET isDefault = 1 WHERE id = :id")
    suspend fun setAsDefault(id: String)
    
    @Query("UPDATE system_prompts SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: String)
}

@Dao
interface KnowledgeDocumentDao {
    @Query("SELECT * FROM knowledge_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<KnowledgeDocumentEntity>>
    
    @Query("SELECT * FROM knowledge_documents WHERE id = :id")
    suspend fun getDocumentById(id: String): KnowledgeDocumentEntity?
    
    // Документы, привязанные к конкретной persona
    @Query("SELECT * FROM knowledge_documents WHERE linkedPersonaIds LIKE '%\"' || :personaId || '\"%' ORDER BY createdAt DESC")
    fun getDocumentsByPersonaId(personaId: String): Flow<List<KnowledgeDocumentEntity>>
    
    // Глобальные документы (не привязанные ни к одной persona)
    @Query("SELECT * FROM knowledge_documents WHERE linkedPersonaIds = '[]' ORDER BY createdAt DESC")
    fun getGlobalDocuments(): Flow<List<KnowledgeDocumentEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: KnowledgeDocumentEntity)
    
    @Update
    suspend fun updateDocument(document: KnowledgeDocumentEntity)
    
    @Delete
    suspend fun deleteDocument(document: KnowledgeDocumentEntity)
    
    @Query("DELETE FROM knowledge_documents WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface PersonaDao {
    @Query("SELECT * FROM personas ORDER BY createdAt DESC")
    fun getAllPersonas(): Flow<List<PersonaEntity>>
    
    @Query("SELECT * FROM personas ORDER BY createdAt DESC")
    suspend fun getAllPersonasSync(): List<PersonaEntity>
    
    @Query("SELECT * FROM personas WHERE id = :id")
    suspend fun getPersonaById(id: String): PersonaEntity?
    
    @Query("SELECT * FROM personas WHERE id = :id")
    fun observePersonaById(id: String): Flow<PersonaEntity?>
    
    @Query("SELECT * FROM personas WHERE isForApi = :isForApi ORDER BY name ASC")
    fun getPersonasByType(isForApi: Boolean): Flow<List<PersonaEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: PersonaEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonas(personas: List<PersonaEntity>)
    
    @Update
    suspend fun updatePersona(persona: PersonaEntity)
    
    @Delete
    suspend fun deletePersona(persona: PersonaEntity)
    
    @Query("DELETE FROM personas WHERE id = :id")
    suspend fun deletePersonaById(id: String)
    
    @Query("SELECT COUNT(*) FROM personas")
    suspend fun getPersonaCount(): Int
}

@Dao
interface BiographyDao {
    @Query("SELECT * FROM user_biography WHERE id = 'default'")
    fun getBiography(): Flow<BiographyEntity?>
    
    @Query("SELECT * FROM user_biography WHERE id = 'default'")
    suspend fun getBiographySync(): BiographyEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBiography(biography: BiographyEntity)
    
    @Query("DELETE FROM user_biography")
    suspend fun deleteBiography()
}

@Dao
interface BiographyChunkDao {
    @Query("SELECT * FROM biography_chunks WHERE biographyId = :biographyId ORDER BY createdAt ASC")
    suspend fun getChunksByBiographyId(biographyId: String): List<BiographyChunkEntity>
    
    @Query("SELECT * FROM biography_chunks WHERE biographyId = 'default'")
    suspend fun getAllChunks(): List<BiographyChunkEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: BiographyChunkEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<BiographyChunkEntity>)
    
    @Query("DELETE FROM biography_chunks WHERE biographyId = :biographyId")
    suspend fun deleteChunksByBiographyId(biographyId: String)
    
    @Query("DELETE FROM biography_chunks")
    suspend fun deleteAllChunks()
}
