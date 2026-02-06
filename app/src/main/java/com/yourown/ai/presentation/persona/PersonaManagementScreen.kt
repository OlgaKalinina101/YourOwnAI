package com.yourown.ai.presentation.persona

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourown.ai.domain.model.Persona

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaManagementScreen(
    viewModel: PersonaViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onEditPersona: (Persona) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Default.Add, "Create Persona")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.personas.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Personas Yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Create a persona to customize AI behavior",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Persona")
                    }
                }
            } else {
                // Persona list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.personas,
                        key = { it.id }
                    ) { persona ->
                        PersonaCard(
                            persona = persona,
                            onEdit = { onEditPersona(persona) },
                            onDelete = { viewModel.showDeleteConfirmation(persona) }
                        )
                    }
                }
            }
            
            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
    
    // Dialogs
    if (uiState.showCreateDialog) {
        CreatePersonaDialog(
            systemPrompts = uiState.systemPrompts,
            existingPersonaPromptIds = uiState.personas.map { it.systemPromptId }.toSet(),
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { systemPromptId, description, isForApi ->
                viewModel.createPersonaFromSystemPrompt(systemPromptId, description, isForApi)
            }
        )
    }
    
    if (uiState.showDeleteConfirmation && uiState.personaToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirmation() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Delete Persona?") },
            text = { 
                Text("Are you sure you want to delete \"${uiState.personaToDelete?.name}\"? This action cannot be undone.") 
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deletePersona(uiState.personaToDelete!!) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Error snackbar
    if (uiState.error != null) {
        LaunchedEffect(uiState.error) {
            // Show snackbar or toast
            viewModel.clearError()
        }
    }
}

@Composable
fun PersonaCard(
    persona: Persona,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = persona.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Model badge
                        if (persona.preferredModelId != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = persona.preferredModelId!!,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    
                    if (persona.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = persona.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Settings preview
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (persona.deepEmpathy) {
                    SettingChip(icon = Icons.Default.FavoriteBorder, label = "Deep Empathy")
                }
                if (persona.memoryEnabled) {
                    SettingChip(icon = Icons.Default.CheckCircle, label = "Memory")
                }
                if (persona.ragEnabled) {
                    SettingChip(icon = Icons.Default.Info, label = "RAG")
                }
                if (persona.linkedDocumentIds.isNotEmpty()) {
                    SettingChip(
                        icon = Icons.Default.Info, 
                        label = "${persona.linkedDocumentIds.size} docs"
                    )
                }
            }
        }
    }
}

@Composable
fun SettingChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreatePersonaDialog(
    systemPrompts: List<com.yourown.ai.domain.model.SystemPrompt>,
    existingPersonaPromptIds: Set<String>,
    onDismiss: () -> Unit,
    onCreate: (systemPromptId: String, description: String, isForApi: Boolean) -> Unit
) {
    var selectedPromptId by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    
    // Фильтруем SystemPrompts, для которых еще нет Persona (только API, не default)
    val availablePrompts = systemPrompts.filter { 
        it.id !in existingPersonaPromptIds && 
        it.type.value == "api" && 
        !it.isDefault 
    }
    
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Persona",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                Text(
                    text = "Select a system prompt to create a persona with custom settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider()
                
                if (availablePrompts.isEmpty()) {
                    // No available prompts
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "All system prompts already have personas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // SystemPrompts list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availablePrompts) { prompt ->
                            SystemPromptSelectItem(
                                prompt = prompt,
                                isSelected = selectedPromptId == prompt.id,
                                onClick = { selectedPromptId = prompt.id }
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // Description only
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            selectedPromptId?.let { id ->
                                // Автоматически определяем isForApi из выбранного SystemPrompt
                                val selectedPrompt = availablePrompts.find { it.id == id }
                                val isForApi = selectedPrompt?.type?.value == "api"
                                onCreate(id, description, isForApi)
                            }
                        },
                        enabled = selectedPromptId != null
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemPromptSelectItem(
    prompt: com.yourown.ai.domain.model.SystemPrompt,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = prompt.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (prompt.isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "Default",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = prompt.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
