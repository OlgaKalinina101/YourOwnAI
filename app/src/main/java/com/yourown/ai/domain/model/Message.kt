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
    val model: String,
    val provider: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val messages: List<Message> = emptyList()
)
