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
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R
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
        title = stringResource(R.string.cloud_sync_title),
        icon = Icons.Default.Cloud,
        subtitle = stringResource(R.string.cloud_sync_subtitle)
    ) {
        // Supabase Configuration with Setup Guide button
        SettingItemClickable(
            title = stringResource(R.string.cloud_sync_configuration),
            subtitle = if (syncSettings.isConfigured) {
                stringResource(R.string.cloud_sync_configured)
            } else {
                stringResource(R.string.cloud_sync_not_configured)
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
                        Text(stringResource(R.string.cloud_sync_instructions), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.Edit, stringResource(R.string.cloud_sync_edit))
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
                        text = stringResource(R.string.cloud_sync_uploaded_data),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.cloud_sync_usage, String.format("%.2f", databaseUsageMB), databaseTotalMB.toInt()),
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
                        text = stringResource(R.string.cloud_sync_free_tier, ((databaseUsageMB / databaseTotalMB) * 100).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.cloud_sync_estimated),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Sync Now Button with Last Sync info
            val context = androidx.compose.ui.platform.LocalContext.current
            val lastSyncText = if (syncSettings.lastSyncTimestamp > 0) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                context.getString(R.string.cloud_sync_last_sync, dateFormat.format(Date(syncSettings.lastSyncTimestamp)))
            } else {
                context.getString(R.string.cloud_sync_manually_sync)
            }
            
            SettingItemClickable(
                title = if (isSyncing) stringResource(R.string.cloud_sync_syncing) else stringResource(R.string.cloud_sync_sync_now),
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
                        Icon(Icons.Default.Sync, stringResource(R.string.cloud_sync_sync_now))
                    }
                }
            )
        }
    }
}
