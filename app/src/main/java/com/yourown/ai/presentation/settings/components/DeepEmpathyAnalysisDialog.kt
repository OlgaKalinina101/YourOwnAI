package com.yourown.ai.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R

/**
 * Dialog for editing Deep Empathy Analysis prompt
 * Last 4 lines (JSON format) are locked
 */
@Composable
fun DeepEmpathyAnalysisDialog(
    currentPrompt: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    // Split prompt into editable and locked parts
    val lines = currentPrompt.lines()
    val lockedLinesCount = 4 // Last 4 lines for JSON format
    val editableLines = lines.dropLast(lockedLinesCount)
    val lockedLines = lines.takeLast(lockedLinesCount)
    
    var editedPrompt by remember { mutableStateOf(editableLines.joinToString("\n")) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val requiredPlaceholder = "{text}"
    val hasPlaceholder = editedPrompt.contains(requiredPlaceholder)
    
    // Reconstruct full prompt
    val fullPrompt = editedPrompt + "\n\n" + lockedLines.joinToString("\n")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.deep_empathy_analysis_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close, 
                            stringResource(R.string.deep_empathy_analysis_close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.deep_empathy_analysis_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
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
                            text = stringResource(R.string.deep_empathy_analysis_warning, requiredPlaceholder),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Editable prompt
                OutlinedTextField(
                    value = editedPrompt,
                    onValueChange = { editedPrompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text(stringResource(R.string.deep_empathy_analysis_label)) },
                    placeholder = { Text(stringResource(R.string.deep_empathy_analysis_placeholder, requiredPlaceholder)) },
                    minLines = 10,
                    textStyle = MaterialTheme.typography.bodySmall,
                    isError = !hasPlaceholder,
                    supportingText = if (!hasPlaceholder) {
                        { Text(stringResource(R.string.deep_empathy_analysis_error, requiredPlaceholder), color = MaterialTheme.colorScheme.error) }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Locked section (JSON format)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.deep_empathy_analysis_locked_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = lockedLines.joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.deep_empathy_analysis_reset))
                    }
                    
                    Button(
                        onClick = { onSave(fullPrompt) },
                        modifier = Modifier.weight(1f),
                        enabled = editedPrompt.isNotBlank() && hasPlaceholder
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.deep_empathy_analysis_save))
                    }
                }
            }
        }
    }
    
    // Reset confirmation dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.deep_empathy_analysis_reset_confirm_title)) },
            text = { Text(stringResource(R.string.deep_empathy_analysis_reset_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetConfirm = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.deep_empathy_analysis_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.deep_empathy_analysis_reset_cancel))
                }
            }
        )
    }
}
