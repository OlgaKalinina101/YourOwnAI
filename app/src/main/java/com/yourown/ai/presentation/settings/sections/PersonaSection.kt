package com.yourown.ai.presentation.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PersonaSection(
    systemPrompts: List<com.yourown.ai.domain.model.SystemPrompt>,
    personas: Map<String, com.yourown.ai.domain.model.Persona>, // Map<systemPromptId, Persona>
    onSelectSystemPrompt: (String) -> Unit // Callback для открытия PersonaDetailScreen
) {
    // Фильтруем только API промпты, исключая default
    val apiPrompts = systemPrompts.filter { 
        it.type.value == "api" && !it.isDefault 
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Header
        Text(
            "Personas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Manage Personas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    "Select a system prompt to customize its AI behavior",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Dropdown для каждого API System Prompt
                apiPrompts.forEach { prompt ->
                    val hasPersona = personas.containsKey(prompt.id)
                    
                    PersonaPromptItem(
                        promptName = prompt.name,
                        hasPersona = hasPersona,
                        onClick = { onSelectSystemPrompt(prompt.id) }
                    )
                }
                
                if (apiPrompts.isEmpty()) {
                    Text(
                        text = "No API system prompts available. Add one in System Prompts section.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
        
        // Help text
        Text(
            text = "Personas allow you to create different AI personalities with unique settings, memory scopes, and document access. Perfect for separating work, personal, or specialized conversations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp)
        )
    }
}

@Composable
private fun PersonaPromptItem(
    promptName: String,
    hasPersona: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (hasPersona) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Text(
                    text = promptName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (hasPersona) FontWeight.Medium else FontWeight.Normal
                )
                
                if (hasPersona) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "Configured",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
