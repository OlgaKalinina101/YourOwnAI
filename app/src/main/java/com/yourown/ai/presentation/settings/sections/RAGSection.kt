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
import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.presentation.settings.SettingsUiState
import com.yourown.ai.presentation.settings.SettingsViewModel
import com.yourown.ai.presentation.settings.components.*

/**
 * RAG Section - Retrieval Augmented Generation with knowledge documents
 */
@Composable
fun RAGSection(
    config: AIConfig,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    documentsCount: Int,
    documentProcessingStatus: com.yourown.ai.data.repository.DocumentProcessingStatus,
    onToggleRAG: () -> Unit,
    onChunkSizeChange: (Float) -> Unit,
    onChunkOverlapChange: (Float) -> Unit,
    onRAGChunkLimitChange: (Int) -> Unit,
    onManageDocuments: () -> Unit,
    onAddDocument: () -> Unit
) {
    SettingsSection(
        title = "RAG (Knowledge & Data)",
        icon = Icons.Default.Storage,
        subtitle = "Use knowledge documents in responses"
    ) {
        // RAG Toggle
        ToggleSetting(
            title = "RAG Enabled",
            subtitle = "Use knowledge documents in responses",
            checked = config.ragEnabled,
            onCheckedChange = { onToggleRAG() }
        )
        
        // RAG Settings (show only when RAG is enabled)
        if (config.ragEnabled) {
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
            
            // RAG Chunk Limit Slider
            SliderSetting(
                title = "RAG Chunk Limit",
                subtitle = "AI knowledge memory limit",
                value = config.ragChunkLimit.toFloat(),
                valueRange = AIConfig.MIN_RAG_CHUNK_LIMIT.toFloat()..AIConfig.MAX_RAG_CHUNK_LIMIT.toFloat(),
                onValueChange = { onRAGChunkLimitChange(it.toInt()) },
                valueFormatter = { "${it.toInt()} chunks" }
            )
            
            // Advanced RAG Settings
            SettingItemClickable(
                title = if (uiState.showAdvancedRAGSettings) "▼ Advanced RAG Settings" else "▶ Advanced RAG Settings",
                subtitle = "Customize RAG behavior",
                onClick = { viewModel.toggleAdvancedRAGSettings() }
            )
            
            if (uiState.showAdvancedRAGSettings) {
                OutlinedTextField(
                    value = config.ragTitle,
                    onValueChange = { viewModel.updateRAGTitle(it) },
                    label = { Text("RAG Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Chunk Size
                SliderSetting(
                    title = "Chunk Size",
                    subtitle = "Text size for each document chunk",
                    value = config.ragChunkSize.toFloat(),
                    valueRange = AIConfig.MIN_CHUNK_SIZE.toFloat()..AIConfig.MAX_CHUNK_SIZE.toFloat(),
                    onValueChange = onChunkSizeChange,
                    valueFormatter = { "${it.toInt()} characters" }
                )
                
                // Chunk Overlap
                SliderSetting(
                    title = "Chunk Overlap",
                    subtitle = "Overlapping characters between chunks",
                    value = config.ragChunkOverlap.toFloat(),
                    valueRange = AIConfig.MIN_CHUNK_OVERLAP.toFloat()..AIConfig.MAX_CHUNK_OVERLAP.toFloat(),
                    onValueChange = onChunkOverlapChange,
                    valueFormatter = { "${it.toInt()} characters" }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingItemClickable(
                    title = "RAG Instructions",
                    subtitle = "What AI do with knowledge documents",
                    onClick = { viewModel.showRAGInstructionsDialog() },
                    trailing = {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }
        }
    }
}
