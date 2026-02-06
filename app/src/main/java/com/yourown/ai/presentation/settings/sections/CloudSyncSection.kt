package com.yourown.ai.presentation.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourown.ai.domain.model.CloudSyncSettings
import com.yourown.ai.presentation.settings.components.SettingItemClickable
import com.yourown.ai.presentation.settings.components.ToggleSetting
import com.yourown.ai.presentation.settings.components.SettingsSection
import java.text.SimpleDateFormat
import java.util.*

/**
 * Cloud Sync Section - PostgreSQL database synchronization
 */
@Composable
fun CloudSyncSection(
    syncSettings: CloudSyncSettings,
    onToggleSync: (Boolean) -> Unit,
    onEditConnectionString: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onShowSql: () -> Unit = {},
    onShowInstructions: () -> Unit = {},
    isSyncing: Boolean = false
) {
    val databaseTotalMB = 500f // Free tier limit
    val databaseUsageMB = syncSettings.uploadedDataMB // From tracked uploads
    
    SettingsSection(
        title = "Cloud Sync",
        icon = Icons.Default.Cloud,
        subtitle = "Sync with Supabase (PostgreSQL)"
    ) {
        // Supabase Configuration with Setup Guide button
        SettingItemClickable(
            title = "Supabase Configuration",
            subtitle = if (syncSettings.isConfigured) {
                "Project configured"
            } else {
                "Set your Project URL and API Key"
            },
            onClick = onEditConnectionString,
            trailing = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Setup Guide button
                    TextButton(onClick = onShowInstructions) {
                        Icon(
                            Icons.Default.HelpOutline,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Instructions", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.Edit, "Edit")
                }
            }
        )
        
        // Database Usage Progress (only shown when configured)
        if (syncSettings.isConfigured) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Uploaded Data",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format("%.2f", databaseUsageMB)} / ${databaseTotalMB.toInt()} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Progress bar
                LinearProgressIndicator(
                    progress = { (databaseUsageMB / databaseTotalMB).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        databaseUsageMB / databaseTotalMB > 0.9f -> MaterialTheme.colorScheme.error
                        databaseUsageMB / databaseTotalMB > 0.7f -> androidx.compose.ui.graphics.Color(0xFFFFA500)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Free tier: ${((databaseUsageMB / databaseTotalMB) * 100).toInt()}% used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Estimated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Sync Now Button with Last Sync info
            val lastSyncText = if (syncSettings.lastSyncTimestamp > 0) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                "Last sync: ${dateFormat.format(Date(syncSettings.lastSyncTimestamp))}"
            } else {
                "Manually sync all data"
            }
            
            SettingItemClickable(
                title = if (isSyncing) "Syncing..." else "Sync Now",
                subtitle = lastSyncText,
                onClick = onSyncNow,
                enabled = !isSyncing,
                trailing = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Sync, "Sync")
                    }
                }
            )
        }
    }
}
