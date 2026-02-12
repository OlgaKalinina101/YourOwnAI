package com.yourown.ai.presentation.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yourown.ai.R
import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.presentation.settings.SettingsUiState
import com.yourown.ai.presentation.settings.SettingsViewModel
import com.yourown.ai.presentation.settings.components.*

/**
 * Memory Section - Long-term memory with saved memories
 */
@Composable
fun MemorySection(
    config: AIConfig,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onToggleMemory: () -> Unit,
    onEditMemoryPrompt: () -> Unit,
    onMemoryLimitChange: (Int) -> Unit,
    onViewMemories: () -> Unit
) {
    SettingsSection(
        title = stringResource(R.string.memory_section_title),
        icon = Icons.Default.Memory,
        subtitle = stringResource(R.string.memory_section_subtitle)
    ) {
        // Memory Toggle
        ToggleSetting(
            title = stringResource(R.string.memory_enabled_title),
            subtitle = stringResource(R.string.memory_enabled_subtitle),
            checked = config.memoryEnabled,
            onCheckedChange = { onToggleMemory() }
        )
        
        // Memory Extraction Prompt
        if (config.memoryEnabled) {
            SettingItemClickable(
                title = stringResource(R.string.memory_extraction_prompt_title),
                subtitle = stringResource(R.string.memory_extraction_prompt_subtitle),
                onClick = onEditMemoryPrompt,
                trailing = {
                    Icon(Icons.Default.Edit, stringResource(R.string.preanalysis_edit), tint = MaterialTheme.colorScheme.primary)
                }
            )
            
            // Memory Limit Slider
            SliderSetting(
                title = stringResource(R.string.memory_limit_title),
                subtitle = stringResource(R.string.memory_limit_subtitle),
                value = config.memoryLimit.toFloat(),
                valueRange = AIConfig.MIN_MEMORY_LIMIT.toFloat()..AIConfig.MAX_MEMORY_LIMIT.toFloat(),
                onValueChange = { onMemoryLimitChange(it.toInt()) },
                valueFormatter = { "${it.toInt()} memories" }
            )
            
            // Advanced Memory Settings
            SettingItemClickable(
                title = if (uiState.showAdvancedMemorySettings) stringResource(R.string.preanalysis_advanced_settings_expanded).replace("Deep Empathy", "Memory") else stringResource(R.string.preanalysis_advanced_settings_collapsed).replace("Deep Empathy", "Memory"),
                subtitle = stringResource(R.string.memory_advanced_subtitle),
                onClick = { viewModel.toggleAdvancedMemorySettings() }
            )
            
            if (uiState.showAdvancedMemorySettings) {
                OutlinedTextField(
                    value = config.memoryTitle,
                    onValueChange = { viewModel.updateMemoryTitle(it) },
                    label = { Text(stringResource(R.string.memory_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SliderSetting(
                    title = "Memory Age Filter",
                    subtitle = "Only retrieve memories older than X days",
                    value = config.memoryMinAgeDays.toFloat(),
                    valueRange = AIConfig.MIN_MEMORY_MIN_AGE_DAYS.toFloat()..AIConfig.MAX_MEMORY_MIN_AGE_DAYS.toFloat(),
                    onValueChange = { viewModel.updateMemoryMinAgeDays(it.toInt()) },
                    valueFormatter = { "${it.toInt()} days" }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingItemClickable(
                    title = "Memory Instructions",
                    subtitle = "What AI do with memories",
                    onClick = { viewModel.showMemoryInstructionsDialog() },
                    trailing = {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            // Saved Memories
            SettingItemClickable(
                title = "Saved Memories",
                subtitle = "View saved memories",
                onClick = onViewMemories,
                trailing = {
                    Icon(Icons.Default.ChevronRight, "View")
                }
            )
        }
    }
}
