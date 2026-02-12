package com.yourown.ai.presentation.chat.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R
import com.yourown.ai.domain.model.Message

/**
 * Dialog for exporting chat with markdown rendering and like filter
 */
@Composable
fun ExportChatDialog(
    chatText: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSaveToFile: () -> Unit = {},
    onFilterChanged: (Boolean) -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }
    var filterByLikes by remember { mutableStateOf(false) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    Dialog(onDismissRequest = onDismiss) {
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
                        text = "Export chat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close, 
                            "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Filter by likes checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            filterByLikes = !filterByLikes
                            onFilterChanged(filterByLikes)
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = filterByLikes,
                        onCheckedChange = {
                            filterByLikes = it
                            onFilterChanged(it)
                        }
                    )
                    Text(
                        text = "Export only liked messages",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Info text
                Text(
                    text = if (filterByLikes) {
                        "Showing only liked messages"
                    } else {
                        "Chat exported with markdown formatting"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Chat content preview with markdown rendering
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    val scrollState = rememberScrollState()
                    val annotatedText = parseMarkdownForExport(chatText)
                    
                    ClickableText(
                        text = annotatedText,
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(scrollState),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    try {
                                        uriHandler.openUri(annotation.item)
                                    } catch (e: Exception) {
                                        // Handle invalid URL
                                    }
                                }
                        }
                    )
                }
                
                // Copied snackbar
                if (showCopiedSnackbar) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        showCopiedSnackbar = false
                    }
                    Text(
                        text = "✓ Copied to clipboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Warning for large chats
                if (chatText.length > 15000) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Large chat (${chatText.length} chars). Use 'Save to File' for import.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                
                // Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(chatText))
                                showCopiedSnackbar = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.export_copy))
                        }
                        
                        Button(
                            onClick = onShare,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.export_share))
                        }
                    }
                    
                    // Save to File button (for large chats)
                    Button(
                        onClick = onSaveToFile,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.export_save_to_file))
                    }
                }
            }
        }
    }
}

/**
 * Parse markdown for export dialog (simplified version for preview)
 * Supports: bold, italic, links, headings, blockquotes, horizontal rules
 */
@Composable
private fun parseMarkdownForExport(text: String): AnnotatedString {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary
    val quoteColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val quoteBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    
    return buildAnnotatedString {
        val lines = text.split("\n")

        lines.forEachIndexed { lineIndex, line ->
            val trimmedLine = line.trimStart()
            
            // Check for horizontal divider (---, ***, ___)
            if (trimmedLine.matches(Regex("^(---|\\*\\*\\*|___)\\s*$"))) {
                withStyle(SpanStyle(color = quoteBorderColor)) {
                    append("─".repeat(20))
                }
                if (lineIndex < lines.size - 1) {
                    append("\n")
                }
                return@forEachIndexed
            }
            
            // Check for headings (# H1, ## H2, ### H3)
            val headingMatch = Regex("^(#{1,3})\\s+(.+)$").find(trimmedLine)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val headingText = headingMatch.groupValues[2]
                
                val headingSize = when (level) {
                    1 -> 1.5f // H1
                    2 -> 1.3f // H2
                    else -> 1.15f // H3
                }
                
                withStyle(SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * headingSize,
                    color = MaterialTheme.colorScheme.primary
                )) {
                    append(headingText)
                }
                
                if (lineIndex < lines.size - 1) {
                    append("\n")
                }
                return@forEachIndexed
            }
            
            // Check for blockquote (> text)
            val isQuote = trimmedLine.startsWith(">")
            val processLine = if (isQuote) {
                trimmedLine.removePrefix(">").trimStart()
            } else {
                line
            }
            
            // Add quote indicator
            if (isQuote) {
                withStyle(SpanStyle(color = quoteBorderColor)) {
                    append("▌ ")
                }
            }
            
            // Regular expressions for markdown
            val boldLinkRegex = """\*\*\[([^\]]+)\]\(([^\)]+)\)\*\*""".toRegex()
            val linkRegex = """\[([^\]]+)\]\(([^\)]+)\)""".toRegex()
            val boldRegex = """\*\*(.+?)\*\*""".toRegex()
            val italicRegex = """\*([^*]+?)\*""".toRegex()

            // Find all matches
            val matches = mutableListOf<Triple<IntRange, String, MatchResult>>()
            boldLinkRegex.findAll(processLine).forEach { matches.add(Triple(it.range, "boldlink", it)) }
            linkRegex.findAll(processLine).forEach { matches.add(Triple(it.range, "link", it)) }
            boldRegex.findAll(processLine).forEach { matches.add(Triple(it.range, "bold", it)) }
            italicRegex.findAll(processLine).forEach { matches.add(Triple(it.range, "italic", it)) }

            // Remove overlapping matches
            val filteredMatches = mutableListOf<Triple<IntRange, String, MatchResult>>()
            matches.sortedBy { it.first.first }.forEach { current ->
                val hasOverlap = filteredMatches.any { existing ->
                    current.first.first < existing.first.last && current.first.last > existing.first.first
                }
                if (!hasOverlap) {
                    filteredMatches.add(current)
                }
            }

            var lastIndex = 0
            filteredMatches.forEach { (range, type, match) ->
                // Add text before match
                if (lastIndex < range.first) {
                    withStyle(SpanStyle(color = if (isQuote) quoteColor else textColor)) {
                        append(processLine.substring(lastIndex, range.first))
                    }
                }

                when (type) {
                    "boldlink" -> {
                        val label = match.groupValues[1]
                        val url = match.groupValues[2]
                        val start = length
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = if (isQuote) quoteColor else linkColor,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append(label)
                        }
                        addStringAnnotation(
                            tag = "URL",
                            annotation = url,
                            start = start,
                            end = start + label.length
                        )
                    }
                    "bold" -> {
                        val innerText = match.groupValues[1]
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = if (isQuote) quoteColor else textColor
                        )) {
                            append(innerText)
                        }
                    }
                    "italic" -> {
                        val innerText = match.groupValues[1]
                        withStyle(SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = if (isQuote) quoteColor.copy(alpha = 0.9f) else textColor.copy(alpha = 0.8f)
                        )) {
                            append(innerText)
                        }
                    }
                    "link" -> {
                        val label = match.groupValues[1]
                        val url = match.groupValues[2]
                        val start = length
                        withStyle(SpanStyle(
                            color = if (isQuote) quoteColor else linkColor,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append(label)
                        }
                        addStringAnnotation(
                            tag = "URL",
                            annotation = url,
                            start = start,
                            end = start + label.length
                        )
                    }
                }

                lastIndex = range.last + 1
            }

            // Rest of the line
            if (lastIndex < processLine.length) {
                withStyle(SpanStyle(color = if (isQuote) quoteColor else textColor)) {
                    append(processLine.substring(lastIndex))
                }
            }

            // Line break (except for last line)
            if (lineIndex < lines.size - 1) {
                append("\n")
            }
        }
    }
}

