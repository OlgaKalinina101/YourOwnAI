package com.yourown.ai.presentation.persona

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R
import com.yourown.ai.domain.model.AIConfig
import com.yourown.ai.domain.model.Persona

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaDetailScreen(
    personaId: String, // Это теперь systemPromptId
    viewModel: PersonaViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var persona by remember { mutableStateOf<Persona?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load or create persona for this systemPromptId
    LaunchedEffect(personaId) {
        viewModel.getOrCreatePersonaForSystemPrompt(personaId)
    }
    
    // Observe selected persona
    LaunchedEffect(uiState.selectedPersona) {
        if (uiState.selectedPersona != null) {
            persona = uiState.selectedPersona
            isLoading = false
        }
    }
    
    if (isLoading || persona == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    var editedPersona by remember(persona) { mutableStateOf(persona!!) }
    var showDocumentLinkDialog by remember { mutableStateOf(false) }
    var showMemoriesDialog by remember { mutableStateOf(false) }
    
    // Auto-save with debounce
    LaunchedEffect(editedPersona) {
        // Skip initial load
        if (editedPersona == persona) return@LaunchedEffect
        
        // Debounce: wait 500ms before saving
        kotlinx.coroutines.delay(500)
        viewModel.updatePersona(editedPersona)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(editedPersona.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.persona_back))
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic Info Section
            BasicInfoSection(
                persona = editedPersona,
                availableModels = uiState.availableModels,
                localModels = uiState.localModels,
                pinnedModels = uiState.pinnedModels,
                onUpdate = { updated ->
                    editedPersona = updated
                },
                onModelSelect = { model ->
                    val (modelId, provider) = when (model) {
                        is com.yourown.ai.domain.model.ModelProvider.API -> 
                            model.modelId to model.provider.displayName
                        is com.yourown.ai.domain.model.ModelProvider.Local -> 
                            model.model.modelName to "local"
                    }
                    editedPersona = editedPersona.copy(
                        preferredModelId = modelId,
                        preferredProvider = provider,
                        isForApi = model is com.yourown.ai.domain.model.ModelProvider.API
                    )
                },
                onDownloadModel = { model ->
                    viewModel.downloadModel(model)
                },
                onTogglePinned = { model ->
                    viewModel.togglePinnedModel(model)
                }
            )
            
            HorizontalDivider()
            
            // System Prompt Section
            SystemPromptSection(
                systemPrompt = editedPersona.systemPrompt,
                onUpdate = { prompt ->
                    editedPersona = editedPersona.copy(systemPrompt = prompt)
                }
            )
            
            HorizontalDivider()
            
            // AI Settings Section
            AISettingsSection(
                persona = editedPersona,
                onUpdate = { updated ->
                    editedPersona = updated
                }
            )
            
            HorizontalDivider()
            
            // Memory Settings Section
            MemorySettingsSection(
                persona = editedPersona,
                memoryCount = uiState.personaMemoryCount[editedPersona.id] ?: 0,
                onUpdate = { updated ->
                    editedPersona = updated
                },
                onManageMemories = { showMemoriesDialog = true }
            )
            
            HorizontalDivider()
            
            // RAG Settings Section
            RAGSettingsSection(
                persona = editedPersona,
                onUpdate = { updated ->
                    editedPersona = updated
                },
                onManageDocuments = { showDocumentLinkDialog = true }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Document Link Dialog
    if (showDocumentLinkDialog) {
        DocumentLinkDialog(
            persona = editedPersona,
            availableDocuments = uiState.knowledgeDocuments,
            onDismiss = { showDocumentLinkDialog = false },
            onUpdate = { updated ->
                editedPersona = updated
                showDocumentLinkDialog = false
            }
        )
    }
    
    // Memories Dialog for this persona
    if (showMemoriesDialog) {
        PersonaMemoriesDialog(
            personaId = editedPersona.id,
            personaName = editedPersona.name,
            viewModel = viewModel,
            onDismiss = { showMemoriesDialog = false }
        )
    }
}

@Composable
fun BasicInfoSection(
    persona: Persona,
    availableModels: List<com.yourown.ai.domain.model.ModelProvider>,
    localModels: Map<com.yourown.ai.domain.model.LocalModel, com.yourown.ai.domain.model.LocalModelInfo>,
    pinnedModels: Set<String>,
    onUpdate: (Persona) -> Unit,
    onModelSelect: (com.yourown.ai.domain.model.ModelProvider) -> Unit,
    onDownloadModel: (com.yourown.ai.domain.model.LocalModel) -> Unit,
    onTogglePinned: (com.yourown.ai.domain.model.ModelProvider) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.persona_basic_info),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        OutlinedTextField(
            value = persona.name,
            onValueChange = { onUpdate(persona.copy(name = it)) },
            label = { Text(stringResource(R.string.persona_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = persona.description,
            onValueChange = { onUpdate(persona.copy(description = it)) },
            label = { Text(stringResource(R.string.persona_description_label)) },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Preferred Model
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.persona_preferred_model),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                stringResource(R.string.persona_preferred_model_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Находим выбранную модель
            val selectedModel = remember(persona.preferredModelId, persona.preferredProvider) {
                if (persona.preferredModelId != null && persona.preferredProvider != null) {
                    availableModels.find { model ->
                        when (model) {
                            is com.yourown.ai.domain.model.ModelProvider.API -> 
                                model.modelId == persona.preferredModelId && model.provider.displayName == persona.preferredProvider
                            is com.yourown.ai.domain.model.ModelProvider.Local -> 
                                model.model.modelName == persona.preferredModelId
                        }
                    }
                } else null
            }
            
            com.yourown.ai.presentation.chat.components.ModelSelector(
                selectedModel = selectedModel,
                availableModels = availableModels,
                localModels = localModels,
                pinnedModels = pinnedModels,
                onModelSelect = onModelSelect,
                onDownloadModel = onDownloadModel,
                onTogglePinned = onTogglePinned,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SystemPromptSection(
    systemPrompt: String,
    onUpdate: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.persona_system_prompt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = stringResource(R.string.persona_linked_to_system_prompt),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        
        Text(
            text = stringResource(R.string.persona_system_prompt_readonly_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = systemPrompt,
            onValueChange = { }, // Read-only
            enabled = false,
            label = { Text(stringResource(R.string.persona_prompt_readonly)) },
            minLines = 5,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun AISettingsSection(
    persona: Persona,
    onUpdate: (Persona) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(R.string.persona_ai_settings),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        // Temperature
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.persona_temperature), style = MaterialTheme.typography.bodyMedium)
                Text(
                    String.format("%.2f", persona.temperature),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Slider(
                value = persona.temperature,
                onValueChange = { onUpdate(persona.copy(temperature = it)) },
                valueRange = AIConfig.MIN_TEMPERATURE..AIConfig.MAX_TEMPERATURE,
                steps = 19
            )
        }
        
        // Top P
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.persona_top_p), style = MaterialTheme.typography.bodyMedium)
                Text(
                    String.format("%.2f", persona.topP),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Slider(
                value = persona.topP,
                onValueChange = { onUpdate(persona.copy(topP = it)) },
                valueRange = AIConfig.MIN_TOP_P..AIConfig.MAX_TOP_P,
                steps = 19
            )
        }
        
        // Max Tokens
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.persona_max_tokens), style = MaterialTheme.typography.bodyMedium)
                Text(
                    persona.maxTokens.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Slider(
                value = persona.maxTokens.toFloat(),
                onValueChange = { onUpdate(persona.copy(maxTokens = it.toInt())) },
                valueRange = AIConfig.MIN_MAX_TOKENS.toFloat()..AIConfig.MAX_MAX_TOKENS.toFloat(),
                steps = 30
            )
        }
        
        // Message History Limit
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.persona_message_history), style = MaterialTheme.typography.bodyMedium)
                Text(
                    persona.messageHistoryLimit.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Slider(
                value = persona.messageHistoryLimit.toFloat(),
                onValueChange = { onUpdate(persona.copy(messageHistoryLimit = it.toInt())) },
                valueRange = AIConfig.MIN_MESSAGE_HISTORY.toFloat()..AIConfig.MAX_MESSAGE_HISTORY.toFloat(),
                steps = 23
            )
        }
        
        // Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.persona_deep_empathy), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.persona_deep_empathy_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = persona.deepEmpathy,
                onCheckedChange = { onUpdate(persona.copy(deepEmpathy = it)) }
            )
        }
    }
}

