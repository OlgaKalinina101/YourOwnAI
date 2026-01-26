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
}
