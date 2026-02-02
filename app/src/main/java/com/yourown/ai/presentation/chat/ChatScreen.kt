package com.yourown.ai.presentation.chat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourown.ai.presentation.chat.components.*
import com.yourown.ai.domain.model.ModelCapabilities
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import com.yourown.ai.domain.model.ModelProvider
import com.yourown.ai.presentation.chat.components.dialogs.EditTitleDialog
import com.yourown.ai.presentation.chat.components.dialogs.ExportChatDialog
import com.yourown.ai.presentation.chat.components.dialogs.RequestLogsDialog
import com.yourown.ai.presentation.chat.components.dialogs.SearchDialog
import com.yourown.ai.presentation.chat.components.dialogs.SystemPromptDialog
import com.yourown.ai.presentation.chat.components.dialogs.ErrorDialog
import com.yourown.ai.presentation.chat.components.dialogs.ImportChatDialog
import com.yourown.ai.presentation.chat.components.dialogs.SourceChatSelectionDialog
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String?,
    onNavigateToSettings: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Speech recognition manager
    val speechRecognitionManager = remember { 
        com.yourown.ai.domain.service.SpeechRecognitionManager(context) 
    }
    
    // Observe speech recognition state
    val isListening by speechRecognitionManager.isListening.collectAsState()
    val recognizedText by speechRecognitionManager.recognizedText.collectAsState()
    
    // Track the last processed text to avoid duplicates
    var lastProcessedText by remember { mutableStateOf("") }
    
    // Update input text when speech is recognized (only once per recognition)
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotEmpty() && recognizedText != lastProcessedText) {
            lastProcessedText = recognizedText
            val currentText = uiState.inputText.trim()
            val newText = if (currentText.isNotEmpty()) {
                "$currentText $recognizedText"
            } else {
                recognizedText
            }
            viewModel.updateInputText(newText)
        }
    }
    
    // Update isListening state in ViewModel
    LaunchedEffect(isListening) {
        viewModel.setListeningState(isListening)
    }
    
    // Request microphone permission
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            speechRecognitionManager.startListening()
        }
    }
    
    // Cleanup speech recognizer on dispose
    DisposableEffect(Unit) {
        onDispose {
            speechRecognitionManager.destroy()
        }
    }
    
    // File picker for saving chat export
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        android.util.Log.d("ChatScreen", "Save file picker result: $uri")
        uri?.let {
            try {
                android.util.Log.d("ChatScreen", "Saving file to: $it")
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val bytes = uiState.exportedChatText?.toByteArray() ?: return@use
                    outputStream.write(bytes)
                    android.util.Log.d("ChatScreen", "File saved: ${bytes.size} bytes")
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Error saving file", e)
            }
        } ?: run {
            android.util.Log.w("ChatScreen", "Save file picker cancelled or returned null")
        }
    }
    
    // File picker for loading chat import
    val loadFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        android.util.Log.d("ChatScreen", "File picker result: $uri")
        uri?.let {
            try {
                android.util.Log.d("ChatScreen", "Reading file from: $it")
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val text = BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readText()
                    }
                    android.util.Log.d("ChatScreen", "File loaded: ${text.length} characters")
                    viewModel.importChat(text)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Error loading file", e)
            }
        } ?: run {
            android.util.Log.w("ChatScreen", "File picker cancelled or returned null")
        }
    }
    
    // Image picker for attachments
    // Image picker for multiple images
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            viewModel.addImage(uri)
        }
    }
    
    // File picker for multiple documents (PDF, TXT, DOC, DOCX)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            viewModel.addFile(uri)
        }
    }
    
    // Check if current model supports attachments
    val supportsAttachments = remember(uiState.selectedModel) {
        uiState.selectedModel?.let { 
            ModelCapabilities.supportsAttachments(it) 
        } ?: false
    }
    
    // Check if model supports documents
    val supportsDocuments = remember(uiState.selectedModel) {
        uiState.selectedModel?.let { model ->
            val modelId = when (model) {
                is ModelProvider.Local -> return@let false // Local models don't support documents
                is ModelProvider.API -> model.modelId
            }
            ModelCapabilities.forModel(modelId).supportsDocuments
        } ?: false
    }
    
    // Check if scrolled to bottom
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            
            // Don't show button if list is empty
            if (uiState.messages.isEmpty()) return@derivedStateOf true
            
            // Check if content actually needs scrolling (has items beyond visible area)
            val totalItemsCount = layoutInfo.totalItemsCount
            val visibleItemsCount = layoutInfo.visibleItemsInfo.size
            val canScroll = totalItemsCount > visibleItemsCount
            
            // If can't scroll (all content fits on screen), consider at bottom
            if (!canScroll) return@derivedStateOf true
            
            // Check if last item is fully visible
            val lastItemIndex = totalItemsCount - 1
            lastVisibleItem?.index == lastItemIndex
        }
    }
    
    // Load conversation when ID changes
    LaunchedEffect(conversationId) {
        conversationId?.let {
            viewModel.selectConversation(it)
        }
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            // Scroll to last item (spacer) to ensure full visibility
            // +1 for loading indicator (if present), +1 for bottom spacer
            val targetIndex = if (uiState.isLoading) {
                uiState.messages.size + 1 // messages + loading + spacer
            } else {
                uiState.messages.size // messages + spacer
            }
            listState.scrollToItem(targetIndex)
        }
    }
    
    // Auto-scroll after streaming completes
    LaunchedEffect(uiState.shouldScrollToBottom) {
        if (uiState.shouldScrollToBottom && uiState.messages.isNotEmpty()) {
            // Scroll to last item (spacer)
            val targetIndex = if (uiState.isLoading) {
                uiState.messages.size + 1
            } else {
                uiState.messages.size
            }
            listState.animateScrollToItem(targetIndex)
            viewModel.onScrolledToBottom()
        }
    }
    
    // Auto-scroll to search result
    LaunchedEffect(uiState.currentSearchIndex, uiState.searchQuery) {
        if (uiState.isSearchMode && uiState.searchMatchCount > 0) {
            val messageIndex = viewModel.getCurrentSearchMessageIndex()
            messageIndex?.let {
                listState.animateScrollToItem(it)
            }
        }
    }
    
    // Scroll to bottom function
    fun scrollToBottom() {
        coroutineScope.launch {
            if (uiState.messages.isNotEmpty()) {
                // Scroll to last item (spacer) to ensure full visibility of last message
                val targetIndex = if (uiState.isLoading) {
                    uiState.messages.size + 1 // messages + loading + spacer
                } else {
                    uiState.messages.size // messages + spacer
                }
                listState.animateScrollToItem(targetIndex)
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Основной контент - Column с чатом и input, реагирует на клавиатуру
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // TopBar - фиксирован вверху
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                ChatTopBar(
                    conversationTitle = uiState.currentConversation?.title ?: "YourOwnAI",
                    selectedModel = uiState.selectedModel,
                    availableModels = uiState.availableModels,
                    localModels = uiState.localModels,
                    pinnedModels = uiState.pinnedModels,
                    isSearchMode = uiState.isSearchMode,
                    searchQuery = uiState.searchQuery,
                    currentSearchIndex = uiState.currentSearchIndex,
                    searchMatchCount = uiState.searchMatchCount,
                    onBackClick = onNavigateBack,
                    onEditTitle = viewModel::showEditTitleDialog,
                    onModelSelect = viewModel::selectModel,
                    onDownloadModel = viewModel::downloadModel,
                    onTogglePinned = viewModel::togglePinnedModel,
                    onSettingsClick = onNavigateToSettings,
                    onSearchClick = viewModel::toggleSearchMode,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onSearchNext = viewModel::navigateToNextSearchResult,
                    onSearchPrevious = viewModel::navigateToPreviousSearchResult,
                    onSearchClose = viewModel::toggleSearchMode,
                    onSystemPromptClick = viewModel::showSystemPromptDialog,
                    onExportChatClick = viewModel::exportChat
                )
            }
            
            // Контент + Input - эта часть реагирует на клавиатуру
            Column(
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
            ) {
                // Чат - занимает все место
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.messages.isEmpty()) {
                        EmptyState(
                            hasModel = uiState.selectedModel != null,
                            onNewChat = {}
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = uiState.messages,
                                key = { it.id },
                                contentType = { "message" }
                            ) { message ->
                                MessageBubble(
                                    message = message,
                                    onLike = { viewModel.toggleLike(message.id) },
                                    onRegenerate = { viewModel.regenerateMessage(message.id) },
                                    onViewLogs = {
                                        message.requestLogs?.let { viewModel.showRequestLogs(it) }
                                    },
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(message.content))
                                    },
                                    onReply = {
                                        viewModel.setReplyToMessage(message)
                                    },
                                    onDelete = {
                                        viewModel.deleteMessage(message.id)
                                    },
                                    searchQuery = if (uiState.isSearchMode) uiState.searchQuery else ""
                                )
                            }

                            // Loading indicator
                            if (uiState.isLoading) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val textToType = "Thinking"
                                        val charCount = textToType.length

                                        val infiniteTransition = rememberInfiniteTransition(label = "typewriter")

                                        // ИЗМЕНЕНО: делаем цикл длиннее - typewriter + пауза с точками
                                        val totalCycleDuration = (charCount * 150) + 1500 // typewriter + 1.5 сек с точками

                                        val progress by infiniteTransition.animateFloat(
                                            initialValue = 0f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(
                                                    durationMillis = totalCycleDuration,
                                                    easing = LinearEasing
                                                ),
                                                repeatMode = RepeatMode.Restart
                                            ),
                                            label = "cycle"
                                        )

                                        // Typewriter фаза длится только первую часть цикла
                                        val typewriterProgress = (progress * totalCycleDuration / (charCount * 150f)).coerceIn(0f, 1f)
                                        val typedIndex = (typewriterProgress * charCount).toInt().coerceIn(0, charCount)

                                        // Показываем ли точки (когда typewriter закончен)
                                        val showDots = typedIndex >= charCount

                                        // Текст typewriter
                                        Text(
                                            text = textToType.substring(0, typedIndex),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Три точки появляются ТОЛЬКО после typewriter
                                        if (showDots) {
                                            Spacer(Modifier.width(8.dp))

                                            // Bounce dots
                                            val dot1 by infiniteTransition.animateFloat(
                                                initialValue = 0f,
                                                targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(600, easing = FastOutSlowInEasing),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "dot1"
                                            )
                                            val dot2 by infiniteTransition.animateFloat(
                                                initialValue = 0f,
                                                targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(600, delayMillis = 200, easing = FastOutSlowInEasing),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "dot2"
                                            )
                                            val dot3 by infiniteTransition.animateFloat(
                                                initialValue = 0f,
                                                targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(600, delayMillis = 400, easing = FastOutSlowInEasing),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "dot3"
                                            )

                                            Box(Modifier.size(20.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterStart)
                                                        .offset(y = (-4.dp) * dot1)
                                                        .size(6.dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f + 0.3f * dot1),
                                                            shape = CircleShape
                                                        )
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.Center)
                                                        .offset(y = (-4.dp) * dot2)
                                                        .size(6.dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f + 0.3f * dot2),
                                                            shape = CircleShape
                                                        )
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterEnd)
                                                        .offset(y = (-4.dp) * dot3)
                                                        .size(6.dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f + 0.3f * dot3),
                                                            shape = CircleShape
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Bottom spacer to ensure last message is fully visible
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        
                        // Scroll to bottom button - inside Box, not Column
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isAtBottom && uiState.messages.isNotEmpty(),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            FloatingActionButton(
                                onClick = { scrollToBottom() },
                                modifier = Modifier.size(40.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Scroll to bottom",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                // Reply preview (if swiped message exists)
                if (uiState.replyToMessage != null) {
                    ReplyPreview(
                        replyToMessage = uiState.replyToMessage!!,
                        onClearReply = viewModel::clearReplyToMessage,
                        onClickReply = {
                            // Scroll to the replied message
                            val messageIndex = uiState.messages.indexOfFirst { it.id == uiState.replyToMessage?.id }
                            if (messageIndex >= 0) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(messageIndex)
                                }
                            }
                        }
                    )
                }
                
                // Attached images preview
                if (uiState.attachedImages.isNotEmpty()) {
                    AttachedImagesPreview(
                        images = uiState.attachedImages,
                        onRemoveImage = viewModel::removeImage
                    )
                }
                
                // Attached files preview
                if (uiState.attachedFiles.isNotEmpty()) {
                    AttachedFilesPreview(
                        files = uiState.attachedFiles,
                        onRemoveFile = viewModel::removeFile
                    )
                }
                
                // Input - внизу, внутри imePadding колонки
                MessageInput(
                    text = uiState.inputText,
                    onTextChange = viewModel::updateInputText,
                    onSend = viewModel::sendMessage,
                    onVoiceInput = {
                        if (isListening) {
                            speechRecognitionManager.stopListening()
                        } else {
                            // Check if permission is granted
                            if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) 
                                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                speechRecognitionManager.startListening()
                            } else {
                                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    onAttachImage = {
                        imagePickerLauncher.launch("image/*")
                    },
                    onAttachFile = if (supportsDocuments) {
                        { filePickerLauncher.launch("*/*") } // Accept all file types, will filter in processor
                    } else null,
                    isListening = isListening,
                    enabled = !uiState.isLoading && uiState.selectedModel != null,
                    supportsAttachments = supportsAttachments,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                )
            }
        }
    }
    
    // Dialogs
    if (uiState.showEditTitleDialog) {
        EditTitleDialog(
            currentTitle = uiState.currentConversation?.title ?: "",
            onDismiss = viewModel::hideEditTitleDialog,
            onSave = viewModel::updateConversationTitle
        )
    }
    
    if (uiState.showRequestLogsDialog && uiState.selectedMessageLogs != null) {
        RequestLogsDialog(
            logs = uiState.selectedMessageLogs!!,
            onDismiss = viewModel::hideRequestLogs
        )
    }
    
    if (uiState.showSystemPromptDialog) {
        SystemPromptDialog(
            systemPrompts = uiState.systemPrompts,
            selectedPromptId = uiState.selectedSystemPromptId,
            onDismiss = viewModel::hideSystemPromptDialog,
            onSelectPrompt = viewModel::selectSystemPrompt
        )
    }
    
    if (uiState.showExportDialog && uiState.exportedChatText != null) {
        ExportChatDialog(
            chatText = uiState.exportedChatText!!,
            onDismiss = viewModel::hideExportDialog,
            onShare = {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, uiState.exportedChatText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Поделиться чатом"))
            },
            onSaveToFile = {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "chat_export_$timestamp.txt"
                saveFileLauncher.launch(fileName)
            },
            onFilterChanged = { filterByLikes ->
                viewModel.exportChat(filterByLikes = filterByLikes)
            }
        )
    }
    
    if (uiState.showImportDialog) {
        ImportChatDialog(
            onDismiss = viewModel::hideImportDialog,
            onImport = { chatText ->
                viewModel.importChat(chatText)
            },
            onLoadFromFile = {
                android.util.Log.d("ChatScreen", "Load from file button clicked")
                try {
                    loadFileLauncher.launch(arrayOf("text/plain", "text/*", "*/*"))
                    android.util.Log.d("ChatScreen", "File picker launched")
                } catch (e: Exception) {
                    android.util.Log.e("ChatScreen", "Error launching file picker", e)
                }
            },
            errorMessage = uiState.importErrorMessage
        )
    }
    
    if (uiState.showSourceChatDialog) {
        SourceChatSelectionDialog(
            conversations = uiState.conversations,
            selectedSourceChatId = uiState.selectedSourceChatId,
            onSourceChatSelected = viewModel::selectSourceChat,
            onConfirm = {
                coroutineScope.launch {
                    viewModel.createNewConversation(uiState.selectedSourceChatId)
                    viewModel.hideSourceChatDialog()
                }
            },
            onDismiss = viewModel::hideSourceChatDialog
        )
    }
    
    if (uiState.showErrorDialog && uiState.errorDetails != null) {
        val clipboardManager = LocalClipboardManager.current
        ErrorDialog(
            errorMessage = uiState.errorDetails!!.errorMessage,
            userMessageContent = uiState.errorDetails!!.userMessageContent,
            modelName = uiState.errorDetails!!.modelName,
            onRetry = viewModel::retryAfterError,
            onCancel = { viewModel.cancelAfterError(clipboardManager) }
        )
    }
    
    // Model load error dialog (simple info dialog)
    if (uiState.showModelLoadErrorDialog && uiState.modelLoadErrorMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::hideModelLoadErrorDialog,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Model Loading Error")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(uiState.modelLoadErrorMessage!!)
                    Text(
                        text = "Please download the model from the model selector before using it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::hideModelLoadErrorDialog) {
                    Text("OK")
                }
            }
        )
    }
}
