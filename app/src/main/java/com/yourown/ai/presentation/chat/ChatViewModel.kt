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
    val isLoading: Boolean = false,
    val shouldScrollToBottom: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val showEditTitleDialog: Boolean = false,
    val showRequestLogsDialog: Boolean = false,
    val selectedMessageLogs: String? = null,
    val inputText: String = "",
    val isInitialConversationsLoad: Boolean = true
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val localModelRepository: LocalModelRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val aiConfigRepository: com.yourown.ai.data.repository.AIConfigRepository,
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
        
        // Add downloaded local models
        _uiState.value.localModels.forEach { (model, info) ->
            if (info.status is DownloadStatus.Downloaded) {
                models.add(ModelProvider.Local(model))
            }
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
        
        // TODO: Add Anthropic
        
        _uiState.update { it.copy(availableModels = models) }
    }
    
    private fun observeSettings() {
        viewModelScope.launch {
            // Observe AI config from repository
            aiConfigRepository.aiConfig.collect { config ->
                _uiState.update { it.copy(aiConfig = config) }
            }
        }
    }
    
    // Conversation Management
    
    suspend fun createNewConversation(): String {
        val count = _uiState.value.conversations.size + 1
        val title = "Chat $count"
        
        val modelName = when (val model = _uiState.value.selectedModel) {
            is ModelProvider.Local -> model.model.modelName
            is ModelProvider.API -> model.modelId
            null -> "unknown"
        }
        
        val provider = when (val model = _uiState.value.selectedModel) {
            is ModelProvider.Local -> "local"
            is ModelProvider.API -> model.provider.displayName
            null -> "unknown"
        }
        
        val id = conversationRepository.createConversation(
            title = title,
            systemPrompt = _uiState.value.aiConfig.systemPrompt,
            model = modelName,
            provider = provider
        )
        
        selectConversation(id)
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
                        val restoredModel = restoreModelFromConversation(conv.model, conv.provider)
                        if (restoredModel != null) {
                            _uiState.update { it.copy(selectedModel = restoredModel) }
                            // Load local model if needed
                            if (restoredModel is ModelProvider.Local) {
                                loadModelInBackground(restoredModel.model)
                        }
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
                // TODO: Show error to user
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
    
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        
        val conversationId = _uiState.value.currentConversationId ?: return
        val selectedModel = _uiState.value.selectedModel ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, inputText = "") }
            
            try {
                // Create user message
                val userMessage = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = MessageRole.USER,
                    content = text,
                    createdAt = System.currentTimeMillis()
                )
                
                messageRepository.addMessage(userMessage)
                
                // Prepare AI config from settings
                val config = _uiState.value.aiConfig
                
                // Get conversation history
                val allMessages = _uiState.value.messages + userMessage
                
                // Build request logs with full context
                val requestLogs = buildRequestLogs(
                    model = selectedModel,
                    config = config,
                    messageCount = allMessages.size,
                    allMessages = allMessages,
                    userContext = null // TODO: Load from settings
                )
                
                // Get model name for logging
                val modelName = when (selectedModel) {
                    is ModelProvider.Local -> selectedModel.model.modelName
                    is ModelProvider.API -> selectedModel.modelId
                }
                
                // Create placeholder for AI response
                val aiMessageId = UUID.randomUUID().toString()
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
                    userContext = null, // TODO: Load from settings
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
                
                // Trigger scroll to bottom after streaming completes
                _uiState.update { it.copy(shouldScrollToBottom = true) }
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error generating response", e)
                
                // Get model name for error message
                val modelName = when (selectedModel) {
                    is ModelProvider.Local -> selectedModel.model.modelName
                    is ModelProvider.API -> selectedModel.modelId
                }
                
                // Add error message
                val errorMessage = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = "Error: ${e.message}",
                    createdAt = System.currentTimeMillis(),
                    isError = true,
                    errorMessage = e.message,
                    model = modelName
                )
                messageRepository.addMessage(errorMessage)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
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
    
    fun regenerateMessage(messageId: String) {
        viewModelScope.launch {
            // TODO: Implement regeneration
            // 1. Get message
            // 2. Delete it
            // 3. Generate new response
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
}
