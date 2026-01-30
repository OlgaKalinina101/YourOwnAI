package com.yourown.ai.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.yourown.ai.domain.model.AIProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "api_keys_secure"
        private const val KEY_DEEPSEEK = "deepseek_key"
        private const val KEY_OPENAI = "openai_key"
        private const val KEY_OPENROUTER = "openrouter_key"
        private const val KEY_XAI = "xai_key"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val _apiKeys = MutableStateFlow<Map<AIProvider, String>>(emptyMap())
    val apiKeys: StateFlow<Map<AIProvider, String>> = _apiKeys.asStateFlow()
    
    init {
        loadApiKeys()
    }
    
    private fun loadApiKeys() {
        val keys = mutableMapOf<AIProvider, String>()
        
        encryptedPrefs.getString(KEY_DEEPSEEK, null)?.let { keys[AIProvider.DEEPSEEK] = it }
        encryptedPrefs.getString(KEY_OPENAI, null)?.let { keys[AIProvider.OPENAI] = it }
        encryptedPrefs.getString(KEY_OPENROUTER, null)?.let { keys[AIProvider.OPENROUTER] = it }
        encryptedPrefs.getString(KEY_XAI, null)?.let { keys[AIProvider.XAI] = it }
        
        _apiKeys.value = keys
    }
    
    suspend fun saveApiKey(provider: AIProvider, apiKey: String) = withContext(Dispatchers.IO) {
        val prefKey = when (provider) {
            AIProvider.DEEPSEEK -> KEY_DEEPSEEK
            AIProvider.OPENAI -> KEY_OPENAI
            AIProvider.OPENROUTER -> KEY_OPENROUTER
            AIProvider.XAI -> KEY_XAI
            AIProvider.CUSTOM -> return@withContext // Not supported
        }
        
        encryptedPrefs.edit().putString(prefKey, apiKey).apply()
        
        _apiKeys.value = _apiKeys.value.toMutableMap().apply {
            this[provider] = apiKey
        }
    }
    
    suspend fun deleteApiKey(provider: AIProvider) = withContext(Dispatchers.IO) {
        val prefKey = when (provider) {
            AIProvider.DEEPSEEK -> KEY_DEEPSEEK
            AIProvider.OPENAI -> KEY_OPENAI
            AIProvider.OPENROUTER -> KEY_OPENROUTER
            AIProvider.XAI -> KEY_XAI
            AIProvider.CUSTOM -> return@withContext
        }
        
        encryptedPrefs.edit().remove(prefKey).apply()
        
        _apiKeys.value = _apiKeys.value.toMutableMap().apply {
            remove(provider)
        }
    }
    
    fun getApiKey(provider: AIProvider): String? {
        return _apiKeys.value[provider]
    }
    
    fun hasApiKey(provider: AIProvider): Boolean {
        return _apiKeys.value.containsKey(provider)
    }
    
    fun getDisplayKey(provider: AIProvider): String? {
        val key = _apiKeys.value[provider]
        return if (key != null && key.length > 4) {
            "****${key.takeLast(4)}"
        } else {
            null
        }
    }
}
