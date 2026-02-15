package com.yourown.ai.data.repository

import androidx.room.withTransaction
import com.yourown.ai.data.local.YourOwnAIDatabase
import com.yourown.ai.data.local.dao.MemoryDao
import com.yourown.ai.data.local.entity.MemoryEntity
import com.yourown.ai.data.local.entity.toDomain
import com.yourown.ai.data.local.entity.toEntity
import com.yourown.ai.data.util.SemanticSearchUtil
import com.yourown.ai.domain.model.MemoryEntry
import com.yourown.ai.domain.service.EmbeddingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory processing status for UI feedback
 */
sealed class MemoryProcessingStatus {
    object Idle : MemoryProcessingStatus()
    data class Recalculating(
        val progress: Int, // 0-100
        val current: Int,
        val total: Int,
        val currentStep: String
    ) : MemoryProcessingStatus()
    data class SavingMemory(val memoryId: String) : MemoryProcessingStatus()
    object Completed : MemoryProcessingStatus()
    data class Failed(val error: String) : MemoryProcessingStatus()
}

/**
 * Repository for managing user memories
 */
@Singleton
class MemoryRepository @Inject constructor(
    private val memoryDao: MemoryDao,
    private val embeddingService: EmbeddingService,
    private val database: YourOwnAIDatabase
) {
    
    private val _processingStatus = MutableStateFlow<MemoryProcessingStatus>(MemoryProcessingStatus.Idle)
    val processingStatus: StateFlow<MemoryProcessingStatus> = _processingStatus.asStateFlow()
    
    /**
     * Get all memories across all conversations
     */
    fun getAllMemories(): Flow<List<MemoryEntry>> {
        return memoryDao.getAllMemories()
            .conflate() // Skip intermediate emissions during rapid changes
            .map { entities ->
                entities.map { it.toDomain() }
            }
            .retryWhen { cause, attempt ->
                // Retry on CursorWindow/IllegalState errors (happen during bulk DB changes)
                if (cause is IllegalStateException || cause is android.database.StaleDataException) {
                    android.util.Log.w("MemoryRepository", "Flow retry #$attempt: ${cause.message}")
                    delay(500L * (attempt + 1).coerceAtMost(4)) // 500ms, 1s, 1.5s, 2s max
                    true
                } else {
                    false
                }
            }
            .flowOn(Dispatchers.IO) // Read cursor on IO thread
    }
    
    /**
     * Get all memory entities (including embeddings) for syncing
     */
    suspend fun getAllMemoryEntities(): List<MemoryEntity> = withContext(Dispatchers.IO) {
        memoryDao.getAllMemoriesEntity()
    }
    
    /**
     * Get memories for a specific persona
     */
    fun getMemoriesByPersonaId(personaId: String): Flow<List<MemoryEntry>> {
        return memoryDao.getMemoriesByPersonaId(personaId)
            .conflate()
            .map { entities -> entities.map { it.toDomain() } }
            .retryWhen { cause, attempt ->
                if (cause is IllegalStateException || cause is android.database.StaleDataException) {
                    delay(500L * (attempt + 1).coerceAtMost(4))
                    true
                } else false
            }
            .flowOn(Dispatchers.IO)
    }
    
    /**
     * Get memories for a specific conversation
     */
    fun getMemoriesForConversation(conversationId: String): Flow<List<MemoryEntry>> {
        return memoryDao.getMemoriesForConversation(conversationId)
            .conflate()
            .map { entities -> entities.map { it.toDomain() } }
            .retryWhen { cause, attempt ->
                if (cause is IllegalStateException || cause is android.database.StaleDataException) {
                    delay(500L * (attempt + 1).coerceAtMost(4))
                    true
                } else false
            }
            .flowOn(Dispatchers.IO)
    }
    
    /**
     * Get memory by ID
     */
    suspend fun getMemoryById(id: String): MemoryEntry? {
        return memoryDao.getMemoryById(id)?.toDomain()
    }
    
    /**
     * Get memory for a specific message (if exists)
     */
    suspend fun getMemoryForMessage(messageId: String): MemoryEntry? {
        return memoryDao.getMemoryForMessage(messageId)?.toDomain()
    }
    
    /**
     * Search memories by fact content
     */
    fun searchMemories(query: String): Flow<List<MemoryEntry>> {
        return memoryDao.searchMemories(query)
            .conflate()
            .map { entities -> entities.map { it.toDomain() } }
            .retryWhen { cause, attempt ->
                if (cause is IllegalStateException || cause is android.database.StaleDataException) {
                    delay(500L * (attempt + 1).coerceAtMost(4))
                    true
                } else false
            }
            .flowOn(Dispatchers.IO)
    }
    
    /**
     * Get total memory count
     */
    suspend fun getTotalMemoryCount(): Int {
        return memoryDao.getTotalMemoryCount()
    }
    
    /**
     * Insert new memory with pre-computed embedding
     */
    suspend fun insertMemory(memory: MemoryEntry) {
        // Generate embedding for the memory fact
        val embeddingResult = embeddingService.generateEmbedding(memory.fact)
        val embeddingString = if (embeddingResult.isSuccess) {
            embeddingResult.getOrNull()?.joinToString(",")
        } else {
            null
        }
        
        // Save memory with embedding
        memoryDao.insertMemory(memory.toEntity(embedding = embeddingString))
    }
    
    /**
     * Update existing memory and recalculate embedding
     */
    suspend fun updateMemory(memory: MemoryEntry) {
        try {
            _processingStatus.value = MemoryProcessingStatus.SavingMemory(memory.id)
            
            // Regenerate embedding for updated fact
            val embeddingResult = embeddingService.generateEmbedding(memory.fact)
            val embeddingString = if (embeddingResult.isSuccess) {
                embeddingResult.getOrNull()?.joinToString(",")
            } else {
                null
            }
            
            // Update memory with new embedding
            memoryDao.updateMemory(memory.toEntity(embedding = embeddingString))
            
            _processingStatus.value = MemoryProcessingStatus.Completed
            
            // Reset to idle after a short delay
            kotlinx.coroutines.delay(1500)
            _processingStatus.value = MemoryProcessingStatus.Idle
        } catch (e: Exception) {
            _processingStatus.value = MemoryProcessingStatus.Failed(
                error = e.message ?: "Failed to save memory"
            )
            
            // Reset to idle after showing error
            kotlinx.coroutines.delay(3000)
            _processingStatus.value = MemoryProcessingStatus.Idle
            
            throw e
        }
    }
    
    /**
     * Archive memory (soft delete)
     */
    suspend fun archiveMemory(id: String) {
        memoryDao.archiveMemory(id)
    }
    
    /**
     * Delete memory permanently
     */
    suspend fun deleteMemory(memory: MemoryEntry) {
        memoryDao.deleteMemory(memory.toEntity())
    }
    
    /**
     * Delete multiple memories by IDs in a single SQL transaction
     */
    suspend fun deleteMemoriesByIds(ids: List<String>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        // Process in chunks to avoid SQLite variable limit
        ids.chunked(50).forEach { chunk ->
            memoryDao.deleteMemoriesByIds(chunk)
        }
    }
    
    /**
     * Update memory fact without regenerating embedding (for batch operations)
     */
    suspend fun updateMemoryFact(memory: MemoryEntry) = withContext(Dispatchers.IO) {
        memoryDao.updateMemory(memory.toEntity())
    }
    
    /**
     * Atomic batch operation: update facts + delete memories in a SINGLE Room transaction.
     * This prevents CursorWindow crashes because Room only triggers ONE
     * InvalidationTracker notification AFTER the entire transaction completes,
     * instead of notifying on each individual update/delete.
     *
     * @param updates List of Pair(MemoryEntry, newFact) - memories to update
     * @param deleteIds List of memory IDs to permanently delete
     */
    suspend fun batchUpdateAndDelete(
        updates: List<Pair<MemoryEntry, String>>,
        deleteIds: List<String>
    ) = withContext(Dispatchers.IO) {
        database.withTransaction {
            // Step 1: Update all facts
            updates.forEach { (memory, newFact) ->
                val updated = memory.copy(fact = newFact)
                memoryDao.updateMemory(updated.toEntity())
            }
            
            // Step 2: Delete all in chunks (inside same transaction)
            if (deleteIds.isNotEmpty()) {
                deleteIds.chunked(50).forEach { chunk ->
                    memoryDao.deleteMemoriesByIds(chunk)
                }
            }
        }
        // InvalidationTracker fires here, ONCE, after commit
    }
    
    /**
     * Delete all memories for a conversation
     */
    suspend fun deleteMemoriesForConversation(conversationId: String) {
        memoryDao.deleteMemoriesForConversation(conversationId)
    }
    
    /**
     * Delete all memories (for debugging/testing)
     */
    suspend fun deleteAllMemories() {
        memoryDao.deleteAllMemories()
    }
    
    /**
     * Find similar memories using semantic search (embedding + keyword matching)
     * Now uses pre-computed embeddings for better performance
     * 
     * @param query User's current message
     * @param limit Maximum number of memories to return (default 5)
     * @param minAgeDays Minimum age in days for memories to be retrieved
     * @return List of most relevant memories
     */
    suspend fun findSimilarMemories(query: String, limit: Int = 5, minAgeDays: Int = 0): List<MemoryEntry> {
        try {
            // Get all memory entities (with embeddings)
            val allMemoryEntities = memoryDao.getAllMemories().first()
            if (allMemoryEntities.isEmpty()) return emptyList()
            
            // Filter memories by age (only retrieve memories older than minAgeDays)
            val currentTimeMillis = System.currentTimeMillis()
            val minAgeMillis = minAgeDays * 24 * 60 * 60 * 1000L
            val filteredEntities = if (minAgeDays > 0) {
                allMemoryEntities.filter { entity ->
                    (currentTimeMillis - entity.createdAt) >= minAgeMillis
                }
            } else {
                allMemoryEntities
            }
            
            if (filteredEntities.isEmpty()) return emptyList()
            
            // Generate embedding for query
            val queryEmbeddingResult = embeddingService.generateEmbedding(query)
            if (queryEmbeddingResult.isFailure) {
                android.util.Log.w("MemoryRepository", "Failed to generate query embedding for memory search")
                return emptyList()
            }
            val queryEmbedding = queryEmbeddingResult.getOrNull() ?: return emptyList()
            
            // Use pre-computed embeddings from database
            val memoriesWithEmbeddings = filteredEntities.mapNotNull { entity ->
                val embedding = entity.embedding?.let { embStr ->
                    embStr.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
                }
                
                // If embedding is missing, generate it (fallback)
                val finalEmbedding = if (embedding == null) {
                    android.util.Log.w("MemoryRepository", "Missing embedding for memory ${entity.id}, generating...")
                    val result = embeddingService.generateEmbedding(entity.fact)
                    result.getOrNull()
                } else {
                    embedding
                }
                
                if (finalEmbedding != null) {
                    entity.toDomain() to finalEmbedding
                } else {
                    null
                }
            }
            
            // Use semantic search to find similar memories
            val results = SemanticSearchUtil.findSimilar(
                query = query,
                queryEmbedding = queryEmbedding,
                items = memoriesWithEmbeddings,
                getText = { it.first.fact },
                getEmbedding = { it.second },
                k = limit
            )
            
            android.util.Log.d("MemoryRepository", "Found ${results.size} similar memories for query")
            
            return results.map { it.item.first }
        } catch (e: Exception) {
            android.util.Log.e("MemoryRepository", "Error finding similar memories", e)
            return emptyList()
        }
    }
    
    /**
     * Find similar memories for a specific Persona
     */
    suspend fun findSimilarMemoriesByPersona(
        query: String, 
        personaId: String, 
        limit: Int = 5, 
        minAgeDays: Int = 0
    ): List<MemoryEntry> {
        try {
            android.util.Log.d("MemoryRepository", "findSimilarMemoriesByPersona: personaId=$personaId")
            
            // Get memories for this persona
            val personaMemories = memoryDao.getMemoriesByPersonaId(personaId).first()
            android.util.Log.d("MemoryRepository", "Found ${personaMemories.size} memories for personaId=$personaId")
            
            if (personaMemories.isEmpty()) return emptyList()
            
            // Filter by age
            val currentTimeMillis = System.currentTimeMillis()
            val minAgeMillis = minAgeDays * 24 * 60 * 60 * 1000L
            val filteredEntities = if (minAgeDays > 0) {
                personaMemories.filter { entity ->
                    (currentTimeMillis - entity.createdAt) >= minAgeMillis
                }
            } else {
                personaMemories
            }
            
            if (filteredEntities.isEmpty()) return emptyList()
            
            // Same semantic search logic as findSimilarMemories
            val queryEmbeddingResult = embeddingService.generateEmbedding(query)
            if (queryEmbeddingResult.isFailure) return emptyList()
            val queryEmbedding = queryEmbeddingResult.getOrNull() ?: return emptyList()
            
            val memoriesWithEmbeddings = filteredEntities.mapNotNull { entity ->
                val embedding = entity.embedding?.let { embStr ->
                    embStr.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
                }
                
                val finalEmbedding = if (embedding == null) {
                    embeddingService.generateEmbedding(entity.fact).getOrNull()
                } else {
                    embedding
                }
                
                if (finalEmbedding != null) {
                    entity.toDomain() to finalEmbedding
                } else {
                    null
                }
            }
            
            val results = SemanticSearchUtil.findSimilar(
                query = query,
                queryEmbedding = queryEmbedding,
                items = memoriesWithEmbeddings,
                getText = { it.first.fact },
                getEmbedding = { it.second },
                k = limit
            )
            
            android.util.Log.d("MemoryRepository", "Found ${results.size} persona memories")
            return results.map { it.item.first }
        } catch (e: Exception) {
            android.util.Log.e("MemoryRepository", "Error finding persona memories", e)
            return emptyList()
        }
    }
    
    /**
     * Find similar global memories (not linked to any Persona)
     */
    suspend fun findSimilarGlobalMemories(
        query: String, 
        limit: Int = 5, 
        minAgeDays: Int = 0
    ): List<MemoryEntry> {
        try {
            // Get global memories (personaId = null)
            val globalMemories = memoryDao.getGlobalMemories().first()
            if (globalMemories.isEmpty()) return emptyList()
            
            // Filter by age
            val currentTimeMillis = System.currentTimeMillis()
            val minAgeMillis = minAgeDays * 24 * 60 * 60 * 1000L
            val filteredEntities = if (minAgeDays > 0) {
                globalMemories.filter { entity ->
                    (currentTimeMillis - entity.createdAt) >= minAgeMillis
                }
            } else {
                globalMemories
            }
            
            if (filteredEntities.isEmpty()) return emptyList()
            
            // Same semantic search logic as findSimilarMemories
            val queryEmbeddingResult = embeddingService.generateEmbedding(query)
            if (queryEmbeddingResult.isFailure) return emptyList()
            val queryEmbedding = queryEmbeddingResult.getOrNull() ?: return emptyList()
            
            val memoriesWithEmbeddings = filteredEntities.mapNotNull { entity ->
                val embedding = entity.embedding?.let { embStr ->
                    embStr.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
                }
                
                val finalEmbedding = if (embedding == null) {
                    embeddingService.generateEmbedding(entity.fact).getOrNull()
                } else {
                    embedding
                }
                
                if (finalEmbedding != null) {
                    entity.toDomain() to finalEmbedding
                } else {
                    null
                }
            }
            
            val results = SemanticSearchUtil.findSimilar(
                query = query,
                queryEmbedding = queryEmbedding,
                items = memoriesWithEmbeddings,
                getText = { it.first.fact },
                getEmbedding = { it.second },
                k = limit
            )
            
            android.util.Log.d("MemoryRepository", "Found ${results.size} global memories")
            return results.map { it.item.first }
        } catch (e: Exception) {
            android.util.Log.e("MemoryRepository", "Error finding global memories", e)
            return emptyList()
        }
    }
    
    /**
     * Recalculate embeddings for all memories
     * Use this when switching embedding models
     * @param onProgress callback with (current, total, percentage) progress
     */
    suspend fun recalculateAllEmbeddings(
        onProgress: (current: Int, total: Int, percentage: Float) -> Unit = { _, _, _ -> }
    ): Result<Int> {
        return try {
            _processingStatus.value = MemoryProcessingStatus.Idle
            
            val allEntities = memoryDao.getAllMemories().first()
            val totalCount = allEntities.size
            var processedCount = 0
            
            if (totalCount == 0) {
                _processingStatus.value = MemoryProcessingStatus.Completed
                return Result.success(0)
            }
            
            allEntities.forEachIndexed { index, entity ->
                val progress = ((index + 1) * 100) / totalCount
                _processingStatus.value = MemoryProcessingStatus.Recalculating(
                    progress = progress,
                    current = index + 1,
                    total = totalCount,
                    currentStep = "Processing memory ${index + 1} of $totalCount"
                )
                
                val embeddingResult = embeddingService.generateEmbedding(entity.fact)
                if (embeddingResult.isSuccess) {
                    val embeddingString = embeddingResult.getOrNull()?.joinToString(",")
                    memoryDao.updateMemory(
                        entity.copy(embedding = embeddingString)
                    )
                    processedCount++
                }
                
                // Update progress callback
                val percentage = if (totalCount > 0) (index + 1).toFloat() / totalCount else 0f
                onProgress(index + 1, totalCount, percentage)
            }
            
            _processingStatus.value = MemoryProcessingStatus.Completed
            android.util.Log.i("MemoryRepository", "Recalculated embeddings for $processedCount memories")
            
            // Reset to idle after a short delay
            kotlinx.coroutines.delay(2000)
            _processingStatus.value = MemoryProcessingStatus.Idle
            
            Result.success(processedCount)
        } catch (e: Exception) {
            _processingStatus.value = MemoryProcessingStatus.Failed(
                error = e.message ?: "Unknown error"
            )
            android.util.Log.e("MemoryRepository", "Error recalculating memory embeddings", e)
            
            // Reset to idle after showing error
            kotlinx.coroutines.delay(3000)
            _processingStatus.value = MemoryProcessingStatus.Idle
            
            Result.failure(e)
        }
    }
    
    /**
     * Upsert memory (for cloud sync)
     * Preserves existing embedding if present, generates only if missing
     */
    suspend fun upsertMemory(memory: MemoryEntity): Unit = withContext(Dispatchers.IO) {
        // Check if this memory already exists in DB
        val existingMemory = memoryDao.getMemoryById(memory.id)
        
        // Determine final embedding:
        // 1. If new memory has embedding -> use it (shouldn't happen with cloud sync, but just in case)
        // 2. If existing memory has embedding AND fact hasn't changed -> preserve it
        // 3. If existing memory has embedding BUT fact changed -> regenerate
        // 4. If neither has embedding -> generate new one
        val factChanged = existingMemory != null && existingMemory.fact != memory.fact
        
        val embeddingString = when {
            !memory.embedding.isNullOrBlank() -> {
                // New memory already has embedding
                android.util.Log.d("MemoryRepository", "Using provided embedding for memory ${memory.id}")
                memory.embedding
            }
            existingMemory != null && !existingMemory.embedding.isNullOrBlank() && !factChanged -> {
                // Preserve existing embedding (fact unchanged - DON'T regenerate!)
                android.util.Log.d("MemoryRepository", "Preserving existing embedding for memory ${memory.id}")
                existingMemory.embedding
            }
            else -> {
                // Generate new embedding (truly missing or fact changed)
                val reason = when {
                    existingMemory == null -> "new memory"
                    factChanged -> "fact changed"
                    else -> "embedding missing"
                }
                android.util.Log.d("MemoryRepository", "Generating embedding for memory ${memory.id} (reason: $reason)")
                val embeddingResult = embeddingService.generateEmbedding(memory.fact)
                if (embeddingResult.isSuccess) {
                    embeddingResult.getOrNull()?.joinToString(",")
                } else {
                    android.util.Log.w("MemoryRepository", "Failed to generate embedding for memory ${memory.id}")
                    null
                }
            }
        }
        
        memoryDao.insertMemory(memory.copy(embedding = embeddingString))
    }
    
    /**
     * Upsert multiple memories (for cloud sync)
     * Preserves existing embeddings if present, generates only if missing
     */
    suspend fun upsertMemories(memories: List<MemoryEntity>): Unit = withContext(Dispatchers.IO) {
        memories.forEach { memory ->
            // Check if this memory already exists in DB
            val existingMemory = memoryDao.getMemoryById(memory.id)
            
            // Determine final embedding (same logic as upsertMemory)
            val factChanged = existingMemory != null && existingMemory.fact != memory.fact
            
            val embeddingString = when {
                !memory.embedding.isNullOrBlank() -> {
                    memory.embedding
                }
                existingMemory != null && !existingMemory.embedding.isNullOrBlank() && !factChanged -> {
                    // Preserve existing embedding (fact unchanged)
                    android.util.Log.d("MemoryRepository", "Preserving existing embedding for memory ${memory.id}")
                    existingMemory.embedding
                }
                else -> {
                    // Generate new embedding (truly missing or fact changed)
                    val reason = when {
                        existingMemory == null -> "new memory"
                        factChanged -> "fact changed"
                        else -> "embedding missing"
                    }
                    android.util.Log.d("MemoryRepository", "Generating embedding for memory ${memory.id} (reason: $reason)")
                    val embeddingResult = embeddingService.generateEmbedding(memory.fact)
                    if (embeddingResult.isSuccess) {
                        embeddingResult.getOrNull()?.joinToString(",")
                    } else {
                        android.util.Log.w("MemoryRepository", "Failed to generate embedding for memory ${memory.id}")
                        null
                    }
                }
            }
            
            memoryDao.insertMemory(memory.copy(embedding = embeddingString))
        }
    }
}
