package com.yourown.ai.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R
import com.yourown.ai.presentation.settings.dialogs.ApiKeyDialog
import com.yourown.ai.presentation.settings.dialogs.AppearanceDialog
import com.yourown.ai.presentation.settings.dialogs.ContextDialog
import com.yourown.ai.presentation.settings.dialogs.ContextHelpDialog
import com.yourown.ai.presentation.settings.dialogs.LocalModelsDialog
import com.yourown.ai.presentation.settings.dialogs.EmbeddingModelsDialog
import com.yourown.ai.presentation.settings.sections.*
import com.yourown.ai.presentation.settings.components.MemoryClusteringDialog
import com.yourown.ai.presentation.settings.components.BiographyViewDialog
import com.yourown.ai.presentation.settings.components.ModelSelectorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToPersonaDetail: (systemPromptId: String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.settings_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Persona Section
            PersonaSection(
                systemPrompts = uiState.systemPrompts,
                personas = uiState.personas
                    .filter { persona ->
                        // Исключаем personas, связанные с default prompts
                        val linkedPrompt = uiState.systemPrompts.find { it.id == persona.systemPromptId }
                        linkedPrompt?.isDefault == false
                    }
                    .associateBy { it.systemPromptId },
                onSelectSystemPrompt = { systemPromptId ->
                    onNavigateToPersonaDetail(systemPromptId)
                }
            )
            
            // API Keys Section
            ApiKeysSection(
                apiKeys = uiState.apiKeys,
                onAddKey = viewModel::showApiKeyDialog,
                onDeleteKey = viewModel::deleteApiKey,
                onShowLocalModels = viewModel::showLocalModelsDialog
            )
            
            // AI Configuration Section
            AIConfigurationSection(
                config = uiState.aiConfig,
                uiState = uiState,
                apiPrompts = uiState.apiPrompts,
                localPrompts = uiState.localPrompts,
                viewModel = viewModel,
                onEditSystemPrompt = viewModel::showSystemPromptDialog,
                onEditLocalSystemPrompt = viewModel::showLocalSystemPromptDialog,
                onTemperatureChange = viewModel::updateTemperature,
                onTopPChange = viewModel::updateTopP,
                onMessageHistoryChange = viewModel::updateMessageHistoryLimit,
                onMaxTokensChange = viewModel::updateMaxTokens
            )
            
            // Preanalysis Section
            PreanalysisSection(
                config = uiState.aiConfig,
                uiState = uiState,
                viewModel = viewModel,
                onToggleDeepEmpathy = viewModel::toggleDeepEmpathy
            )
            
            // Context Section
            ContextSection(
                hasContext = uiState.userContext.content.isNotEmpty(),
                uiState = uiState,
                viewModel = viewModel,
                onEditContext = viewModel::showContextDialog,
                onShowHelp = viewModel::showContextHelpDialog
            )
            
            // Embedding Models Section
            EmbeddingModelsSection(
                onShowEmbeddingModels = viewModel::showEmbeddingModelsDialog,
                onRecalculateEmbeddings = viewModel::recalculateAllEmbeddings,
                isRecalculating = uiState.isRecalculatingEmbeddings,
                recalculationProgress = uiState.recalculationProgress,
                recalculationProgressPercent = uiState.recalculationProgressPercent,
                memoryProcessingStatus = uiState.memoryProcessingStatus,
                useApiEmbeddings = uiState.aiConfig.useApiEmbeddings,
                apiEmbeddingsProvider = uiState.aiConfig.apiEmbeddingsProvider,
                apiEmbeddingsModel = uiState.aiConfig.apiEmbeddingsModel,
                onUseApiEmbeddingsChange = viewModel::updateUseApiEmbeddings,
                onApiEmbeddingsProviderChange = viewModel::updateApiEmbeddingsProvider,
                onApiEmbeddingsModelChange = viewModel::updateApiEmbeddingsModel
            )
            
            // Memory Section
            MemorySection(
                config = uiState.aiConfig,
                uiState = uiState,
                viewModel = viewModel,
                onToggleMemory = viewModel::toggleMemory,
                onEditMemoryPrompt = viewModel::showMemoryPromptDialog,
                onMemoryLimitChange = viewModel::updateMemoryLimit,
                onViewMemories = viewModel::showMemoriesDialog,
                onManageMemory = viewModel::showMemoryClusteringDialog
            )
            
            // RAG Section
            RAGSection(
                config = uiState.aiConfig,
                uiState = uiState,
                viewModel = viewModel,
                documentsCount = uiState.knowledgeDocuments.size,
                documentProcessingStatus = uiState.documentProcessingStatus,
                onToggleRAG = viewModel::toggleRAG,
                onChunkSizeChange = { viewModel.updateRAGChunkSize(it.toInt()) },
                onChunkOverlapChange = { viewModel.updateRAGChunkOverlap(it.toInt()) },
                onRAGChunkLimitChange = viewModel::updateRAGChunkLimit,
                onManageDocuments = viewModel::showDocumentsListDialog,
                onAddDocument = viewModel::createNewDocument
            )
            
            // Appearance Section
            AppearanceSection(
                onShowAppearance = viewModel::showAppearanceDialog
            )
            
            // Sound & Haptics Section
            SoundHapticsSection(
                keyboardSoundVolume = uiState.keyboardSoundVolume,
                onSoundVolumeChange = viewModel::updateKeyboardSoundVolume,
                onTestSound = viewModel::testKeyboardSound
            )
            
            // Cloud Sync Section
            CloudSyncSection(
                syncSettings = uiState.cloudSyncSettings,
                syncableDataSizeMB = uiState.syncableDataSizeMB,
                onToggleSync = viewModel::toggleCloudSync,
                onEditConnectionString = viewModel::showCloudSyncDialog,
                onToggleAutoSync = viewModel::toggleAutoSync,
                onSyncNow = viewModel::syncNow,
                onShowSql = viewModel::showSqlSchemaDialog,
                onShowInstructions = viewModel::showCloudSyncInstructionsDialog,
                isSyncing = uiState.isSyncing
            )
            
            // Language Section
            LanguageSection(
                promptLanguage = uiState.promptLanguage,
                onPromptLanguageChange = viewModel::updatePromptLanguage
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // ===== ALL DIALOGS =====
    
    // System Prompts List Dialog
    if (uiState.showSystemPromptsListDialog && uiState.promptTypeFilter != null) {
        val promptsList = when (uiState.promptTypeFilter) {
            com.yourown.ai.data.repository.PromptType.API -> uiState.apiPrompts
            com.yourown.ai.data.repository.PromptType.LOCAL -> uiState.localPrompts
            else -> emptyList()
        }
        
        com.yourown.ai.presentation.settings.components.SystemPromptsListDialog(
            prompts = promptsList,
            promptType = uiState.promptTypeFilter!!,
            personas = uiState.personas
                .filter { persona ->
                    // Исключаем personas, связанные с default prompts
                    val linkedPrompt = uiState.systemPrompts.find { it.id == persona.systemPromptId }
                    linkedPrompt?.isDefault == false
                }
                .associateBy { it.systemPromptId },
            onDismiss = viewModel::hideSystemPromptsListDialog,
            onAddNew = { viewModel.showEditPromptDialog(null) },
            onEdit = { viewModel.showEditPromptDialog(it) },
            onDelete = viewModel::deletePrompt,
            onSetDefault = viewModel::setPromptAsDefault
        )
    }
    
    // Edit Prompt Dialog
    if (uiState.showEditPromptDialog) {
        com.yourown.ai.presentation.settings.components.EditPromptDialog(
            prompt = uiState.selectedPromptForEdit,
            promptType = uiState.promptTypeFilter ?: com.yourown.ai.data.repository.PromptType.API,
            allPrompts = uiState.systemPrompts,
            onDismiss = viewModel::hideEditPromptDialog,
            onSave = { name, content, isDefault ->
                viewModel.savePrompt(
                    id = uiState.selectedPromptForEdit?.id,
                    name = name,
                    content = content,
                    type = uiState.promptTypeFilter ?: com.yourown.ai.data.repository.PromptType.API,
                    isDefault = isDefault
                )
            }
        )
    }
    
    // Documents List Dialog
    if (uiState.showDocumentsListDialog) {
        com.yourown.ai.presentation.settings.components.KnowledgeDocumentsListDialog(
            documents = uiState.knowledgeDocuments,
            processingStatus = uiState.documentProcessingStatus,
            onDismiss = viewModel::hideDocumentsListDialog,
            onAddDocument = viewModel::createNewDocument,
            onEditDocument = { viewModel.showEditDocumentDialog(it) },
            onDeleteDocument = viewModel::deleteDocument
        )
    }
    
    // Edit Document Dialog
    if (uiState.showEditDocumentDialog) {
        com.yourown.ai.presentation.settings.components.EditDocumentDialog(
            document = uiState.selectedDocumentForEdit,
            personas = uiState.personas,
            onDismiss = viewModel::hideEditDocumentDialog,
            onSave = { id, name, content, linkedPersonaIds ->
                viewModel.saveDocument(id, name, content, linkedPersonaIds)
            }
        )
    }
    
    // Context Dialog
    if (uiState.showContextDialog) {
        ContextDialog(
            currentContext = uiState.userContext.content,
            onDismiss = viewModel::hideContextDialog,
            onSave = viewModel::updateContext
        )
    }
    
    // API Key Dialog
    if (uiState.showApiKeyDialog && uiState.selectedProvider != null) {
        ApiKeyDialog(
            provider = uiState.selectedProvider!!,
            onDismiss = viewModel::hideApiKeyDialog,
            onSave = { key -> viewModel.saveApiKey(uiState.selectedProvider!!, key) }
        )
    }
    
    // Local Models Dialog
    if (uiState.showLocalModelsDialog) {
        LocalModelsDialog(
            models = uiState.localModels,
            onDismiss = viewModel::hideLocalModelsDialog,
            onDownload = viewModel::downloadModel,
            onDelete = viewModel::deleteModel
        )
    }
    
    // Embedding Models Dialog
    if (uiState.showEmbeddingModelsDialog) {
        EmbeddingModelsDialog(
            models = uiState.embeddingModels,
            onDismiss = viewModel::hideEmbeddingModelsDialog,
            onDownload = viewModel::downloadEmbeddingModel,
            onDelete = viewModel::deleteEmbeddingModel
        )
    }
    
    // Appearance Dialog
    if (uiState.showAppearanceDialog) {
        AppearanceDialog(
            onDismiss = viewModel::hideAppearanceDialog
        )
    }
    
    // Memories Dialog
    if (uiState.showMemoriesDialog) {
        com.yourown.ai.presentation.settings.components.MemoriesDialog(
            memories = uiState.memories,
            personas = uiState.personas,
            processingStatus = uiState.memoryProcessingStatus,
            onDismiss = viewModel::hideMemoriesDialog,
            onEditMemory = viewModel::showEditMemoryDialog,
            onDeleteMemory = viewModel::deleteMemory
        )
    }
    
    // Memory Clustering Dialog
    if (uiState.showMemoryClusteringDialog) {
        MemoryClusteringDialog(
            clusteringStatus = uiState.memoryClusteringStatus,
            biographyStatus = uiState.biographyGenerationStatus,
            memoryCleaningStatus = uiState.memoryCleaningStatus,
            selectedModel = uiState.selectedModelForBiography,
            biography = uiState.userBiography,
            onStartClustering = viewModel::startMemoryClustering,
            onSelectModel = viewModel::showModelSelectorForBiography,
            onGenerateBiography = viewModel::generateBiography,
            onCancelBiography = viewModel::cancelBiographyGeneration,
            onCleanMemories = viewModel::cleanMemories,
            onCancelCleaning = viewModel::cancelMemoryCleaning,
            onViewBiography = viewModel::showBiographyDialog,
            onDismiss = viewModel::hideMemoryClusteringDialog
        )
    }
    
    // Biography View Dialog
    if (uiState.showBiographyDialog && uiState.userBiography != null) {
        BiographyViewDialog(
            biography = uiState.userBiography!!,
            onDelete = viewModel::deleteBiography,
            onDismiss = viewModel::hideBiographyDialog
        )
    }
    
    // Model Selector for Biography
    if (uiState.showModelSelectorForBiography) {
        ModelSelectorDialog(
            selectedModel = uiState.selectedModelForBiography,
            availableModels = viewModel.getAvailableModels(),
            onSelectModel = viewModel::selectModelForBiography,
            onDismiss = viewModel::hideModelSelectorForBiography
        )
    }
    
    // Edit Memory Dialog
    if (uiState.showEditMemoryDialog && uiState.selectedMemoryForEdit != null) {
        com.yourown.ai.presentation.settings.components.EditMemoryDialog(
            memory = uiState.selectedMemoryForEdit!!,
            personas = uiState.personas,
            processingStatus = uiState.memoryProcessingStatus,
            onDismiss = viewModel::hideEditMemoryDialog,
            onSave = { fact, personaId ->
                viewModel.saveMemory(fact, personaId)
            }
        )
    }
    
    // Memory Prompt Dialog
    if (uiState.showMemoryPromptDialog) {
        com.yourown.ai.presentation.settings.components.MemoryExtractionPromptDialog(
            currentPrompt = uiState.aiConfig.memoryExtractionPrompt,
            onDismiss = viewModel::hideMemoryPromptDialog,
            onSave = viewModel::updateMemoryExtractionPrompt,
            onReset = viewModel::resetMemoryExtractionPrompt
        )
    }
    
    // Deep Empathy Prompt Dialog
    if (uiState.showDeepEmpathyPromptDialog) {
        com.yourown.ai.presentation.settings.components.DeepEmpathyPromptDialog(
            currentPrompt = uiState.aiConfig.deepEmpathyPrompt,
            onDismiss = viewModel::hideDeepEmpathyPromptDialog,
            onSave = viewModel::updateDeepEmpathyPrompt,
            onReset = viewModel::resetDeepEmpathyPrompt
        )
    }
    
    // Deep Empathy Analysis Dialog
    if (uiState.showDeepEmpathyAnalysisDialog) {
        com.yourown.ai.presentation.settings.components.DeepEmpathyAnalysisDialog(
            currentPrompt = uiState.aiConfig.deepEmpathyAnalysisPrompt,
            onDismiss = viewModel::hideDeepEmpathyAnalysisDialog,
            onSave = viewModel::updateDeepEmpathyAnalysisPrompt,
            onReset = viewModel::resetDeepEmpathyAnalysisPrompt
        )
    }
    
    // Context Instructions Dialog
    if (uiState.showContextInstructionsDialog) {
        com.yourown.ai.presentation.settings.components.ContextInstructionsDialog(
            currentInstructions = uiState.aiConfig.contextInstructions,
            onDismiss = viewModel::hideContextInstructionsDialog,
            onSave = viewModel::updateContextInstructions,
            onReset = viewModel::resetContextInstructions
        )
    }
    
    // Context Help Dialog
    if (uiState.showContextHelpDialog) {
        ContextHelpDialog(
            onDismiss = viewModel::hideContextHelpDialog
        )
    }
    
    // Swipe Message Prompt Dialog
    if (uiState.showSwipeMessagePromptDialog) {
        com.yourown.ai.presentation.settings.components.SwipeMessagePromptDialog(
            currentPrompt = uiState.aiConfig.swipeMessagePrompt,
            onDismiss = viewModel::hideSwipeMessagePromptDialog,
            onSave = viewModel::updateSwipeMessagePrompt,
            onReset = viewModel::resetSwipeMessagePrompt
        )
    }
    
    // Memory Instructions Dialog
    if (uiState.showMemoryInstructionsDialog) {
        com.yourown.ai.presentation.settings.components.MemoryInstructionsDialog(
            currentInstructions = uiState.aiConfig.memoryInstructions,
            onDismiss = viewModel::hideMemoryInstructionsDialog,
            onSave = viewModel::updateMemoryInstructions,
            onReset = viewModel::resetMemoryInstructions
        )
    }
    
    // RAG Instructions Dialog
    if (uiState.showRAGInstructionsDialog) {
        com.yourown.ai.presentation.settings.components.RAGInstructionsDialog(
            currentInstructions = uiState.aiConfig.ragInstructions,
            onDismiss = viewModel::hideRAGInstructionsDialog,
            onSave = viewModel::updateRAGInstructions,
            onReset = viewModel::resetRAGInstructions
        )
    }
    
    // Embedding Required Dialog
    if (uiState.showEmbeddingRequiredDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideEmbeddingRequiredDialog,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.embedding_required_title)) },
            text = {
                Text(stringResource(R.string.embedding_required_message))
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.hideEmbeddingRequiredDialog()
                    viewModel.showEmbeddingModelsDialog()
                }) {
                    Text(stringResource(R.string.embedding_download_button))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideEmbeddingRequiredDialog) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
    
    // Cloud Sync Dialog
    if (uiState.showCloudSyncDialog) {
        com.yourown.ai.presentation.settings.dialogs.CloudSyncDialog(
            currentUrl = uiState.cloudSyncSettings.supabaseUrl,
            currentKey = uiState.cloudSyncSettings.supabaseKey,
            onDismiss = viewModel::hideCloudSyncDialog,
            onSave = viewModel::saveSupabaseCredentials,
            onTestConnection = viewModel::testSupabaseConnection
        )
    }
    
    // SQL Schema Dialog
    if (uiState.showSqlSchemaDialog) {
        com.yourown.ai.presentation.settings.dialogs.SqlSchemaDialog(
            onDismiss = viewModel::hideSqlSchemaDialog
        )
    }
    
    // Cloud Sync Instructions Dialog
    if (uiState.showCloudSyncInstructionsDialog) {
        com.yourown.ai.presentation.settings.dialogs.CloudSyncInstructionsDialog(
            onDismiss = viewModel::hideCloudSyncInstructionsDialog
        )
    }
}
