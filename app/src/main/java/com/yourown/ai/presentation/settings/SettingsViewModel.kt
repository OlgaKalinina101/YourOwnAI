package com.yourown.ai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourown.ai.data.remote.deepseek.DeepseekClient
import com.yourown.ai.data.repository.LocalModelRepository
import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.domain.model.AIProvider
import com.yourown.ai.domain.model.ApiKeyInfo
import com.yourown.ai.domain.model.LocalModel
import com.yourown.ai.domain.model.LocalModelInfo
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
        ApiKeyInfo(AIProvider.ANTHROPIC),
        ApiKeyInfo(AIProvider.XAI)
    ),
    val aiConfig: AIConfig = AIConfig(),
    val userContext: UserContext = UserContext(),
    val localModels: Map<LocalModel, LocalModelInfo> = emptyMap(),
    val showSystemPromptDialog: Boolean = false,
    val showLocalSystemPromptDialog: Boolean = false,
    val showContextDialog: Boolean = false,
    val showApiKeyDialog: Boolean = false,
    val showLocalModelsDialog: Boolean = false,
    val showAppearanceDialog: Boolean = false,
    val selectedProvider: AIProvider? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val localModelRepository: LocalModelRepository,
    private val apiKeyRepository: com.yourown.ai.data.repository.ApiKeyRepository,
    private val aiConfigRepository: com.yourown.ai.data.repository.AIConfigRepository,
    private val deepseekClient: DeepseekClient,
    private val openAIClient: com.yourown.ai.data.remote.openai.OpenAIClient,
    private val xaiClient: com.yourown.ai.data.remote.xai.XAIClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        observeLocalModels()
        observeApiKeys()
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
            aiConfigRepository.setMemoryEnabled(newValue)
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
                    // TODO: Implement for Anthropic
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
}
