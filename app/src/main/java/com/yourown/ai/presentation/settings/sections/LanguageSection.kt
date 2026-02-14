package com.yourown.ai.presentation.settings.sections

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.yourown.ai.R
import com.yourown.ai.presentation.settings.components.DropdownSettingString
import com.yourown.ai.presentation.settings.components.SettingsSection

/**
 * Language Section - choose app language and prompt language
 * Note: setApplicationLocales() automatically recreates the activity
 */
@Composable
fun LanguageSection(
    promptLanguage: String = "ru",
    onPromptLanguageChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    
    // Get current locale from AppCompat or system default
    val appLocales = AppCompatDelegate.getApplicationLocales()
    val currentLocale = if (!appLocales.isEmpty) {
        appLocales.get(0)?.language ?: "en"
    } else {
        // If no app locale set, check system locale
        val systemLocale = context.resources.configuration.locales.get(0).language
        Log.d("LanguageSection", "No app locale set, system locale: $systemLocale")
        "en" // Default to English if nothing is set
    }
    
    Log.d("LanguageSection", "Current locale: $currentLocale, isEmpty: ${appLocales.isEmpty}")
    
    var selectedLanguage by remember { mutableStateOf(currentLocale) }
    
    // Update selectedLanguage when currentLocale changes
    LaunchedEffect(currentLocale) {
        selectedLanguage = currentLocale
    }
    
    // Available languages
    val languages = listOf(
        "en" to stringResource(R.string.language_english),
        "ru" to stringResource(R.string.language_russian),
        "uk" to stringResource(R.string.language_ukrainian)
    )
    
    SettingsSection(
        title = stringResource(R.string.language_section_title),
        icon = Icons.Default.Language,
        subtitle = stringResource(R.string.language_section_subtitle)
    ) {
        // App Language Dropdown
        DropdownSettingString(
            title = stringResource(R.string.language_app_language_title),
            subtitle = stringResource(R.string.language_app_language_subtitle),
            value = selectedLanguage,
            options = languages,
            onValueChange = { code ->
                Log.d("LanguageSection", "App language changed to: $code")
                selectedLanguage = code
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(code)
                )
            }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Prompt Language Dropdown
        DropdownSettingString(
            title = stringResource(R.string.language_prompt_language_title),
            subtitle = stringResource(R.string.language_prompt_language_subtitle),
            value = promptLanguage,
            options = languages,
            onValueChange = onPromptLanguageChange
        )
        
        // Info text
        Text(
            text = stringResource(R.string.language_custom_prompts_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
