package com.yourown.ai.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R

/**
 * Dialog for editing context instructions
 */
@Composable
fun ContextInstructionsDialog(
    currentInstructions: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    var text by remember { mutableStateOf(currentInstructions) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.context_instructions_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.context_instructions_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    placeholder = { Text(stringResource(R.string.context_instructions_placeholder)) },
                    maxLines = 15
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(stringResource(R.string.context_instructions_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onReset()
                    onDismiss()
                }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.context_instructions_reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.context_instructions_cancel))
                }
            }
        }
    )
}

/**
 * Dialog for editing memory instructions
 */
@Composable
fun MemoryInstructionsDialog(
    currentInstructions: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    var text by remember { mutableStateOf(currentInstructions) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.memory_instructions_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.memory_instructions_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text(stringResource(R.string.memory_instructions_placeholder)) },
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(stringResource(R.string.memory_instructions_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onReset()
                    onDismiss()
                }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.memory_instructions_reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.memory_instructions_cancel))
                }
            }
        }
    )
}

/**
 * Dialog for editing RAG instructions
 */
@Composable
fun RAGInstructionsDialog(
    currentInstructions: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    var text by remember { mutableStateOf(currentInstructions) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rag_instructions_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.rag_instructions_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    placeholder = { Text(stringResource(R.string.rag_instructions_placeholder)) },
                    maxLines = 12
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(stringResource(R.string.rag_instructions_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onReset()
                    onDismiss()
                }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.rag_instructions_reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.rag_instructions_cancel))
                }
            }
        }
    )
}

/**
 * Dialog for editing Deep Empathy prompt with required placeholder validation
 */
@Composable
fun DeepEmpathyPromptDialog(
    currentPrompt: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    var text by remember { mutableStateOf(currentPrompt) }
    val requiredPlaceholder = "{dialogue_focus}"
    val hasPlaceholder = text.contains(requiredPlaceholder)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.deep_empathy_prompt_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.deep_empathy_prompt_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Placeholder warning
                if (!hasPlaceholder) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(R.string.deep_empathy_prompt_warning, requiredPlaceholder),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text(stringResource(R.string.deep_empathy_prompt_placeholder, requiredPlaceholder)) },
                    maxLines = 6,
                    isError = !hasPlaceholder,
                    supportingText = if (!hasPlaceholder) {
                        { Text(stringResource(R.string.deep_empathy_prompt_error, requiredPlaceholder), color = MaterialTheme.colorScheme.error) }
                    } else null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = hasPlaceholder
            ) {
                Text(stringResource(R.string.deep_empathy_prompt_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onReset()
                    onDismiss()
                }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.deep_empathy_prompt_reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.deep_empathy_prompt_cancel))
                }
            }
        }
    )
}

/**
 * Dialog for editing swipe message prompt with required placeholder validation
 */
@Composable
fun SwipeMessagePromptDialog(
    currentPrompt: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    var text by remember { mutableStateOf(currentPrompt) }
    val requiredPlaceholder = "{swipe_message}"
    val hasPlaceholder = text.contains(requiredPlaceholder)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.swipe_message_prompt_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.swipe_message_prompt_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Placeholder warning
                if (!hasPlaceholder) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(R.string.swipe_message_prompt_warning, requiredPlaceholder),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text(stringResource(R.string.swipe_message_prompt_placeholder)) },
                    maxLines = 6,
                    isError = !hasPlaceholder,
                    supportingText = if (!hasPlaceholder) {
                        { Text(stringResource(R.string.swipe_message_prompt_error, requiredPlaceholder), color = MaterialTheme.colorScheme.error) }
                    } else null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = hasPlaceholder
            ) {
                Text(stringResource(R.string.swipe_message_prompt_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onReset()
                    onDismiss()
                }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.swipe_message_prompt_reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.swipe_message_prompt_cancel))
                }
            }
        }
    )
}

