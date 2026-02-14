package com.yourown.ai.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yourown.ai.data.local.YourOwnAIDatabase
import com.yourown.ai.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Migration from version 2 to 3
     * 1. Added promptType field to SystemPromptEntity
     * 2. Recreated memories table with new schema
     */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 1. Add promptType column to system_prompts
            database.execSQL(
                "ALTER TABLE system_prompts ADD COLUMN promptType TEXT NOT NULL DEFAULT 'api'"
            )
            
            // Create index on promptType for faster queries
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_system_prompts_promptType ON system_prompts(promptType)"
            )
            
            // 2. Recreate memories table with new schema
            // Drop old table (losing data, but this is beta)
            database.execSQL("DROP TABLE IF EXISTS memories")
            
            // Create new memories table with correct schema
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS memories (
                    id TEXT PRIMARY KEY NOT NULL,
                    conversation_id TEXT NOT NULL,
                    message_id TEXT NOT NULL,
                    category TEXT NOT NULL,
                    fact TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    is_archived INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                    FOREIGN KEY(message_id) REFERENCES messages(id) ON DELETE CASCADE
                )
            """.trimIndent())
            
            // Create indices for memories
            database.execSQL("CREATE INDEX IF NOT EXISTS index_memories_conversation_id ON memories(conversation_id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_memories_message_id ON memories(message_id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_memories_category ON memories(category)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_memories_created_at ON memories(created_at)")
        }
    }
    
    /**
     * Migration from version 5 to 6
     * Remove category column from memories table (new memory extraction doesn't use categories)
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // SQLite doesn't support DROP COLUMN, so we need to:
            // 1. Create new table without category
            // 2. Copy data
            // 3. Drop old table
            // 4. Rename new table
            
            // Create new memories table without category
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS memories_new (
                    id TEXT PRIMARY KEY NOT NULL,
                    conversation_id TEXT NOT NULL,
                    message_id TEXT NOT NULL,
                    fact TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    is_archived INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                    FOREIGN KEY(message_id) REFERENCES messages(id) ON DELETE CASCADE
                )
            """.trimIndent())
            
            // Copy data from old table (excluding category column)
            database.execSQL("""
                INSERT INTO memories_new (id, conversation_id, message_id, fact, created_at, is_archived)
                SELECT id, conversation_id, message_id, fact, created_at, is_archived
                FROM memories
            """.trimIndent())
            
            // Drop old table
            database.execSQL("DROP TABLE memories")
            
            // Rename new table to memories
            database.execSQL("ALTER TABLE memories_new RENAME TO memories")
            
            // Recreate indices (without category index)
            database.execSQL("CREATE INDEX IF NOT EXISTS index_memories_conversation_id ON memories(conversation_id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_memories_message_id ON memories(message_id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_memories_created_at ON memories(created_at)")
        }
    }
    
    /**
     * Migration from version 6 to 7
     * Add embedding column to memories table for pre-computed embeddings
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add embedding column (stored as comma-separated floats)
            database.execSQL(
                "ALTER TABLE memories ADD COLUMN embedding TEXT DEFAULT NULL"
            )
        }
    }
    
    /**
     * Migration from version 7 to 8
     * Add sourceConversationId column to conversations table for context inheritance
     */
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add sourceConversationId column (ID of parent chat for context inheritance)
            database.execSQL(
                "ALTER TABLE conversations ADD COLUMN sourceConversationId TEXT DEFAULT NULL"
            )
            
            // Create index for faster lookups
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_conversations_sourceConversationId ON conversations(sourceConversationId)"
            )
        }
    }
    
    /**
     * Migration from version 8 to 9
     * Add imageAttachments column to messages table for multimodal support
     */
    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add imageAttachments column (JSON array of image paths)
            database.execSQL(
                "ALTER TABLE messages ADD COLUMN imageAttachments TEXT DEFAULT NULL"
            )
        }
    }
    
    /**
     * Migration from version 9 to 10
     * Add fileAttachments column to messages table for PDF/TXT/DOC support
     */
    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add fileAttachments column (JSON array: [{"path":"...","name":"file.pdf","type":"pdf"}])
            database.execSQL(
                "ALTER TABLE messages ADD COLUMN fileAttachments TEXT DEFAULT NULL"
            )
        }
    }
    
    /**
     * Migration from version 10 to 11
     * Add Persona system:
     * 1. Create personas table
     * 2. Add personaId to conversations
     * 3. Add persona_id to memories
     * 4. Add linkedPersonaIds to knowledge_documents
     */
    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 1. Add personaId to conversations
            database.execSQL(
                "ALTER TABLE conversations ADD COLUMN personaId TEXT DEFAULT NULL"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_conversations_personaId ON conversations(personaId)"
            )
            
            // 2. Add persona_id to memories
            database.execSQL(
                "ALTER TABLE memories ADD COLUMN persona_id TEXT DEFAULT NULL"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_memories_persona_id ON memories(persona_id)"
            )
            
            // 3. Add linkedPersonaIds to knowledge_documents
            database.execSQL(
                "ALTER TABLE knowledge_documents ADD COLUMN linkedPersonaIds TEXT NOT NULL DEFAULT '[]'"
            )
            
            // 4. Create personas table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS personas (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    systemPromptId TEXT NOT NULL,
                    systemPrompt TEXT NOT NULL,
                    isForApi INTEGER NOT NULL,
                    temperature REAL NOT NULL,
                    topP REAL NOT NULL,
                    maxTokens INTEGER NOT NULL,
                    deepEmpathy INTEGER NOT NULL,
                    memoryEnabled INTEGER NOT NULL,
                    ragEnabled INTEGER NOT NULL,
                    messageHistoryLimit INTEGER NOT NULL,
                    deepEmpathyPrompt TEXT NOT NULL,
                    deepEmpathyAnalysisPrompt TEXT NOT NULL,
                    memoryExtractionPrompt TEXT NOT NULL,
                    contextInstructions TEXT NOT NULL,
                    memoryInstructions TEXT NOT NULL,
                    ragInstructions TEXT NOT NULL,
                    swipeMessagePrompt TEXT NOT NULL,
                    memoryLimit INTEGER NOT NULL,
                    memoryMinAgeDays INTEGER NOT NULL,
                    memoryTitle TEXT NOT NULL,
                    ragChunkSize INTEGER NOT NULL,
                    ragChunkOverlap INTEGER NOT NULL,
                    ragChunkLimit INTEGER NOT NULL,
                    ragTitle TEXT NOT NULL,
                    preferredModelId TEXT,
                    preferredProvider TEXT,
                    linkedDocumentIds TEXT NOT NULL,
                    useOnlyPersonaMemories INTEGER NOT NULL,
                    shareMemoriesGlobally INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
            
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_personas_createdAt ON personas(createdAt)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_personas_name ON personas(name)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_personas_isForApi ON personas(isForApi)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_personas_systemPromptId ON personas(systemPromptId)"
            )
        }
    }
    
    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No schema changes needed, just clean up
            // This migration was initially empty but kept to maintain version number consistency
        }
    }
    
    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Clean up any personas that were created without systemPromptId
            database.execSQL("DELETE FROM personas WHERE systemPromptId = '' OR systemPromptId IS NULL")
        }
    }
    
    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Удаляем старый индекс
            database.execSQL("DROP INDEX IF EXISTS index_personas_systemPromptId")
            
            // Удаляем дубликаты персон с одинаковым systemPromptId
            // Оставляем только самую новую (с максимальным updatedAt)
            database.execSQL("""
                DELETE FROM personas 
                WHERE rowid NOT IN (
                    SELECT MAX(rowid)
                    FROM personas
                    GROUP BY systemPromptId
                )
            """.trimIndent())
            
            // Создаем уникальный индекс на systemPromptId
            database.execSQL(
                "CREATE UNIQUE INDEX index_personas_systemPromptId ON personas(systemPromptId)"
            )
        }
    }
    
    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Просто очищаем таблицу personas от всех старых/дублированных записей
            // Пользователи смогут пересоздать personas через UI
            database.execSQL("DELETE FROM personas")
            
            // Пересоздаем индексы с правильными настройками
            database.execSQL("DROP INDEX IF EXISTS index_personas_systemPromptId")
            database.execSQL("CREATE UNIQUE INDEX index_personas_systemPromptId ON personas(systemPromptId)")
        }
    }
    
    /**
     * Migration from version 15 to 16
     * Add webSearchEnabled column to conversations table for OpenRouter :online support
     */
    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add webSearchEnabled column (default false for existing conversations)
            database.execSQL(
                "ALTER TABLE conversations ADD COLUMN webSearchEnabled INTEGER NOT NULL DEFAULT 0"
            )
        }
    }
    
    /**
     * Migration from version 16 to 17
     * Add xSearchEnabled column to conversations table for xAI Grok X Search
     */
    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add xSearchEnabled column (default false for existing conversations)
            database.execSQL(
                "ALTER TABLE conversations ADD COLUMN xSearchEnabled INTEGER NOT NULL DEFAULT 0"
            )
        }
    }
    
    /**
     * Migration from version 17 to 18
     * Add API Embeddings configuration to personas table
     */
    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add API Embeddings configuration columns
            database.execSQL(
                "ALTER TABLE personas ADD COLUMN useApiEmbeddings INTEGER NOT NULL DEFAULT 0"
            )
            database.execSQL(
                "ALTER TABLE personas ADD COLUMN apiEmbeddingsProvider TEXT NOT NULL DEFAULT 'openai'"
            )
            database.execSQL(
                "ALTER TABLE personas ADD COLUMN apiEmbeddingsModel TEXT NOT NULL DEFAULT 'text-embedding-3-small'"
            )
        }
    }
    
    /**
     * Migration from version 18 to 19
     * Add user_biography table for storing user biography generated from memory clusters
     */
    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create user_biography table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS user_biography (
                    id TEXT PRIMARY KEY NOT NULL,
                    userValues TEXT NOT NULL DEFAULT '',
                    profile TEXT NOT NULL DEFAULT '',
                    painPoints TEXT NOT NULL DEFAULT '',
                    joys TEXT NOT NULL DEFAULT '',
                    fears TEXT NOT NULL DEFAULT '',
                    loves TEXT NOT NULL DEFAULT '',
                    currentSituation TEXT NOT NULL DEFAULT '',
                    lastUpdated INTEGER NOT NULL,
                    processedClusters INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
        }
    }
    
    /**
     * Migration from version 19 to 20
     * Add biography_chunks table for storing biography fragments with embeddings
     */
    private val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Fix potential issue with 'values' column name (reserved SQL keyword)
            // Check if table has 'values' column and rename to 'userValues'
            try {
                // Try to query 'values' column
                database.query("SELECT values FROM user_biography LIMIT 1")
                // If successful, we have old 'values' column, need to recreate table
                
                // Create temp table with correct schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_biography_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        userValues TEXT NOT NULL DEFAULT '',
                        profile TEXT NOT NULL DEFAULT '',
                        painPoints TEXT NOT NULL DEFAULT '',
                        joys TEXT NOT NULL DEFAULT '',
                        fears TEXT NOT NULL DEFAULT '',
                        loves TEXT NOT NULL DEFAULT '',
                        currentSituation TEXT NOT NULL DEFAULT '',
                        lastUpdated INTEGER NOT NULL,
                        processedClusters INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // Copy data from old table, renaming 'values' to 'userValues'
                database.execSQL("""
                    INSERT INTO user_biography_new 
                    (id, userValues, profile, painPoints, joys, fears, loves, currentSituation, lastUpdated, processedClusters)
                    SELECT id, values, profile, painPoints, joys, fears, loves, currentSituation, lastUpdated, processedClusters
                    FROM user_biography
                """.trimIndent())
                
                // Drop old table
                database.execSQL("DROP TABLE user_biography")
                
                // Rename new table
                database.execSQL("ALTER TABLE user_biography_new RENAME TO user_biography")
            } catch (e: Exception) {
                // Table either doesn't exist or already has correct schema, continue
            }
            
            // Create biography_chunks table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS biography_chunks (
                    id TEXT PRIMARY KEY NOT NULL,
                    biographyId TEXT NOT NULL DEFAULT 'default',
                    section TEXT NOT NULL,
                    text TEXT NOT NULL,
                    embedding TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Create indices for fast lookups
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_biography_chunks_biographyId 
                ON biography_chunks(biographyId)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_biography_chunks_section 
                ON biography_chunks(section)
            """.trimIndent())
        }
    }
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): YourOwnAIDatabase {
        return Room.databaseBuilder(
            context,
            YourOwnAIDatabase::class.java,
            YourOwnAIDatabase.DATABASE_NAME
        )
            .addMigrations(
                MIGRATION_2_3, 
                MIGRATION_5_6, 
                MIGRATION_6_7, 
                MIGRATION_7_8, 
                MIGRATION_8_9, 
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_19_20
            )
            .fallbackToDestructiveMigration() // Keep for future migrations
            .build()
    }
    
    @Provides
    @Singleton
    fun provideConversationDao(database: YourOwnAIDatabase): ConversationDao {
        return database.conversationDao()
    }
    
    @Provides
    @Singleton
    fun provideMessageDao(database: YourOwnAIDatabase): MessageDao {
        return database.messageDao()
    }
    
    @Provides
    @Singleton
    fun provideMemoryDao(database: YourOwnAIDatabase): MemoryDao {
        return database.memoryDao()
    }
    
    @Provides
    @Singleton
    fun provideDocumentDao(database: YourOwnAIDatabase): DocumentDao {
        return database.documentDao()
    }
    
    @Provides
    @Singleton
    fun provideDocumentChunkDao(database: YourOwnAIDatabase): DocumentChunkDao {
        return database.documentChunkDao()
    }
    
    @Provides
    @Singleton
    fun provideApiKeyDao(database: YourOwnAIDatabase): ApiKeyDao {
        return database.apiKeyDao()
    }
    
    @Provides
    @Singleton
    fun provideUsageStatsDao(database: YourOwnAIDatabase): UsageStatsDao {
        return database.usageStatsDao()
    }
    
    @Provides
    @Singleton
    fun provideSystemPromptDao(database: YourOwnAIDatabase): SystemPromptDao {
        return database.systemPromptDao()
    }
    
    @Provides
    @Singleton
    fun provideKnowledgeDocumentDao(database: YourOwnAIDatabase): KnowledgeDocumentDao {
        return database.knowledgeDocumentDao()
    }
    
    @Provides
    @Singleton
    fun providePersonaDao(database: YourOwnAIDatabase): PersonaDao {
        return database.personaDao()
    }
    
    @Provides
    @Singleton
    fun provideBiographyDao(database: YourOwnAIDatabase): BiographyDao {
        return database.biographyDao()
    }
    
    @Provides
    @Singleton
    fun provideBiographyChunkDao(database: YourOwnAIDatabase): BiographyChunkDao {
        return database.biographyChunkDao()
    }
}
