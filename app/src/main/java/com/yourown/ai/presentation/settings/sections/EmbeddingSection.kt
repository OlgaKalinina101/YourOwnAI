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
    memoryProcessingStatus: com.yourown.ai.data.repository.MemoryProcessingStatus = com.yourown.ai.data.repository.MemoryProcessingStatus.Idle
) {
    SettingsSection(
        title = "Embedding Models",
        icon = Icons.Default.Memory,
        subtitle = "Models for semantic search and RAG"
    ) {
        // Download Embedding Model button
        FilledTonalButton(
            onClick = onShowEmbeddingModels,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download Embedding Model")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
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
            Text("Recalculate All Embeddings")
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
                                text = "Memory Embeddings",
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
                        text = recalculationProgress ?: "Processing...",
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
            text = "⚠️ Important: Recalculate all embeddings after switching embedding models",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
