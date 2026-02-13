package com.yourown.ai.presentation.settings.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yourown.ai.R
import androidx.compose.ui.res.stringResource

/**
 * Dialog with step-by-step instructions for Supabase setup (Russian)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncInstructionsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var fullScreenImageRes by remember { mutableStateOf<Int?>(null) }
    var showCopiedMessage by remember { mutableStateOf(false) }
    
    // Auto-hide "copied" message after 2 seconds
    LaunchedEffect(showCopiedMessage) {
        if (showCopiedMessage) {
            kotlinx.coroutines.delay(2000)
            showCopiedMessage = false
        }
    }
    
    // Full-screen image dialog with zoom support
    if (fullScreenImageRes != null) {
        Dialog(
            onDismissRequest = { fullScreenImageRes = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            
            val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 5f)
                
                // Only allow panning when zoomed in
                if (scale > 1f) {
                    offset = offset + offsetChange
                }
            }
            
            // Reset on image change or double tap
            LaunchedEffect(fullScreenImageRes) {
                scale = 1f
                offset = Offset.Zero
            }
            
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = fullScreenImageRes!!),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .transformable(state = state),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Close button
                    IconButton(
                        onClick = { fullScreenImageRes = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cloud_sync_instructions_close),
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Zoom hint (only show when scale is 1)
                    if (scale == 1f) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.cloud_sync_instructions_zoom_hint),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        icon = { Icon(Icons.Default.Help, null) },
        title = { Text(stringResource(R.string.cloud_sync_instructions_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Step 1
                InstructionStepWithImage(
                    stepNumber = 1,
                    title = stringResource(R.string.cloud_sync_instructions_step1_title),
                    description = stringResource(R.string.cloud_sync_instructions_step1_desc),
                    imageRes = R.drawable.step1_create_suprabase_account,
                    onImageClick = { fullScreenImageRes = it },
                    clickableUrl = "https://supabase.com",
                    context = context,
                    onUrlCopied = { showCopiedMessage = true }
                )
                
                // Copied message snackbar
                if (showCopiedMessage) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.cloud_sync_instructions_url_copied),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Step 2
                InstructionStepWithImage(
                    stepNumber = 2,
                    title = stringResource(R.string.cloud_sync_instructions_step2_title),
                    description = stringResource(R.string.cloud_sync_instructions_step2_desc),
                    imageRes = R.drawable.step2_create_organization,
                    onImageClick = { fullScreenImageRes = it }
                )
                
                // Step 3
                InstructionStepWithImage(
                    stepNumber = 3,
                    title = stringResource(R.string.cloud_sync_instructions_step3_title),
                    description = stringResource(R.string.cloud_sync_instructions_step3_desc),
                    imageRes = R.drawable.step3_name_your_organization,
                    onImageClick = { fullScreenImageRes = it }
                )
                
                // Step 4
                InstructionStepWithImage(
                    stepNumber = 4,
                    title = stringResource(R.string.cloud_sync_instructions_step4_title),
                    description = stringResource(R.string.cloud_sync_instructions_step4_desc),
                    imageRes = R.drawable.step4_organization_setup,
                    onImageClick = { fullScreenImageRes = it }
                )
                
                // Step 5
                InstructionStepWithImage(
                    stepNumber = 5,
                    title = stringResource(R.string.cloud_sync_instructions_step5_title),
                    description = stringResource(R.string.cloud_sync_instructions_step5_desc),
                    imageRes = R.drawable.step5_copy_credentials,
                    onImageClick = { fullScreenImageRes = it }
                )
                
                // Step 6
                InstructionStepWithImage(
                    stepNumber = 6,
                    title = stringResource(R.string.cloud_sync_instructions_step6_title),
                    description = stringResource(R.string.cloud_sync_instructions_step6_desc),
                    imageRes = R.drawable.step6_go_to_sql_editor,
                    onImageClick = { fullScreenImageRes = it }
                )
                
                // Step 7 with SQL copy button
                InstructionStepWithSqlQuery(
                    stepNumber = 7,
                    title = stringResource(R.string.cloud_sync_instructions_step7_title),
                    description = stringResource(R.string.cloud_sync_instructions_step7_desc),
                    imageRes = R.drawable.step7_run_sql_query,
                    context = context,
                    onImageClick = { fullScreenImageRes = it }
                )
                
                // Step 8
                InstructionStepWithImage(
                    stepNumber = 8,
                    title = stringResource(R.string.cloud_sync_instructions_step8_title),
                    description = stringResource(R.string.cloud_sync_instructions_step8_desc),
                    imageRes = R.drawable.step8_sql_query_successfully,
                    onImageClick = { fullScreenImageRes = it }
                )
                
                // Step 9
                InstructionStepWithImage(
                    stepNumber = 9,
                    title = stringResource(R.string.cloud_sync_instructions_step9_title),
                    description = stringResource(R.string.cloud_sync_instructions_step9_desc),
                    imageRes = R.drawable.clous_sync,
                    onImageClick = { fullScreenImageRes = it }
                )
                
                // Step 10
                InstructionStepWithImage(
                    stepNumber = 10,
                    title = stringResource(R.string.cloud_sync_instructions_step10_title),
                    description = stringResource(R.string.cloud_sync_instructions_step10_desc),
                    imageRes = R.drawable.test_connection,
                    onImageClick = { fullScreenImageRes = it }
                )
                
                // Success message
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = stringResource(R.string.cloud_sync_instructions_success),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Divider()
                
                // Info cards
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cloud_sync_instructions_data_tracking_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = stringResource(R.string.cloud_sync_instructions_data_tracking_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cloud_sync_instructions_pricing_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.cloud_sync_instructions_pricing_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cloud_sync_instructions_ok))
            }
        }
    )
}

/**
 * Instruction step with image
 */
