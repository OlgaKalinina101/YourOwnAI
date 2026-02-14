package com.yourown.ai.presentation.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R
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
        title = stringResource(R.string.preanalysis_title),
        icon = Icons.Default.Analytics,
        subtitle = stringResource(R.string.preanalysis_subtitle)
    ) {
        // Deep Empathy Toggle
        ToggleSetting(
            title = stringResource(R.string.preanalysis_deep_empathy),
            subtitle = stringResource(R.string.preanalysis_deep_empathy_subtitle),
            checked = config.deepEmpathy,
            onCheckedChange = { onToggleDeepEmpathy() },
            hintResId = R.string.hint_deep_empathy
        )
        
        // Deep Empathy Settings (shown when Deep Empathy is enabled)
        if (config.deepEmpathy) {
            SettingItemClickable(
                title = stringResource(R.string.preanalysis_deep_empathy_prompt),
                subtitle = stringResource(R.string.preanalysis_deep_empathy_prompt_subtitle),
                onClick = { viewModel.showDeepEmpathyPromptDialog() },
                trailing = {
                    Icon(Icons.Default.Edit, stringResource(R.string.preanalysis_edit), tint = MaterialTheme.colorScheme.primary)
                }
            )
            
            // Advanced Deep Empathy Settings
            SettingItemClickable(
                title = if (uiState.showAdvancedDeepEmpathySettings) stringResource(R.string.preanalysis_advanced_settings_expanded) else stringResource(R.string.preanalysis_advanced_settings_collapsed),
                subtitle = stringResource(R.string.preanalysis_advanced_settings_subtitle),
                onClick = { viewModel.toggleAdvancedDeepEmpathySettings() }
            )
            
            if (uiState.showAdvancedDeepEmpathySettings) {
                SettingItemClickable(
                    title = stringResource(R.string.preanalysis_analysis_prompt),
                    subtitle = stringResource(R.string.preanalysis_analysis_prompt_subtitle),
                    onClick = { viewModel.showDeepEmpathyAnalysisDialog() },
                    trailing = {
                        Icon(Icons.Default.Edit, stringResource(R.string.preanalysis_edit), tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }
        }
    }
}
