package com.yourown.ai.presentation.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourown.ai.data.local.preferences.SettingsManager
import com.yourown.ai.data.repository.ApiKeyRepository
import com.yourown.ai.data.repository.ConversationRepository
import com.yourown.ai.data.repository.LocalModelRepository
import com.yourown.ai.data.repository.MessageRepository
import com.yourown.ai.domain.model.*
import com.yourown.ai.domain.service.LlamaService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: String? = null,
    val currentConversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val localModels: Map<LocalModel, LocalModelInfo> = emptyMap(),
    val availableModels: List<ModelProvider> = emptyList(),
    val selectedModel: ModelProvider? = null,
    val pinnedModels: Set<String> = emptySet(),
    val aiConfig: AIConfig = AIConfig(),
    val userContext: com.yourown.ai.domain.model.UserContext = com.yourown.ai.domain.model.UserContext(),
    val systemPrompts: List<com.yourown.ai.domain.model.SystemPrompt> = emptyList(),
    val selectedSystemPromptId: String? = null,
    val isLoading: Boolean = false,
    val shouldScrollToBottom: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val showEditTitleDialog: Boolean = false,
    val showRequestLogsDialog: Boolean = false,
    val isSearchMode: Boolean = false,
    val showSystemPromptDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showErrorDialog: Boolean = false,
    val errorDetails: ErrorDetails? = null,
    val showModelLoadErrorDialog: Boolean = false,
    val modelLoadErrorMessage: String? = null,
    val selectedMessageLogs: String? = null,
    val exportedChatText: String? = null,
    val importErrorMessage: String? = null,
    val importedConversationId: String? = null,
    val searchQuery: String = "",
    val currentSearchIndex: Int = 0,
    val searchMatchCount: Int = 0,
    val inputText: String = "",
    val replyToMessage: Message? = null,
    val isInitialConversationsLoad: Boolean = true,
    val showSourceChatDialog: Boolean = false,
    val selectedSourceChatId: String? = null,
    val isListening: Boolean = false,
    val attachedImages: List<android.net.Uri> = emptyList(),
    val attachedFiles: List<android.net.Uri> = emptyList()
)

/**
 * Details about an error that occurred during message generation
 */
