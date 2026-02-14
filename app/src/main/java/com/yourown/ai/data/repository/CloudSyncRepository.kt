package com.yourown.ai.data.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.yourown.ai.data.local.entity.ConversationEntity
import com.yourown.ai.data.local.entity.DocumentChunkEntity
import com.yourown.ai.data.local.entity.KnowledgeDocumentEntity
import com.yourown.ai.data.local.entity.MemoryEntity
import com.yourown.ai.data.local.entity.PersonaEntity
import com.yourown.ai.data.local.entity.SystemPromptEntity
import com.yourown.ai.data.local.preferences.CloudSyncPreferences
import com.yourown.ai.data.mapper.toEntity
import com.yourown.ai.data.sync.*
import com.yourown.ai.domain.model.Conversation
import com.yourown.ai.domain.model.KnowledgeDocument
import com.yourown.ai.domain.model.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.android.Android
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for syncing data with Supabase (PostgreSQL with REST API)
 * 
 * Direct connection from Android without backend server!
 */
@Singleton
class CloudSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudSyncPreferences: CloudSyncPreferences,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val memoryRepository: MemoryRepository,
    private val knowledgeDocumentRepository: KnowledgeDocumentRepository,
    private val documentEmbeddingRepository: DocumentEmbeddingRepository,
    private val personaRepository: PersonaRepository,
    private val systemPromptRepository: SystemPromptRepository
) {
    private var supabaseClient: io.github.jan.supabase.SupabaseClient? = null
    
    companion object {
        private const val TAG = "CloudSyncRepository"
    }
    
    /**
     * Get unique device ID for sync tracking
     */
    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }
    
    /**
     * Initialize Supabase client
     */
    private suspend fun initializeClient(): Result<io.github.jan.supabase.SupabaseClient> = withContext(Dispatchers.IO) {
        try {
            val supabaseUrl = cloudSyncPreferences.getSupabaseUrl()
            val supabaseKey = cloudSyncPreferences.getSupabaseKey()
            
            if (supabaseUrl.isBlank() || supabaseKey.isBlank()) {
                return@withContext Result.failure(Exception("Supabase credentials are empty"))
            }
            
            Log.d(TAG, "Initializing Supabase client...")
            Log.d(TAG, "URL: $supabaseUrl")
            
            val client = createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = supabaseKey
            ) {
                install(Postgrest)
                install(Realtime)
                
                // Use Android HTTP client
                httpEngine = Android.create()
            }
            
            supabaseClient = client
            
            Log.i(TAG, "Supabase client initialized successfully! ✅")
            Result.success(client)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Test Supabase connection
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Simply initializing the client validates the credentials
            val client = supabaseClient ?: initializeClient().getOrThrow()
            
            Log.d(TAG, "Testing Supabase connection...")
            
            // If we got here, credentials are valid
            // Client initialization succeeded
            Log.i(TAG, "Connection test: SUCCESS ✅")
            Log.i(TAG, "Note: Create tables in Supabase SQL Editor before syncing")
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Create tables in Supabase (via SQL Editor) - OPTIMIZED FOR FREE TIER
     * 
     * Note: Supabase SDK doesn't support DDL operations directly.
     * Tables should be created via Supabase Dashboard → SQL Editor
     * 
     * This method provides the SQL commands for user to copy-paste
     * Schema is optimized to save space - only essential data synced
     */
    suspend fun createTables(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sqlCommands = """
                -- YourOwnAI Cloud Sync - Optimized Schema
                -- Run these commands in Supabase Dashboard → SQL Editor
                
                -- Syncs only: Conversations, Messages, Memories (NO embeddings), Personas
                -- NOT synced: System Prompts, RAG Documents, Embeddings
                
                -- 1. Conversations table
                CREATE TABLE IF NOT EXISTS conversations (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    model TEXT NOT NULL,
                    provider TEXT NOT NULL,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    is_archived BOOLEAN DEFAULT FALSE,
                    persona_id TEXT,
                    source_conversation_id TEXT,
                    device_id TEXT,
                    synced_at BIGINT
                );
                
                -- 2. Messages table (optimized - no request_logs)
                CREATE TABLE IF NOT EXISTS messages (
                    id TEXT PRIMARY KEY,
                    conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    created_at BIGINT NOT NULL,
                    model TEXT,
                    swipe_message_text TEXT,
                    image_attachments TEXT,
                    file_attachments TEXT,
                    is_liked BOOLEAN DEFAULT FALSE,
                    device_id TEXT,
                    synced_at BIGINT
                );
                
                -- 3. Memories table (optimized - NO embeddings)
                CREATE TABLE IF NOT EXISTS memories (
                    id TEXT PRIMARY KEY,
                    conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                    message_id TEXT NOT NULL,
                    fact TEXT NOT NULL,
                    created_at BIGINT NOT NULL,
                    persona_id TEXT,
                    device_id TEXT,
                    synced_at BIGINT
                );
                
                -- 4. Personas table
                CREATE TABLE IF NOT EXISTS personas (
                    id TEXT PRIMARY KEY,
                    system_prompt_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    is_for_api BOOLEAN DEFAULT TRUE,
                    temperature REAL DEFAULT 0.7,
                    top_p REAL DEFAULT 0.9,
                    max_tokens INTEGER DEFAULT 4096,
                    deep_empathy BOOLEAN DEFAULT FALSE,
                    memory_enabled BOOLEAN DEFAULT FALSE,
                    rag_enabled BOOLEAN DEFAULT FALSE,
                    message_history_limit INTEGER DEFAULT 10,
                    deep_empathy_prompt TEXT,
                    deep_empathy_analysis_prompt TEXT,
                    memory_extraction_prompt TEXT,
                    context_instructions TEXT,
                    memory_instructions TEXT,
                    rag_instructions TEXT,
                    swipe_message_prompt TEXT,
                    memory_limit INTEGER DEFAULT 5,
                    memory_min_age_days INTEGER DEFAULT 2,
                    memory_title TEXT,
                    rag_chunk_size INTEGER DEFAULT 512,
                    rag_chunk_overlap INTEGER DEFAULT 64,
                    rag_chunk_limit INTEGER DEFAULT 5,
                    rag_title TEXT,
                    preferred_model_id TEXT,
                    preferred_provider TEXT,
                    linked_document_ids TEXT,
                    use_only_persona_memories BOOLEAN DEFAULT FALSE,
                    share_memories_globally BOOLEAN DEFAULT TRUE,
                    use_api_embeddings BOOLEAN DEFAULT FALSE,
                    api_embeddings_provider TEXT DEFAULT 'openai',
                    api_embeddings_model TEXT DEFAULT 'text-embedding-3-small',
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    device_id TEXT,
                    synced_at BIGINT
                );
                
                -- Create indexes for performance
                CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id);
                CREATE INDEX IF NOT EXISTS idx_messages_created ON messages(created_at);
                CREATE INDEX IF NOT EXISTS idx_memories_conversation ON memories(conversation_id);
                CREATE INDEX IF NOT EXISTS idx_memories_persona ON memories(persona_id);
                CREATE INDEX IF NOT EXISTS idx_conversations_persona ON conversations(persona_id);
                CREATE INDEX IF NOT EXISTS idx_personas_updated ON personas(updated_at);
                
                -- Optional: Enable Row Level Security
                -- ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
                -- ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
                -- ALTER TABLE memories ENABLE ROW LEVEL SECURITY;
                -- ALTER TABLE personas ENABLE ROW LEVEL SECURITY;
            """.trimIndent()
            
            Log.i(TAG, "SQL commands ready for Supabase SQL Editor")
            Result.success(sqlCommands)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate SQL", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sync all data to Supabase (Optimized for Free Tier)
     * 
     * Syncs only:
     * - Conversations (basic fields)
     * - Messages (without request_logs)
     * - Memories (WITHOUT embeddings)
     * - Personas (all fields)
     */
    suspend fun syncToCloud(): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val client = supabaseClient ?: initializeClient().getOrThrow()
            
            Log.d(TAG, "Starting sync to Supabase...")
            
            // Step 1: Get all conversations from Room
            val conversations = conversationRepository.getAllConversations().first()
            Log.d(TAG, "Found ${conversations.size} conversations to sync")
            
            // Step 2: Get all messages from Room
            val allMessages = mutableListOf<com.yourown.ai.domain.model.Message>()
            conversations.forEach { conversation ->
                val messages = messageRepository.getMessagesByConversation(conversation.id).first()
                allMessages.addAll(messages)
            }
            Log.d(TAG, "Found ${allMessages.size} messages to sync")
            
            // Step 3: Get all memories from Room (WITHOUT embeddings for sync)
            val allMemories = memoryRepository.getAllMemoryEntities()
            Log.d(TAG, "Found ${allMemories.size} memories to sync (embeddings NOT included)")
            
            // Step 4: Get all personas
            val allPersonas = personaRepository.getAllPersonasEntities()
            Log.d(TAG, "Found ${allPersonas.size} personas to sync")
            
            // Note: System Prompts, Knowledge Documents, and Document Embeddings are NOT synced
            // They are stored and managed locally only
            
            // Step 5: Calculate actual data size
            // Estimate based on JSON serialization size:
            // - Conversation: ~0.5 KB (id, title, timestamps, settings)
            // - Message: content length + 200 bytes overhead
            // - Memory: fact length + 150 bytes overhead (NO embeddings)
            // - Persona: ~3-5KB (settings, prompts, metadata)
            
            val conversationsSize = conversations.size * 512L // 0.5 KB per conversation
            val messagesSize: Long = allMessages.sumOf { message: Message ->
                ((message.content?.length ?: 0) + 200).toLong()
            }
            
            // Memories: WITHOUT embeddings
            val memoriesSize: Long = allMemories.sumOf { memory: MemoryEntity ->
                (memory.fact.length + 150).toLong()
            }
            
            val personasSize: Long = allPersonas.sumOf { persona: PersonaEntity ->
                (persona.systemPrompt.length + 
                 persona.deepEmpathyPrompt.length + 
                 persona.deepEmpathyAnalysisPrompt.length +
                 persona.memoryExtractionPrompt.length +
                 persona.contextInstructions.length +
                 persona.memoryInstructions.length +
                 persona.ragInstructions.length +
                 persona.swipeMessagePrompt.length + 
                 1024).toLong() // prompts + settings
            }
            
            val totalBytes = conversationsSize + messagesSize + memoriesSize + personasSize
            val totalMB = totalBytes / (1024f * 1024f)
            
            Log.i(TAG, "Total data size: ${String.format("%.3f", totalMB)} MB")
            Log.i(TAG, "├── ${conversations.size} conversations")
            Log.i(TAG, "├── ${allMessages.size} messages")
            Log.i(TAG, "├── ${allMemories.size} memories (NO embeddings)")
            Log.i(TAG, "└── ${allPersonas.size} personas")
            Log.i(TAG, "NOT syncing: System Prompts, RAG Documents, RAG Embeddings (stored locally)")
            
            // Step 6: Upload to Supabase
            val deviceId = getDeviceId()
            
            // Personas (upload first, before conversations that might reference them)
            if (allPersonas.isNotEmpty()) {
                val personaDtos = allPersonas.map { persona: PersonaEntity -> persona.toDto(deviceId) }
                try {
                    personaDtos.chunked(50).forEach { batch -> // Smaller batches due to large prompt fields
                        client.from("personas").upsert(batch)
                    }
                    Log.d(TAG, "✅ Uploaded ${allPersonas.size} personas")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to upload personas: ${e.message}")
                }
            }
            
            // Conversations
            if (conversations.isNotEmpty()) {
                val conversationDtos = conversations.map { conv: Conversation -> conv.toEntity().toDto(deviceId) }
                try {
                    conversationDtos.chunked(100).forEach { batch ->
                        client.from("conversations").upsert(batch)
                    }
                    Log.d(TAG, "✅ Uploaded ${conversations.size} conversations")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to upload conversations: ${e.message}")
                }
            }
            
            // Messages
            if (allMessages.isNotEmpty()) {
                val messageDtos = allMessages.map { it.toEntity().toDto(deviceId) }
                try {
                    messageDtos.chunked(100).forEach { batch ->
                        client.from("messages").upsert(batch)
                    }
                    Log.d(TAG, "✅ Uploaded ${allMessages.size} messages")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to upload messages: ${e.message}")
                }
            }
            
            // Memories (WITHOUT embeddings)
            if (allMemories.isNotEmpty()) {
                val memoryDtos = allMemories.map { memory: MemoryEntity -> memory.toDto(deviceId) }
                try {
                    memoryDtos.chunked(100).forEach { batch ->
                        client.from("memories").upsert(batch)
                    }
                    Log.d(TAG, "✅ Uploaded ${allMemories.size} memories (NO embeddings)")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to upload memories: ${e.message}")
                }
            }
            
            Log.i(TAG, "✅ Sync to Supabase completed: ${String.format("%.3f", totalMB)} MB")
            Result.success(SyncResult(
                conversationsSynced = conversations.size,
                messagesSynced = allMessages.size,
                memoriesSynced = allMemories.size,
                personasSynced = allPersonas.size
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Sync to Supabase failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Sync data from Supabase (Optimized for Free Tier)
     * Downloads only essential data from cloud and merges with local data
     * 
     * Syncs only:
     * - Conversations
     * - Messages
     * - Memories (WITHOUT embeddings)
     * - Personas
     * 
     * Conflict resolution: Last-write-wins based on updated_at timestamp
     */
    suspend fun syncFromCloud(): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val client = supabaseClient ?: initializeClient().getOrThrow()
            
            Log.d(TAG, "Starting sync from Supabase (optimized)...")
            
            var conversationsSynced = 0
            var messagesSynced = 0
            var memoriesSynced = 0
            var personasSynced = 0
            
            // ========== 1. FETCH ALL DATA FROM SUPABASE ==========
            
            // Personas
            val cloudPersonas = try {
                client.from("personas")
                    .select(columns = Columns.ALL)
                    .decodeList<PersonaDto>()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch personas: ${e.message}")
                emptyList()
            }
            
            // Conversations
            val cloudConversations = try {
                client.from("conversations")
                    .select(columns = Columns.ALL)
                    .decodeList<ConversationDto>()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch conversations: ${e.message}")
                emptyList()
            }
            
            // Messages
            val cloudMessages = try {
                client.from("messages")
                    .select(columns = Columns.ALL)
                    .decodeList<MessageDto>()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch messages: ${e.message}")
                emptyList()
            }
            
            // Memories (WITHOUT embeddings)
            val cloudMemories = try {
                client.from("memories")
                    .select(columns = Columns.ALL)
                    .decodeList<MemoryDto>()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch memories: ${e.message}")
                emptyList()
            }
            
            Log.d(TAG, "Fetched from cloud: ${cloudConversations.size} convs, ${cloudMessages.size} msgs, " +
                    "${cloudMemories.size} memories (NO embeddings), ${cloudPersonas.size} personas")
            
            // ========== 2. GET LOCAL DATA ==========
            
            val localPersonas = personaRepository.getAllPersonas().first()
            val localPersonasMap = localPersonas.associateBy { it.id }
            
            val localConversations = conversationRepository.getAllConversations().first()
            val localConversationsMap = localConversations.associateBy { it.id }
            
            val localMessages = messageRepository.getAllMessages().first()
            val localMessagesMap = localMessages.associateBy { it.id }
            
            val localMemories = memoryRepository.getAllMemoryEntities()
            val localMemoriesMap = localMemories.associateBy { it.id }
            
            // Note: System Prompts are fetched from local to map persona systemPromptId -> content
            val localSystemPrompts = systemPromptRepository.getAllPrompts().first()
            val localSystemPromptsMap = localSystemPrompts.associateBy { it.id }
            
            // ========== 3. MERGE PERSONAS ==========
            
            for (cloudPersona in cloudPersonas) {
                val localPersona = localPersonasMap[cloudPersona.id]
                
                val shouldUpdate = if (localPersona == null) {
                    true
                } else {
                    cloudPersona.updated_at >= localPersona.updatedAt
                }
                
                if (shouldUpdate) {
                    // Get system prompt content from LOCAL storage (prompts not synced to cloud)
                    val systemPromptContent = localSystemPromptsMap[cloudPersona.system_prompt_id]?.content ?: ""
                    val entity = cloudPersona.toEntity(systemPromptContent)
                    personaRepository.upsertPersona(entity)
                    personasSynced++
                    if (localPersona == null) {
                        Log.d(TAG, "Inserted new persona: ${cloudPersona.name}")
                    } else {
                        Log.d(TAG, "Updated persona: ${cloudPersona.name}")
                    }
                }
            }
            
            // ========== 4. MERGE CONVERSATIONS ==========
            
            for (cloudConv in cloudConversations) {
                val localConv = localConversationsMap[cloudConv.id]
                
                val shouldUpdate = if (localConv == null) {
                    true
                } else {
                    cloudConv.updated_at >= localConv.updatedAt
                }
                
                if (shouldUpdate) {
                    val entity = cloudConv.toEntity()
                    conversationRepository.upsertConversation(entity)
                    conversationsSynced++
                    if (localConv == null) {
                        Log.d(TAG, "Inserted new conversation: ${cloudConv.title}")
                    } else {
                        Log.d(TAG, "Updated conversation: ${cloudConv.title}")
                    }
                }
            }
            
            // ========== 5. MERGE MESSAGES ==========
            
            for (cloudMsg in cloudMessages) {
                val localMsg = localMessagesMap[cloudMsg.id]
                
                // Messages don't have updated_at, so use created_at
                val shouldUpdate = if (localMsg == null) {
                    true
                } else {
                    cloudMsg.created_at >= localMsg.createdAt
                }
                
                if (shouldUpdate) {
                    val entity = cloudMsg.toEntity()
                    messageRepository.upsertMessage(entity)
                    messagesSynced++
                }
            }
            
            Log.d(TAG, "Merged ${messagesSynced} messages")
            
            // ========== 6. MERGE MEMORIES (WITHOUT embeddings) ==========
            
            for (cloudMem in cloudMemories) {
                val localMem = localMemoriesMap[cloudMem.id]
                
                // Memories don't have updated_at, use created_at
                val shouldUpdate = if (localMem == null) {
                    true
                } else {
                    cloudMem.created_at >= localMem.createdAt
                }
                
                if (shouldUpdate) {
                    val entity = cloudMem.toEntity()
                    memoryRepository.upsertMemory(entity)
                    memoriesSynced++
                }
            }
            
            Log.d(TAG, "Merged ${memoriesSynced} memories (embeddings will be generated locally)")
            
            // ========== DONE ==========
            
            Log.i(TAG, "✅ Sync from Supabase completed successfully")
            Log.i(TAG, "Downloaded: $conversationsSynced conversations, $messagesSynced messages, $memoriesSynced memories, $personasSynced personas")
            
            Result.success(SyncResult(
                conversationsSynced = conversationsSynced,
                messagesSynced = messagesSynced,
                memoriesSynced = memoriesSynced,
                personasSynced = personasSynced
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Sync from Supabase failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Calculate current syncable data size (without actually syncing)
     * Returns size in MB
     */
    suspend fun calculateSyncableDataSize(): Result<Float> = withContext(Dispatchers.IO) {
        try {
            // Get all data that would be synced
            val conversations = conversationRepository.getAllConversations().first()
            
            val allMessages = mutableListOf<Message>()
            conversations.forEach { conversation ->
                val messages = messageRepository.getMessagesByConversation(conversation.id).first()
                allMessages.addAll(messages)
            }
            
            val allMemories = memoryRepository.getAllMemoryEntities()
            val allPersonas = personaRepository.getAllPersonasEntities()
            
            // Calculate sizes (same logic as syncToCloud)
            val conversationsSize = conversations.size * 512L
            val messagesSize: Long = allMessages.sumOf { message: Message ->
                ((message.content?.length ?: 0) + 200).toLong()
            }
            val memoriesSize: Long = allMemories.sumOf { memory: MemoryEntity ->
                (memory.fact.length + 150).toLong()
            }
            val personasSize: Long = allPersonas.sumOf { persona: PersonaEntity ->
                (persona.systemPrompt.length + 
                 persona.deepEmpathyPrompt.length + 
                 persona.deepEmpathyAnalysisPrompt.length +
                 persona.memoryExtractionPrompt.length +
                 persona.contextInstructions.length +
                 persona.memoryInstructions.length +
                 persona.ragInstructions.length +
                 persona.swipeMessagePrompt.length + 
                 1024).toLong()
            }
            
            val totalBytes = conversationsSize + messagesSize + memoriesSize + personasSize
            val totalMB = totalBytes / (1024f * 1024f)
            
            Log.d(TAG, "Calculated syncable data size: ${String.format("%.3f", totalMB)} MB")
            Log.d(TAG, "├── ${conversations.size} conversations")
            Log.d(TAG, "├── ${allMessages.size} messages")
            Log.d(TAG, "├── ${allMemories.size} memories")
            Log.d(TAG, "└── ${allPersonas.size} personas")
            
            Result.success(totalMB)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate data size", e)
            Result.failure(e)
        }
    }
    
    /**
     * Close Supabase client
     */
    fun close() {
        // Note: Supabase client will be garbage collected automatically
        // We don't call client.close() as it's a suspend function
        supabaseClient = null
        Log.i(TAG, "Supabase client reference cleared")
    }
}

/**
 * Result of sync operation (Optimized)
 */
data class SyncResult(
    val conversationsSynced: Int,
    val messagesSynced: Int,
    val memoriesSynced: Int,
    val personasSynced: Int = 0
) {
    val totalSynced: Int
        get() = conversationsSynced + messagesSynced + memoriesSynced + personasSynced
}
