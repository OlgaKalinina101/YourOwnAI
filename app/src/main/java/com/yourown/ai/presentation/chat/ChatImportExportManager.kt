package com.yourown.ai.presentation.chat

import android.util.Log
import com.yourown.ai.data.repository.ConversationRepository
import com.yourown.ai.data.repository.MessageRepository
import com.yourown.ai.domain.model.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Handles chat import/export functionality
 */
class ChatImportExportManager @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) {
    
    /**
     * Export chat to markdown text
     */
    fun exportChat(
        conversation: Conversation,
        messages: List<Message>,
        filterByLikes: Boolean = false
    ): String {
        if (messages.isEmpty()) {
            return ""
        }
        
        // Filter messages by likes if requested
        val filteredMessages = if (filterByLikes) {
            messages.filter { it.isLiked }
        } else {
            messages
        }
        
        if (filteredMessages.isEmpty() && filterByLikes) {
            return "No liked messages to export.\n\nTip: Like messages by clicking the ❤️ icon in the message menu."
        }
        
        val exportBuilder = StringBuilder()
        exportBuilder.appendLine("# Chat Export: ${conversation.title}")
        exportBuilder.appendLine()
        exportBuilder.appendLine("**Date:** ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        exportBuilder.appendLine("**Model:** ${conversation.model} (${conversation.provider})")
        if (filterByLikes) {
            exportBuilder.appendLine("**Filter:** ❤️ Liked messages only (${filteredMessages.size} messages)")
        } else {
            exportBuilder.appendLine("**Total messages:** ${filteredMessages.size}")
        }
        exportBuilder.appendLine()
        exportBuilder.appendLine("---")
        exportBuilder.appendLine()
        
        filteredMessages.forEach { message ->
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Date(message.createdAt))
            val role = when (message.role) {
                MessageRole.USER -> "## 👤 User"
                MessageRole.ASSISTANT -> "## 🤖 Assistant"
                MessageRole.SYSTEM -> "## ⚙️ System"
            }
            val likeIndicator = if (message.isLiked) " ❤️" else ""
            
            exportBuilder.appendLine("$role$likeIndicator")
            exportBuilder.appendLine("*$timestamp*")
            exportBuilder.appendLine()
            exportBuilder.appendLine(message.content)
            exportBuilder.appendLine()
            exportBuilder.appendLine("---")
            exportBuilder.appendLine()
        }
        
        return exportBuilder.toString()
    }
    
    /**
     * Import chat from markdown text
     * Returns conversation ID on success, null on failure
     */
    suspend fun importChat(chatText: String): Pair<String?, String?> {
        try {
            val lines = chatText.lines()
            
            // Parse title (first line starting with "# Chat Export:")
            val titleLine = lines.firstOrNull { it.startsWith("# Chat Export:") }
            val title = titleLine?.removePrefix("# Chat Export:")?.trim() ?: "Imported Chat"
            
            // Parse model and provider
            val modelLine = lines.firstOrNull { it.startsWith("**Model:**") }
            var provider: ModelProvider = OpenAIModel.GPT_4O.toModelProvider()
            var modelName = "gpt-4o"
            
            if (modelLine != null) {
                val modelPart = modelLine.removePrefix("**Model:**").trim()
                // Format: "model-name (provider)"
                val regex = """(.+?)\s*\((.+?)\)""".toRegex()
                val match = regex.find(modelPart)
                if (match != null) {
                    modelName = match.groupValues[1].trim()
                    val providerName = match.groupValues[2].trim()
                    
                    // Try to match provider
                    provider = when {
                        providerName.contains("OpenAI", ignoreCase = true) -> {
                            // Try to find matching OpenAI model, fallback to GPT_4O
                            OpenAIModel.entries.find { it.modelId == modelName || it.displayName == modelName }
                                ?.toModelProvider() ?: OpenAIModel.GPT_4O.toModelProvider()
                        }
                        providerName.contains("Deepseek", ignoreCase = true) -> {
                            // Try to find matching Deepseek model, fallback to DEEPSEEK_CHAT
                            DeepseekModel.entries.find { it.modelId == modelName || it.displayName == modelName }
                                ?.toModelProvider() ?: DeepseekModel.DEEPSEEK_CHAT.toModelProvider()
                        }
                        providerName.contains("OpenRouter", ignoreCase = true) -> {
                            // Try to find matching OpenRouter model, fallback to CLAUDE_SONNET_4_5
                            OpenRouterModel.entries.find { it.modelId == modelName || it.displayName == modelName }
                                ?.toModelProvider() ?: OpenRouterModel.CLAUDE_SONNET_4_5.toModelProvider()
                        }
                        providerName.contains("xAI", ignoreCase = true) || providerName.contains("Grok", ignoreCase = true) -> {
                            // Try to find matching xAI model, fallback to GROK_4_1_FAST_REASONING
                            XAIModel.entries.find { it.modelId == modelName || it.displayName == modelName }
                                ?.toModelProvider() ?: XAIModel.GROK_4_1_FAST_REASONING.toModelProvider()
                        }
                        else -> OpenAIModel.GPT_4O.toModelProvider()
                    }
                }
            }
            
            // Parse messages
            val messages = parseMessages(lines)
            
            if (messages.isEmpty()) {
                return null to "No messages found in the imported text"
            }
            
            // Get provider name correctly
            val providerName = when (provider) {
                is ModelProvider.Local -> "local"
                is ModelProvider.API -> {
                    when (provider.provider) {
                        com.yourown.ai.domain.model.AIProvider.OPENAI -> "OpenAI"
                        com.yourown.ai.domain.model.AIProvider.DEEPSEEK -> "Deepseek"
                        com.yourown.ai.domain.model.AIProvider.OPENROUTER -> "OpenRouter"
                        com.yourown.ai.domain.model.AIProvider.XAI -> "x.ai (Grok)"
                        com.yourown.ai.domain.model.AIProvider.CUSTOM -> "Custom"
                    }
                }
            }
            
            // Create conversation and get ID
            val conversationId = conversationRepository.createConversation(
                title = title,
                systemPrompt = "", // Imported chats use default system prompt
                model = modelName,
                provider = providerName,
                systemPromptId = null
            )
            
            // Insert messages with correct conversation ID
            messages.forEach { message ->
                messageRepository.addMessage(message.copy(conversationId = conversationId))
            }
            
            Log.d("ChatImportExportManager", "Imported chat: $conversationId, ${messages.size} messages")
            
            return conversationId to null
        } catch (e: Exception) {
            Log.e("ChatImportExportManager", "Error importing chat", e)
            return null to "Error parsing chat: ${e.message}"
        }
    }
    
