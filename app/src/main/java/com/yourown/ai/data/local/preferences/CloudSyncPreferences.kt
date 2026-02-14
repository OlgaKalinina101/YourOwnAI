package com.yourown.ai.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.yourown.ai.domain.model.CloudSyncSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for Cloud Sync settings using EncryptedSharedPreferences
 */
@Singleton
class CloudSyncPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "cloud_sync_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val _cloudSyncSettings = MutableStateFlow(loadSettings())
    val cloudSyncSettings: Flow<CloudSyncSettings> = _cloudSyncSettings.asStateFlow()
    
    companion object {
        private const val KEY_ENABLED = "cloud_sync_enabled"
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_KEY = "supabase_key"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_SYNC_ONLY_ON_WIFI = "sync_only_on_wifi"
    }
    
    private fun loadSettings(): CloudSyncSettings {
        return CloudSyncSettings(
            enabled = encryptedPreferences.getBoolean(KEY_ENABLED, false),
            supabaseUrl = encryptedPreferences.getString(KEY_SUPABASE_URL, "") ?: "",
            supabaseKey = encryptedPreferences.getString(KEY_SUPABASE_KEY, "") ?: "",
            autoSyncEnabled = encryptedPreferences.getBoolean(KEY_AUTO_SYNC_ENABLED, false),
            syncIntervalMinutes = encryptedPreferences.getInt(KEY_SYNC_INTERVAL_MINUTES, 30),
            lastSyncTimestamp = encryptedPreferences.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L),
            syncOnlyOnWifi = encryptedPreferences.getBoolean(KEY_SYNC_ONLY_ON_WIFI, true)
        )
    }
    
    suspend fun saveSupabaseCredentials(url: String, key: String) {
        encryptedPreferences.edit()
            .putString(KEY_SUPABASE_URL, url)
            .putString(KEY_SUPABASE_KEY, key)
            .apply()
        _cloudSyncSettings.value = loadSettings()
    }
    
    fun getSupabaseUrl(): String {
        return encryptedPreferences.getString(KEY_SUPABASE_URL, "") ?: ""
    }
    
    fun getSupabaseKey(): String {
        return encryptedPreferences.getString(KEY_SUPABASE_KEY, "") ?: ""
    }
    
    suspend fun setEnabled(enabled: Boolean) {
        encryptedPreferences.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        _cloudSyncSettings.value = loadSettings()
    }
    
    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        encryptedPreferences.edit()
            .putBoolean(KEY_AUTO_SYNC_ENABLED, enabled)
            .apply()
        _cloudSyncSettings.value = loadSettings()
    }
    
    suspend fun setSyncIntervalMinutes(minutes: Int) {
        encryptedPreferences.edit()
            .putInt(KEY_SYNC_INTERVAL_MINUTES, minutes)
            .apply()
        _cloudSyncSettings.value = loadSettings()
    }
    
    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        encryptedPreferences.edit()
            .putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp)
            .apply()
        _cloudSyncSettings.value = loadSettings()
    }
    
    suspend fun setSyncOnlyOnWifi(enabled: Boolean) {
        encryptedPreferences.edit()
            .putBoolean(KEY_SYNC_ONLY_ON_WIFI, enabled)
            .apply()
        _cloudSyncSettings.value = loadSettings()
    }
    
}
