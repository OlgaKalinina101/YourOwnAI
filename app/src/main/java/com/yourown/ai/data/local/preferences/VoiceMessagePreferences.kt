package com.yourown.ai.data.local.preferences

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yourown.ai.presentation.voice.VoiceMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferences for storing Voice Chat message history
 */
@Singleton
class VoiceMessagePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("voice_messages", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_MESSAGES = "messages"
        private const val KEY_SELECTED_VOICE = "selected_voice"
        private const val MAX_MESSAGES = 100 // Limit history to 100 messages
    }
    
    /**
     * Save voice messages to preferences
     */
    fun saveMessages(messages: List<VoiceMessage>) {
        // Keep only last MAX_MESSAGES
        val messagesToSave = if (messages.size > MAX_MESSAGES) {
            messages.takeLast(MAX_MESSAGES)
        } else {
            messages
        }
        
        val json = gson.toJson(messagesToSave)
        prefs.edit().putString(KEY_MESSAGES, json).apply()
    }
    
    /**
     * Load voice messages from preferences
     */
    fun loadMessages(): List<VoiceMessage> {
        val json = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<VoiceMessage>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clear all voice messages
     */
    fun clearMessages() {
        prefs.edit().remove(KEY_MESSAGES).apply()
    }
    
    /**
     * Save selected voice ID
     */
    fun saveSelectedVoice(voiceId: String) {
        prefs.edit().putString(KEY_SELECTED_VOICE, voiceId).apply()
    }
    
    /**
     * Load selected voice ID (returns null if not set)
     */
    fun loadSelectedVoice(): String? {
        return prefs.getString(KEY_SELECTED_VOICE, null)
    }
}
