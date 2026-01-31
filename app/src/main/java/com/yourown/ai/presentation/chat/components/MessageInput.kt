package com.yourown.ai.presentation.chat.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/**
 * Message input field with send button
 * Expands vertically as text grows and adjusts for keyboard
 * Grok-style: attachment icon on left, voice/send on right (inside the field)
 */
@Composable
fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit,
    onAttachImage: () -> Unit,
    onAttachFile: (() -> Unit)? = null,
    isListening: Boolean = false,
    enabled: Boolean = true,
    supportsAttachments: Boolean = false,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val hasText = text.trim().isNotEmpty()
    val showFileMenu = remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Text field with icons inside
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            "Message your AI...",
                            style = MaterialTheme.typography.bodyLarge
                        ) 
                    },
                    enabled = enabled,
                    minLines = 1,
                    maxLines = 6,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    leadingIcon = {
                        // Attachment menu - only visible if model supports it
                        if (supportsAttachments) {
                            IconButton(
                                onClick = { 
                                    if (onAttachFile != null) {
                                        showFileMenu.value = true
                                    } else {
                                        onAttachImage()
                                    }
                                },
                                enabled = enabled
                            ) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = "Attach file",
                                    tint = if (enabled) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    }
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        // Voice or Send button
                        if (hasText) {
                            // Send button with circular background
                            FilledIconButton(
                                onClick = {
                                    onSend()
                                    keyboardController?.hide()
                                },
                                enabled = enabled,
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            // Voice button with pulsating animation when listening
                            val scale by animateFloatAsState(
                                targetValue = if (isListening) 1.2f else 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(500),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "mic_scale"
                            )
                            
                            IconButton(
                                onClick = onVoiceInput,
                                enabled = enabled,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice input",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .scale(if (isListening) scale else 1f),
                                    tint = if (isListening) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
        
        // Dropdown menu positioned at bottom-left, attached to the Surface
        if (onAttachFile != null) {
            DropdownMenu(
                expanded = showFileMenu.value,
                onDismissRequest = { showFileMenu.value = false },
                modifier = Modifier.align(Alignment.BottomStart),
                offset = androidx.compose.ui.unit.DpOffset(16.dp, (-8).dp) // Offset to align with attach icon
            ) {
                DropdownMenuItem(
                    text = { Text("Image") },
                    onClick = {
                        showFileMenu.value = false
                        onAttachImage()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Image, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Document (PDF, TXT)") },
                    onClick = {
                        showFileMenu.value = false
                        onAttachFile()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    }
                )
            }
        }
    }
}
