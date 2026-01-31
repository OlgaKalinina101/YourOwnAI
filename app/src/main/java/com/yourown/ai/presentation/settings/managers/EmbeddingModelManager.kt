package com.yourown.ai.presentation.settings.managers

import com.yourown.ai.data.repository.LocalEmbeddingModelRepository
import com.yourown.ai.data.repository.MemoryRepository
import com.yourown.ai.data.repository.DocumentEmbeddingRepository
import com.yourown.ai.domain.model.LocalEmbeddingModel
import javax.inject.Inject

/**
 * Manager for Embedding Models
 * Handles: Downloading/deleting embedding models and recalculating embeddings
 */
class EmbeddingModelManager @Inject constructor(
    private val embeddingModelRepository: LocalEmbeddingModelRepository,
    private val memoryRepository: MemoryRepository,
    private val documentEmbeddingRepository: DocumentEmbeddingRepository
) {
    
    /**
     * Download embedding model
     */
    suspend fun downloadModel(model: LocalEmbeddingModel) {
        embeddingModelRepository.downloadModel(model)
    }
    
    /**
     * Delete embedding model
     */
    suspend fun deleteModel(model: LocalEmbeddingModel) {
        embeddingModelRepository.deleteModel(model)
    }
    
    /**
     * Recalculate all embeddings (Memory + RAG)
     * Returns pair of (memoryCount, ragChunkCount)
     */
    suspend fun recalculateAllEmbeddings(
        onMemoryProgress: (current: Int, total: Int, percentage: Float) -> Unit,
        onRAGProgress: (current: Int, total: Int, percentage: Float) -> Unit
    ): Result<Pair<Int, Int>> {
        return try {
            // Recalculate Memory embeddings
            val memoryResult = memoryRepository.recalculateAllEmbeddings { current, total, percentage ->
                onMemoryProgress(current, total, percentage)
            }
            val memoryCount = memoryResult.getOrNull() ?: 0
            
            // Recalculate RAG embeddings
            val ragResult = documentEmbeddingRepository.recalculateAllEmbeddings { current, total, percentage ->
                onRAGProgress(current, total, percentage)
            }
            val ragCount = ragResult.getOrNull() ?: 0
            
            Result.success(Pair(memoryCount, ragCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