@Composable
fun MemorySettingsSection(
    persona: Persona,
    memoryCount: Int,
    onUpdate: (Persona) -> Unit,
    onManageMemories: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(R.string.persona_memory_settings),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        // Memory Enabled
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.persona_enable_memory), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.persona_enable_memory_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = persona.memoryEnabled,
                onCheckedChange = { onUpdate(persona.copy(memoryEnabled = it)) }
            )
        }
        
        if (persona.memoryEnabled) {
            // Saved Memories Card
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            stringResource(R.string.persona_saved_memories),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            stringResource(R.string.persona_memories_count, memoryCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    TextButton(onClick = onManageMemories) {
                        Text(stringResource(R.string.persona_manage_memories))
                    }
                }
            }
            
            // Use Only Persona Memories
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.persona_isolate_memories),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            stringResource(R.string.persona_isolate_memories_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = persona.useOnlyPersonaMemories,
                        onCheckedChange = { onUpdate(persona.copy(useOnlyPersonaMemories = it)) }
                    )
                }
            }
            
            // Share Memories Globally
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.persona_share_memories),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            stringResource(R.string.persona_share_memories_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = persona.shareMemoriesGlobally,
                        onCheckedChange = { onUpdate(persona.copy(shareMemoriesGlobally = it)) }
                    )
                }
            }
            
            // Memory Limit
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.persona_memory_limit), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        persona.memoryLimit.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = persona.memoryLimit.toFloat(),
                    onValueChange = { onUpdate(persona.copy(memoryLimit = it.toInt())) },
                    valueRange = AIConfig.MIN_MEMORY_LIMIT.toFloat()..AIConfig.MAX_MEMORY_LIMIT.toFloat(),
                    steps = 8
                )
            }
        }
    }
}

