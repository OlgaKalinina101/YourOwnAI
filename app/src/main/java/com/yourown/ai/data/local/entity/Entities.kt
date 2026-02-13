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
        Index(value = ["updatedAt"]),
        Index(value = ["systemPromptId"]),
        Index(value = ["sourceConversationId"]),
        Index(value = ["personaId"])
    ]
)
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    
    val title: String,                    // Название беседы
    val systemPrompt: String,             // Системный промпт для этой беседы (legacy)
    val systemPromptId: String? = null,   // ID системного промпта из таблицы system_prompts (legacy)
    val personaId: String? = null,        // ID persona (если null - используем глобальные настройки)
    val model: String,                    // Используемая модель (gpt-4, claude-3, etc)
    val provider: String,                 // Провайдер (openai, anthropic, etc)
    
    val createdAt: Long,                  // Timestamp создания
    val updatedAt: Long,                  // Timestamp последнего обновления
    
    val isPinned: Boolean = false,        // Закреплена ли беседа
    val isArchived: Boolean = false,      // Архивирована ли
    
    val sourceConversationId: String? = null,  // ID чата, из которого подтянута история для контекста
    
    val webSearchEnabled: Boolean = false,     // Включен ли web search для этого чата (OpenRouter :online)
    val xSearchEnabled: Boolean = false,       // Включен ли X search для Grok (только xAI)
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
    
    // Attachments
    val imageAttachments: String? = null, // JSON array of image paths
    val fileAttachments: String? = null, // JSON array: [{"path":"path1","name":"file.pdf","type":"pdf"}]
    
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

/**
 * Knowledge Document Entity
 * Текстовые документы для контекста (из настроек)
 */
@Entity(
    tableName = "knowledge_documents",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["name"])
    ]
)
data class KnowledgeDocumentEntity(
    @PrimaryKey
    val id: String,
    
    val name: String,                     // Название документа
    val content: String,                  // Текст документа
    
    val createdAt: Long,                  // Timestamp создания
    val updatedAt: Long,                  // Последнее обновление
    
    val sizeBytes: Int = 0,               // Размер в байтах
    val linkedPersonaIds: String = "[]",  // JSON array of persona IDs
)

/**
 * Persona Entity
 * Профили с настройками AI, документами и memory scope
 */
@Entity(
    tableName = "personas",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["name"]),
        Index(value = ["isForApi"]),
        Index(value = ["systemPromptId"], unique = true)
    ]
)
data class PersonaEntity(
    @PrimaryKey
    val id: String,
    
    val name: String,                     // Название persona
    val description: String = "",         // Описание
    val systemPromptId: String,           // Ссылка на SystemPrompt
    val systemPrompt: String,             // Кеш текста промпта
    val isForApi: Boolean = true,         // Для API или Local модели
    
    // AI Configuration
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 4096,
    val deepEmpathy: Boolean = false,
    val memoryEnabled: Boolean = false,
    val ragEnabled: Boolean = false,
    val messageHistoryLimit: Int = 10,
    
    // Prompts
    val deepEmpathyPrompt: String,
    val deepEmpathyAnalysisPrompt: String,
    val memoryExtractionPrompt: String,
    val contextInstructions: String,
    val memoryInstructions: String,
    val ragInstructions: String,
    val swipeMessagePrompt: String,
    
    // Memory Configuration
    val memoryLimit: Int = 5,
    val memoryMinAgeDays: Int = 2,
    val memoryTitle: String = "Твои воспоминания",
    
    // RAG Configuration
    val ragChunkSize: Int = 512,
    val ragChunkOverlap: Int = 64,
    val ragChunkLimit: Int = 5,
    val ragTitle: String = "Твоя библиотека текстов",
    
    // Model Preference
    val preferredModelId: String? = null,
    val preferredProvider: String? = null,
    
    // Document Links
    val linkedDocumentIds: String = "[]", // JSON array of document IDs
    
    // Memory Scope
    val useOnlyPersonaMemories: Boolean = false,
    val shareMemoriesGlobally: Boolean = true,
    
    // API Embeddings Configuration
    val useApiEmbeddings: Boolean = false,
    val apiEmbeddingsProvider: String = "openai",
    val apiEmbeddingsModel: String = "text-embedding-3-small",
    
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * User Biography Entity
 * Сгенерированная биография пользователя на основе кластеров воспоминаний
 */
@Entity(tableName = "user_biography")
data class BiographyEntity(
    @PrimaryKey
    val id: String = "default", // Всегда один экземпляр биографии
    
    val userValues: String = "",        // Что важно (ценности, убеждения)
    val profile: String = "",           // Профиль пользователя
    val painPoints: String = "",        // Что болит
    val joys: String = "",              // Что радует
    val fears: String = "",             // Что пугает
    val loves: String = "",             // Что любит
    val currentSituation: String = "",  // Что происходит сейчас
    
    val lastUpdated: Long = System.currentTimeMillis(),
    val processedClusters: Int = 0
)

/**
 * Biography Chunk Entity
 * Фрагменты биографии с эмбеддингами для семантического поиска
 */
@Entity(
    tableName = "biography_chunks",
    indices = [
        Index(value = ["biographyId"]),
        Index(value = ["section"])
    ]
)
data class BiographyChunkEntity(
    @PrimaryKey
    val id: String, // e.g. "bio_chunk_values_1"
    
    val biographyId: String = "default", // Ссылка на биографию
    val section: String,                  // values, profile, painPoints, joys, fears, loves, currentSituation
    val text: String,                     // Текст фрагмента (одно предложение или абзац)
    val embedding: String,                // JSON массив эмбеддинга
    
    val createdAt: Long = System.currentTimeMillis()
)
