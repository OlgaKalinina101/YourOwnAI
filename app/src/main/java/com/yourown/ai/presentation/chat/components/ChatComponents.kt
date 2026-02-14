package com.yourown.ai.presentation.chat.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R

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
    activePersona: com.yourown.ai.domain.model.Persona?,
    isSearchMode: Boolean,
    searchQuery: String,
    currentSearchIndex: Int,
    searchMatchCount: Int,
    webSearchEnabled: Boolean = false,
    supportsWebSearch: Boolean = false,
    xSearchEnabled: Boolean = false,
    supportsXSearch: Boolean = false,
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
    onExportChatClick: () -> Unit = {},
    onToggleWebSearch: () -> Unit = {},
    onToggleXSearch: () -> Unit = {},
    isExporting: Boolean = false
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
                        placeholder = { Text(stringResource(R.string.topbar_search_placeholder)) },
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
                        Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.topbar_previous_match))
                    }
                    
                    // Navigate down button
                    IconButton(
                        onClick = onSearchNext,
                        enabled = searchMatchCount > 0
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.topbar_next_match))
                    }
                    
                    // Close search button
                    IconButton(onClick = onSearchClose) {
                        Icon(
                            Icons.Default.Close, 
                            stringResource(R.string.topbar_close_search),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // Normal mode
                // Row 1: Back, ModelSelector (left), WebSearch, More Menu, Settings (right)
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
                            Icon(Icons.Default.ArrowBack, stringResource(R.string.topbar_back))
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
                                Icon(Icons.Default.MoreVert, stringResource(R.string.topbar_more_options))
                            }
                        
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.topbar_search)) },
                                    onClick = {
                                        showMenu = false
                                        onSearchClick()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                    }
                                )
                                
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.topbar_persona)) },
                                    onClick = {
                                        showMenu = false
                                        onSystemPromptClick()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    }
                                )
                                
                                DropdownMenuItem(
                                    text = { Text(if (isExporting) stringResource(R.string.topbar_exporting) else stringResource(R.string.topbar_save_chat)) },
                                    onClick = {
                                        showMenu = false
                                        onExportChatClick()
                                    },
                                    enabled = !isExporting,
                                    leadingIcon = {
                                        if (isExporting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Default.Download, contentDescription = null)
                                        }
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
                
                // Row 2: Title (left) + Search buttons (right)
                androidx.compose.runtime.key(conversationTitle) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Title + Edit + Persona
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
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
                            
                            // Persona indicator (inline)
                            if (activePersona != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = activePersona.name,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                        
                        // Right: Search buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            // Web Search button (only show if model supports it)
                            if (supportsWebSearch) {
                                Button(
                                    onClick = onToggleWebSearch,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (webSearchEnabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        contentColor = if (webSearchEnabled) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    ),
                                    modifier = Modifier.height(36.dp),
                                    elevation = if (webSearchEnabled) {
                                        ButtonDefaults.buttonElevation(
                                            defaultElevation = 4.dp,
                                            pressedElevation = 8.dp
                                        )
                                    } else {
                                        ButtonDefaults.buttonElevation(
                                            defaultElevation = 0.dp
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Web",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (webSearchEnabled) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            // X Search button (only show for xAI/Grok models)
                            if (supportsXSearch) {
                                Button(
                                    onClick = onToggleXSearch,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (xSearchEnabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        contentColor = if (xSearchEnabled) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    ),
                                    modifier = Modifier.height(36.dp),
                                    elevation = if (xSearchEnabled) {
                                        ButtonDefaults.buttonElevation(
                                            defaultElevation = 4.dp,
                                            pressedElevation = 8.dp
                                        )
                                    } else {
                                        ButtonDefaults.buttonElevation(
                                            defaultElevation = 0.dp
                                        )
                                    }
                                ) {
                                    Text(
                                        text = "𝕏",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Search",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (xSearchEnabled) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Search status banner (temporary overlay, shown below header with animation)
 */
@Composable
fun BoxScope.SearchStatusBanner(
    message: String?,
    onDismiss: () -> Unit
) {
    // Auto-dismiss after 3 seconds
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3000)
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = message != null,
        enter = androidx.compose.animation.slideInVertically(
            initialOffsetY = { -it }
        ) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically(
            targetOffsetY = { -it }
        ) + androidx.compose.animation.fadeOut(),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = message ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
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
                stringResource(R.string.empty_state_start_conversation)
            } else {
                stringResource(R.string.empty_state_select_model)
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (hasModel) {
                stringResource(R.string.empty_state_type_message)
            } else {
                stringResource(R.string.empty_state_download_model)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}