package com.yourown.ai.di

import com.yourown.ai.data.repository.ConversationRepository
import com.yourown.ai.data.repository.LocalModelRepository
import com.yourown.ai.data.repository.MessageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    // LocalModelRepository, ConversationRepository, and MessageRepository
    // are provided automatically by Hilt because they have @Inject constructors
    // This module is here for future repository dependencies if needed
}
