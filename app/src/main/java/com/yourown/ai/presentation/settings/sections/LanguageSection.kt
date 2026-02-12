package com.yourown.ai.presentation.settings.sections

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.yourown.ai.R
import com.yourown.ai.presentation.settings.components.SettingsSection
import java.util.Locale

/**
 * Language Section - choose app language
 * Note: setApplicationLocales() automatically recreates the activity
 */
@Composable
fun LanguageSection() {
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
    var showRestartSnackbar by remember { mutableStateOf(false) }
    
    SettingsSection(
        title = stringResource(R.string.language_section_title),
        icon = Icons.Default.Language,
        subtitle = stringResource(R.string.language_section_subtitle)
    ) {
        // English
        LanguageItem(
            languageName = stringResource(R.string.language_english),
            languageCode = "en",
            isSelected = selectedLanguage == "en",
            onClick = {
                Log.d("LanguageSection", "English clicked, current: $selectedLanguage")
                selectedLanguage = "en"
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags("en")
                )
                Log.d("LanguageSection", "Locale set to English")
            }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Russian
        LanguageItem(
            languageName = stringResource(R.string.language_russian),
            languageCode = "ru",
            isSelected = selectedLanguage == "ru",
            onClick = {
                Log.d("LanguageSection", "Russian clicked, current: $selectedLanguage")
                selectedLanguage = "ru"
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags("ru")
                )
                Log.d("LanguageSection", "Locale set to Russian")
            }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Ukrainian
        LanguageItem(
            languageName = stringResource(R.string.language_ukrainian),
            languageCode = "uk",
            isSelected = selectedLanguage == "uk",
            onClick = {
                Log.d("LanguageSection", "Ukrainian clicked, current: $selectedLanguage")
                selectedLanguage = "uk"
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags("uk")
                )
                Log.d("LanguageSection", "Locale set to Ukrainian")
            }
        )
    }
}

@Composable
private fun LanguageItem(
    languageName: String,
    languageCode: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = languageName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            RadioButton(
                selected = false,
                onClick = onClick
            )
        }
    }
}
