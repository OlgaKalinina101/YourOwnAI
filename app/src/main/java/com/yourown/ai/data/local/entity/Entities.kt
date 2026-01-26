package com.yourown.ai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Conversation Entity
 * Представляет отдельную беседу/чат с AI
 */
@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["updatedAt"])
    ]
)
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    
    val title: String,                    // Название беседы
    val systemPrompt: String,             // Системный промпт для этой беседы
    val model: String,                    // Используемая модель (gpt-4, claude-3, etc)
    val provider: String,                 // Провайдер (openai, anthropic, etc)
    
    val createdAt: Long,                  // Timestamp создания
    val updatedAt: Long,                  // Timestamp последнего обновления
    
    val isPinned: Boolean = false,        // Закреплена ли беседа
    val isArchived: Boolean = false,      // Архивирована ли
)

/**
 * Message Entity
 * Отдельное сообщение в беседе
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["createdAt"]),
        Index(value = ["role"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    
    val conversationId: String,           // ID беседы
    
    val role: String,                     // user, assistant, system
    val content: String,                  // Текст сообщения
    
    val createdAt: Long,                  // Timestamp
    
    val tokenCount: Int? = null,          // Количество токенов (если известно)
    val model: String? = null,            // Модель, которая ответила (для assistant)
    
    val isError: Boolean = false,         // Ошибка при генерации
    val errorMessage: String? = null,     // Текст ошибки
    
    // User interaction
    val isLiked: Boolean = false,         // Лайк от пользователя
    
    // Swipe/alternative responses
    val swipeMessageId: String? = null,   // ID альтернативного ответа
    val swipeMessageText: String? = null, // Текст альтернативного ответа
    
    // Settings snapshot - флаги на момент генерации сообщения
    val temperature: Float? = null,
    val topP: Float? = null,
    val deepEmpathy: Boolean = false,
    val memoryEnabled: Boolean = true,
    val messageHistoryLimit: Int? = null,
    val systemPrompt: String? = null,     // System prompt на момент генерации
    
    // Request logs for debugging
    val requestLogs: String? = null       // JSON с параметрами запроса
)

/**
 * Document Entity
 * Загруженные документы для RAG
 */
@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["uploadedAt"]),
        Index(value = ["fileType"]),
        Index(value = ["conversationId"])
    ]
)
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    
    val fileName: String,                 // Оригинальное имя файла
    val fileType: String,                 // pdf, txt, etc
    val filePath: String,                 // Путь к файлу на устройстве
    val fileSize: Long,                   // Размер в байтах
    
    val conversationId: String? = null,   // К какой беседе привязан (опционально)
    
    val uploadedAt: Long,                 // Timestamp загрузки
    
    val isProcessed: Boolean = false,     // Обработан ли для RAG
    val chunkCount: Int = 0,              // Количество chunks
)

/**
 * Document Chunk Entity
 * Части документа для RAG поиска
 */
@Entity(
    tableName = "document_chunks",
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["chunkIndex"])
    ]
)
data class DocumentChunkEntity(
    @PrimaryKey
    val id: String,
    
    val documentId: String,               // ID родительского документа
    
    val content: String,                  // Текст chunk'а
    val chunkIndex: Int,                  // Порядковый номер
    
    val embedding: String? = null,        // JSON массив embedding для semantic search
    
    val metadata: String? = null,         // JSON с метаданными (страница, параграф, etc)
)

/**
 * API Key Entity
 * Хранение API ключей (будет зашифровано через EncryptedSharedPreferences)
 * Эта таблица только для метаданных, сами ключи - в EncryptedSharedPreferences
 */
@Entity(
    tableName = "api_keys",
    indices = [
        Index(value = ["provider"], unique = true)
    ]
)
data class ApiKeyEntity(
    @PrimaryKey
    val id: String,
    
    val provider: String,                 // openai, anthropic, google, groq, openrouter, local
    val displayName: String,              // Название для UI
    
    val isActive: Boolean = true,         // Активен ли ключ
    
    val addedAt: Long,                    // Когда добавлен
    val lastUsedAt: Long? = null,         // Последнее использование
)

/**
 * Usage Stats Entity
 * Статистика использования токенов и затрат
 */
@Entity(
    tableName = "usage_stats",
    indices = [
        Index(value = ["date"]),
        Index(value = ["provider"]),
        Index(value = ["model"])
    ]
)
data class UsageStatsEntity(
    @PrimaryKey
    val id: String,
    
    val date: String,                     // Дата в формате YYYY-MM-DD
    
    val provider: String,                 // Провайдер
    val model: String,                    // Модель
    
    val inputTokens: Int,                 // Входящие токены
    val outputTokens: Int,                // Исходящие токены
    val totalTokens: Int,                 // Всего токенов
    
    val estimatedCost: Double,            // Примерная стоимость в USD
    
    val requestCount: Int,                // Количество запросов
)

/**
 * System Prompt Entity
 * Сохраненные системные промпты
 */
@Entity(
    tableName = "system_prompts",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isDefault"]),
        Index(value = ["promptType"])
    ]
)
data class SystemPromptEntity(
    @PrimaryKey
    val id: String,
    
    val name: String,                     // Название промпта
    val content: String,                  // Текст промпта
    
    val promptType: String = "api",       // "api" или "local"
    val isDefault: Boolean = false,       // Дефолтный ли промпт для своего типа
    
    val createdAt: Long,                  // Timestamp создания
    val updatedAt: Long,                  // Последнее обновление
    
    val usageCount: Int = 0,              // Сколько раз использован
)
