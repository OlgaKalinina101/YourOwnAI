package com.yourown.ai.domain.model

/**
 * Status of memory cleaning process
 */
sealed class MemoryCleaningStatus {
    object Idle : MemoryCleaningStatus()
    data class Processing(
        val currentCluster: Int,
        val totalClusters: Int,
        val step: String
    ) : MemoryCleaningStatus()
    data class Completed(
        val totalMemories: Int,
        val removedMemories: Int,
        val mergedMemories: Int
    ) : MemoryCleaningStatus()
    data class Failed(val error: String) : MemoryCleaningStatus()
}
