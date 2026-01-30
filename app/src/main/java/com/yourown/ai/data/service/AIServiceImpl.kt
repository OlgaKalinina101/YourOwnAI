package com.yourown.ai.data.service

import android.util.Log
import com.yourown.ai.data.remote.deepseek.ChatMessage
import com.yourown.ai.data.remote.deepseek.DeepseekClient
import com.yourown.ai.data.remote.openai.OpenAIClient
import com.yourown.ai.data.remote.openrouter.OpenRouterClient
import com.yourown.ai.data.remote.openrouter.OpenRouterMessage
import com.yourown.ai.data.remote.xai.XAIClient
import com.yourown.ai.data.repository.ApiKeyRepository
import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.domain.model.AIProvider
import com.yourown.ai.domain.model.Message
import com.yourown.ai.domain.model.MessageRole
import com.yourown.ai.domain.model.ModelProvider
import com.yourown.ai.domain.service.AIService
import com.yourown.ai.domain.service.LlamaService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIServiceImpl @Inject constructor(
    private val llamaService: LlamaService,
    private val deepseekClient: DeepseekClient,
    private val openAIClient: OpenAIClient,
    private val openRouterClient: OpenRouterClient,
    private val xaiClient: XAIClient,
    private val apiKeyRepository: ApiKeyRepository
) : AIService {
    
    companion object {
        private const val TAG = "AIService"
    }
    
    override suspend fun isModelAvailable(provider: ModelProvider): Boolean {
        return when (provider) {
            is ModelProvider.Local -> {
                llamaService.isModelLoaded() && llamaService.getCurrentModel() == provider.model
            }
            is ModelProvider.API -> {
                apiKeyRepository.hasApiKey(provider.provider)
            }
        }
    }
    
    override fun generateResponse(
        provider: ModelProvider,
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig
    ): Flow<String> {
        return when (provider) {
            is ModelProvider.Local -> {
                generateLocalResponse(provider, messages, systemPrompt, userContext, config)
            }
            is ModelProvider.API -> {
                generateAPIResponse(provider, messages, systemPrompt, userContext, config)
            }
        }
    }
    
    private fun generateLocalResponse(
        provider: ModelProvider.Local,
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig
    ): Flow<String> {
        Log.d(TAG, "Generating local response with ${provider.model.displayName}")
        return llamaService.generateResponse(messages, systemPrompt, userContext, config)
    }
    
    private fun generateAPIResponse(
        provider: ModelProvider.API,
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig
    ): Flow<String> = flow {
        when (provider.provider) {
            AIProvider.DEEPSEEK -> {
                generateDeepseekResponse(provider, messages, systemPrompt, userContext, config)
                    .collect { emit(it) }
            }
            AIProvider.OPENAI -> {
                generateOpenAIResponse(provider, messages, systemPrompt, userContext, config)
                    .collect { emit(it) }
            }
            AIProvider.OPENROUTER -> {
                generateOpenRouterResponse(provider, messages, systemPrompt, userContext, config)
                    .collect { emit(it) }
            }
            AIProvider.XAI -> {
                generateXAIResponse(provider, messages, systemPrompt, userContext, config)
                    .collect { emit(it) }
            }
            AIProvider.CUSTOM -> {
                throw NotImplementedError("Custom providers not yet implemented")
            }
        }
    }
    
    private fun generateDeepseekResponse(
        provider: ModelProvider.API,
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig
    ): Flow<String> = flow {
        Log.d(TAG, "Generating Deepseek response with ${provider.modelId}")
        
        val apiKey = apiKeyRepository.getApiKey(provider.provider)
            ?: throw IllegalStateException("API key not set for ${provider.provider.displayName}")
        
        // Build messages list
        val chatMessages = buildChatMessages(messages, systemPrompt, userContext, config)
        
        // Stream response
        deepseekClient.chatCompletionStream(
            apiKey = apiKey,
            model = provider.modelId,
            messages = chatMessages,
            temperature = config.temperature,
            topP = config.topP,
            maxTokens = config.maxTokens
        ).collect { chunk ->
            emit(chunk)
        }
    }
    
    private fun generateOpenAIResponse(
        provider: ModelProvider.API,
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig
    ): Flow<String> = flow {
        Log.d(TAG, "Generating OpenAI response with ${provider.modelId}")
        
        val apiKey = apiKeyRepository.getApiKey(provider.provider)
            ?: throw IllegalStateException("API key not set for ${provider.provider.displayName}")
        
        // Build messages list
        val chatMessages = buildChatMessages(messages, systemPrompt, userContext, config)
        
        // Stream response
        openAIClient.chatCompletionStream(
            apiKey = apiKey,
            model = provider.modelId,
            messages = chatMessages,
            temperature = config.temperature,
            topP = config.topP,
            maxTokens = config.maxTokens
        ).collect { chunk ->
            emit(chunk)
        }
    }
    
    private fun generateXAIResponse(
        provider: ModelProvider.API,
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig
    ): Flow<String> = flow {
        Log.d(TAG, "Generating x.ai response with ${provider.modelId}")
        
        val apiKey = apiKeyRepository.getApiKey(provider.provider)
            ?: throw IllegalStateException("API key not set for ${provider.provider.displayName}")
        
        // Build messages list
        val chatMessages = buildChatMessages(messages, systemPrompt, userContext, config)
        
        // Stream response
        xaiClient.chatCompletionStream(
            apiKey = apiKey,
            model = provider.modelId,
            messages = chatMessages,
            temperature = config.temperature,
            topP = config.topP,
            maxTokens = config.maxTokens
        ).collect { chunk ->
            emit(chunk)
        }
    }
    
    private fun generateOpenRouterResponse(
        provider: ModelProvider.API,
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig
    ): Flow<String> = flow {
        Log.d(TAG, "Generating OpenRouter response with ${provider.modelId}")
        
        val apiKey = apiKeyRepository.getApiKey(provider.provider)
            ?: throw IllegalStateException("API key not set for ${provider.provider.displayName}")
        
        // Build messages list for OpenRouter (uses string content for now, multimodal later)
        val openRouterMessages = buildOpenRouterMessages(messages, systemPrompt, userContext, config)
        
        // Stream response
        openRouterClient.chatCompletionStream(
            apiKey = apiKey,
            model = provider.modelId,
            messages = openRouterMessages,
            temperature = config.temperature,
            topP = config.topP,
            maxTokens = config.maxTokens
        ).collect { chunk ->
            emit(chunk)
        }
    }
    
    /**
     * Build chat messages list for API request
     */
    private fun buildChatMessages(
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig
    ): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()
        
        // Add system prompt
        var systemContent = systemPrompt
        if (!userContext.isNullOrBlank()) {
            systemContent += "\n\nContext:\n$userContext"
        }
        result.add(ChatMessage(role = "system", content = systemContent))
        
        // Add conversation history (limited by messageHistoryLimit)
        val historyLimit = config.messageHistoryLimit * 2 // pairs = user + assistant
        val relevantMessages = messages.takeLast(historyLimit)
        
        for (message in relevantMessages) {
            val role = when (message.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            result.add(ChatMessage(role = role, content = message.content))
        }
        
        return result
    }
    
    /**
     * Build OpenRouter messages list (supports multimodal content)
     */
    private fun buildOpenRouterMessages(
        messages: List<Message>,
        systemPrompt: String,
        userContext: String?,
        config: AIConfig
    ): List<OpenRouterMessage> {
        val result = mutableListOf<OpenRouterMessage>()
        
        // Add system prompt
        var systemContent = systemPrompt
        if (!userContext.isNullOrBlank()) {
            systemContent += "\n\nContext:\n$userContext"
        }
        result.add(OpenRouterMessage(role = "system", content = systemContent))
        
        // Add conversation history (limited by messageHistoryLimit)
        val historyLimit = config.messageHistoryLimit * 2 // pairs = user + assistant
        val relevantMessages = messages.takeLast(historyLimit)
        
        for (message in relevantMessages) {
            val role = when (message.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            // For now, use simple string content
            // TODO: Support multimodal content (images) in the future
            result.add(OpenRouterMessage(role = role, content = message.content))
        }
        
        return result
    }
    
    override suspend fun stopGeneration() {
        // Stop local generation
        llamaService.stopGeneration()
        
        // TODO: Implement cancellation for API requests
    }
}
