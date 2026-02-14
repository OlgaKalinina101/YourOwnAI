package com.yourown.ai.di

import android.content.Context
import com.yourown.ai.data.local.preferences.SettingsManager
import com.yourown.ai.domain.prompt.PromptTranslationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideSettingsManager(
        @ApplicationContext context: Context
    ): SettingsManager {
        return SettingsManager(context)
    }
    
    @Provides
    @Singleton
    fun providePromptTranslationManager(
        settingsManager: SettingsManager
    ): PromptTranslationManager {
        return PromptTranslationManager(settingsManager)
    }
    
    @Provides
    @Singleton
    fun provideKeyboardSoundManager(
        @ApplicationContext context: Context
    ): com.yourown.ai.domain.service.KeyboardSoundManager {
        return com.yourown.ai.domain.service.KeyboardSoundManager(
            context = context,
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob())
        )
    }
}
