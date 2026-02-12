package com.yourown.ai.data.remote.openai

import com.google.gson.annotations.SerializedName

/**
 * OpenAI Responses API models
 * https://platform.openai.com/docs/api-reference/responses-streaming
 * Note: Responses API uses 'input' instead of 'messages'
 */

/**
 * Request for Responses API
 */
data class ResponsesApiRequest(
    val model: String,
    val input: List<com.yourown.ai.data.remote.deepseek.ChatMessage>, // 'input' not 'messages'
    val tools: List<Tool>? = null,
    val stream: Boolean = true,
    val temperature: Float? = null,
    @SerializedName("top_p")
    val top_p: Float? = null,
    @SerializedName("max_output_tokens")
    val max_output_tokens: Int? = null  // Responses API uses 'max_output_tokens'
)

/**
 * Request for Responses API with multimodal support
 */
data class ResponsesApiMultimodalRequest(
    val model: String,
    val input: List<MultimodalChatMessage>, // 'input' not 'messages'
    val tools: List<Tool>? = null,
    val stream: Boolean = true,
    val temperature: Float? = null,
    @SerializedName("top_p")
    val top_p: Float? = null,
    @SerializedName("max_output_tokens")
    val max_output_tokens: Int? = null  // Responses API uses 'max_output_tokens'
)

/**
 * Tool definition for Responses API
 */
data class Tool(
    val type: String  // "web_search", "x_search", "code_execution", etc.
)

/**
 * Streaming event from Responses API
 */
data class ResponseStreamEvent(
    // Event metadata
    val type: String? = null,
    
    // For text deltas
    val delta: String? = null,
    val index: Int? = null,
    
    // For tool calls
    @SerializedName("call_id")
    val callId: String? = null,
    val status: String? = null,
    val results: List<SearchResult>? = null,
    
    // For response metadata
    val id: String? = null,
    val response: ResponseMetadata? = null,
    
    // For output items
    @SerializedName("output_item")
    val outputItem: OutputItem? = null,
    
    // For content parts
    @SerializedName("content_part")
    val contentPart: ContentPart? = null
)

data class ResponseMetadata(
    val id: String? = null,
    val status: String? = null,
    val model: String? = null
)

data class OutputItem(
    val id: String? = null,
    val type: String? = null,
    val status: String? = null
)

data class ContentPart(
    val type: String? = null,
    val text: String? = null
)

data class SearchResult(
    val title: String? = null,
    val url: String? = null,
    val snippet: String? = null
)

/**
 * Embedding API models
 */

/**
 * Request for creating embeddings
 */
data class EmbeddingRequest(
    val input: Any, // String or List<String>
    val model: String,
    @SerializedName("encoding_format")
    val encodingFormat: String? = "float" // "float" or "base64"
)

/**
 * Response from embeddings API
 */
data class EmbeddingResponse(
    val `object`: String, // "list"
    val data: List<EmbeddingData>,
    val model: String,
    val usage: EmbeddingUsage
)

data class EmbeddingData(
    val `object`: String, // "embedding"
    val embedding: List<Float>,
    val index: Int
)

data class EmbeddingUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)
