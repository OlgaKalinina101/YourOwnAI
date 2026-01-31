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
            .addMigrations(MIGRATION_2_3, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
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
}
