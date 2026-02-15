package com.yourown.ai.data.repository

import com.yourown.ai.data.local.entity.MemoryEntity
import com.yourown.ai.data.local.entity.toDomain
import com.yourown.ai.data.util.SemanticSearchUtil
import com.yourown.ai.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Service for clustering memories based on semantic similarity
 * Simplified version without sklearn - uses hierarchical clustering with cosine similarity
 */
@Singleton
class MemoryClusteringService @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    
    private val _clusteringStatus = MutableStateFlow<ClusteringStatus>(ClusteringStatus.Idle)
    val clusteringStatus: StateFlow<ClusteringStatus> = _clusteringStatus.asStateFlow()
    
    /**
     * Reset clustering status to Idle
     * Called when dialog is closed or new analysis starts
     */
    fun resetStatus() {
        _clusteringStatus.value = ClusteringStatus.Idle
    }
    
    /**
     * Cluster all memories for user
     * @param targetClusterSize Target size range for clusters (min, max)
     * @param similarityThreshold Threshold for grouping memories (0-1, higher = more strict)
     *        Note: With hybrid approach (embeddings + keywords), can use slightly lower threshold
     */
    suspend fun clusterMemories(
        targetClusterSize: Pair<Int, Int> = 5 to 10,
        similarityThreshold: Float = 0.60f // Slightly lower due to hybrid approach
    ): Result<ClusteringResult> = withContext(Dispatchers.Default) {
        try {
            android.util.Log.d("MemoryClustering", "=== Starting clustering ===")
            _clusteringStatus.value = ClusteringStatus.Processing(5, "Loading memories...")
            
            // 1. Load all memories with embeddings
            val memoryEntities = memoryRepository.getAllMemoryEntities()
            android.util.Log.d("MemoryClustering", "Loaded ${memoryEntities.size} memory entities")
            if (memoryEntities.isEmpty()) {
                return@withContext Result.failure(Exception("No memories found"))
            }
            
            // Check if any memories are missing embeddings
            val memoriesWithoutEmbeddings = memoryEntities.filter { it.embedding.isNullOrBlank() }
            if (memoriesWithoutEmbeddings.isNotEmpty()) {
                android.util.Log.w("MemoryClustering", "${memoriesWithoutEmbeddings.size} memories missing embeddings - need to recalculate first!")
                _clusteringStatus.value = ClusteringStatus.Failed(
                    "Missing embeddings for ${memoriesWithoutEmbeddings.size} memories. Please regenerate embeddings in Settings → Memory Management."
                )
                return@withContext Result.failure(Exception("Missing embeddings. Please recalculate embeddings first."))
            }
            
            _clusteringStatus.value = ClusteringStatus.Processing(20, "Processing ${memoryEntities.size} memories...")
            
            // 2. Convert to MemoryWithAge and filter out those without embeddings
            val memoriesWithData = memoryEntities.mapNotNull { entity ->
                val embedding = parseEmbedding(entity.embedding) ?: run {
                    android.util.Log.w("MemoryClustering", "Memory ${entity.id} has no embedding")
                    return@mapNotNull null
                }
                android.util.Log.d("MemoryClustering", "Memory ${entity.id} embedding size: ${embedding.size}")
                val ageDays = calculateAgeDays(entity.createdAt)
                
                MemoryWithAge(
                    memory = entity.toDomain(),
                    ageDays = ageDays,
                    embedding = embedding
                )
            }
            
            android.util.Log.d("MemoryClustering", "Memories with embeddings: ${memoriesWithData.size}")
            if (memoriesWithData.isEmpty()) {
                return@withContext Result.failure(Exception("No memories with embeddings found"))
            }
            
            _clusteringStatus.value = ClusteringStatus.Processing(30, "Stage 1: Finding main themes...")
            
            // 3. Stage 1: Coarse clustering - find main themes
            android.util.Log.d("MemoryClustering", "Starting coarse clustering...")
            val coarseLabels = performCoarseClustering(memoriesWithData, similarityThreshold)
            android.util.Log.d("MemoryClustering", "Coarse clustering complete. Labels: ${coarseLabels.toList()}")
            
            _clusteringStatus.value = ClusteringStatus.Processing(60, "Stage 2: Refining clusters...")
            
            // 4. Stage 2: Refine clusters to target size
            android.util.Log.d("MemoryClustering", "Refining clusters...")
            val (finalClusters, outliers) = refineClusters(
                memoriesWithData,
                coarseLabels,
                targetClusterSize
            )
            android.util.Log.d("MemoryClustering", "Refinement complete. Clusters: ${finalClusters.size}, Outliers: ${outliers?.size ?: 0}")
            
            _clusteringStatus.value = ClusteringStatus.Processing(90, "Calculating metrics...")
            
            // 5. Build result
            val result = ClusteringResult(
                clusters = finalClusters,
                outliers = outliers,
                totalMemories = memoriesWithData.size
            )
            
            android.util.Log.d("MemoryClustering", "=== Clustering complete! ===")
            _clusteringStatus.value = ClusteringStatus.Completed(result)
            
            // Don't auto-reset - let ViewModel handle it when dialog closes
            Result.success(result)
        } catch (e: Exception) {
            android.util.Log.e("MemoryClustering", "Error clustering memories", e)
            _clusteringStatus.value = ClusteringStatus.Failed(e.message ?: "Unknown error")
            
            // Don't auto-reset - let user retry or close dialog
            Result.failure(e)
        }
    }
    
    /**
     * Stage 1: Coarse clustering using simple threshold-based grouping
     * Returns cluster labels for each memory
     */
    private fun performCoarseClustering(
        memories: List<MemoryWithAge>,
        threshold: Float
    ): IntArray {
        val n = memories.size
        val labels = IntArray(n) { -1 } // -1 means not assigned
        var currentCluster = 0
        
        android.util.Log.d("MemoryClustering", "performCoarseClustering: ${n} memories, threshold=${threshold}")
        
        for (i in 0 until n) {
            if (labels[i] != -1) continue // Already assigned
            
            // Start new cluster
            labels[i] = currentCluster
            val clusterMembers = mutableListOf(i)
            
            android.util.Log.d("MemoryClustering", "  Starting cluster $currentCluster with memory $i")
            
            // Find all similar memories for this cluster
            for (j in (i + 1) until n) {
                if (labels[j] != -1) continue
                
                // Check similarity with cluster using hybrid approach
                val similarity = calculateClusterSimilarity(
                    embedding = memories[j].embedding,
                    clusterEmbeddings = clusterMembers.map { memories[it].embedding },
                    memoryText = memories[j].memory.fact,
                    clusterTexts = clusterMembers.map { memories[it].memory.fact }
                )
                
                if (similarity >= threshold) {
                    labels[j] = currentCluster
                    clusterMembers.add(j)
                    android.util.Log.d("MemoryClustering", "    Added memory $j (similarity=${similarity})")
                }
            }
            
            android.util.Log.d("MemoryClustering", "  Cluster $currentCluster complete with ${clusterMembers.size} members")
            currentCluster++
        }
        
        android.util.Log.d("MemoryClustering", "performCoarseClustering complete: ${currentCluster} clusters")
        return labels
    }
    
    /**
     * Calculate similarity between a memory and a cluster
     * Uses hybrid approach: embedding similarity + keyword overlap
     */
    private fun calculateClusterSimilarity(
        embedding: FloatArray,
        clusterEmbeddings: List<FloatArray>,
        memoryText: String? = null,
        clusterTexts: List<String>? = null
    ): Float {
        if (clusterEmbeddings.isEmpty()) return 0f
        
        // 1. Embedding similarity (main component)
        val embeddingSimilarities = clusterEmbeddings.map { clusterEmb ->
            SemanticSearchUtil.cosineSimilarity(embedding, clusterEmb)
        }
        val avgEmbeddingSimilarity = embeddingSimilarities.average().toFloat()
        
        // 2. Keyword overlap boost (if texts provided)
        var keywordBoost = 0f
        if (memoryText != null && !clusterTexts.isNullOrEmpty()) {
            val memoryTokens = tokenizeMemory(memoryText)
            
            // Calculate overlap with each cluster member
            val overlapScores = clusterTexts.map { clusterText ->
                val clusterTokens = tokenizeMemory(clusterText)
                calculateTokenOverlap(memoryTokens, clusterTokens)
            }
            
            // Average overlap * weight
            keywordBoost = overlapScores.average().toFloat() * 0.2f // 20% weight
        }
        
        // Combine: 80% embeddings + 20% keywords
        return (avgEmbeddingSimilarity + keywordBoost).coerceIn(0f, 1f)
    }
    
    /**
     * Tokenize memory text using SemanticSearchUtil (removes stop-words)
     */
    private fun tokenizeMemory(text: String): Set<String> {
        val normalized = text.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
        
        return normalized
            .split(Regex("[\\s,;.!?()\\[\\]{}\"']+"))
            .filter { token ->
                token.isNotBlank() && 
                token.length > 3 && // Longer than 3 characters
                !isStopWord(token.lowercase())
            }
            .map { it.lowercase() }
            .toSet()
    }
    
    /**
     * Check if word is a stop-word (common Russian/Ukrainian/English words)
     */
    private fun isStopWord(word: String): Boolean {
        val stopWords = setOf(
            // Russian
            "а", "и", "в", "на", "с", "у", "к", "о", "из", "за", "по", "от", "до",
            "что", "как", "это", "так", "ты", "я", "мы", "он", "она", "они", "вы",
            "не", "да", "но", "же", "ли", "бы", "то", "ещё", "еще", "уже", "вот",
            "все", "всё", "мне", "меня", "тебе", "тебя", "нам", "нас", "мой", "твой",
            "если", "когда", "чтобы", "потому", "очень", "только", "просто", "прям",
            "какие", "какой", "какая", "какое", "который", "которая", "которое",
            "хочешь", "хочу", "могу", "можешь", "буду", "будет", "есть", "был", "была",
            "опять", "снова", "теперь", "сейчас", "тоже", "также", "быть", "этот",
            "эти", "этим", "этих", "того", "тому", "том", "без", "для", "про", "при",
            
            // Ukrainian
            "а", "і", "в", "на", "з", "у", "до", "від", "за", "по", "для", "про", "при",
            "що", "як", "це", "так", "ти", "я", "ми", "він", "вона", "вони", "ви",
            "не", "та", "але", "ж", "чи", "би", "те", "ще", "вже", "ось",
            "все", "всі", "мені", "мене", "тобі", "тебе", "нам", "нас", "мій", "твій",
            "якщо", "коли", "щоб", "тому", "дуже", "тільки", "просто",
            "які", "який", "яка", "яке", "котрий", "котра", "котре",
            "хочеш", "хочу", "можу", "можеш", "буду", "буде", "є", "був", "була",
            "знову", "тепер", "зараз", "теж", "також", "бути", "цей", "ці", "цим", "цих",
            "того", "тому", "тим", "без", "або",
            
            // English
            "the", "is", "at", "which", "on", "a", "an", "and", "or", "but",
            "in", "with", "to", "for", "of", "as", "by", "that", "this", "these",
            "it", "be", "are", "was", "were", "been", "have", "has", "had",
            "do", "does", "did", "will", "would", "could", "should", "may", "might",
            "i", "you", "he", "she", "we", "they", "me", "him", "her", "us", "them",
            "my", "your", "his", "her", "its", "our", "their",
            "not", "no", "yes", "can", "from", "up", "down", "out", "about",
            "into", "through", "over", "under", "again", "then", "once",
            "here", "there", "when", "where", "why", "how", "all", "each",
            "some", "such", "only", "own", "same", "so", "than", "too", "very",
            "just", "now", "also", "more", "most", "much", "any", "both"
        )
        return word in stopWords
    }
    
    /**
     * Calculate token overlap between two sets
     * Returns Jaccard similarity (intersection / union)
     */
    private fun calculateTokenOverlap(tokens1: Set<String>, tokens2: Set<String>): Float {
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0f
        
        val intersection = tokens1.intersect(tokens2).size.toFloat()
        val union = tokens1.union(tokens2).size.toFloat()
        
        return if (union > 0) intersection / union else 0f
    }
    
    /**
     * Stage 2: Refine clusters to target size
     */
    private fun refineClusters(
        memories: List<MemoryWithAge>,
        labels: IntArray,
        targetSize: Pair<Int, Int>
    ): Pair<List<MemoryCluster>, MemoryCluster?> {
        val (minSize, maxSize) = targetSize
        val clusters = mutableListOf<MemoryCluster>()
        val outlierIndices = mutableListOf<Int>()
        
        // Group by cluster label
        val clusterGroups = memories.indices.groupBy { labels[it] }
        
        var clusterId = 0
        for ((_, indices) in clusterGroups) {
            val clusterMemories = indices.map { memories[it] }
            val size = clusterMemories.size
            
            when {
                // Perfect size - keep as is
                size in minSize..maxSize -> {
                    clusters.add(createCluster(clusterId++, clusterMemories))
                }
                // Too small - treat as outliers if size == 1, otherwise keep
                size < minSize -> {
                    if (size == 1) {
                        outlierIndices.addAll(indices)
                    } else {
                        clusters.add(createCluster(clusterId++, clusterMemories))
                    }
                }
                // Too large - split into subclusters
                else -> {
                    val subclusters = splitLargeCluster(clusterMemories, maxSize)
                    subclusters.forEach { subcluster ->
                        if (subcluster.size >= minSize) {
                            clusters.add(createCluster(clusterId++, subcluster))
                        } else {
                            outlierIndices.addAll(subcluster.map { memories.indexOf(it) })
                        }
                    }
                }
            }
        }
        
        // Create outliers cluster
        val outliersCluster = if (outlierIndices.isNotEmpty()) {
            val outlierMemories = outlierIndices.map { memories[it] }
            createCluster(-1, outlierMemories)
        } else {
            null
        }
        
        return clusters to outliersCluster
    }
    
    /**
     * Split large cluster into smaller subclusters
     */
    private fun splitLargeCluster(
        memories: List<MemoryWithAge>,
        maxSize: Int
    ): List<List<MemoryWithAge>> {
        val numSubclusters = (memories.size + maxSize - 1) / maxSize // Ceiling division
        val subclusters = List(numSubclusters) { mutableListOf<MemoryWithAge>() }
        
        // Simple distribution - can be improved with better algorithm
        memories.forEachIndexed { index, memory ->
            subclusters[index % numSubclusters].add(memory)
        }
        
        return subclusters
    }
    
    /**
     * Create cluster object with calculated metrics
     */
    private fun createCluster(id: Int, memories: List<MemoryWithAge>): MemoryCluster {
        android.util.Log.d("MemoryClustering", "createCluster: id=$id, size=${memories.size}")
        val embeddings = memories.map { it.embedding }
        
        // Calculate centroid (average embedding)
        val centroid = calculateCentroid(embeddings)
        android.util.Log.d("MemoryClustering", "  Calculated centroid")
        
        // Density: average similarity to centroid
        val similarities = embeddings.map { emb ->
            SemanticSearchUtil.cosineSimilarity(emb, centroid)
        }
        val density = similarities.average().toFloat()
        android.util.Log.d("MemoryClustering", "  Calculated density: $density")
        
        // Age metrics
        val avgAgeDays = memories.map { it.ageDays }.average().toInt()
        
        // Diversity: 1 - average pairwise similarity
        android.util.Log.d("MemoryClustering", "  Calculating diversity (${embeddings.size} embeddings)...")
        val diversity = if (memories.size > 1) {
            val pairwiseSimilarities = mutableListOf<Float>()
            for (i in embeddings.indices) {
                for (j in (i + 1) until embeddings.size) {
                    val sim = SemanticSearchUtil.cosineSimilarity(embeddings[i], embeddings[j])
                    pairwiseSimilarities.add(sim)
                }
            }
            android.util.Log.d("MemoryClustering", "  Calculated ${pairwiseSimilarities.size} pairwise similarities")
            1.0f - pairwiseSimilarities.average().toFloat()
        } else {
            0f
        }
        android.util.Log.d("MemoryClustering", "  Diversity: $diversity")
        
        // Priority score calculation
        val priorityScore = calculatePriority(density, avgAgeDays, diversity)
        
        android.util.Log.d("MemoryClustering", "createCluster complete: id=$id")
        return MemoryCluster(
            id = id,
            memories = memories,
            density = density,
            avgAgeDays = avgAgeDays,
            diversity = diversity,
            priorityScore = priorityScore
        )
    }
    
    /**
     * Calculate centroid (average) of embeddings
     */
    private fun calculateCentroid(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) return floatArrayOf()
        
        val dim = embeddings.first().size
        val centroid = FloatArray(dim)
        
        for (emb in embeddings) {
            for (i in emb.indices) {
                centroid[i] += emb[i]
            }
        }
        
        for (i in centroid.indices) {
            centroid[i] /= embeddings.size
        }
        
        return centroid
    }
    
    /**
     * Calculate priority score for cluster
     * Higher score = more important to review
     * - Old + sparse = candidates for deletion
     * - Young + dense = check for duplicates
     */
    private fun calculatePriority(density: Float, avgAgeDays: Int, diversity: Float): Float {
        val ageFactor = min(avgAgeDays / 90f, 1f) // Normalize age to 0-1 (max 90 days)
        val sparsity = 1f - density
        
        // High priority if:
        // 1. Old and sparse (possibly garbage)
        // 2. Young and dense (possibly duplicates)
        val priority = (ageFactor * sparsity * 0.6f) + ((1f - ageFactor) * density * 0.4f)
        
        return priority.coerceIn(0f, 1f)
    }
    
    /**
     * Parse embedding from comma-separated string
     */
    private fun parseEmbedding(embeddingString: String?): FloatArray? {
        if (embeddingString.isNullOrBlank()) return null
        
        return try {
            embeddingString.split(",")
                .mapNotNull { it.toFloatOrNull() }
                .toFloatArray()
                .takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate age in days from timestamp
     */
    private fun calculateAgeDays(createdAt: Long): Int {
        val now = System.currentTimeMillis()
        val diffMillis = now - createdAt
        return (diffMillis / (24 * 60 * 60 * 1000)).toInt()
    }
}
