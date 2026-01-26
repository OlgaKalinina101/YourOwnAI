package com.yourown.ai.data.remote.deepseek

/**
 * Deepseek API request/response models
 * Compatible with OpenAI API format
 */
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float? = null,
    val top_p: Float? = null,
    val max_tokens: Int? = null,
    val max_completion_tokens: Int? = null, // OpenAI's new parameter for GPT-5/GPT-4.1
    val stream: Boolean = false
)

data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class ChatCompletionResponse(
    val id: String,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
)

data class Choice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class ChatCompletionChunk(
    val id: String,
    val model: String,
    val choices: List<StreamChoice>
)

data class StreamChoice(
    val index: Int,
    val delta: Delta,
    val finish_reason: String?
)

data class Delta(
    val role: String? = null,
    val content: String? = null
)

data class ModelsResponse(
    val data: List<ModelInfo>
)

data class ModelInfo(
    val id: String,
    val `object`: String,
    val owned_by: String
)
