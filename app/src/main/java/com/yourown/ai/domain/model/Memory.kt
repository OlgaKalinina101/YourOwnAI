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
    val fact: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val personaId: String? = null  // ID persona, в рамках которой создано воспоминание
) {
    companion object {
        /**
         * Parse memory from AI response
         * Format: "Факт" or "Нет ключевой информации"
         */
        fun parseFromResponse(
            response: String,
            conversationId: String,
            messageId: String,
            personaId: String? = null
        ): MemoryEntry? {
            val trimmed = response.trim()
            
            // Check for empty response
            if (trimmed.isEmpty()) {
                return null
            }
            
            // Normalize: lowercase + remove punctuation
            val normalized = trimmed.lowercase()
                .replace(Regex("[.!?,:;…—–-]"), "")
                .trim()
            
            // Check for explicit "no key information" markers
            val noInfoMarkers = listOf(
                "нет ключевой информации",
                "нет ключевых данных",
                "ключевой информации нет",
                "no key information",
                "no key info",
                "нет информации",
                "недостаточно информации",
                "не содержит ключевой информации",
                "ключевая информация отсутствует"
            )
            
            // Check if normalized response matches any marker
            if (noInfoMarkers.any { marker -> 
                normalized == marker || normalized.contains(marker)
            }) {
                return null
            }
            
            return MemoryEntry(
                id = generateId(),
                conversationId = conversationId,
                messageId = messageId,
                fact = trimmed,
                personaId = personaId
            )
        }
        
        private fun generateId(): String {
            return "mem_${System.currentTimeMillis()}_${(0..999).random()}"
        }
    }
}

/**
 * Memory statistics
 */
data class MemoryStats(
    val totalMemories: Int
)
