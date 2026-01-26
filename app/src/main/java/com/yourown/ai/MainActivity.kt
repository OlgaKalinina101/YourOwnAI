package com.yourown.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.yourown.ai.data.local.preferences.SettingsManager
import com.yourown.ai.presentation.chat.ChatScreen
import com.yourown.ai.presentation.home.HomeScreen
import com.yourown.ai.presentation.onboarding.OnboardingScreen
import com.yourown.ai.presentation.settings.SettingsScreen
import com.yourown.ai.presentation.theme.YourOwnAITheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var settingsManager: SettingsManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            YourOwnAIApp(settingsManager)
        }
    }
}

// Simple navigation state
enum class Screen {
    ONBOARDING,
    HOME,
    CHAT,
    SETTINGS
}

@Composable
fun YourOwnAIApp(settingsManager: SettingsManager) {
    val hasCompletedOnboarding by settingsManager.hasCompletedOnboarding.collectAsState(initial = false)
    
    // Theme settings from SettingsManager
    val themeMode by settingsManager.themeMode.collectAsState(initial = com.yourown.ai.presentation.theme.ThemeMode.SYSTEM)
    val colorStyle by settingsManager.colorStyle.collectAsState(initial = com.yourown.ai.presentation.theme.ColorStyle.DYNAMIC)
    
    var currentScreen by remember { mutableStateOf(if (hasCompletedOnboarding) Screen.HOME else Screen.ONBOARDING) }
    var previousScreen by remember { mutableStateOf<Screen?>(null) }
    var currentChatId by remember { mutableStateOf<String?>(null) }
    
    // Update screen when onboarding completes
    LaunchedEffect(hasCompletedOnboarding) {
        if (hasCompletedOnboarding && currentScreen == Screen.ONBOARDING) {
            currentScreen = Screen.HOME
        }
    }
    
    YourOwnAITheme(
        themeMode = themeMode,
        colorStyle = colorStyle
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                Screen.ONBOARDING -> {
                    OnboardingScreen(
                        onComplete = {
                            currentScreen = Screen.HOME
                        }
                    )
                }
                
                Screen.HOME -> {
                    HomeScreen(
                        onNavigateToChat = { chatId ->
                            currentChatId = chatId
                            previousScreen = Screen.HOME
                            currentScreen = Screen.CHAT
                        },
                        onNavigateToSettings = {
                            previousScreen = Screen.HOME
                            currentScreen = Screen.SETTINGS
                        }
                    )
                }
                
                Screen.CHAT -> {
                    ChatScreen(
                        conversationId = currentChatId,
                        onNavigateToSettings = {
                            previousScreen = Screen.CHAT
                            currentScreen = Screen.SETTINGS
                        },
                        onNavigateBack = {
                            currentScreen = Screen.HOME
                        }
                    )
                }
                
                Screen.SETTINGS -> {
                    SettingsScreen(
                        onNavigateBack = {
                            // Return to previous screen, default to HOME if null
                            currentScreen = previousScreen ?: Screen.HOME
                            previousScreen = null
                        },
                        onViewMemories = {
                            // TODO: Navigate to memories screen
                        }
                    )
                }
            }
        }
    }
}
