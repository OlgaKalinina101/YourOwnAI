package com.yourown.ai.data.sync

import com.yourown.ai.data.local.entity.*
import com.yourown.ai.domain.model.*

/**
 * Mappers between Supabase DTOs and local entities (Optimized)
 * 
 * Optimized for Free Tier - removed:
 * - System Prompts (stored locally)
 * - Knowledge Documents (RAG stored locally)
 * - Document Embeddings (RAG stored locally)
 * - Embeddings from Memories (generated locally)
 */

// ===== ConversationDto ↔ ConversationEntity =====

fun ConversationDto.toEntity(): ConversationEntity {
    return ConversationEntity(
        id = id,
        title = title,
        createdAt = created_at,
        updatedAt = updated_at,
        model = model,
        provider = provider,
        systemPrompt = "", // Legacy field
        systemPromptId = null,
        personaId = persona_id,
        sourceConversationId = source_conversation_id,
        isPinned = false,
        isArchived = is_archived
    )
}

fun ConversationEntity.toDto(deviceId: String): ConversationDto {
    return ConversationDto(
        id = id,
        title = title,
        model = model,
        provider = provider,
        created_at = createdAt,
        updated_at = updatedAt,
        is_archived = isArchived,
        source_conversation_id = sourceConversationId,
        persona_id = personaId,
        device_id = deviceId,
        synced_at = System.currentTimeMillis()
    )
}

// ===== MessageDto ↔ MessageEntity =====

fun MessageDto.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        conversationId = conversation_id,
        role = role,
        content = content,
        createdAt = created_at,
        model = model,
        swipeMessageText = swipe_message_text,
        imageAttachments = image_attachments,
        fileAttachments = file_attachments,
        isLiked = is_liked
    )
}

fun MessageEntity.toDto(deviceId: String): MessageDto {
    return MessageDto(
        id = id,
        conversation_id = conversationId,
        role = role,
        content = content,
        created_at = createdAt,
        model = model,
        swipe_message_text = swipeMessageText,
        image_attachments = imageAttachments,
        file_attachments = fileAttachments,
        is_liked = isLiked,
        device_id = deviceId,
        synced_at = System.currentTimeMillis()
    )
}

// ===== MemoryDto ↔ MemoryEntity (WITHOUT embeddings) =====

fun MemoryDto.toEntity(): MemoryEntity {
    return MemoryEntity(
        id = id,
        conversationId = conversation_id,
        messageId = message_id,
        fact = fact,
        createdAt = created_at,
        embedding = null, // Embeddings not synced - generated locally
        personaId = persona_id,
        isArchived = false
    )
}

fun MemoryEntity.toDto(deviceId: String): MemoryDto {
    // Note: embeddings are NOT synced to save space
    return MemoryDto(
        id = id,
        conversation_id = conversationId,
        message_id = messageId,
        fact = fact,
        created_at = createdAt,
        persona_id = personaId,
        device_id = deviceId,
        synced_at = System.currentTimeMillis()
    )
}

// ===== PersonaDto ↔ PersonaEntity =====

fun PersonaDto.toEntity(systemPrompt: String): PersonaEntity {
    return PersonaEntity(
        id = id,
        name = name,
        description = description ?: "",
        systemPromptId = system_prompt_id,
        systemPrompt = systemPrompt,
        isForApi = is_for_api,
        // AI Configuration
        temperature = temperature,
        topP = top_p,
        maxTokens = max_tokens,
        deepEmpathy = deep_empathy,
        memoryEnabled = memory_enabled,
        ragEnabled = rag_enabled,
        messageHistoryLimit = message_history_limit,
        // Prompts
        deepEmpathyPrompt = deep_empathy_prompt ?: "",
        deepEmpathyAnalysisPrompt = deep_empathy_analysis_prompt ?: "",
        memoryExtractionPrompt = memory_extraction_prompt ?: "",
        contextInstructions = context_instructions ?: "",
        memoryInstructions = memory_instructions ?: "",
        ragInstructions = rag_instructions ?: "",
        swipeMessagePrompt = swipe_message_prompt ?: "",
        // Memory Configuration
        memoryLimit = memory_limit,
        memoryMinAgeDays = memory_min_age_days,
        memoryTitle = memory_title ?: "Твои воспоминания",
        // RAG Configuration
        ragChunkSize = rag_chunk_size,
        ragChunkOverlap = rag_chunk_overlap,
        ragChunkLimit = rag_chunk_limit,
        ragTitle = rag_title ?: "Твоя библиотека текстов",
        // Model Preference
        preferredModelId = preferred_model_id,
        preferredProvider = preferred_provider,
        // Document Links
        linkedDocumentIds = linked_document_ids ?: "[]",
        // Memory Scope
        useOnlyPersonaMemories = use_only_persona_memories,
        shareMemoriesGlobally = share_memories_globally,
        // API Embeddings Configuration
        useApiEmbeddings = use_api_embeddings,
        apiEmbeddingsProvider = api_embeddings_provider,
        apiEmbeddingsModel = api_embeddings_model,
        createdAt = created_at,
        updatedAt = updated_at
    )
}

fun PersonaEntity.toDto(deviceId: String): PersonaDto {
    return PersonaDto(
        id = id,
        system_prompt_id = systemPromptId,
        name = name,
        description = description,
        is_for_api = isForApi,
        // AI Configuration
        temperature = temperature,
        top_p = topP,
        max_tokens = maxTokens,
        deep_empathy = deepEmpathy,
        memory_enabled = memoryEnabled,
        rag_enabled = ragEnabled,
        message_history_limit = messageHistoryLimit,
        // Prompts
        deep_empathy_prompt = deepEmpathyPrompt,
        deep_empathy_analysis_prompt = deepEmpathyAnalysisPrompt,
        memory_extraction_prompt = memoryExtractionPrompt,
        context_instructions = contextInstructions,
        memory_instructions = memoryInstructions,
        rag_instructions = ragInstructions,
        swipe_message_prompt = swipeMessagePrompt,
        // Memory Configuration
        memory_limit = memoryLimit,
        memory_min_age_days = memoryMinAgeDays,
        memory_title = memoryTitle,
        // RAG Configuration
        rag_chunk_size = ragChunkSize,
        rag_chunk_overlap = ragChunkOverlap,
        rag_chunk_limit = ragChunkLimit,
        rag_title = ragTitle,
        // Model Preference
        preferred_model_id = preferredModelId,
        preferred_provider = preferredProvider,
        // Document Links
        linked_document_ids = linkedDocumentIds,
        // Memory Scope
        use_only_persona_memories = useOnlyPersonaMemories,
        share_memories_globally = shareMemoriesGlobally,
        // API Embeddings Configuration
        use_api_embeddings = useApiEmbeddings,
        api_embeddings_provider = apiEmbeddingsProvider,
        api_embeddings_model = apiEmbeddingsModel,
        created_at = createdAt,
        updated_at = updatedAt,
        device_id = deviceId,
        synced_at = System.currentTimeMillis()
    )
}
