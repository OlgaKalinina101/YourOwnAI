package com.yourown.ai.presentation.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.yourown.ai.R
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
        title = stringResource(R.string.appearance_section_title),
        icon = Icons.Default.Palette,
        subtitle = stringResource(R.string.appearance_section_subtitle)
    ) {
        SettingItemClickable(
            title = stringResource(R.string.appearance_customize_title),
            subtitle = stringResource(R.string.appearance_customize_subtitle),
            onClick = onShowAppearance,
            trailing = {
                Icon(Icons.Default.ChevronRight, stringResource(R.string.appearance_customize_icon))
            }
        )
    }
}
