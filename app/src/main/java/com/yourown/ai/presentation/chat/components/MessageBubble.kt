package com.yourown.ai.presentation.chat.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.LaunchedEffect
import coil.compose.AsyncImage
import com.yourown.ai.domain.model.Message
import com.yourown.ai.domain.model.MessageRole
import java.text.SimpleDateFormat
import java.util.*

/**
 * Message bubble for chat
 */
@Composable
fun MessageBubble(
    message: Message,
    onLike: () -> Unit,
    onRegenerate: () -> Unit,
    onViewLogs: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onReply: () -> Unit = {},
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    isStreaming: Boolean = false
) {
    var showRegenerateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isUser = message.role == MessageRole.USER
    val isDark = isSystemInDarkTheme()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 340.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Message bubble
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = getMessageColor(isUser, isDark),
                tonalElevation = if (isUser) 2.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Swipe message preview (if this message is a reply)
                    if (message.swipeMessageId != null && message.swipeMessageText != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Replied to:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = message.swipeMessageText!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Image attachments (for user messages)
                    if (message.imageAttachments != null) {
                        val imagePaths = try {
                            com.google.gson.Gson().fromJson(
                                message.imageAttachments,
                                Array<String>::class.java
                            ).toList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                        
                        if (imagePaths.isNotEmpty()) {
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(imagePaths.size) { index ->
                                    val imagePath = imagePaths[index]
                                    AsyncImage(
                                        model = java.io.File(imagePath),
                                        contentDescription = "Attached image",
                                        modifier = Modifier
                                            .width(200.dp)
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    // File attachments (for user messages)
                    if (message.fileAttachments != null) {
                        val fileAttachments = try {
                            com.google.gson.Gson().fromJson(
                                message.fileAttachments,
                                Array<com.yourown.ai.domain.model.FileAttachment>::class.java
                            ).toList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                        
                        if (fileAttachments.isNotEmpty()) {
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(fileAttachments.size) { index ->
                                    val attachment = fileAttachments[index]
                                    Surface(
                                        modifier = Modifier.width(200.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        tonalElevation = 2.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // File icon
                                            Icon(
                                                imageVector = when (attachment.type) {
                                                    "pdf" -> Icons.Default.PictureAsPdf
                                                    "txt" -> Icons.Default.Description
                                                    "doc", "docx" -> Icons.Default.Article
                                                    else -> Icons.Default.InsertDriveFile
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp),
                                                tint = when (attachment.type) {
                                                    "pdf" -> MaterialTheme.colorScheme.error
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                            )
                                            
                                            // File info
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = attachment.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = String.format("%.1f MB", attachment.sizeBytes / (1024f * 1024f)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    if (message.isError) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // Markdown-aware text with clickable links and code blocks
                    RenderMessageContent(
                        content = message.content,
                        isUser = isUser,
                        searchQuery = searchQuery
                    )
                }
            }
            
            // Message metadata and actions - показываем только если НЕ стримится
            if (!isStreaming) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timestamp
                    Text(
                        text = formatTimestamp(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Copy button - for both user and assistant
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Reply button - for both user and assistant
                    IconButton(
                        onClick = onReply,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Reply,
                            contentDescription = "Reply",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Delete button - for both user and assistant
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Assistant message actions
                    if (!isUser) {
                        // Like button
                        IconButton(
                            onClick = onLike,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                if (message.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                modifier = Modifier.size(16.dp),
                                tint = if (message.isLiked) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        
                        // Regenerate button
                        IconButton(
                            onClick = { showRegenerateDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // View logs button
                        if (message.requestLogs != null) {
                            IconButton(
                                onClick = onViewLogs,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = "View Request Logs",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Regenerate confirmation dialog
    if (showRegenerateDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateDialog = false },
            title = { Text("Regenerate Response?") },
            text = { 
                Text("This will delete the current response and generate a new one. This action cannot be undone.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenerateDialog = false
                        onRegenerate()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Message?") },
            text = { 
                Text("This will permanently delete this message. This action cannot be undone.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Get message bubble color based on role and theme
 */
@Composable
private fun getMessageColor(isUser: Boolean, isDark: Boolean): Color {
    return if (isUser) {
        // User message: darker on light theme, lighter on dark theme
        if (isDark) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    } else {
        // Assistant message: background color
        MaterialTheme.colorScheme.background
    }
}

/**
 * Format timestamp to readable format
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    val isToday = calendar.get(Calendar.DAY_OF_YEAR) == messageCalendar.get(Calendar.DAY_OF_YEAR) &&
            calendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR)
    
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateTimeFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    return if (isToday) {
        timeFormat.format(Date(timestamp))
    } else {
        dateTimeFormat.format(Date(timestamp))
    }
}

/**
 * Render message content with separate code blocks
 */
@Composable
private fun RenderMessageContent(
    content: String,
    isUser: Boolean,
    searchQuery: String = ""
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    // Split content into text and code blocks
    data class ContentBlock(val content: String, val isCodeBlock: Boolean, val language: String = "")
    val blocks = mutableListOf<ContentBlock>()
    
    val lines = content.split("\n")
    var i = 0
    val currentTextBlock = StringBuilder()
    
    while (i < lines.size) {
        val line = lines[i]
        
        if (line.trimStart().startsWith("```")) {
            // Save accumulated text
            if (currentTextBlock.isNotEmpty()) {
                blocks.add(ContentBlock(currentTextBlock.toString(), false))
                currentTextBlock.clear()
            }
            
            // Extract language
            val language = line.trimStart().removePrefix("```").trim()
            Log.d("MessageBubble", "Found code block start, language: '$language'")
            i++
            
            // Collect code lines
            val codeLines = mutableListOf<String>()
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            
            val codeContent = codeLines.joinToString("\n")
            Log.d("MessageBubble", "Code block content length: ${codeContent.length}, language: '$language'")
            
            // Add code block
            blocks.add(ContentBlock(codeContent, true, language))
            i++ // Skip closing ```
        } else {
            currentTextBlock.append(line)
            if (i < lines.size - 1) {
                currentTextBlock.append("\n")
            }
            i++
        }
    }
    
    // Add remaining text
    if (currentTextBlock.isNotEmpty()) {
        blocks.add(ContentBlock(currentTextBlock.toString(), false))
    }
    
    // Render blocks
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            if (block.isCodeBlock) {
                // Render code block with copy button
                CodeBlock(
                    code = block.content,
                    language = block.language,
                    isUser = isUser,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(block.content))
                    }
                )
            } else {
                // Render text with markdown
                var annotatedText = parseMarkdownWithoutCodeBlocks(block.content, isUser)
                
                // Apply search highlighting if search is active
                if (searchQuery.isNotBlank() && block.content.contains(searchQuery, ignoreCase = true)) {
                    annotatedText = highlightSearchQuery(annotatedText, searchQuery)
                }
                
                ClickableText(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyLarge,
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle invalid URL
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Code block with copy button
 */
@Composable
private fun CodeBlock(
    code: String,
    language: String,
    isUser: Boolean,
    onCopy: () -> Unit
) {
    var showCopied by remember { mutableStateOf(false) }
    
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val codeTextColor = if (isUser) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    
    // Reset copied state
    if (showCopied) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showCopied = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        // Header: language + copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (language.isNotEmpty()) language else "code",
                style = MaterialTheme.typography.labelMedium,
                color = codeTextColor.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            
            // Copy button with pointerInput - bypasses parent touch interception
            Box(
                modifier = Modifier
                    .background(
                        color = codeTextColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .pointerInput(code) {
                        detectTapGestures(
                            onTap = {
                                Log.d("CodeBlock", "TAP DETECTED! code length: ${code.length}")
                                onCopy()
                                showCopied = true
                            }
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        modifier = Modifier.size(14.dp),
                        tint = if (showCopied) MaterialTheme.colorScheme.primary 
                               else codeTextColor
                    )
                    Text(
                        text = if (showCopied) "Copied!" else "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (showCopied) MaterialTheme.colorScheme.primary 
                               else codeTextColor
                    )
                }
            }
        }
        
        HorizontalDivider(
            color = codeTextColor.copy(alpha = 0.1f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        // Code content - wrap text
        Text(
            text = code,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = codeTextColor,
                fontSize = MaterialTheme.typography.bodySmall.fontSize
            ),
            softWrap = true
        )
    }
}

/**
 * Parse markdown to AnnotatedString with support for:
 * - **bold text**
 * - *italic*
 * - `inline code`
 * - [text](url)
 * - **[bold link](url)**
 * - > blockquote
 * - # Heading 1-6
 * - --- horizontal divider
 * (Code blocks are handled separately)
 */
@Composable
private fun parseMarkdownWithoutCodeBlocks(text: String, isUser: Boolean): AnnotatedString {
    val linkColor = if (isUser) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val quoteColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val quoteBorderColor = if (isUser) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    }
    
    val codeBackgroundColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val codeTextColor = if (isUser) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    
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
            
            // Check for headings (# H1-H6)
            val headingMatch = Regex("^(#{1,6})\\s+(.+)$").find(trimmedLine)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                var headingText = headingMatch.groupValues[2]
                
                // Clean markdown formatting from heading text
                headingText = headingText
                    .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")  // Remove **bold**
                    .replace(Regex("\\*(.+?)\\*"), "$1")        // Remove *italic*
                    .replace(Regex("`(.+?)`"), "$1")            // Remove `code`
                    .replace(Regex("\\[(.+?)\\]\\(.+?\\)"), "$1") // Remove [link](url) - keep text only
                
                val headingSize = when (level) {
                    1 -> 1.5f  // H1
                    2 -> 1.3f  // H2
                    3 -> 1.15f // H3
                    4 -> 1.1f  // H4
                    5 -> 1.05f // H5
                    else -> 1.0f // H6
                }
                
                withStyle(SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * headingSize,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
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
            
            // Regular expressions (order is important!)
            val boldLinkRegex = """\*\*\[([^\]]+)\]\(([^\)]+)\)\*\*""".toRegex()  // **[text](url)**
            val linkRegex = """\[([^\]]+)\]\(([^\)]+)\)""".toRegex()  // [text](url)
            val boldRegex = """\*\*(.+?)\*\*""".toRegex()  // **text**
            val italicRegex = """\*([^*]+?)\*""".toRegex()  // *text*
            val inlineCodeRegex = """`([^`]+?)`""".toRegex()  // `code`

            // Find all matches
            val matches = mutableListOf<Triple<IntRange, String, MatchResult>>()

            // Important: first bold+link, then links, then inline code, then bold, then italic
            boldLinkRegex.findAll(processLine).forEach {
                matches.add(Triple(it.range, "boldlink", it))
            }
            linkRegex.findAll(processLine).forEach {
                matches.add(Triple(it.range, "link", it))
            }
            inlineCodeRegex.findAll(processLine).forEach {
                matches.add(Triple(it.range, "inlinecode", it))
            }
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
                        // **[text](url)** - bold link
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
                    "inlinecode" -> {
                        // `code` - inline code
                        val codeText = match.groupValues[1]
                        withStyle(SpanStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = codeTextColor,
                            background = codeBackgroundColor,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize
                        )) {
                            append(codeText)
                        }
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
 * Highlights search query in annotated text
 */
private fun highlightSearchQuery(
    annotatedText: AnnotatedString,
    searchQuery: String
): AnnotatedString {
    if (searchQuery.isBlank()) return annotatedText
    
    val text = annotatedText.text
    val builder = AnnotatedString.Builder(annotatedText)
    
    var startIndex = 0
    while (startIndex < text.length) {
        val index = text.indexOf(searchQuery, startIndex, ignoreCase = true)
        if (index == -1) break
        
        // Add yellow background highlight
        builder.addStyle(
            style = SpanStyle(
                background = Color(0xFFFFEB3B), // Yellow highlight
                color = Color(0xFF000000) // Black text for contrast
            ),
            start = index,
            end = index + searchQuery.length
        )
        
        startIndex = index + searchQuery.length
    }
    
    return builder.toAnnotatedString()
}
