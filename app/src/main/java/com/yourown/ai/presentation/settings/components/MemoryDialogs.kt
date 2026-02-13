package com.yourown.ai.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yourown.ai.domain.model.MemoryEntry
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R

/**
 * Dialog to view all saved memories
 */
@Composable
fun MemoriesDialog(
    memories: List<MemoryEntry>,
    personas: List<com.yourown.ai.domain.model.Persona> = emptyList(),
    processingStatus: com.yourown.ai.data.repository.MemoryProcessingStatus = com.yourown.ai.data.repository.MemoryProcessingStatus.Idle,
    onDismiss: () -> Unit,
    onEditMemory: (MemoryEntry) -> Unit,
    onDeleteMemory: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                    Text(
                        text = stringResource(R.string.memory_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close, 
                            stringResource(R.string.memory_dialog_close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Memory count
                Text(
                    text = stringResource(R.string.memory_dialog_count, memories.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Processing status indicator
                when (processingStatus) {
                    is com.yourown.ai.data.repository.MemoryProcessingStatus.Recalculating -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.memory_dialog_recalculating),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = processingStatus.currentStep,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.memory_dialog_progress, processingStatus.progress),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = processingStatus.progress / 100f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    is com.yourown.ai.data.repository.MemoryProcessingStatus.Completed -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.memory_dialog_completed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    is com.yourown.ai.data.repository.MemoryProcessingStatus.Failed -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.memory_dialog_failed),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = processingStatus.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    else -> {}
                }
                
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
                                text = stringResource(R.string.memory_dialog_empty),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.memory_dialog_empty_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // Memories list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(memories) { memory ->
                            val linkedPersona = memory.personaId?.let { personaId ->
                                personas.find { it.id == personaId }
                            }
                            val isBeingProcessed = processingStatus is com.yourown.ai.data.repository.MemoryProcessingStatus.SavingMemory &&
                                                    (processingStatus as? com.yourown.ai.data.repository.MemoryProcessingStatus.SavingMemory)?.memoryId == memory.id
                            MemoryItem(
                                memory = memory,
                                linkedPersona = linkedPersona,
                                isProcessing = isBeingProcessed,
                                onEdit = { onEditMemory(memory) },
                                onDelete = { onDeleteMemory(memory.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single memory item
 */
@Composable
fun MemoryItem(
    memory: MemoryEntry,
    linkedPersona: com.yourown.ai.domain.model.Persona? = null,
    isProcessing: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isProcessing) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Processing indicator
            if (isProcessing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(R.string.memory_item_generating),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Date and Persona Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(memory.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Persona Badge
                    Surface(
                        color = if (linkedPersona != null) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = linkedPersona?.name ?: stringResource(R.string.memory_item_for_all),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = if (linkedPersona != null) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                }
                
                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp),
                        enabled = !isProcessing
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.memory_item_edit),
                            modifier = Modifier.size(18.dp),
                            tint = if (isProcessing) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(32.dp),
                        enabled = !isProcessing
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.memory_item_delete),
                            modifier = Modifier.size(18.dp),
                            tint = if (isProcessing) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Memory fact
            Text(
                text = memory.fact,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.memory_item_delete_title)) },
            text = { Text(stringResource(R.string.memory_item_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.memory_item_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.memory_item_delete_cancel))
                }
            }
        )
    }
}

/**
 * Dialog to edit a memory
 */
