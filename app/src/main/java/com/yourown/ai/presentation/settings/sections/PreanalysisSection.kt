package com.yourown.ai.presentation.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.presentation.settings.SettingsUiState
import com.yourown.ai.presentation.settings.SettingsViewModel
import com.yourown.ai.presentation.settings.components.*

/**
 * Preanalysis Section - Deep Empathy and dialogue focus tracking
 */
@Composable
fun PreanalysisSection(
    config: AIConfig,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onToggleDeepEmpathy: () -> Unit
) {
    SettingsSection(
        title = "Preanalysis",
        icon = Icons.Default.Analytics,
        subtitle = "What AI do before responds"
    ) {
        // Deep Empathy Toggle
        ToggleSetting(
            title = "Deep Empathy",
            subtitle = "How deeply your AI can hear you",
            checked = config.deepEmpathy,
            onCheckedChange = { onToggleDeepEmpathy() }
        )
        
        // Deep Empathy Settings (shown when Deep Empathy is enabled)
        if (config.deepEmpathy) {
            SettingItemClickable(
                title = "Deep Empathy Prompt",
                subtitle = "Customize focus tracking prompt • Required: {dialogue_focus}",
                onClick = { viewModel.showDeepEmpathyPromptDialog() },
                trailing = {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            )
            
            // Advanced Deep Empathy Settings
            SettingItemClickable(
                title = if (uiState.showAdvancedDeepEmpathySettings) "▼ Advanced Deep Empathy Settings" else "▶ Advanced Deep Empathy Settings",
                subtitle = "Customize dialogue focus analysis",
                onClick = { viewModel.toggleAdvancedDeepEmpathySettings() }
            )
            
            if (uiState.showAdvancedDeepEmpathySettings) {
                SettingItemClickable(
                    title = "Analysis Prompt",
                    subtitle = "How AI finds focus points • Required: {text}",
                    onClick = { viewModel.showDeepEmpathyAnalysisDialog() },
                    trailing = {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }
        }
    }
}
