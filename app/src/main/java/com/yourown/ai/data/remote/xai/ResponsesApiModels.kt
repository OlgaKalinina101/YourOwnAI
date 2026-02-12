package com.yourown.ai.data.remote.xai

import com.google.gson.annotations.SerializedName

/**
 * xAI (Grok) Responses API models
 * https://docs.x.ai/developers/model-capabilities/text/generate-text
 * https://docs.x.ai/developers/tools
 */

/**
 * Request for Responses API (note: uses 'input' instead of 'messages')
 */
data class ResponsesApiRequest(
    val model: String,
    val input: List<com.yourown.ai.data.remote.deepseek.ChatMessage>,
    val tools: List<XAITool>? = null,
    val stream: Boolean = true,
    val temperature: Float? = null,
    @SerializedName("top_p")
    val top_p: Float? = null,
    @SerializedName("max_output_tokens")
    val max_output_tokens: Int? = null,
    val store: Boolean = false // Don't store conversation history on xAI servers
)

/**
 * Request for Responses API with multimodal support
 */
data class ResponsesApiMultimodalRequest(
    val model: String,
    val input: List<ResponseInputMessage>,
    val tools: List<XAITool>? = null,
    val stream: Boolean = true,
    val temperature: Float? = null,
    @SerializedName("top_p")
    val top_p: Float? = null,
    @SerializedName("max_output_tokens")
    val max_output_tokens: Int? = null,
    val store: Boolean = false
)

/**
 * Input message for Responses API (can contain text and images)
 * content can be:
 * - String for simple text messages
 * - List<ResponseContentPart> for multimodal messages (with images)
 */
data class ResponseInputMessage(
    val role: String,
    val content: Any // String or List<ResponseContentPart>
)

/**
 * Content part for multimodal input
 */
data class ResponseContentPart(
    val type: String, // "text" or "image_url"
    val text: String? = null,
    @SerializedName("image_url")
    val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    val url: String,
    val detail: String? = "auto"
)

/**
 * Tool definition for xAI Responses API
 * Supports: web_search, x_search, code_execution
 */
data class XAITool(
    val type: String,  // "web_search", "x_search", "code_execution"
    
    // For x_search
    @SerializedName("allowed_x_handles")
    val allowedXHandles: List<String>? = null,
    @SerializedName("excluded_x_handles")
    val excludedXHandles: List<String>? = null,
    @SerializedName("from_date")
    val fromDate: String? = null,
    @SerializedName("to_date")
    val toDate: String? = null,
    @SerializedName("enable_image_understanding")
    val enableImageUnderstanding: Boolean? = null,
    @SerializedName("enable_video_understanding")
    val enableVideoUnderstanding: Boolean? = null,
    
    // For web_search
    @SerializedName("allowed_domains")
    val allowedDomains: List<String>? = null,
    @SerializedName("excluded_domains")
    val excludedDomains: List<String>? = null
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
    val snippet: String? = null,
    val handle: String? = null  // For X search results
)
