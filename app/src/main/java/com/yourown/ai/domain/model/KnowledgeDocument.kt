package com.yourown.ai.domain.model

data class KnowledgeDocument(
    val id: String,
    val name: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sizeBytes: Int = 0,
    val linkedPersonaIds: List<String> = emptyList()  // Список ID persona, к которым привязан документ
) {
    /**
     * Проверить, доступен ли документ для конкретной persona
     */
    fun isAvailableForPersona(personaId: String?): Boolean {
        // Если список пуст - документ доступен всем (legacy/global)
        if (linkedPersonaIds.isEmpty()) return true
        
        // Если personaId null (глобальные настройки) - доступны только документы без привязки
        if (personaId == null) return linkedPersonaIds.isEmpty()
        
        // Проверяем, есть ли personaId в списке
        return linkedPersonaIds.contains(personaId)
    }
    
    /**
     * Проверить, доступен ли документ для любой из persona
     */
    fun isAvailableForAnyPersona(personaIds: List<String>): Boolean {
        if (linkedPersonaIds.isEmpty()) return true
        return personaIds.any { linkedPersonaIds.contains(it) }
    }
}
