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
        private val TEMPERATURE = floatPreferencesKey("temperature")
        private val TOP_P = floatPreferencesKey("top_p")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val DEEP_EMPATHY = booleanPreferencesKey("deep_empathy")
        private val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
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
            temperature = preferences[TEMPERATURE] ?: 0.7f,
            topP = preferences[TOP_P] ?: 0.9f,
            maxTokens = preferences[MAX_TOKENS] ?: 4096,
            deepEmpathy = preferences[DEEP_EMPATHY] ?: false,
            memoryEnabled = preferences[MEMORY_ENABLED] ?: true,
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
            temperature = preferences[TEMPERATURE] ?: 0.7f,
            topP = preferences[TOP_P] ?: 0.9f,
            maxTokens = preferences[MAX_TOKENS] ?: 4096,
            deepEmpathy = preferences[DEEP_EMPATHY] ?: false,
            memoryEnabled = preferences[MEMORY_ENABLED] ?: true,
            messageHistoryLimit = preferences[MESSAGE_HISTORY_LIMIT] ?: 10
        )
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
