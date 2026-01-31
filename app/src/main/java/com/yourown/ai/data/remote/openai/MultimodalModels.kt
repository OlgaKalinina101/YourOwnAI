package com.yourown.ai.data.remote.openai

/**
 * OpenAI API models with multimodal support (images, PDFs)
 * https://platform.openai.com/docs/guides/vision
 * https://platform.openai.com/docs/guides/pdf-files
 */

/**
 * Content part for multimodal messages
 */
sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class ImageUrl(val image_url: ImageUrlContent) : MessageContent()
    data class ImageFile(val image_url: String) : MessageContent() // Base64 data URL
}

/**
 * Image URL content with optional detail parameter
 */
data class ImageUrlContent(
    val url: String, // Can be URL or base64 data URL: "data:image/jpeg;base64,..."
    val detail: String? = "auto" // "low", "high", or "auto"
)

/**
 * Multimodal chat message supporting text and images
 */
data class MultimodalChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: Any // String for simple text, List<Map<String, Any>> for multimodal
)

/**
 * Helper to build multimodal messages
 */
object MultimodalMessageBuilder {
    
    /**
     * Build a simple text message
     */
    fun buildTextMessage(role: String, text: String): MultimodalChatMessage {
        return MultimodalChatMessage(role = role, content = text)
    }
    
    /**
     * Build a multimodal message with text and images
     */
    fun buildMultimodalMessage(
        role: String,
        text: String,
        imageBase64List: List<String> = emptyList(),
        imageDetail: String = "auto"
    ): MultimodalChatMessage {
        val contentParts = mutableListOf<Map<String, Any>>()
        
        // Add text part
        contentParts.add(mapOf("type" to "text", "text" to text))
        
        // Add image parts
        imageBase64List.forEach { base64 ->
            contentParts.add(
                mapOf(
                    "type" to "image_url",
                    "image_url" to mapOf(
                        "url" to "data:image/jpeg;base64,$base64",
                        "detail" to imageDetail
                    )
                )
            )
        }
        
        return MultimodalChatMessage(role = role, content = contentParts)
    }
    
    /**
     * Build a multimodal message with text, images, and files (PDFs, etc)
     */
    fun buildMultimodalMessageWithFiles(
        role: String,
        text: String,
        imageBase64List: List<String> = emptyList(),
        fileData: List<Pair<String, String>> = emptyList(), // Pair<base64, filename>
        imageDetail: String = "auto"
    ): MultimodalChatMessage {
        val contentParts = mutableListOf<Map<String, Any>>()
        
        // Add text part
        contentParts.add(mapOf("type" to "text", "text" to text))
        
        // Add image parts
        imageBase64List.forEach { base64 ->
            contentParts.add(
                mapOf(
                    "type" to "image_url",
                    "image_url" to mapOf(
                        "url" to "data:image/jpeg;base64,$base64",
                        "detail" to imageDetail
                    )
                )
            )
        }
        
        // Add file parts (PDF, TXT, DOC, DOCX)
        fileData.forEach { (base64, filename) ->
            val extension = filename.substringAfterLast('.', "pdf").lowercase()
            val mimeType = when (extension) {
                "pdf" -> "application/pdf"
                "txt" -> "text/plain"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                else -> "application/octet-stream"
            }
            
            contentParts.add(
                mapOf(
                    "type" to "input_file",
                    "filename" to filename,
                    "file_data" to "data:$mimeType;base64,$base64"
                )
            )
        }
        
        return MultimodalChatMessage(role = role, content = contentParts)
    }
}

/**
 * Chat completion request with multimodal support
 */
data class MultimodalChatCompletionRequest(
    val model: String,
    val messages: List<MultimodalChatMessage>,
    val temperature: Float? = null,
    val top_p: Float? = null,
    val max_tokens: Int? = null,
    val max_completion_tokens: Int? = null,
    val stream: Boolean = false
)
