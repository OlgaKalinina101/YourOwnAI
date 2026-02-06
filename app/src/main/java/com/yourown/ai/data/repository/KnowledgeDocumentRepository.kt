package com.yourown.ai.data.repository

import com.yourown.ai.data.local.YourOwnAIDatabase
import com.yourown.ai.data.local.entity.KnowledgeDocumentEntity
import com.yourown.ai.domain.model.KnowledgeDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing knowledge documents
 */
@Singleton
class KnowledgeDocumentRepository @Inject constructor(
    private val database: YourOwnAIDatabase,
    private val documentEmbeddingRepository: DocumentEmbeddingRepository,
    private val aiConfigRepository: AIConfigRepository
) {
    private val dao = database.knowledgeDocumentDao()
    
    /**
     * Get all documents as Flow
     */
    fun getAllDocuments(): Flow<List<KnowledgeDocument>> {
        return dao.getAllDocuments().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get document by ID
     */
    suspend fun getDocumentById(id: String): KnowledgeDocument? {
        return dao.getDocumentById(id)?.toDomain()
    }
    
    /**
     * Create new document and process it for RAG
     */
    suspend fun createDocument(
        name: String, 
        content: String, 
        linkedPersonaIds: List<String> = emptyList()
    ): Result<String> {
        return try {
            val now = System.currentTimeMillis()
            val documentId = UUID.randomUUID().toString()
            val entity = KnowledgeDocumentEntity(
                id = documentId,
                name = name,
                content = content,
                createdAt = now,
                updatedAt = now,
                sizeBytes = content.toByteArray().size,
                linkedPersonaIds = com.google.gson.Gson().toJson(linkedPersonaIds)
            )
            dao.insertDocument(entity)
            
            // Get current RAG settings
            val config = aiConfigRepository.getAIConfig()
            
            // Process document for RAG (chunk and embed)
            if (config.ragEnabled) {
                documentEmbeddingRepository.processDocument(
                    documentId = documentId,
                    documentName = name,
                    content = content,
                    chunkSize = config.ragChunkSize,
                    chunkOverlap = config.ragChunkOverlap
                )
            }
            
            Result.success(documentId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update document and reprocess for RAG
     */
    suspend fun updateDocument(document: KnowledgeDocument): Result<Unit> {
        return try {
            val entity = document.toEntity().copy(
                updatedAt = System.currentTimeMillis(),
                sizeBytes = document.content.toByteArray().size
            )
            dao.updateDocument(entity)
            
            // Get current RAG settings
            val config = aiConfigRepository.getAIConfig()
            
            // Reprocess document for RAG if enabled
            if (config.ragEnabled) {
                // Delete old chunks
                documentEmbeddingRepository.deleteDocumentChunks(
                    documentId = document.id,
                    documentName = document.name
                )
                
                // Process with new content
                documentEmbeddingRepository.processDocument(
                    documentId = document.id,
                    documentName = document.name,
                    content = document.content,
                    chunkSize = config.ragChunkSize,
                    chunkOverlap = config.ragChunkOverlap
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete document and its chunks
     */
    suspend fun deleteDocument(documentId: String): Result<Unit> {
        return try {
            // Get document info for progress display
            val document = dao.getDocumentById(documentId)
            
            if (document != null) {
                // Delete chunks first
                documentEmbeddingRepository.deleteDocumentChunks(
                    documentId = documentId,
                    documentName = document.name
                )
            }
            
            // Delete document
            dao.deleteById(documentId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get processing status flow
     */
    fun getProcessingStatus() = documentEmbeddingRepository.processingStatus
    
    // Mappers
    private fun KnowledgeDocumentEntity.toDomain() = KnowledgeDocument(
        id = id,
        name = name,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sizeBytes = sizeBytes
    )
    
    private fun KnowledgeDocument.toEntity() = KnowledgeDocumentEntity(
        id = id,
        name = name,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sizeBytes = sizeBytes
    )
    
    /**
     * Upsert document (for cloud sync)
     */
    suspend fun upsertDocument(document: KnowledgeDocumentEntity): Unit = withContext(Dispatchers.IO) {
        dao.insertDocument(document)
    }
    
    /**
     * Upsert multiple documents (for cloud sync)
     */
    suspend fun upsertDocuments(documents: List<KnowledgeDocumentEntity>): Unit = withContext(Dispatchers.IO) {
        documents.forEach { dao.insertDocument(it) }
    }
}
