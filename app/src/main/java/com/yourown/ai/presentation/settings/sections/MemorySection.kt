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
    onViewMemories: () -> Unit,
    onManageMemory: () -> Unit = {}
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
            onCheckedChange = { onToggleMemory() },
            hintResId = R.string.hint_memory_enabled
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
            val memoryLimitFormatter = stringResource(R.string.memory_limit_formatter)
            SliderSetting(
                title = stringResource(R.string.memory_limit_title),
                subtitle = stringResource(R.string.memory_limit_subtitle),
                value = config.memoryLimit.toFloat(),
                valueRange = AIConfig.MIN_MEMORY_LIMIT.toFloat()..AIConfig.MAX_MEMORY_LIMIT.toFloat(),
                onValueChange = { onMemoryLimitChange(it.toInt()) },
                valueFormatter = { memoryLimitFormatter.format(it.toInt()) },
                hintResId = R.string.hint_memory_limit
            )
            
            // Advanced Memory Settings
            SettingItemClickable(
                title = if (uiState.showAdvancedMemorySettings) 
                    stringResource(R.string.memory_advanced_expanded) 
                else 
                    stringResource(R.string.memory_advanced_collapsed),
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
                
                val memoryAgeFormatter = stringResource(R.string.memory_age_filter_formatter)
                SliderSetting(
                    title = stringResource(R.string.memory_age_filter_title),
                    subtitle = stringResource(R.string.memory_age_filter_subtitle),
                    value = config.memoryMinAgeDays.toFloat(),
                    valueRange = AIConfig.MIN_MEMORY_MIN_AGE_DAYS.toFloat()..AIConfig.MAX_MEMORY_MIN_AGE_DAYS.toFloat(),
                    onValueChange = { viewModel.updateMemoryMinAgeDays(it.toInt()) },
                    valueFormatter = { memoryAgeFormatter.format(it.toInt()) },
                    hintResId = R.string.hint_memory_age_filter
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingItemClickable(
                    title = stringResource(R.string.memory_instructions_title),
                    subtitle = stringResource(R.string.memory_instructions_subtitle),
                    onClick = { viewModel.showMemoryInstructionsDialog() },
                    trailing = {
                        Icon(Icons.Default.Edit, stringResource(R.string.memory_edit_icon), tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            // Memory Management
            SettingItemClickable(
                title = stringResource(R.string.memory_management_title),
                subtitle = stringResource(R.string.memory_management_subtitle),
                onClick = onManageMemory,
                trailing = {
                    Icon(Icons.Default.Settings, stringResource(R.string.memory_management_title))
                }
            )
            
            // Saved Memories
            SettingItemClickable(
                title = stringResource(R.string.memory_saved_title),
                subtitle = stringResource(R.string.memory_saved_subtitle),
                onClick = onViewMemories,
                trailing = {
                    Icon(Icons.Default.ChevronRight, stringResource(R.string.memory_view_icon))
                }
            )
        }
    }
}
