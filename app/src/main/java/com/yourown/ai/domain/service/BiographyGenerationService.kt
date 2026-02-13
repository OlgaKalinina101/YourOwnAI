package com.yourown.ai.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yourown.ai.R
import com.yourown.ai.domain.model.*
import com.yourown.ai.domain.usecase.GenerateBiographyUseCase
import com.yourown.ai.data.repository.BiographyRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * Foreground Service for biography generation
 * Ensures the process continues even when app is in background
 */
@AndroidEntryPoint
class BiographyGenerationService : Service() {
    
    @Inject
    lateinit var generateBiographyUseCase: GenerateBiographyUseCase
    
    @Inject
    lateinit var biographyRepository: BiographyRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var notificationManager: NotificationManager? = null
    
    companion object {
        const val CHANNEL_ID = "biography_generation"
        const val NOTIFICATION_ID = 1001
        
        const val EXTRA_CLUSTERS = "extra_clusters"
        const val EXTRA_MODEL_PROVIDER = "extra_model_provider"
        const val EXTRA_MODEL_NAME = "extra_model_name"
        
        const val ACTION_CANCEL = "action_cancel"
        
        fun start(
            context: Context,
            clusters: List<MemoryCluster>,
            model: ModelProvider
        ) {
            val intent = Intent(context, BiographyGenerationService::class.java).apply {
                // We can't pass complex objects via Intent easily
                // So we'll trigger the service and it will get data from UseCase
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun cancel(context: Context) {
            val intent = Intent(context, BiographyGenerationService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                // Cancel generation and stop service
                generateBiographyUseCase.resetStatus()
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // Start foreground with notification
                startForeground(NOTIFICATION_ID, createNotification(0, 0, "Starting..."))
                
                // Observe generation status and update notification
                serviceScope.launch {
                    generateBiographyUseCase.generationStatus.collectLatest { status ->
                        when (status) {
                            is BiographyGenerationStatus.Processing -> {
                                val notification = createNotification(
                                    current = status.currentCluster,
                                    total = status.totalClusters,
                                    text = "Processing cluster ${status.currentCluster}/${status.totalClusters}"
                                )
                                notificationManager?.notify(NOTIFICATION_ID, notification)
                            }
                            
                            is BiographyGenerationStatus.Completed -> {
                                // Save and stop service
                                biographyRepository.saveBiography(status.biography)
                                showCompletionNotification()
                                delay(3000) // Show completion notification for 3 seconds
                                stopForeground(true)
                                stopSelf()
                            }
                            
                            is BiographyGenerationStatus.Failed -> {
                                showErrorNotification(status.error)
                                delay(5000) // Show error notification for 5 seconds
                                stopForeground(true)
                                stopSelf()
                            }
                            
                            BiographyGenerationStatus.Idle -> {
                                // Generation was cancelled or not started
                                stopForeground(true)
                                stopSelf()
                            }
                        }
                    }
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.biography_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.biography_notification_channel_description)
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(current: Int, total: Int, text: String): Notification {
        val cancelIntent = Intent(this, BiographyGenerationService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.biography_generating))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_delete,
                getString(R.string.biography_cancel),
                cancelPendingIntent
            )
        
        if (total > 0) {
            builder.setProgress(total, current, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        
        return builder.build()
    }
    
    private fun showCompletionNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.biography_completed))
            .setContentText(getString(R.string.biography_completed_description))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showErrorNotification(error: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.biography_error))
            .setContentText(error)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
}
