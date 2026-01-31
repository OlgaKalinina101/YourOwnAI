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
        title = "Memory",
        icon = Icons.Default.Memory,
        subtitle = "AI remembers important things automatically"
    ) {
        // Memory Toggle
        ToggleSetting(
            title = "Memory Enabled",
            subtitle = "AI remembers important things automatically",
            checked = config.memoryEnabled,
            onCheckedChange = { onToggleMemory() }
        )
        
        // Memory Extraction Prompt
        if (config.memoryEnabled) {
            SettingItemClickable(
                title = "Memory Extraction Prompt",
                subtitle = "Customize how AI extracts memories • Required: {text}",
                onClick = onEditMemoryPrompt,
                trailing = {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            )
            
            // Memory Limit Slider
            SliderSetting(
                title = "Memory Limit",
                subtitle = "AI memory for memories limit",
                value = config.memoryLimit.toFloat(),
                valueRange = AIConfig.MIN_MEMORY_LIMIT.toFloat()..AIConfig.MAX_MEMORY_LIMIT.toFloat(),
                onValueChange = { onMemoryLimitChange(it.toInt()) },
                valueFormatter = { "${it.toInt()} memories" }
            )
            
            // Advanced Memory Settings
            SettingItemClickable(
                title = if (uiState.showAdvancedMemorySettings) "▼ Advanced Memory Settings" else "▶ Advanced Memory Settings",
                subtitle = "Customize memory behavior",
                onClick = { viewModel.toggleAdvancedMemorySettings() }
            )
            
            if (uiState.showAdvancedMemorySettings) {
                OutlinedTextField(
                    value = config.memoryTitle,
                    onValueChange = { viewModel.updateMemoryTitle(it) },
                    label = { Text("Memory Title") },
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
