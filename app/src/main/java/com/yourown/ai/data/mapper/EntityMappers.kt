package com.yourown.ai.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yourown.ai.data.local.entity.ConversationEntity
import com.yourown.ai.data.local.entity.KnowledgeDocumentEntity
import com.yourown.ai.data.local.entity.MessageEntity
import com.yourown.ai.data.local.entity.PersonaEntity
import com.yourown.ai.domain.model.Conversation
import com.yourown.ai.domain.model.KnowledgeDocument
import com.yourown.ai.domain.model.Message
import com.yourown.ai.domain.model.MessageRole
import com.yourown.ai.domain.model.Persona

/**
 * Mappers for converting between Entity and Domain models
 */

// Message Mappers
fun MessageEntity.toDomain(): Message {
    return Message(
        id = id,
        conversationId = conversationId,
        role = MessageRole.fromString(role),
        content = content,
        createdAt = createdAt,
        tokenCount = tokenCount,
        model = model,
        isError = isError,
        errorMessage = errorMessage,
        isLiked = isLiked,
        swipeMessageId = swipeMessageId,
        swipeMessageText = swipeMessageText,
        imageAttachments = imageAttachments,
        fileAttachments = fileAttachments,
        temperature = temperature,
        topP = topP,
        deepEmpathy = deepEmpathy,
        memoryEnabled = memoryEnabled,
        messageHistoryLimit = messageHistoryLimit,
        systemPrompt = systemPrompt,
        requestLogs = requestLogs
    )
}

fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.toStringValue(),
        content = content,
        createdAt = createdAt,
        tokenCount = tokenCount,
        model = model,
        isError = isError,
        errorMessage = errorMessage,
        isLiked = isLiked,
        swipeMessageId = swipeMessageId,
        swipeMessageText = swipeMessageText,
        imageAttachments = imageAttachments,
        fileAttachments = fileAttachments,
        temperature = temperature,
        topP = topP,
        deepEmpathy = deepEmpathy,
        memoryEnabled = memoryEnabled,
        messageHistoryLimit = messageHistoryLimit,
        systemPrompt = systemPrompt,
        requestLogs = requestLogs
    )
}

// Conversation Mappers
fun ConversationEntity.toDomain(messages: List<Message> = emptyList()): Conversation {
    return Conversation(
        id = id,
        title = title,
        systemPrompt = systemPrompt,
        systemPromptId = systemPromptId,
        personaId = personaId,
        model = model,
        provider = provider,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isArchived = isArchived,
        sourceConversationId = sourceConversationId,
        messages = messages
    )
}

fun Conversation.toEntity(): ConversationEntity {
    return ConversationEntity(
        id = id,
        title = title,
        systemPrompt = systemPrompt,
        systemPromptId = systemPromptId,
        personaId = null, // Note: personaId не хранится в Conversation domain model, управляется отдельно
        model = model,
        provider = provider,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isArchived = isArchived,
        sourceConversationId = sourceConversationId
    )
}

// Persona Mappers
fun PersonaEntity.toDomain(): Persona {
    val gson = Gson()
    val linkedDocumentIds = try {
        gson.fromJson<List<String>>(
            linkedDocumentIds,
            object : TypeToken<List<String>>() {}.type
        ) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
    
    return Persona(
        id = id,
        name = name,
        description = description,
        systemPromptId = systemPromptId,
        systemPrompt = systemPrompt,
        isForApi = isForApi,
        temperature = temperature,
        topP = topP,
        maxTokens = maxTokens,
        deepEmpathy = deepEmpathy,
        memoryEnabled = memoryEnabled,
        ragEnabled = ragEnabled,
        messageHistoryLimit = messageHistoryLimit,
        deepEmpathyPrompt = deepEmpathyPrompt,
        deepEmpathyAnalysisPrompt = deepEmpathyAnalysisPrompt,
        memoryExtractionPrompt = memoryExtractionPrompt,
        contextInstructions = contextInstructions,
        memoryInstructions = memoryInstructions,
        ragInstructions = ragInstructions,
        swipeMessagePrompt = swipeMessagePrompt,
        memoryLimit = memoryLimit,
        memoryMinAgeDays = memoryMinAgeDays,
        memoryTitle = memoryTitle,
        ragChunkSize = ragChunkSize,
        ragChunkOverlap = ragChunkOverlap,
        ragChunkLimit = ragChunkLimit,
        ragTitle = ragTitle,
        preferredModelId = preferredModelId,
        preferredProvider = preferredProvider,
        linkedDocumentIds = linkedDocumentIds,
        useOnlyPersonaMemories = useOnlyPersonaMemories,
        shareMemoriesGlobally = shareMemoriesGlobally,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Persona.toEntity(): PersonaEntity {
    val gson = Gson()
    val linkedDocumentIdsJson = gson.toJson(linkedDocumentIds)
    
    return PersonaEntity(
        id = id,
        name = name,
        description = description,
        systemPromptId = systemPromptId,
        systemPrompt = systemPrompt,
        isForApi = isForApi,
        temperature = temperature,
        topP = topP,
        maxTokens = maxTokens,
        deepEmpathy = deepEmpathy,
        memoryEnabled = memoryEnabled,
        ragEnabled = ragEnabled,
        messageHistoryLimit = messageHistoryLimit,
        deepEmpathyPrompt = deepEmpathyPrompt,
        deepEmpathyAnalysisPrompt = deepEmpathyAnalysisPrompt,
        memoryExtractionPrompt = memoryExtractionPrompt,
        contextInstructions = contextInstructions,
        memoryInstructions = memoryInstructions,
        ragInstructions = ragInstructions,
        swipeMessagePrompt = swipeMessagePrompt,
        memoryLimit = memoryLimit,
        memoryMinAgeDays = memoryMinAgeDays,
        memoryTitle = memoryTitle,
        ragChunkSize = ragChunkSize,
        ragChunkOverlap = ragChunkOverlap,
        ragChunkLimit = ragChunkLimit,
        ragTitle = ragTitle,
        preferredModelId = preferredModelId,
        preferredProvider = preferredProvider,
        linkedDocumentIds = linkedDocumentIdsJson,
        useOnlyPersonaMemories = useOnlyPersonaMemories,
        shareMemoriesGlobally = shareMemoriesGlobally,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// KnowledgeDocument Mappers
fun KnowledgeDocumentEntity.toDomain(): KnowledgeDocument {
    val gson = Gson()
    val linkedPersonaIds = try {
        gson.fromJson<List<String>>(
            linkedPersonaIds,
            object : TypeToken<List<String>>() {}.type
        ) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
    
    return KnowledgeDocument(
        id = id,
        name = name,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sizeBytes = sizeBytes,
        linkedPersonaIds = linkedPersonaIds
    )
}

fun KnowledgeDocument.toEntity(): KnowledgeDocumentEntity {
    val gson = Gson()
    val linkedPersonaIdsJson = gson.toJson(linkedPersonaIds)
    
    return KnowledgeDocumentEntity(
        id = id,
        name = name,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sizeBytes = sizeBytes,
        linkedPersonaIds = linkedPersonaIdsJson
    )
}
