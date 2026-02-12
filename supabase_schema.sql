-- YourOwnAI Cloud Sync - Supabase Schema
-- Copy and run this in Supabase Dashboard → SQL Editor

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
    is_for_api BOOLEAN DEFAULT TRUE,
    
    -- AI Configuration
    temperature REAL DEFAULT 0.7,
    top_p REAL DEFAULT 0.9,
    max_tokens INTEGER DEFAULT 4096,
    deep_empathy BOOLEAN DEFAULT FALSE,
    memory_enabled BOOLEAN DEFAULT FALSE,
    rag_enabled BOOLEAN DEFAULT FALSE,
    message_history_limit INTEGER DEFAULT 10,
    
    -- Prompts
    deep_empathy_prompt TEXT,
    deep_empathy_analysis_prompt TEXT,
    memory_extraction_prompt TEXT,
    context_instructions TEXT,
    memory_instructions TEXT,
    rag_instructions TEXT,
    swipe_message_prompt TEXT,
    
    -- Memory Configuration
    memory_limit INTEGER DEFAULT 5,
    memory_min_age_days INTEGER DEFAULT 2,
    memory_title TEXT,
    
    -- RAG Configuration
    rag_chunk_size INTEGER DEFAULT 512,
    rag_chunk_overlap INTEGER DEFAULT 64,
    rag_chunk_limit INTEGER DEFAULT 5,
    rag_title TEXT,
    
    -- Model Preference
    preferred_model_id TEXT,
    preferred_provider TEXT,
    
    -- Document Links
    linked_document_ids TEXT,
    
    -- Memory Scope
    use_only_persona_memories BOOLEAN DEFAULT FALSE,
    share_memories_globally BOOLEAN DEFAULT TRUE,
    
    -- API Embeddings Configuration
    use_api_embeddings BOOLEAN DEFAULT FALSE,
    api_embeddings_provider TEXT DEFAULT 'openai',
    api_embeddings_model TEXT DEFAULT 'text-embedding-3-small',
    
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
CREATE INDEX IF NOT EXISTS idx_messages_device ON messages(device_id);
CREATE INDEX IF NOT EXISTS idx_memories_conversation ON memories(conversation_id);
CREATE INDEX IF NOT EXISTS idx_memories_persona ON memories(persona_id);
CREATE INDEX IF NOT EXISTS idx_document_embeddings_document ON document_embeddings(document_id);
CREATE INDEX IF NOT EXISTS idx_conversations_device ON conversations(device_id);
CREATE INDEX IF NOT EXISTS idx_conversations_updated ON conversations(updated_at);

-- Enable Row Level Security (RLS) - OPTIONAL
-- This ensures users can only access their own data
-- You'll need to configure RLS policies in Supabase Dashboard

-- ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE memories ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE personas ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE system_prompts ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE knowledge_documents ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE document_embeddings ENABLE ROW LEVEL SECURITY;

-- Success message
SELECT 'Schema created successfully! ✅' as status;