    /**
     * Parse messages from imported text lines
     */
    private fun parseMessages(lines: List<String>): List<Message> {
        val messages = mutableListOf<Message>()
        var currentRole: MessageRole? = null
        var currentContent = StringBuilder()
        var currentTimestamp = System.currentTimeMillis()
        var isLiked = false
        
        for (line in lines) {
            when {
                line.startsWith("## 👤 User") -> {
                    // Save previous message
                    if (currentRole != null && currentContent.isNotEmpty()) {
                        messages.add(
                            Message(
                                id = UUID.randomUUID().toString(),
                                conversationId = "", // Will be set later
                                role = currentRole,
                                content = currentContent.toString().trim(),
                                createdAt = currentTimestamp,
                                isLiked = isLiked
                            )
                        )
                    }
                    currentRole = MessageRole.USER
                    currentContent = StringBuilder()
                    isLiked = line.contains("❤️")
                }
                line.startsWith("## 🤖 Assistant") -> {
                    // Save previous message
                    if (currentRole != null && currentContent.isNotEmpty()) {
                        messages.add(
                            Message(
                                id = UUID.randomUUID().toString(),
                                conversationId = "", // Will be set later
                                role = currentRole,
                                content = currentContent.toString().trim(),
                                createdAt = currentTimestamp,
                                isLiked = isLiked
                            )
                        )
                    }
                    currentRole = MessageRole.ASSISTANT
                    currentContent = StringBuilder()
                    isLiked = line.contains("❤️")
                }
                line.startsWith("## ⚙️ System") -> {
                    // Save previous message
                    if (currentRole != null && currentContent.isNotEmpty()) {
                        messages.add(
                            Message(
                                id = UUID.randomUUID().toString(),
                                conversationId = "", // Will be set later
                                role = currentRole,
                                content = currentContent.toString().trim(),
                                createdAt = currentTimestamp,
                                isLiked = isLiked
                            )
                        )
                    }
                    currentRole = MessageRole.SYSTEM
                    currentContent = StringBuilder()
                    isLiked = line.contains("❤️")
                }
                line.startsWith("*") && line.endsWith("*") -> {
                    // Timestamp line - ignore
                }
                line == "---" || line.isBlank() -> {
                    // Separator or blank - ignore
                }
                else -> {
                    // Content line
                    if (currentRole != null) {
                        if (currentContent.isNotEmpty()) {
                            currentContent.append("\n")
                        }
                        currentContent.append(line)
                    }
                }
            }
        }
        
        // Save last message
        if (currentRole != null && currentContent.isNotEmpty()) {
            messages.add(
                Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = "", // Will be set later
                    role = currentRole,
                    content = currentContent.toString().trim(),
                    createdAt = currentTimestamp,
                    isLiked = isLiked
                )
            )
        }
        
        return messages
    }
}
