package com.yourown.ai.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yourown.ai.presentation.theme.ColorStyle
import com.yourown.ai.presentation.theme.FontScale
import com.yourown.ai.presentation.theme.FontStyle
import com.yourown.ai.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Settings Manager для хранения настроек приложения
 */
@Singleton
class SettingsManager @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val COLOR_STYLE = stringPreferencesKey("color_style")
        private val FONT_STYLE = stringPreferencesKey("font_style")
        private val FONT_SCALE = floatPreferencesKey("font_scale")
        
        private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        private val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        
        // Selected model
        private val SELECTED_MODEL_TYPE = stringPreferencesKey("selected_model_type") // "local" or "api"
        private val SELECTED_MODEL_ID = stringPreferencesKey("selected_model_id")
        private val SELECTED_MODEL_PROVIDER = stringPreferencesKey("selected_model_provider") // for API models
        
        // Pinned models
        private val PINNED_MODELS = stringSetPreferencesKey("pinned_models")
        
        // Keyboard sound settings
        private val KEYBOARD_SOUND_VOLUME = floatPreferencesKey("keyboard_sound_volume")
        
        // Prompt language
        private val PROMPT_LANGUAGE = stringPreferencesKey("prompt_language")
    }
    
    // Theme Mode
    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        when (preferences[THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }
    
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }
    
    // Color Style
    val colorStyle: Flow<ColorStyle> = dataStore.data.map { preferences ->
        when (preferences[COLOR_STYLE]) {
            "NEUTRAL" -> ColorStyle.NEUTRAL
            "CUSTOM" -> ColorStyle.CUSTOM
            else -> ColorStyle.DYNAMIC
        }
    }
    
    suspend fun setColorStyle(style: ColorStyle) {
        dataStore.edit { preferences ->
            preferences[COLOR_STYLE] = style.name
        }
    }
    
    // Font Style
    val fontStyle: Flow<FontStyle> = dataStore.data.map { preferences ->
        when (preferences[FONT_STYLE]) {
            "SYSTEM" -> FontStyle.SYSTEM
            else -> FontStyle.ROBOTO
        }
    }
    
    suspend fun setFontStyle(style: FontStyle) {
        dataStore.edit { preferences ->
            preferences[FONT_STYLE] = style.name
        }
    }
    
    // Font Scale
    val fontScale: Flow<FontScale> = dataStore.data.map { preferences ->
        val scale = preferences[FONT_SCALE] ?: 1.0f
        when (scale) {
            0.85f -> FontScale.SMALL
            1.15f -> FontScale.MEDIUM
            1.3f -> FontScale.LARGE
            1.5f -> FontScale.EXTRA_LARGE
            else -> FontScale.DEFAULT
        }
    }
    
    suspend fun setFontScale(scale: FontScale) {
        dataStore.edit { preferences ->
            preferences[FONT_SCALE] = scale.scale
        }
    }
    
    // Onboarding
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_FIRST_LAUNCH] ?: true
    }
    
    suspend fun setFirstLaunchCompleted() {
        dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH] = false
        }
    }
    
    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAS_COMPLETED_ONBOARDING] ?: false
    }
    
    suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] = true
        }
    }
    
    // Selected Model
    data class SavedModel(
        val type: String, // "local" or "api"
        val modelId: String,
        val provider: String? = null // for API models
    )
    
    val selectedModel: Flow<SavedModel?> = dataStore.data.map { preferences ->
        val type = preferences[SELECTED_MODEL_TYPE] ?: return@map null
        val modelId = preferences[SELECTED_MODEL_ID] ?: return@map null
        val provider = preferences[SELECTED_MODEL_PROVIDER]
        SavedModel(type, modelId, provider)
    }
    
    suspend fun setSelectedModel(type: String, modelId: String, provider: String? = null) {
        dataStore.edit { preferences ->
            preferences[SELECTED_MODEL_TYPE] = type
            preferences[SELECTED_MODEL_ID] = modelId
            if (provider != null) {
                preferences[SELECTED_MODEL_PROVIDER] = provider
            } else {
                preferences.remove(SELECTED_MODEL_PROVIDER)
            }
        }
    }
    
    // Pinned Models
    val pinnedModels: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[PINNED_MODELS] ?: emptySet()
    }
    
    suspend fun togglePinnedModel(modelKey: String) {
        dataStore.edit { preferences ->
            val current = preferences[PINNED_MODELS] ?: emptySet()
            preferences[PINNED_MODELS] = if (modelKey in current) {
                current - modelKey
            } else {
                current + modelKey
            }
        }
    }
    
    suspend fun isPinnedModel(modelKey: String): Boolean {
        val current = dataStore.data.map { it[PINNED_MODELS] ?: emptySet() }
        return current.map { modelKey in it }.first()
    }
    
    // Keyboard Sound Settings
    val keyboardSoundVolume: Flow<Float> = dataStore.data.map { preferences ->
        preferences[KEYBOARD_SOUND_VOLUME] ?: 0f // Default: 0% (off)
    }
    
    suspend fun setKeyboardSoundVolume(volume: Float) {
        dataStore.edit { preferences ->
            preferences[KEYBOARD_SOUND_VOLUME] = volume.coerceIn(0f, 1f)
        }
    }
    
    // Prompt Language
    val promptLanguage: Flow<String> = dataStore.data.map { preferences ->
        preferences[PROMPT_LANGUAGE] ?: "ru" // Default: Russian
    }
    
    suspend fun setPromptLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PROMPT_LANGUAGE] = language
        }
    }
}
