package com.yourown.ai.presentation.chat.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourown.ai.util.FileProcessor

/**
 * Preview of attached files with ability to remove them
 */
@Composable
fun AttachedFilesPreview(
    files: List<Uri>,
    onRemoveFile: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    if (files.isEmpty()) return
    
    val context = LocalContext.current
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files) { uri ->
                FilePreviewItem(
                    uri = uri,
                    context = context,
                    onRemove = { onRemoveFile(uri) }
                )
            }
        }
    }
}

@Composable
private fun FilePreviewItem(
    uri: Uri,
    context: android.content.Context,
    onRemove: () -> Unit
) {
    val fileInfo = FileProcessor.getFileInfo(context, uri)
    val fileName = fileInfo?.first ?: "Unknown"
    val fileSize = fileInfo?.second ?: 0L
    val fileSizeMB = fileSize / (1024f * 1024f)
    val extension = FileProcessor.getFileExtension(fileName)
    
    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(80.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File icon
                Icon(
                    imageVector = when (extension) {
                        "pdf" -> Icons.Default.PictureAsPdf
                        "txt" -> Icons.Default.Description
                        "doc", "docx" -> Icons.Default.Article
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = when (extension) {
                        "pdf" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                
                // File info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format("%.1f MB", fileSizeMB),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Remove button
            FilledIconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove file",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
