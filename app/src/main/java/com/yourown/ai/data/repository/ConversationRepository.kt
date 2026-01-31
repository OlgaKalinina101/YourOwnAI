package com.yourown.ai.data.repository

import com.yourown.ai.data.local.dao.ConversationDao
import com.yourown.ai.data.local.dao.MessageDao
import com.yourown.ai.data.local.entity.ConversationEntity
import com.yourown.ai.data.mapper.toDomain
import com.yourown.ai.data.mapper.toEntity
import com.yourown.ai.domain.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    
    /**
     * Get all non-archived conversations with their messages
     */
    fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { conversations ->
            conversations.map { entity ->
                val messages = messageDao.getMessagesByConversation(entity.id)
                    .map { messageEntities ->
                        messageEntities.map { it.toDomain() }
                    }
                // For now, return without messages (will be loaded separately)
                entity.toDomain(emptyList())
            }
        }
    }
    
    /**
     * Get conversation by ID with messages
     */
    fun getConversationById(id: String): Flow<Conversation?> {
        return combine(
            conversationDao.observeConversationById(id),
            messageDao.getMessagesByConversation(id)
        ) { conversation, messages ->
            conversation?.toDomain(messages.map { it.toDomain() })
        }
    }
    
    /**
     * Create new conversation
     */
    suspend fun createConversation(
        title: String,
        systemPrompt: String,
        model: String,
        provider: String,
        systemPromptId: String? = null,
        sourceConversationId: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        
        val conversation = ConversationEntity(
            id = id,
            title = title,
            systemPrompt = systemPrompt,
            systemPromptId = systemPromptId,
            model = model,
            provider = provider,
            createdAt = now,
            updatedAt = now,
            sourceConversationId = sourceConversationId
        )
        
        conversationDao.insertConversation(conversation)
        return id
    }
    
    /**
     * Update conversation
     */
    suspend fun updateConversation(conversation: Conversation) {
        conversationDao.updateConversation(conversation.toEntity())
    }
    
    /**
     * Update conversation title
     */
    suspend fun updateConversationTitle(id: String, title: String) {
        val conversation = conversationDao.getConversationById(id)
        conversation?.let {
            conversationDao.updateConversation(
                it.copy(
                    title = title,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Update conversation model
     */
    suspend fun updateConversationModel(id: String, model: String, provider: String) {
        val conversation = conversationDao.getConversationById(id)
        conversation?.let {
            conversationDao.updateConversation(
                it.copy(
                    model = model,
                    provider = provider,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Update conversation system prompt
     */
    suspend fun updateConversationSystemPrompt(id: String, systemPromptId: String, systemPrompt: String) {
        val conversation = conversationDao.getConversationById(id)
        conversation?.let {
            conversationDao.updateConversation(
                it.copy(
                    systemPromptId = systemPromptId,
                    systemPrompt = systemPrompt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Update conversation's updatedAt timestamp
     * Called when a new message is added to keep conversations sorted by last activity
     */
    suspend fun updateConversationTimestamp(id: String) {
        val conversation = conversationDao.getConversationById(id)
        conversation?.let {
            conversationDao.updateConversation(
                it.copy(updatedAt = System.currentTimeMillis())
            )
        }
    }
    
    /**
     * Delete conversation
     */
    suspend fun deleteConversation(id: String) {
        val conversation = conversationDao.getConversationById(id)
        conversation?.let {
            messageDao.deleteMessagesByConversation(id)
            conversationDao.deleteConversation(it)
        }
    }
    
    /**
     * Pin/unpin conversation
     */
    suspend fun setPinned(id: String, isPinned: Boolean) {
        conversationDao.setPinned(id, isPinned)
    }
    
    /**
     * Archive conversation
     */
    suspend fun setArchived(id: String, isArchived: Boolean) {
        conversationDao.setArchived(id, isArchived)
    }
    
    /**
     * Get next conversation number for auto-naming (Chat 1, Chat 2, etc)
     */
    suspend fun getNextConversationNumber(): Int {
        // Get all conversations and find the highest number
        val conversations = conversationDao.getAllConversations()
        // This is a simplified version - in real app would need better logic
        return 1 // TODO: Implement proper numbering
    }
}
