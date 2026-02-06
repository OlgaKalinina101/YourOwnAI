package com.yourown.ai.presentation.settings.managers

import com.yourown.ai.data.repository.KnowledgeDocumentRepository
import com.yourown.ai.domain.model.KnowledgeDocument
import javax.inject.Inject

/**
 * Manager for Knowledge Documents (RAG)
 * Handles: CRUD operations for knowledge documents
 */
class KnowledgeDocumentManager @Inject constructor(
    private val knowledgeDocumentRepository: KnowledgeDocumentRepository
) {
    
    /**
     * Create new knowledge document
     */
    suspend fun createDocument(
        name: String, 
        content: String, 
        linkedPersonaIds: List<String> = emptyList()
    ): Result<String> {
        return knowledgeDocumentRepository.createDocument(
            name = name,
            content = content,
            linkedPersonaIds = linkedPersonaIds
        )
    }
    
    /**
     * Update existing knowledge document
     */
    suspend fun updateDocument(
        id: String,
        name: String,
        content: String,
        createdAt: Long,
        linkedPersonaIds: List<String> = emptyList()
    ): Result<Unit> {
        val document = KnowledgeDocument(
            id = id,
            name = name,
            content = content,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis(),
            sizeBytes = content.toByteArray().size,
            linkedPersonaIds = linkedPersonaIds
        )
        return knowledgeDocumentRepository.updateDocument(document)
    }
    
    /**
     * Delete knowledge document and its embeddings
     */
    suspend fun deleteDocument(id: String): Result<Unit> {
        return knowledgeDocumentRepository.deleteDocument(id)
    }
}
