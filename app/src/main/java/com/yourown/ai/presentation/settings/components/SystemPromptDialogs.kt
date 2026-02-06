package com.yourown.ai.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yourown.ai.data.repository.PromptType
import com.yourown.ai.domain.model.SystemPrompt

/**
 * Dialog for managing list of system prompts
 */
@Composable
fun SystemPromptsListDialog(
    prompts: List<SystemPrompt>,
    promptType: PromptType,
    personas: Map<String, com.yourown.ai.domain.model.Persona> = emptyMap(), // Map<systemPromptId, Persona>
    onDismiss: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (SystemPrompt) -> Unit,
    onDelete: (String) -> Unit,
    onSetDefault: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (promptType == PromptType.API) "API" else "Local"} Prompts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row {
                        IconButton(onClick = onAddNew) {
                            Icon(Icons.Default.Add, "Add new prompt")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close, 
                                "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Divider()
                
                // Prompts list
                if (prompts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "No prompts",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = onAddNew) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add prompt")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(prompts, key = { it.id }) { prompt ->
                            PromptListItem(
                                prompt = prompt,
                                hasLinkedPersona = personas.containsKey(prompt.id),
                                onEdit = { onEdit(prompt) },
                                onDelete = { onDelete(prompt.id) },
                                onSetDefault = { onSetDefault(prompt.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptListItem(
    prompt: SystemPrompt,
    hasLinkedPersona: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (prompt.isDefault) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = prompt.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (prompt.isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "Default",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    
                    if (hasLinkedPersona) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "Persona",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                
                Row {
                    if (!prompt.isDefault) {
                        IconButton(
                            onClick = onSetDefault,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                "Set as default",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            "Edit",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    if (!prompt.isDefault) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                "Delete",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            Text(
                text = prompt.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Used: ${prompt.usageCount} times",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete prompt?") },
            text = { 
                Text(
                    if (hasLinkedPersona) {
                        "This will also delete the linked Persona and all its settings. This action cannot be undone."
                    } else {
                        "This action cannot be undone."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Dialog for editing/creating prompt
 */
@Composable
fun EditPromptDialog(
    prompt: SystemPrompt?,
    promptType: PromptType,
    allPrompts: List<SystemPrompt>,
    onDismiss: () -> Unit,
    onSave: (name: String, content: String, isDefault: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(prompt?.name ?: "") }
    var content by remember { mutableStateOf(prompt?.content ?: "") }
    var isDefault by remember { mutableStateOf(prompt?.isDefault ?: false) }
    
    // Get original default prompt value for reset functionality
    val originalDefaultPrompt = remember(promptType) {
        when (promptType) {
            PromptType.API -> com.yourown.ai.domain.model.AIConfig.DEFAULT_SYSTEM_PROMPT
            PromptType.LOCAL -> com.yourown.ai.domain.model.AIConfig.DEFAULT_LOCAL_SYSTEM_PROMPT
        }
    }
    
    // Найти дефолтный промпт для этого типа и взять первые 100 символов
    val defaultPromptPreview = remember(allPrompts, promptType) {
        allPrompts
            .firstOrNull { it.type == promptType && it.isDefault }
            ?.content
            ?.let { fullContent ->
                if (fullContent.length > 100) {
                    fullContent.take(100) + "..."
                } else {
                    fullContent
                }
            }
            ?: "Enter your system prompt..."
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (prompt == null) "New Persona" else "Edit Persona",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close, 
                            "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    placeholder = { Text("System 1") },
                    singleLine = true
                )
                
                // Content field
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text("Prompt Content") },
                    placeholder = { Text(defaultPromptPreview) },
                    minLines = 10
                )
                
                // Default checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use as default")
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show "Reset to default" for default prompts, "Cancel" for others
                    if (prompt?.isDefault == true) {
                        OutlinedButton(
                            onClick = { 
                                // Reset content to original default
                                content = originalDefaultPrompt
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset to default")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                    
                    Button(
                        onClick = { 
                            onSave(name, content, isDefault)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank() && content.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
