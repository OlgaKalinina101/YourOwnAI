package com.yourown.ai.data.repository

import com.yourown.ai.data.local.dao.MessageDao
import com.yourown.ai.data.mapper.toDomain
import com.yourown.ai.data.mapper.toEntity
import com.yourown.ai.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {
    
    /**
     * Get messages for conversation
     */
    fun getMessagesByConversation(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesByConversation(conversationId)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    /**
     * Add message to conversation
     */
    suspend fun addMessage(message: Message): String {
        val id = message.id.ifEmpty { UUID.randomUUID().toString() }
        val messageWithId = message.copy(id = id)
        messageDao.insertMessage(messageWithId.toEntity())
        return id
    }
    
    /**
     * Add multiple messages (batch)
     */
    suspend fun addMessages(messages: List<Message>) {
        val entities = messages.map { it.toEntity() }
        messageDao.insertMessages(entities)
    }
    
    /**
     * Update message
     */
    suspend fun updateMessage(message: Message) {
        messageDao.insertMessage(message.toEntity())
    }
    
    /**
     * Toggle like on message
     */
    suspend fun toggleLike(messageId: String) {
        val message = messageDao.getMessageById(messageId)
        message?.let {
            messageDao.insertMessage(it.copy(isLiked = !it.isLiked))
        }
    }
    
    /**
     * Update message with swipe alternative
     */
    suspend fun updateSwipeAlternative(
        messageId: String,
        swipeMessageId: String,
        swipeMessageText: String
    ) {
        val message = messageDao.getMessageById(messageId)
        message?.let {
            messageDao.insertMessage(
                it.copy(
                    swipeMessageId = swipeMessageId,
                    swipeMessageText = swipeMessageText
                )
            )
        }
    }
    
    /**
     * Update message with request logs
     */
    suspend fun updateRequestLogs(messageId: String, logs: String) {
        val message = messageDao.getMessageById(messageId)
        message?.let {
            messageDao.insertMessage(it.copy(requestLogs = logs))
        }
    }
    
    /**
     * Delete message
     */
    suspend fun deleteMessage(messageId: String) {
        val message = messageDao.getMessageById(messageId)
        message?.let {
            messageDao.deleteMessage(it)
        }
    }
    
    /**
     * Delete all messages in conversation
     */
    suspend fun deleteMessagesByConversation(conversationId: String) {
        messageDao.deleteMessagesByConversation(conversationId)
    }
    
    /**
     * Get total tokens for conversation
     */
    suspend fun getTotalTokens(conversationId: String): Int {
        return messageDao.getTotalTokensByConversation(conversationId) ?: 0
    }
    
    /**
     * Get last N message pairs (user-assistant) from a conversation
     * Used for context inheritance when forking a conversation
     * 
     * @param conversationId ID of the conversation to get messages from
     * @param pairLimit Number of pairs to retrieve (each pair = user + assistant message)
     * @return List of messages (user-assistant pairs), sorted chronologically
     */
    suspend fun getLastMessagePairs(conversationId: String, pairLimit: Int): List<Message> {
        // Get all messages for this conversation, sorted by creation time
        val allMessages = messageDao.getMessagesByConversationSync(conversationId)
            .map { it.toDomain() }
            .sortedBy { it.createdAt }
        
        // Extract pairs (user followed by assistant)
        val pairs = mutableListOf<Pair<Message, Message>>()
        var i = 0
        while (i < allMessages.size - 1) {
            val current = allMessages[i]
            val next = allMessages[i + 1]
            
            // Check if this is a user-assistant pair
            if (current.role.toStringValue() == "user" && next.role.toStringValue() == "assistant") {
                pairs.add(Pair(current, next))
                i += 2  // Skip both messages
            } else {
                i += 1  // Move to next message
            }
        }
        
        // Take last N pairs and flatten to list
        return pairs.takeLast(pairLimit).flatMap { listOf(it.first, it.second) }
    }
}
