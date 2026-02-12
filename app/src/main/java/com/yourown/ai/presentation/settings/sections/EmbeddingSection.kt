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
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R
import com.yourown.ai.presentation.settings.components.SettingsSection

/**
 * Embedding Models Section - download and manage embedding models
 */
@Composable
fun EmbeddingModelsSection(
    onShowEmbeddingModels: () -> Unit,
    onRecalculateEmbeddings: () -> Unit,
    isRecalculating: Boolean,
    recalculationProgress: String?,
    recalculationProgressPercent: Float,
    memoryProcessingStatus: com.yourown.ai.data.repository.MemoryProcessingStatus = com.yourown.ai.data.repository.MemoryProcessingStatus.Idle,
    useApiEmbeddings: Boolean = false,
    apiEmbeddingsProvider: String = "openai",
    apiEmbeddingsModel: String = "text-embedding-3-small",
    onUseApiEmbeddingsChange: (Boolean) -> Unit = {},
    onApiEmbeddingsProviderChange: (String) -> Unit = {},
    onApiEmbeddingsModelChange: (String) -> Unit = {}
) {
    SettingsSection(
        title = stringResource(R.string.embedding_section_title),
        icon = Icons.Default.Memory,
        subtitle = stringResource(R.string.embedding_section_subtitle)
    ) {
        // Use API Embeddings Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.embedding_use_api_title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.embedding_use_api_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = useApiEmbeddings,
                onCheckedChange = onUseApiEmbeddingsChange
            )
        }
        
        // API Embeddings Configuration (shown when enabled)
        if (useApiEmbeddings) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Provider Selection
            var expandedProvider by remember { mutableStateOf(false) }
            val providers = listOf(
                "openai" to "OpenAI",
                "openrouter" to "OpenRouter"
            )
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.embedding_provider_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { expandedProvider = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = providers.find { it.first == apiEmbeddingsProvider }?.second ?: "OpenAI",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                
                DropdownMenu(
                    expanded = expandedProvider,
                    onDismissRequest = { expandedProvider = false }
                ) {
                    providers.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onApiEmbeddingsProviderChange(value)
                                expandedProvider = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Model Selection
            var expandedModel by remember { mutableStateOf(false) }
            val models = when (apiEmbeddingsProvider) {
                "openai" -> listOf(
                    "text-embedding-3-small" to "text-embedding-3-small (1536 dim)",
                    "text-embedding-3-large" to "text-embedding-3-large (3072 dim)",
                    "text-embedding-ada-002" to "text-embedding-ada-002 (1536 dim, legacy)"
                )
                "openrouter" -> listOf(
                    "text-embedding-3-small" to "OpenAI: text-embedding-3-small",
                    "text-embedding-3-large" to "OpenAI: text-embedding-3-large",
                    "voyage-3" to "Voyage AI: voyage-3",
                    "voyage-3-lite" to "Voyage AI: voyage-3-lite"
                )
                else -> listOf("text-embedding-3-small" to "text-embedding-3-small")
            }
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.embedding_model_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { expandedModel = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = models.find { it.first == apiEmbeddingsModel }?.second 
                            ?: apiEmbeddingsModel,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                
                DropdownMenu(
                    expanded = expandedModel,
                    onDismissRequest = { expandedModel = false }
                ) {
                    models.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onApiEmbeddingsModelChange(value)
                                expandedModel = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Download Embedding Model button (shown when using local)
            FilledTonalButton(
                onClick = onShowEmbeddingModels,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.embedding_download_model))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Recalculate All Embeddings button
        OutlinedButton(
            onClick = onRecalculateEmbeddings,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRecalculating
        ) {
            if (isRecalculating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.embedding_recalculate))
        }
        
        // Memory processing status (during recalculation)
        if (memoryProcessingStatus is com.yourown.ai.data.repository.MemoryProcessingStatus.Recalculating) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.embedding_memory_embeddings),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = memoryProcessingStatus.currentStep,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${memoryProcessingStatus.progress}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = memoryProcessingStatus.progress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Show progress bar and text
        if (isRecalculating || recalculationProgress != null) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            if (isRecalculating) {
                LinearProgressIndicator(
                    progress = { recalculationProgressPercent },
                    modifier = Modifier.fillMaxWidth(),
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recalculationProgress ?: stringResource(R.string.embedding_processing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "${(recalculationProgressPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (recalculationProgress != null) {
                // Completion or error message
                Text(
                    text = recalculationProgress,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (recalculationProgress.startsWith("✅")) {
                        MaterialTheme.colorScheme.primary
                    } else if (recalculationProgress.startsWith("❌")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Warning text
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.embedding_recalculate_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
