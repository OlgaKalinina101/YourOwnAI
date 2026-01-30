package com.yourown.ai.data.mapper

import com.yourown.ai.data.local.entity.ConversationEntity
import com.yourown.ai.data.local.entity.MessageEntity
import com.yourown.ai.domain.model.Conversation
import com.yourown.ai.domain.model.Message
import com.yourown.ai.domain.model.MessageRole

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
        model = model,
        provider = provider,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isArchived = isArchived,
        sourceConversationId = sourceConversationId
    )
}
