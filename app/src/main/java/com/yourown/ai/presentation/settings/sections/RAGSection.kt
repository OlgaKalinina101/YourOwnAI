package com.yourown.ai.presentation.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourown.ai.R
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
        title = stringResource(R.string.rag_section_title),
        icon = Icons.Default.Storage,
        subtitle = stringResource(R.string.rag_section_subtitle)
    ) {
        // RAG Toggle
        ToggleSetting(
            title = stringResource(R.string.rag_enabled_title),
            subtitle = stringResource(R.string.rag_enabled_subtitle),
            checked = config.ragEnabled,
            onCheckedChange = { onToggleRAG() }
        )
        
        // RAG Settings (show only when RAG is enabled)
        if (config.ragEnabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            // Knowledge Documents
            SettingItemClickable(
                title = stringResource(R.string.rag_knowledge_data_title),
                subtitle = stringResource(R.string.rag_knowledge_data_subtitle),
                onClick = onManageDocuments,
                trailing = {
                    Row {
                        IconButton(onClick = onManageDocuments) {
                            Icon(Icons.Default.Description, stringResource(R.string.rag_view_documents_icon), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            // RAG Documents
            SettingItemClickable(
                title = stringResource(R.string.rag_documents_title),
                subtitle = stringResource(R.string.rag_documents_subtitle),
                onClick = onAddDocument,
                trailing = {
                    Row {
                        IconButton(onClick = onAddDocument) {
                            Icon(Icons.Default.Add, stringResource(R.string.rag_add_icon), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { /* TODO: Show help */ }) {
                            Icon(Icons.Default.HelpOutline, stringResource(R.string.rag_help_icon), modifier = Modifier.size(20.dp))
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
                                    text = stringResource(R.string.rag_processing_status, status.documentName),
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
                                text = stringResource(R.string.rag_deleting_status, status.documentName),
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
                                text = stringResource(R.string.rag_completed_status),
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
                                    text = stringResource(R.string.rag_failed_status),
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
            val ragChunkLimitFormatter = stringResource(R.string.rag_chunk_limit_formatter)
            SliderSetting(
                title = stringResource(R.string.rag_chunk_limit_title),
                subtitle = stringResource(R.string.rag_chunk_limit_subtitle),
                value = config.ragChunkLimit.toFloat(),
                valueRange = AIConfig.MIN_RAG_CHUNK_LIMIT.toFloat()..AIConfig.MAX_RAG_CHUNK_LIMIT.toFloat(),
                onValueChange = { onRAGChunkLimitChange(it.toInt()) },
                valueFormatter = { ragChunkLimitFormatter.format(it.toInt()) }
            )
            
            // Advanced RAG Settings
            SettingItemClickable(
                title = if (uiState.showAdvancedRAGSettings) 
                    stringResource(R.string.rag_advanced_expanded) 
                else 
                    stringResource(R.string.rag_advanced_collapsed),
                subtitle = stringResource(R.string.rag_advanced_subtitle),
                onClick = { viewModel.toggleAdvancedRAGSettings() }
            )
            
            if (uiState.showAdvancedRAGSettings) {
                OutlinedTextField(
                    value = config.ragTitle,
                    onValueChange = { viewModel.updateRAGTitle(it) },
                    label = { Text(stringResource(R.string.rag_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Chunk Size
                val chunkSizeFormatter = stringResource(R.string.rag_chunk_size_formatter)
                SliderSetting(
                    title = stringResource(R.string.rag_chunk_size_title),
                    subtitle = stringResource(R.string.rag_chunk_size_subtitle),
                    value = config.ragChunkSize.toFloat(),
                    valueRange = AIConfig.MIN_CHUNK_SIZE.toFloat()..AIConfig.MAX_CHUNK_SIZE.toFloat(),
                    onValueChange = { value ->
                        // Round to nearest 10
                        val rounded = (value / 10).toInt() * 10
                        onChunkSizeChange(rounded.toFloat())
                    },
                    valueFormatter = { chunkSizeFormatter.format(it.toInt()) }
                )
                
                // Chunk Overlap
                val chunkOverlapFormatter = stringResource(R.string.rag_chunk_overlap_formatter)
                SliderSetting(
                    title = stringResource(R.string.rag_chunk_overlap_title),
                    subtitle = stringResource(R.string.rag_chunk_overlap_subtitle),
                    value = config.ragChunkOverlap.toFloat(),
                    valueRange = AIConfig.MIN_CHUNK_OVERLAP.toFloat()..AIConfig.MAX_CHUNK_OVERLAP.toFloat(),
                    onValueChange = { value ->
                        // Round to nearest 10
                        val rounded = (value / 10).toInt() * 10
                        onChunkOverlapChange(rounded.toFloat())
                    },
                    valueFormatter = { chunkOverlapFormatter.format(it.toInt()) }
                )
                
                // Warning about recalculating embeddings
                Text(
                    text = stringResource(R.string.rag_recalculate_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingItemClickable(
                    title = stringResource(R.string.rag_instructions_title),
                    subtitle = stringResource(R.string.rag_instructions_subtitle),
                    onClick = { viewModel.showRAGInstructionsDialog() },
                    trailing = {
                        Icon(Icons.Default.Edit, stringResource(R.string.rag_edit_icon), tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }
        }
    }
}
