package com.yourown.ai.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Neutral Light Color Scheme
 * Максимально нейтральная светлая тема с серыми оттенками
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF404040),           // Тёмно-серый
    onPrimary = Color(0xFFFFFFFF),         // Белый текст на primary
    primaryContainer = Color(0xFFE0E0E0), // Светло-серый контейнер
    onPrimaryContainer = Color(0xFF1C1C1C),
    
    secondary = Color(0xFF5F5F5F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEEEEEE),
    onSecondaryContainer = Color(0xFF2C2C2C),
    
    tertiary = Color(0xFF757575),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF5F5F5),
    onTertiaryContainer = Color(0xFF303030),
    
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    background = Color(0xFFFFFFFF),        // Чистый белый фон
    onBackground = Color(0xFF000000),      // Чёрный текст
    
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF5F5F5F),  // Серый для вторичного текста
    
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0),
    
    scrim = Color(0x80000000),
)

/**
 * Neutral Dark Color Scheme
 * Максимально нейтральная тёмная тема с true black для OLED
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFFE0E0E0),           // Светло-серый в тёмной теме
    onPrimary = Color(0xFF000000),         // Чёрный текст на primary
    primaryContainer = Color(0xFF2C2C2C), // Тёмно-серый контейнер
    onPrimaryContainer = Color(0xFFE0E0E0),
    
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF121212),
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFEEEEEE),
    
    tertiary = Color(0xFF9E9E9E),
    onTertiary = Color(0xFF1C1C1C),
    tertiaryContainer = Color(0xFF484848),
    onTertiaryContainer = Color(0xFFE0E0E0),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    background = Color(0xFF000000),        // True black для OLED
    onBackground = Color(0xFFFFFFFF),      // Белый текст
    
    surface = Color(0xFF121212),           // Чуть светлее фона
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFF9E9E9E),  // Серый для вторичного текста
    
    outline = Color(0xFF5F5F5F),
    outlineVariant = Color(0xFF3A3A3A),
    
    scrim = Color(0x80000000),
)

/**
 * Theme Mode настройки
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Color Style настройки
 */
enum class ColorStyle {
    DYNAMIC,      // Material You Dynamic Colors (Android 12+)
    NEUTRAL,      // Нейтральная серая палитра
    CUSTOM        // Кастомный акцентный цвет (будущая фича)
}

/**
 * YourOwnAI Theme
 * 
 * Максимально нейтральная тема с поддержкой:
 * - Dynamic Color (Material You) для Android 12+
 * - Нейтральная серая палитра для старых версий
 * - Опциональная кастомизация
 */
@Composable
fun YourOwnAITheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorStyle: ColorStyle = ColorStyle.DYNAMIC,
    customAccentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Определяем, тёмная тема или нет
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    // Выбираем цветовую схему
    val colorScheme = when {
        // Dynamic Color доступен только на Android 12+
        colorStyle == ColorStyle.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        // Кастомный цвет (будущая фича)
        colorStyle == ColorStyle.CUSTOM && customAccentColor != null -> {
            // TODO: Генерировать схему на основе customAccentColor
            if (darkTheme) DarkColors else LightColors
        }
        // Нейтральная серая палитра (дефолт для Android 11-)
        else -> {
            if (darkTheme) DarkColors else LightColors
        }
    }
    
    // Настройка system bars
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
