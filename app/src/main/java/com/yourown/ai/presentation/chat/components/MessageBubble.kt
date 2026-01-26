package com.yourown.ai.presentation.chat.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val isDark = isSystemInDarkTheme()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
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
                    
                    // Markdown-aware text with clickable links
                    val context = LocalContext.current
                    val annotatedText = parseMarkdown(message.content, isUser)
                    
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
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Message metadata and actions
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
                
                // Delete button - for both user and assistant
                IconButton(
                    onClick = onDelete,
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
                        onClick = onRegenerate,
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
 * Parse markdown to AnnotatedString with support for:
 * - **bold text**
 * - *italic*
 * - [text](url)
 * - **[bold link](url)**
 * - > blockquote
 */
@Composable
private fun parseMarkdown(text: String, isUser: Boolean): AnnotatedString {
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
    
    return buildAnnotatedString {
        val lines = text.split("\n")

        lines.forEachIndexed { lineIndex, line ->
            // Check for blockquote (> text)
            val isQuote = line.trimStart().startsWith(">")
            val processLine = if (isQuote) {
                line.trimStart().removePrefix(">").trimStart()
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

            // Find all matches
            val matches = mutableListOf<Triple<IntRange, String, MatchResult>>()

            // Important: first bold+link, then links, then bold, then italic
            boldLinkRegex.findAll(processLine).forEach {
                matches.add(Triple(it.range, "boldlink", it))
            }
            linkRegex.findAll(processLine).forEach {
                matches.add(Triple(it.range, "link", it))
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
