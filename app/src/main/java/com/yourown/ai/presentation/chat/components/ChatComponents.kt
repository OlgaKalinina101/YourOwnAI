package com.yourown.ai.presentation.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Top bar for chat screen with title editing and model selector
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    conversationTitle: String,
    selectedModel: com.yourown.ai.domain.model.ModelProvider?,
    availableModels: List<com.yourown.ai.domain.model.ModelProvider>,
    localModels: Map<com.yourown.ai.domain.model.LocalModel, com.yourown.ai.domain.model.LocalModelInfo>,
    pinnedModels: Set<String>,
    isSearchMode: Boolean,
    searchQuery: String,
    currentSearchIndex: Int,
    searchMatchCount: Int,
    onBackClick: () -> Unit,
    onEditTitle: () -> Unit,
    onModelSelect: (com.yourown.ai.domain.model.ModelProvider) -> Unit,
    onDownloadModel: (com.yourown.ai.domain.model.LocalModel) -> Unit,
    onTogglePinned: (com.yourown.ai.domain.model.ModelProvider) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearchNext: () -> Unit = {},
    onSearchPrevious: () -> Unit = {},
    onSearchClose: () -> Unit = {},
    onSystemPromptClick: () -> Unit = {},
    onExportChatClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    // Reset menu state when search mode changes to prevent issues
    LaunchedEffect(isSearchMode) {
        showMenu = false
    }
    
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            if (isSearchMode) {
                // Search mode: Search field with navigation buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search in chat...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        trailingIcon = {
                            if (searchMatchCount > 0) {
                                Text(
                                    text = "${currentSearchIndex + 1}/$searchMatchCount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Navigate up button
                    IconButton(
                        onClick = onSearchPrevious,
                        enabled = searchMatchCount > 0
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, "Previous match")
                    }
                    
                    // Navigate down button
                    IconButton(
                        onClick = onSearchNext,
                        enabled = searchMatchCount > 0
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, "Next match")
                    }
                    
                    // Close search button
                    IconButton(onClick = onSearchClose) {
                        Icon(
                            Icons.Default.Close, 
                            "Close search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // Normal mode: Two rows
                // Row 1: Back, ModelSelector (compact, left-aligned), More Menu, Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side: Back button + ModelSelector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // Back button
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                        
                        // Model selector (compact, not stretched)
                        ModelSelector(
                            selectedModel = selectedModel,
                            availableModels = availableModels,
                            localModels = localModels,
                            pinnedModels = pinnedModels,
                            onModelSelect = onModelSelect,
                            onDownloadModel = onDownloadModel,
                            onTogglePinned = onTogglePinned,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    }
                    
                    // Right side: More menu + Settings
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // More menu button
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "More options")
                            }
                        
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Search") },
                                    onClick = {
                                        showMenu = false
                                        onSearchClick()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                    }
                                )
                                
                                DropdownMenuItem(
                                    text = { Text("Persona") },
                                    onClick = {
                                        showMenu = false
                                        onSystemPromptClick()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    }
                                )
                                
                                DropdownMenuItem(
                                    text = { Text("Save chat") },
                                    onClick = {
                                        showMenu = false
                                        onExportChatClick()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                    }
                                )
                            }
                        }
                        
                        // Settings button
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                }
                
                // Row 2: Title + Edit button (centered)
                androidx.compose.runtime.key(conversationTitle) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = conversationTitle,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = onEditTitle,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Title",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Empty state when no messages
 */
@Composable
fun EmptyState(
    hasModel: Boolean,
    onNewChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (hasModel) {
                "Start your conversation"
            } else {
                "Select a model to begin"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (hasModel) {
                "Type a message below to start chatting with your AI"
            } else {
                "Download a local model or add an API key in Settings"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}