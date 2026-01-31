package com.yourown.ai.presentation.chat

import android.util.Log
import com.yourown.ai.data.repository.ConversationRepository
import com.yourown.ai.data.repository.MessageRepository
import com.yourown.ai.data.repository.SystemPromptRepository
import com.yourown.ai.domain.model.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Manages conversations: creation, deletion, selection, title editing
 */
class ChatConversationManager @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val systemPromptRepository: SystemPromptRepository
) {
    
    /**
     * Create new conversation, optionally inheriting context from source chat
     */
    suspend fun createNewConversation(
        conversationCount: Int,
        sourceConversationId: String? = null
    ): String {
        val count = conversationCount + 1
        val title = "Chat $count"
        
        // NEW CHAT - no model selected yet (user must choose)
        val modelName = "No model selected"
        val provider = "unknown"
        
        // Get default API system prompt (will be updated when user selects model)
        val defaultPrompt = systemPromptRepository.getDefaultApiPrompt()
        val systemPromptContent = defaultPrompt?.content 
            ?: "Ты — цифровой партнёр, большая языковая модель."
        val systemPromptId = defaultPrompt?.id
        
        val id = conversationRepository.createConversation(
            title = title,
            systemPrompt = systemPromptContent,
            model = modelName,
            provider = provider,
            systemPromptId = systemPromptId,
            sourceConversationId = sourceConversationId
        )
        
        Log.i("ChatConversationManager", "Created conversation: $id (source: $sourceConversationId)")
        return id
    }
    
    /**
     * Delete conversation
     */
    suspend fun deleteConversation(conversationId: String) {
        conversationRepository.deleteConversation(conversationId)
        Log.i("ChatConversationManager", "Deleted conversation: $conversationId")
    }
    
    /**
     * Update conversation title
     */
    suspend fun updateConversationTitle(conversationId: String, title: String) {
        conversationRepository.updateConversationTitle(conversationId, title)
        Log.i("ChatConversationManager", "Updated conversation title: $conversationId -> $title")
    }
    
    /**
     * Update conversation model
     */
    suspend fun updateConversationModel(
        conversationId: String,
        modelName: String,
        providerName: String
    ) {
        conversationRepository.updateConversationModel(conversationId, modelName, providerName)
        Log.i("ChatConversationManager", "Updated conversation model: $conversationId -> $modelName ($providerName)")
    }
    
    /**
     * Update conversation system prompt
     */
    suspend fun updateConversationSystemPrompt(
        conversationId: String,
        systemPromptId: String,
        systemPrompt: String
    ) {
        conversationRepository.updateConversationSystemPrompt(conversationId, systemPromptId, systemPrompt)
        // Increment usage count
        systemPromptRepository.incrementUsageCount(systemPromptId)
        Log.i("ChatConversationManager", "Updated conversation system prompt: $conversationId")
    }
    
    /**
     * Restore model from conversation metadata
     */
    fun restoreModelFromConversation(modelName: String, providerName: String): ModelProvider? {
        return when (providerName) {
            "local" -> {
                // Find local model by name
                LocalModel.entries.find { it.modelName == modelName }?.let {
                    ModelProvider.Local(it)
                }
            }
            "Deepseek" -> {
                // Find Deepseek model
                DeepseekModel.entries.find { it.modelId == modelName }?.toModelProvider()
            }
            "OpenAI" -> {
                // Find OpenAI model
                OpenAIModel.entries.find { it.modelId == modelName }?.toModelProvider()
            }
            "OpenRouter" -> {
                // Find OpenRouter model
                OpenRouterModel.entries.find { it.modelId == modelName }?.toModelProvider()
            }
            "x.ai (Grok)" -> {
                // Find x.ai model
                XAIModel.entries.find { it.modelId == modelName }?.toModelProvider()
            }
            else -> null
        }
    }
    
    /**
     * Get next conversation to select after deletion
     */
    suspend fun getNextConversationAfterDeletion(
        deletedId: String,
        allConversations: List<Conversation>
    ): String? {
        return allConversations.firstOrNull { it.id != deletedId }?.id
    }
}
