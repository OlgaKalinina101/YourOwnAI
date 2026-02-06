package com.yourown.ai.presentation.chat

import android.util.Log
import com.yourown.ai.data.repository.MemoryRepository
import com.yourown.ai.data.repository.MessageRepository
import com.yourown.ai.domain.model.*
import com.yourown.ai.domain.service.AIService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject

/**
 * Handles message sending, AI generation, and memory extraction
 */
class ChatMessageHandler @Inject constructor(
    private val messageRepository: MessageRepository,
    private val memoryRepository: MemoryRepository,
    private val aiService: AIService,
    private val contextBuilder: ChatContextBuilder
) {
    
    /**
     * Send message and generate AI response
     * Returns Flow of AI response chunks
     */
    suspend fun sendMessage(
        userMessage: Message,
        selectedModel: ModelProvider,
        config: AIConfig,
        userContext: String,
        allMessages: List<Message>,
        swipeMessage: Message? = null,
        personaId: String? = null
    ): Flow<String> = flow {
        Log.d("ChatMessageHandler", "sendMessage: personaId=$personaId")
        
        // Build enhanced context with Deep Empathy, Memory, and RAG
        val enhancedContextResult = contextBuilder.buildEnhancedContext(
            baseContext = userContext,
            userMessage = userMessage.content,
            config = config,
            selectedModel = selectedModel,
            conversationId = userMessage.conversationId,
            swipeMessage = swipeMessage,
            personaId = personaId
        )
        
        // Choose system prompt based on model type
        val systemPrompt = when (selectedModel) {
            is ModelProvider.Local -> config.localSystemPrompt
            is ModelProvider.API -> config.systemPrompt
        }
        
        // Generate response using unified AIService
        aiService.generateResponse(
            provider = selectedModel,
            messages = allMessages,
            systemPrompt = systemPrompt,
            userContext = enhancedContextResult.fullContext.ifBlank { null },
            config = config
        ).collect { chunk ->
            emit(chunk)
        }
    }
    
    /**
     * Build enhanced context for logging (without sending message)
     */
    suspend fun buildEnhancedContextForLogs(
        baseContext: String,
        userMessage: String,
        config: AIConfig,
        selectedModel: ModelProvider,
        conversationId: String,
        swipeMessage: Message? = null,
        personaId: String? = null
    ): EnhancedContextResult {
        return contextBuilder.buildEnhancedContext(
            baseContext = baseContext,
            userMessage = userMessage,
            config = config,
            selectedModel = selectedModel,
            conversationId = conversationId,
            swipeMessage = swipeMessage,
            personaId = personaId
        )
    }
    
    /**
     * Extract and save memory from user message
     */
    suspend fun extractAndSaveMemory(
        userMessage: Message,
        selectedModel: ModelProvider,
        config: AIConfig,
        conversationId: String,
        personaId: String? = null
    ) {
        try {
            Log.d("ChatMessageHandler", "Starting memory extraction for message: ${userMessage.id}, personaId=$personaId")
            
            // Get memory extraction prompt from config (user can customize it)
            val memoryPrompt = config.memoryExtractionPrompt
            
            // Replace {text} placeholder with user message content
            val filledPrompt = memoryPrompt.replace("{text}", userMessage.content)
            
            // Create a temporary message list with just the prompt
            val promptMessage = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.USER,
                content = filledPrompt,
                createdAt = System.currentTimeMillis()
            )
            
            // Use simple system prompt for memory extraction
            val systemPrompt = "Ты - аналитик смысла."
            
            // Call AI to extract memory
            val responseBuilder = StringBuilder()
            aiService.generateResponse(
                provider = selectedModel,
                messages = listOf(promptMessage),
                systemPrompt = systemPrompt,
                userContext = null,
                config = config.copy(messageHistoryLimit = 1) // Don't need history for memory extraction
            ).collect { chunk ->
                responseBuilder.append(chunk)
            }
            
            val memoryResponse = responseBuilder.toString().trim()
            Log.d("ChatMessageHandler", "Memory extraction response: $memoryResponse")
            
            // Parse and save memory
            val memoryEntry = com.yourown.ai.domain.model.MemoryEntry.parseFromResponse(
                response = memoryResponse,
                conversationId = conversationId,
                messageId = userMessage.id,
                personaId = personaId
            )
            
            if (memoryEntry != null) {
                Log.d("ChatMessageHandler", "Parsed memory entry: personaId=${memoryEntry.personaId}, fact=${memoryEntry.fact}")
                memoryRepository.insertMemory(memoryEntry)
                Log.i("ChatMessageHandler", "Memory saved with personaId=${memoryEntry.personaId}: ${memoryEntry.fact}")
            } else {
                Log.d("ChatMessageHandler", "No key information extracted")
            }
            
        } catch (e: Exception) {
            Log.e("ChatMessageHandler", "Error extracting memory", e)
        }
    }
    
    /**
     * Build message history with context inheritance support
     * 
     * If conversation has sourceConversationId:
     * - Get last N pairs from source conversation
     * - Current messages gradually replace inherited messages as new messages are added
     * - Total pairs = messageHistoryLimit (e.g., 10 pairs = 20 messages)
     * 
     * @param currentMessages Messages from current conversation
     * @param sourceConversationId ID of conversation to inherit context from
     * @param messageHistoryLimit Total number of message pairs to include
     * @return Combined message list (inherited + current), limited by messageHistoryLimit
     */
    suspend fun buildMessageHistoryWithInheritance(
        currentMessages: List<Message>,
        sourceConversationId: String?,
        messageHistoryLimit: Int
    ): List<Message> {
        // If no source conversation, use current messages only
        if (sourceConversationId == null) {
            return currentMessages
        }
        
        try {
            // Count current message pairs
            val currentPairs = mutableListOf<Pair<Message, Message>>()
            var i = 0
            while (i < currentMessages.size - 1) {
                val current = currentMessages[i]
                val next = currentMessages[i + 1]
                
                if (current.role.toStringValue() == "user" && next.role.toStringValue() == "assistant") {
                    currentPairs.add(Pair(current, next))
                    i += 2
                } else {
                    i += 1
                }
            }
            
            // Handle last unpaired user message
            val lastUnpairedUser = if (currentMessages.isNotEmpty() && 
                                      currentMessages.last().role.toStringValue() == "user" &&
                                      (currentPairs.isEmpty() || currentPairs.last().first.id != currentMessages.last().id)) {
                currentMessages.last()
            } else null
            
            val currentPairCount = currentPairs.size
            Log.d("ChatMessageHandler", "Context inheritance: current has $currentPairCount pairs, limit is $messageHistoryLimit")
            
            // Calculate how many pairs to get from source
            val neededFromSource = (messageHistoryLimit - currentPairCount).coerceAtLeast(0)
            
            if (neededFromSource == 0) {
                // Current conversation has enough pairs, use only last N pairs from current
                val messagesToUse = currentPairs.takeLast(messageHistoryLimit)
                    .flatMap { listOf(it.first, it.second) }
                return if (lastUnpairedUser != null) {
                    messagesToUse + lastUnpairedUser
                } else {
                    messagesToUse
                }
            }
            
            // Get inherited pairs from source conversation
            val inheritedMessages = messageRepository.getLastMessagePairs(
                conversationId = sourceConversationId,
                pairLimit = neededFromSource
            )
            
            Log.d("ChatMessageHandler", "Got ${inheritedMessages.size} inherited messages from source (${inheritedMessages.size / 2} pairs)")
            
            // Combine: inherited messages + current messages
            val combined = inheritedMessages + currentPairs.flatMap { listOf(it.first, it.second) }
            
            // Add unpaired user message if exists
            return if (lastUnpairedUser != null) {
                combined + lastUnpairedUser
            } else {
                combined
            }
            
        } catch (e: Exception) {
            Log.e("ChatMessageHandler", "Error building message history with inheritance", e)
            // Fallback to current messages only
            return currentMessages
        }
    }
    
    /**
     * Build request logs with full context
     */
    fun buildRequestLogs(
        model: ModelProvider,
        config: AIConfig,
        allMessages: List<Message> = emptyList(),
        fullContext: String? = null,
        deepEmpathyAnalysis: String? = null,
        memoriesUsed: List<String> = emptyList(),
        ragChunksUsed: List<String> = emptyList()
    ): String {
        val modelInfo = when (model) {
            is ModelProvider.Local -> mapOf(
                "type" to "local",
                "modelName" to model.model.modelName,
                "displayName" to model.model.displayName,
                "sizeInMB" to model.model.sizeInMB
            )
            is ModelProvider.API -> mapOf(
                "type" to "api",
                "provider" to model.provider.displayName,
                "modelId" to model.modelId,
                "displayName" to model.displayName
            )
        }
        
        // Choose system prompt based on model type
        val actualSystemPrompt = when (model) {
            is ModelProvider.Local -> config.localSystemPrompt
            is ModelProvider.API -> config.systemPrompt
        }
        
        // Build messages list (limited by history)
        val historyLimit = config.messageHistoryLimit * 2
        val relevantMessages = allMessages.takeLast(historyLimit)
        val messagesJson = relevantMessages.map { msg ->
            mapOf(
                "role" to msg.role.toStringValue(),
                "content" to msg.content.take(200) + if (msg.content.length > 200) "..." else "",
                "timestamp" to msg.createdAt
            )
        }
        
        // Build context breakdown
        val contextBreakdown = mutableMapOf<String, Any>()
        if (!fullContext.isNullOrBlank()) {
            contextBreakdown["full_context"] = fullContext
        }
        if (!deepEmpathyAnalysis.isNullOrBlank()) {
            contextBreakdown["deep_empathy_analysis"] = deepEmpathyAnalysis
        }
        if (memoriesUsed.isNotEmpty()) {
            contextBreakdown["memories_count"] = memoriesUsed.size
            contextBreakdown["memories"] = memoriesUsed
        }
        if (ragChunksUsed.isNotEmpty()) {
            contextBreakdown["rag_chunks_count"] = ragChunksUsed.size
            contextBreakdown["rag_chunks"] = ragChunksUsed.map { it.take(200) + if (it.length > 200) "..." else "" }
        }
        
        // Build full request snapshot
        val requestSnapshot = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "model" to modelInfo,
            "parameters" to mapOf(
                "temperature" to config.temperature,
                "top_p" to config.topP,
                "max_tokens" to config.maxTokens,
                "message_history_limit" to config.messageHistoryLimit
            ),
            "flags" to mapOf(
                "deep_empathy" to config.deepEmpathy,
                "memory_enabled" to config.memoryEnabled,
                "rag_enabled" to config.ragEnabled
            ),
            "system_prompt" to actualSystemPrompt,
            "context" to contextBreakdown,
            "message_count" to mapOf(
                "total" to allMessages.size,
                "sent_to_model" to relevantMessages.size
            ),
            "messages" to messagesJson
        )
        
        // Convert to pretty JSON
        return com.google.gson.GsonBuilder()
            .setPrettyPrinting()
            .create()
            .toJson(requestSnapshot)
    }
    
    /**
     * Delete message
     */
    suspend fun deleteMessage(messageId: String) {
        messageRepository.deleteMessage(messageId)
        Log.i("ChatMessageHandler", "Deleted message: $messageId")
    }
    
    /**
     * Toggle like on message
     */
    suspend fun toggleLike(messageId: String) {
        messageRepository.toggleLike(messageId)
        Log.d("ChatMessageHandler", "Toggled like on message: $messageId")
    }
    
    /**
     * Update message content
     */
    suspend fun updateMessage(message: Message) {
        messageRepository.updateMessage(message)
        Log.d("ChatMessageHandler", "Updated message: ${message.id}")
    }
}
