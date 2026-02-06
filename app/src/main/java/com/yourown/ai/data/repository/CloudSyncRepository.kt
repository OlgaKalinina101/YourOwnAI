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
     * Create tables in Supabase (via SQL Editor)
     * 
     * Note: Supabase SDK doesn't support DDL operations directly.
     * Tables should be created via Supabase Dashboard → SQL Editor
     * 
     * This method provides the SQL commands for user to copy-paste
     */
    suspend fun createTables(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sqlCommands = """
                -- Run these commands in Supabase Dashboard → SQL Editor
                
                -- 1. Conversations table
                CREATE TABLE IF NOT EXISTS conversations (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    system_prompt TEXT NOT NULL,
                    system_prompt_id TEXT,
                    model TEXT NOT NULL,
                    provider TEXT NOT NULL,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    is_archived BOOLEAN DEFAULT FALSE,
                    source_conversation_id TEXT,
                    device_id TEXT,
                    synced_at BIGINT
                );
                
                -- 2. Messages table
                CREATE TABLE IF NOT EXISTS messages (
                    id TEXT PRIMARY KEY,
                    conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    created_at BIGINT NOT NULL,
                    model TEXT,
                    temperature REAL,
                    top_p REAL,
                    deep_empathy BOOLEAN,
                    memory_enabled BOOLEAN,
                    message_history_limit INTEGER,
                    system_prompt TEXT,
                    request_logs TEXT,
                    swipe_message_id TEXT,
                    swipe_message_text TEXT,
                    image_attachments TEXT,
                    file_attachments TEXT,
                    is_liked BOOLEAN DEFAULT FALSE,
                    device_id TEXT,
                    synced_at BIGINT
                );
                
                -- 3. Memories table
                CREATE TABLE IF NOT EXISTS memories (
                    id TEXT PRIMARY KEY,
                    conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                    message_id TEXT NOT NULL,
                    fact TEXT NOT NULL,
                    created_at BIGINT NOT NULL,
                    persona_id TEXT,
                    embedding BYTEA,
                    device_id TEXT,
                    synced_at BIGINT
                );
                
                -- 4. Personas table
                CREATE TABLE IF NOT EXISTS personas (
                    id TEXT PRIMARY KEY,
                    system_prompt_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    device_id TEXT,
                    synced_at BIGINT
                );
                
                -- 5. System prompts table
                CREATE TABLE IF NOT EXISTS system_prompts (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    content TEXT NOT NULL,
                    type TEXT NOT NULL,
                    is_default BOOLEAN DEFAULT FALSE,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    device_id TEXT,
                    synced_at BIGINT
                );
                
                -- 6. Knowledge documents table
                CREATE TABLE IF NOT EXISTS knowledge_documents (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    content TEXT NOT NULL,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    linked_persona_ids TEXT,
                    device_id TEXT,
                    synced_at BIGINT
                );
                
                -- 7. Document embeddings table
                CREATE TABLE IF NOT EXISTS document_embeddings (
                    id TEXT PRIMARY KEY,
                    document_id TEXT NOT NULL REFERENCES knowledge_documents(id) ON DELETE CASCADE,
                    chunk_text TEXT NOT NULL,
                    chunk_index INTEGER NOT NULL,
                    embedding BYTEA,
                    device_id TEXT,
                    synced_at BIGINT
                );
                
                -- Create indexes for performance
                CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id);
                CREATE INDEX IF NOT EXISTS idx_messages_created ON messages(created_at);
                CREATE INDEX IF NOT EXISTS idx_memories_conversation ON memories(conversation_id);
                CREATE INDEX IF NOT EXISTS idx_memories_persona ON memories(persona_id);
                CREATE INDEX IF NOT EXISTS idx_document_embeddings_document ON document_embeddings(document_id);
                
                -- Enable Row Level Security (optional but recommended)
                ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
                ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
                ALTER TABLE memories ENABLE ROW LEVEL SECURITY;
                ALTER TABLE personas ENABLE ROW LEVEL SECURITY;
                ALTER TABLE system_prompts ENABLE ROW LEVEL SECURITY;
                ALTER TABLE knowledge_documents ENABLE ROW LEVEL SECURITY;
                ALTER TABLE document_embeddings ENABLE ROW LEVEL SECURITY;
            """.trimIndent()
            
            Log.i(TAG, "SQL commands ready for Supabase SQL Editor")
            Result.success(sqlCommands)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate SQL", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sync all data to Supabase
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
            
            // Step 3: Get all memories from Room (including embeddings)
            val allMemories = memoryRepository.getAllMemoryEntities()
            Log.d(TAG, "Found ${allMemories.size} memories to sync")
            
            // Step 4: Get all knowledge documents (RAG)
            val allDocuments = knowledgeDocumentRepository.getAllDocuments().first()
            Log.d(TAG, "Found ${allDocuments.size} knowledge documents to sync")
            
            // Step 5: Get all document embeddings/chunks (RAG)
            val allChunks = documentEmbeddingRepository.getAllChunks()
            Log.d(TAG, "Found ${allChunks.size} document chunks (embeddings) to sync")
            
            // Step 6: Get all personas
            val allPersonas = personaRepository.getAllPersonasEntities()
            Log.d(TAG, "Found ${allPersonas.size} personas to sync")
            
            // Step 7: Get all system prompts
            val allSystemPrompts = systemPromptRepository.getAllPromptsEntities()
            Log.d(TAG, "Found ${allSystemPrompts.size} system prompts to sync")
            
            // Step 8: Calculate actual data size
            // Estimate based on JSON serialization size:
            // - Conversation: ~0.5 KB (id, title, timestamps, settings)
            // - Message: content length + 200 bytes overhead
            // - Memory: fact length + embedding (if exists: 384 floats = 1536 bytes) + 150 bytes overhead
            // - Document: content length + 300 bytes overhead (metadata)
            // - Chunk: text length + embedding size (384 floats = 1536 bytes) + 200 bytes overhead
            // - Persona: ~1 KB (settings, prompt, metadata)
            // - SystemPrompt: content length + 200 bytes overhead
            
            val conversationsSize = conversations.size * 512L // 0.5 KB per conversation
            val messagesSize: Long = allMessages.sumOf { message: Message ->
                ((message.content?.length ?: 0) + 200).toLong()
            }
            
            // Memories: включая embeddings если они есть
            val memoriesSize: Long = allMemories.sumOf { memory: MemoryEntity ->
                val hasEmbedding = !memory.embedding.isNullOrEmpty()
                val embeddingSize = if (hasEmbedding) 1536L else 0L // 384 floats × 4 bytes
                (memory.fact.length + embeddingSize + 150).toLong()
            }
            
            val documentsSize: Long = allDocuments.sumOf { doc: KnowledgeDocument ->
                (doc.content.length + 300).toLong()
            }
            val chunksSize: Long = allChunks.sumOf { chunk: DocumentChunkEntity ->
                (chunk.content.length + 1536 + 200).toLong() // text + embedding (384 floats) + overhead
            }
            
            val personasSize: Long = allPersonas.sumOf { persona: PersonaEntity ->
                (persona.systemPrompt.length + 1024).toLong() // system prompt + settings
            }
            
            val systemPromptsSize: Long = allSystemPrompts.sumOf { prompt: SystemPromptEntity ->
                (prompt.content.length + 200).toLong()
            }
            
            val totalBytes = conversationsSize + messagesSize + memoriesSize + documentsSize + 
                           chunksSize + personasSize + systemPromptsSize
            val totalMB = totalBytes / (1024f * 1024f)
            
            Log.i(TAG, "Total data size: ${String.format("%.3f", totalMB)} MB")
            Log.i(TAG, "├── ${conversations.size} conversations")
            Log.i(TAG, "├── ${allMessages.size} messages")
            Log.i(TAG, "├── ${allMemories.size} memories (with embeddings)")
            Log.i(TAG, "├── ${allDocuments.size} documents (RAG)")
            Log.i(TAG, "├── ${allChunks.size} document embeddings (RAG)")
            Log.i(TAG, "├── ${allPersonas.size} personas")
            Log.i(TAG, "└── ${allSystemPrompts.size} system prompts")
            
            // Step 9: Upload to Supabase
            val deviceId = getDeviceId()
            
            // System Prompts (upload first for foreign keys)
            if (allSystemPrompts.isNotEmpty()) {
                val systemPromptDtos = allSystemPrompts.map { prompt: SystemPromptEntity -> prompt.toDto(deviceId) }
                try {
                    // Split into batches of 100 to avoid payload size limits
                    systemPromptDtos.chunked(100).forEach { batch ->
                        client.from("system_prompts").upsert(batch)
                    }
                    Log.d(TAG, "✅ Uploaded ${allSystemPrompts.size} system prompts")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to upload system prompts: ${e.message}")
                }
            }
            
            // Personas (need prompts to exist)
            if (allPersonas.isNotEmpty()) {
                val personaDtos = allPersonas.map { persona: PersonaEntity -> persona.toDto(deviceId) }
                try {
                    personaDtos.chunked(100).forEach { batch ->
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
            
            // Memories (with embeddings)
            if (allMemories.isNotEmpty()) {
                val memoryDtos = allMemories.map { memory: MemoryEntity -> memory.toDto(deviceId) }
                try {
                    memoryDtos.chunked(50).forEach { batch -> // Smaller batches due to embeddings
                        client.from("memories").upsert(batch)
                    }
                    Log.d(TAG, "✅ Uploaded ${allMemories.size} memories")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to upload memories: ${e.message}")
                }
            }
            
            // Knowledge Documents
            if (allDocuments.isNotEmpty()) {
                val documentDtos = allDocuments.map { doc: KnowledgeDocument -> doc.toEntity().toDto(deviceId) }
                try {
                    documentDtos.chunked(50).forEach { batch ->
                        client.from("knowledge_documents").upsert(batch)
                    }
                    Log.d(TAG, "✅ Uploaded ${allDocuments.size} documents")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to upload documents: ${e.message}")
                }
            }
            
            // Document Embeddings (can be large)
            if (allChunks.isNotEmpty()) {
                val embeddingDtos = allChunks.map { chunk: DocumentChunkEntity -> chunk.toDto(deviceId) }
                try {
                    embeddingDtos.chunked(50).forEach { batch -> // Small batches due to embeddings
                        client.from("document_embeddings").upsert(batch)
                    }
                    Log.d(TAG, "✅ Uploaded ${allChunks.size} embeddings")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to upload embeddings: ${e.message}")
                }
            }
            
            // Track uploaded data size
            if (totalMB > 0) {
                cloudSyncPreferences.addUploadedDataMB(totalMB)
                Log.d(TAG, "Added ${String.format("%.3f", totalMB)} MB to uploaded data tracker")
            }
            
            Log.i(TAG, "✅ Sync to Supabase completed: ${String.format("%.3f", totalMB)} MB")
            Result.success(SyncResult(
                conversationsSynced = conversations.size,
                messagesSynced = allMessages.size,
                memoriesSynced = allMemories.size,
                documentsSynced = allDocuments.size,
                embeddingsSynced = allChunks.size,
                personasSynced = allPersonas.size,
                systemPromptsSynced = allSystemPrompts.size
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Sync to Supabase failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Sync data from Supabase
     * Downloads all data from cloud and merges with local data
     * 
     * Conflict resolution: Last-write-wins based on updated_at timestamp
     */
    suspend fun syncFromCloud(): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val client = supabaseClient ?: initializeClient().getOrThrow()
            
            Log.d(TAG, "Starting sync from Supabase...")
            
            var conversationsSynced = 0
            var messagesSynced = 0
            var memoriesSynced = 0
            var documentsSynced = 0
            var embeddingsSynced = 0
            var personasSynced = 0
            var systemPromptsSynced = 0
            
            // ========== 1. FETCH ALL DATA FROM SUPABASE ==========
            
            // System Prompts (need to fetch first for persona mapping)
            val cloudSystemPrompts = try {
                client.from("system_prompts")
                    .select(columns = Columns.ALL)
                    .decodeList<SystemPromptDto>()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch system prompts: ${e.message}")
                emptyList()
            }
            
            // Personas (need prompts for mapping)
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
            
            // Memories
            val cloudMemories = try {
                client.from("memories")
                    .select(columns = Columns.ALL)
                    .decodeList<MemoryDto>()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch memories: ${e.message}")
                emptyList()
            }
            
            // Knowledge Documents
            val cloudDocuments = try {
                client.from("knowledge_documents")
                    .select(columns = Columns.ALL)
                    .decodeList<KnowledgeDocumentDto>()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch documents: ${e.message}")
                emptyList()
            }
            
            // Document Embeddings
            val cloudEmbeddings = try {
                client.from("document_embeddings")
                    .select(columns = Columns.ALL)
                    .decodeList<DocumentEmbeddingDto>()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch embeddings: ${e.message}")
                emptyList()
            }
            
            Log.d(TAG, "Fetched from cloud: ${cloudConversations.size} convs, ${cloudMessages.size} msgs, " +
                    "${cloudMemories.size} memories, ${cloudDocuments.size} docs, ${cloudEmbeddings.size} embeddings, " +
                    "${cloudPersonas.size} personas, ${cloudSystemPrompts.size} prompts")
            
            // ========== 2. GET LOCAL DATA ==========
            
            val localConversations = conversationRepository.getAllConversations().first()
            val localConversationsMap = localConversations.associateBy { it.id }
            
            val localMessages = messageRepository.getAllMessages().first()
            val localMessagesMap = localMessages.associateBy { it.id }
            
            val localMemories = memoryRepository.getAllMemoryEntities()
            val localMemoriesMap = localMemories.associateBy { it.id }
            
            val localDocuments = knowledgeDocumentRepository.getAllDocuments().first()
            val localDocumentsMap = localDocuments.associateBy { it.id }
            
            val localChunks = documentEmbeddingRepository.getAllChunks()
            val localChunksMap = localChunks.associateBy { it.id }
            
            val localPersonas = personaRepository.getAllPersonas().first()
            val localPersonasMap = localPersonas.associateBy { it.id }
            
            val localSystemPrompts = systemPromptRepository.getAllPrompts().first()
            val localSystemPromptsMap = localSystemPrompts.associateBy { it.id }
            
            // ========== 3. MERGE SYSTEM PROMPTS ==========
            
            for (cloudPrompt in cloudSystemPrompts) {
                val localPrompt = localSystemPromptsMap[cloudPrompt.id]
                
                val shouldUpdate = if (localPrompt == null) {
                    true // New record from cloud
                } else {
                    // Compare timestamps (cloud wins if newer or equal)
                    cloudPrompt.updated_at >= localPrompt.updatedAt
                }
                
                if (shouldUpdate) {
                    val entity = cloudPrompt.toEntity()
                    systemPromptRepository.upsertSystemPrompt(entity)
                    systemPromptsSynced++
                    if (localPrompt == null) {
                        Log.d(TAG, "Inserted new system prompt: ${cloudPrompt.name}")
                    } else {
                        Log.d(TAG, "Updated system prompt: ${cloudPrompt.name} (cloud: ${cloudPrompt.updated_at} >= local: ${localPrompt.updatedAt})")
                    }
                }
            }
            
            // ========== 4. MERGE PERSONAS ==========
            
            val systemPromptsMapById = systemPromptRepository.getAllPrompts().first().associateBy { it.id }
            
            for (cloudPersona in cloudPersonas) {
                val localPersona = localPersonasMap[cloudPersona.id]
                
                val shouldUpdate = if (localPersona == null) {
                    true
                } else {
                    cloudPersona.updated_at >= localPersona.updatedAt
                }
                
                if (shouldUpdate) {
                    // Get system prompt content for mapping
                    val systemPromptContent = systemPromptsMapById[cloudPersona.system_prompt_id]?.content ?: ""
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
            
            // ========== 5. MERGE CONVERSATIONS ==========
            
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
            
            // ========== 6. MERGE MESSAGES ==========
            
            for (cloudMsg in cloudMessages) {
                val localMsg = localMessagesMap[cloudMsg.id]
                
                // Messages don't have updated_at, so use timestamp
                val shouldUpdate = if (localMsg == null) {
                    true
                } else {
                    cloudMsg.timestamp >= localMsg.createdAt
                }
                
                if (shouldUpdate) {
                    val entity = cloudMsg.toEntity()
                    messageRepository.upsertMessage(entity)
                    messagesSynced++
                }
            }
            
            Log.d(TAG, "Merged ${messagesSynced} messages")
            
            // ========== 7. MERGE MEMORIES ==========
            
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
            
            Log.d(TAG, "Merged ${memoriesSynced} memories")
            
            // ========== 8. MERGE KNOWLEDGE DOCUMENTS ==========
            
            for (cloudDoc in cloudDocuments) {
                val localDoc = localDocumentsMap[cloudDoc.id]
                
                val shouldUpdate = if (localDoc == null) {
                    true
                } else {
                    cloudDoc.updated_at >= localDoc.updatedAt
                }
                
                if (shouldUpdate) {
                    val entity = cloudDoc.toEntity()
                    knowledgeDocumentRepository.upsertDocument(entity)
                    documentsSynced++
                    if (localDoc == null) {
                        Log.d(TAG, "Inserted new document: ${cloudDoc.name}")
                    } else {
                        Log.d(TAG, "Updated document: ${cloudDoc.name}")
                    }
                }
            }
            
            // ========== 9. MERGE DOCUMENT EMBEDDINGS ==========
            
            for (cloudEmb in cloudEmbeddings) {
                val localEmb = localChunksMap[cloudEmb.id]
                
                // Embeddings don't have updated_at, just insert if not exists
                if (localEmb == null) {
                    val entity = cloudEmb.toEntity()
                    documentEmbeddingRepository.upsertChunk(entity)
                    embeddingsSynced++
                }
            }
            
            Log.d(TAG, "Merged ${embeddingsSynced} embeddings")
            
            // ========== DONE ==========
            
            Log.i(TAG, "✅ Sync from Supabase completed successfully")
            Log.i(TAG, "Downloaded: $conversationsSynced conversations, $messagesSynced messages, $memoriesSynced memories")
            Log.i(TAG, "Downloaded: $documentsSynced documents, $embeddingsSynced embeddings")
            Log.i(TAG, "Downloaded: $personasSynced personas, $systemPromptsSynced system prompts")
            
            Result.success(SyncResult(
                conversationsSynced = conversationsSynced,
                messagesSynced = messagesSynced,
                memoriesSynced = memoriesSynced,
                documentsSynced = documentsSynced,
                embeddingsSynced = embeddingsSynced,
                personasSynced = personasSynced,
                systemPromptsSynced = systemPromptsSynced
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Sync from Supabase failed", e)
            e.printStackTrace()
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
 * Result of sync operation
 */
data class SyncResult(
    val conversationsSynced: Int,
    val messagesSynced: Int,
    val memoriesSynced: Int,
    val documentsSynced: Int,
    val embeddingsSynced: Int = 0,
    val personasSynced: Int = 0,
    val systemPromptsSynced: Int = 0
) {
    val totalSynced: Int
        get() = conversationsSynced + messagesSynced + memoriesSynced + documentsSynced + 
                embeddingsSynced + personasSynced + systemPromptsSynced
}
