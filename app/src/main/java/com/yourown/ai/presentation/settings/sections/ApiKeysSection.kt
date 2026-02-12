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
import com.yourown.ai.domain.model.AIProvider
import com.yourown.ai.domain.model.ApiKeyInfo
import com.yourown.ai.presentation.settings.components.SettingsSection

/**
 * API Keys Section - configure API providers
 */
@Composable
fun ApiKeysSection(
    apiKeys: List<ApiKeyInfo>,
    onAddKey: (AIProvider) -> Unit,
    onDeleteKey: (AIProvider) -> Unit,
    onShowLocalModels: () -> Unit
) {
    SettingsSection(
        title = stringResource(R.string.settings_api_keys_title),
        icon = Icons.Default.Key,
        subtitle = stringResource(R.string.settings_api_keys_subtitle),
        isCollapsible = true,
        initiallyExpanded = false
    ) {
        apiKeys.forEach { keyInfo ->
            ApiKeyItem(
                keyInfo = keyInfo,
                onAdd = { onAddKey(keyInfo.provider) },
                onEdit = { onAddKey(keyInfo.provider) },
                onDelete = { onDeleteKey(keyInfo.provider) }
            )
            if (keyInfo != apiKeys.last()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = { /* TODO: Add custom provider */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_custom_provider))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        FilledTonalButton(
            onClick = onShowLocalModels,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.download_local_model))
        }
    }
}

/**
 * Single API key item
 */
@Composable
private fun ApiKeyItem(
    keyInfo: ApiKeyInfo,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = keyInfo.provider.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (keyInfo.isSet && keyInfo.displayKey != null) {
                Text(
                    text = keyInfo.displayKey,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (keyInfo.isSet) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, stringResource(R.string.api_key_item_edit), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, stringResource(R.string.api_key_item_delete), modifier = Modifier.size(20.dp))
                }
            } else {
                FilledTonalButton(onClick = onAdd) {
                    Text(stringResource(R.string.api_key_item_add))
                }
            }
            
            IconButton(onClick = { /* TODO: Show help */ }) {
                Icon(Icons.Default.HelpOutline, stringResource(R.string.settings_api_keys_title), modifier = Modifier.size(20.dp))
            }
        }
    }
}
