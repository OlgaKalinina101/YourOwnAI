package com.yourown.ai.presentation.settings.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R

/**
 * Help dialog explaining User Context feature
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextHelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        icon = { Icon(Icons.Default.Info, null) },
        title = { Text(stringResource(R.string.context_help_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // What is User Context
                HelpSection(
                    title = stringResource(R.string.context_help_what_title),
                    text = stringResource(R.string.context_help_what_text),
                    icon = Icons.Default.Person
                )
                
                Divider()
                
                // Why use it
                HelpSection(
                    title = stringResource(R.string.context_help_why_title),
                    text = stringResource(R.string.context_help_why_text),
                    icon = Icons.Default.Star
                )
                
                Divider()
                
                // Examples
                HelpSection(
                    title = stringResource(R.string.context_help_examples_title),
                    text = stringResource(R.string.context_help_examples_text),
                    icon = Icons.Default.FormatQuote
                )
                
                Divider()
                
                // Privacy note
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.context_help_privacy_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.context_help_privacy_text),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.context_help_got_it))
            }
        }
    )
}

@Composable
private fun HelpSection(
    title: String,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
