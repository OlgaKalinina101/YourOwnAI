-- YourOwnAI Cloud Sync - Migration Script for Personas Full Sync
-- Run this if you already created the schema before and want to add persona fields
-- Go to Supabase Dashboard → SQL Editor → Run this script

-- Add new columns to personas table
ALTER TABLE personas ADD COLUMN IF NOT EXISTS is_for_api BOOLEAN DEFAULT TRUE;

-- AI Configuration
ALTER TABLE personas ADD COLUMN IF NOT EXISTS temperature REAL DEFAULT 0.7;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS top_p REAL DEFAULT 0.9;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS max_tokens INTEGER DEFAULT 4096;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS deep_empathy BOOLEAN DEFAULT FALSE;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS memory_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS rag_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS message_history_limit INTEGER DEFAULT 10;

-- Prompts
ALTER TABLE personas ADD COLUMN IF NOT EXISTS deep_empathy_prompt TEXT;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS deep_empathy_analysis_prompt TEXT;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS memory_extraction_prompt TEXT;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS context_instructions TEXT;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS memory_instructions TEXT;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS rag_instructions TEXT;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS swipe_message_prompt TEXT;

-- Memory Configuration
ALTER TABLE personas ADD COLUMN IF NOT EXISTS memory_limit INTEGER DEFAULT 5;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS memory_min_age_days INTEGER DEFAULT 2;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS memory_title TEXT;

-- RAG Configuration
ALTER TABLE personas ADD COLUMN IF NOT EXISTS rag_chunk_size INTEGER DEFAULT 512;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS rag_chunk_overlap INTEGER DEFAULT 64;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS rag_chunk_limit INTEGER DEFAULT 5;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS rag_title TEXT;

-- Model Preference
ALTER TABLE personas ADD COLUMN IF NOT EXISTS preferred_model_id TEXT;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS preferred_provider TEXT;

-- Document Links
ALTER TABLE personas ADD COLUMN IF NOT EXISTS linked_document_ids TEXT;

-- Memory Scope
ALTER TABLE personas ADD COLUMN IF NOT EXISTS use_only_persona_memories BOOLEAN DEFAULT FALSE;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS share_memories_globally BOOLEAN DEFAULT TRUE;

-- API Embeddings Configuration (NEW)
ALTER TABLE personas ADD COLUMN IF NOT EXISTS use_api_embeddings BOOLEAN DEFAULT FALSE;
ALTER TABLE personas ADD COLUMN IF NOT EXISTS api_embeddings_provider TEXT DEFAULT 'openai';
ALTER TABLE personas ADD COLUMN IF NOT EXISTS api_embeddings_model TEXT DEFAULT 'text-embedding-3-small';

-- Success message
SELECT 'Personas table migration completed successfully! ✅' as status;
SELECT 'Now all persona settings will be synced across devices' as info;
