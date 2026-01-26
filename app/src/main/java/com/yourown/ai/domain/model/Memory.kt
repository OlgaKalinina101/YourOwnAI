package com.yourown.ai.domain.model

import com.yourown.ai.domain.prompt.AIPrompts

/**
 * Deep Empathy analysis result
 */
data class DialogueFocus(
    val focusPoints: List<String>,
    val isStrongFocus: List<Boolean>
) {
    /**
     * Get the strongest focus point (if any)
     */
    fun getStrongestFocus(): String? {
        val strongIndex = isStrongFocus.indexOfFirst { it }
        return if (strongIndex != -1 && focusPoints.size > strongIndex) {
            focusPoints[strongIndex]
        } else {
            null
        }
    }
    
    /**
     * Check if there are any focus points
     */
    fun hasFocus(): Boolean = focusPoints.isNotEmpty()
}

/**
 * Memory entry extracted from conversation
 */
data class MemoryEntry(
    val id: String,
    val conversationId: String,
    val messageId: String,
    val category: AIPrompts.Memory.MemoryCategory,
    val fact: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
) {
    /**
     * Format for display
     */
    fun toDisplayString(): String = "${category.displayName}: $fact"
    
    companion object {
        /**
         * Parse memory from AI response
         * Format: "Категория:Факт" or "Нет ключевой информации"
         */
        fun parseFromResponse(
            response: String,
            conversationId: String,
            messageId: String
        ): MemoryEntry? {
            val trimmed = response.trim()
            
            // Check for "no key info" response
            if (trimmed.equals("Нет ключевой информации", ignoreCase = true)) {
                return null
            }
            
            // Parse "Category:Fact" format
            val parts = trimmed.split(":", limit = 2)
            if (parts.size != 2) {
                return null
            }
            
            val categoryName = parts[0].trim()
            val fact = parts[1].trim()
            
            val category = AIPrompts.Memory.MemoryCategory.fromRussian(categoryName)
                ?: return null
            
            return MemoryEntry(
                id = generateId(),
                conversationId = conversationId,
                messageId = messageId,
                category = category,
                fact = fact
            )
        }
        
        private fun generateId(): String {
            return "mem_${System.currentTimeMillis()}_${(0..999).random()}"
        }
    }
}

/**
 * Memory statistics by category
 */
data class MemoryStats(
    val totalMemories: Int,
    val byCategory: Map<String, Int>
) {
    companion object {
        /**
         * Create from DAO result
         */
        fun fromCategoryCounts(counts: List<com.yourown.ai.data.local.dao.CategoryCount>): MemoryStats {
            return MemoryStats(
                totalMemories = counts.sumOf { it.count },
                byCategory = counts.associate { it.category to it.count }
            )
        }
    }
}
