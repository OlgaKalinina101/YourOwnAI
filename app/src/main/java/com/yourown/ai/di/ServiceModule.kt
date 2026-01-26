package com.yourown.ai.di

import com.yourown.ai.data.service.AIServiceImpl
import com.yourown.ai.data.service.LlamaServiceImpl
import com.yourown.ai.domain.service.AIService
import com.yourown.ai.domain.service.LlamaService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    
    @Binds
    @Singleton
    abstract fun bindLlamaService(
        llamaServiceImpl: LlamaServiceImpl
    ): LlamaService
    
    @Binds
    @Singleton
    abstract fun bindAIService(
        aiServiceImpl: AIServiceImpl
    ): AIService
}
