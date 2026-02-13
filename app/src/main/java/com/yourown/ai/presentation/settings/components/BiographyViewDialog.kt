package com.yourown.ai.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yourown.ai.R
import com.yourown.ai.domain.model.UserBiography
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BiographyViewDialog(
    biography: UserBiography,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.biography_copied)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.biography_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Text(
                            text = "Updated: ${dateFormat.format(Date(biography.lastUpdated))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "Processed: ${biography.processedClusters} clusters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Biography content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (biography.values.isNotBlank()) {
                        BiographySection(
                            title = stringResource(R.string.biography_values),
                            content = biography.values
                        )
                    }
                    
                    if (biography.profile.isNotBlank()) {
                        BiographySection(
                            title = stringResource(R.string.biography_profile),
                            content = biography.profile
                        )
                    }
                    
                    if (biography.painPoints.isNotBlank()) {
                        BiographySection(
                            title = stringResource(R.string.biography_pain_points),
                            content = biography.painPoints
                        )
                    }
                    
                    if (biography.joys.isNotBlank()) {
                        BiographySection(
                            title = stringResource(R.string.biography_joys),
                            content = biography.joys
                        )
                    }
                    
                    if (biography.fears.isNotBlank()) {
                        BiographySection(
                            title = stringResource(R.string.biography_fears),
                            content = biography.fears
                        )
                    }
                    
                    if (biography.loves.isNotBlank()) {
                        BiographySection(
                            title = stringResource(R.string.biography_loves),
                            content = biography.loves
                        )
                    }
                    
                    if (biography.currentSituation.isNotBlank()) {
                        BiographySection(
                            title = stringResource(R.string.biography_current_situation),
                            content = biography.currentSituation
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy button
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(biography.toFormattedText()))
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = copiedMessage,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.biography_copy))
                    }
                    
                    // Delete button
                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.biography_delete))
                    }
                }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.biography_delete_confirm_title)) },
            text = { Text(stringResource(R.string.biography_delete_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.biography_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun BiographySection(
    title: String,
    content: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
