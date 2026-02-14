package com.yourown.ai.presentation.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.yourown.ai.data.local.preferences.SettingsManager
import com.yourown.ai.data.repository.ApiKeyRepository
import com.yourown.ai.data.repository.ConversationRepository
import com.yourown.ai.data.repository.LocalModelRepository
import com.yourown.ai.data.repository.MessageRepository
import com.yourown.ai.domain.model.*
import com.yourown.ai.domain.service.LlamaService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
    val personas: Map<String, Persona> = emptyMap(), // Map<systemPromptId, Persona>
    val selectedSystemPromptId: String? = null,
    val activePersona: Persona? = null, // Активная Persona для текущего чата
    val isLoading: Boolean = false,
    val streamingMessage: Message? = null,
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
    val selectedNewChatPersonaId: String? = null,
    val isListening: Boolean = false,
    val attachedImages: List<android.net.Uri> = emptyList(),
    val attachedFiles: List<android.net.Uri> = emptyList(),
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportProgressMessage: String = "",
    val searchStatusMessage: String? = null,
    val totalMemoriesCount: Int = 0,
    val totalDocumentsCount: Int = 0
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
    private val personaRepository: com.yourown.ai.data.repository.PersonaRepository,
    private val settingsManager: SettingsManager,
    private val llamaService: LlamaService,
    private val keyboardSoundManager: com.yourown.ai.domain.service.KeyboardSoundManager,
    private val memoryRepository: com.yourown.ai.data.repository.MemoryRepository,
    private val knowledgeDocumentRepository: com.yourown.ai.data.repository.KnowledgeDocumentRepository,
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
        observePersonas()
        initializeDefaultPrompts()
        observePinnedModels()
        observeKeyboardSoundSettings()
        observeMemoriesAndDocuments()
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
    
    private fun observePersonas() {
        viewModelScope.launch {
            personaRepository.getAllPersonas().collect { personas ->
                // Дедупликация по systemPromptId
                val uniquePersonas = personas
                    .groupBy { it.systemPromptId }
                    .mapValues { (_, duplicates) -> 
                        // Если есть дубликаты - берем самую новую (по updatedAt)
                        duplicates.maxByOrNull { it.updatedAt } ?: duplicates.first()
                    }
                    .values
                    .toList()
                
                // Создаем Map<systemPromptId, Persona> для быстрого поиска
                val personaMap = uniquePersonas.associateBy { it.systemPromptId }
                _uiState.update { it.copy(personas = personaMap) }
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
                                "OPENROUTER" -> OpenRouterModel.entries.find { it.modelId == savedModel.modelId }?.toModelProvider()
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
    
    private fun observeMemoriesAndDocuments() {
        viewModelScope.launch {
            memoryRepository.getAllMemoryEntities().let { memories ->
                _uiState.update { it.copy(totalMemoriesCount = memories.size) }
            }
        }
        viewModelScope.launch {
            knowledgeDocumentRepository.getAllDocuments().collect { documents ->
                _uiState.update { it.copy(totalDocumentsCount = documents.size) }
            }
        }
    }
    
    /**
     * Получить эффективный AIConfig:
     * - Если активна Persona - использовать её настройки
     * - Иначе - использовать глобальный конфиг
     */
    private fun getEffectiveConfig(): AIConfig {
        val activePersona = _uiState.value.activePersona
        val globalConfig = _uiState.value.aiConfig
        
        return if (activePersona != null) {
            // Создаем AIConfig из настроек Persona
            AIConfig(
                temperature = activePersona.temperature,
                topP = activePersona.topP,
                maxTokens = activePersona.maxTokens,
                deepEmpathy = activePersona.deepEmpathy,
                memoryEnabled = activePersona.memoryEnabled,
                ragEnabled = activePersona.ragEnabled,
                messageHistoryLimit = activePersona.messageHistoryLimit,
                systemPrompt = activePersona.systemPrompt,
                deepEmpathyPrompt = activePersona.deepEmpathyPrompt,
                deepEmpathyAnalysisPrompt = activePersona.deepEmpathyAnalysisPrompt,
                memoryExtractionPrompt = activePersona.memoryExtractionPrompt,
                contextInstructions = activePersona.contextInstructions,
                memoryInstructions = activePersona.memoryInstructions,
                ragInstructions = activePersona.ragInstructions,
                swipeMessagePrompt = activePersona.swipeMessagePrompt,
                memoryLimit = activePersona.memoryLimit,
                memoryMinAgeDays = activePersona.memoryMinAgeDays,
                memoryTitle = activePersona.memoryTitle,
                ragChunkSize = activePersona.ragChunkSize,
                ragChunkOverlap = activePersona.ragChunkOverlap,
                ragChunkLimit = activePersona.ragChunkLimit,
                ragTitle = activePersona.ragTitle
            )
        } else {
            globalConfig
        }
    }
    
    fun togglePinnedModel(model: ModelProvider) {
        viewModelScope.launch {
            settingsManager.togglePinnedModel(model.getModelKey())
        }
    }
    
    fun toggleWebSearch() {
        val conversationId = _uiState.value.currentConversationId ?: return
        val currentState = _uiState.value.currentConversation?.webSearchEnabled ?: false
        val newState = !currentState
        
        viewModelScope.launch {
            conversationRepository.updateWebSearchEnabled(conversationId, newState)
            
            // Show temporary banner
            val message = if (newState) {
                "Web Search enabled - searching the internet"
            } else {
                "Web Search disabled"
            }
            _uiState.update { it.copy(searchStatusMessage = message) }
        }
    }
    
    fun toggleXSearch() {
        val conversationId = _uiState.value.currentConversationId ?: return
        val currentState = _uiState.value.currentConversation?.xSearchEnabled ?: false
        val newState = !currentState
        
        viewModelScope.launch {
            conversationRepository.updateXSearchEnabled(conversationId, newState)
            
            // Show temporary banner
            val message = if (newState) {
                "𝕏 Search enabled - searching posts on X (Twitter)"
            } else {
                "𝕏 Search disabled"
            }
            _uiState.update { it.copy(searchStatusMessage = message) }
        }
    }
    
    fun clearSearchStatusMessage() {
        _uiState.update { it.copy(searchStatusMessage = null) }
    }
    
    // ===== CONVERSATION MANAGEMENT =====
    
    fun showSourceChatDialog() {
        _uiState.update { it.copy(showSourceChatDialog = true, selectedSourceChatId = null, selectedNewChatPersonaId = null) }
    }
    
    fun hideSourceChatDialog() {
        _uiState.update { it.copy(showSourceChatDialog = false, selectedSourceChatId = null, selectedNewChatPersonaId = null) }
    }
    
    fun selectSourceChat(chatId: String?) {
        _uiState.update { it.copy(selectedSourceChatId = chatId) }
    }
    
    fun selectNewChatPersona(personaId: String?) {
        _uiState.update { it.copy(selectedNewChatPersonaId = personaId) }
    }
    
    suspend fun createNewConversation(sourceConversationId: String? = null): String {
        val selectedPersonaId = _uiState.value.selectedNewChatPersonaId
        
        val id = conversationManager.createNewConversation(
            conversationCount = _uiState.value.conversations.size,
            sourceConversationId = sourceConversationId
        )
        
        // If persona selected, apply it to the new conversation
        var personaModel: ModelProvider? = null
        if (selectedPersonaId != null) {
            val persona = personaRepository.getPersonaById(selectedPersonaId)
            if (persona != null) {
                conversationManager.updateConversationWithPersona(
                    conversationId = id,
                    systemPromptId = persona.systemPromptId,
                    systemPrompt = persona.systemPrompt,
                    personaId = persona.id
                )
                Log.d("ChatViewModel", "Applied persona '${persona.name}' to new conversation")
                
                // Restore preferred model from persona if set
                if (persona.preferredModelId != null && persona.preferredProvider != null) {
                    Log.d("ChatViewModel", "Attempting to restore model: modelId=${persona.preferredModelId}, provider=${persona.preferredProvider}")
                    Log.d("ChatViewModel", "Provider string length: ${persona.preferredProvider!!.length}, exact value: '${persona.preferredProvider}'")
                    
                    personaModel = conversationManager.restoreModelFromConversation(
                        persona.preferredModelId!!,
                        persona.preferredProvider!!
                    )
                    
                    if (personaModel != null) {
                        Log.d("ChatViewModel", "Successfully restored model from persona: ${persona.preferredModelId}")
                        
                        // Save model to conversation immediately
                        val modelName = when (personaModel) {
                            is ModelProvider.Local -> personaModel.model.modelName
                            is ModelProvider.API -> personaModel.modelId
                        }
                        val providerName = when (personaModel) {
                            is ModelProvider.Local -> "local"
                            is ModelProvider.API -> personaModel.provider.displayName
                        }
                        
                        conversationManager.updateConversationModel(
                            conversationId = id,
                            modelName = modelName,
                            providerName = providerName
                        )
                        Log.d("ChatViewModel", "Saved model to conversation: model=$modelName, provider=$providerName")
                    } else {
                        Log.w("ChatViewModel", "Failed to restore model from persona: modelId=${persona.preferredModelId}, provider='${persona.preferredProvider}'")
                    }
                } else {
                    Log.d("ChatViewModel", "Persona has no preferred model: modelId=${persona.preferredModelId}, provider=${persona.preferredProvider}")
                }
            }
        }
        
        selectConversation(id)
        
        // Set model from persona if available, otherwise clear selection
        _uiState.update { it.copy(selectedModel = personaModel) }
        
        // If it's a local model, load it in background
        if (personaModel is ModelProvider.Local) {
            loadModelInBackground(personaModel.model)
        }
        
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
                        
                        // Restore active Persona from conversation
                        if (conv.personaId != null) {
                            val persona = personaRepository.getPersonaById(conv.personaId!!)
                            _uiState.update { it.copy(activePersona = persona) }
                            Log.i("ChatViewModel", "Restored Persona: ${persona?.name}")
                        } else {
                            _uiState.update { it.copy(activePersona = null) }
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
        val replyMessage = _uiState.value.replyToMessage

        viewModelScope.launch {
            // Сразу очищаем UI
            _uiState.update { state ->
                state.copy(
                    inputText = "",
                    replyToMessage = null,
                    attachedImages = emptyList(),
                    attachedFiles = emptyList(),
                    isLoading = true
                )
            }

            // 1. Создаём сообщение
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.USER,
                content = text,
                createdAt = System.currentTimeMillis(),
                swipeMessageId = replyMessage?.id,
                swipeMessageText = replyMessage?.content,
                imageAttachments = null,
                fileAttachments = null
            )

            // Объявляем переменные ДО блока try
            val aiMessageId = UUID.randomUUID().toString()

            try {
                // Process images
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
                } else null

                // Process files
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
                } else null

                // 2. Создаём ФИНАЛЬНОЕ сообщение со всеми attachments
                val finalUserMessage = userMessage.copy(
                    imageAttachments = imageAttachmentsJson,
                    fileAttachments = fileAttachmentsJson
                )

                // 3. Добавляем ОДИН РАЗ в БД - БД сама обновит UI через Flow
                messageRepository.addMessage(finalUserMessage)

                // 4. Строим историю и context
                val config = getEffectiveConfig() // Используем настройки Persona, если она активна
                val sourceConvId = _uiState.value.currentConversation?.sourceConversationId

                // Получаем все сообщения из БД напрямую (включая только что добавленное)
                val allMessagesFromDb = messageRepository.getMessagesByConversation(conversationId).first()

                val allMessages = messageHandler.buildMessageHistoryWithInheritance(
                    currentMessages = allMessagesFromDb,
                    sourceConversationId = sourceConvId,
                    messageHistoryLimit = config.messageHistoryLimit
                )

                val userContextContent = _uiState.value.userContext.content
                val activePersonaId = _uiState.value.activePersona?.id
                val activePersonaName = _uiState.value.activePersona?.name
                
                Log.d("ChatViewModel", "Building context for logs: activePersonaId=$activePersonaId, activePersonaName=$activePersonaName")

                val enhancedContextResult = messageHandler.buildEnhancedContextForLogs(
                    baseContext = userContextContent,
                    userMessage = finalUserMessage.content,
                    config = config,
                    selectedModel = selectedModel,
                    conversationId = conversationId,
                    swipeMessage = replyMessage,
                    personaId = activePersonaId
                )

                val requestLogs = messageHandler.buildRequestLogs(
                    model = selectedModel,
                    config = config,
                    allMessages = allMessages,
                    fullContext = enhancedContextResult.fullContext,
                    deepEmpathyAnalysis = enhancedContextResult.deepEmpathyAnalysis,
                    memoriesUsed = enhancedContextResult.memoriesUsed,
                    ragChunksUsed = enhancedContextResult.ragChunksUsed
                )

                // 6. Создаём структуру AI сообщения (БЕЗ сохранения в БД!)
                val modelName = when (selectedModel) {
                    is ModelProvider.Local -> selectedModel.model.modelName
                    is ModelProvider.API -> selectedModel.modelId
                }

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

                // 7. Генерируем ответ со streaming (ТОЛЬКО в UI state!)
                val responseBuilder = StringBuilder()
                
                Log.d("ChatViewModel", "Calling sendMessage with personaId=$activePersonaId")
                
                val webSearchEnabled = _uiState.value.currentConversation?.webSearchEnabled ?: false
                val xSearchEnabled = _uiState.value.currentConversation?.xSearchEnabled ?: false

                messageHandler.sendMessage(
                    userMessage = finalUserMessage,
                    selectedModel = selectedModel,
                    config = config,
                    userContext = userContextContent,
                    allMessages = allMessages,
                    swipeMessage = replyMessage,
                    personaId = activePersonaId,
                    webSearchEnabled = webSearchEnabled,
                    xSearchEnabled = xSearchEnabled
                ).collect { chunk ->
                    responseBuilder.append(chunk)

                    keyboardSoundManager.playTypingForToken(chunk)

                    // Обновляем ТОЛЬКО UI state, НЕ БД
                    val updatedMessage = aiMessage.copy(
                        content = responseBuilder.toString().trim()
                    )

                    _uiState.update { it.copy(streamingMessage = updatedMessage) }
                }

                // 8. После завершения стрима - сохраняем в БД ОДИН РАЗ
                val finalMessage = aiMessage.copy(
                    content = responseBuilder.toString().trim()
                )
                
                // Сначала сохраняем в БД (с request logs!)
                messageRepository.addMessage(finalMessage)
                
                // Даем время Flow'у из БД обновиться (обычно ~50-100ms)
                kotlinx.coroutines.delay(150)
                
                // Теперь очищаем streamingMessage - сообщение уже в БД, дубликат отфильтрован в UI
                _uiState.update { 
                    it.copy(
                        streamingMessage = null,
                        isLoading = false,
                        shouldScrollToBottom = true
                    ) 
                }

                // Извлекаем memory если включено
                if (config.memoryEnabled) {
                    val memoryPersonaId = _uiState.value.activePersona?.id
                    Log.d("ChatViewModel", "Extracting memory with personaId=$memoryPersonaId")
                    
                    messageHandler.extractAndSaveMemory(
                        userMessage = finalUserMessage,
                        selectedModel = selectedModel,
                        config = config,
                        conversationId = conversationId,
                        personaId = memoryPersonaId
                    )
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error generating response", e)
                e.printStackTrace()

                val errorModelName = when (selectedModel) {
                    is ModelProvider.Local -> selectedModel.model.modelName
                    is ModelProvider.API -> selectedModel.modelId
                }

                _uiState.update {
                    it.copy(
                        streamingMessage = null,
                        isLoading = false,
                        showErrorDialog = true,
                        errorDetails = ErrorDetails(
                            errorMessage = e.message ?: "Unknown error",
                            userMessageId = userMessage.id,
                            userMessageContent = userMessage.content,
                            assistantMessageId = "",
                            modelName = errorModelName
                        )
                    )
                }
            } finally {
                // Гарантируем что isLoading выключен (на случай неожиданных ошибок)
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
                // Проверяем, есть ли Persona, связанная с этим SystemPrompt
                val linkedPersona = personaRepository.getPersonaBySystemPromptId(promptId)
                
                Log.d("ChatViewModel", "selectSystemPrompt: promptId=$promptId, linkedPersona=${linkedPersona?.name}")
                
                if (linkedPersona != null) {
                    // Есть Persona - применяем её настройки
                    conversationManager.updateConversationWithPersona(conversationId, promptId, prompt.content, linkedPersona.id)
                    
                    // Устанавливаем активную Persona
                    _uiState.update { 
                        it.copy(
                            selectedSystemPromptId = promptId,
                            activePersona = linkedPersona
                        ) 
                    }
                    
                    Log.d("ChatViewModel", "Active persona set to: ${linkedPersona.name}")
                    
                    Log.i("ChatViewModel", "Applied Persona settings: ${linkedPersona.name}")
                } else {
                    // Нет Persona - используем обычный SystemPrompt
                    conversationManager.updateConversationSystemPrompt(conversationId, promptId, prompt.content)
                    
                    // Сбрасываем активную Persona
                    _uiState.update { 
                        it.copy(
                            selectedSystemPromptId = promptId,
                            activePersona = null
                        ) 
                    }
                }
            }
            
            hideSystemPromptDialog()
        }
    }
    
    // ===== EXPORT/IMPORT =====
    
    fun exportChat(filterByLikes: Boolean = false) {
        val conversation = _uiState.value.currentConversation
        val allMessages = _uiState.value.messages
        
        if (conversation == null || allMessages.isEmpty()) return
        
        viewModelScope.launch {
            try {
                // Filter messages if needed
                val messagesToExport = if (filterByLikes) {
                    allMessages.filter { it.isLiked }
                } else {
                    allMessages
                }
                
                if (messagesToExport.isEmpty() && filterByLikes) {
                    _uiState.update { 
                        it.copy(
                            showExportDialog = true,
                            exportedChatText = "No liked messages to export.\n\nTip: Like messages by clicking the ❤️ icon in the message menu.",
                            isExporting = false
                        ) 
                    }
                    return@launch
                }
                
                // HYBRID APPROACH: Choose method based on chat size
                // Small/medium chats (< 30): Direct export with yield
                // Large chats (>= 30): WorkManager with notification
                if (messagesToExport.size < 30) {
                    exportChatDirect(conversation, messagesToExport, filterByLikes)
                } else {
                    exportChatWithWorkManager(conversation, messagesToExport, filterByLikes)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Export failed", e)
                _uiState.update { 
                    it.copy(
                        showExportDialog = true,
                        exportedChatText = "Export failed: ${e.message}",
                        isExporting = false
                    ) 
                }
            }
        }
    }
    
    /**
     * Direct export for small/medium chats (< 1000 messages)
     */
    private suspend fun exportChatDirect(
        conversation: Conversation,
        messages: List<Message>,
        filterByLikes: Boolean
    ) = withContext(Dispatchers.Default) {
        try {
            // Show spinner
            withContext(Dispatchers.Main) {
                _uiState.update { 
                    it.copy(
                        isExporting = true, 
                        exportProgressMessage = "Exporting ${messages.size} messages..."
                    ) 
                }
            }
            
            android.util.Log.d("ChatViewModel", "Direct export of ${messages.size} messages")
            val startTime = System.currentTimeMillis()
            
            // Export with yields
            val exportedText = importExportManager.exportChatWithProgress(
                conversation = conversation,
                messages = messages,
                filterByLikes = filterByLikes,
                onProgress = { _, _ -> } // No-op: UI updates are too expensive
            )
            
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("ChatViewModel", "Direct export completed in ${duration}ms")
            
            // Show result
            withContext(Dispatchers.Main) {
                _uiState.update { 
                    it.copy(
                        showExportDialog = true, 
                        exportedChatText = exportedText,
                        isExporting = false
                    ) 
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Direct export failed", e)
            withContext(Dispatchers.Main) {
                _uiState.update { 
                    it.copy(
                        showExportDialog = true,
                        exportedChatText = "Export failed: ${e.message}",
                        isExporting = false
                    ) 
                }
            }
        }
    }
    
    /**
     * WorkManager export for large chats (>= 1000 messages)
     * Shows notification, works in real background
     */
    private suspend fun exportChatWithWorkManager(
        conversation: Conversation,
        messages: List<Message>,
        filterByLikes: Boolean
    ) {
        try {
            android.util.Log.d("ChatViewModel", "WorkManager export of ${messages.size} messages")
            
            // Prepare output file
            val exportDir = File(context.getExternalFilesDir(null), "exports")
            exportDir.mkdirs()
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "chat_export_${conversation.id}_$timestamp.md"
            val outputFile = File(exportDir, fileName)
            
            // Create WorkManager request
            val inputData = Data.Builder()
                .putString(ExportChatWorker.KEY_CONVERSATION_ID, conversation.id)
                .putBoolean(ExportChatWorker.KEY_FILTER_LIKES, filterByLikes)
                .putString(ExportChatWorker.KEY_OUTPUT_FILE, outputFile.absolutePath)
                .putInt(ExportChatWorker.KEY_TOTAL_MESSAGES, messages.size)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<ExportChatWorker>()
                .setInputData(inputData)
                .build()
            
            val workManager = WorkManager.getInstance(context)
            workManager.enqueue(workRequest)
            
            // Show notification info
            withContext(Dispatchers.Main) {
                _uiState.update { 
                    it.copy(
                        showExportDialog = true,
                        exportedChatText = """
                            Exporting ${messages.size} messages in background...
                            
                            📱 Check your notification panel for progress
                            ✅ You can close the app - export will continue
                            📁 File will be saved to: ${outputFile.name}
                            
                            The exported file will open automatically when done.
                        """.trimIndent(),
                        isExporting = false
                    )
                }
            }
            
            // Observe work progress
            workManager.getWorkInfoByIdLiveData(workRequest.id).observeForever { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val exportedFilePath = workInfo.outputData.getString(ExportChatWorker.KEY_OUTPUT_FILE)
                            android.util.Log.d("ChatViewModel", "WorkManager export succeeded: $exportedFilePath")
                            
                            // Load exported file and show in dialog
                            viewModelScope.launch {
                                try {
                                    val exportedText = File(exportedFilePath ?: return@launch).readText()
                                    _uiState.update { 
                                        it.copy(
                                            showExportDialog = true,
                                            exportedChatText = exportedText,
                                            isExporting = false
                                        )
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ChatViewModel", "Failed to read exported file", e)
                                }
                            }
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString("error") ?: "Unknown error"
                            android.util.Log.e("ChatViewModel", "WorkManager export failed: $error")
                            
                            viewModelScope.launch {
                                _uiState.update { 
                                    it.copy(
                                        showExportDialog = true,
                                        exportedChatText = "Export failed: $error",
                                        isExporting = false
                                    )
                                }
                            }
                        }
                        else -> {
                            // In progress...
                            android.util.Log.d("ChatViewModel", "WorkManager export state: ${workInfo.state}")
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "WorkManager export setup failed", e)
            withContext(Dispatchers.Main) {
                _uiState.update { 
                    it.copy(
                        showExportDialog = true,
                        exportedChatText = "Export setup failed: ${e.message}",
                        isExporting = false
                    )
                }
            }
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
