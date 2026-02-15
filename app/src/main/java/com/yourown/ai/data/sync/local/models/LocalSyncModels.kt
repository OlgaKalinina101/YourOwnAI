package com.yourown.ai.data.sync.local.models

import com.yourown.ai.data.local.entity.ConversationEntity
import com.yourown.ai.data.local.entity.MessageEntity
import com.yourown.ai.data.local.entity.MemoryEntity
import com.yourown.ai.data.local.entity.PersonaEntity

/**
 * Device info for discovery and identification
 */
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val appVersion: String,
    val platform: String = "Android",
    val ipAddress: String = "unknown"
)

/**
 * Sync request from desktop client
 */
data class SyncRequest(
    val deviceInfo: DeviceInfo,
    val lastSyncTimestamp: Long = 0 // Get only data newer than this
)

/**
 * Send message request from web/desktop
 */
data class SendMessageRequest(
    val content: String,
    val clientMessageId: String? = null,
    val personaId: String? = null,
    val webSearchEnabled: Boolean = false,
    val imageAttachments: List<String>? = null,  // Base64 encoded images
    val fileAttachments: List<FileAttachment>? = null
)

/**
 * File attachment data
 */
data class FileAttachment(
    val name: String,
    val content: String,  // Base64 encoded
    val type: String
)

/**
 * Create conversation request
 */
data class CreateConversationRequest(
    val title: String? = null,
    val systemPrompt: String? = null,
    val model: String? = null,
    val provider: String? = null,
    val personaId: String? = null
)

/**
 * Full sync response with all data
 */
data class SyncResponse(
    val deviceInfo: DeviceInfo,
    val conversations: List<ConversationEntity>,
    val messages: List<MessageEntity>,
    val memories: List<MemoryEntity>, // Without embeddings
    val personas: List<PersonaEntity>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Incremental sync response (only changes since lastSyncTimestamp)
 */
data class IncrementalSyncResponse(
    val deviceInfo: DeviceInfo,
    val newConversations: List<ConversationEntity>,
    val updatedConversations: List<ConversationEntity>,
    val newMessages: List<MessageEntity>,
    val newMemories: List<MemoryEntity>,
    val newPersonas: List<PersonaEntity>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Server status response
 */
data class ServerStatus(
    val isRunning: Boolean,
    val deviceInfo: DeviceInfo,
    val port: Int,
    val totalConversations: Int,
    val totalMessages: Int,
    val totalMemories: Int,
    val totalPersonas: Int
)

data class AppearanceSettingsResponse(
    val themeMode: String,
    val colorStyle: String,
    val fontStyle: String,
    val fontScale: Float
)
