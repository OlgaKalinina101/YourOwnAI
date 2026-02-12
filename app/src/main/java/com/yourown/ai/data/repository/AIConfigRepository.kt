package com.yourown.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.domain.model.UserContext
import com.yourown.ai.domain.model.UserGender
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_config")

/**
 * Repository for AI configuration and user context
 */
@Singleton
class AIConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.aiConfigDataStore
    
    companion object {
        private val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val LOCAL_SYSTEM_PROMPT = stringPreferencesKey("local_system_prompt")
        private val MEMORY_EXTRACTION_PROMPT = stringPreferencesKey("memory_extraction_prompt")
        private val TEMPERATURE = floatPreferencesKey("temperature")
        private val TOP_P = floatPreferencesKey("top_p")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val DEEP_EMPATHY = booleanPreferencesKey("deep_empathy")
        private val DEEP_EMPATHY_PROMPT = stringPreferencesKey("deep_empathy_prompt")
        private val DEEP_EMPATHY_ANALYSIS_PROMPT = stringPreferencesKey("deep_empathy_analysis_prompt")
        private val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        private val RAG_ENABLED = booleanPreferencesKey("rag_enabled")
        private val RAG_CHUNK_SIZE = intPreferencesKey("rag_chunk_size")
        private val RAG_CHUNK_OVERLAP = intPreferencesKey("rag_chunk_overlap")
        private val MEMORY_LIMIT = intPreferencesKey("memory_limit")
        private val MEMORY_MIN_AGE_DAYS = intPreferencesKey("memory_min_age_days")
        private val MEMORY_TITLE = stringPreferencesKey("memory_title")
        private val MEMORY_INSTRUCTIONS = stringPreferencesKey("memory_instructions")
        private val RAG_CHUNK_LIMIT = intPreferencesKey("rag_chunk_limit")
        private val RAG_TITLE = stringPreferencesKey("rag_title")
        private val RAG_INSTRUCTIONS = stringPreferencesKey("rag_instructions")
        private val USE_API_EMBEDDINGS = booleanPreferencesKey("use_api_embeddings")
        private val API_EMBEDDINGS_PROVIDER = stringPreferencesKey("api_embeddings_provider")
        private val API_EMBEDDINGS_MODEL = stringPreferencesKey("api_embeddings_model")
        private val CONTEXT_INSTRUCTIONS = stringPreferencesKey("context_instructions")
        private val SWIPE_MESSAGE_PROMPT = stringPreferencesKey("swipe_message_prompt")
        private val MESSAGE_HISTORY_LIMIT = intPreferencesKey("message_history_limit")
        
        private val USER_CONTEXT = stringPreferencesKey("user_context")
        private val USER_GENDER = stringPreferencesKey("user_gender")
    }
    
    /**
     * Get AI configuration as Flow
     */
    val aiConfig: Flow<AIConfig> = dataStore.data.map { preferences ->
        AIConfig(
            systemPrompt = preferences[SYSTEM_PROMPT] ?: AIConfig.DEFAULT_SYSTEM_PROMPT,
            localSystemPrompt = preferences[LOCAL_SYSTEM_PROMPT] ?: AIConfig.DEFAULT_LOCAL_SYSTEM_PROMPT,
            memoryExtractionPrompt = preferences[MEMORY_EXTRACTION_PROMPT] ?: AIConfig.DEFAULT_MEMORY_EXTRACTION_PROMPT,
            temperature = preferences[TEMPERATURE] ?: 0.7f,
            topP = preferences[TOP_P] ?: 0.9f,
            maxTokens = preferences[MAX_TOKENS] ?: 4096,
            deepEmpathy = preferences[DEEP_EMPATHY] ?: false,
            deepEmpathyPrompt = preferences[DEEP_EMPATHY_PROMPT] ?: AIConfig.DEFAULT_DEEP_EMPATHY_PROMPT,
            deepEmpathyAnalysisPrompt = preferences[DEEP_EMPATHY_ANALYSIS_PROMPT] ?: AIConfig.DEFAULT_DEEP_EMPATHY_ANALYSIS_PROMPT,
            memoryEnabled = preferences[MEMORY_ENABLED] ?: true,
            memoryLimit = preferences[MEMORY_LIMIT] ?: 5,
            memoryMinAgeDays = preferences[MEMORY_MIN_AGE_DAYS] ?: 2,
            memoryTitle = preferences[MEMORY_TITLE] ?: "Твои воспоминания",
            memoryInstructions = preferences[MEMORY_INSTRUCTIONS] ?: AIConfig.DEFAULT_MEMORY_INSTRUCTIONS,
            ragEnabled = preferences[RAG_ENABLED] ?: false,
            ragChunkSize = preferences[RAG_CHUNK_SIZE] ?: 512,
            ragChunkOverlap = preferences[RAG_CHUNK_OVERLAP] ?: 64,
            ragChunkLimit = preferences[RAG_CHUNK_LIMIT] ?: 5,
            ragTitle = preferences[RAG_TITLE] ?: "Твоя библиотека текстов",
            ragInstructions = preferences[RAG_INSTRUCTIONS] ?: AIConfig.DEFAULT_RAG_INSTRUCTIONS,
            useApiEmbeddings = preferences[USE_API_EMBEDDINGS] ?: false,
            apiEmbeddingsProvider = preferences[API_EMBEDDINGS_PROVIDER] ?: "openai",
            apiEmbeddingsModel = preferences[API_EMBEDDINGS_MODEL] ?: "text-embedding-3-small",
            contextInstructions = preferences[CONTEXT_INSTRUCTIONS] ?: AIConfig.DEFAULT_CONTEXT_INSTRUCTIONS,
            swipeMessagePrompt = preferences[SWIPE_MESSAGE_PROMPT] ?: AIConfig.DEFAULT_SWIPE_MESSAGE_PROMPT,
            messageHistoryLimit = preferences[MESSAGE_HISTORY_LIMIT] ?: 10
        )
    }
    
    /**
     * Get user context as Flow
     */
    val userContext: Flow<UserContext> = dataStore.data.map { preferences ->
        UserContext(
            content = preferences[USER_CONTEXT] ?: "",
            gender = UserGender.fromValue(preferences[USER_GENDER] ?: "other")
        )
    }
    
    /**
     * Update system prompt (API models)
     */
    suspend fun updateSystemPrompt(prompt: String) {
        dataStore.edit { preferences ->
            preferences[SYSTEM_PROMPT] = prompt
        }
    }
    
    /**
     * Update local system prompt (Local models)
     */
    suspend fun updateLocalSystemPrompt(prompt: String) {
        dataStore.edit { preferences ->
            preferences[LOCAL_SYSTEM_PROMPT] = prompt
        }
    }
    
    /**
     * Update memory extraction prompt
     */
    suspend fun updateMemoryExtractionPrompt(prompt: String) {
        dataStore.edit { preferences ->
            preferences[MEMORY_EXTRACTION_PROMPT] = prompt
        }
    }
    
    /**
     * Reset memory extraction prompt to default
     */
    suspend fun resetMemoryExtractionPrompt() {
        dataStore.edit { preferences ->
            preferences.remove(MEMORY_EXTRACTION_PROMPT)
        }
    }
    
    /**
     * Update deep empathy prompt
     */
    suspend fun updateDeepEmpathyPrompt(prompt: String) {
        dataStore.edit { preferences ->
            preferences[DEEP_EMPATHY_PROMPT] = prompt
        }
    }
    
    /**
     * Reset deep empathy prompt to default
     */
    suspend fun resetDeepEmpathyPrompt() {
        dataStore.edit { preferences ->
            preferences.remove(DEEP_EMPATHY_PROMPT)
        }
    }
    
    /**
     * Update deep empathy analysis prompt
     */
    suspend fun updateDeepEmpathyAnalysisPrompt(prompt: String) {
        dataStore.edit { preferences ->
            preferences[DEEP_EMPATHY_ANALYSIS_PROMPT] = prompt
        }
    }
    
    /**
     * Reset deep empathy analysis prompt to default
     */
    suspend fun resetDeepEmpathyAnalysisPrompt() {
        dataStore.edit { preferences ->
            preferences.remove(DEEP_EMPATHY_ANALYSIS_PROMPT)
        }
    }
    
    /**
     * Update temperature
     */
    suspend fun updateTemperature(value: Float) {
        dataStore.edit { preferences ->
            preferences[TEMPERATURE] = value.coerceIn(AIConfig.MIN_TEMPERATURE, AIConfig.MAX_TEMPERATURE)
        }
    }
    
    /**
     * Update top-p
     */
    suspend fun updateTopP(value: Float) {
        dataStore.edit { preferences ->
            preferences[TOP_P] = value.coerceIn(AIConfig.MIN_TOP_P, AIConfig.MAX_TOP_P)
        }
    }
    
    /**
     * Update max tokens
     */
    suspend fun updateMaxTokens(value: Int) {
        dataStore.edit { preferences ->
            preferences[MAX_TOKENS] = value.coerceIn(AIConfig.MIN_MAX_TOKENS, AIConfig.MAX_MAX_TOKENS)
        }
    }
    
    /**
     * Toggle deep empathy
     */
    suspend fun setDeepEmpathy(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DEEP_EMPATHY] = enabled
        }
    }
    
    /**
     * Toggle memory
     */
    suspend fun setMemoryEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[MEMORY_ENABLED] = enabled
        }
    }
    
    /**
     * Toggle RAG (Retrieval Augmented Generation)
     */
    suspend fun setRAGEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[RAG_ENABLED] = enabled
        }
    }
    
    /**
     * Update RAG chunk size
     */
    suspend fun updateRAGChunkSize(value: Int) {
        dataStore.edit { preferences ->
            preferences[RAG_CHUNK_SIZE] = value.coerceIn(
                AIConfig.MIN_CHUNK_SIZE,
                AIConfig.MAX_CHUNK_SIZE
            )
        }
    }
    
    /**
     * Update RAG chunk overlap
     */
    suspend fun updateRAGChunkOverlap(value: Int) {
        dataStore.edit { preferences ->
            preferences[RAG_CHUNK_OVERLAP] = value.coerceIn(
                AIConfig.MIN_CHUNK_OVERLAP,
                AIConfig.MAX_CHUNK_OVERLAP
            )
        }
    }
    
    /**
     * Update memory limit (number of memories to include)
     */
    suspend fun updateMemoryLimit(value: Int) {
        dataStore.edit { preferences ->
            preferences[MEMORY_LIMIT] = value.coerceIn(
                AIConfig.MIN_MEMORY_LIMIT,
                AIConfig.MAX_MEMORY_LIMIT
            )
        }
    }
    
    /**
     * Update memory minimum age in days
     */
    suspend fun updateMemoryMinAgeDays(value: Int) {
        dataStore.edit { preferences ->
            preferences[MEMORY_MIN_AGE_DAYS] = value.coerceIn(
                AIConfig.MIN_MEMORY_MIN_AGE_DAYS,
                AIConfig.MAX_MEMORY_MIN_AGE_DAYS
            )
        }
    }
    
    /**
     * Update RAG chunk limit (number of chunks to include)
     */
    suspend fun updateRAGChunkLimit(value: Int) {
        dataStore.edit { preferences ->
            preferences[RAG_CHUNK_LIMIT] = value.coerceIn(
                AIConfig.MIN_RAG_CHUNK_LIMIT,
                AIConfig.MAX_RAG_CHUNK_LIMIT
            )
        }
    }
    
    /**
     * Update memory title
     */
    suspend fun updateMemoryTitle(title: String) {
        dataStore.edit { preferences ->
            preferences[MEMORY_TITLE] = title
        }
    }
    
    /**
     * Update memory instructions
     */
    suspend fun updateMemoryInstructions(instructions: String) {
        dataStore.edit { preferences ->
            preferences[MEMORY_INSTRUCTIONS] = instructions
        }
    }
    
    /**
     * Reset memory instructions to default
     */
    suspend fun resetMemoryInstructions() {
        dataStore.edit { preferences ->
            preferences.remove(MEMORY_INSTRUCTIONS)
        }
    }
    
    /**
     * Update RAG title
     */
    suspend fun updateRAGTitle(title: String) {
        dataStore.edit { preferences ->
            preferences[RAG_TITLE] = title
        }
    }
    
    /**
     * Update RAG instructions
     */
    suspend fun updateRAGInstructions(instructions: String) {
        dataStore.edit { preferences ->
            preferences[RAG_INSTRUCTIONS] = instructions
        }
    }
    
    /**
     * Reset RAG instructions to default
     */
    suspend fun resetRAGInstructions() {
        dataStore.edit { preferences ->
            preferences.remove(RAG_INSTRUCTIONS)
        }
    }
    
    /**
     * Update context instructions
     */
    suspend fun updateContextInstructions(instructions: String) {
        dataStore.edit { preferences ->
            preferences[CONTEXT_INSTRUCTIONS] = instructions
        }
    }
    
    /**
     * Reset context instructions to default
     */
    suspend fun resetContextInstructions() {
        dataStore.edit { preferences ->
            preferences.remove(CONTEXT_INSTRUCTIONS)
        }
    }
    
    suspend fun updateSwipeMessagePrompt(prompt: String) {
        dataStore.edit { preferences ->
            preferences[SWIPE_MESSAGE_PROMPT] = prompt
        }
    }
    
    suspend fun resetSwipeMessagePrompt() {
        dataStore.edit { preferences ->
            preferences.remove(SWIPE_MESSAGE_PROMPT)
        }
    }
    
    /**
     * Update message history limit
     */
    suspend fun updateMessageHistoryLimit(value: Int) {
        dataStore.edit { preferences ->
            preferences[MESSAGE_HISTORY_LIMIT] = value.coerceIn(
                AIConfig.MIN_MESSAGE_HISTORY,
                AIConfig.MAX_MESSAGE_HISTORY
            )
        }
    }
    
    /**
     * Update user context
     */
    suspend fun updateUserContext(content: String, gender: UserGender? = null) {
        dataStore.edit { preferences ->
            preferences[USER_CONTEXT] = content
            if (gender != null) {
                preferences[USER_GENDER] = gender.value
            }
        }
    }
    
    /**
     * Update user gender
     */
    suspend fun updateUserGender(gender: UserGender) {
        dataStore.edit { preferences ->
            preferences[USER_GENDER] = gender.value
        }
    }
    
    /**
     * Get current AI config (suspend function for one-time read)
     */
    suspend fun getAIConfig(): AIConfig {
        val preferences = dataStore.data.map { it }.first()
        return AIConfig(
            systemPrompt = preferences[SYSTEM_PROMPT] ?: AIConfig.DEFAULT_SYSTEM_PROMPT,
            localSystemPrompt = preferences[LOCAL_SYSTEM_PROMPT] ?: AIConfig.DEFAULT_LOCAL_SYSTEM_PROMPT,
            memoryExtractionPrompt = preferences[MEMORY_EXTRACTION_PROMPT] ?: AIConfig.DEFAULT_MEMORY_EXTRACTION_PROMPT,
            temperature = preferences[TEMPERATURE] ?: 0.7f,
            topP = preferences[TOP_P] ?: 0.9f,
            maxTokens = preferences[MAX_TOKENS] ?: 4096,
            deepEmpathy = preferences[DEEP_EMPATHY] ?: false,
            deepEmpathyPrompt = preferences[DEEP_EMPATHY_PROMPT] ?: AIConfig.DEFAULT_DEEP_EMPATHY_PROMPT,
            deepEmpathyAnalysisPrompt = preferences[DEEP_EMPATHY_ANALYSIS_PROMPT] ?: AIConfig.DEFAULT_DEEP_EMPATHY_ANALYSIS_PROMPT,
            memoryEnabled = preferences[MEMORY_ENABLED] ?: true,
            memoryLimit = preferences[MEMORY_LIMIT] ?: 5,
            memoryMinAgeDays = preferences[MEMORY_MIN_AGE_DAYS] ?: 2,
            memoryTitle = preferences[MEMORY_TITLE] ?: "Твои воспоминания",
            memoryInstructions = preferences[MEMORY_INSTRUCTIONS] ?: AIConfig.DEFAULT_MEMORY_INSTRUCTIONS,
            ragEnabled = preferences[RAG_ENABLED] ?: false,
            ragChunkSize = preferences[RAG_CHUNK_SIZE] ?: 512,
            ragChunkOverlap = preferences[RAG_CHUNK_OVERLAP] ?: 64,
            ragChunkLimit = preferences[RAG_CHUNK_LIMIT] ?: 5,
            ragTitle = preferences[RAG_TITLE] ?: "Твоя библиотека текстов",
            ragInstructions = preferences[RAG_INSTRUCTIONS] ?: AIConfig.DEFAULT_RAG_INSTRUCTIONS,
            useApiEmbeddings = preferences[USE_API_EMBEDDINGS] ?: false,
            apiEmbeddingsProvider = preferences[API_EMBEDDINGS_PROVIDER] ?: "openai",
            apiEmbeddingsModel = preferences[API_EMBEDDINGS_MODEL] ?: "text-embedding-3-small",
            contextInstructions = preferences[CONTEXT_INSTRUCTIONS] ?: AIConfig.DEFAULT_CONTEXT_INSTRUCTIONS,
            swipeMessagePrompt = preferences[SWIPE_MESSAGE_PROMPT] ?: AIConfig.DEFAULT_SWIPE_MESSAGE_PROMPT,
            messageHistoryLimit = preferences[MESSAGE_HISTORY_LIMIT] ?: 10
        )
    }
    
    /**
     * Set use API embeddings flag
     */
    suspend fun setUseApiEmbeddings(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_API_EMBEDDINGS] = enabled
        }
    }
    
    /**
     * Set API embeddings provider
     */
    suspend fun setApiEmbeddingsProvider(provider: String) {
        dataStore.edit { preferences ->
            preferences[API_EMBEDDINGS_PROVIDER] = provider
        }
    }
    
    /**
     * Set API embeddings model
     */
    suspend fun setApiEmbeddingsModel(model: String) {
        dataStore.edit { preferences ->
            preferences[API_EMBEDDINGS_MODEL] = model
        }
    }
    
    /**
     * Get current user context (suspend function for one-time read)
     */
    suspend fun getUserContext(): UserContext {
        val preferences = dataStore.data.map { it }.first()
        return UserContext(
            content = preferences[USER_CONTEXT] ?: "",
            gender = UserGender.fromValue(preferences[USER_GENDER] ?: "other")
        )
    }
}
