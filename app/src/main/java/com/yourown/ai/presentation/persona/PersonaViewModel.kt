package com.yourown.ai.presentation.persona

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourown.ai.data.repository.PersonaRepository
import com.yourown.ai.domain.model.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonaUiState(
    val personas: List<Persona> = emptyList(),
    val systemPrompts: List<com.yourown.ai.domain.model.SystemPrompt> = emptyList(),
    val availableModels: List<com.yourown.ai.domain.model.ModelProvider> = emptyList(),
    val localModels: Map<com.yourown.ai.domain.model.LocalModel, com.yourown.ai.domain.model.LocalModelInfo> = emptyMap(),
    val pinnedModels: Set<String> = emptySet(),
    val knowledgeDocuments: List<com.yourown.ai.domain.model.KnowledgeDocument> = emptyList(),
    val personaMemoryCount: Map<String, Int> = emptyMap(), // personaId -> count
    val personaMemories: Map<String, List<com.yourown.ai.domain.model.MemoryEntry>> = emptyMap(), // personaId -> memories
    val selectedPersona: Persona? = null,
    val isLoading: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val personaToDelete: Persona? = null,
    val error: String? = null
)

@HiltViewModel
class PersonaViewModel @Inject constructor(
    private val personaRepository: PersonaRepository,
    private val systemPromptRepository: com.yourown.ai.data.repository.SystemPromptRepository,
    private val localModelRepository: com.yourown.ai.data.repository.LocalModelRepository,
    private val apiKeyRepository: com.yourown.ai.data.repository.ApiKeyRepository,
    private val settingsManager: com.yourown.ai.data.local.preferences.SettingsManager,
    private val knowledgeDocumentRepository: com.yourown.ai.data.repository.KnowledgeDocumentRepository,
    private val memoryRepository: com.yourown.ai.data.repository.MemoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PersonaUiState())
    val uiState: StateFlow<PersonaUiState> = _uiState.asStateFlow()
    
    init {
        loadPersonas()
        loadSystemPrompts()
        observeLocalModels()
        observePinnedModels()
        observeApiKeys()
        observeKnowledgeDocuments()
        observeMemories()
    }
    
    private fun observeKnowledgeDocuments() {
        viewModelScope.launch {
            knowledgeDocumentRepository.getAllDocuments().collect { documents ->
                _uiState.update { it.copy(knowledgeDocuments = documents) }
            }
        }
    }
    
    private fun observeMemories() {
        viewModelScope.launch {
            memoryRepository.getAllMemories().collect { memories ->
                // Группируем memories для каждой persona (только с не-null personaId)
                val personaMemoriesMap = memories
                    .filter { it.personaId != null }
                    .groupBy { it.personaId!! }
                
                val memoryCount = personaMemoriesMap.mapValues { it.value.size }
                
                _uiState.update { 
                    it.copy(
                        personaMemoryCount = memoryCount,
                        personaMemories = personaMemoriesMap
                    ) 
                }
            }
        }
    }
    
    private fun loadPersonas() {
        viewModelScope.launch {
            personaRepository.getAllPersonas().collect { personas ->
                _uiState.update { it.copy(personas = personas) }
            }
        }
    }
    
    private fun loadSystemPrompts() {
        viewModelScope.launch {
            systemPromptRepository.getAllPrompts().collect { prompts ->
                _uiState.update { it.copy(systemPrompts = prompts) }
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
    
    private fun observePinnedModels() {
        viewModelScope.launch {
            settingsManager.pinnedModels.collect { pinned ->
                _uiState.update { it.copy(pinnedModels = pinned) }
            }
        }
    }
    
    private fun observeApiKeys() {
        viewModelScope.launch {
            // Observe API keys to update available models when keys change
            apiKeyRepository.apiKeys.collect { _ -> 
                updateAvailableModels() 
            }
        }
    }
    
    private fun updateAvailableModels() {
        val models = mutableListOf<com.yourown.ai.domain.model.ModelProvider>()
        
        // Add local models
        _uiState.value.localModels.forEach { (model, _) ->
            models.add(com.yourown.ai.domain.model.ModelProvider.Local(model))
        }
        
        // Add API models if keys are set
        if (apiKeyRepository.hasApiKey(com.yourown.ai.domain.model.AIProvider.DEEPSEEK)) {
            com.yourown.ai.domain.model.DeepseekModel.entries.forEach { 
                models.add(it.toModelProvider()) 
            }
        }
        if (apiKeyRepository.hasApiKey(com.yourown.ai.domain.model.AIProvider.OPENAI)) {
            com.yourown.ai.domain.model.OpenAIModel.entries.forEach { 
                models.add(it.toModelProvider()) 
            }
        }
        if (apiKeyRepository.hasApiKey(com.yourown.ai.domain.model.AIProvider.XAI)) {
            com.yourown.ai.domain.model.XAIModel.entries.forEach { 
                models.add(it.toModelProvider()) 
            }
        }
        if (apiKeyRepository.hasApiKey(com.yourown.ai.domain.model.AIProvider.OPENROUTER)) {
            com.yourown.ai.domain.model.OpenRouterModel.entries.forEach { 
                models.add(it.toModelProvider()) 
            }
        }
        
        _uiState.update { it.copy(availableModels = models) }
    }
    
    fun selectPersona(persona: Persona) {
        _uiState.update { it.copy(selectedPersona = persona) }
    }
    
    /**
     * Get existing Persona for SystemPrompt or create new one
     */
    fun getOrCreatePersonaForSystemPrompt(systemPromptId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                // Проверяем, существует ли Persona для этого SystemPrompt
                val existingPersona = personaRepository.getPersonaBySystemPromptId(systemPromptId)
                
                if (existingPersona != null) {
                    // Persona существует - загружаем её
                    _uiState.update { it.copy(selectedPersona = existingPersona) }
                } else {
                    // Persona не существует - создаем новую
                    val systemPrompt = _uiState.value.systemPrompts.find { it.id == systemPromptId }
                    if (systemPrompt == null) {
                        _uiState.update { it.copy(error = "System prompt not found") }
                        return@launch
                    }
                    
                    val newPersona = personaRepository.createPersonaFromSystemPrompt(
                        systemPromptId = systemPrompt.id,
                        systemPromptName = systemPrompt.name,
                        systemPromptContent = systemPrompt.content,
                        description = "",
                        isForApi = systemPrompt.type.value == "api"
                    )
                    
                    _uiState.update { it.copy(selectedPersona = newPersona) }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to load persona") 
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }
    
    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }
    
    fun showEditDialog(persona: Persona) {
        _uiState.update { 
            it.copy(
                selectedPersona = persona,
                showEditDialog = true
            ) 
        }
    }
    
    fun hideEditDialog() {
        _uiState.update { 
            it.copy(
                showEditDialog = false,
                selectedPersona = null
            ) 
        }
    }
    
    fun showDeleteConfirmation(persona: Persona) {
        _uiState.update { 
            it.copy(
                showDeleteConfirmation = true,
                personaToDelete = persona
            ) 
        }
    }
    
    fun hideDeleteConfirmation() {
        _uiState.update { 
            it.copy(
                showDeleteConfirmation = false,
                personaToDelete = null
            ) 
        }
    }
    
    fun createPersonaFromSystemPrompt(systemPromptId: String, description: String, isForApi: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                // Найти SystemPrompt
                val systemPrompt = _uiState.value.systemPrompts.find { it.id == systemPromptId }
                if (systemPrompt == null) {
                    _uiState.update { 
                        it.copy(error = "System prompt not found") 
                    }
                    return@launch
                }
                
                personaRepository.createPersonaFromSystemPrompt(
                    systemPromptId = systemPrompt.id,
                    systemPromptName = systemPrompt.name,
                    systemPromptContent = systemPrompt.content,
                    description = description,
                    isForApi = isForApi
                )
                
                hideCreateDialog()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to create persona") 
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun updatePersona(persona: Persona) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                personaRepository.updatePersona(persona)
                
                // Обновляем selectedPersona в state после успешного сохранения
                _uiState.update { it.copy(selectedPersona = persona) }
                
                hideEditDialog()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to update persona") 
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun deletePersona(persona: Persona, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                personaRepository.deletePersona(persona.id)
                
                // Очищаем selectedPersona после удаления
                _uiState.update { it.copy(selectedPersona = null) }
                
                hideDeleteConfirmation()
                
                // Вызываем callback для навигации назад
                onDeleted()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to delete persona") 
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun updateAISettings(
        personaId: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        deepEmpathy: Boolean,
        memoryEnabled: Boolean,
        ragEnabled: Boolean,
        messageHistoryLimit: Int
    ) {
        viewModelScope.launch {
            try {
                personaRepository.updateAISettings(
                    personaId = personaId,
                    temperature = temperature,
                    topP = topP,
                    maxTokens = maxTokens,
                    deepEmpathy = deepEmpathy,
                    memoryEnabled = memoryEnabled,
                    ragEnabled = ragEnabled,
                    messageHistoryLimit = messageHistoryLimit
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to update AI settings") 
                }
            }
        }
    }
    
    fun updateMemorySettings(
        personaId: String,
        useOnlyPersonaMemories: Boolean,
        shareMemoriesGlobally: Boolean,
        memoryLimit: Int,
        memoryMinAgeDays: Int
    ) {
        viewModelScope.launch {
            try {
                personaRepository.updateMemorySettings(
                    personaId = personaId,
                    useOnlyPersonaMemories = useOnlyPersonaMemories,
                    shareMemoriesGlobally = shareMemoriesGlobally,
                    memoryLimit = memoryLimit,
                    memoryMinAgeDays = memoryMinAgeDays
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to update memory settings") 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun downloadModel(model: com.yourown.ai.domain.model.LocalModel) {
        viewModelScope.launch {
            try {
                localModelRepository.downloadModel(model)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to download model") 
                }
            }
        }
    }
    
    fun togglePinnedModel(model: com.yourown.ai.domain.model.ModelProvider) {
        viewModelScope.launch {
            settingsManager.togglePinnedModel(model.getModelKey())
        }
    }
}
