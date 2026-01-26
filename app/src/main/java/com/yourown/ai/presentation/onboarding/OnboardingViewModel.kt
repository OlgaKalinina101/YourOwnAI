package com.yourown.ai.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourown.ai.data.local.preferences.SettingsManager
import com.yourown.ai.presentation.theme.ColorStyle
import com.yourown.ai.presentation.theme.FontScale
import com.yourown.ai.presentation.theme.FontStyle
import com.yourown.ai.presentation.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorStyle: ColorStyle = ColorStyle.DYNAMIC,
    val fontStyle: FontStyle = FontStyle.ROBOTO,
    val fontScale: FontScale = FontScale.DEFAULT,
    val isFirstLaunch: Boolean = true,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {
    
    val uiState: StateFlow<OnboardingUiState> = combine(
        settingsManager.themeMode,
        settingsManager.colorStyle,
        settingsManager.fontStyle,
        settingsManager.fontScale,
        settingsManager.isFirstLaunch
    ) { themeMode, colorStyle, fontStyle, fontScale, isFirstLaunch ->
        OnboardingUiState(
            themeMode = themeMode,
            colorStyle = colorStyle,
            fontStyle = fontStyle,
            fontScale = fontScale,
            isFirstLaunch = isFirstLaunch
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OnboardingUiState()
    )
    
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsManager.setThemeMode(mode)
        }
    }
    
    fun setColorStyle(style: ColorStyle) {
        viewModelScope.launch {
            settingsManager.setColorStyle(style)
        }
    }
    
    fun setFontStyle(style: FontStyle) {
        viewModelScope.launch {
            settingsManager.setFontStyle(style)
        }
    }
    
    fun setFontScale(scale: FontScale) {
        viewModelScope.launch {
            settingsManager.setFontScale(scale)
        }
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            settingsManager.setOnboardingCompleted()
            settingsManager.setFirstLaunchCompleted()
        }
    }
}
