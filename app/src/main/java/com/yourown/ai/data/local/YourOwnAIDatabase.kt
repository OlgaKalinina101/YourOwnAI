package com.yourown.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yourown.ai.data.local.dao.*
import com.yourown.ai.data.local.entity.*

/**
 * YourOwnAI Room Database
 * 
 * Содержит все таблицы для локального хранения:
 * - Conversations (беседы)
 * - Messages (сообщения)
 * - Memories (долговременная память)
 * - Documents (загруженные файлы)
 * - DocumentChunks (части документов для RAG)
 * - ApiKeys (метаданные API ключей)
 * - UsageStats (статистика использования)
 * - SystemPrompts (системные промпты)
 * - KnowledgeDocuments (текстовые документы для контекста)
 * - Personas (профили с настройками AI)
 */
@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        DocumentEntity::class,
        DocumentChunkEntity::class,
        ApiKeyEntity::class,
        UsageStatsEntity::class,
        SystemPromptEntity::class,
        KnowledgeDocumentEntity::class,
        PersonaEntity::class,
    ],
    version = 15,  // Increased: updated Conversation with personaId mapping
    exportSchema = true
)
abstract class YourOwnAIDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun documentDao(): DocumentDao
    abstract fun documentChunkDao(): DocumentChunkDao
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun usageStatsDao(): UsageStatsDao
    abstract fun systemPromptDao(): SystemPromptDao
    abstract fun knowledgeDocumentDao(): KnowledgeDocumentDao
    abstract fun personaDao(): PersonaDao
    
    companion object {
        const val DATABASE_NAME = "yourown_ai_database"
    }
}