@Composable
fun EditMemoryDialog(
    memory: MemoryEntry,
    personas: List<com.yourown.ai.domain.model.Persona> = emptyList(),
    processingStatus: com.yourown.ai.data.repository.MemoryProcessingStatus = com.yourown.ai.data.repository.MemoryProcessingStatus.Idle,
    onDismiss: () -> Unit,
    onSave: (fact: String, personaId: String?) -> Unit
) {
    var editedFact by remember { mutableStateOf(memory.fact) }
    var selectedPersonaId by remember { mutableStateOf(memory.personaId) }
    var showPersonaMenu by remember { mutableStateOf(false) }
    
    val isSaving = processingStatus is com.yourown.ai.data.repository.MemoryProcessingStatus.SavingMemory &&
                   (processingStatus as? com.yourown.ai.data.repository.MemoryProcessingStatus.SavingMemory)?.memoryId == memory.id
    
    // Auto-close on completion
    LaunchedEffect(processingStatus) {
        if (processingStatus is com.yourown.ai.data.repository.MemoryProcessingStatus.Completed) {
            kotlinx.coroutines.delay(500) // Короткая задержка чтобы пользователь увидел успех
            onDismiss()
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.memory_edit_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.memory_edit_created, formatDate(memory.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Memory text
                OutlinedTextField(
                    value = editedFact,
                    onValueChange = { editedFact = it },
                    label = { Text(stringResource(R.string.memory_edit_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Persona selection
                if (personas.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.memory_edit_link_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box {
                        OutlinedButton(
                            onClick = { showPersonaMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val selectedPersona = personas.find { it.id == selectedPersonaId }
                            Text(
                                text = selectedPersona?.name ?: stringResource(R.string.memory_edit_no_persona),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        
                        DropdownMenu(
                            expanded = showPersonaMenu,
                            onDismissRequest = { showPersonaMenu = false }
                        ) {
                            // "For all" option
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.memory_edit_no_persona)) },
                                onClick = {
                                    selectedPersonaId = null
                                    showPersonaMenu = false
                                },
                                leadingIcon = {
                                    if (selectedPersonaId == null) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                            
                            Divider()
                            
                            // Personas
                            personas.forEach { persona ->
                                DropdownMenuItem(
                                    text = { Text(persona.name) },
                                    onClick = {
                                        selectedPersonaId = persona.id
                                        showPersonaMenu = false
                                    },
                                    leadingIcon = {
                                        if (selectedPersonaId == persona.id) {
                                            Icon(Icons.Default.Check, null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Processing indicator
                if (isSaving) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = stringResource(R.string.memory_edit_generating),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text(stringResource(R.string.memory_edit_cancel))
                    }
                    
                    Button(
                        onClick = { onSave(editedFact, selectedPersonaId) },
                        enabled = editedFact.isNotBlank() && !isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.memory_edit_save))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog to edit memory extraction prompt
 */
@Composable
fun MemoryExtractionPromptDialog(
    currentPrompt: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    // Split prompt into editable and locked parts
    // Locked part: only the last line with format instruction
    val lines = currentPrompt.lines()
    val lockedLinesCount = 1 // Only "Верни только одну строку: ..."
    val editableLines = lines.dropLast(lockedLinesCount)
    val lockedLines = lines.takeLast(lockedLinesCount)
    
    var editedPrompt by remember { mutableStateOf(editableLines.joinToString("\n")) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val requiredPlaceholder = "{text}"
    val hasPlaceholder = editedPrompt.contains(requiredPlaceholder)
    
    // Reconstruct full prompt
    val fullPrompt = editedPrompt + "\n\n" + lockedLines.joinToString("\n")
    
    Dialog(onDismissRequest = onDismiss) {
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
                    Text(
                        text = stringResource(R.string.memory_prompt_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close, 
                            stringResource(R.string.memory_dialog_close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.memory_prompt_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Placeholder warning
                if (!hasPlaceholder) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(R.string.memory_prompt_placeholder_warning, requiredPlaceholder),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Editable prompt
                OutlinedTextField(
                    value = editedPrompt,
                    onValueChange = { editedPrompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text(stringResource(R.string.memory_prompt_editable_label)) },
                    placeholder = { Text(stringResource(R.string.memory_prompt_editable_placeholder, requiredPlaceholder)) },
                    minLines = 8,
                    textStyle = MaterialTheme.typography.bodySmall,
                    isError = !hasPlaceholder,
                    supportingText = if (!hasPlaceholder) {
                        { Text(stringResource(R.string.memory_prompt_error, requiredPlaceholder), color = MaterialTheme.colorScheme.error) }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Locked section (Format instructions and examples)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.memory_prompt_locked_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = lockedLines.joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.memory_prompt_reset))
                    }
                    
                    Button(
                        onClick = { onSave(fullPrompt) },
                        modifier = Modifier.weight(1f),
                        enabled = editedPrompt.isNotBlank() && hasPlaceholder
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.memory_prompt_save))
                    }
                }
            }
        }
    }
    
    // Reset confirmation dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.memory_prompt_reset_title)) },
            text = { Text(stringResource(R.string.memory_prompt_reset_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetConfirm = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.memory_prompt_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.memory_prompt_reset_cancel))
                }
            }
        )
    }
}

/**
 * Format timestamp to readable date
 */
private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
