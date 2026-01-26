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
import com.yourown.ai.domain.model.LocalModel
import com.yourown.ai.domain.model.ModelProvider

/**
 * Model selector dropdown supporting both local and API models
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    selectedModel: ModelProvider?,
    availableModels: List<ModelProvider>,
    onModelSelect: (ModelProvider) -> Unit,
    onDownloadModel: (LocalModel) -> Unit,
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
        modifier = modifier
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.menuAnchor(),
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
            onDismissRequest = { expanded = false }
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
                val localModels = availableModels.filterIsInstance<ModelProvider.Local>()
                val apiModels = availableModels.filterIsInstance<ModelProvider.API>()
                
                // Local models section
                if (localModels.isNotEmpty()) {
                    Text(
                        text = "LOCAL MODELS",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    localModels.forEach { provider ->
                        ModelMenuItem(
                            provider = provider,
                            isSelected = selectedModel == provider,
                            onSelect = {
                                onModelSelect(provider)
                                expanded = false
                            }
                        )
                    }
                }
                
                // API models section
                if (apiModels.isNotEmpty()) {
                    if (localModels.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Text(
                        text = "API MODELS",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    apiModels.forEach { provider ->
                        ModelMenuItem(
                            provider = provider,
                            isSelected = selectedModel == provider,
                            onSelect = {
                                onModelSelect(provider)
                                expanded = false
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
    onSelect: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        onClick = onSelect
    )
}
