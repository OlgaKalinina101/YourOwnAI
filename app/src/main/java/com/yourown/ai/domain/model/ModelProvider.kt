package com.yourown.ai.domain.model

/**
 * Model provider types
 */
sealed class ModelProvider {
    /**
     * Local on-device model
     */
    data class Local(val model: LocalModel) : ModelProvider()
    
    /**
     * API-based model
     */
    data class API(
        val provider: AIProvider,
        val modelId: String,
        val displayName: String
    ) : ModelProvider()
    
    /**
     * Get unique key for model identification (used for pinning, etc.)
     */
    fun getModelKey(): String = when (this) {
        is Local -> "local:${model.name}"
        is API -> "api:${provider.name}:${modelId}"
    }
}

/**
 * Available Deepseek models
 */
enum class DeepseekModel(
    val modelId: String,
    val displayName: String,
    val description: String
) {
    DEEPSEEK_CHAT(
        modelId = "deepseek-chat",
        displayName = "DeepSeek Chat (V3.2)",
        description = "Non-thinking mode - fast and efficient"
    ),
    DEEPSEEK_REASONER(
        modelId = "deepseek-reasoner",
        displayName = "DeepSeek Reasoner (V3.2)",
        description = "Thinking mode - deeper reasoning"
    );
    
    fun toModelProvider(): ModelProvider.API {
        return ModelProvider.API(
            provider = AIProvider.DEEPSEEK,
            modelId = modelId,
            displayName = displayName
        )
    }
}

/**
 * Available OpenAI models
 */
enum class OpenAIModel(
    val modelId: String,
    val displayName: String,
    val description: String
) {
    // GPT-5 Series (Latest)
    GPT_5_2(
        modelId = "gpt-5.2",
        displayName = "GPT-5.2",
        description = "Best for coding and agentic tasks"
    ),
    GPT_5_1(
        modelId = "gpt-5.1",
        displayName = "GPT-5.1",
        description = "Coding with configurable reasoning effort"
    ),

    // GPT-4.1 Series
    GPT_4_1(
        modelId = "gpt-4.1-2025-04-14",
        displayName = "GPT-4.1",
        description = "Strong, intelligent, base"
    ),
    
    // GPT-4o Series
    GPT_4O(
        modelId = "gpt-4o-2024-08-06",
        displayName = "GPT-4o",
        description = "Fast, intelligent, flexible"
    );
    
    fun toModelProvider(): ModelProvider.API {
        return ModelProvider.API(
            provider = AIProvider.OPENAI,
            modelId = modelId,
            displayName = displayName
        )
    }
}

/**
 * Available x.ai (Grok) models
 */
enum class XAIModel(
    val modelId: String,
    val displayName: String,
    val description: String
) {
    GROK_4_1_FAST_REASONING(
        modelId = "grok-4-1-fast-reasoning",
        displayName = "Grok 4.1 Fast Reasoning",
        description = "Fast reasoning with extended context"
    ),
    GROK_4_1_FAST_NON_REASONING(
        modelId = "grok-4-1-fast-non-reasoning",
        displayName = "Grok 4.1 Fast Non-Reasoning",
        description = "Fastest responses without reasoning"
    ),
    GROK_CODE_FAST_1(
        modelId = "grok-code-fast-1",
        displayName = "Grok Code Fast 1",
        description = "Optimized for code generation"
    ),
    GROK_4_FAST_REASONING(
        modelId = "grok-4-fast-reasoning",
        displayName = "Grok 4 Fast Reasoning",
        description = "Fast reasoning mode"
    ),
    GROK_4_FAST_NON_REASONING(
        modelId = "grok-4-fast-non-reasoning",
        displayName = "Grok 4 Fast Non-Reasoning",
        description = "Fast non-reasoning mode"
    ),
    GROK_4_0709(
        modelId = "grok-4-0709",
        displayName = "Grok 4 (0709)",
        description = "Stable snapshot from July 9"
    ),
    GROK_3_MINI(
        modelId = "grok-3-mini",
        displayName = "Grok 3 Mini",
        description = "Compact, efficient model"
    ),
    GROK_3(
        modelId = "grok-3",
        displayName = "Grok 3",
        description = "Full-featured Grok 3"
    );
    
    fun toModelProvider(): ModelProvider.API {
        return ModelProvider.API(
            provider = AIProvider.XAI,
            modelId = modelId,
            displayName = displayName
        )
    }
}

