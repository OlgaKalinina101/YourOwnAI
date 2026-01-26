package com.yourown.ai.presentation.settings

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.domain.model.AIProvider
import com.yourown.ai.domain.model.ApiKeyInfo
import com.yourown.ai.presentation.settings.dialogs.ApiKeyDialog
import com.yourown.ai.presentation.settings.dialogs.AppearanceDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onViewMemories: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Keys Section
            ApiKeysSection(
                apiKeys = uiState.apiKeys,
                onAddKey = viewModel::showApiKeyDialog,
                onDeleteKey = viewModel::deleteApiKey,
                onShowLocalModels = viewModel::showLocalModelsDialog
            )
            
            // AI Configuration Section
            AIConfigurationSection(
                config = uiState.aiConfig,
                onEditSystemPrompt = viewModel::showSystemPromptDialog,
                onEditLocalSystemPrompt = viewModel::showLocalSystemPromptDialog,
                onTemperatureChange = viewModel::updateTemperature,
                onTopPChange = viewModel::updateTopP,
                onToggleDeepEmpathy = viewModel::toggleDeepEmpathy,
                onToggleMemory = viewModel::toggleMemory,
                onMessageHistoryChange = viewModel::updateMessageHistoryLimit,
                onMaxTokensChange = viewModel::updateMaxTokens
            )
            
            // Knowledge & Memory Section
            KnowledgeMemorySection(
                hasContext = uiState.userContext.content.isNotEmpty(),
                onEditContext = viewModel::showContextDialog,
                onAddDocument = { /* TODO */ },
                onViewMemories = onViewMemories
            )
            
            // Appearance Section
            AppearanceSection(
                onShowAppearance = viewModel::showAppearanceDialog
            )
            
            // Account & Sync Section
            AccountSyncSection()
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Dialogs
    if (uiState.showSystemPromptDialog) {
        SystemPromptDialog(
            currentPrompt = uiState.aiConfig.systemPrompt,
            onDismiss = viewModel::hideSystemPromptDialog,
            onSave = viewModel::updateSystemPrompt
        )
    }
    
    if (uiState.showLocalSystemPromptDialog) {
        SystemPromptDialog(
            currentPrompt = uiState.aiConfig.localSystemPrompt,
            onDismiss = viewModel::hideLocalSystemPromptDialog,
            onSave = viewModel::updateLocalSystemPrompt,
            title = "Local Model System Prompt",
            subtitle = "Shorter prompt for local models (Qwen, Llama)"
        )
    }
    
    if (uiState.showContextDialog) {
        ContextDialog(
            currentContext = uiState.userContext.content,
            onDismiss = viewModel::hideContextDialog,
            onSave = viewModel::updateContext
        )
    }
    
    if (uiState.showApiKeyDialog && uiState.selectedProvider != null) {
        ApiKeyDialog(
            provider = uiState.selectedProvider!!,
            onDismiss = viewModel::hideApiKeyDialog,
            onSave = { key -> viewModel.saveApiKey(uiState.selectedProvider!!, key) }
        )
    }
    
    if (uiState.showLocalModelsDialog) {
        LocalModelsDialog(
            models = uiState.localModels,
            onDismiss = viewModel::hideLocalModelsDialog,
            onDownload = viewModel::downloadModel,
            onDelete = viewModel::deleteModel
        )
    }
    
    if (uiState.showAppearanceDialog) {
        AppearanceDialog(
            onDismiss = viewModel::hideAppearanceDialog
        )
    }
}

