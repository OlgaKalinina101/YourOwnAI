package com.yourown.ai.presentation.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        title = "Sound & Haptics",
        icon = Icons.Default.VolumeUp,
        subtitle = "Keyboard typing effects"
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
                    text = "Sound volume",
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
            title = "Test sound",
            subtitle = "Preview typing sound",
            onClick = onTestSound,
            trailing = {
                Icon(Icons.Default.PlayArrow, "Test sound")
            }
        )
    }
}
