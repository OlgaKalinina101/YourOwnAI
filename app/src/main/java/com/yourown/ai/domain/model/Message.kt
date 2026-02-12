package com.yourown.ai.domain.model

/**
 * Message in a conversation (domain model)
 */
data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val createdAt: Long,
    val tokenCount: Int? = null,
    val model: String? = null,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val isLiked: Boolean = false,
    val swipeMessageId: String? = null,
    val swipeMessageText: String? = null,
    
    // Attachments (stored as JSON array of paths/URIs)
    val imageAttachments: String? = null, // JSON array: ["path1", "path2"]
    val fileAttachments: String? = null, // JSON array: [{"path":"path1","name":"file.pdf","type":"pdf"}]
    
    // Settings snapshot
    val temperature: Float? = null,
    val topP: Float? = null,
    val deepEmpathy: Boolean = false,
    val memoryEnabled: Boolean = true,
    val messageHistoryLimit: Int? = null,
    val systemPrompt: String? = null,
    
    // Request logs
    val requestLogs: String? = null
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM;
    
    fun toStringValue(): String = name.lowercase()
    
    companion object {
        fun fromString(value: String): MessageRole {
            return when (value.lowercase()) {
                "user" -> USER
                "assistant" -> ASSISTANT
                "system" -> SYSTEM
                else -> USER
            }
        }
    }
}

/**
 * Conversation with messages
 */
data class Conversation(
    val id: String,
    val title: String,
    val systemPrompt: String,
    val systemPromptId: String? = null,
    val personaId: String? = null,  // ID активной Persona
    val model: String,
    val provider: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val sourceConversationId: String? = null,  // ID чата-источника для наследования контекста
    val webSearchEnabled: Boolean = false,  // Включен ли web search (OpenRouter :online)
    val xSearchEnabled: Boolean = false,  // Включен ли X search (только xAI/Grok)
    val messages: List<Message> = emptyList()
)
