package com.yourown.ai.presentation.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourown.ai.presentation.settings.SettingsUiState
import com.yourown.ai.presentation.settings.SettingsViewModel
import com.yourown.ai.presentation.settings.components.*

/**
 * Knowledge & Memory Section - Context, Documents, RAG, Memories
 */
@Composable
fun KnowledgeMemorySection(
    hasContext: Boolean,
    documentsCount: Int,
    documentProcessingStatus: com.yourown.ai.data.repository.DocumentProcessingStatus,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onEditContext: () -> Unit,
    onManageDocuments: () -> Unit,
    onAddDocument: () -> Unit,
    onViewMemories: () -> Unit
) {
    SettingsSection(
        title = "Knowledge & Memory",
        icon = Icons.Default.Psychology,
        subtitle = "Teach your AI about you and your world"
    ) {
        // Context
        SettingItemClickable(
            title = "Context",
            subtitle = "What you want your AI to know about you or anything else",
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
                subtitle = "How AI uses enhanced context",
                onClick = { viewModel.showContextInstructionsDialog() },
                trailing = {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            )
            
            SettingItemClickable(
                title = "Swipe Message Prompt",
                subtitle = "Prompt for replied messages",
                onClick = { viewModel.showSwipeMessagePromptDialog() },
                trailing = {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Knowledge Documents
        SettingItemClickable(
            title = "Knowledge data",
            subtitle = "Text documents for AI teaching",
            onClick = onManageDocuments,
            trailing = {
                Row {
                    IconButton(onClick = onManageDocuments) {
                        Icon(Icons.Default.Description, "View Documents", modifier = Modifier.size(20.dp))
                    }
                }
            }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // RAG Documents
        SettingItemClickable(
            title = "RAG Documents",
            subtitle = "What you want to teach your AI - documents, notes, conversations",
            onClick = onAddDocument,
            trailing = {
                Row {
                    IconButton(onClick = onAddDocument) {
                        Icon(Icons.Default.Add, "Add", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { /* TODO: Show help */ }) {
                        Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
                    }
                }
            }
        )
        
        // Document processing indicator
        when (val status = documentProcessingStatus) {
            is com.yourown.ai.data.repository.DocumentProcessingStatus.Processing -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Processing: ${status.documentName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = status.currentStep,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { status.progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${status.progress}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            is com.yourown.ai.data.repository.DocumentProcessingStatus.Deleting -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Deleting: ${status.documentName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            is com.yourown.ai.data.repository.DocumentProcessingStatus.Completed -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "✓ Processing completed!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            is com.yourown.ai.data.repository.DocumentProcessingStatus.Failed -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Processing failed",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = status.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            else -> { /* No processing indicator needed */ }
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
