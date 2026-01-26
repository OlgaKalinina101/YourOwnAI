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
    
    // GPT-4o Series
    GPT_4O(
        modelId = "gpt-4o",
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
