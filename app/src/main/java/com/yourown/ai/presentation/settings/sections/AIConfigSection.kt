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
        title = "AI Configuration",
        icon = Icons.Default.SmartToy,
        subtitle = "Customize your AI's behavior"
    ) {
        // System Prompts (API models)
        SettingItemClickable(
            title = "System Prompt (API)",
            subtitle = "For cloud models (Deepseek, OpenAI, Grok) • Total: ${apiPrompts.size}",
            onClick = { viewModel.showSystemPromptsListDialog(com.yourown.ai.data.repository.PromptType.API) },
            trailing = {
                Row {
                    IconButton(onClick = { viewModel.createNewPrompt(com.yourown.ai.data.repository.PromptType.API) }) {
                        Icon(Icons.Default.Add, "Add API prompt", modifier = Modifier.size(20.dp))
                    }
                }
            }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // System Prompts (Local models)
        SettingItemClickable(
            title = "System Prompt (Local)",
            subtitle = "For local models (Qwen, Llama) • Total: ${localPrompts.size}",
            onClick = { viewModel.showSystemPromptsListDialog(com.yourown.ai.data.repository.PromptType.LOCAL) },
            trailing = {
                Row {
                    IconButton(onClick = { viewModel.createNewPrompt(com.yourown.ai.data.repository.PromptType.LOCAL) }) {
                        Icon(Icons.Default.Add, "Add Local prompt", modifier = Modifier.size(20.dp))
                    }
                }
            }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Temperature
        SliderSetting(
            title = "Temperature",
            subtitle = "How creative and free your AI can sound",
            value = config.temperature,
            valueRange = AIConfig.MIN_TEMPERATURE..AIConfig.MAX_TEMPERATURE,
            onValueChange = onTemperatureChange
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Top-p
        SliderSetting(
            title = "Top-p",
            subtitle = "How chaotic your AI can be",
            value = config.topP,
            valueRange = AIConfig.MIN_TOP_P..AIConfig.MAX_TOP_P,
            onValueChange = onTopPChange
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Message History Limit (API models only)
        DropdownSetting(
            title = "Message History (API only)",
            subtitle = "How many messages to send to API models • Local models always use last message",
            value = config.messageHistoryLimit,
            options = (AIConfig.MIN_MESSAGE_HISTORY..AIConfig.MAX_MESSAGE_HISTORY).toList(),
            onValueChange = onMessageHistoryChange,
            valueSuffix = "pairs"
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Max Tokens
        DropdownSetting(
            title = "Max Tokens",
            subtitle = "Maximum length of AI response",
            value = config.maxTokens,
            options = listOf(256, 512, 1024, 2048, 4096, 8192),
            onValueChange = onMaxTokensChange,
            valueSuffix = "tokens"
        )
    }
}
