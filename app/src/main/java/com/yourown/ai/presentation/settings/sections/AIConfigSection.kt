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
 * AI Configuration Section - System Prompts, Temperature, Top-p, Message History, Max Tokens
 */
@Composable
fun AIConfigurationSection(
    config: AIConfig,
    uiState: SettingsUiState,
    apiPrompts: List<com.yourown.ai.domain.model.SystemPrompt>,
    localPrompts: List<com.yourown.ai.domain.model.SystemPrompt>,
    viewModel: SettingsViewModel,
    onEditSystemPrompt: () -> Unit,
    onEditLocalSystemPrompt: () -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onTopPChange: (Float) -> Unit,
    onMessageHistoryChange: (Int) -> Unit,
    onMaxTokensChange: (Int) -> Unit
) {
    SettingsSection(
        title = stringResource(R.string.settings_ai_configuration),
        icon = Icons.Default.SmartToy,
        subtitle = stringResource(R.string.settings_ai_configuration_subtitle)
    ) {
        // System Prompts (API models)
        SettingItemClickable(
            title = stringResource(R.string.settings_system_prompt_api),
            subtitle = stringResource(R.string.settings_system_prompt_api_subtitle, apiPrompts.size),
            onClick = { viewModel.showSystemPromptsListDialog(com.yourown.ai.data.repository.PromptType.API) }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // System Prompts (Local models)
        SettingItemClickable(
            title = stringResource(R.string.settings_system_prompt_local),
            subtitle = stringResource(R.string.settings_system_prompt_local_subtitle, localPrompts.size),
            onClick = { viewModel.showSystemPromptsListDialog(com.yourown.ai.data.repository.PromptType.LOCAL) }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Temperature
        SliderSetting(
            title = stringResource(R.string.settings_temperature),
            subtitle = stringResource(R.string.settings_temperature_subtitle),
            value = config.temperature,
            valueRange = AIConfig.MIN_TEMPERATURE..AIConfig.MAX_TEMPERATURE,
            onValueChange = onTemperatureChange
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Top-p
        SliderSetting(
            title = stringResource(R.string.settings_top_p),
            subtitle = stringResource(R.string.settings_top_p_subtitle),
            value = config.topP,
            valueRange = AIConfig.MIN_TOP_P..AIConfig.MAX_TOP_P,
            onValueChange = onTopPChange
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Message History Limit (API models only)
        DropdownSetting(
            title = stringResource(R.string.settings_message_history),
            subtitle = stringResource(R.string.settings_message_history_subtitle),
            value = config.messageHistoryLimit,
            options = (AIConfig.MIN_MESSAGE_HISTORY..AIConfig.MAX_MESSAGE_HISTORY).toList(),
            onValueChange = onMessageHistoryChange,
            valueSuffix = stringResource(R.string.settings_pairs)
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Max Tokens
        DropdownSetting(
            title = stringResource(R.string.settings_max_tokens),
            subtitle = stringResource(R.string.settings_max_tokens_subtitle),
            value = config.maxTokens,
            options = listOf(256, 512, 1024, 2048, 4096, 8192),
            onValueChange = onMaxTokensChange,
            valueSuffix = stringResource(R.string.settings_tokens)
        )
    }
}