/**
 * Available OpenRouter models
 */
enum class OpenRouterModel(
    val modelId: String,
    val displayName: String,
    val description: String
) {
    // Claude 4.5 Series
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
    ),
    
    // Claude 4 & 3.x Series
    CLAUDE_SONNET_4(
        modelId = "anthropic/claude-sonnet-4",
        displayName = "Claude Sonnet 4",
        description = "Stable Claude 4 with vision support"
    ),
    CLAUDE_3_7_SONNET(
        modelId = "anthropic/claude-3.7-sonnet",
        displayName = "Claude 3.7 Sonnet",
        description = "Enhanced 3.5 with better reasoning"
    ),
    CLAUDE_3_5_HAIKU(
        modelId = "anthropic/claude-3.5-haiku",
        displayName = "Claude 3.5 Haiku",
        description = "Fast, efficient with vision"
    ),
    
    // Llama 4 Series
    LLAMA_4_MAVERICK(
        modelId = "meta-llama/llama-4-maverick",
        displayName = "Llama 4 Maverick",
        description = "Flagship Llama 4 model with advanced reasoning"
    ),
    LLAMA_4_SCOUT(
        modelId = "meta-llama/llama-4-scout",
        displayName = "Llama 4 Scout",
        description = "Efficient Llama 4 variant for fast inference"
    ),

    // Llama 3.1 Series
    LLAMA_3_1_EURUALE(
        modelId = "sao10k/l3.1-euryale-70b",
        displayName = "Llama 3.1-euryale",
        description = "Euryale L3.1 70B v2.2 focused on creative roleplay"
    ),
    
    // Gemini 3 Series (with reasoning tokens)
    GEMINI_3_PRO_PREVIEW(
        modelId = "google/gemini-3-pro-preview",
        displayName = "Gemini 3 Pro Preview",
        description = "Advanced reasoning with thinking capabilities"
    ),
    GEMINI_3_FLASH_PREVIEW(
        modelId = "google/gemini-3-flash-preview",
        displayName = "Gemini 3 Flash Preview",
        description = "Fast reasoning with preview features"
    ),
    
    // Gemini 2.5 Series (multimodal support)
    GEMINI_2_5_PRO(
        modelId = "google/gemini-2.5-pro",
        displayName = "Gemini 2.5 Pro",
        description = "Multimodal: text, image, audio, video"
    ),
    GEMINI_2_5_FLASH(
        modelId = "google/gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        description = "Fast multimodal with 1M+ context"
    ),

    // Nous: Hermes
    NOUS_HERMES_3_405B(
        modelId = "nousresearch/hermes-3-llama-3.1-405b:free",
        displayName = "Nous: Hermes 3 free",
        description = "Focused on aligning LLMs to the user, with powerful steering capabilities and control given to the end user"
    ),

    NOUS_HERMES_3_70B(
        modelId = "nousresearch/hermes-3-llama-3.1-70b",
        displayName = "Nous: Hermes 3 70B",
        description = "Focused on aligning LLMs to the user, with powerful steering capabilities and control given to the end user."
    ),
    
    // OpenAI GPT-4o Series
    GPT_4O_2024_05_13(
        modelId = "openai/gpt-4o-2024-05-13",
        displayName = "GPT-4o (2024-05-13)",
        description = "Stable GPT-4o snapshot with vision"
    );
    
    fun toModelProvider(): ModelProvider.API {
        return ModelProvider.API(
            provider = AIProvider.OPENROUTER,
            modelId = modelId,
            displayName = displayName
        )
    }
}
