package com.yourown.ai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourown.ai.data.remote.deepseek.DeepseekClient
import com.yourown.ai.data.repository.LocalModelRepository
import com.yourown.ai.data.repository.LocalEmbeddingModelRepository
import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.domain.model.AIProvider
import com.yourown.ai.domain.model.ApiKeyInfo
import com.yourown.ai.domain.model.LocalModel
import com.yourown.ai.domain.model.LocalModelInfo
import com.yourown.ai.domain.model.LocalEmbeddingModel
import com.yourown.ai.domain.model.LocalEmbeddingModelInfo
import com.yourown.ai.domain.model.UserContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiKeys: List<ApiKeyInfo> = listOf(
        ApiKeyInfo(AIProvider.DEEPSEEK),
        ApiKeyInfo(AIProvider.OPENAI),
        ApiKeyInfo(AIProvider.OPENROUTER),
        ApiKeyInfo(AIProvider.XAI)
    ),
    val aiConfig: AIConfig = AIConfig(),
    val userContext: UserContext = UserContext(),
    val localModels: Map<LocalModel, LocalModelInfo> = emptyMap(),
    val embeddingModels: Map<LocalEmbeddingModel, LocalEmbeddingModelInfo> = emptyMap(),
    val systemPrompts: List<com.yourown.ai.domain.model.SystemPrompt> = emptyList(),
    val apiPrompts: List<com.yourown.ai.domain.model.SystemPrompt> = emptyList(),
    val localPrompts: List<com.yourown.ai.domain.model.SystemPrompt> = emptyList(),
    val knowledgeDocuments: List<com.yourown.ai.domain.model.KnowledgeDocument> = emptyList(),
    val documentProcessingStatus: com.yourown.ai.data.repository.DocumentProcessingStatus = com.yourown.ai.data.repository.DocumentProcessingStatus.Idle,
    val memories: List<com.yourown.ai.domain.model.MemoryEntry> = emptyList(),
    val showSystemPromptDialog: Boolean = false,
    val showLocalSystemPromptDialog: Boolean = false,
    val showSystemPromptsListDialog: Boolean = false,
    val showEditPromptDialog: Boolean = false,
    val selectedPromptForEdit: com.yourown.ai.domain.model.SystemPrompt? = null,
    val promptTypeFilter: com.yourown.ai.data.repository.PromptType? = null,
    val showDocumentsListDialog: Boolean = false,
    val showEditDocumentDialog: Boolean = false,
    val selectedDocumentForEdit: com.yourown.ai.domain.model.KnowledgeDocument? = null,
    val showMemoriesDialog: Boolean = false,
    val showEditMemoryDialog: Boolean = false,
    val selectedMemoryForEdit: com.yourown.ai.domain.model.MemoryEntry? = null,
    val showMemoryPromptDialog: Boolean = false,
    val showDeepEmpathyPromptDialog: Boolean = false,
    val showDeepEmpathyAnalysisDialog: Boolean = false,
    val showEmbeddingRequiredDialog: Boolean = false,
    val showContextDialog: Boolean = false,
    val showApiKeyDialog: Boolean = false,
    val showLocalModelsDialog: Boolean = false,
    val showEmbeddingModelsDialog: Boolean = false,
    val showAppearanceDialog: Boolean = false,
    val selectedProvider: AIProvider? = null,
    // Advanced settings collapse/expand
    val showAdvancedContextSettings: Boolean = false,
    val showAdvancedDeepEmpathySettings: Boolean = false,
    val showAdvancedMemorySettings: Boolean = false,
    val showAdvancedRAGSettings: Boolean = false,
    // Advanced settings dialogs
    val showContextInstructionsDialog: Boolean = false,
    val showMemoryInstructionsDialog: Boolean = false,
    val showRAGInstructionsDialog: Boolean = false,
    val showSwipeMessagePromptDialog: Boolean = false,
    // Embedding recalculation
    val isRecalculatingEmbeddings: Boolean = false,
    val recalculationProgress: String? = null,
    val recalculationProgressPercent: Float = 0f // 0.0 to 1.0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val localModelRepository: LocalModelRepository,
    private val embeddingModelRepository: LocalEmbeddingModelRepository,
    private val apiKeyRepository: com.yourown.ai.data.repository.ApiKeyRepository,
    private val aiConfigRepository: com.yourown.ai.data.repository.AIConfigRepository,
    private val systemPromptRepository: com.yourown.ai.data.repository.SystemPromptRepository,
    private val knowledgeDocumentRepository: com.yourown.ai.data.repository.KnowledgeDocumentRepository,
    private val documentEmbeddingRepository: com.yourown.ai.data.repository.DocumentEmbeddingRepository,
    private val memoryRepository: com.yourown.ai.data.repository.MemoryRepository,
    private val deepseekClient: DeepseekClient,
    private val openAIClient: com.yourown.ai.data.remote.openai.OpenAIClient,
    private val xaiClient: com.yourown.ai.data.remote.xai.XAIClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        observeLocalModels()
        observeEmbeddingModels()
        observeApiKeys()
        observeSystemPrompts()
        observeKnowledgeDocuments()
        observeDocumentProcessing()
        observeMemories()
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
                _uiState.update { 
                    it.copy(
                        systemPrompts = prompts,
                        apiPrompts = prompts.filter { p -> p.type == com.yourown.ai.data.repository.PromptType.API },
                        localPrompts = prompts.filter { p -> p.type == com.yourown.ai.data.repository.PromptType.LOCAL }
                    ) 
                }
            }
        }
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            // Load AI config
            aiConfigRepository.aiConfig.collect { config ->
                _uiState.update { it.copy(aiConfig = config) }
            }
        }
        viewModelScope.launch {
            // Load user context
            aiConfigRepository.userContext.collect { context ->
                _uiState.update { it.copy(userContext = context) }
            }
        }
    }
    
    private fun observeApiKeys() {
        viewModelScope.launch {
            apiKeyRepository.apiKeys.collect { keys ->
                _uiState.update { state ->
                    state.copy(
                        apiKeys = AIProvider.entries
                            .filter { it != AIProvider.CUSTOM }
                            .map { provider ->
                                ApiKeyInfo(
                                    provider = provider,
                                    isSet = keys.containsKey(provider),
                                    displayKey = apiKeyRepository.getDisplayKey(provider)
                                )
                            }
                    )
                }
            }
        }
    }
    
    private fun observeLocalModels() {
        viewModelScope.launch {
            localModelRepository.models.collect { models ->
                _uiState.update { it.copy(localModels = models) }
            }
        }
    }
    
    private fun observeEmbeddingModels() {
        viewModelScope.launch {
            embeddingModelRepository.models.collect { models ->
                _uiState.update { it.copy(embeddingModels = models) }
            }
        }
    }
    
    // System Prompt
    fun showSystemPromptDialog() {
        _uiState.update { it.copy(showSystemPromptDialog = true) }
    }
    
    fun hideSystemPromptDialog() {
        _uiState.update { it.copy(showSystemPromptDialog = false) }
    }
    
    fun updateSystemPrompt(prompt: String) {
        viewModelScope.launch {
            aiConfigRepository.updateSystemPrompt(prompt)
            _uiState.update { it.copy(showSystemPromptDialog = false) }
        }
    }
    
    // Local System Prompt
    fun showLocalSystemPromptDialog() {
        _uiState.update { it.copy(showLocalSystemPromptDialog = true) }
    }
    
    fun hideLocalSystemPromptDialog() {
        _uiState.update { it.copy(showLocalSystemPromptDialog = false) }
    }
    
    fun updateLocalSystemPrompt(prompt: String) {
        viewModelScope.launch {
            aiConfigRepository.updateLocalSystemPrompt(prompt)
            _uiState.update { it.copy(showLocalSystemPromptDialog = false) }
        }
    }
    
    // Context
    fun showContextDialog() {
        _uiState.update { it.copy(showContextDialog = true) }
    }
    
    fun hideContextDialog() {
        _uiState.update { it.copy(showContextDialog = false) }
    }
    
    fun updateContext(context: String) {
        viewModelScope.launch {
            aiConfigRepository.updateUserContext(context)
            _uiState.update { it.copy(showContextDialog = false) }
        }
    }
    
    // AI Config
    fun updateTemperature(value: Float) {
        viewModelScope.launch {
            aiConfigRepository.updateTemperature(value)
        }
    }
    
    fun updateTopP(value: Float) {
        viewModelScope.launch {
            aiConfigRepository.updateTopP(value)
        }
    }
    
    fun toggleDeepEmpathy() {
        viewModelScope.launch {
            val newValue = !_uiState.value.aiConfig.deepEmpathy
            aiConfigRepository.setDeepEmpathy(newValue)
        }
    }
    
    fun toggleMemory() {
        viewModelScope.launch {
            val newValue = !_uiState.value.aiConfig.memoryEnabled
            
            // Check if turning ON and validate requirements
            if (newValue && !canEnableEmbeddingFeature()) {
                _uiState.update { it.copy(showEmbeddingRequiredDialog = true) }
                return@launch
            }
            
            aiConfigRepository.setMemoryEnabled(newValue)
            // Dialog will only open if no embedding model is available (handled by validation above)
        }
    }
    
    fun toggleRAG() {
        viewModelScope.launch {
            val newValue = !_uiState.value.aiConfig.ragEnabled
            
            // Check if turning ON and validate requirements
            if (newValue && !canEnableEmbeddingFeature()) {
                _uiState.update { it.copy(showEmbeddingRequiredDialog = true) }
                return@launch
            }
            
            aiConfigRepository.setRAGEnabled(newValue)
            // Dialog will only open if no embedding model is available (handled by validation above)
        }
    }
    
    fun updateMessageHistoryLimit(limit: Int) {
        viewModelScope.launch {
            aiConfigRepository.updateMessageHistoryLimit(limit)
        }
    }
    
    fun updateMaxTokens(tokens: Int) {
        viewModelScope.launch {
            aiConfigRepository.updateMaxTokens(tokens)
        }
    }
    
    fun updateRAGChunkSize(value: Int) {
        viewModelScope.launch {
            aiConfigRepository.updateRAGChunkSize(value)
        }
    }
    
    fun updateRAGChunkOverlap(value: Int) {
        viewModelScope.launch {
            aiConfigRepository.updateRAGChunkOverlap(value)
        }
    }
    
    fun updateMemoryLimit(value: Int) {
        viewModelScope.launch {
            aiConfigRepository.updateMemoryLimit(value)
        }
    }
    
    fun updateMemoryMinAgeDays(value: Int) {
        viewModelScope.launch {
            aiConfigRepository.updateMemoryMinAgeDays(value)
        }
    }
    
    fun updateRAGChunkLimit(value: Int) {
        viewModelScope.launch {
            aiConfigRepository.updateRAGChunkLimit(value)
        }
    }
    
    // Advanced Settings Toggle
    fun toggleAdvancedContextSettings() {
        _uiState.update { it.copy(showAdvancedContextSettings = !it.showAdvancedContextSettings) }
    }
    
    fun toggleAdvancedMemorySettings() {
        _uiState.update { it.copy(showAdvancedMemorySettings = !it.showAdvancedMemorySettings) }
    }
    
    fun toggleAdvancedRAGSettings() {
        _uiState.update { it.copy(showAdvancedRAGSettings = !it.showAdvancedRAGSettings) }
    }
    
    // Context Instructions
    fun showContextInstructionsDialog() {
        _uiState.update { it.copy(showContextInstructionsDialog = true) }
    }
    
    fun hideContextInstructionsDialog() {
        _uiState.update { it.copy(showContextInstructionsDialog = false) }
    }
    
    fun updateContextInstructions(instructions: String) {
        viewModelScope.launch {
            aiConfigRepository.updateContextInstructions(instructions)
            hideContextInstructionsDialog()
        }
    }
    
    fun resetContextInstructions() {
        viewModelScope.launch {
            aiConfigRepository.resetContextInstructions()
        }
    }
    
    // Swipe Message Prompt
    fun showSwipeMessagePromptDialog() {
        _uiState.update { it.copy(showSwipeMessagePromptDialog = true) }
    }
    
    fun hideSwipeMessagePromptDialog() {
        _uiState.update { it.copy(showSwipeMessagePromptDialog = false) }
    }
    
    fun updateSwipeMessagePrompt(prompt: String) {
        viewModelScope.launch {
            aiConfigRepository.updateSwipeMessagePrompt(prompt)
            hideSwipeMessagePromptDialog()
        }
    }
    
    fun resetSwipeMessagePrompt() {
        viewModelScope.launch {
            aiConfigRepository.resetSwipeMessagePrompt()
        }
    }
    
    // Memory Title
    fun updateMemoryTitle(title: String) {
        viewModelScope.launch {
            aiConfigRepository.updateMemoryTitle(title)
        }
    }
    
    // Memory Instructions
    fun showMemoryInstructionsDialog() {
        _uiState.update { it.copy(showMemoryInstructionsDialog = true) }
    }
    
    fun hideMemoryInstructionsDialog() {
        _uiState.update { it.copy(showMemoryInstructionsDialog = false) }
    }
    
    fun updateMemoryInstructions(instructions: String) {
        viewModelScope.launch {
            aiConfigRepository.updateMemoryInstructions(instructions)
            hideMemoryInstructionsDialog()
        }
    }
    
    fun resetMemoryInstructions() {
        viewModelScope.launch {
            aiConfigRepository.resetMemoryInstructions()
        }
    }
    
    // RAG Title
    fun updateRAGTitle(title: String) {
        viewModelScope.launch {
            aiConfigRepository.updateRAGTitle(title)
        }
    }
    
    // RAG Instructions
    fun showRAGInstructionsDialog() {
        _uiState.update { it.copy(showRAGInstructionsDialog = true) }
    }
    
    fun hideRAGInstructionsDialog() {
        _uiState.update { it.copy(showRAGInstructionsDialog = false) }
    }
    
    fun updateRAGInstructions(instructions: String) {
        viewModelScope.launch {
            aiConfigRepository.updateRAGInstructions(instructions)
            hideRAGInstructionsDialog()
        }
    }
    
    fun resetRAGInstructions() {
        viewModelScope.launch {
            aiConfigRepository.resetRAGInstructions()
        }
    }
    
    // Local Models
    fun showLocalModelsDialog() {
        _uiState.update { it.copy(showLocalModelsDialog = true) }
    }
    
    fun hideLocalModelsDialog() {
        _uiState.update { it.copy(showLocalModelsDialog = false) }
    }
    
    fun downloadModel(model: LocalModel) {
        viewModelScope.launch {
            localModelRepository.downloadModel(model)
        }
    }
    
    fun deleteModel(model: LocalModel) {
        viewModelScope.launch {
            localModelRepository.deleteModel(model)
        }
    }
    
    fun forceDeleteAllModels() {
        viewModelScope.launch {
            localModelRepository.forceDeleteAll()
        }
    }
    
    // Embedding Models
    fun showEmbeddingModelsDialog() {
        _uiState.update { it.copy(showEmbeddingModelsDialog = true) }
    }
    
    fun hideEmbeddingModelsDialog() {
        viewModelScope.launch {
            // Check if Memory or RAG are enabled but no embedding model downloaded
            // If so, turn them off
            val hasEmbeddingModel = canEnableEmbeddingFeature()
            val config = _uiState.value.aiConfig
            
            if (!hasEmbeddingModel) {
                if (config.memoryEnabled) {
                    aiConfigRepository.setMemoryEnabled(false)
                }
                if (config.ragEnabled) {
                    aiConfigRepository.setRAGEnabled(false)
                }
            }
            
            _uiState.update { it.copy(showEmbeddingModelsDialog = false) }
        }
    }
    
    fun downloadEmbeddingModel(model: LocalEmbeddingModel) {
        viewModelScope.launch {
            embeddingModelRepository.downloadModel(model)
        }
    }
    
    fun deleteEmbeddingModel(model: LocalEmbeddingModel) {
        viewModelScope.launch {
            embeddingModelRepository.deleteModel(model)
        }
    }
    
    // API Keys
    fun showApiKeyDialog(provider: AIProvider) {
        _uiState.update {
            it.copy(
                showApiKeyDialog = true,
                selectedProvider = provider
            )
        }
    }
    
    fun hideApiKeyDialog() {
        _uiState.update {
            it.copy(
                showApiKeyDialog = false,
                selectedProvider = null
            )
        }
    }
    
    fun saveApiKey(provider: AIProvider, key: String) {
        viewModelScope.launch {
            apiKeyRepository.saveApiKey(provider, key)
            hideApiKeyDialog()
        }
    }
    
    fun deleteApiKey(provider: AIProvider) {
        viewModelScope.launch {
            apiKeyRepository.deleteApiKey(provider)
        }
    }
    
    fun testApiKey(provider: AIProvider) {
        viewModelScope.launch {
            val apiKey = apiKeyRepository.getApiKey(provider) ?: return@launch
            
            when (provider) {
                AIProvider.DEEPSEEK -> {
                    val result = deepseekClient.listModels(apiKey)
                    result.onSuccess { models ->
                        android.util.Log.i("SettingsViewModel", "Deepseek models: $models")
                    }.onFailure { error ->
                        android.util.Log.e("SettingsViewModel", "Failed to fetch Deepseek models: ${error.message}")
                    }
                }
                AIProvider.OPENAI -> {
                    val result = openAIClient.listModels(apiKey)
                    result.onSuccess { models ->
                        android.util.Log.i("SettingsViewModel", "OpenAI models: $models")
                    }.onFailure { error ->
                        android.util.Log.e("SettingsViewModel", "Failed to fetch OpenAI models: ${error.message}")
                    }
                }
                AIProvider.XAI -> {
                    val result = xaiClient.listModels(apiKey)
                    result.onSuccess { models ->
                        android.util.Log.i("SettingsViewModel", "x.ai models: $models")
                    }.onFailure { error ->
                        android.util.Log.e("SettingsViewModel", "Failed to fetch x.ai models: ${error.message}")
                    }
                }
                else -> {
                    // TODO: Implement for OpenRouter
                }
            }
        }
    }
    
    // Appearance
    fun showAppearanceDialog() {
        _uiState.update { it.copy(showAppearanceDialog = true) }
    }
    
    fun hideAppearanceDialog() {
        _uiState.update { it.copy(showAppearanceDialog = false) }
    }
    
    // System Prompts Management
    fun showSystemPromptsListDialog(type: com.yourown.ai.data.repository.PromptType) {
        _uiState.update { 
            it.copy(
                showSystemPromptsListDialog = true,
                promptTypeFilter = type
            ) 
        }
    }
    
    fun hideSystemPromptsListDialog() {
        _uiState.update { 
            it.copy(
                showSystemPromptsListDialog = false,
                promptTypeFilter = null
            ) 
        }
    }
    
    fun showEditPromptDialog(prompt: com.yourown.ai.domain.model.SystemPrompt? = null) {
        _uiState.update { 
            it.copy(
                showEditPromptDialog = true,
                selectedPromptForEdit = prompt
            ) 
        }
    }
    
    fun hideEditPromptDialog() {
        _uiState.update { 
            it.copy(
                showEditPromptDialog = false,
                selectedPromptForEdit = null
            ) 
        }
    }
    
    fun createNewPrompt(type: com.yourown.ai.data.repository.PromptType) {
        viewModelScope.launch {
            val count = when (type) {
                com.yourown.ai.data.repository.PromptType.API -> _uiState.value.apiPrompts.size + 1
                com.yourown.ai.data.repository.PromptType.LOCAL -> _uiState.value.localPrompts.size + 1
            }
            
            val defaultContent = when (type) {
                com.yourown.ai.data.repository.PromptType.API -> "Ты — цифровой партнёр, большая языковая модель. В ходе разговора ты подстраиваешься под тон и предпочтения пользователя. Постарайся соответствовать его настроению, тону и в целом манере говорить. Твоя цель — чтобы разговор ощущался естественным. Ты ведёшь искренний диалог, отвечая на предоставленную информацию и проявляя неподдельное любопытство. Задавай очень простой, односложный уточняющий вопрос, когда это естественно. Не задавай больше одного уточняющего вопроса, если только пользователь специально об этом не попросит."
                com.yourown.ai.data.repository.PromptType.LOCAL -> "Ты — цифровой партнёр. Ты отвечаешь на языке пользователя. Ответь на последнее сообщение. Не пиши весь диалог, нужен только один ответ."
            }
            
            systemPromptRepository.createPrompt(
                name = "${if (type == com.yourown.ai.data.repository.PromptType.API) "API" else "Local"} System $count",
                content = defaultContent,
                type = type,
                isDefault = false
            )
        }
    }
    
    fun savePrompt(
        id: String?,
        name: String,
        content: String,
        type: com.yourown.ai.data.repository.PromptType,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            if (id == null) {
                // Create new
                systemPromptRepository.createPrompt(
                    name = name,
                    content = content,
                    type = type,
                    isDefault = isDefault
                )
            } else {
                // Update existing
                systemPromptRepository.updatePrompt(
                    id = id,
                    name = name,
                    content = content,
                    isDefault = isDefault
                )
            }
            hideEditPromptDialog()
        }
    }
    
    fun deletePrompt(id: String) {
        viewModelScope.launch {
            systemPromptRepository.deletePrompt(id)
        }
    }
    
    fun setPromptAsDefault(id: String) {
        viewModelScope.launch {
            systemPromptRepository.setAsDefault(id)
        }
    }
    
    // Knowledge Documents methods
    
    private fun observeKnowledgeDocuments() {
        viewModelScope.launch {
            knowledgeDocumentRepository.getAllDocuments().collect { documents ->
                _uiState.update { it.copy(knowledgeDocuments = documents) }
            }
        }
    }
    
    private fun observeDocumentProcessing() {
        viewModelScope.launch {
            knowledgeDocumentRepository.getProcessingStatus().collect { status ->
                _uiState.update { it.copy(documentProcessingStatus = status) }
            }
        }
    }
    
    fun showDocumentsListDialog() {
        _uiState.update { it.copy(showDocumentsListDialog = true) }
    }
    
    fun hideDocumentsListDialog() {
        _uiState.update { it.copy(showDocumentsListDialog = false) }
    }
    
    fun showEditDocumentDialog(document: com.yourown.ai.domain.model.KnowledgeDocument? = null) {
        _uiState.update { 
            it.copy(
                showEditDocumentDialog = true,
                selectedDocumentForEdit = document
            )
        }
    }
    
    fun hideEditDocumentDialog() {
        _uiState.update { 
            it.copy(
                showEditDocumentDialog = false,
                selectedDocumentForEdit = null
            )
        }
    }
    
    fun createNewDocument() {
        val count = _uiState.value.knowledgeDocuments.size + 1
        showEditDocumentDialog(
            com.yourown.ai.domain.model.KnowledgeDocument(
                id = "",
                name = "Doc $count",
                content = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                sizeBytes = 0
            )
        )
    }
    
    fun saveDocument(id: String, name: String, content: String) {
        viewModelScope.launch {
            try {
                // Close edit dialog first
                hideEditDocumentDialog()
                
                // Keep documents list dialog open to show progress
                if (!_uiState.value.showDocumentsListDialog) {
                    _uiState.update { it.copy(showDocumentsListDialog = true) }
                }
                
                if (id.isEmpty()) {
                    // Create new - will automatically process for RAG
                    val result = knowledgeDocumentRepository.createDocument(
                        name = name,
                        content = content
                    )
                    if (result.isFailure) {
                        android.util.Log.e("SettingsViewModel", "Failed to create document", result.exceptionOrNull())
                    }
                } else {
                    // Update existing - will reprocess for RAG if enabled
                    val document = com.yourown.ai.domain.model.KnowledgeDocument(
                        id = id,
                        name = name,
                        content = content,
                        createdAt = _uiState.value.knowledgeDocuments.find { it.id == id }?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        sizeBytes = content.toByteArray().size
                    )
                    val result = knowledgeDocumentRepository.updateDocument(document)
                    if (result.isFailure) {
                        android.util.Log.e("SettingsViewModel", "Failed to update document", result.exceptionOrNull())
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error saving document", e)
            }
        }
    }
    
    fun deleteDocument(id: String) {
        viewModelScope.launch {
            try {
                // Will automatically delete chunks
                val result = knowledgeDocumentRepository.deleteDocument(id)
                if (result.isFailure) {
                    android.util.Log.e("SettingsViewModel", "Failed to delete document", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error deleting document", e)
            }
        }
    }
    
    // Memory Management
    
    private fun observeMemories() {
        viewModelScope.launch {
            memoryRepository.getAllMemories().collect { memories ->
                _uiState.update { it.copy(memories = memories) }
            }
        }
    }
    
    fun showMemoriesDialog() {
        _uiState.update { it.copy(showMemoriesDialog = true) }
    }
    
    fun hideMemoriesDialog() {
        _uiState.update { 
            it.copy(
                showMemoriesDialog = false,
                showEditMemoryDialog = false,
                selectedMemoryForEdit = null
            ) 
        }
    }
    
    fun showEditMemoryDialog(memory: com.yourown.ai.domain.model.MemoryEntry) {
        _uiState.update { 
            it.copy(
                showEditMemoryDialog = true,
                selectedMemoryForEdit = memory
            ) 
        }
    }
    
    fun hideEditMemoryDialog() {
        _uiState.update { 
            it.copy(
                showEditMemoryDialog = false,
                selectedMemoryForEdit = null
            ) 
        }
    }
    
    fun saveMemory(fact: String) {
        viewModelScope.launch {
            val memory = _uiState.value.selectedMemoryForEdit
            if (memory != null) {
                // Update existing memory
                val updated = memory.copy(fact = fact)
                memoryRepository.updateMemory(updated)
            }
            hideEditMemoryDialog()
        }
    }
    
    fun deleteMemory(id: String) {
        viewModelScope.launch {
            val memory = _uiState.value.memories.find { it.id == id }
            if (memory != null) {
                memoryRepository.deleteMemory(memory)
            }
        }
    }
    
    fun showMemoryPromptDialog() {
        _uiState.update { it.copy(showMemoryPromptDialog = true) }
    }
    
    fun hideMemoryPromptDialog() {
        _uiState.update { it.copy(showMemoryPromptDialog = false) }
    }
    
    fun updateMemoryExtractionPrompt(prompt: String) {
        viewModelScope.launch {
            aiConfigRepository.updateMemoryExtractionPrompt(prompt)
            hideMemoryPromptDialog()
        }
    }
    
    fun resetMemoryExtractionPrompt() {
        viewModelScope.launch {
            aiConfigRepository.resetMemoryExtractionPrompt()
        }
    }
    
    fun showDeepEmpathyPromptDialog() {
        _uiState.update { it.copy(showDeepEmpathyPromptDialog = true) }
    }
    
    fun hideDeepEmpathyPromptDialog() {
        _uiState.update { it.copy(showDeepEmpathyPromptDialog = false) }
    }
    
    fun updateDeepEmpathyPrompt(prompt: String) {
        viewModelScope.launch {
            aiConfigRepository.updateDeepEmpathyPrompt(prompt)
            hideDeepEmpathyPromptDialog()
        }
    }
    
    fun resetDeepEmpathyPrompt() {
        viewModelScope.launch {
            aiConfigRepository.resetDeepEmpathyPrompt()
        }
    }
    
    fun toggleAdvancedDeepEmpathySettings() {
        _uiState.update { it.copy(showAdvancedDeepEmpathySettings = !it.showAdvancedDeepEmpathySettings) }
    }
    
    fun showDeepEmpathyAnalysisDialog() {
        _uiState.update { it.copy(showDeepEmpathyAnalysisDialog = true) }
    }
    
    fun hideDeepEmpathyAnalysisDialog() {
        _uiState.update { it.copy(showDeepEmpathyAnalysisDialog = false) }
    }
    
    fun updateDeepEmpathyAnalysisPrompt(prompt: String) {
        viewModelScope.launch {
            aiConfigRepository.updateDeepEmpathyAnalysisPrompt(prompt)
            hideDeepEmpathyAnalysisDialog()
        }
    }
    
    fun resetDeepEmpathyAnalysisPrompt() {
        viewModelScope.launch {
            aiConfigRepository.resetDeepEmpathyAnalysisPrompt()
        }
    }
    
    /**
     * Check if embedding features (Memory, RAG) can be enabled
     * Requires a downloaded embedding model (local embeddings are needed)
     */
    private fun canEnableEmbeddingFeature(): Boolean {
        return _uiState.value.embeddingModels.values.any { 
            it.status is com.yourown.ai.domain.model.DownloadStatus.Downloaded 
        }
    }
    
    fun hideEmbeddingRequiredDialog() {
        _uiState.update { it.copy(showEmbeddingRequiredDialog = false) }
    }
    
    /**
     * Recalculate all embeddings (Memory + RAG)
     * Use when switching embedding models
     */
    fun recalculateAllEmbeddings() {
        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        isRecalculatingEmbeddings = true,
                        recalculationProgress = "Starting recalculation...",
                        recalculationProgressPercent = 0f
                    ) 
                }
                
                var memoryCount = 0
                var ragCount = 0
                
                // Step 1: Recalculate Memory embeddings (50% of total progress)
                _uiState.update { 
                    it.copy(
                        recalculationProgress = "Recalculating Memory embeddings...",
                        recalculationProgressPercent = 0f
                    ) 
                }
                
                val memoryResult = memoryRepository.recalculateAllEmbeddings { current, total, percentage ->
                    // Memory is 50% of total, so map 0-1 to 0-0.5
                    val overallProgress = percentage * 0.5f
                    _uiState.update { 
                        it.copy(
                            recalculationProgress = "Memory: $current/$total",
                            recalculationProgressPercent = overallProgress
                        ) 
                    }
                }
                memoryCount = memoryResult.getOrNull() ?: 0
                
                // Step 2: Recalculate RAG embeddings (next 50% of total progress)
                _uiState.update { 
                    it.copy(
                        recalculationProgress = "Recalculating RAG embeddings...",
                        recalculationProgressPercent = 0.5f
                    ) 
                }
                
                val ragResult = documentEmbeddingRepository.recalculateAllEmbeddings { current, total, percentage ->
                    // RAG is 50% of total, so map 0-1 to 0.5-1.0
                    val overallProgress = 0.5f + (percentage * 0.5f)
                    _uiState.update { 
                        it.copy(
                            recalculationProgress = "RAG: $current/$total chunks",
                            recalculationProgressPercent = overallProgress
                        ) 
                    }
                }
                ragCount = ragResult.getOrNull() ?: 0
                
                // Done
                _uiState.update { 
                    it.copy(
                        isRecalculatingEmbeddings = false,
                        recalculationProgress = "✅ Completed! Memory: $memoryCount, RAG: $ragCount chunks",
                        recalculationProgressPercent = 1f
                    ) 
                }
                
                // Clear progress after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.update { 
                    it.copy(
                        recalculationProgress = null,
                        recalculationProgressPercent = 0f
                    ) 
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error recalculating embeddings", e)
                _uiState.update { 
                    it.copy(
                        isRecalculatingEmbeddings = false,
                        recalculationProgress = "❌ Error: ${e.message}",
                        recalculationProgressPercent = 0f
                    ) 
                }
                
                // Clear error after 5 seconds
                kotlinx.coroutines.delay(5000)
                _uiState.update { 
                    it.copy(
                        recalculationProgress = null,
                        recalculationProgressPercent = 0f
                    ) 
                }
            }
        }
    }
}
