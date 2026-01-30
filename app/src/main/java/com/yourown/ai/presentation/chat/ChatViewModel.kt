package com.yourown.ai.presentation.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourown.ai.data.local.preferences.SettingsManager
import com.yourown.ai.data.repository.ApiKeyRepository
import com.yourown.ai.data.repository.ConversationRepository
import com.yourown.ai.data.repository.LocalModelRepository
import com.yourown.ai.data.repository.MessageRepository
import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.domain.model.Conversation
import com.yourown.ai.domain.model.DeepseekModel
import com.yourown.ai.domain.model.OpenAIModel
import com.yourown.ai.domain.model.OpenRouterModel
import com.yourown.ai.domain.model.XAIModel
import com.yourown.ai.domain.model.DownloadStatus
import com.yourown.ai.domain.model.LocalModel
import com.yourown.ai.domain.model.LocalModelInfo
import com.yourown.ai.domain.model.Message
import com.yourown.ai.domain.model.MessageRole
import com.yourown.ai.domain.model.ModelProvider
import com.yourown.ai.domain.service.AIService
import com.yourown.ai.domain.service.LlamaService
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val importedConversationId: String? = null, // ID of imported chat to open
    val searchQuery: String = "",
    val currentSearchIndex: Int = 0,
    val searchMatchCount: Int = 0,
    val inputText: String = "",
    val replyToMessage: Message? = null, // Swiped message for reply
    val isInitialConversationsLoad: Boolean = true,
    val showSourceChatDialog: Boolean = false, // Dialog for selecting source chat for context inheritance
    val selectedSourceChatId: String? = null   // ID of chat to inherit context from
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
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val localModelRepository: LocalModelRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val aiConfigRepository: com.yourown.ai.data.repository.AIConfigRepository,
    private val systemPromptRepository: com.yourown.ai.data.repository.SystemPromptRepository,
    private val memoryRepository: com.yourown.ai.data.repository.MemoryRepository,
    private val documentEmbeddingRepository: com.yourown.ai.data.repository.DocumentEmbeddingRepository,
    private val settingsManager: SettingsManager,
    private val llamaService: LlamaService,
    private val aiService: AIService
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
                
                _uiState.update { it.copy(
                    conversations = conversations,
                    isInitialConversationsLoad = false
                ) }
                
                // Only auto-select on FIRST load when no conversation is selected
                // Don't re-select when conversations list updates
                if (isInitial && _uiState.value.currentConversationId == null && conversations.isNotEmpty()) {
                    selectConversation(conversations.first().id)
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
                            // Find local model by id
                            LocalModel.entries.find { it.name == savedModel.modelId }?.let {
                                ModelProvider.Local(it)
                            }
                        }
                        "api" -> {
                            // Find API model
                            when (savedModel.provider) {
                                "DEEPSEEK" -> {
                                    DeepseekModel.entries.find { it.modelId == savedModel.modelId }
                                        ?.toModelProvider()
                                }
                                "OPENAI" -> {
                                    OpenAIModel.entries.find { it.modelId == savedModel.modelId }
                                        ?.toModelProvider()
                                }
                                "XAI" -> {
                                    XAIModel.entries.find { it.modelId == savedModel.modelId }
                                        ?.toModelProvider()
                                }
                                else -> null
                            }
                        }
                        else -> null
                    }
                    
                    provider?.let { 
                        _uiState.update { state -> state.copy(selectedModel = it) }
                        // Load local model if needed
                        if (it is ModelProvider.Local) {
                            loadModelInBackground(it.model)
                        }
                    }
                } else if (savedModel == null && _uiState.value.selectedModel == null) {
                    // No saved model, try to auto-select first available
                    autoSelectFirstModel()
                }
            }
        }
    }
    
    private fun autoSelectFirstModel() {
        // Try local models first
        val firstDownloaded = _uiState.value.localModels.entries.firstOrNull { 
            it.value.status is DownloadStatus.Downloaded 
        }
        if (firstDownloaded != null) {
            val provider = ModelProvider.Local(firstDownloaded.key)
            _uiState.update { it.copy(selectedModel = provider) }
            loadModelInBackground(firstDownloaded.key)
            return
        }
        
        // Then try API models
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
        
        // Add ALL local models (downloaded or not)
        _uiState.value.localModels.forEach { (model, info) ->
            models.add(ModelProvider.Local(model))
        }
        
        // Add Deepseek models if API key is set
        if (apiKeyRepository.hasApiKey(com.yourown.ai.domain.model.AIProvider.DEEPSEEK)) {
            DeepseekModel.entries.forEach { model ->
                models.add(model.toModelProvider())
            }
        }
        
        // Add OpenAI models if API key is set
        if (apiKeyRepository.hasApiKey(com.yourown.ai.domain.model.AIProvider.OPENAI)) {
            OpenAIModel.entries.forEach { model ->
                models.add(model.toModelProvider())
            }
        }
        
        // Add x.ai models if API key is set
        if (apiKeyRepository.hasApiKey(com.yourown.ai.domain.model.AIProvider.XAI)) {
            XAIModel.entries.forEach { model ->
                models.add(model.toModelProvider())
            }
        }
        
        // Add OpenRouter models if API key is set
        if (apiKeyRepository.hasApiKey(com.yourown.ai.domain.model.AIProvider.OPENROUTER)) {
            OpenRouterModel.entries.forEach { model ->
                models.add(model.toModelProvider())
            }
        }
        
        _uiState.update { it.copy(availableModels = models) }
    }
    
    private fun observeSettings() {
        viewModelScope.launch {
            // Observe AI config from repository
            aiConfigRepository.aiConfig.collect { config ->
                _uiState.update { it.copy(aiConfig = config) }
            }
        }
        viewModelScope.launch {
            // Observe user context from repository
            aiConfigRepository.userContext.collect { context ->
                _uiState.update { it.copy(userContext = context) }
            }
        }
    }
    
    // Conversation Management
    
    /**
     * Show dialog for selecting source chat (for context inheritance)
     */
    fun showSourceChatDialog() {
        _uiState.update { it.copy(showSourceChatDialog = true, selectedSourceChatId = null) }
    }
    
    /**
     * Hide source chat selection dialog
     */
    fun hideSourceChatDialog() {
        _uiState.update { it.copy(showSourceChatDialog = false, selectedSourceChatId = null) }
    }
    
    /**
     * Select source chat for context inheritance
     */
    fun selectSourceChat(chatId: String?) {
        _uiState.update { it.copy(selectedSourceChatId = chatId) }
    }
    
    /**
     * Create new conversation, optionally inheriting context from source chat
     */
    suspend fun createNewConversation(sourceConversationId: String? = null): String {
        val count = _uiState.value.conversations.size + 1
        val title = "Chat $count"
        
        // NEW CHAT - no model selected yet (user must choose)
        val modelName = "No model selected"
        val provider = "unknown"
        
        // Get default API system prompt (will be updated when user selects model)
        val defaultPrompt = systemPromptRepository.getDefaultApiPrompt()
        val systemPromptContent = defaultPrompt?.content ?: _uiState.value.aiConfig.systemPrompt
        val systemPromptId = defaultPrompt?.id
        
        val id = conversationRepository.createConversation(
            title = title,
            systemPrompt = systemPromptContent,
            model = modelName,
            provider = provider,
            systemPromptId = systemPromptId,
            sourceConversationId = sourceConversationId
        )
        
        selectConversation(id)
        
        // IMPORTANT: Reset selectedModel to null for new chat
        // User MUST select model manually
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
                    
                    // ALWAYS restore model from conversation (each chat has its own model)
                    conversation?.let { conv ->
                        // Only restore if model was selected (not "No model selected")
                        if (conv.model != "No model selected" && conv.provider != "unknown") {
                            val restoredModel = restoreModelFromConversation(conv.model, conv.provider)
                            if (restoredModel != null) {
                                _uiState.update { it.copy(selectedModel = restoredModel) }
                                // Load local model if needed
                                if (restoredModel is ModelProvider.Local) {
                                    loadModelInBackground(restoredModel.model)
                                }
                            } else {
                                // Model not found - reset to null (user must select)
                                _uiState.update { it.copy(selectedModel = null) }
                            }
                        } else {
                            // New chat without model - reset to null
                            _uiState.update { it.copy(selectedModel = null) }
                        }
                    }
                }
        }
    }
    
    private fun restoreModelFromConversation(modelName: String, providerName: String): ModelProvider? {
        return when (providerName) {
            "local" -> {
                // Find local model by name
                LocalModel.entries.find { it.modelName == modelName }?.let {
                    ModelProvider.Local(it)
                }
            }
            "Deepseek" -> {
                // Find Deepseek model
                DeepseekModel.entries.find { it.modelId == modelName }?.toModelProvider()
            }
            "OpenAI" -> {
                // Find OpenAI model
                OpenAIModel.entries.find { it.modelId == modelName }?.toModelProvider()
            }
            "OpenRouter" -> {
                // Find OpenRouter model
                OpenRouterModel.entries.find { it.modelId == modelName }?.toModelProvider()
            }
            "x.ai (Grok)" -> {
                // Find x.ai model
                XAIModel.entries.find { it.modelId == modelName }?.toModelProvider()
            }
            else -> null
        }
    }
    
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(conversationId)
            
            // If deleted current conversation, select another
            if (_uiState.value.currentConversationId == conversationId) {
                val conversations = _uiState.value.conversations
                val nextConversation = conversations.firstOrNull { it.id != conversationId }
                if (nextConversation != null) {
                    selectConversation(nextConversation.id)
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
                conversationRepository.updateConversationTitle(id, title)
                hideEditTitleDialog()
            }
        }
    }
    
    // Drawer
    
    fun openDrawer() {
        _uiState.update { it.copy(isDrawerOpen = true) }
    }
    
    fun closeDrawer() {
        _uiState.update { it.copy(isDrawerOpen = false) }
    }
    
    // Model Selection
    
    fun selectModel(model: ModelProvider) {
        _uiState.update { it.copy(selectedModel = model) }
        
        viewModelScope.launch {
            // Save to current conversation
            _uiState.value.currentConversationId?.let { conversationId ->
                val (modelName, providerName) = when (model) {
                    is ModelProvider.Local -> model.model.modelName to "local"
                    is ModelProvider.API -> model.modelId to model.provider.displayName
                }
                conversationRepository.updateConversationModel(conversationId, modelName, providerName)
            }
            
            // Auto-set default system prompt based on model type
            when (model) {
                is ModelProvider.Local -> {
                    // Set local default system prompt for offline models
                    aiConfigRepository.updateSystemPrompt(AIConfig.DEFAULT_LOCAL_SYSTEM_PROMPT)
                }
                is ModelProvider.API -> {
                    // Set API default system prompt for cloud models
                    aiConfigRepository.updateSystemPrompt(AIConfig.DEFAULT_SYSTEM_PROMPT)
                }
            }
            
            // Also save as default for new chats
            when (model) {
                is ModelProvider.Local -> {
                    settingsManager.setSelectedModel(
                        type = "local",
                        modelId = model.model.name
                    )
                    loadModelInBackground(model.model)
                }
                is ModelProvider.API -> {
                    settingsManager.setSelectedModel(
                        type = "api",
                        modelId = model.modelId,
                        provider = model.provider.name
                    )
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
                
                // Show error dialog for model loading failure
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
    
    // Messages
    
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }
    
    fun setReplyToMessage(message: Message) {
        _uiState.update { it.copy(replyToMessage = message) }
    }
    
    fun clearReplyToMessage() {
        _uiState.update { it.copy(replyToMessage = null) }
    }
    
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        
        val conversationId = _uiState.value.currentConversationId ?: return
        val selectedModel = _uiState.value.selectedModel ?: return
        
        viewModelScope.launch {
            // Get reply message before clearing UI state
            val replyMessage = _uiState.value.replyToMessage
            
            _uiState.update { it.copy(isLoading = true, inputText = "", replyToMessage = null) }
            
            // Create user message (declare before try block for error handling)
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.USER,
                content = text,
                createdAt = System.currentTimeMillis(),
                swipeMessageId = replyMessage?.id,
                swipeMessageText = replyMessage?.content
            )
            
            // Generate AI message ID (declare before try block for error handling)
            val aiMessageId = UUID.randomUUID().toString()
            var aiMessageCreated = false // Track if AI message was added to DB
            
            try {
                // Add user message to repository
                messageRepository.addMessage(userMessage)
                
                // Prepare AI config from settings
                val config = _uiState.value.aiConfig
                
                // Get conversation history with context inheritance support
                val currentMessages = _uiState.value.messages + userMessage
                val sourceConvId = _uiState.value.currentConversation?.sourceConversationId
                
                // Build message history with context inheritance
                val allMessages = buildMessageHistoryWithInheritance(
                    currentMessages = currentMessages,
                    sourceConversationId = sourceConvId,
                    messageHistoryLimit = config.messageHistoryLimit
                )
                
                // Get user context from state
                val userContextContent = _uiState.value.userContext.content
                
                // Deep Empathy, Memory and RAG work ONLY with API models
                // Local models use only localSystemPrompt and last message
                val isApiModel = selectedModel is ModelProvider.API
                
                // Analyze Deep Empathy focus points if enabled (ONLY for API models)
                val deepEmpathyFocusPrompt = if (config.deepEmpathy && isApiModel) {
                    try {
                        // Replace {text} placeholder with actual message
                        val analysisPrompt = config.deepEmpathyAnalysisPrompt.replace("{text}", text)
                        
                        // Call AI to analyze focus points
                        val provider = _uiState.value.selectedModel
                        if (provider == null) {
                            Log.w("ChatViewModel", "No model selected for Deep Empathy analysis")
                            null
                        } else if (provider is ModelProvider.API) {
                            // Deep Empathy analysis only for API models
                            val analysisResponseBuilder = StringBuilder()
                            aiService.generateResponse(
                                provider = provider,
                                messages = listOf(
                                    Message(
                                        id = UUID.randomUUID().toString(),
                                        conversationId = conversationId,
                                        role = MessageRole.USER,
                                        content = analysisPrompt,
                                        createdAt = System.currentTimeMillis()
                                    )
                                ),
                                systemPrompt = "Ты - аналитик смысла.",
                                userContext = null,
                                config = config.copy(temperature = 0.3f) // Lower temperature for structured output
                            ).collect { chunk ->
                                analysisResponseBuilder.append(chunk)
                            }
                            
                            val analysisResponse = analysisResponseBuilder.toString()
                        
                            // Parse JSON response
                            val focusPoints = parseDeepEmpathyFocus(analysisResponse)
                        
                            // If focus points found, format them and insert into deepEmpathyPrompt
                            if (focusPoints.isNotEmpty()) {
                                val focusText = focusPoints.joinToString(", ")
                                config.deepEmpathyPrompt.replace("{dialogue_focus}", focusText)
                            } else {
                                null
                            }
                        } else {
                            // Local models don't support Deep Empathy
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "Deep Empathy analysis failed", e)
                        null
                    }
                } else {
                    null
                }
                
                // Get relevant memories if Memory is enabled (ONLY for API models)
                val relevantMemories = if (config.memoryEnabled && isApiModel) {
                    memoryRepository.findSimilarMemories(
                        query = text, 
                        limit = config.memoryLimit,
                        minAgeDays = config.memoryMinAgeDays
                    )
                } else {
                    emptyList()
                }
                
                // Get relevant RAG chunks if RAG is enabled (ONLY for API models)
                val relevantChunks = if (config.ragEnabled && isApiModel) {
                    documentEmbeddingRepository.searchSimilarChunks(text, topK = config.ragChunkLimit)
                        .map { it.first } // Extract DocumentChunkEntity from Pair
                } else {
                    emptyList()
                }
                
                // Build enhanced context with Deep Empathy focus, swipe message, memories and RAG
                val enhancedContext = buildEnhancedContext(
                    baseContext = userContextContent,
                    memories = relevantMemories,
                    ragChunks = relevantChunks,
                    deepEmpathyFocus = deepEmpathyFocusPrompt,
                    swipeMessage = replyMessage
                )
                
                // Build request logs with full context
                val requestLogs = buildRequestLogs(
                    model = selectedModel,
                    config = config,
                    messageCount = allMessages.size,
                    allMessages = allMessages,
                    userContext = enhancedContext.ifBlank { null }
                )
                
                // Get model name for logging
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
                
                // Add placeholder message
                messageRepository.addMessage(aiMessage)
                aiMessageCreated = true // Mark that AI message was added to DB
                
                // Generate response using unified AIService
                val responseBuilder = StringBuilder()
                
                // Choose system prompt based on model type
                val systemPrompt = when (selectedModel) {
                    is ModelProvider.Local -> config.localSystemPrompt
                    is ModelProvider.API -> config.systemPrompt
                }
                
                aiService.generateResponse(
                    provider = selectedModel,
                    messages = allMessages,
                    systemPrompt = systemPrompt,
                    userContext = enhancedContext.ifBlank { null },
                    config = config
                ).collect { chunk ->
                    responseBuilder.append(chunk)
                    
                    // Update message locally in UI state (not DB) to avoid triggering Flow
                    val updatedMessage = aiMessage.copy(
                        content = responseBuilder.toString().trim()
                    )
                    
                    // Update messages list in UI state directly
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map { msg ->
                            if (msg.id == aiMessageId) updatedMessage else msg
                        }
                        state.copy(messages = updatedMessages)
                    }
                }
                
                // After streaming completes, save final message to DB
                val finalMessage = aiMessage.copy(
                    content = responseBuilder.toString().trim()
                )
                messageRepository.updateMessage(finalMessage)
                
                // Extract and save memory if enabled
                if (config.memoryEnabled) {
                    extractAndSaveMemory(
                        userMessage = userMessage,
                        selectedModel = selectedModel,
                        config = config,
                        conversationId = conversationId
                    )
                }
                
                // Trigger scroll to bottom after streaming completes
                _uiState.update { it.copy(shouldScrollToBottom = true) }
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error generating response", e)
                
                // Get model name for error message
                val errorModelName = when (selectedModel) {
                    is ModelProvider.Local -> selectedModel.model.modelName
                    is ModelProvider.API -> selectedModel.modelId
                }
                
                // Show error dialog instead of immediately saving error message
                // Only include assistantMessageId if the message was actually created in DB
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

    /**
     * Build enhanced context with base context, memories, and RAG chunks
     *
     * Format:
     * [Base Context]
     *
     * Твои воспоминания:
     * - Memory 1
     * - Memory 2
     *
     * Твоя база знаний:
     * - Chunk 1
     * - Chunk 2
     */
    private fun buildEnhancedContext(
        baseContext: String,
        memories: List<com.yourown.ai.domain.model.MemoryEntry>,
        ragChunks: List<com.yourown.ai.data.local.entity.DocumentChunkEntity>,
        deepEmpathyFocus: String? = null,
        swipeMessage: Message? = null
    ): String {
        val config = _uiState.value.aiConfig
        val parts = mutableListOf<String>()

        // Deep Empathy Focus (if present, add at the very beginning)
        if (!deepEmpathyFocus.isNullOrBlank()) {
            parts.add(deepEmpathyFocus.trim())
        }
        
        // Swipe Message (reply) - add right after Deep Empathy
        if (swipeMessage != null && config.swipeMessagePrompt.isNotBlank()) {
            val swipePrompt = config.swipeMessagePrompt.replace("{swipe_message}", swipeMessage.content)
            parts.add(swipePrompt.trim())
        }

        // Add context instructions ONLY if Memory OR RAG are enabled and have content
        // Don't show instructions if both are disabled
        if ((memories.isNotEmpty() || ragChunks.isNotEmpty()) 
            && config.contextInstructions.isNotBlank()) {
            parts.add(config.contextInstructions.trim())
        }

        // Base context (персона, настройки и т.п.)
        if (baseContext.isNotBlank()) {
            parts.add(baseContext.trim())
        }

        // Memories (grouped by time)
        if (memories.isNotEmpty()) {
            val memoriesText = buildString {
                // Add title only if not blank
                if (config.memoryTitle.isNotBlank()) {
                    appendLine("${config.memoryTitle}:")
                }
                // Add instructions only if not blank
                if (config.memoryInstructions.isNotBlank()) {
                    appendLine(config.memoryInstructions)
                    appendLine()
                }
                
                // Group memories by time and add timestamps
                val groupedMemories = groupMemoriesByTime(memories)
                groupedMemories.forEach { (timeLabel, memoryList) ->
                    appendLine("$timeLabel:")
                    memoryList.forEach { memory ->
                        appendLine("  • ${memory.fact}")
                    }
                    appendLine()
                }
            }.trim()
            parts.add(memoriesText)
        }

        // RAG chunks
        if (ragChunks.isNotEmpty()) {
            val ragText = buildString {
                // Add title only if not blank
                if (config.ragTitle.isNotBlank()) {
                    appendLine("${config.ragTitle}:")
                }
                // Add instructions only if not blank
                if (config.ragInstructions.isNotBlank()) {
                    appendLine(config.ragInstructions)
                    appendLine()
                }
                ragChunks.forEachIndexed { index, chunk ->
                    appendLine("${index + 1}. ${chunk.content}")
                }
            }.trim()
            parts.add(ragText)
        }

        return parts.joinToString("\n\n")
    }
    
    /**
     * Group memories by time periods with human-readable labels
     * 
     * Time periods:
     * - Days: "1 день назад", "2 дня назад", ..., "7 дней назад" (1-7 days)
     * - Weeks: "1 неделю назад", "2 недели назад", "3 недели назад" (7-21 days)
     * - Months: "1 месяц назад", "2 месяца назад", ..., "5 месяцев назад" (21-150 days)
     * - "Полгода назад" (150-180 days)
     * - "Давно" (> 180 days / ~6 months)
     */
    private fun groupMemoriesByTime(
        memories: List<com.yourown.ai.domain.model.MemoryEntry>
    ): Map<String, List<com.yourown.ai.domain.model.MemoryEntry>> {
        val currentTime = System.currentTimeMillis()
        val groups = mutableMapOf<String, MutableList<com.yourown.ai.domain.model.MemoryEntry>>()
        
        memories.forEach { memory ->
            val ageMillis = currentTime - memory.createdAt
            val ageDays = ageMillis / (24 * 60 * 60 * 1000)
            
            val timeLabel = when {
                // Days: 1-7 дней
                ageDays < 1 -> "1 день назад"
                ageDays < 2 -> "2 дня назад"
                ageDays < 3 -> "3 дня назад"
                ageDays < 4 -> "4 дня назад"
                ageDays < 5 -> "5 дней назад"
                ageDays < 6 -> "6 дней назад"
                ageDays < 7 -> "7 дней назад"
                
                // Weeks: 1-3 недели (7-21 days)
                ageDays < 14 -> "1 неделю назад"
                ageDays < 21 -> "2 недели назад"
                ageDays < 28 -> "3 недели назад"
                
                // Months: 1-5 месяцев (approximating 1 month = 30 days)
                ageDays < 60 -> "1 месяц назад"    // ~30-60 days
                ageDays < 90 -> "2 месяца назад"   // ~60-90 days
                ageDays < 120 -> "3 месяца назад"  // ~90-120 days
                ageDays < 150 -> "4 месяца назад"  // ~120-150 days
                ageDays < 180 -> "5 месяцев назад" // ~150-180 days
                
                // Half a year and beyond
                ageDays < 365 -> "Полгода назад"   // ~180-365 days
                else -> "Давно"                     // > 365 days (1 year+)
            }
            
            groups.getOrPut(timeLabel) { mutableListOf() }.add(memory)
        }
        
        // Sort by time (newest to oldest)
        val timeOrder = listOf(
            // Days
            "1 день назад",
            "2 дня назад",
            "3 дня назад",
            "4 дня назад",
            "5 дней назад",
            "6 дней назад",
            "7 дней назад",
            // Weeks
            "1 неделю назад",
            "2 недели назад",
            "3 недели назад",
            // Months
            "1 месяц назад",
            "2 месяца назад",
            "3 месяца назад",
            "4 месяца назад",
            "5 месяцев назад",
            // Beyond
            "Полгода назад",
            "Давно"
        )
        
        return groups.toSortedMap(compareBy { timeOrder.indexOf(it) })
    }
    
    private fun buildRequestLogs(
        model: ModelProvider,
        config: AIConfig,
        messageCount: Int,
        allMessages: List<Message> = emptyList(),
        userContext: String? = null
    ): String {
        val modelInfo = when (model) {
            is ModelProvider.Local -> mapOf(
                "type" to "local",
                "modelName" to model.model.modelName,
                "displayName" to model.model.displayName,
                "sizeInMB" to model.model.sizeInMB
            )
            is ModelProvider.API -> mapOf(
                "type" to "api",
                "provider" to model.provider.displayName,
                "modelId" to model.modelId,
                "displayName" to model.displayName
            )
        }
        
        // Choose system prompt based on model type
        val actualSystemPrompt = when (model) {
            is ModelProvider.Local -> config.localSystemPrompt
            is ModelProvider.API -> config.systemPrompt
        }
        
        // Build messages list (limited by history)
        val historyLimit = config.messageHistoryLimit * 2
        val relevantMessages = allMessages.takeLast(historyLimit)
        val messagesJson = relevantMessages.map { msg ->
            mapOf(
                "role" to msg.role.toStringValue(),
                "content" to msg.content.take(200) + if (msg.content.length > 200) "..." else "",
                "timestamp" to msg.createdAt
            )
        }
        
        // Build full request snapshot
        val requestSnapshot = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "model" to modelInfo,
            "parameters" to mapOf(
                "temperature" to config.temperature,
                "top_p" to config.topP,
                "max_tokens" to config.maxTokens,
                "message_history_limit" to config.messageHistoryLimit
            ),
            "flags" to mapOf(
                "deep_empathy" to config.deepEmpathy,
                "memory_enabled" to config.memoryEnabled
            ),
            "system_prompt" to actualSystemPrompt,
            "user_context" to (userContext ?: ""),
            "message_count" to mapOf(
                "total" to allMessages.size,
                "sent_to_model" to relevantMessages.size
            ),
            "messages" to messagesJson
        )
        
        // Convert to pretty JSON
        return com.google.gson.GsonBuilder()
            .setPrettyPrinting()
            .create()
            .toJson(requestSnapshot)
    }
    
    fun toggleLike(messageId: String) {
        viewModelScope.launch {
            messageRepository.toggleLike(messageId)
        }
    }
    
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }
    
    /**
     * Regenerate AI response for a given message
     * Deletes the current assistant message and generates a new one
     */
    fun regenerateMessage(messageId: String) {
        viewModelScope.launch {
            try {
                // Find the message to regenerate
                val messageToRegenerate = _uiState.value.messages.find { it.id == messageId }
                if (messageToRegenerate == null || messageToRegenerate.role != MessageRole.ASSISTANT) {
                    Log.w("ChatViewModel", "Cannot regenerate: message not found or not an assistant message")
                    return@launch
                }
                
                // Find the corresponding user message (the one immediately before this assistant message)
                val messageIndex = _uiState.value.messages.indexOfFirst { it.id == messageId }
                val userMessage = if (messageIndex > 0) {
                    _uiState.value.messages.getOrNull(messageIndex - 1)
                } else {
                    null
                }
                
                if (userMessage == null || userMessage.role != MessageRole.USER) {
                    Log.w("ChatViewModel", "Cannot regenerate: corresponding user message not found")
                    return@launch
                }
                
                // Delete both user and assistant messages
                messageRepository.deleteMessage(userMessage.id)
                messageRepository.deleteMessage(messageToRegenerate.id)
                
                // Set the user message content back to input and trigger send
                _uiState.update { it.copy(inputText = userMessage.content) }
                
                // Wait a bit for UI to update, then send
                kotlinx.coroutines.delay(100)
                sendMessage()
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error regenerating message", e)
            }
        }
    }
    
    /**
     * Retry after error: delete user message and re-send it
     */
    fun retryAfterError() {
        viewModelScope.launch {
            try {
                val errorDetails = _uiState.value.errorDetails ?: return@launch
                
                // Hide error dialog
                hideErrorDialog()
                
                // Set user message content to input
                _uiState.update { it.copy(inputText = errorDetails.userMessageContent) }
                
                // Delete user message from DB
                messageRepository.deleteMessage(errorDetails.userMessageId)
                
                // Delete assistant message only if it was created
                if (errorDetails.assistantMessageId.isNotEmpty()) {
                    messageRepository.deleteMessage(errorDetails.assistantMessageId)
                }
                
                // Wait a bit, then resend
                kotlinx.coroutines.delay(100)
                sendMessage()
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error retrying after error", e)
            }
        }
    }
    
    /**
     * Cancel after error: copy user message to clipboard and delete the pair
     */
    fun cancelAfterError(clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
        viewModelScope.launch {
            try {
                val errorDetails = _uiState.value.errorDetails ?: return@launch
                
                // Copy user message to clipboard
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(errorDetails.userMessageContent))
                
                // Delete user message from DB
                messageRepository.deleteMessage(errorDetails.userMessageId)
                
                // Delete assistant message only if it was created
                if (errorDetails.assistantMessageId.isNotEmpty()) {
                    messageRepository.deleteMessage(errorDetails.assistantMessageId)
                }
                
                // Hide error dialog
                hideErrorDialog()
                
                Log.i("ChatViewModel", "User message copied to clipboard and messages deleted")
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error canceling after error", e)
            }
        }
    }
    
    fun hideErrorDialog() {
        _uiState.update { 
            it.copy(
                showErrorDialog = false,
                errorDetails = null
            )
        }
    }
    
    fun hideModelLoadErrorDialog() {
        _uiState.update {
            it.copy(
                showModelLoadErrorDialog = false,
                modelLoadErrorMessage = null
            )
        }
    }
    
    fun showRequestLogs(logs: String) {
        _uiState.update { 
            it.copy(
                showRequestLogsDialog = true,
                selectedMessageLogs = logs
            ) 
        }
    }
    
    fun hideRequestLogs() {
        _uiState.update { 
            it.copy(
                showRequestLogsDialog = false,
                selectedMessageLogs = null
            ) 
        }
    }
    
    fun onScrolledToBottom() {
        _uiState.update { it.copy(shouldScrollToBottom = false) }
    }
    
    // Search functionality
    fun toggleSearchMode() {
        val newSearchMode = !_uiState.value.isSearchMode
        if (newSearchMode) {
            _uiState.update { 
                it.copy(
                    isSearchMode = true,
                    searchQuery = "",
                    currentSearchIndex = 0,
                    searchMatchCount = 0
                ) 
            }
        } else {
            _uiState.update { 
                it.copy(
                    isSearchMode = false,
                    searchQuery = "",
                    currentSearchIndex = 0,
                    searchMatchCount = 0
                ) 
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
    
    // System prompt functionality
    fun showSystemPromptDialog() {
        // Load current conversation's system prompt ID
        val currentPromptId = _uiState.value.currentConversation?.systemPromptId
        _uiState.update { 
            it.copy(
                showSystemPromptDialog = true,
                selectedSystemPromptId = currentPromptId
            ) 
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
                // Update conversation with selected prompt
                conversationRepository.updateConversationSystemPrompt(
                    id = conversationId,
                    systemPromptId = promptId,
                    systemPrompt = prompt.content
                )
                
                // Increment usage count
                systemPromptRepository.incrementUsageCount(promptId)
                
                _uiState.update { it.copy(selectedSystemPromptId = promptId) }
            }
            
            hideSystemPromptDialog()
        }
    }
    
    // Export chat functionality
    fun exportChat(filterByLikes: Boolean = false) {
        val conversation = _uiState.value.currentConversation
        val allMessages = _uiState.value.messages
        
        if (conversation == null || allMessages.isEmpty()) return
        
        // Filter messages by likes if requested
        val messages = if (filterByLikes) {
            allMessages.filter { it.isLiked }
        } else {
            allMessages
        }
        
        if (messages.isEmpty() && filterByLikes) {
            // Show empty state if no liked messages
            _uiState.update { 
                it.copy(
                    showExportDialog = true,
                    exportedChatText = "No liked messages to export.\n\nTip: Like messages by clicking the ❤️ icon in the message menu."
                ) 
            }
            return
        }
        
        val exportBuilder = StringBuilder()
        exportBuilder.appendLine("# Chat Export: ${conversation.title}")
        exportBuilder.appendLine()
        exportBuilder.appendLine("**Date:** ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        exportBuilder.appendLine("**Model:** ${conversation.model} (${conversation.provider})")
        if (filterByLikes) {
            exportBuilder.appendLine("**Filter:** ❤️ Liked messages only (${messages.size} messages)")
        } else {
            exportBuilder.appendLine("**Total messages:** ${messages.size}")
        }
        exportBuilder.appendLine()
        exportBuilder.appendLine("---")
        exportBuilder.appendLine()
        
        messages.forEach { message ->
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(message.createdAt))
            val role = when (message.role) {
                MessageRole.USER -> "## 👤 User"
                MessageRole.ASSISTANT -> "## 🤖 Assistant"
                MessageRole.SYSTEM -> "## ⚙️ System"
            }
            val likeIndicator = if (message.isLiked) " ❤️" else ""
            
            exportBuilder.appendLine("$role$likeIndicator")
            exportBuilder.appendLine("*$timestamp*")
            exportBuilder.appendLine()
            exportBuilder.appendLine(message.content)
            exportBuilder.appendLine()
            exportBuilder.appendLine("---")
            exportBuilder.appendLine()
        }
        
        _uiState.update { 
            it.copy(
                showExportDialog = true,
                exportedChatText = exportBuilder.toString()
            ) 
        }
    }
    
    fun hideExportDialog() {
        _uiState.update { 
            it.copy(
                showExportDialog = false,
                exportedChatText = null
            ) 
        }
    }
    
    // Import chat functionality
    fun showImportDialog() {
        _uiState.update { it.copy(showImportDialog = true, importErrorMessage = null) }
    }
    
    fun hideImportDialog() {
        _uiState.update { 
            it.copy(
                showImportDialog = false, 
                importErrorMessage = null,
                importedConversationId = null
            ) 
        }
    }
    
    fun clearImportedConversationId() {
        _uiState.update { it.copy(importedConversationId = null) }
    }
    
    fun importChat(chatText: String): String? {
        try {
            val lines = chatText.lines()
            
            // Parse title (first line starting with "# Chat Export:")
            val titleLine = lines.firstOrNull { it.startsWith("# Chat Export:") }
            val title = titleLine?.removePrefix("# Chat Export:")?.trim() ?: "Imported Chat"
            
            // Parse model and provider
            val modelLine = lines.firstOrNull { it.startsWith("**Model:**") }
            var provider: ModelProvider = OpenAIModel.GPT_4O.toModelProvider()
            var modelName = "gpt-4o"
            
            if (modelLine != null) {
                val modelPart = modelLine.removePrefix("**Model:**").trim()
                // Format: "model-name (provider)"
                val regex = """(.+?)\s*\((.+?)\)""".toRegex()
                val match = regex.find(modelPart)
                if (match != null) {
                    modelName = match.groupValues[1].trim()
                    val providerName = match.groupValues[2].trim()
                    
                    // Try to match provider
                    provider = when {
                        providerName.contains("OpenAI", ignoreCase = true) -> {
                            // Try to find matching OpenAI model, fallback to GPT_4O
                            OpenAIModel.entries.find { it.modelId == modelName || it.displayName == modelName }
                                ?.toModelProvider() ?: OpenAIModel.GPT_4O.toModelProvider()
                        }
                        providerName.contains("Deepseek", ignoreCase = true) -> {
                            // Try to find matching Deepseek model, fallback to DEEPSEEK_CHAT
                            DeepseekModel.entries.find { it.modelId == modelName || it.displayName == modelName }
                                ?.toModelProvider() ?: DeepseekModel.DEEPSEEK_CHAT.toModelProvider()
                        }
                        providerName.contains("OpenRouter", ignoreCase = true) -> {
                            // Try to find matching OpenRouter model, fallback to CLAUDE_SONNET_4_5
                            OpenRouterModel.entries.find { it.modelId == modelName || it.displayName == modelName }
                                ?.toModelProvider() ?: OpenRouterModel.CLAUDE_SONNET_4_5.toModelProvider()
                        }
                        providerName.contains("xAI", ignoreCase = true) || providerName.contains("Grok", ignoreCase = true) -> {
                            // Try to find matching xAI model, fallback to GROK_4_1_FAST_REASONING
                            XAIModel.entries.find { it.modelId == modelName || it.displayName == modelName }
                                ?.toModelProvider() ?: XAIModel.GROK_4_1_FAST_REASONING.toModelProvider()
                        }
                        else -> OpenAIModel.GPT_4O.toModelProvider()
                    }
                }
            }
            
            // Parse messages
            val messages = mutableListOf<Message>()
            var currentRole: MessageRole? = null
            var currentContent = StringBuilder()
            var currentTimestamp = System.currentTimeMillis()
            var isLiked = false
            
            for (line in lines) {
                when {
                    line.startsWith("## 👤 User") -> {
                        // Save previous message
                        if (currentRole != null && currentContent.isNotEmpty()) {
                            messages.add(
                                Message(
                                    id = UUID.randomUUID().toString(),
                                    conversationId = "", // Will be set later
                                    role = currentRole,
                                    content = currentContent.toString().trim(),
                                    createdAt = currentTimestamp,
                                    isLiked = isLiked
                                )
                            )
                        }
                        currentRole = MessageRole.USER
                        currentContent = StringBuilder()
                        isLiked = line.contains("❤️")
                    }
                    line.startsWith("## 🤖 Assistant") -> {
                        // Save previous message
                        if (currentRole != null && currentContent.isNotEmpty()) {
                            messages.add(
                                Message(
                                    id = UUID.randomUUID().toString(),
                                    conversationId = "", // Will be set later
                                    role = currentRole,
                                    content = currentContent.toString().trim(),
                                    createdAt = currentTimestamp,
                                    isLiked = isLiked
                                )
                            )
                        }
                        currentRole = MessageRole.ASSISTANT
                        currentContent = StringBuilder()
                        isLiked = line.contains("❤️")
                    }
                    line.startsWith("## ⚙️ System") -> {
                        // Save previous message
                        if (currentRole != null && currentContent.isNotEmpty()) {
                            messages.add(
                                Message(
                                    id = UUID.randomUUID().toString(),
                                    conversationId = "", // Will be set later
                                    role = currentRole,
                                    content = currentContent.toString().trim(),
                                    createdAt = currentTimestamp,
                                    isLiked = isLiked
                                )
                            )
                        }
                        currentRole = MessageRole.SYSTEM
                        currentContent = StringBuilder()
                        isLiked = line.contains("❤️")
                    }
                    line.startsWith("*") && line.endsWith("*") -> {
                        // Timestamp line - ignore
                    }
                    line == "---" || line.isBlank() -> {
                        // Separator or blank - ignore
                    }
                    else -> {
                        // Content line
                        if (currentRole != null) {
                            if (currentContent.isNotEmpty()) {
                                currentContent.append("\n")
                            }
                            currentContent.append(line)
                        }
                    }
                }
            }
            
            // Save last message
            if (currentRole != null && currentContent.isNotEmpty()) {
                messages.add(
                    Message(
                        id = UUID.randomUUID().toString(),
                        conversationId = "", // Will be set later
                        role = currentRole,
                        content = currentContent.toString().trim(),
                        createdAt = currentTimestamp,
                        isLiked = isLiked
                    )
                )
            }
            
            if (messages.isEmpty()) {
                _uiState.update { it.copy(importErrorMessage = "No messages found in the imported text") }
                return null
            }
            
            // Create new conversation
            viewModelScope.launch {
                // Get provider name correctly
                val providerName = when (provider) {
                    is ModelProvider.Local -> "local"
                    is ModelProvider.API -> {
                        when (provider.provider) {
                            com.yourown.ai.domain.model.AIProvider.OPENAI -> "OpenAI"
                            com.yourown.ai.domain.model.AIProvider.DEEPSEEK -> "Deepseek"
                            com.yourown.ai.domain.model.AIProvider.OPENROUTER -> "OpenRouter"
                            com.yourown.ai.domain.model.AIProvider.XAI -> "x.ai (Grok)"
                            com.yourown.ai.domain.model.AIProvider.CUSTOM -> "Custom"
                        }
                    }
                }
                
                // Create conversation and get ID
                val conversationId = conversationRepository.createConversation(
                    title = title,
                    systemPrompt = "", // Imported chats use default system prompt
                    model = modelName,
                    provider = providerName,
                    systemPromptId = null
                )
                
                // Insert messages with correct conversation ID
                messages.forEach { message ->
                    messageRepository.addMessage(message.copy(conversationId = conversationId))
                }
                
                Log.d("ChatViewModel", "Imported chat: $conversationId, ${messages.size} messages")
                
                // Set imported conversation ID for navigation and hide dialog
                _uiState.update { 
                    it.copy(
                        showImportDialog = false,
                        importErrorMessage = null,
                        importedConversationId = conversationId
                    )
                }
            }
            
            return title
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error importing chat", e)
            _uiState.update { it.copy(importErrorMessage = "Error parsing chat: ${e.message}") }
            return null
        }
    }
    
    /**
     * Extract memory from user message and save it
     */
    private fun extractAndSaveMemory(
        userMessage: Message,
        selectedModel: ModelProvider,
        config: AIConfig,
        conversationId: String
    ) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Starting memory extraction for message: ${userMessage.id}")
                
                // Get memory extraction prompt from config (user can customize it)
                val memoryPrompt = config.memoryExtractionPrompt
                
                // Replace {text} placeholder with user message content
                val filledPrompt = memoryPrompt.replace("{text}", userMessage.content)
                
                // Create a temporary message list with just the prompt
                val promptMessage = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = MessageRole.USER,
                    content = filledPrompt,
                    createdAt = System.currentTimeMillis()
                )
                
                // Use simple system prompt for memory extraction
                val systemPrompt = "Ты - аналитик смысла."
                
                // Call AI to extract memory
                val responseBuilder = StringBuilder()
                aiService.generateResponse(
                    provider = selectedModel,
                    messages = listOf(promptMessage),
                    systemPrompt = systemPrompt,
                    userContext = null,
                    config = config.copy(messageHistoryLimit = 1) // Don't need history for memory extraction
                ).collect { chunk ->
                    responseBuilder.append(chunk)
                }
                
                val memoryResponse = responseBuilder.toString().trim()
                Log.d("ChatViewModel", "Memory extraction response: $memoryResponse")
                
                // Parse and save memory
                val memoryEntry = com.yourown.ai.domain.model.MemoryEntry.parseFromResponse(
                    response = memoryResponse,
                    conversationId = conversationId,
                    messageId = userMessage.id
                )
                
                if (memoryEntry != null) {
                    memoryRepository.insertMemory(memoryEntry)
                    Log.i("ChatViewModel", "Memory saved: ${memoryEntry.fact}")
                } else {
                    Log.d("ChatViewModel", "No key information extracted")
                }
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error extracting memory", e)
            }
        }
    }
    
    /**
     * Build message history with context inheritance support
     * 
     * If conversation has sourceConversationId:
     * - Get last N pairs from source conversation
     * - Current messages gradually replace inherited messages as new messages are added
     * - Total pairs = messageHistoryLimit (e.g., 10 pairs = 20 messages)
     * 
     * @param currentMessages Messages from current conversation
     * @param sourceConversationId ID of conversation to inherit context from
     * @param messageHistoryLimit Total number of message pairs to include
     * @return Combined message list (inherited + current), limited by messageHistoryLimit
     */
    private suspend fun buildMessageHistoryWithInheritance(
        currentMessages: List<Message>,
        sourceConversationId: String?,
        messageHistoryLimit: Int
    ): List<Message> {
        // If no source conversation, use current messages only
        if (sourceConversationId == null) {
            return currentMessages
        }
        
        try {
            // Count current message pairs
            val currentPairs = mutableListOf<Pair<Message, Message>>()
            var i = 0
            while (i < currentMessages.size - 1) {
                val current = currentMessages[i]
                val next = currentMessages[i + 1]
                
                if (current.role.toStringValue() == "user" && next.role.toStringValue() == "assistant") {
                    currentPairs.add(Pair(current, next))
                    i += 2
                } else {
                    i += 1
                }
            }
            
            // Handle last unpaired user message
            val lastUnpairedUser = if (currentMessages.isNotEmpty() && 
                                      currentMessages.last().role.toStringValue() == "user" &&
                                      (currentPairs.isEmpty() || currentPairs.last().first.id != currentMessages.last().id)) {
                currentMessages.last()
            } else null
            
            val currentPairCount = currentPairs.size
            Log.d("ChatViewModel", "Context inheritance: current has $currentPairCount pairs, limit is $messageHistoryLimit")
            
            // Calculate how many pairs to get from source
            val neededFromSource = (messageHistoryLimit - currentPairCount).coerceAtLeast(0)
            
            if (neededFromSource == 0) {
                // Current conversation has enough pairs, use only last N pairs from current
                val messagesToUse = currentPairs.takeLast(messageHistoryLimit)
                    .flatMap { listOf(it.first, it.second) }
                return if (lastUnpairedUser != null) {
                    messagesToUse + lastUnpairedUser
                } else {
                    messagesToUse
                }
            }
            
            // Get inherited pairs from source conversation
            val inheritedMessages = messageRepository.getLastMessagePairs(
                conversationId = sourceConversationId,
                pairLimit = neededFromSource
            )
            
            Log.d("ChatViewModel", "Got ${inheritedMessages.size} inherited messages from source (${inheritedMessages.size / 2} pairs)")
            
            // Combine: inherited messages + current messages
            val combined = inheritedMessages + currentPairs.flatMap { listOf(it.first, it.second) }
            
            // Add unpaired user message if exists
            return if (lastUnpairedUser != null) {
                combined + lastUnpairedUser
            } else {
                combined
            }
            
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error building message history with inheritance", e)
            // Fallback to current messages only
            return currentMessages
        }
    }
    
    /**
     * Parse Deep Empathy focus points from JSON response
     * Returns only focus points where is_strong_focus = true
     * Expected format: {"focus_points": ["...", "..."], "is_strong_focus": [true, false]}
     */
    private fun parseDeepEmpathyFocus(jsonResponse: String): List<String> {
        return try {
            // Extract JSON from response (handle cases where model adds text before/after)
            val jsonStart = jsonResponse.indexOf("{")
            val jsonEnd = jsonResponse.lastIndexOf("}") + 1
            
            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                Log.w("ChatViewModel", "No JSON found in Deep Empathy response")
                return emptyList()
            }
            
            val jsonString = jsonResponse.substring(jsonStart, jsonEnd)
            
            // Parse focus_points array
            val focusPointsMatch = Regex(""""focus_points"\s*:\s*\[(.*?)]""").find(jsonString)
            
            if (focusPointsMatch == null) {
                Log.w("ChatViewModel", "No focus_points found in JSON")
                return emptyList()
            }
            
            val focusPointsStr = focusPointsMatch.groupValues[1]
            val points = Regex(""""([^"]+)"""").findAll(focusPointsStr)
                .map { it.groupValues[1] }
                .toList()
            
            // Parse is_strong_focus array
            val isStrongFocusMatch = Regex(""""is_strong_focus"\s*:\s*\[(.*?)]""").find(jsonString)
            
            if (isStrongFocusMatch == null) {
                Log.w("ChatViewModel", "No is_strong_focus found in JSON")
                return emptyList()
            }
            
            val isStrongFocusStr = isStrongFocusMatch.groupValues[1]
            val strongFlags = isStrongFocusStr.split(",")
                .map { it.trim().lowercase() == "true" }
            
            // Filter: return only points where is_strong_focus = true
            val strongFocusPoints = points.filterIndexed { index, _ ->
                index < strongFlags.size && strongFlags[index]
            }
            
            if (strongFocusPoints.isEmpty()) {
                Log.d("ChatViewModel", "No strong focus points found")
            } else {
                Log.d("ChatViewModel", "Deep Empathy strong focus points: $strongFocusPoints")
            }
            
            return strongFocusPoints
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error parsing Deep Empathy JSON", e)
            return emptyList()
        }
    }
}
