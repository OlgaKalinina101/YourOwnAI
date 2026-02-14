-- YourOwnAI Cloud Sync - Supabase Schema (Optimized for Free Tier)
-- Copy and run this in Supabase Dashboard → SQL Editor
--
-- This schema syncs only essential data:
-- ✅ Conversations (basic fields)
-- ✅ Messages (without request_logs, with attachments)
-- ✅ Memories (WITHOUT embeddings - generated locally)
-- ✅ Personas (all fields - important for functionality)
--
-- NOT synced (to save space):
-- ❌ System Prompts (stored only locally)
-- ❌ Knowledge Documents (RAG - stored only locally)
-- ❌ Document Embeddings (RAG - stored only locally)
-- ❌ Memory embeddings (generated locally on each device)

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

-- 4. Personas table (full sync - important for functionality)
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
    
    -- Document Links (stored as JSON array string)
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

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_created ON messages(created_at);
CREATE INDEX IF NOT EXISTS idx_messages_device ON messages(device_id);
CREATE INDEX IF NOT EXISTS idx_memories_conversation ON memories(conversation_id);
CREATE INDEX IF NOT EXISTS idx_memories_persona ON memories(persona_id);
CREATE INDEX IF NOT EXISTS idx_conversations_device ON conversations(device_id);
CREATE INDEX IF NOT EXISTS idx_conversations_updated ON conversations(updated_at);
CREATE INDEX IF NOT EXISTS idx_conversations_persona ON conversations(persona_id);
CREATE INDEX IF NOT EXISTS idx_personas_updated ON personas(updated_at);

-- Enable Row Level Security (RLS) - OPTIONAL
-- This ensures users can only access their own data
-- You'll need to configure RLS policies in Supabase Dashboard

-- ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE memories ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE personas ENABLE ROW LEVEL SECURITY;

-- Success message
SELECT 'Optimized schema created successfully! ✅' as status;
SELECT 'This schema is optimized for Supabase Free Tier (500 MB, 5 GB bandwidth/month)' as info;
