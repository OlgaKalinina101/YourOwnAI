package com.yourown.ai.presentation.settings.managers

import com.yourown.ai.data.repository.MemoryRepository
import com.yourown.ai.domain.model.MemoryEntry
import javax.inject.Inject

/**
 * Manager for Memory entries
 * Handles: CRUD operations for memories
 */
class MemoryManager @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    
    /**
     * Update existing memory
     */
    suspend fun updateMemory(memory: MemoryEntry, fact: String) {
        val updated = memory.copy(fact = fact)
        memoryRepository.updateMemory(updated)
    }
    
    /**
     * Delete memory
     */
    suspend fun deleteMemory(memory: MemoryEntry) {
        memoryRepository.deleteMemory(memory)
    }
}
