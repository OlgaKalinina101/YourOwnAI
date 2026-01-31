package com.yourown.ai.domain.model

/**
 * File attachment metadata
 */
data class FileAttachment(
    val path: String,          // Local file path
    val name: String,          // Original file name
    val type: String,          // File extension: pdf, txt, doc, docx
    val sizeBytes: Long = 0    // File size in bytes
)

/**
 * Helper for file attachment types
 */
object FileAttachmentType {
    const val PDF = "pdf"
    const val TXT = "txt"
    const val DOC = "doc"
    const val DOCX = "docx"
    
    fun isSupported(extension: String): Boolean {
        return extension.lowercase() in listOf(PDF, TXT, DOC, DOCX)
    }
    
    fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            PDF -> "application/pdf"
            TXT -> "text/plain"
            DOC -> "application/msword"
            DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
    }
}
