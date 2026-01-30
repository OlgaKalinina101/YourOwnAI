package com.yourown.ai.data.remote.openrouter

import com.yourown.ai.domain.model.AIProvider
import com.yourown.ai.domain.model.ModelProvider

/**
 * OpenRouter API request/response models
 * OpenRouter uses OpenAI-compatible format with additional fields
 * https://openrouter.ai/docs/api-reference
 */
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Float? = null,
    val top_p: Float? = null,
    val max_tokens: Int? = null,
    val stream: Boolean = false
)

/**
 * OpenRouter message - supports text and images
 */
data class OpenRouterMessage(
    val role: String, // "system", "user", "assistant"
    val content: Any // Can be String or List<ContentPart>
)

/**
 * Content part for multimodal messages
 */
sealed class ContentPart {
    data class Text(
        val type: String = "text",
        val text: String
    ) : ContentPart()
    
    data class Image(
        val type: String = "image_url",
        val image_url: ImageUrl
    ) : ContentPart()
}

data class ImageUrl(
    val url: String
)

/**
 * OpenRouter response
 */
data class OpenRouterResponse(
    val id: String,
    val model: String,
    val choices: List<OpenRouterChoice>,
    val usage: OpenRouterUsage?
)

data class OpenRouterChoice(
    val index: Int,
    val message: OpenRouterMessage,
    val finish_reason: String?
)

data class OpenRouterUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

/**
 * Streaming chunk
 */
data class OpenRouterChunk(
    val id: String,
    val model: String,
    val choices: List<OpenRouterStreamChoice>
)

data class OpenRouterStreamChoice(
    val index: Int,
    val delta: OpenRouterDelta,
    val finish_reason: String?
)

data class OpenRouterDelta(
    val role: String? = null,
    val content: String? = null
)

/**
 * Available OpenRouter models (Claude via OpenRouter)
 */
enum class OpenRouterModel(
    val modelId: String,
    val displayName: String,
    val description: String
) {
    CLAUDE_SONNET_4_5(
        modelId = "anthropic/claude-sonnet-4.5",
        displayName = "Claude Sonnet 4.5",
        description = "Balanced performance and speed"
    ),
    CLAUDE_OPUS_4_5(
        modelId = "anthropic/claude-opus-4.5",
        displayName = "Claude Opus 4.5",
        description = "Most capable, best for complex tasks"
    ),
    CLAUDE_HAIKU_4_5(
        modelId = "anthropic/claude-haiku-4.5",
        displayName = "Claude Haiku 4.5",
        description = "Fast and efficient"
    );
    
    fun toModelProvider(): ModelProvider.API {
        return ModelProvider.API(
            provider = AIProvider.OPENROUTER,
            modelId = modelId,
            displayName = displayName
        )
    }
}
