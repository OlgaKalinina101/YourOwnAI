package com.yourown.ai.data.sync

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.yourown.ai.data.local.preferences.CloudSyncPreferences
import com.yourown.ai.data.repository.CloudSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages automatic background sync when app goes to foreground/background
 */
@Singleton
class AutoSyncManager @Inject constructor(
    private val cloudSyncRepository: CloudSyncRepository,
    private val cloudSyncPreferences: CloudSyncPreferences
) : DefaultLifecycleObserver {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val TAG = "AutoSyncManager"
        private const val MIN_SYNC_INTERVAL_MS = 60_000L // Минимум 1 минута между синхронизациями
    }
    
    fun initialize() {
        Log.d(TAG, "Initializing AutoSyncManager...")
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.i(TAG, "AutoSyncManager initialized ✅")
    }
    
    /**
     * Called when app comes to foreground (user opens/resumes app)
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "App came to foreground → Starting sync...")
        performAutoSync("app_foreground")
    }
    
    /**
     * Called when app goes to background (user closes/minimizes app)
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "App went to background → Starting sync...")
        performAutoSync("app_background")
    }
    
    /**
     * Perform sync if conditions are met
     */
    private fun performAutoSync(trigger: String) {
        scope.launch {
            try {
                // Check if sync is configured and enabled
                val settings = cloudSyncPreferences.cloudSyncSettings.first()
                
                if (!settings.isConfigured) {
                    Log.d(TAG, "Sync not configured, skipping auto-sync")
                    return@launch
                }
                
                if (!settings.enabled) {
                    Log.d(TAG, "Sync disabled, skipping auto-sync")
                    return@launch
                }
                
                // Check if enough time has passed since last sync
                val now = System.currentTimeMillis()
                val timeSinceLastSync = now - settings.lastSyncTimestamp
                
                if (timeSinceLastSync < MIN_SYNC_INTERVAL_MS) {
                    Log.d(TAG, "Synced ${timeSinceLastSync}ms ago, too soon (min: ${MIN_SYNC_INTERVAL_MS}ms)")
                    return@launch
                }
                
                Log.i(TAG, "Auto-sync triggered by: $trigger")
                
                // Perform sync
                val toCloudResult = cloudSyncRepository.syncToCloud()
                if (toCloudResult.isSuccess) {
                    Log.i(TAG, "Auto-sync to cloud completed ✅")
                } else {
                    Log.e(TAG, "Auto-sync to cloud failed: ${toCloudResult.exceptionOrNull()?.message}")
                }
                
                val fromCloudResult = cloudSyncRepository.syncFromCloud()
                if (fromCloudResult.isSuccess) {
                    Log.i(TAG, "Auto-sync from cloud completed ✅")
                } else {
                    Log.e(TAG, "Auto-sync from cloud failed: ${fromCloudResult.exceptionOrNull()?.message}")
                }
                
                // Update last sync timestamp
                cloudSyncPreferences.updateLastSyncTimestamp(now)
                
                Log.i(TAG, "Auto-sync completed successfully ($trigger) ✅")
                
            } catch (e: Exception) {
                Log.e(TAG, "Auto-sync failed", e)
            }
        }
    }
}
