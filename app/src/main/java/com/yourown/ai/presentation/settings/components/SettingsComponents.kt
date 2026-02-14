package com.yourown.ai.presentation.settings.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Common settings UI components
 */

/**
 * Settings section with card, title, icon and content
 */
@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String,
    isCollapsible: Boolean = false,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = if (isCollapsible) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                }
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isCollapsible) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                content()
            }
        }
    }
}

/**
 * Clickable settings item
 */
@Composable
fun SettingItemClickable(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            trailing()
        }
    }
}

/**
 * Slider setting with value display
 */
@Composable
fun SliderSetting(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = { "%.2f".format(it) },
    hintResId: Int? = null
) {
    var showHelpDialog by remember { mutableStateOf(false) }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = valueFormatter(value),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (hintResId != null) {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    if (showHelpDialog && hintResId != null) {
        HelpDialog(
            hintResId = hintResId,
            onDismiss = { showHelpDialog = false }
        )
    }
}

/**
 * Toggle (switch) setting
 */
@Composable
fun ToggleSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    hintResId: Int? = null
) {
    var showHelpDialog by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
            if (hintResId != null) {
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
    
    if (showHelpDialog && hintResId != null) {
        HelpDialog(
            hintResId = hintResId,
            onDismiss = { showHelpDialog = false }
        )
    }
}

/**
 * Dropdown setting for selecting from options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSetting(
    title: String,
    subtitle: String,
    value: Int,
    options: List<Int>,
    onValueChange: (Int) -> Unit,
    valueSuffix: String = "",
    hintResId: Int? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.menuAnchor()
                    ) {
                        Text(
                            text = "$value",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$option $valueSuffix",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    onValueChange(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                if (hintResId != null) {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, "Help", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
    
    if (showHelpDialog && hintResId != null) {
        HelpDialog(
            hintResId = hintResId,
            onDismiss = { showHelpDialog = false }
        )
    }
}

/**
 * Dropdown setting for String values (e.g., language selection)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSettingString(
    title: String,
    subtitle: String,
    value: String,
    options: List<Pair<String, String>>, // code to display name
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.menuAnchor()
                    ) {
                        Text(
                            text = options.find { it.first == value }?.second ?: value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    onValueChange(code)
                                    expanded = false
                                },
                                leadingIcon = if (code == value) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simple Help Dialog to display hints
 */
@Composable
fun HelpDialog(
    hintResId: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hintText = stringResource(hintResId)
    
    // Regex для поиска URL в тексте
    val urlPattern = Regex("https?://[^\\s]+")
    val urls = urlPattern.findAll(hintText).map { it.value }.toList()
    
    // Создаем AnnotatedString с кликабельными ссылками
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        urlPattern.findAll(hintText).forEach { matchResult ->
            // Добавляем текст до ссылки
            append(hintText.substring(lastIndex, matchResult.range.first))
            
            // Добавляем ссылку с аннотацией
            pushStringAnnotation(
                tag = "URL",
                annotation = matchResult.value
            )
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(matchResult.value)
            }
            pop()
            
            lastIndex = matchResult.range.last + 1
        }
        // Добавляем оставшийся текст
        if (lastIndex < hintText.length) {
            append(hintText.substring(lastIndex))
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                            context.startActivity(intent)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("OK")
                }
            }
        }
    }
}
