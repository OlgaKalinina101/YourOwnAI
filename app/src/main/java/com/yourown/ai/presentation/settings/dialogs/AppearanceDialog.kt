package com.yourown.ai.presentation.settings.dialogs

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourown.ai.presentation.onboarding.OnboardingViewModel
import com.yourown.ai.presentation.theme.*
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceDialog(
    onDismiss: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.appearance_dialog_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.appearance_dialog_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close, 
                                stringResource(R.string.appearance_dialog_close),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Theme Settings Section
                    SettingsSection(title = stringResource(R.string.appearance_theme_section)) {
                        // Theme Mode
                        SettingItem(
                            title = stringResource(R.string.appearance_theme_mode_title),
                            description = stringResource(R.string.appearance_theme_mode_desc)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                        val label = when (mode) {
                                        ThemeMode.LIGHT -> stringResource(R.string.appearance_theme_light)
                                        ThemeMode.DARK -> stringResource(R.string.appearance_theme_dark)
                                        ThemeMode.SYSTEM -> stringResource(R.string.appearance_theme_system)
                                    }
                                    FilterChip(
                                        selected = uiState.themeMode == mode,
                                        onClick = { viewModel.setThemeMode(mode) },
                                        label = { Text(text = label) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = when (mode) {
                                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Color Style
                        SettingItem(
                            title = stringResource(R.string.appearance_colors_title),
                            description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                stringResource(R.string.appearance_colors_desc_dynamic)
                            } else {
                                stringResource(R.string.appearance_colors_desc)
                            }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    FilterChip(
                                        selected = uiState.colorStyle == ColorStyle.DYNAMIC,
                                        onClick = { viewModel.setColorStyle(ColorStyle.DYNAMIC) },
                                        label = { Text(stringResource(R.string.appearance_colors_dynamic)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                                FilterChip(
                                    selected = uiState.colorStyle == ColorStyle.NEUTRAL,
                                    onClick = { viewModel.setColorStyle(ColorStyle.NEUTRAL) },
                                    label = { Text(stringResource(R.string.appearance_colors_neutral)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Circle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    // Typography Settings Section
                    SettingsSection(title = stringResource(R.string.appearance_typography_section)) {
                        // Font Style
                        SettingItem(
                            title = stringResource(R.string.appearance_font_title),
                            description = stringResource(R.string.appearance_font_desc)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.fontStyle == FontStyle.ROBOTO,
                                    onClick = { viewModel.setFontStyle(FontStyle.ROBOTO) },
                                    label = { Text(stringResource(R.string.appearance_font_roboto)) }
                                )
                                FilterChip(
                                    selected = uiState.fontStyle == FontStyle.SYSTEM,
                                    onClick = { viewModel.setFontStyle(FontStyle.SYSTEM) },
                                    label = { Text(stringResource(R.string.appearance_font_system)) }
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Font Scale
                        SettingItem(
                            title = stringResource(R.string.appearance_text_size_title),
                            description = stringResource(R.string.appearance_text_size_desc)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FontScale.entries.forEach { scale ->
                                        val label = when (scale) {
                                            FontScale.SMALL -> stringResource(R.string.appearance_text_size_s)
                                            FontScale.DEFAULT -> stringResource(R.string.appearance_text_size_m)
                                            FontScale.MEDIUM -> stringResource(R.string.appearance_text_size_l)
                                            FontScale.LARGE -> stringResource(R.string.appearance_text_size_xl)
                                            FontScale.EXTRA_LARGE -> stringResource(R.string.appearance_text_size_xxl)
                                        }
                                        FilterChip(
                                            selected = uiState.fontScale == scale,
                                            onClick = { viewModel.setFontScale(scale) },
                                            label = {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                
                                // Preview text
                                Text(
                                    text = stringResource(R.string.appearance_preview_text),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * uiState.fontScale.scale
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                
                // Save Button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.appearance_save),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            content()
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        content()
    }
}