@Composable
private fun ApiKeysSection(
    apiKeys: List<ApiKeyInfo>,
    onAddKey: (AIProvider) -> Unit,
    onDeleteKey: (AIProvider) -> Unit,
    onShowLocalModels: () -> Unit
) {
    SettingsSection(
        title = "API Keys",
        icon = Icons.Default.Key,
        subtitle = "Configure your AI providers"
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
            Text("Add custom provider")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        FilledTonalButton(
            onClick = onShowLocalModels,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download Local Model")
        }
    }
}

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
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(20.dp))
                }
            } else {
                FilledTonalButton(onClick = onAdd) {
                    Text("Add")
                }
            }
            
            IconButton(onClick = { /* TODO: Show help */ }) {
                Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AIConfigurationSection(
    config: AIConfig,
    onEditSystemPrompt: () -> Unit,
    onEditLocalSystemPrompt: () -> Unit, // NEW
    onTemperatureChange: (Float) -> Unit,
    onTopPChange: (Float) -> Unit,
    onToggleDeepEmpathy: () -> Unit,
    onToggleMemory: () -> Unit,
    onMessageHistoryChange: (Int) -> Unit,
    onMaxTokensChange: (Int) -> Unit
) {
    SettingsSection(
        title = "AI Configuration",
        icon = Icons.Default.SmartToy,
        subtitle = "Customize your AI's behavior"
    ) {
        // System Prompt (API models)
        SettingItemClickable(
            title = "System Prompt (API)",
            subtitle = "For cloud models (Deepseek, OpenAI, Grok)",
            onClick = onEditSystemPrompt,
            trailing = {
                IconButton(onClick = { /* TODO: Show help */ }) {
                    Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
                }
            }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Local System Prompt (Local models)
        SettingItemClickable(
            title = "System Prompt (Local)",
            subtitle = "For local models (Qwen, Llama)",
            onClick = onEditLocalSystemPrompt,
            trailing = {
                IconButton(onClick = { /* TODO: Show help */ }) {
                    Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
                }
            }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Temperature
        SliderSetting(
            title = "Temperature",
            subtitle = "How creative and free your AI can sound",
            value = config.temperature,
            valueRange = AIConfig.MIN_TEMPERATURE..AIConfig.MAX_TEMPERATURE,
            onValueChange = onTemperatureChange
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Top-p
        SliderSetting(
            title = "Top-p",
            subtitle = "How chaotic your AI can be",
            value = config.topP,
            valueRange = AIConfig.MIN_TOP_P..AIConfig.MAX_TOP_P,
            onValueChange = onTopPChange
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Deep Empathy
        ToggleSetting(
            title = "Deep Empathy",
            subtitle = "How deeply your AI can hear you",
            checked = config.deepEmpathy,
            onCheckedChange = { onToggleDeepEmpathy() }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Memory
        ToggleSetting(
            title = "Memory",
            subtitle = "AI remembers important things automatically",
            checked = config.memoryEnabled,
            onCheckedChange = { onToggleMemory() }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Message History Limit
        DropdownSetting(
            title = "Message History",
            subtitle = "How long the conversation can be before it's considered memory",
            value = config.messageHistoryLimit,
            options = (AIConfig.MIN_MESSAGE_HISTORY..AIConfig.MAX_MESSAGE_HISTORY).toList(),
            onValueChange = onMessageHistoryChange,
            valueSuffix = "pairs"
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Max Tokens
        DropdownSetting(
            title = "Max Tokens",
            subtitle = "Maximum length of AI response",
            value = config.maxTokens,
            options = listOf(256, 512, 1024, 2048, 4096, 8192),
            onValueChange = onMaxTokensChange,
            valueSuffix = "tokens"
        )
    }
}

@Composable
private fun KnowledgeMemorySection(
    hasContext: Boolean,
    onEditContext: () -> Unit,
    onAddDocument: () -> Unit,
    onViewMemories: () -> Unit
) {
    SettingsSection(
        title = "Knowledge & Memory",
        icon = Icons.Default.Psychology,
        subtitle = "Teach your AI about you and your world"
    ) {
        // Context
        SettingItemClickable(
            title = "Context",
            subtitle = "What you want your AI to know about you or anything else",
            onClick = onEditContext,
            trailing = {
                Row {
                    if (hasContext) {
                        Icon(
                            Icons.Default.CheckCircle,
                            "Set",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = { /* TODO: Show help */ }) {
                        Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
                    }
                }
            }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // RAG Documents
        SettingItemClickable(
            title = "RAG Documents",
            subtitle = "What you want to teach your AI - documents, notes, conversations",
            onClick = onAddDocument,
            trailing = {
                Row {
                    IconButton(onClick = onAddDocument) {
                        Icon(Icons.Default.Add, "Add", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { /* TODO: Show help */ }) {
                        Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
                    }
                }
            }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        // Saved Memories
        SettingItemClickable(
            title = "Saved Memories",
            subtitle = "View saved memories",
            onClick = onViewMemories,
            trailing = {
                Icon(Icons.Default.ChevronRight, "View")
            }
        )
    }
}

@Composable
private fun AppearanceSection(
    onShowAppearance: () -> Unit
) {
    SettingsSection(
        title = "Appearance",
        icon = Icons.Default.Palette,
        subtitle = "Theme, colors, and fonts"
    ) {
        SettingItemClickable(
            title = "Customize",
            subtitle = "Change theme, colors, and text size",
            onClick = onShowAppearance,
            trailing = {
                Icon(Icons.Default.ChevronRight, "Customize")
            }
        )
    }
}

@Composable
private fun AccountSyncSection() {
    SettingsSection(
        title = "Account & Sync",
        icon = Icons.Default.CloudSync,
        subtitle = "Backup and synchronization"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Your data is stored locally",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Want to sync across devices?",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { /* TODO: Skip */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
            
            Button(
                onClick = { /* TODO: Sign in with Google */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Sign in with Google")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Why sign in?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                BulletPoint("Backup your chats")
                BulletPoint("Sync across devices")
                BulletPoint("Access from anywhere")
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Without sign in: data stays on this device only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Helper Composables

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            content()
        }
    }
}

@Composable
private fun SettingItemClickable(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing()
        }
    }
}

@Composable
private fun SliderSetting(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "%.2f".format(value),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { /* TODO: Show help */ }) {
                    Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
                }
            }
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ToggleSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            IconButton(onClick = { /* TODO: Show help */ }) {
                Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSetting(
    title: String,
    subtitle: String,
    value: Int,
    options: List<Int>,
    onValueChange: (Int) -> Unit,
    valueSuffix: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.menuAnchor()
                    ) {
                        Text(
                            text = "$value",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$option $valueSuffix",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    onValueChange(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                IconButton(onClick = { /* TODO: Show help */ }) {
                    Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
