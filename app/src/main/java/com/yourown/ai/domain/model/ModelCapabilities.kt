package com.yourown.ai.domain.model

/**
 * Multimodal capabilities for AI models
 * Defines support for images, documents, and attachments
 */
data class ModelCapabilities(
    val supportsVision: Boolean = false,
    val supportsDocuments: Boolean = false,
    val supportsWebSearch: Boolean = false,
    val imageSupport: ImageSupport? = null,
    val documentSupport: DocumentSupport? = null,
    val totalAttachmentsLimit: Int = 0,
    val notes: String = ""
) {
    companion object {
        /**
         * Get capabilities for a specific model
         */
        fun forModel(modelId: String): ModelCapabilities {
            return when (modelId.lowercase()) {
                // OpenAI Models
                "gpt-5.2" -> ModelCapabilities(
                    supportsVision = true,
                    supportsDocuments = true,
                    supportsWebSearch = true,
                    imageSupport = ImageSupport(
                        maxImages = 500,
                        supportedFormats = listOf("jpeg", "jpg", "png", "gif", "webp"),
                        maxSizePerImageMB = 50,
                        maxTotalPayloadMB = 50,
                        supportsDetail = true
                    ),
                    documentSupport = DocumentSupport(
                        maxDocuments = 50,
                        supportedFormats = listOf("pdf", "txt", "doc", "docx"),
                        maxSizePerDocumentMB = 50
                    ),
                    totalAttachmentsLimit = 500,
                    notes = "GPT-5.2: Up to 500 images or files, 50MB total payload. Web search via Responses API."
                )
                
                // GPT-5.1 - Coding focused with vision and PDF support
                "gpt-5.1" -> ModelCapabilities(
                    supportsVision = true,
                    supportsDocuments = true,
                    supportsWebSearch = true,
                    imageSupport = ImageSupport(
                        maxImages = 500,
                        maxSizePerImageMB = 50,
                        maxTotalPayloadMB = 50,
                        supportsDetail = true
                    ),
                    documentSupport = DocumentSupport(
                        maxDocuments = 50,
                        supportedFormats = listOf("pdf", "txt", "doc", "docx"),
                        maxSizePerDocumentMB = 50
                    ),
                    totalAttachmentsLimit = 500,
                    notes = "GPT-5.1: Up to 500 images or files, 50MB total payload. Web search via Responses API."
                )
                
                // GPT-4o - Fast multimodal with PDF support
                "gpt-4o-2024-08-06", "gpt-4o" -> ModelCapabilities(
                    supportsVision = true,
                    supportsDocuments = true,
                    supportsWebSearch = true,
                    imageSupport = ImageSupport(
                        maxImages = 500,
                        maxSizePerImageMB = 50,
                        maxTotalPayloadMB = 50,
                        supportsDetail = true
                    ),
                    documentSupport = DocumentSupport(
                        maxDocuments = 50,
                        supportedFormats = listOf("pdf", "txt"),
                        maxSizePerDocumentMB = 50
                    ),
                    totalAttachmentsLimit = 500,
                    notes = "GPT-4o: Up to 500 images or files, 50MB total payload. Web search via Responses API."
                )

                // GPT-4.1 - Fast multimodal only image support
                "gpt-4.1-2025-04-14", "gpt-4.1" -> ModelCapabilities(
                    supportsVision = true,
                    supportsDocuments = false,
                    supportsWebSearch = true,
                    imageSupport = ImageSupport(
                        maxImages = 500,
                        maxSizePerImageMB = 50,
                        maxTotalPayloadMB = 50,
                        supportsDetail = true
                    ),
                    totalAttachmentsLimit = 500,
                    notes = "GPT-4.1: Up to 500 images, 50MB total payload. Web search via Responses API."
                )
                
                // DeepSeek Models - Direct API
                "deepseek-chat" -> ModelCapabilities(
                    supportsVision = false,
                    supportsDocuments = false,
                    notes = "DeepSeek Chat (V3.2): Text only. Fast non-thinking mode. 163K context."
                )
                
                "deepseek-reasoner" -> ModelCapabilities(
                    supportsVision = false,
                    supportsDocuments = false,
                    notes = "DeepSeek Reasoner (V3.2): Text only. Thinking mode with chain-of-thought. 163K context."
                )
                
                // DeepSeek Models via OpenRouter - with vision support in Exp
                "deepseek/deepseek-v3.2-exp" -> ModelCapabilities(
                    supportsVision = true,
                    supportsDocuments = false, // Images only, no PDF confirmed
                    supportsWebSearch = true,
                    imageSupport = ImageSupport(
                        maxImages = 10, // Conservative estimate based on multimodal capabilities
                        supportedFormats = listOf("jpeg", "jpg", "png", "gif", "webp"),
                        maxSizePerImageMB = 20, // Conservative estimate
                        maxTotalPayloadMB = 100,
                        supportsDetail = true // Advanced multimodal with layered architecture
                    ),
                    totalAttachmentsLimit = 10,
                    notes = "DeepSeek V3.2 Exp: Multimodal with vision. Screenshots, diagrams, tables, code. DSA for long context. Web search via :online. Via OpenRouter."
                )
                
                "deepseek/deepseek-v3.2", "deepseek/deepseek-v3.2-speciale" -> ModelCapabilities(
                    supportsVision = false,
                    supportsDocuments = false,
                    supportsWebSearch = true,
                    notes = "DeepSeek V3.2/Speciale: Text only. Gold-medal reasoning (IMO, IOI 2025). Thinking + tool-use. 163K context. Web search via :online."
                )
                
                // Claude Models via OpenRouter - Full multimodal support
                "anthropic/claude-sonnet-4.5", "anthropic/claude-opus-4.5",
                "anthropic/claude-opus-4.6",
                "anthropic/claude-haiku-4.5", "anthropic/claude-sonnet-4",
                "anthropic/claude-3.7-sonnet", "anthropic/claude-3.5-haiku" -> ModelCapabilities(
                    supportsVision = true,
                    supportsDocuments = true,
                    supportsWebSearch = true,
                    imageSupport = ImageSupport(
                        maxImages = 100, // API limit: 100 images per request
                        supportedFormats = listOf("jpeg", "jpg", "png", "gif", "webp"),
                        maxSizePerImageMB = 30, // Practical limit (32MB request total)
                        maxTotalPayloadMB = 32, // Total request size limit
                        supportsDetail = true // Supports high quality analysis
                    ),
                    documentSupport = DocumentSupport(
                        maxDocuments = 10, // Reasonable limit within 32MB and 100 pages
                        supportedFormats = listOf("pdf"), // Native PDF support
                        maxSizePerDocumentMB = 32, // Per-request limit
                        requiresOCR = false // Claude processes text + images from PDF
                    ),
                    totalAttachmentsLimit = 100,
                    notes = "Claude: Up to 100 images (8000x8000px each) or PDFs (100 pages). 32MB total request size. Web search via :online. Via OpenRouter."
                )
                
                // Llama 4 Models via OpenRouter - Native multimodal support
                "meta-llama/llama-4-maverick", "meta-llama/llama-4-scout" -> ModelCapabilities(
                    supportsVision = true,
                    supportsDocuments = false, // No PDF support confirmed yet
                    supportsWebSearch = true,
                    imageSupport = ImageSupport(
                        maxImages = 10, // Up to 10 images per request (tested up to 8, supports 10)
                        supportedFormats = listOf("jpeg", "jpg", "png", "gif", "webp"),
                        maxSizePerImageMB = 20, // Conservative estimate
                        maxTotalPayloadMB = 100, // Reasonable total payload
                        supportsDetail = true // Native multimodal with early fusion
                    ),
                    totalAttachmentsLimit = 10,
                    notes = "Llama 4: Up to 10 images. Native multimodal with early fusion. Pre-trained on 48 images. Web search via :online. Via OpenRouter."
                )

                // l3.1-euryale-70b - via OpenRouter
                "sao10k/l3.1-euryale-70b" -> ModelCapabilities(
                    supportsVision = false,
                    supportsDocuments = false,
                    supportsWebSearch = true,
                    notes = "l3.1-euryale-70b: Text only. Focused on creative roleplay. 32.8K context. Web search via :online."
                )

                // hermes-3-llama-3.1-70b - via OpenRouter
                "nousresearch/hermes-3-llama-3.1-70b" -> ModelCapabilities(
                    supportsVision = false,
                    supportsDocuments = false,
                    supportsWebSearch = true,
                    notes = "Focused on aligning LLMs to the user, with powerful steering capabilities and control given to the end user. Web search via :online."
                )

                // Cohere: Command R+ (08-2024) - via OpenRouter
                "cohere/command-r-plus-08-2024" -> ModelCapabilities(
                    supportsVision = false,
                    supportsDocuments = false,
                    supportsWebSearch = true,
                    notes = "It's useful for roleplay, general consumer usecases, and Retrieval Augmented Generation (RAG). 128,000 context. Web search via :online."
                )

                // Mistral Large - via OpenRouter
                "mistralai/mistral-large" -> ModelCapabilities(
                    supportsVision = false,
                    supportsDocuments = false,
                    supportsWebSearch = true,
                    notes = "It's a proprietary weights-available model and excels at reasoning, code, JSON, chat, and more. Web search via :online."
                )

                // Qwen3 Max - via OpenRouter
                "qwen/qwen3-max" -> ModelCapabilities(
                    supportsVision = false,
                    supportsDocuments = false,
                    supportsWebSearch = true,
                    notes = "major improvements in reasoning, instruction following, multilingual support, and long-tail knowledge coverage compared to the January 2025 version. Web search via :online."
                )

                // Gemini 3 & 2.5 Models via OpenRouter - Full multimodal support
                "google/gemini-3-pro-preview", "google/gemini-3-flash-preview",
                "google/gemini-2.5-pro", "google/gemini-2.5-flash" -> ModelCapabilities(
                    supportsVision = true,
                    supportsDocuments = true, // Full PDF support
                    supportsWebSearch = true,
                    imageSupport = ImageSupport(
                        maxImages = 3000, // Vertex AI batch: up to 3,000 files. Consumer: 10
                        supportedFormats = listOf("jpeg", "jpg", "png", "gif", "webp"),
                        maxSizePerImageMB = 100, // Increased from 20MB to 100MB per file
                        maxTotalPayloadMB = 100, // Total inline data: 20MB. File API: 2GB per file
                        supportsDetail = true // Advanced multimodal capabilities
                    ),
                    documentSupport = DocumentSupport(
                        maxDocuments = 10, // Consumer limit per prompt
                        supportedFormats = listOf("pdf", "txt", "doc", "docx", "md", "csv"), // Wide format support
                        maxSizePerDocumentMB = 100, // Up to 100MB per file via File API
                        requiresOCR = false // Native document processing
                    ),
                    totalAttachmentsLimit = 10, // Consumer: 10 files per prompt. Enterprise: 3,000
                    notes = "Gemini: Up to 10 files (100MB each). PDF support up to 30MB/2000 pages. Text, images, audio, video. Web search via :online. Via OpenRouter."
                )
                
                // OpenRouter GPT-4o via OpenRouter proxy
                "openai/gpt-4o-2024-05-13" -> ModelCapabilities(
                    supportsVision = true,
                    supportsDocuments = true,
                    supportsWebSearch = true,
                    imageSupport = ImageSupport(
                        maxImages = 500,
                        supportedFormats = listOf("jpeg", "jpg", "png", "gif", "webp"),
                        maxSizePerImageMB = 50,
                        maxTotalPayloadMB = 50,
                        supportsDetail = true
                    ),
                    documentSupport = DocumentSupport(
                        maxDocuments = 50,
                        supportedFormats = listOf("pdf", "txt"),
                        maxSizePerDocumentMB = 50
                    ),
                    totalAttachmentsLimit = 500,
                    notes = "GPT-4o (OpenRouter): Same as OpenAI direct. Up to 500 images or files, 50MB total. Web search via :online."
                )
                
                // x.ai Grok Models - Full multimodal support
                "grok-4-1-fast-reasoning", "grok-4-1-fast-non-reasoning",
                "grok-code-fast-1", "grok-4-fast-reasoning", 
                "grok-4-fast-non-reasoning", "grok-4-0709",
                "grok-3-mini", "grok-3", "grok-4", "grok-4-1" -> ModelCapabilities(
                    supportsVision = true,
                    supportsDocuments = true,
                    supportsWebSearch = true,
                    imageSupport = ImageSupport(
                        maxImages = Int.MAX_VALUE, // No limit according to docs
                        supportedFormats = listOf("jpg", "jpeg", "png"),
                        maxSizePerImageMB = 20, // Max 20MB per image
                        maxTotalPayloadMB = 100, // Generous total
                        supportsDetail = true // Supports "high" detail
                    ),
                    documentSupport = DocumentSupport(
                        maxDocuments = 50, // Reasonable limit for Files API
                        supportedFormats = listOf("pdf", "txt", "md", "csv", "json", "py", "js", "java", "kt"),
                        maxSizePerDocumentMB = 48 // Files API limit: 48MB per file
                    ),
                    totalAttachmentsLimit = Int.MAX_VALUE,
                    notes = "Grok: Unlimited images (20MB each), 48MB files (PDF, TXT, code, etc.). Web search & X search via Responses API."
                )
                
                // Default: no multimodal support
                else -> ModelCapabilities(
                    supportsVision = false,
                    supportsDocuments = false,
                    notes = "No multimodal support for this model"
                )
            }
        }
        
        /**
         * Check if model supports any attachments (images or documents)
         */
        fun supportsAttachments(model: ModelProvider): Boolean {
            val modelId = when (model) {
                is ModelProvider.Local -> return false // Local models don't support attachments yet
                is ModelProvider.API -> model.modelId
            }
            val capabilities = forModel(modelId)
            return capabilities.supportsVision || capabilities.supportsDocuments
        }
    }
}

/**
 * Image support details
 */
data class ImageSupport(
    val maxImages: Int,
    val supportedFormats: List<String> = listOf("jpeg", "jpg", "png", "gif", "webp"),
    val maxSizePerImageMB: Int,
    val maxTotalPayloadMB: Int,
    val supportsDetail: Boolean = false
)

/**
 * Document support details
 */
data class DocumentSupport(
    val maxDocuments: Int,
    val supportedFormats: List<String>,
    val maxSizePerDocumentMB: Int,
    val requiresOCR: Boolean = false
)
