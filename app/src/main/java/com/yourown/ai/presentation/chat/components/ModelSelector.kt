package com.yourown.ai.presentation.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourown.ai.domain.model.DownloadStatus
import com.yourown.ai.domain.model.LocalModel
import com.yourown.ai.domain.model.LocalModelInfo
import com.yourown.ai.domain.model.ModelProvider

/**
 * Model selector dropdown supporting both local and API models
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    selectedModel: ModelProvider?,
    availableModels: List<ModelProvider>,
    localModels: Map<LocalModel, LocalModelInfo>,
    pinnedModels: Set<String>,
    onModelSelect: (ModelProvider) -> Unit,
    onDownloadModel: (LocalModel) -> Unit,
    onTogglePinned: (ModelProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    val displayName = selectedModel?.let { 
        when (it) {
            is ModelProvider.Local -> it.model.displayName
            is ModelProvider.API -> it.displayName
        }
    } ?: "Select Model"
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.widthIn(min = 280.dp)
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.Memory,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 280.dp)
        ) {
            if (availableModels.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "No models available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { expanded = false }
                )
            } else {
                // Group models by type
                val localModelProviders = availableModels.filterIsInstance<ModelProvider.Local>()
                val apiModels = availableModels.filterIsInstance<ModelProvider.API>()
                
                // Sort models: pinned first, then others
                fun sortByPinned(models: List<ModelProvider>): List<ModelProvider> {
                    val pinned = models.filter { it.getModelKey() in pinnedModels }
                    val unpinned = models.filter { it.getModelKey() !in pinnedModels }
                    return pinned + unpinned
                }
                
                val sortedLocalModels = sortByPinned(localModelProviders)
                val sortedApiModels = sortByPinned(apiModels)
                
                // Local models section
                if (sortedLocalModels.isNotEmpty()) {
                    Text(
                        text = "LOCAL MODELS",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    sortedLocalModels.forEach { provider ->
                        val modelInfo = localModels[(provider as ModelProvider.Local).model]
                        ModelMenuItem(
                            provider = provider,
                            isSelected = selectedModel == provider,
                            isPinned = provider.getModelKey() in pinnedModels,
                            modelInfo = modelInfo,
                            onSelect = {
                                onModelSelect(provider)
                                expanded = false
                            },
                            onDownload = {
                                onDownloadModel(provider.model)
                            },
                            onTogglePinned = {
                                onTogglePinned(provider)
                            }
                        )
                    }
                }
                
                // API models section
                if (sortedApiModels.isNotEmpty()) {
                    if (sortedLocalModels.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Text(
                        text = "API MODELS",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    sortedApiModels.forEach { provider ->
                        ModelMenuItem(
                            provider = provider,
                            isSelected = selectedModel == provider,
                            isPinned = provider.getModelKey() in pinnedModels,
                            modelInfo = null,
                            onSelect = {
                                onModelSelect(provider)
                                expanded = false
                            },
                            onDownload = {},
                            onTogglePinned = {
                                onTogglePinned(provider)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelMenuItem(
    provider: ModelProvider,
    isSelected: Boolean,
    isPinned: Boolean,
    modelInfo: LocalModelInfo?,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onTogglePinned: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pin icon
                IconButton(
                    onClick = onTogglePinned,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isPinned) "Unpin" else "Pin",
                        modifier = Modifier.size(16.dp),
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (provider) {
                            is ModelProvider.Local -> provider.model.displayName
                            is ModelProvider.API -> provider.displayName
                        },
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                    when (provider) {
                        is ModelProvider.Local -> {
                            Text(
                                text = provider.model.getSizeFormatted(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is ModelProvider.API -> {
                            Text(
                                text = "Online",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Show download button/status for local models
                if (provider is ModelProvider.Local && modelInfo != null) {
                    when (val status = modelInfo.status) {
                        is DownloadStatus.Downloaded -> {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        is DownloadStatus.Queued -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(60.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Queued",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is DownloadStatus.Downloading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(60.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = status.progress / 100f,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = status.progress / 100f,
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(3.dp)
                                )
                                Text(
                                    text = "${status.progress}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        else -> {
                            IconButton(
                                onClick = {
                                    onDownload()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        onClick = {
            // Only allow selection if model is downloaded (for local models)
            if (provider is ModelProvider.Local && modelInfo != null) {
                if (modelInfo.status is DownloadStatus.Downloaded) {
                    onSelect()
                }
            } else {
                onSelect()
            }
        }
    )
}