/**
 * Dialog for importing chat from text
 */
@Composable
fun ImportChatDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    onLoadFromFile: () -> Unit = {},
    errorMessage: String? = null
) {
    var chatText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(stringResource(R.string.import_dialog_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Paste exported chat text below:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = chatText,
                    onValueChange = { chatText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    placeholder = { Text(stringResource(R.string.import_placeholder)) },
                    maxLines = Int.MAX_VALUE, // No limit on lines
                    singleLine = false,
                    isError = errorMessage != null,
                    supportingText = {
                        Text(
                            text = "${chatText.length} characters, ${chatText.lines().size} lines",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                
                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                
                // Warning about clipboard limit
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Clipboard limit: 20,000 characters",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Text(
                            text = "For large chats, use 'Load from File' button below",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                
                // Load from file button
                OutlinedButton(
                    onClick = onLoadFromFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.import_load_from_file))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (chatText.isNotBlank()) {
                        onImport(chatText)
                    }
                },
                enabled = chatText.isNotBlank()
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.import_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.import_cancel))
            }
        }
    )
}

/**
 * Dialog for selecting source chat to inherit context from
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceChatSelectionDialog(
    conversations: List<com.yourown.ai.domain.model.Conversation>,
    selectedSourceChatId: String?,
    personas: List<com.yourown.ai.domain.model.Persona> = emptyList(),
    selectedPersonaId: String? = null,
    onSourceChatSelected: (String?) -> Unit,
    onPersonaSelected: (String?) -> Unit = {},
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expandedChat by remember { mutableStateOf(false) }
    var expandedPersona by remember { mutableStateOf(false) }
    
    // Filter out archived conversations and sort by most recent
    val availableChats = remember(conversations) {
        conversations
            .filter { !it.isArchived }
            .sortedByDescending { it.updatedAt }
    }
    
    // Find selected conversation title
    val selectedChatTitle = remember(selectedSourceChatId, availableChats) {
        if (selectedSourceChatId == null) {
            "None (start fresh)"
        } else {
            availableChats.find { it.id == selectedSourceChatId }?.title ?: "None (start fresh)"
        }
    }
    
    // Find selected persona name
    val selectedPersonaName = remember(selectedPersonaId, personas) {
        if (selectedPersonaId == null) {
            "None (Default)"
        } else {
            personas.find { it.id == selectedPersonaId }?.name ?: "None (Default)"
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.inherit_context_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select a chat to inherit message history from. " +
                           "The last ${10} message pairs will be loaded into context.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Dropdown for Persona selection
                ExposedDropdownMenuBox(
                    expanded = expandedPersona,
                    onExpandedChange = { expandedPersona = !expandedPersona }
                ) {
                    OutlinedTextField(
                        value = selectedPersonaName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.inherit_context_persona_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPersona) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedPersona,
                        onDismissRequest = { expandedPersona = false }
                    ) {
                        // Option: None (Default)
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(
                                        "None (Default)",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "Use global settings",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onPersonaSelected(null)
                                expandedPersona = false
                            },
                            leadingIcon = {
                                if (selectedPersonaId == null) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        
                        if (personas.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            // Available personas
                            personas.forEach { persona ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                persona.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (persona.description.isNotEmpty()) {
                                                Text(
                                                    persona.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onPersonaSelected(persona.id)
                                        expandedPersona = false
                                    },
                                    leadingIcon = {
                                        if (selectedPersonaId == persona.id) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Dropdown for source chat selection
                ExposedDropdownMenuBox(
                    expanded = expandedChat,
                    onExpandedChange = { expandedChat = !expandedChat }
                ) {
                    OutlinedTextField(
                        value = selectedChatTitle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.inherit_context_source_chat_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedChat) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedChat,
                        onDismissRequest = { expandedChat = false }
                    ) {
                        // Option: None (start fresh)
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(
                                        "None (start fresh)",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "No inherited context",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onSourceChatSelected(null)
                                expandedChat = false
                            },
                            leadingIcon = {
                                if (selectedSourceChatId == null) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        // Available chats
                        availableChats.forEach { chat ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            chat.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${chat.model} • ${chat.messages.size} messages",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onSourceChatSelected(chat.id)
                                    expandedChat = false
                                },
                                leadingIcon = {
                                    if (selectedSourceChatId == chat.id) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.inherit_context_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.inherit_context_cancel))
            }
        }
    )
}
