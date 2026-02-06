package com.yourown.ai.data.repository

import android.util.Log
import com.google.gson.Gson
import com.yourown.ai.data.local.dao.DocumentChunkDao
import com.yourown.ai.data.local.dao.DocumentDao
import com.yourown.ai.data.local.entity.DocumentChunkEntity
import com.yourown.ai.data.util.SemanticSearchUtil
import com.yourown.ai.domain.service.EmbeddingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processing status for document embedding
 */
sealed class DocumentProcessingStatus {
    object Idle : DocumentProcessingStatus()
    data class Processing(
        val documentId: String,
        val documentName: String,
        val progress: Int, // 0-100
        val currentStep: String
    ) : DocumentProcessingStatus()
    data class Deleting(
        val documentId: String,
        val documentName: String,
        val progress: Int // 0-100
    ) : DocumentProcessingStatus()
    data class Completed(val documentId: String) : DocumentProcessingStatus()
    data class Failed(val documentId: String, val error: String) : DocumentProcessingStatus()
}

/**
 * Repository for managing document embeddings and chunks for RAG
 */
@Singleton
class DocumentEmbeddingRepository @Inject constructor(
    private val documentDao: DocumentDao,
    private val documentChunkDao: DocumentChunkDao,
    private val knowledgeDocumentDao: com.yourown.ai.data.local.dao.KnowledgeDocumentDao,
    private val embeddingService: EmbeddingService,
    private val gson: Gson
) {
    
    private val _processingStatus = MutableStateFlow<DocumentProcessingStatus>(DocumentProcessingStatus.Idle)
    val processingStatus: StateFlow<DocumentProcessingStatus> = _processingStatus.asStateFlow()
    
    companion object {
        private const val TAG = "DocumentEmbeddingRepo"
        private const val BATCH_SIZE = 5 // Process 5 chunks at a time for battery optimization
    }
    
    /**
     * Process document: chunk text and generate embeddings
     */
    suspend fun processDocument(
        documentId: String,
        documentName: String,
        content: String,
        chunkSize: Int,
        chunkOverlap: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting document processing: $documentName")
            
            _processingStatus.value = DocumentProcessingStatus.Processing(
                documentId = documentId,
                documentName = documentName,
                progress = 0,
                currentStep = "Chunking document..."
            )
            
            // 1. Split document into chunks
            val chunks = chunkText(content, chunkSize, chunkOverlap)
            Log.i(TAG, "Created ${chunks.size} chunks")
            
            if (chunks.isEmpty()) {
                _processingStatus.value = DocumentProcessingStatus.Failed(
                    documentId,
                    "Document is empty or too short"
                )
                return@withContext Result.failure(Exception("Document is empty"))
            }
            
            // 2. Generate embeddings in batches
            val chunkEntities = mutableListOf<DocumentChunkEntity>()
            val totalChunks = chunks.size
            
            chunks.forEachIndexed { index, chunkText ->
                val batchIndex = index / BATCH_SIZE
                val batchProgress = ((index + 1) * 100) / totalChunks
                
                _processingStatus.value = DocumentProcessingStatus.Processing(
                    documentId = documentId,
                    documentName = documentName,
                    progress = batchProgress,
                    currentStep = "Embedding chunk ${index + 1}/$totalChunks..."
                )
                
                // Generate embedding for this chunk
                val embeddingResult = embeddingService.generateEmbedding(chunkText)
                
                if (embeddingResult.isSuccess) {
                    val embedding = embeddingResult.getOrNull()
                    val embeddingJson = if (embedding != null) {
                        gson.toJson(embedding.toList())
                    } else null
                    
                    chunkEntities.add(
                        DocumentChunkEntity(
                            id = UUID.randomUUID().toString(),
                            documentId = documentId,
                            content = chunkText,
                            chunkIndex = index,
                            embedding = embeddingJson
                        )
                    )
                } else {
                    Log.w(TAG, "Failed to embed chunk $index: ${embeddingResult.exceptionOrNull()?.message}")
                    // Still save chunk without embedding
                    chunkEntities.add(
                        DocumentChunkEntity(
                            id = UUID.randomUUID().toString(),
                            documentId = documentId,
                            content = chunkText,
                            chunkIndex = index,
                            embedding = null
                        )
                    )
                }
                
                // Save batch to database periodically to avoid memory issues
                if ((index + 1) % BATCH_SIZE == 0 || index == chunks.size - 1) {
                    val batchToSave = chunkEntities.takeLast(
                        minOf(BATCH_SIZE, chunkEntities.size - (batchIndex * BATCH_SIZE))
                    )
                    documentChunkDao.insertChunks(batchToSave)
                    Log.d(TAG, "Saved batch of ${batchToSave.size} chunks")
                }
            }
            
            // 3. Update document status
            val document = documentDao.getDocumentById(documentId)
            if (document != null) {
                documentDao.updateDocument(
                    document.copy(
                        isProcessed = true,
                        chunkCount = chunks.size
                    )
                )
            }
            
            _processingStatus.value = DocumentProcessingStatus.Completed(documentId)
            Log.i(TAG, "Document processing completed: $documentName")
            
            // Reset to Idle after a short delay to show completion status
            kotlinx.coroutines.delay(1500)
            _processingStatus.value = DocumentProcessingStatus.Idle
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing document", e)
            _processingStatus.value = DocumentProcessingStatus.Failed(
                documentId,
                e.message ?: "Unknown error"
            )
            
            // Reset to Idle after showing error
            kotlinx.coroutines.delay(3000)
            _processingStatus.value = DocumentProcessingStatus.Idle
            
            Result.failure(e)
        }
    }
    
    /**
     * Delete document chunks and embeddings
     */
    suspend fun deleteDocumentChunks(
        documentId: String,
        documentName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Deleting chunks for document: $documentName")
            
            _processingStatus.value = DocumentProcessingStatus.Deleting(
                documentId = documentId,
                documentName = documentName,
                progress = 50
            )
            
            documentChunkDao.deleteChunksByDocument(documentId)
            
            _processingStatus.value = DocumentProcessingStatus.Deleting(
                documentId = documentId,
                documentName = documentName,
                progress = 100
            )
            
            // Reset to idle after a short delay
            kotlinx.coroutines.delay(500)
            _processingStatus.value = DocumentProcessingStatus.Idle
            
            Log.i(TAG, "Chunks deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chunks", e)
            _processingStatus.value = DocumentProcessingStatus.Failed(
                documentId,
                "Failed to delete chunks: ${e.message}"
            )
            Result.failure(e)
        }
    }
    
    /**
     * Get chunks for a document
     */
    suspend fun getDocumentChunks(documentId: String): List<DocumentChunkEntity> {
        return withContext(Dispatchers.IO) {
            documentChunkDao.getChunksByDocument(documentId)
        }
    }
    
    /**
     * Search similar chunks using embedding
     */
    /**
     * Search for similar chunks using semantic search
     * Combines embedding similarity + keyword matching + exact match boost
     * 
     * @param query User's current message
     * @param topK Maximum number of chunks to return (default 5)
     * @return List of most relevant chunks with similarity scores
     */
    suspend fun searchSimilarChunks(
        query: String,
        topK: Int = 5
    ): List<Pair<DocumentChunkEntity, Float>> = withContext(Dispatchers.IO) {
        try {
            // Generate embedding for query
            val queryEmbeddingResult = embeddingService.generateEmbedding(query)
            if (queryEmbeddingResult.isFailure) {
                Log.w(TAG, "Failed to generate query embedding for RAG search")
                return@withContext emptyList()
            }
            val queryEmbedding = queryEmbeddingResult.getOrNull() ?: return@withContext emptyList()
            
            // Get all chunks with embeddings
            val allChunks = documentChunkDao.getAllChunks()
                .filter { it.embedding != null }
            
            if (allChunks.isEmpty()) {
                Log.d(TAG, "No chunks available for RAG search")
                return@withContext emptyList()
            }
            
            // Parse embeddings
            val chunksWithEmbeddings = allChunks.mapNotNull { chunk ->
                try {
                    val embeddingList = gson.fromJson(chunk.embedding, List::class.java) as List<Double>
                    val embedding = embeddingList.map { it.toFloat() }.toFloatArray()
                    chunk to embedding
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse embedding for chunk ${chunk.id}")
                    null
                }
            }
            
            // Use semantic search
            val results = SemanticSearchUtil.findSimilar(
                query = query,
                queryEmbedding = queryEmbedding,
                items = chunksWithEmbeddings,
                getText = { it.first.content },
                getEmbedding = { it.second },
                k = topK
            )
            
            Log.d(TAG, "Found ${results.size} similar chunks for RAG")
            
            // Return chunks with their scores
            results.map { it.item.first to it.score }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching similar chunks", e)
            emptyList()
        }
    }
    
    /**
     * Search similar chunks for a specific Persona
     */
    suspend fun searchSimilarChunksByPersona(
        query: String,
        personaId: String,
        topK: Int = 5
    ): List<Pair<DocumentChunkEntity, Float>> = withContext(Dispatchers.IO) {
        try {
            val queryEmbeddingResult = embeddingService.generateEmbedding(query)
            if (queryEmbeddingResult.isFailure) return@withContext emptyList()
            val queryEmbedding = queryEmbeddingResult.getOrNull() ?: return@withContext emptyList()
            
            // Get documents linked to this persona
            val personaDocuments = knowledgeDocumentDao.getDocumentsByPersonaId(personaId).first()
            val personaDocumentIds = personaDocuments.map { it.id }.toSet()
            
            // Get chunks only from persona documents
            val personaChunks = documentChunkDao.getAllChunks()
                .filter { it.documentId in personaDocumentIds && it.embedding != null }
            
            if (personaChunks.isEmpty()) return@withContext emptyList()
            
            val chunksWithEmbeddings = personaChunks.mapNotNull { chunk ->
                try {
                    val embeddingList = gson.fromJson(chunk.embedding, List::class.java) as List<Double>
                    val embedding = embeddingList.map { it.toFloat() }.toFloatArray()
                    chunk to embedding
                } catch (e: Exception) {
                    null
                }
            }
            
            val results = SemanticSearchUtil.findSimilar(
                query = query,
                queryEmbedding = queryEmbedding,
                items = chunksWithEmbeddings,
                getText = { it.first.content },
                getEmbedding = { it.second },
                k = topK
            )
            
            Log.d(TAG, "Found ${results.size} persona chunks")
            results.map { it.item.first to it.score }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching persona chunks", e)
            emptyList()
        }
    }
    
    /**
     * Search similar chunks from global documents (not linked to any Persona)
     */
    suspend fun searchSimilarGlobalChunks(
        query: String,
        topK: Int = 5
    ): List<Pair<DocumentChunkEntity, Float>> = withContext(Dispatchers.IO) {
        try {
            val queryEmbeddingResult = embeddingService.generateEmbedding(query)
            if (queryEmbeddingResult.isFailure) return@withContext emptyList()
            val queryEmbedding = queryEmbeddingResult.getOrNull() ?: return@withContext emptyList()
            
            // Get global documents (not linked to any persona)
            val globalDocuments = knowledgeDocumentDao.getGlobalDocuments().first()
            val globalDocumentIds = globalDocuments.map { it.id }.toSet()
            
            // Get chunks only from global documents
            val globalChunks = documentChunkDao.getAllChunks()
                .filter { it.documentId in globalDocumentIds && it.embedding != null }
            
            if (globalChunks.isEmpty()) return@withContext emptyList()
            
            val chunksWithEmbeddings = globalChunks.mapNotNull { chunk ->
                try {
                    val embeddingList = gson.fromJson(chunk.embedding, List::class.java) as List<Double>
                    val embedding = embeddingList.map { it.toFloat() }.toFloatArray()
                    chunk to embedding
                } catch (e: Exception) {
                    null
                }
            }
            
            val results = SemanticSearchUtil.findSimilar(
                query = query,
                queryEmbedding = queryEmbedding,
                items = chunksWithEmbeddings,
                getText = { it.first.content },
                getEmbedding = { it.second },
                k = topK
            )
            
            Log.d(TAG, "Found ${results.size} global chunks")
            results.map { it.item.first to it.score }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching global chunks", e)
            emptyList()
        }
    }
    
    /**
     * Chunk text into overlapping segments
     */
    private fun chunkText(text: String, chunkSize: Int, overlap: Int): List<String> {
        if (text.isEmpty()) return emptyList()
        
        val chunks = mutableListOf<String>()
        var startIndex = 0
        
        while (startIndex < text.length) {
            val endIndex = minOf(startIndex + chunkSize, text.length)
            val chunk = text.substring(startIndex, endIndex).trim()
            
            if (chunk.isNotEmpty()) {
                chunks.add(chunk)
            }
            
            // Move forward by (chunkSize - overlap)
            startIndex += (chunkSize - overlap)
            
            // Break if we've reached the end
            if (endIndex >= text.length) break
        }
        
        return chunks
    }
    
    /**
     * Reset processing status
     */
    fun resetStatus() {
        _processingStatus.value = DocumentProcessingStatus.Idle
    }
    
    /**
     * Recalculate embeddings for all RAG chunks
     * Use this when switching embedding models
     * @param onProgress callback with (current, total, percentage) progress
     */
    suspend fun recalculateAllEmbeddings(
        onProgress: (current: Int, total: Int, percentage: Float) -> Unit = { _, _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Get all chunks
            val allChunks = documentChunkDao.getAllChunks()
            if (allChunks.isEmpty()) {
                Log.i(TAG, "No RAG chunks to recalculate")
                return@withContext Result.success(0)
            }
            
            Log.i(TAG, "Starting RAG embedding recalculation for ${allChunks.size} chunks")
            
            var processedCount = 0
            val totalChunks = allChunks.size
            
            // Process each chunk with progress tracking
            allChunks.forEachIndexed { index, chunk ->
                // Generate new embedding
                val embeddingResult = embeddingService.generateEmbedding(chunk.content)
                
                if (embeddingResult.isSuccess) {
                    val embedding = embeddingResult.getOrNull()
                    if (embedding != null) {
                        // Convert embedding to JSON string
                        val embeddingJson = gson.toJson(embedding.toList())
                        
                        // Update chunk with new embedding (using insert with REPLACE strategy)
                        documentChunkDao.insertChunk(
                            chunk.copy(embedding = embeddingJson)
                        )
                        processedCount++
                    }
                } else {
                    Log.w(TAG, "Failed to generate embedding for chunk ${chunk.id}")
                }
                
                // Update progress after each chunk
                val percentage = if (totalChunks > 0) (index + 1).toFloat() / totalChunks else 0f
                onProgress(index + 1, totalChunks, percentage)
            }
            
            Log.i(TAG, "Completed RAG embedding recalculation: $processedCount chunks")
            Result.success(processedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error recalculating RAG embeddings", e)
            Result.failure(e)
        }
    }
}
