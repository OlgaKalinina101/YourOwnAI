package com.yourown.ai.presentation.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yourown.ai.R
import com.yourown.ai.presentation.settings.components.SettingItemClickable
import com.yourown.ai.presentation.settings.components.SettingsSection
import kotlin.math.roundToInt

/**
 * Sound & Haptics Section - keyboard sounds with volume slider
 */
@Composable
fun SoundHapticsSection(
    keyboardSoundVolume: Float,
    onSoundVolumeChange: (Float) -> Unit,
    onTestSound: () -> Unit
) {
    SettingsSection(
        title = stringResource(R.string.sound_haptics_title),
        icon = Icons.Default.VolumeUp,
        subtitle = stringResource(R.string.sound_haptics_subtitle)
    ) {
        // Keyboard Sound Volume Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sound_volume_label),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${(keyboardSoundVolume * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Slider(
                value = keyboardSoundVolume,
                onValueChange = onSoundVolumeChange,
                modifier = Modifier.fillMaxWidth(),
                valueRange = 0f..1f
            )
        }
        
        // Test Sound Button
        SettingItemClickable(
            title = stringResource(R.string.sound_test_title),
            subtitle = stringResource(R.string.sound_test_subtitle),
            onClick = onTestSound,
            trailing = {
                Icon(Icons.Default.PlayArrow, stringResource(R.string.sound_test_icon))
            }
        )
    }
}