@Composable
private fun InstructionStepWithImage(
    stepNumber: Int,
    title: String,
    description: String,
    imageRes: Int,
    onImageClick: (Int) -> Unit,
    clickableUrl: String? = null,
    context: Context? = null,
    onUrlCopied: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with step number
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Clickable URL card (if provided)
            if (clickableUrl != null && context != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Supabase URL", clickableUrl)
                        clipboard.setPrimaryClip(clip)
                        onUrlCopied?.invoke()
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Language,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = clickableUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            Icons.Default.ContentCopy,
                            stringResource(R.string.cloud_sync_instructions_copy_icon),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            // Screenshot (clickable for full-screen)
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clickable { onImageClick(imageRes) },
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

/**
 * Instruction step with SQL query and copy button
 */
@Composable
private fun InstructionStepWithSqlQuery(
    stepNumber: Int,
    title: String,
    description: String,
    imageRes: Int,
    context: Context,
    onImageClick: (Int) -> Unit
) {
    val sqlQuery = """
-- YourOwnAI Cloud Sync - Supabase Schema
-- Copy and run this in Supabase Dashboard → SQL Editor

-- 1. Conversations table
CREATE TABLE IF NOT EXISTS conversations (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    system_prompt TEXT NOT NULL,
    system_prompt_id TEXT,
    model TEXT NOT NULL,
    provider TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_archived BOOLEAN DEFAULT FALSE,
    source_conversation_id TEXT,
    device_id TEXT,
    synced_at BIGINT
);

-- 2. Messages table
CREATE TABLE IF NOT EXISTS messages (
    id TEXT PRIMARY KEY,
    conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    model TEXT,
    temperature REAL,
    top_p REAL,
    deep_empathy BOOLEAN,
    memory_enabled BOOLEAN,
    message_history_limit INTEGER,
    system_prompt TEXT,
    request_logs TEXT,
    swipe_message_id TEXT,
    swipe_message_text TEXT,
    image_attachments TEXT,
    file_attachments TEXT,
    is_liked BOOLEAN DEFAULT FALSE,
    device_id TEXT,
    synced_at BIGINT
);

-- 3. Memories table
CREATE TABLE IF NOT EXISTS memories (
    id TEXT PRIMARY KEY,
    conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    message_id TEXT NOT NULL,
    fact TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    persona_id TEXT,
    embedding BYTEA,
    device_id TEXT,
    synced_at BIGINT
);

-- 4. Personas table
CREATE TABLE IF NOT EXISTS personas (
    id TEXT PRIMARY KEY,
    system_prompt_id TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    is_for_api BOOLEAN DEFAULT TRUE,
    
    -- AI Configuration
    temperature REAL DEFAULT 0.7,
    top_p REAL DEFAULT 0.9,
    max_tokens INTEGER DEFAULT 4096,
    deep_empathy BOOLEAN DEFAULT FALSE,
    memory_enabled BOOLEAN DEFAULT FALSE,
    rag_enabled BOOLEAN DEFAULT FALSE,
    message_history_limit INTEGER DEFAULT 10,
    
    -- Prompts
    deep_empathy_prompt TEXT,
    deep_empathy_analysis_prompt TEXT,
    memory_extraction_prompt TEXT,
    context_instructions TEXT,
    memory_instructions TEXT,
    rag_instructions TEXT,
    swipe_message_prompt TEXT,
    
    -- Memory Configuration
    memory_limit INTEGER DEFAULT 5,
    memory_min_age_days INTEGER DEFAULT 2,
    memory_title TEXT,
    
    -- RAG Configuration
    rag_chunk_size INTEGER DEFAULT 512,
    rag_chunk_overlap INTEGER DEFAULT 64,
    rag_chunk_limit INTEGER DEFAULT 5,
    rag_title TEXT,
    
    -- Model Preference
    preferred_model_id TEXT,
    preferred_provider TEXT,
    
    -- Document Links
    linked_document_ids TEXT,
    
    -- Memory Scope
    use_only_persona_memories BOOLEAN DEFAULT FALSE,
    share_memories_globally BOOLEAN DEFAULT TRUE,
    
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    device_id TEXT,
    synced_at BIGINT
);

-- 5. System prompts table
CREATE TABLE IF NOT EXISTS system_prompts (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    content TEXT NOT NULL,
    type TEXT NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    device_id TEXT,
    synced_at BIGINT
);

-- 6. Knowledge documents table
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    linked_persona_ids TEXT,
    device_id TEXT,
    synced_at BIGINT
);

-- 7. Document embeddings table
CREATE TABLE IF NOT EXISTS document_embeddings (
    id TEXT PRIMARY KEY,
    document_id TEXT NOT NULL REFERENCES knowledge_documents(id) ON DELETE CASCADE,
    chunk_text TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    embedding BYTEA,
    device_id TEXT,
    synced_at BIGINT
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_created ON messages(created_at);
CREATE INDEX IF NOT EXISTS idx_messages_device ON messages(device_id);
CREATE INDEX IF NOT EXISTS idx_memories_conversation ON memories(conversation_id);
CREATE INDEX IF NOT EXISTS idx_memories_persona ON memories(persona_id);
CREATE INDEX IF NOT EXISTS idx_document_embeddings_document ON document_embeddings(document_id);
CREATE INDEX IF NOT EXISTS idx_conversations_device ON conversations(device_id);
CREATE INDEX IF NOT EXISTS idx_conversations_updated ON conversations(updated_at);

-- Enable Row Level Security (RLS) - OPTIONAL
-- This ensures users can only access their own data
-- You'll need to configure RLS policies in Supabase Dashboard

-- ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE memories ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE personas ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE system_prompts ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE knowledge_documents ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE document_embeddings ENABLE ROW LEVEL SECURITY;

-- Success message
SELECT 'Schema created successfully! ✅' as status;
    """.trimIndent()
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Copy SQL button
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("SQL Query", sqlQuery)
                    clipboard.setPrimaryClip(clip)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.cloud_sync_instructions_copy_sql))
            }
            
            // Screenshot (clickable for full-screen)
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clickable { onImageClick(imageRes) },
                contentScale = ContentScale.FillWidth
            )
        }
    }
}