@Composable
fun RAGSettingsSection(
    persona: Persona,
    onUpdate: (Persona) -> Unit,
    onManageDocuments: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(R.string.persona_rag_settings),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        // RAG Enabled
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.persona_enable_rag), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.persona_enable_rag_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = persona.ragEnabled,
                onCheckedChange = { onUpdate(persona.copy(ragEnabled = it)) }
            )
        }
        
        if (persona.ragEnabled) {
            // Linked Documents
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            stringResource(R.string.persona_linked_documents),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            stringResource(R.string.persona_documents_count, persona.linkedDocumentIds.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    TextButton(onClick = onManageDocuments) {
                        Text(stringResource(R.string.persona_manage_memories))
                    }
                }
            }
            
            // RAG Chunk Limit
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.persona_rag_chunk_limit), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        persona.ragChunkLimit.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = persona.ragChunkLimit.toFloat(),
                    onValueChange = { onUpdate(persona.copy(ragChunkLimit = it.toInt())) },
                    valueRange = AIConfig.MIN_RAG_CHUNK_LIMIT.toFloat()..AIConfig.MAX_RAG_CHUNK_LIMIT.toFloat(),
                    steps = 8
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentLinkDialog(
    persona: Persona,
    availableDocuments: List<com.yourown.ai.domain.model.KnowledgeDocument>,
    onDismiss: () -> Unit,
    onUpdate: (Persona) -> Unit
) {
    var linkedDocumentIds by remember { mutableStateOf(persona.linkedDocumentIds.toSet()) }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.document_link_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.document_link_close))
                    }
                }
                
                Text(
                    text = stringResource(R.string.document_link_subtitle, persona.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.document_link_selected_count, linkedDocumentIds.size, availableDocuments.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                HorizontalDivider()
                
                // Documents list
                if (availableDocuments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.document_link_no_documents),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.document_link_add_first),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableDocuments, key = { it.id }) { document ->
                            val isLinked = linkedDocumentIds.contains(document.id)
                            
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    linkedDocumentIds = if (isLinked) {
                                        linkedDocumentIds - document.id
                                    } else {
                                        linkedDocumentIds + document.id
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(
                                        checked = isLinked,
                                        onCheckedChange = {
                                            linkedDocumentIds = if (it) {
                                                linkedDocumentIds + document.id
                                            } else {
                                                linkedDocumentIds - document.id
                                            }
                                        }
                                    )
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = document.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = stringResource(R.string.document_size_kb, document.sizeBytes / 1024),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.document_link_cancel))
                    }
                    
                    Button(
                        onClick = {
                            val updated = persona.copy(linkedDocumentIds = linkedDocumentIds.toList())
                            onUpdate(updated)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.document_link_save))
                    }
                }
            }
        }
    }
}

@Composable
fun PersonaMemoriesDialog(
    personaId: String,
    personaName: String,
    viewModel: PersonaViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val memories = uiState.personaMemories[personaId] ?: emptyList()
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Memories",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "for \"$personaName\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close, 
                            "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (memories.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No memories yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Memories will appear here as the AI learns about this persona",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Memories list
                    Text(
                        text = "${memories.size} memories",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(memories, key = { it.id }) { memory ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = memory.fact,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Saved ${formatTimestamp(memory.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
        else -> "just now"
    }
}
