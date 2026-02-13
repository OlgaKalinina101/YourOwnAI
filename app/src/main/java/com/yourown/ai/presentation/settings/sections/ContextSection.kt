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
        title = stringResource(R.string.context_section_title),
        icon = Icons.Default.Person,
        subtitle = stringResource(R.string.context_section_subtitle)
    ) {
        // Context
        SettingItemClickable(
            title = stringResource(R.string.context_your_context_title),
            subtitle = stringResource(R.string.context_your_context_subtitle),
            onClick = onEditContext,
            trailing = {
                Row {
                    if (hasContext) {
                        Icon(
                            Icons.Default.CheckCircle,
                            stringResource(R.string.context_set_icon),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = { /* TODO: Show help */ }) {
                        Icon(Icons.Default.HelpOutline, stringResource(R.string.context_help_icon), modifier = Modifier.size(20.dp))
                    }
                }
            }
        )
        
        // Advanced Context Settings
        SettingItemClickable(
            title = if (uiState.showAdvancedContextSettings) 
                stringResource(R.string.context_advanced_expanded) 
            else 
                stringResource(R.string.context_advanced_collapsed),
            subtitle = stringResource(R.string.context_advanced_subtitle),
            onClick = { viewModel.toggleAdvancedContextSettings() }
        )
        
        if (uiState.showAdvancedContextSettings) {
            SettingItemClickable(
                title = stringResource(R.string.context_instructions_title),
                subtitle = stringResource(R.string.context_instructions_subtitle),
                onClick = { viewModel.showContextInstructionsDialog() },
                trailing = {
                    Icon(Icons.Default.Edit, stringResource(R.string.context_edit_icon), tint = MaterialTheme.colorScheme.primary)
                }
            )
            
            SettingItemClickable(
                title = stringResource(R.string.context_swipe_message_title),
                subtitle = stringResource(R.string.context_swipe_message_subtitle),
                onClick = { viewModel.showSwipeMessagePromptDialog() },
                trailing = {
                    Icon(Icons.Default.Edit, stringResource(R.string.context_edit_icon), tint = MaterialTheme.colorScheme.primary)
                }
            )
        }
    }
}