data class ErrorDetails(
    val errorMessage: String,
    val userMessageId: String,
    val userMessageContent: String,
    val assistantMessageId: String,
    val modelName: String
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val localModelRepository: LocalModelRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val aiConfigRepository: com.yourown.ai.data.repository.AIConfigRepository,
    private val systemPromptRepository: com.yourown.ai.data.repository.SystemPromptRepository,
    private val settingsManager: SettingsManager,
    private val llamaService: LlamaService,
    private val keyboardSoundManager: com.yourown.ai.domain.service.KeyboardSoundManager,
    // New managers
    private val conversationManager: ChatConversationManager,
    private val messageHandler: ChatMessageHandler,
    private val importExportManager: ChatImportExportManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    init {
        loadConversations()
        observeLocalModels()
        observeApiKeys()
        observeSettings()
        loadSavedModel()
        observeSystemPrompts()
        initializeDefaultPrompts()
        observePinnedModels()
        observeKeyboardSoundSettings()
    }
    
    private fun initializeDefaultPrompts() {
        viewModelScope.launch {
            systemPromptRepository.initializeDefaultPrompts()
        }
    }
    
    private fun observeSystemPrompts() {
        viewModelScope.launch {
            systemPromptRepository.getAllPrompts().collect { prompts ->
                _uiState.update { it.copy(systemPrompts = prompts) }
            }
        }
    }
    
    private fun loadConversations() {
        viewModelScope.launch {
            conversationRepository.getAllConversations().collect { conversations ->
                val isInitial = _uiState.value.isInitialConversationsLoad
                
                // Sort conversations by timestamp (newest first)
                val sortedConversations = conversations.sortedByDescending { it.updatedAt }
                
                _uiState.update { it.copy(
                    conversations = sortedConversations,
                    isInitialConversationsLoad = false
                ) }
                
                // Only auto-select on FIRST load when no conversation is selected
                if (isInitial && _uiState.value.currentConversationId == null && sortedConversations.isNotEmpty()) {
                    selectConversation(sortedConversations.first().id)
                }
            }
        }
    }
    
    private fun observeLocalModels() {
        viewModelScope.launch {
            localModelRepository.models.collect { models ->
                _uiState.update { it.copy(localModels = models) }
                updateAvailableModels()
            }
        }
    }
    
    private fun loadSavedModel() {
        viewModelScope.launch {
            settingsManager.selectedModel.collect { savedModel ->
                if (savedModel != null && _uiState.value.selectedModel == null) {
                    // Try to restore saved model
                    val provider = when (savedModel.type) {
                        "local" -> {
                            LocalModel.entries.find { it.name == savedModel.modelId }?.let {
                                ModelProvider.Local(it)
                            }
                        }
                        "api" -> {
                            when (savedModel.provider) {
                                "DEEPSEEK" -> DeepseekModel.entries.find { it.modelId == savedModel.modelId }?.toModelProvider()
                                "OPENAI" -> OpenAIModel.entries.find { it.modelId == savedModel.modelId }?.toModelProvider()
                                "XAI" -> XAIModel.entries.find { it.modelId == savedModel.modelId }?.toModelProvider()
                                else -> null
                            }
                        }
                        else -> null
                    }
                    
                    provider?.let { 
                        _uiState.update { state -> state.copy(selectedModel = it) }
                        if (it is ModelProvider.Local) {
                            loadModelInBackground(it.model)
                        }
                    }
                } else if (savedModel == null && _uiState.value.selectedModel == null) {
                    autoSelectFirstModel()
                }
            }
        }
    }
    
    private fun autoSelectFirstModel() {
        val firstDownloaded = _uiState.value.localModels.entries.firstOrNull { 
            it.value.status is DownloadStatus.Downloaded 
        }
        if (firstDownloaded != null) {
            val provider = ModelProvider.Local(firstDownloaded.key)
            _uiState.update { it.copy(selectedModel = provider) }
            loadModelInBackground(firstDownloaded.key)
            return
        }
        
        val firstApiModel = _uiState.value.availableModels.firstOrNull { it is ModelProvider.API }
        if (firstApiModel != null) {
            _uiState.update { it.copy(selectedModel = firstApiModel) }
        }
    }
    
    private fun observeApiKeys() {
        viewModelScope.launch {
            apiKeyRepository.apiKeys.collect { _ ->
                updateAvailableModels()
            }
        }
    }
    
    private fun updateAvailableModels() {
        val models = mutableListOf<ModelProvider>()
        
        // Add ALL local models
        _uiState.value.localModels.forEach { (model, _) ->
            models.add(ModelProvider.Local(model))
        }
        
        // Add API models if keys are set
        if (apiKeyRepository.hasApiKey(com.yourown.ai.domain.model.AIProvider.DEEPSEEK)) {
            DeepseekModel.entries.forEach { models.add(it.toModelProvider()) }
        }
        if (apiKeyRepository.hasApiKey(com.yourown.ai.domain.model.AIProvider.OPENAI)) {
            OpenAIModel.entries.forEach { models.add(it.toModelProvider()) }
        }
        if (apiKeyRepository.hasApiKey(com.yourown.ai.domain.model.AIProvider.XAI)) {
            XAIModel.entries.forEach { models.add(it.toModelProvider()) }
        }
        if (apiKeyRepository.hasApiKey(com.yourown.ai.domain.model.AIProvider.OPENROUTER)) {
            OpenRouterModel.entries.forEach { models.add(it.toModelProvider()) }
        }
        
        _uiState.update { it.copy(availableModels = models) }
    }
    
    private fun observeSettings() {
        viewModelScope.launch {
            aiConfigRepository.aiConfig.collect { config ->
                _uiState.update { it.copy(aiConfig = config) }
            }
        }
        viewModelScope.launch {
            aiConfigRepository.userContext.collect { context ->
                _uiState.update { it.copy(userContext = context) }
            }
        }
    }
    
    private fun observePinnedModels() {
        viewModelScope.launch {
            settingsManager.pinnedModels.collect { pinnedModels ->
                _uiState.update { it.copy(pinnedModels = pinnedModels) }
            }
        }
    }
    
    private fun observeKeyboardSoundSettings() {
        viewModelScope.launch {
            settingsManager.keyboardSoundVolume.collect { volume ->
                keyboardSoundManager.setSoundVolume(volume)
            }
        }
    }
    
    fun togglePinnedModel(model: ModelProvider) {
        viewModelScope.launch {
            settingsManager.togglePinnedModel(model.getModelKey())
        }
    }
    
    // ===== CONVERSATION MANAGEMENT =====
    
    fun showSourceChatDialog() {
        _uiState.update { it.copy(showSourceChatDialog = true, selectedSourceChatId = null) }
    }
    
    fun hideSourceChatDialog() {
        _uiState.update { it.copy(showSourceChatDialog = false, selectedSourceChatId = null) }
    }
    
    fun selectSourceChat(chatId: String?) {
        _uiState.update { it.copy(selectedSourceChatId = chatId) }
    }
    
    suspend fun createNewConversation(sourceConversationId: String? = null): String {
        val id = conversationManager.createNewConversation(
            conversationCount = _uiState.value.conversations.size,
            sourceConversationId = sourceConversationId
        )
        
        selectConversation(id)
        _uiState.update { it.copy(selectedModel = null) }
        closeDrawer()
        return id
    }
    
    fun selectConversation(conversationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentConversationId = conversationId) }
            
            conversationRepository.getConversationById(conversationId)
                .distinctUntilChanged()
                .collect { conversation ->
                    _uiState.update { 
                        it.copy(
                            currentConversation = conversation,
                            messages = conversation?.messages ?: emptyList()
                        ) 
                    }
                    
                    // Restore model from conversation
                    conversation?.let { conv ->
                        if (conv.model != "No model selected" && conv.provider != "unknown") {
                            val restoredModel = conversationManager.restoreModelFromConversation(conv.model, conv.provider)
                            if (restoredModel != null) {
                                _uiState.update { it.copy(selectedModel = restoredModel) }
                                if (restoredModel is ModelProvider.Local) {
                                    loadModelInBackground(restoredModel.model)
                                }
                            } else {
                                _uiState.update { it.copy(selectedModel = null) }
                            }
                        } else {
                            _uiState.update { it.copy(selectedModel = null) }
                        }
                    }
                }
        }
    }
    
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            conversationManager.deleteConversation(conversationId)
            
            if (_uiState.value.currentConversationId == conversationId) {
                val nextId = conversationManager.getNextConversationAfterDeletion(
                    conversationId,
                    _uiState.value.conversations
                )
                if (nextId != null) {
                    selectConversation(nextId)
                } else {
                    createNewConversation()
                }
            }
        }
    }
    
    fun showEditTitleDialog() {
        _uiState.update { it.copy(showEditTitleDialog = true) }
    }
    
    fun hideEditTitleDialog() {
        _uiState.update { it.copy(showEditTitleDialog = false) }
    }
    
    fun updateConversationTitle(title: String) {
        viewModelScope.launch {
            _uiState.value.currentConversationId?.let { id ->
                conversationManager.updateConversationTitle(id, title)
                hideEditTitleDialog()
            }
        }
    }
    
    // ===== DRAWER =====
    
    fun openDrawer() {
        _uiState.update { it.copy(isDrawerOpen = true) }
    }
    
    fun closeDrawer() {
        _uiState.update { it.copy(isDrawerOpen = false) }
    }
    
    // ===== MODEL SELECTION =====
    
    fun selectModel(model: ModelProvider) {
        _uiState.update { it.copy(selectedModel = model) }
        
        viewModelScope.launch {
            _uiState.value.currentConversationId?.let { conversationId ->
                val (modelName, providerName) = when (model) {
                    is ModelProvider.Local -> model.model.modelName to "local"
                    is ModelProvider.API -> model.modelId to model.provider.displayName
                }
                conversationManager.updateConversationModel(conversationId, modelName, providerName)
            }
            
            // Auto-set default system prompt
            when (model) {
                is ModelProvider.Local -> aiConfigRepository.updateSystemPrompt(AIConfig.DEFAULT_LOCAL_SYSTEM_PROMPT)
                is ModelProvider.API -> aiConfigRepository.updateSystemPrompt(AIConfig.DEFAULT_SYSTEM_PROMPT)
            }
            
            // Save as default
            when (model) {
                is ModelProvider.Local -> {
                    settingsManager.setSelectedModel("local", model.model.name)
                    loadModelInBackground(model.model)
                }
                is ModelProvider.API -> {
                    settingsManager.setSelectedModel("api", model.modelId, model.provider.name)
                }
            }
        }
    }
    
    private fun loadModelInBackground(model: LocalModel) {
        viewModelScope.launch {
            Log.d("ChatViewModel", "Attempting to load model: ${model.displayName}")
            val result = llamaService.loadModel(model)
            result.onSuccess {
                Log.i("ChatViewModel", "Model loaded successfully: ${model.displayName}")
            }.onFailure { error ->
                Log.e("ChatViewModel", "Failed to load model: ${error.message}", error)
                _uiState.update {
                    it.copy(
                        showModelLoadErrorDialog = true,
                        modelLoadErrorMessage = error.message ?: "Unknown error loading model"
                    )
                }
            }
        }
    }
    
    fun downloadModel(model: LocalModel) {
        viewModelScope.launch {
            localModelRepository.downloadModel(model)
        }
    }
    
    // ===== MESSAGES =====
    
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }
    
    fun setListeningState(isListening: Boolean) {
        _uiState.update { it.copy(isListening = isListening) }
    }
    
    fun setReplyToMessage(message: Message) {
        _uiState.update { it.copy(replyToMessage = message) }
    }
    
    fun clearReplyToMessage() {
        _uiState.update { it.copy(replyToMessage = null) }
    }
    
    fun addImage(uri: android.net.Uri) {
        val currentImages = _uiState.value.attachedImages
        val selectedModel = _uiState.value.selectedModel ?: return
        
        // Check model capabilities
        val modelId = when (selectedModel) {
            is ModelProvider.Local -> return // Local models don't support images
            is ModelProvider.API -> selectedModel.modelId
        }
        val capabilities = ModelCapabilities.forModel(modelId)
        val maxImages = capabilities.imageSupport?.maxImages ?: 0
        
        if (currentImages.size >= maxImages) {
            // TODO: Show error to user - max images reached
            Log.d("ChatViewModel", "Max images reached: $maxImages")
            return
        }
        
        _uiState.update { it.copy(attachedImages = currentImages + uri) }
    }
    
    fun removeImage(uri: android.net.Uri) {
        val currentImages = _uiState.value.attachedImages
        _uiState.update { it.copy(attachedImages = currentImages - uri) }
    }
    
    fun clearImages() {
        _uiState.update { it.copy(attachedImages = emptyList()) }
    }
    
    fun addFile(uri: android.net.Uri) {
        val currentFiles = _uiState.value.attachedFiles
        val selectedModel = _uiState.value.selectedModel ?: return
        
        // Check model capabilities
        val modelId = when (selectedModel) {
            is ModelProvider.Local -> return // Local models don't support files
            is ModelProvider.API -> selectedModel.modelId
        }
        val capabilities = ModelCapabilities.forModel(modelId)
        val maxFiles = capabilities.documentSupport?.maxDocuments ?: 0
        
        if (currentFiles.size >= maxFiles) {
            Log.d("ChatViewModel", "Max files reached: $maxFiles")
            return
        }
        
        _uiState.update { it.copy(attachedFiles = currentFiles + uri) }
    }
    
    fun removeFile(uri: android.net.Uri) {
        val currentFiles = _uiState.value.attachedFiles
        _uiState.update { it.copy(attachedFiles = currentFiles - uri) }
    }
    
    fun clearFiles() {
        _uiState.update { it.copy(attachedFiles = emptyList()) }
    }
    
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        
        val conversationId = _uiState.value.currentConversationId ?: return
        val selectedModel = _uiState.value.selectedModel ?: return
        val attachedImages = _uiState.value.attachedImages
        val attachedFiles = _uiState.value.attachedFiles
        
        viewModelScope.launch {
            val replyMessage = _uiState.value.replyToMessage
            
            // Process images - save to cache and get paths
            val imagePaths = attachedImages.mapNotNull { uri ->
                try {
                    com.yourown.ai.util.ImageCompressor.saveCompressedImage(context, uri)
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error processing image", e)
                    null
                }
            }
            
            val imageAttachmentsJson = if (imagePaths.isNotEmpty()) {
                com.google.gson.Gson().toJson(imagePaths)
            } else {
                null
            }
            
            // Process files - save to cache and get metadata
            val fileAttachments = attachedFiles.mapNotNull { uri ->
                try {
                    val filePath = com.yourown.ai.util.FileProcessor.saveFileToCache(context, uri)
                    if (filePath != null) {
                        val fileInfo = com.yourown.ai.util.FileProcessor.getFileInfo(context, uri)
                        if (fileInfo != null) {
                            val extension = com.yourown.ai.util.FileProcessor.getFileExtension(fileInfo.first)
                            com.yourown.ai.domain.model.FileAttachment(
                                path = filePath,
                                name = fileInfo.first,
                                type = extension,
                                sizeBytes = fileInfo.second
                            )
                        } else null
                    } else null
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error processing file", e)
                    null
                }
            }
            
            val fileAttachmentsJson = if (fileAttachments.isNotEmpty()) {
                com.google.gson.Gson().toJson(fileAttachments)
            } else {
                null
            }
            
            _uiState.update { it.copy(
                isLoading = true, 
                inputText = "", 
                replyToMessage = null,
                attachedImages = emptyList(), // Clear attached images
                attachedFiles = emptyList() // Clear attached files
            ) }
            
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.USER,
                content = text,
                createdAt = System.currentTimeMillis(),
                swipeMessageId = replyMessage?.id,
                swipeMessageText = replyMessage?.content,
                imageAttachments = imageAttachmentsJson,
                fileAttachments = fileAttachmentsJson
            )
            
            val aiMessageId = UUID.randomUUID().toString()
            var aiMessageCreated = false
            
            try {
                val config = _uiState.value.aiConfig
                val currentMessages = _uiState.value.messages + userMessage
                val sourceConvId = _uiState.value.currentConversation?.sourceConversationId
                
                // Build message history with context inheritance
                val allMessages = messageHandler.buildMessageHistoryWithInheritance(
                    currentMessages = currentMessages,
                    sourceConversationId = sourceConvId,
                    messageHistoryLimit = config.messageHistoryLimit
                )
                
                val userContextContent = _uiState.value.userContext.content
                
                // Build enhanced context FIRST (before sendMessage)
                val enhancedContextResult = messageHandler.buildEnhancedContextForLogs(
                    baseContext = userContextContent,
                    userMessage = userMessage.content,
                    config = config,
                    selectedModel = selectedModel,
                    conversationId = conversationId,
                    swipeMessage = replyMessage
                )
                
                // Build request logs with full context breakdown
                val requestLogs = messageHandler.buildRequestLogs(
                    model = selectedModel,
                    config = config,
                    allMessages = allMessages,
                    fullContext = enhancedContextResult.fullContext,
                    deepEmpathyAnalysis = enhancedContextResult.deepEmpathyAnalysis,
                    memoriesUsed = enhancedContextResult.memoriesUsed,
                    ragChunksUsed = enhancedContextResult.ragChunksUsed
                )
                
                val modelName = when (selectedModel) {
                    is ModelProvider.Local -> selectedModel.model.modelName
                    is ModelProvider.API -> selectedModel.modelId
                }
                
                // Create placeholder for AI response
                val aiMessage = Message(
                    id = aiMessageId,
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = "",
                    createdAt = System.currentTimeMillis(),
                    model = modelName,
                    temperature = config.temperature,
                    topP = config.topP,
                    deepEmpathy = config.deepEmpathy,
                    memoryEnabled = config.memoryEnabled,
                    messageHistoryLimit = config.messageHistoryLimit,
                    systemPrompt = config.systemPrompt,
                    requestLogs = requestLogs
                )
                
                messageRepository.addMessage(aiMessage)
                aiMessageCreated = true
                
                // Generate response
                val responseBuilder = StringBuilder()
                
                messageHandler.sendMessage(
                    userMessage = userMessage,
                    selectedModel = selectedModel,
                    config = config,
                    userContext = userContextContent,
                    allMessages = allMessages,
                    swipeMessage = replyMessage
                ).collect { chunk ->
                    responseBuilder.append(chunk)
                    
                    // Play keyboard sound for this token
                    keyboardSoundManager.playTypingForToken(chunk)
                    
                    val updatedMessage = aiMessage.copy(
                        content = responseBuilder.toString().trim()
                    )
                    
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map { msg ->
                            if (msg.id == aiMessageId) updatedMessage else msg
                        }
                        state.copy(messages = updatedMessages)
                    }
                }
                
                // Save final message
                val finalMessage = aiMessage.copy(
                    content = responseBuilder.toString().trim()
                )
                messageRepository.updateMessage(finalMessage)
                
                // Extract memory if enabled
                if (config.memoryEnabled) {
                    messageHandler.extractAndSaveMemory(
                        userMessage = userMessage,
                        selectedModel = selectedModel,
                        config = config,
                        conversationId = conversationId
                    )
                }
                
                _uiState.update { it.copy(shouldScrollToBottom = true) }
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error generating response", e)
                
                val errorModelName = when (selectedModel) {
                    is ModelProvider.Local -> selectedModel.model.modelName
                    is ModelProvider.API -> selectedModel.modelId
                }
                
                _uiState.update { 
                    it.copy(
                        showErrorDialog = true,
                        errorDetails = ErrorDetails(
                            errorMessage = e.message ?: "Unknown error",
                            userMessageId = userMessage.id,
                            userMessageContent = userMessage.content,
                            assistantMessageId = if (aiMessageCreated) aiMessageId else "",
                            modelName = errorModelName
                        )
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun toggleLike(messageId: String) {
        viewModelScope.launch {
            messageHandler.toggleLike(messageId)
        }
    }
    
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageHandler.deleteMessage(messageId)
        }
    }
    
    fun regenerateMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val messageToRegenerate = _uiState.value.messages.find { it.id == messageId }
                if (messageToRegenerate == null || messageToRegenerate.role != MessageRole.ASSISTANT) {
                    Log.w("ChatViewModel", "Cannot regenerate: message not found or not assistant")
                    return@launch
                }
                
                val messageIndex = _uiState.value.messages.indexOfFirst { it.id == messageId }
                val userMessage = if (messageIndex > 0) {
                    _uiState.value.messages.getOrNull(messageIndex - 1)
                } else null
                
                if (userMessage == null || userMessage.role != MessageRole.USER) {
                    Log.w("ChatViewModel", "Cannot regenerate: user message not found")
                    return@launch
                }
                
                messageRepository.deleteMessage(userMessage.id)
                messageRepository.deleteMessage(messageToRegenerate.id)
                
                _uiState.update { it.copy(inputText = userMessage.content) }
                kotlinx.coroutines.delay(100)
                sendMessage()
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error regenerating message", e)
            }
        }
    }
    
    fun retryAfterError() {
        viewModelScope.launch {
            try {
                val errorDetails = _uiState.value.errorDetails ?: return@launch
                hideErrorDialog()
                
                _uiState.update { it.copy(inputText = errorDetails.userMessageContent) }
                messageRepository.deleteMessage(errorDetails.userMessageId)
                
                if (errorDetails.assistantMessageId.isNotEmpty()) {
                    messageRepository.deleteMessage(errorDetails.assistantMessageId)
                }
                
                kotlinx.coroutines.delay(100)
                sendMessage()
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error retrying after error", e)
            }
        }
    }
    
    fun cancelAfterError(clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
        viewModelScope.launch {
            try {
                val errorDetails = _uiState.value.errorDetails ?: return@launch
                
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(errorDetails.userMessageContent))
                
                messageRepository.deleteMessage(errorDetails.userMessageId)
                if (errorDetails.assistantMessageId.isNotEmpty()) {
                    messageRepository.deleteMessage(errorDetails.assistantMessageId)
                }
                
                hideErrorDialog()
                Log.i("ChatViewModel", "User message copied to clipboard and messages deleted")
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error canceling after error", e)
            }
        }
    }
    
    fun hideErrorDialog() {
        _uiState.update { 
            it.copy(showErrorDialog = false, errorDetails = null)
        }
    }
    
    fun hideModelLoadErrorDialog() {
        _uiState.update {
            it.copy(showModelLoadErrorDialog = false, modelLoadErrorMessage = null)
        }
    }
    
    fun showRequestLogs(logs: String) {
        _uiState.update { 
            it.copy(showRequestLogsDialog = true, selectedMessageLogs = logs) 
        }
    }
    
    fun hideRequestLogs() {
        _uiState.update { 
            it.copy(showRequestLogsDialog = false, selectedMessageLogs = null) 
        }
    }
    
    fun onScrolledToBottom() {
        _uiState.update { it.copy(shouldScrollToBottom = false) }
    }
    
    // ===== SEARCH =====
    
    fun toggleSearchMode() {
        val newSearchMode = !_uiState.value.isSearchMode
        if (newSearchMode) {
            _uiState.update { 
                it.copy(isSearchMode = true, searchQuery = "", currentSearchIndex = 0, searchMatchCount = 0) 
            }
        } else {
            _uiState.update { 
                it.copy(isSearchMode = false, searchQuery = "", currentSearchIndex = 0, searchMatchCount = 0) 
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        val matches = if (query.isBlank()) {
            emptyList()
        } else {
            _uiState.value.messages.filter { message ->
                message.content.contains(query, ignoreCase = true)
            }
        }
        
        _uiState.update { 
            it.copy(
                searchQuery = query,
                searchMatchCount = matches.size,
                currentSearchIndex = if (matches.isNotEmpty()) 0 else -1
            ) 
        }
    }
    
    fun navigateToNextSearchResult() {
        val currentState = _uiState.value
        if (currentState.searchMatchCount == 0) return
        
        val nextIndex = (currentState.currentSearchIndex + 1) % currentState.searchMatchCount
        _uiState.update { it.copy(currentSearchIndex = nextIndex) }
    }
    
    fun navigateToPreviousSearchResult() {
        val currentState = _uiState.value
        if (currentState.searchMatchCount == 0) return
        
        val prevIndex = if (currentState.currentSearchIndex == 0) {
            currentState.searchMatchCount - 1
        } else {
            currentState.currentSearchIndex - 1
        }
        _uiState.update { it.copy(currentSearchIndex = prevIndex) }
    }
    
    fun getCurrentSearchMessageIndex(): Int? {
        val currentState = _uiState.value
        if (currentState.searchQuery.isBlank() || currentState.searchMatchCount == 0) return null
        
        val matches = currentState.messages.filter { message ->
            message.content.contains(currentState.searchQuery, ignoreCase = true)
        }
        
        val currentMessage = matches.getOrNull(currentState.currentSearchIndex) ?: return null
        return currentState.messages.indexOf(currentMessage)
    }
    
    // ===== SYSTEM PROMPT =====
    
    fun showSystemPromptDialog() {
        val currentPromptId = _uiState.value.currentConversation?.systemPromptId
        _uiState.update { 
            it.copy(showSystemPromptDialog = true, selectedSystemPromptId = currentPromptId) 
        }
    }
    
    fun hideSystemPromptDialog() {
        _uiState.update { it.copy(showSystemPromptDialog = false) }
    }
    
    fun selectSystemPrompt(promptId: String) {
        viewModelScope.launch {
            val prompt = systemPromptRepository.getPromptById(promptId)
            val conversationId = _uiState.value.currentConversationId
            
            if (prompt != null && conversationId != null) {
                conversationManager.updateConversationSystemPrompt(conversationId, promptId, prompt.content)
                _uiState.update { it.copy(selectedSystemPromptId = promptId) }
            }
            
            hideSystemPromptDialog()
        }
    }
    
    // ===== EXPORT/IMPORT =====
    
    fun exportChat(filterByLikes: Boolean = false) {
        val conversation = _uiState.value.currentConversation
        val allMessages = _uiState.value.messages
        
        if (conversation == null || allMessages.isEmpty()) return
        
        val exportedText = importExportManager.exportChat(conversation, allMessages, filterByLikes)
        
        _uiState.update { 
            it.copy(showExportDialog = true, exportedChatText = exportedText) 
        }
    }
    
    fun hideExportDialog() {
        _uiState.update { 
            it.copy(showExportDialog = false, exportedChatText = null) 
        }
    }
    
    fun showImportDialog() {
        _uiState.update { it.copy(showImportDialog = true, importErrorMessage = null) }
    }
    
    fun hideImportDialog() {
        _uiState.update { 
            it.copy(showImportDialog = false, importErrorMessage = null, importedConversationId = null) 
        }
    }
    
    fun clearImportedConversationId() {
        _uiState.update { it.copy(importedConversationId = null) }
    }
    
    fun importChat(chatText: String): String? {
        viewModelScope.launch {
            val (conversationId, error) = importExportManager.importChat(chatText)
            
            if (error != null) {
                _uiState.update { it.copy(importErrorMessage = error) }
            } else if (conversationId != null) {
                _uiState.update { 
                    it.copy(
                        showImportDialog = false,
                        importErrorMessage = null,
                        importedConversationId = conversationId
                    )
                }
            }
        }
        
        return null // Return synchronously for compatibility
    }
}
