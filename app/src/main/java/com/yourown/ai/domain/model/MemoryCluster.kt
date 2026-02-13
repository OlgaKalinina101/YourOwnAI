package com.yourown.ai.domain.model

import kotlin.math.min

/**
 * Memory cluster with metadata and metrics
 */
data class MemoryCluster(
    val id: Int,
    val memories: List<MemoryWithAge>,
    val density: Float,           // Насколько близки воспоминания (0-1)
    val avgAgeDays: Int,          // Средний возраст воспоминаний
    val diversity: Float,         // Насколько разные воспоминания (0-1)
    val priorityScore: Float      // Приоритет для проверки (0-1)
) {
    val size: Int get() = memories.size
    
    /**
     * Get age category emoji
     */
    fun getAgeEmoji(): String = when {
        avgAgeDays > 60 -> "🔴"
        avgAgeDays > 30 -> "🟡"
        else -> "🟢"
    }
    
    /**
     * Get priority category
     */
    fun getPriorityCategory(): String = when {
        priorityScore > 0.7f -> "High"
        priorityScore > 0.4f -> "Medium"
        else -> "Low"
    }
}

/**
 * Memory with additional age information
 */
data class MemoryWithAge(
    val memory: MemoryEntry,
    val ageDays: Int,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MemoryWithAge
        return memory == other.memory && ageDays == other.ageDays
    }

    override fun hashCode(): Int {
        var result = memory.hashCode()
        result = 31 * result + ageDays
        return result
    }
}

/**
 * Clustering result with main clusters and outliers
 */
data class ClusteringResult(
    val clusters: List<MemoryCluster>,
    val outliers: MemoryCluster?,
    val totalMemories: Int
) {
    /**
     * Get top priority clusters
     */
    fun getTopPriorityClusters(limit: Int = 5): List<MemoryCluster> {
        return clusters.sortedByDescending { it.priorityScore }.take(limit)
    }
    
    /**
     * Get clusters by age (oldest first)
     */
    fun getOldestClusters(limit: Int = 5): List<MemoryCluster> {
        return clusters.sortedByDescending { it.avgAgeDays }.take(limit)
    }
    
    /**
     * Get average cluster size
     */
    fun getAverageClusterSize(): Float {
        return if (clusters.isEmpty()) 0f else clusters.map { it.size }.average().toFloat()
    }
}

/**
 * Clustering status for UI feedback
 */
sealed class ClusteringStatus {
    object Idle : ClusteringStatus()
    data class Processing(val progress: Int, val step: String) : ClusteringStatus()
    data class Completed(val result: ClusteringResult) : ClusteringStatus()
    data class Failed(val error: String) : ClusteringStatus()
}
