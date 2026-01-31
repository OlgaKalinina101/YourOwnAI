package com.yourown.ai.presentation.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourown.ai.presentation.settings.SettingsUiState
import com.yourown.ai.presentation.settings.SettingsViewModel
import com.yourown.ai.presentation.settings.components.*

/**
 * Context Section - User context and settings
 */
@Composable
fun ContextSection(
    hasContext: Boolean,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onEditContext: () -> Unit
) {
    SettingsSection(
        title = "Context",
        icon = Icons.Default.Person,
        subtitle = "What you want your AI to know about you"
    ) {
        // Context
        SettingItemClickable(
            title = "Your Context",
            subtitle = "Information about you or anything else",
            onClick = onEditContext,
            trailing = {
                Row {
                    if (hasContext) {
                        Icon(
                            Icons.Default.CheckCircle,
                            "Set",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = { /* TODO: Show help */ }) {
                        Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
                    }
                }
            }
        )
        
        // Advanced Context Settings
        SettingItemClickable(
            title = if (uiState.showAdvancedContextSettings) "▼ Advanced Context Settings" else "▶ Advanced Context Settings",
            subtitle = "Customize enhanced context instructions",
            onClick = { viewModel.toggleAdvancedContextSettings() }
        )
        
        if (uiState.showAdvancedContextSettings) {
            SettingItemClickable(
                title = "Context Instructions",
                subtitle = "How AI understands enhanced context",
                onClick = { viewModel.showContextInstructionsDialog() },
                trailing = {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            )
            
            SettingItemClickable(
                title = "Swipe Message Prompt",
                subtitle = "How AI sees replied messages",
                onClick = { viewModel.showSwipeMessagePromptDialog() },
                trailing = {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            )
        }
    }
}
