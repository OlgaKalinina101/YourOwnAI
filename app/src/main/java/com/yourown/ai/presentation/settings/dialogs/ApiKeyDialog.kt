package com.yourown.ai.presentation.settings.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.yourown.ai.domain.model.AIProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyDialog(
    provider: AIProvider,
    onDismiss: () -> Unit,
    onSave: (AIProvider, String) -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Key, contentDescription = null)
        },
        title = {
            Text("${provider.displayName} API Key")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your API key will be encrypted and stored securely on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        showError = false
                    },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (isVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { isVisible = !isVisible }) {
                            Icon(
                                if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isVisible) "Hide" else "Show"
                            )
                        }
                    },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("API key cannot be empty", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = false,
                    maxLines = 3
                )
                
                // Security info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Encrypted with AES-256",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Help text
                Text(
                    text = "Get your API key from ${getApiKeyUrl(provider)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (apiKey.trim().isEmpty()) {
                        showError = true
                    } else {
                        onSave(provider, apiKey.trim())
                    }
                }
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

private fun getApiKeyUrl(provider: AIProvider): String {
    return when (provider) {
        AIProvider.DEEPSEEK -> "platform.deepseek.com"
        AIProvider.OPENAI -> "platform.openai.com"
        AIProvider.ANTHROPIC -> "console.anthropic.com"
        AIProvider.XAI -> "x.ai"
        AIProvider.CUSTOM -> "your provider"
    }
}
