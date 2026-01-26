package com.yourown.ai.di

import com.google.gson.Gson
import com.yourown.ai.data.remote.deepseek.DeepseekClient
import com.yourown.ai.data.remote.openai.OpenAIClient
import com.yourown.ai.data.remote.xai.XAIClient
import com.yourown.ai.data.repository.LocalModelRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    @ApiClient
    fun provideApiOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
        
        // TEMPORARILY DISABLED for debugging OOM issues
        // TODO: Re-enable with Level.HEADERS only
        /*
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            // Redact sensitive headers
            redactHeader("Authorization")
            redactHeader("API-Key")
        }
        builder.addInterceptor(loggingInterceptor)
        */
        
        return builder.build()
    }
    
    @Provides
    @Singleton
    @DownloadClient
    fun provideDownloadOkHttpClientV2(): OkHttpClient {
        // OkHttpClient for large file downloads WITHOUT any logging
        // This prevents OutOfMemoryError when downloading multi-GB model files
        android.util.Log.d("NetworkModule", "Creating DownloadClient OkHttpClient - NO LOGGING")
        
        val builder = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)  // 5 minutes for large files
            .writeTimeout(60, TimeUnit.SECONDS)
        
        // NO LOGGING INTERCEPTOR - prevents loading file into memory
        // Progress updates are handled in LocalModelRepository
        
        val client = builder.build()
        android.util.Log.d("NetworkModule", "DownloadClient created: ${client.hashCode()}, interceptors: ${client.interceptors.size}")
        return client
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
    
    @Provides
    @Singleton
    fun provideDeepseekClient(
        @ApiClient okHttpClient: OkHttpClient,
        gson: Gson
    ): DeepseekClient {
        return DeepseekClient(okHttpClient, gson)
    }
    
    @Provides
    @Singleton
    fun provideOpenAIClient(
        @ApiClient okHttpClient: OkHttpClient,
        gson: Gson
    ): OpenAIClient {
        return OpenAIClient(okHttpClient, gson)
    }
    
    @Provides
    @Singleton
    fun provideXAIClient(
        @ApiClient okHttpClient: OkHttpClient,
        gson: Gson
    ): XAIClient {
        return XAIClient(okHttpClient, gson)
    }
}
