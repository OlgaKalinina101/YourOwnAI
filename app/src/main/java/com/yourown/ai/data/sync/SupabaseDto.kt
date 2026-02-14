package com.yourown.ai.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects for Supabase sync (Optimized)
 * These match the Supabase table structure (snake_case)
 * 
 * Optimized for Free Tier:
 * - Removed System Prompts (stored locally)
 * - Removed Knowledge Documents (RAG stored locally)
 * - Removed Document Embeddings (RAG stored locally)
 * - Removed embeddings from Memories (generated locally)
 */

@Serializable
data class ConversationDto(
    val id: String,
    val title: String,
    val model: String,
    val provider: String,
    val created_at: Long,
    val updated_at: Long,
    val is_archived: Boolean = false,
    val persona_id: String? = null,
    val source_conversation_id: String? = null,
    val device_id: String? = null,
    val synced_at: Long? = null
)

@Serializable
data class MessageDto(
    val id: String,
    val conversation_id: String,
    val role: String,
    val content: String,
    val created_at: Long,
    val model: String? = null,
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
    val device_id: String? = null,
    val synced_at: Long? = null
)

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
    
    // API Embeddings Configuration
    val use_api_embeddings: Boolean = false,
    val api_embeddings_provider: String = "openai",
    val api_embeddings_model: String = "text-embedding-3-small",
    
    val created_at: Long,
    val updated_at: Long,
    val device_id: String? = null,
    val synced_at: Long? = null
)
