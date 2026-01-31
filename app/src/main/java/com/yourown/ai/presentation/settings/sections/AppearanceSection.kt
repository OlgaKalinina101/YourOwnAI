package com.yourown.ai.presentation.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import com.yourown.ai.presentation.settings.components.SettingItemClickable
import com.yourown.ai.presentation.settings.components.SettingsSection

/**
 * Appearance Section - theme, colors, fonts
 */
@Composable
fun AppearanceSection(
    onShowAppearance: () -> Unit
) {
    SettingsSection(
        title = "Appearance",
        icon = Icons.Default.Palette,
        subtitle = "Theme, colors, and fonts"
    ) {
        SettingItemClickable(
            title = "Customize",
            subtitle = "Change theme, colors, and text size",
            onClick = onShowAppearance,
            trailing = {
                Icon(Icons.Default.ChevronRight, "Customize")
            }
        )
    }
}
