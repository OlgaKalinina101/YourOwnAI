package com.yourown.ai.presentation.chat.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Dialog for editing conversation title
 */
@Composable
fun EditTitleDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    // Use remember with key to reset state when dialog reopens with different title
    var title by remember(currentTitle) { mutableStateOf(currentTitle) }
    
    // Reset title when currentTitle changes (e.g., when dialog reopens)
    LaunchedEffect(currentTitle) {
        title = currentTitle
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Chat Title") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                placeholder = { Text("Chat 1") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(title) },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for viewing request logs
 */
@Composable
fun RequestLogsDialog(
    logs: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }
    
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
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "Request Logs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                HorizontalDivider()
                
                // Content
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = logs,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(logs))
                            showCopiedSnackbar = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy")
                    }
                    
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                }
                
                // Snackbar message
                if (showCopiedSnackbar) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        showCopiedSnackbar = false
                    }
                    
                    Text(
                        text = "✓ Copied to clipboard",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
