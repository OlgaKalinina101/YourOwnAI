package com.yourown.ai.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects for Supabase sync
 * These match the Supabase table structure (snake_case)
 */

@Serializable
data class ConversationDto(
    val id: String,
    val title: String,
    val created_at: Long,
    val updated_at: Long,
    val model: String? = null,
    val temperature: Float? = null,
    val max_tokens: Int? = null,
    val source_conversation_id: String? = null,
    val persona_id: String? = null,
    val device_id: String? = null,
    val synced_at: Long? = null
)

@Serializable
data class MessageDto(
    val id: String,
    val conversation_id: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val model: String? = null,
    val user_context: String? = null,
    val persona_id: String? = null,
    val swipe_message_text: String? = null,
    val image_attachments: String? = null,
    val file_attachments: String? = null,
    val is_liked: Boolean = false,
    val device_id: String? = null,
    val synced_at: Long? = null
)

@Serializable
data class MemoryDto(
    val id: String,
    val conversation_id: String,
    val message_id: String,
    val fact: String,
    val created_at: Long,
    val persona_id: String? = null,
    val embedding: ByteArray? = null, // BYTEA in PostgreSQL
    val device_id: String? = null,
    val synced_at: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryDto

        if (id != other.id) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}

@Serializable
data class PersonaDto(
    val id: String,
    val system_prompt_id: String,
    val name: String,
    val description: String? = null,
    val is_for_api: Boolean = true,
    
    // AI Configuration
    val temperature: Float = 0.7f,
    val top_p: Float = 0.9f,
    val max_tokens: Int = 4096,
    val deep_empathy: Boolean = false,
    val memory_enabled: Boolean = false,
    val rag_enabled: Boolean = false,
    val message_history_limit: Int = 10,
    
    // Prompts
    val deep_empathy_prompt: String? = null,
    val deep_empathy_analysis_prompt: String? = null,
    val memory_extraction_prompt: String? = null,
    val context_instructions: String? = null,
    val memory_instructions: String? = null,
    val rag_instructions: String? = null,
    val swipe_message_prompt: String? = null,
    
    // Memory Configuration
    val memory_limit: Int = 5,
    val memory_min_age_days: Int = 2,
    val memory_title: String? = null,
    
    // RAG Configuration
    val rag_chunk_size: Int = 512,
    val rag_chunk_overlap: Int = 64,
    val rag_chunk_limit: Int = 5,
    val rag_title: String? = null,
    
    // Model Preference
    val preferred_model_id: String? = null,
    val preferred_provider: String? = null,
    
    // Document Links
    val linked_document_ids: String? = null,
    
    // Memory Scope
    val use_only_persona_memories: Boolean = false,
    val share_memories_globally: Boolean = true,
    
    val created_at: Long,
    val updated_at: Long,
    val device_id: String? = null,
    val synced_at: Long? = null
)

@Serializable
data class SystemPromptDto(
    val id: String,
    val name: String,
    val content: String,
    val type: String,
    val is_default: Boolean = false,
    val created_at: Long,
    val updated_at: Long,
    val device_id: String? = null,
    val synced_at: Long? = null
)

@Serializable
data class KnowledgeDocumentDto(
    val id: String,
    val name: String,
    val content: String,
    val created_at: Long,
    val updated_at: Long,
    val linked_persona_ids: String? = null,
    val device_id: String? = null,
    val synced_at: Long? = null
)

@Serializable
data class DocumentEmbeddingDto(
    val id: String,
    val document_id: String,
    val chunk_text: String,
    val chunk_index: Int,
    val embedding: ByteArray? = null,
    val device_id: String? = null,
    val synced_at: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocumentEmbeddingDto

        if (id != other.id) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}
