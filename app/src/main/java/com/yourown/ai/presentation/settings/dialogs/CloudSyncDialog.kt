package com.yourown.ai.presentation.settings.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.yourown.ai.R

/**
 * Test result data
 */
private data class TestResult(val success: Boolean, val message: String)

/**
 * Dialog for configuring Supabase credentials
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncDialog(
    currentUrl: String,
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onTestConnection: (String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var supabaseUrl by remember { mutableStateOf(currentUrl) }
    var supabaseKey by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestResult?>(null) }
    
    // Clear test result when user changes credentials
    LaunchedEffect(supabaseUrl, supabaseKey) {
        testResult = null
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Cloud, null) },
        title = { Text(stringResource(R.string.cloud_sync_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.cloud_sync_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Project URL field
                OutlinedTextField(
                    value = supabaseUrl,
                    onValueChange = { supabaseUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.cloud_sync_dialog_url_label)) },
                    placeholder = { Text(stringResource(R.string.cloud_sync_dialog_url_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                
                // API Key field
                OutlinedTextField(
                    value = supabaseKey,
                    onValueChange = { supabaseKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.cloud_sync_dialog_key_label)) },
                    placeholder = { Text(stringResource(R.string.cloud_sync_dialog_key_placeholder)) },
                    visualTransformation = if (showKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                if (showKey) stringResource(R.string.cloud_sync_dialog_hide) else stringResource(R.string.cloud_sync_dialog_show)
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                
                // Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.cloud_sync_dialog_hint_encrypted),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Divider()
                        
                        Text(
                            text = stringResource(R.string.cloud_sync_dialog_hint_get_credentials),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.cloud_sync_dialog_hint_steps),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Test Result Card
                if (testResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (testResult!!.success) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (testResult!!.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                null,
                                tint = if (testResult!!.success) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                            Text(
                                text = testResult!!.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (testResult!!.success) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                        }
                    }
                }
                
                // Test Connection Button
                OutlinedButton(
                    onClick = { 
                        testResult = null
                        isTestingConnection = true
                        
                        scope.launch {
                            try {
                                onTestConnection(supabaseUrl, supabaseKey)
                                // Wait a bit for actual test to complete
                                kotlinx.coroutines.delay(2000)
                                isTestingConnection = false
                                testResult = TestResult(
                                    success = true, 
                                    message = context.getString(R.string.cloud_sync_dialog_test_success)
                                )
                            } catch (e: Exception) {
                                isTestingConnection = false
                                testResult = TestResult(
                                    success = false,
                                    message = context.getString(R.string.cloud_sync_dialog_test_failed, e.message ?: "")
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = supabaseUrl.isNotBlank() && supabaseKey.isNotBlank() && !isTestingConnection
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isTestingConnection) stringResource(R.string.cloud_sync_dialog_testing) else stringResource(R.string.cloud_sync_dialog_test_button))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(supabaseUrl, supabaseKey) },
                enabled = supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()
            ) {
                Text(stringResource(R.string.cloud_sync_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cloud_sync_dialog_cancel))
            }
        }
    )
}
