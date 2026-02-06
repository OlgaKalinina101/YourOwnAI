package com.yourown.ai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourown.ai.data.repository.*
import com.yourown.ai.domain.model.*
import com.yourown.ai.presentation.settings.managers.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val personas: List<Persona> = emptyList(),
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
    val systemPrompts: List<SystemPrompt> = emptyList(),
    val apiPrompts: List<SystemPrompt> = emptyList(),
    val localPrompts: List<SystemPrompt> = emptyList(),
    val knowledgeDocuments: List<KnowledgeDocument> = emptyList(),
    val documentProcessingStatus: DocumentProcessingStatus = DocumentProcessingStatus.Idle,
    val memories: List<MemoryEntry> = emptyList(),
    val memoryProcessingStatus: com.yourown.ai.data.repository.MemoryProcessingStatus = com.yourown.ai.data.repository.MemoryProcessingStatus.Idle,
    val showSystemPromptDialog: Boolean = false,
    val showLocalSystemPromptDialog: Boolean = false,
    val showSystemPromptsListDialog: Boolean = false,
    val showEditPromptDialog: Boolean = false,
    val selectedPromptForEdit: SystemPrompt? = null,
    val promptTypeFilter: PromptType? = null,
    val showDocumentsListDialog: Boolean = false,
    val showEditDocumentDialog: Boolean = false,
    val selectedDocumentForEdit: KnowledgeDocument? = null,
    val showMemoriesDialog: Boolean = false,
    val showEditMemoryDialog: Boolean = false,
    val selectedMemoryForEdit: MemoryEntry? = null,
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
    val recalculationProgressPercent: Float = 0f, // 0.0 to 1.0
    // Sound & Haptics
    val keyboardSoundVolume: Float = 0f
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val localModelRepository: LocalModelRepository,
    private val embeddingModelRepository: LocalEmbeddingModelRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val aiConfigRepository: AIConfigRepository,
    private val systemPromptRepository: SystemPromptRepository,
    private val knowledgeDocumentRepository: KnowledgeDocumentRepository,
    private val memoryRepository: MemoryRepository,
    private val personaRepository: PersonaRepository,
    private val settingsManager: com.yourown.ai.data.local.preferences.SettingsManager,
    private val keyboardSoundManager: com.yourown.ai.domain.service.KeyboardSoundManager,
    // Managers
    private val aiConfigManager: AIConfigManager,
    private val systemPromptManager: SystemPromptManager,
    private val knowledgeDocumentManager: KnowledgeDocumentManager,
    private val memoryManager: MemoryManager,
    private val localModelManager: LocalModelManager,
    private val embeddingModelManager: EmbeddingModelManager,
    private val apiKeyManager: ApiKeyManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        observePersonas()
        observeLocalModels()
        observeEmbeddingModels()
        observeSystemPrompts()
        observeKnowledgeDocuments()
        observeDocumentProcessing()
        observeMemoryProcessing()
        observeMemories()
        observeApiKeys()
        initializeDefaultPrompts()
        observeSoundSettings()
    }
    
    private fun initializeDefaultPrompts() {
        viewModelScope.launch {
            systemPromptRepository.initializeDefaultPrompts()
        }
    }
    
    private fun observePersonas() {
        viewModelScope.launch {
            personaRepository.getAllPersonas().collect { personas ->
                // Дедупликация по systemPromptId - оставляем только уникальные
                val uniquePersonas = personas
                    .groupBy { it.systemPromptId }
                    .mapValues { (_, duplicates) -> 
                        // Если есть дубликаты - берем самую новую (по updatedAt)
                        duplicates.maxByOrNull { it.updatedAt } ?: duplicates.first()
                    }
                    .values
                    .toList()
                    
                _uiState.update { it.copy(personas = uniquePersonas) }
            }
        }
    }
    
    private fun observeSystemPrompts() {
        viewModelScope.launch {
            systemPromptRepository.getAllPrompts().collect { prompts ->
                _uiState.update {
                    it.copy(
                        systemPrompts = prompts,
                        apiPrompts = prompts.filter { p -> p.type.value == "api" },
                        localPrompts = prompts.filter { p -> p.type.value == "local" }
                    )
                }
            }
        }
    }
    
    private fun loadSettings() {
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
    
    private fun observeApiKeys() {
        viewModelScope.launch {
            apiKeyRepository.apiKeys.collect { keys ->
                _uiState.update { state ->
                    state.copy(
                        apiKeys = AIProvider.entries.map { provider ->
                            ApiKeyInfo(
                                provider = provider,
                                isSet = keys.containsKey(provider)
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
    
    private fun observeMemoryProcessing() {
        viewModelScope.launch {
            memoryRepository.processingStatus.collect { status ->
                _uiState.update { it.copy(memoryProcessingStatus = status) }
            }
        }
    }
    
    private fun observeMemories() {
        viewModelScope.launch {
            memoryRepository.getAllMemories().collect { memories ->
                _uiState.update { it.copy(memories = memories) }
            }
        }
    }
    
    // ===== AI Config Methods =====
    
    fun updateTemperature(value: Float) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateTemperature(_uiState.value.aiConfig, value)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateTopP(value: Float) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateTopP(_uiState.value.aiConfig, value)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun toggleDeepEmpathy() {
        viewModelScope.launch {
            val updated = aiConfigManager.toggleDeepEmpathy(_uiState.value.aiConfig)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun toggleMemory() {
        viewModelScope.launch {
            if (!_uiState.value.aiConfig.memoryEnabled && !canEnableEmbeddingFeature()) {
                _uiState.update { it.copy(showEmbeddingRequiredDialog = true) }
                return@launch
            }
            val updated = aiConfigManager.toggleMemory(_uiState.value.aiConfig)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun toggleRAG() {
        viewModelScope.launch {
            if (!_uiState.value.aiConfig.ragEnabled && !canEnableEmbeddingFeature()) {
                _uiState.update { it.copy(showEmbeddingRequiredDialog = true) }
                return@launch
            }
            val updated = aiConfigManager.toggleRAG(_uiState.value.aiConfig)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateMessageHistoryLimit(limit: Int) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateMessageHistoryLimit(_uiState.value.aiConfig, limit)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateMaxTokens(tokens: Int) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateMaxTokens(_uiState.value.aiConfig, tokens)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateRAGChunkSize(value: Int) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateRAGChunkSize(_uiState.value.aiConfig, value)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateRAGChunkOverlap(value: Int) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateRAGChunkOverlap(_uiState.value.aiConfig, value)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateMemoryLimit(value: Int) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateMemoryLimit(_uiState.value.aiConfig, value)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateMemoryMinAgeDays(value: Int) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateMemoryMinAgeDays(_uiState.value.aiConfig, value)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateRAGChunkLimit(value: Int) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateRAGChunkLimit(_uiState.value.aiConfig, value)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateContextInstructions(instructions: String) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateContextInstructions(_uiState.value.aiConfig, instructions)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun resetContextInstructions() {
        viewModelScope.launch {
            val updated = aiConfigManager.resetContextInstructions(_uiState.value.aiConfig)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateSwipeMessagePrompt(prompt: String) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateSwipeMessagePrompt(_uiState.value.aiConfig, prompt)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun resetSwipeMessagePrompt() {
        viewModelScope.launch {
            val updated = aiConfigManager.resetSwipeMessagePrompt(_uiState.value.aiConfig)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateMemoryTitle(title: String) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateMemoryTitle(_uiState.value.aiConfig, title)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateMemoryInstructions(instructions: String) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateMemoryInstructions(_uiState.value.aiConfig, instructions)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun resetMemoryInstructions() {
        viewModelScope.launch {
            val updated = aiConfigManager.resetMemoryInstructions(_uiState.value.aiConfig)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateRAGTitle(title: String) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateRAGTitle(_uiState.value.aiConfig, title)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateRAGInstructions(instructions: String) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateRAGInstructions(_uiState.value.aiConfig, instructions)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun resetRAGInstructions() {
        viewModelScope.launch {
            val updated = aiConfigManager.resetRAGInstructions(_uiState.value.aiConfig)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateMemoryExtractionPrompt(prompt: String) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateMemoryExtractionPrompt(_uiState.value.aiConfig, prompt)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun resetMemoryExtractionPrompt() {
        viewModelScope.launch {
            val updated = aiConfigManager.resetMemoryExtractionPrompt(_uiState.value.aiConfig)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateDeepEmpathyPrompt(prompt: String) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateDeepEmpathyPrompt(_uiState.value.aiConfig, prompt)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun resetDeepEmpathyPrompt() {
        viewModelScope.launch {
            val updated = aiConfigManager.resetDeepEmpathyPrompt(_uiState.value.aiConfig)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateDeepEmpathyAnalysisPrompt(prompt: String) {
        viewModelScope.launch {
            val updated = aiConfigManager.updateDeepEmpathyAnalysisPrompt(_uiState.value.aiConfig, prompt)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun resetDeepEmpathyAnalysisPrompt() {
        viewModelScope.launch {
            val updated = aiConfigManager.resetDeepEmpathyAnalysisPrompt(_uiState.value.aiConfig)
            _uiState.update { it.copy(aiConfig = updated) }
        }
    }
    
    fun updateContext(context: String) {
        viewModelScope.launch {
            aiConfigManager.updateContext(_uiState.value.aiConfig, context)
        }
    }
    
    // ===== Dialog State Methods =====
    
    fun showSystemPromptDialog() {
        _uiState.update { it.copy(showSystemPromptDialog = true) }
    }
    
    fun hideSystemPromptDialog() {
        _uiState.update { it.copy(showSystemPromptDialog = false) }
    }
    
    fun updateSystemPrompt(prompt: String) {
        viewModelScope.launch {
            aiConfigRepository.updateSystemPrompt(prompt)
            hideSystemPromptDialog()
        }
    }
    
    fun showLocalSystemPromptDialog() {
        _uiState.update { it.copy(showLocalSystemPromptDialog = true) }
    }
    
    fun hideLocalSystemPromptDialog() {
        _uiState.update { it.copy(showLocalSystemPromptDialog = false) }
    }
    
    fun updateLocalSystemPrompt(prompt: String) {
        viewModelScope.launch {
            aiConfigRepository.updateLocalSystemPrompt(prompt)
            hideLocalSystemPromptDialog()
        }
    }
    
    fun showContextDialog() {
        _uiState.update { it.copy(showContextDialog = true) }
    }
    
    fun hideContextDialog() {
        _uiState.update { it.copy(showContextDialog = false) }
    }
    
    fun toggleAdvancedContextSettings() {
        _uiState.update { it.copy(showAdvancedContextSettings = !it.showAdvancedContextSettings) }
    }
    
    fun toggleAdvancedMemorySettings() {
        _uiState.update { it.copy(showAdvancedMemorySettings = !it.showAdvancedMemorySettings) }
    }
    
    fun toggleAdvancedRAGSettings() {
        _uiState.update { it.copy(showAdvancedRAGSettings = !it.showAdvancedRAGSettings) }
    }
    
    fun showContextInstructionsDialog() {
        _uiState.update { it.copy(showContextInstructionsDialog = true) }
    }
    
    fun hideContextInstructionsDialog() {
        _uiState.update { it.copy(showContextInstructionsDialog = false) }
    }
    
    fun showSwipeMessagePromptDialog() {
        _uiState.update { it.copy(showSwipeMessagePromptDialog = true) }
    }
    
    fun hideSwipeMessagePromptDialog() {
        _uiState.update { it.copy(showSwipeMessagePromptDialog = false) }
    }
    
    fun showMemoryInstructionsDialog() {
        _uiState.update { it.copy(showMemoryInstructionsDialog = true) }
    }
    
    fun hideMemoryInstructionsDialog() {
        _uiState.update { it.copy(showMemoryInstructionsDialog = false) }
    }
    
    fun showRAGInstructionsDialog() {
        _uiState.update { it.copy(showRAGInstructionsDialog = true) }
    }
    
    fun hideRAGInstructionsDialog() {
        _uiState.update { it.copy(showRAGInstructionsDialog = false) }
    }
    
    fun showLocalModelsDialog() {
        _uiState.update { it.copy(showLocalModelsDialog = true) }
    }
    
    fun hideLocalModelsDialog() {
        _uiState.update { it.copy(showLocalModelsDialog = false) }
    }
    
    fun showEmbeddingModelsDialog() {
        _uiState.update { it.copy(showEmbeddingModelsDialog = true) }
    }
    
    fun hideEmbeddingModelsDialog() {
        viewModelScope.launch {
            // Check if Memory or RAG are enabled but no embedding model downloaded
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
    
    fun showAppearanceDialog() {
        _uiState.update { it.copy(showAppearanceDialog = true) }
    }
    
    fun hideAppearanceDialog() {
        _uiState.update { it.copy(showAppearanceDialog = false) }
    }
    
    fun showSystemPromptsListDialog(type: PromptType) {
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
    
    fun showEditPromptDialog(prompt: SystemPrompt? = null) {
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
    
    fun showDocumentsListDialog() {
        _uiState.update { it.copy(showDocumentsListDialog = true) }
    }
    
    fun hideDocumentsListDialog() {
        _uiState.update { it.copy(showDocumentsListDialog = false) }
    }
    
    fun showEditDocumentDialog(document: KnowledgeDocument? = null) {
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
    
    fun showEditMemoryDialog(memory: MemoryEntry) {
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
    
    fun showMemoryPromptDialog() {
        _uiState.update { it.copy(showMemoryPromptDialog = true) }
    }
    
    fun hideMemoryPromptDialog() {
        _uiState.update { it.copy(showMemoryPromptDialog = false) }
    }
    
    fun showDeepEmpathyPromptDialog() {
        _uiState.update { it.copy(showDeepEmpathyPromptDialog = true) }
    }
    
    fun hideDeepEmpathyPromptDialog() {
        _uiState.update { it.copy(showDeepEmpathyPromptDialog = false) }
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
    
    fun hideEmbeddingRequiredDialog() {
        _uiState.update { it.copy(showEmbeddingRequiredDialog = false) }
    }
    
    // ===== System Prompts Methods =====
    
    fun createNewPrompt(type: PromptType) {
        viewModelScope.launch {
            systemPromptManager.createNewPrompt(
                type,
                _uiState.value.apiPrompts.size,
                _uiState.value.localPrompts.size
            )
        }
    }
    
    fun savePrompt(
        id: String?,
        name: String,
        content: String,
        type: PromptType,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            systemPromptManager.savePrompt(id, name, content, type, isDefault)
            hideEditPromptDialog()
        }
    }
    
    fun deletePrompt(id: String) {
        viewModelScope.launch {
            // Сначала проверяем, есть ли связанная Persona
            val linkedPersona = personaRepository.getPersonaBySystemPromptId(id)
            
            // Если есть - удаляем Persona перед удалением System Prompt
            if (linkedPersona != null) {
                personaRepository.deletePersona(linkedPersona.id)
            }
            
            // Удаляем System Prompt
            systemPromptManager.deletePrompt(id)
        }
    }
    
    fun setPromptAsDefault(id: String) {
        viewModelScope.launch {
            systemPromptManager.setPromptAsDefault(id)
        }
    }
    
    // ===== Knowledge Documents Methods =====
    
    fun createNewDocument() {
        val count = _uiState.value.knowledgeDocuments.size + 1
        showEditDocumentDialog(
            KnowledgeDocument(
                id = "",
                name = "Doc $count",
                content = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                sizeBytes = 0
            )
        )
    }
    
    fun saveDocument(id: String, name: String, content: String, linkedPersonaIds: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                hideEditDocumentDialog()
                
                if (!_uiState.value.showDocumentsListDialog) {
                    _uiState.update { it.copy(showDocumentsListDialog = true) }
                }
                
                if (id.isEmpty()) {
                    val result = knowledgeDocumentManager.createDocument(name, content, linkedPersonaIds)
                    if (result.isFailure) {
                        android.util.Log.e("SettingsViewModel", "Failed to create document", result.exceptionOrNull())
                    }
                } else {
                    val existingDoc = _uiState.value.knowledgeDocuments.find { it.id == id }
                    val createdAt = existingDoc?.createdAt ?: System.currentTimeMillis()
                    val result = knowledgeDocumentManager.updateDocument(id, name, content, createdAt, linkedPersonaIds)
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
                val result = knowledgeDocumentManager.deleteDocument(id)
                if (result.isFailure) {
                    android.util.Log.e("SettingsViewModel", "Failed to delete document", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error deleting document", e)
            }
        }
    }
    
    // ===== Memory Methods =====
    
    fun saveMemory(fact: String, personaId: String? = null) {
        viewModelScope.launch {
            val memory = _uiState.value.selectedMemoryForEdit
            if (memory != null) {
                memoryManager.updateMemory(memory, fact, personaId)
            }
            hideEditMemoryDialog()
        }
    }
    
    fun deleteMemory(id: String) {
        viewModelScope.launch {
            try {
                val memory = _uiState.value.memories.find { it.id == id }
                if (memory != null) {
                    memoryManager.deleteMemory(memory)
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error deleting memory", e)
            }
        }
    }
    
    // ===== Local Models Methods =====
    
    fun downloadModel(model: LocalModel) {
        viewModelScope.launch {
            localModelManager.downloadModel(model)
        }
    }
    
    fun deleteModel(model: LocalModel) {
        viewModelScope.launch {
            localModelManager.deleteModel(model)
        }
    }
    
    fun forceDeleteAllModels() {
        viewModelScope.launch {
            localModelManager.forceDeleteAllModels()
        }
    }
    
    // ===== Embedding Models Methods =====
    
    fun downloadEmbeddingModel(model: LocalEmbeddingModel) {
        viewModelScope.launch {
            embeddingModelManager.downloadModel(model)
        }
    }
    
    fun deleteEmbeddingModel(model: LocalEmbeddingModel) {
        viewModelScope.launch {
            embeddingModelManager.deleteModel(model)
        }
    }
    
    fun recalculateMemoryEmbeddings() {
        viewModelScope.launch {
            try {
                val result = memoryRepository.recalculateAllEmbeddings()
                
                if (result.isSuccess) {
                    android.util.Log.i("SettingsViewModel", "Memory embeddings recalculated successfully")
                } else {
                    android.util.Log.e("SettingsViewModel", "Failed to recalculate memory embeddings", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error recalculating memory embeddings", e)
            }
        }
    }
    
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
                
                val result = embeddingModelManager.recalculateAllEmbeddings(
                    onMemoryProgress = { current, total, percentage ->
                        val overallProgress = percentage * 0.5f
                        _uiState.update { 
                            it.copy(
                                recalculationProgress = "Memory: $current/$total",
                                recalculationProgressPercent = overallProgress
                            ) 
                        }
                    },
                    onRAGProgress = { current, total, percentage ->
                        val overallProgress = 0.5f + (percentage * 0.5f)
                        _uiState.update { 
                            it.copy(
                                recalculationProgress = "RAG: $current/$total chunks",
                                recalculationProgressPercent = overallProgress
                            ) 
                        }
                    }
                )
                
                result.onSuccess { (memoryCount, ragCount) ->
                    _uiState.update { 
                        it.copy(
                            isRecalculatingEmbeddings = false,
                            recalculationProgress = "✅ Completed! Memory: $memoryCount, RAG: $ragCount chunks",
                            recalculationProgressPercent = 1f
                        ) 
                    }
                    
                    kotlinx.coroutines.delay(3000)
                    _uiState.update { 
                        it.copy(
                            recalculationProgress = null,
                            recalculationProgressPercent = 0f
                        ) 
                    }
                }.onFailure { e ->
                    android.util.Log.e("SettingsViewModel", "Error recalculating embeddings", e)
                    _uiState.update { 
                        it.copy(
                            isRecalculatingEmbeddings = false,
                            recalculationProgress = "❌ Error: ${e.message}",
                            recalculationProgressPercent = 0f
                        ) 
                    }
                    
                    kotlinx.coroutines.delay(5000)
                    _uiState.update { 
                        it.copy(
                            recalculationProgress = null,
                            recalculationProgressPercent = 0f
                        ) 
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error recalculating embeddings", e)
            }
        }
    }
    
    // ===== API Keys Methods =====
    
    fun saveApiKey(provider: AIProvider, key: String) {
        viewModelScope.launch {
            apiKeyManager.saveApiKey(provider, key)
            hideApiKeyDialog()
        }
    }
    
    fun deleteApiKey(provider: AIProvider) {
        viewModelScope.launch {
            apiKeyManager.deleteApiKey(provider)
        }
    }
    
    fun testApiKey(provider: AIProvider) {
        viewModelScope.launch {
            val result = apiKeyManager.testApiKey(provider)
            result.onSuccess { models ->
                android.util.Log.i("SettingsViewModel", "$provider models: $models")
            }.onFailure { error ->
                android.util.Log.e("SettingsViewModel", "Failed to test $provider: ${error.message}")
            }
        }
    }
    
    // ===== Helper Methods =====
    
    private fun canEnableEmbeddingFeature(): Boolean {
        return _uiState.value.embeddingModels.values.any { 
            it.status is DownloadStatus.Downloaded 
        }
    }
    
    // ===== Sound & Haptics =====
    
    private fun observeSoundSettings() {
        viewModelScope.launch {
            settingsManager.keyboardSoundVolume.collect { volume ->
                _uiState.update { it.copy(keyboardSoundVolume = volume) }
            }
        }
    }
    
    fun updateKeyboardSoundVolume(volume: Float) {
        viewModelScope.launch {
            settingsManager.setKeyboardSoundVolume(volume)
        }
    }
    
    fun testKeyboardSound() {
        keyboardSoundManager.playTestSound()
    }
}