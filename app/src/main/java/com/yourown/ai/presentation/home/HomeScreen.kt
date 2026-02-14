package com.yourown.ai.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourown.ai.R
import com.yourown.ai.presentation.chat.ChatViewModel
import com.yourown.ai.presentation.chat.components.ConversationDrawer
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Home screen - main entry point with getting started guide
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToVoiceChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle imported conversation - navigate to it
    val chatImportedMessage = stringResource(R.string.home_chat_imported)
    LaunchedEffect(uiState.importedConversationId) {
        uiState.importedConversationId?.let { conversationId ->
            scope.launch {
                // Show success message
                snackbarHostState.showSnackbar(
                    message = chatImportedMessage,
                    duration = SnackbarDuration.Short
                )
                // Close drawer
                drawerState.close()
                // Navigate to imported chat
                onNavigateToChat(conversationId)
                // Clear the imported ID
                viewModel.clearImportedConversationId()
            }
        }
    }
    
    // File picker for loading chat import
    val loadFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        android.util.Log.d("HomeScreen", "File picker result: $uri")
        uri?.let {
            try {
                android.util.Log.d("HomeScreen", "Reading file from: $it")
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val text = BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readText()
                    }
                    android.util.Log.d("HomeScreen", "File loaded: ${text.length} characters")
                    viewModel.importChat(text)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeScreen", "Error loading file", e)
            }
        } ?: run {
            android.util.Log.w("HomeScreen", "File picker cancelled or returned null")
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                ConversationDrawer(
                    conversations = uiState.conversations,
                    currentConversationId = null,
                    onConversationClick = { id ->
                        scope.launch { drawerState.close() }
                        onNavigateToChat(id)
                    },
                    onNewConversation = {
                        scope.launch {
                            drawerState.close()
                        }
                        viewModel.showSourceChatDialog()
                    },
                    onVoiceChatClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToVoiceChat()
                    },
                    onImportChatClick = {
                        scope.launch { drawerState.close() }
                        viewModel.showImportDialog()
                    },
                    onDeleteConversation = viewModel::deleteConversation
                )
            }
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "YourOwnAI",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Voice Chat FAB
                    FloatingActionButton(
                        onClick = onNavigateToVoiceChat,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Icon(Icons.Default.Mic, stringResource(R.string.home_voice_chat_icon))
                    }
                    
                    // New Chat FAB
                    ExtendedFloatingActionButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.showSourceChatDialog()
                        },
                        icon = {
                            Icon(Icons.Default.Add, stringResource(R.string.home_new_chat_icon))
                        },
                        text = {
                            Text(stringResource(R.string.home_new_chat_button))
                        }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Chat icon
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Title - показываем статистику если есть чаты, иначе "No conversations yet"
                if (uiState.conversations.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_total_chats, uiState.conversations.size),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = stringResource(R.string.home_total_memories, uiState.totalMemoriesCount),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = stringResource(R.string.home_total_documents, uiState.totalDocumentsCount),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.home_no_conversations),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Subtitle
                    Text(
                        text = stringResource(R.string.home_no_conversations_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Getting Started card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_getting_started),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Step 1: API Key
                        GettingStartedItem(
                            icon = Icons.Default.Key,
                            text = stringResource(R.string.home_step_api_key)
                        )
                        
                        // Step 2: Create conversation
                        GettingStartedItem(
                            icon = Icons.Default.ChatBubbleOutline,
                            text = stringResource(R.string.home_step_create_conversation)
                        )
                        
                        // Step 3: Customize prompts
                        GettingStartedItem(
                            icon = Icons.Default.Edit,
                            text = stringResource(R.string.home_step_customize_prompts)
                        )
                    }
                }
            }
        }
    }
    
    // Import dialog
    if (uiState.showImportDialog) {
        com.yourown.ai.presentation.chat.components.dialogs.ImportChatDialog(
            onDismiss = viewModel::hideImportDialog,
            onImport = { chatText ->
                viewModel.importChat(chatText)
            },
            onLoadFromFile = {
                android.util.Log.d("HomeScreen", "Load from file button clicked")
                try {
                    loadFileLauncher.launch(arrayOf("text/plain", "text/*", "*/*"))
                    android.util.Log.d("HomeScreen", "File picker launched")
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Error launching file picker", e)
                }
            },
            errorMessage = uiState.importErrorMessage
        )
    }
    
    if (uiState.showSourceChatDialog) {
        com.yourown.ai.presentation.chat.components.dialogs.SourceChatSelectionDialog(
            conversations = uiState.conversations,
            selectedSourceChatId = uiState.selectedSourceChatId,
            personas = uiState.personas.values.toList(),
            selectedPersonaId = uiState.selectedNewChatPersonaId,
            onSourceChatSelected = viewModel::selectSourceChat,
            onPersonaSelected = viewModel::selectNewChatPersona,
            onConfirm = {
                scope.launch {
                    val newId = viewModel.createNewConversation(uiState.selectedSourceChatId)
                    viewModel.hideSourceChatDialog()
                    onNavigateToChat(newId)
                }
            },
            onDismiss = viewModel::hideSourceChatDialog
        )
    }
}

@Composable
private fun GettingStartedItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
