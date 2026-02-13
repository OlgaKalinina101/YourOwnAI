package com.yourown.ai.data.repository

import com.yourown.ai.data.local.dao.BiographyChunkDao
import com.yourown.ai.data.local.dao.BiographyDao
import com.yourown.ai.data.local.entity.BiographyChunkEntity
import com.yourown.ai.data.local.entity.BiographyEntity
import com.yourown.ai.data.util.SemanticSearchUtil
import com.yourown.ai.domain.model.UserBiography
import com.yourown.ai.domain.service.EmbeddingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Biography chunk for semantic search
 */
data class BiographyChunk(
    val id: String,
    val section: String,
    val text: String,
    val embedding: FloatArray
)

/**
 * Repository for managing user biography and its semantic chunks
 */
@Singleton
class BiographyRepository @Inject constructor(
    private val biographyDao: BiographyDao,
    private val biographyChunkDao: BiographyChunkDao,
    private val embeddingService: EmbeddingService
) {
    
    /**
     * Observe biography changes
     */
    fun getBiography(): Flow<UserBiography?> {
        return biographyDao.getBiography().map { entity ->
            entity?.toDomain()
        }
    }
    
    /**
     * Get biography synchronously
     */
    suspend fun getBiographySync(): UserBiography? = withContext(Dispatchers.IO) {
        biographyDao.getBiographySync()?.toDomain()
    }
    
    /**
     * Save biography
     */
    suspend fun saveBiography(biography: UserBiography): Unit = withContext(Dispatchers.IO) {
        biographyDao.insertBiography(biography.toEntity())
        
        // NOTE: Biography chunks generation is disabled to save API costs
        // Uncomment if you need semantic search over biography:
        // generateBiographyChunks(biography)
    }
    
    /**
     * Delete biography
     */
    suspend fun deleteBiography(): Unit = withContext(Dispatchers.IO) {
        biographyDao.deleteBiography()
        biographyChunkDao.deleteAllChunks()
    }
    
    /**
     * Generate biography chunks with embeddings from biography sections
     * Splits each section by sentences and creates embeddings for semantic search
     */
    private suspend fun generateBiographyChunks(biography: UserBiography) = withContext(Dispatchers.IO) {
        // Delete existing chunks
        biographyChunkDao.deleteAllChunks()
        
        val chunks = mutableListOf<BiographyChunkEntity>()
        var chunkIndex = 0
        
        // Process each section
        val sections = mapOf(
            "values" to biography.values,
            "profile" to biography.profile,
            "painPoints" to biography.painPoints,
            "joys" to biography.joys,
            "fears" to biography.fears,
            "loves" to biography.loves,
            "currentSituation" to biography.currentSituation
        )
        
        sections.forEach { (sectionName, sectionText) ->
            if (sectionText.isNotBlank()) {
                // Split by sentences using shared utility
                val sentences = SemanticSearchUtil.splitTextToSentences(
                    text = sectionText,
                    minLength = 0 // No minimum - keep all sentences for biography
                )
                
                sentences.forEach { sentence ->
                    // Generate embedding for this sentence
                    val embeddingResult = embeddingService.generateEmbedding(sentence)
                    val embedding = embeddingResult.getOrNull() ?: return@withContext
                    
                    // Convert FloatArray to JSONArray manually
                    val embeddingJson = JSONArray().apply {
                        embedding.forEach { value -> put(value.toDouble()) }
                    }.toString()
                    
                    val chunk = BiographyChunkEntity(
                        id = "bio_chunk_${sectionName}_${chunkIndex++}",
                        biographyId = "default",
                        section = sectionName,
                        text = sentence,
                        embedding = embeddingJson,
                        createdAt = System.currentTimeMillis()
                    )
                    chunks.add(chunk)
                }
            }
        }
        
        // Save all chunks at once
        if (chunks.isNotEmpty()) {
            biographyChunkDao.insertChunks(chunks)
            android.util.Log.d("BiographyRepository", "Generated ${chunks.size} biography chunks")
        }
    }
    
    /**
     * Find similar biography chunks using semantic search
     * @param query User message to search for
     * @param limit Maximum number of chunks to return
     * @return List of relevant biography chunks sorted by relevance
     */
    suspend fun findSimilarBiographyChunks(
        query: String,
        limit: Int = 3
    ): List<BiographyChunk> = withContext(Dispatchers.IO) {
        // Get all chunks
        val chunkEntities = biographyChunkDao.getAllChunks()
        if (chunkEntities.isEmpty()) {
            return@withContext emptyList()
        }
        
        // Generate embedding for query
        val queryEmbeddingResult = embeddingService.generateEmbedding(query)
        val queryEmbedding = queryEmbeddingResult.getOrNull() ?: return@withContext emptyList()
        
        // Convert entities to domain chunks
        val chunks = chunkEntities.mapNotNull { entity ->
            try {
                val embeddingArray = JSONArray(entity.embedding)
                val embedding = FloatArray(embeddingArray.length()) { i ->
                    embeddingArray.getDouble(i).toFloat()
                }
                BiographyChunk(
                    id = entity.id,
                    section = entity.section,
                    text = entity.text,
                    embedding = embedding
                )
            } catch (e: Exception) {
                android.util.Log.e("BiographyRepository", "Failed to parse chunk embedding", e)
                null
            }
        }
        
        // Calculate cosine similarity for each chunk
        val scored = chunks.map { chunk ->
            val similarity = SemanticSearchUtil.cosineSimilarity(queryEmbedding, chunk.embedding)
            chunk to similarity
        }
        
        // Return top N by similarity
        scored.sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}

/**
 * Convert BiographyEntity to domain model
 */
fun BiographyEntity.toDomain(): UserBiography {
    return UserBiography(
        values = userValues,
        profile = profile,
        painPoints = painPoints,
        joys = joys,
        fears = fears,
        loves = loves,
        currentSituation = currentSituation,
        lastUpdated = lastUpdated,
        processedClusters = processedClusters
    )
}

/**
 * Convert UserBiography to entity
 */
fun UserBiography.toEntity(): BiographyEntity {
    return BiographyEntity(
        id = "default",
        userValues = values,
        profile = profile,
        painPoints = painPoints,
        joys = joys,
        fears = fears,
        loves = loves,
        currentSituation = currentSituation,
        lastUpdated = lastUpdated,
        processedClusters = processedClusters
    )
}
