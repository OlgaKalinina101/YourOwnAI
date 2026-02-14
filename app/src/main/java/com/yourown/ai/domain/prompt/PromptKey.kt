package com.yourown.ai.domain.prompt

/**
 * Keys for all translatable prompts in the app
 */
enum class PromptKey {
    // Main system prompts
    SYSTEM_PROMPT,
    LOCAL_SYSTEM_PROMPT,
    
    // Memory extraction
    MEMORY_EXTRACTION_PROMPT,
    
    // Deep Empathy
    DEEP_EMPATHY_PROMPT,
    DEEP_EMPATHY_ANALYSIS_PROMPT,
    
    // Context and instructions
    CONTEXT_INSTRUCTIONS,
    MEMORY_INSTRUCTIONS,
    RAG_INSTRUCTIONS,
    SWIPE_MESSAGE_PROMPT,
    
    // Biography generation
    BIOGRAPHY_INITIAL_PROMPT,
    BIOGRAPHY_UPDATE_PROMPT,
    
    // Memory cleaning
    MEMORY_CLEANING_PROMPT
}
