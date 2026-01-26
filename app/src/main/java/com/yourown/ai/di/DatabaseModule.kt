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
            .addMigrations(MIGRATION_2_3)
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
