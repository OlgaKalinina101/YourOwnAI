package com.yourown.ai

import android.app.Application
import androidx.work.Configuration
import com.yourown.ai.data.sync.AutoSyncManager
import com.yourown.ai.data.workers.HiltWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class YourOwnAIApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var autoSyncManager: AutoSyncManager
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize auto-sync for background/foreground sync
        autoSyncManager.initialize()
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
