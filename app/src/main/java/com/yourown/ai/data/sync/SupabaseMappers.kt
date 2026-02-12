package com.yourown.ai.data.sync

import com.yourown.ai.data.local.entity.*
import com.yourown.ai.domain.model.*

/**
 * Mappers between Supabase DTOs and local entities
 */

// ===== ConversationDto ↔ ConversationEntity =====

fun ConversationDto.toEntity(): ConversationEntity {
    return ConversationEntity(
        id = id,
        title = title,
        createdAt = created_at,
        updatedAt = updated_at,
        model = model ?: "",
        provider = "openai", // Default, will be updated on use
        systemPrompt = "", // Legacy field
        systemPromptId = null,
        personaId = persona_id,
        sourceConversationId = source_conversation_id,
        isPinned = false,
        isArchived = false
    )
}

fun ConversationEntity.toDto(deviceId: String): ConversationDto {
    return ConversationDto(
        id = id,
        title = title,
        created_at = createdAt,
        updated_at = updatedAt,
        model = model,
        temperature = null,
        max_tokens = null,
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
        createdAt = timestamp,
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
        timestamp = createdAt,
        model = model,
        user_context = null,
        persona_id = null,
        swipe_message_text = swipeMessageText,
        image_attachments = imageAttachments,
        file_attachments = fileAttachments,
        is_liked = isLiked,
        device_id = deviceId,
        synced_at = System.currentTimeMillis()
    )
}

// ===== MemoryDto ↔ MemoryEntity =====

fun MemoryDto.toEntity(): MemoryEntity {
    // Convert ByteArray embedding to String (comma-separated floats)
    val embeddingString = embedding?.let { bytes ->
        // Parse ByteArray as floats and convert to comma-separated string
        val floats = bytes.toList().chunked(4).map { chunk ->
            if (chunk.size == 4) {
                java.nio.ByteBuffer.wrap(chunk.toByteArray()).float
            } else {
                0f
            }
        }
        floats.joinToString(",")
    }
    
    return MemoryEntity(
        id = id,
        conversationId = conversation_id,
        messageId = message_id,
        fact = fact,
        createdAt = created_at,
        embedding = embeddingString,
        personaId = persona_id,
        isArchived = false
    )
}

fun MemoryEntity.toDto(deviceId: String): MemoryDto {
    // Convert comma-separated floats to ByteArray
    val embeddingBytes = embedding?.let { str ->
        val floats = str.split(",").mapNotNull { it.toFloatOrNull() }
        val buffer = java.nio.ByteBuffer.allocate(floats.size * 4)
        floats.forEach { buffer.putFloat(it) }
        buffer.array()
    }
    
    return MemoryDto(
        id = id,
        conversation_id = conversationId,
        message_id = messageId,
        fact = fact,
        created_at = createdAt,
        persona_id = personaId,
        embedding = embeddingBytes,
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

// ===== SystemPromptDto ↔ SystemPromptEntity =====

fun SystemPromptDto.toEntity(): SystemPromptEntity {
    return SystemPromptEntity(
        id = id,
        name = name,
        content = content,
        promptType = type,
        isDefault = is_default,
        createdAt = created_at,
        updatedAt = updated_at,
        usageCount = 0
    )
}

fun SystemPromptEntity.toDto(deviceId: String): SystemPromptDto {
    return SystemPromptDto(
        id = id,
        name = name,
        content = content,
        type = promptType,
        is_default = isDefault,
        created_at = createdAt,
        updated_at = updatedAt,
        device_id = deviceId,
        synced_at = System.currentTimeMillis()
    )
}

// ===== KnowledgeDocumentDto ↔ KnowledgeDocumentEntity =====

fun KnowledgeDocumentDto.toEntity(): KnowledgeDocumentEntity {
    return KnowledgeDocumentEntity(
        id = id,
        name = name,
        content = content,
        createdAt = created_at,
        updatedAt = updated_at,
        linkedPersonaIds = linked_persona_ids ?: "[]"
    )
}

fun KnowledgeDocumentEntity.toDto(deviceId: String): KnowledgeDocumentDto {
    return KnowledgeDocumentDto(
        id = id,
        name = name,
        content = content,
        created_at = createdAt,
        updated_at = updatedAt,
        linked_persona_ids = linkedPersonaIds,
        device_id = deviceId,
        synced_at = System.currentTimeMillis()
    )
}

// ===== DocumentEmbeddingDto ↔ DocumentChunkEntity =====

fun DocumentEmbeddingDto.toEntity(): DocumentChunkEntity {
    // Convert ByteArray embedding to JSON string
    val embeddingString = embedding?.let { bytes ->
        val floats = bytes.toList().chunked(4).map { chunk ->
            if (chunk.size == 4) {
                java.nio.ByteBuffer.wrap(chunk.toByteArray()).float
            } else {
                0f
            }
        }
        com.google.gson.Gson().toJson(floats)
    }
    
    return DocumentChunkEntity(
        id = id,
        documentId = document_id,
        content = chunk_text,
        chunkIndex = chunk_index,
        embedding = embeddingString
    )
}

fun DocumentChunkEntity.toDto(deviceId: String): DocumentEmbeddingDto {
    // Convert JSON string to ByteArray
    val embeddingBytes = embedding?.let { json ->
        try {
            val floats = com.google.gson.Gson().fromJson(json, Array<Float>::class.java)
            val buffer = java.nio.ByteBuffer.allocate(floats.size * 4)
            floats.forEach { buffer.putFloat(it) }
            buffer.array()
        } catch (e: Exception) {
            null
        }
    }
    
    return DocumentEmbeddingDto(
        id = id,
        document_id = documentId,
        chunk_text = content,
        chunk_index = chunkIndex,
        embedding = embeddingBytes,
        device_id = deviceId,
        synced_at = System.currentTimeMillis()
    )
}
