package com.yourown.ai.presentation.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yourown.ai.R
import com.yourown.ai.data.repository.ConversationRepository
import com.yourown.ai.data.repository.MessageRepository
import com.yourown.ai.domain.model.Conversation
import com.yourown.ai.domain.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * WorkManager worker for exporting large chats in the background
 * Shows notification with progress and allows app to be minimized
 */
class ExportChatWorker(
    private val context: Context,
    params: WorkerParameters,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_CONVERSATION_ID = "conversation_id"
        const val KEY_FILTER_LIKES = "filter_likes"
        const val KEY_OUTPUT_FILE = "output_file"
        const val KEY_TOTAL_MESSAGES = "total_messages"
        
        const val NOTIFICATION_CHANNEL_ID = "chat_export_channel"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        try {
            val conversationId = inputData.getString(KEY_CONVERSATION_ID) ?: return@withContext Result.failure()
            val filterLikes = inputData.getBoolean(KEY_FILTER_LIKES, false)
            val outputPath = inputData.getString(KEY_OUTPUT_FILE) ?: return@withContext Result.failure()
            
            // Create notification channel
            createNotificationChannel()
            
            // Show foreground notification
            val totalMessages = inputData.getInt(KEY_TOTAL_MESSAGES, 0)
            setForeground(createForegroundInfo(0, totalMessages))
            
            // Load conversation and messages
            val conversation = conversationRepository.getConversationById(conversationId).first()
                ?: return@withContext Result.failure(workDataOf("error" to "Conversation not found"))
            
            val allMessages = messageRepository.getMessagesByConversation(conversationId).first()
            val messagesToExport = if (filterLikes) {
                allMessages.filter { it.isLiked }
            } else {
                allMessages
            }
            
            if (messagesToExport.isEmpty()) {
                return@withContext Result.failure(workDataOf("error" to "No messages to export"))
            }
            
            // Export with progress updates
            val exportedText = exportChatWithProgress(
                conversation = conversation,
                messages = messagesToExport,
                totalMessages = messagesToExport.size,
                onProgress = { processed, total ->
                    // Update notification
                    setForegroundAsync(createForegroundInfo(processed, total))
                }
            )
            
            // Write to file
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(exportedText)
            
            // Success
            Result.success(workDataOf(
                KEY_OUTPUT_FILE to outputPath,
                KEY_TOTAL_MESSAGES to messagesToExport.size
            ))
            
        } catch (e: Exception) {
            android.util.Log.e("ExportChatWorker", "Export failed", e)
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }
    
    private suspend fun exportChatWithProgress(
        conversation: Conversation,
        messages: List<Message>,
        totalMessages: Int,
        onProgress: (Int, Int) -> Unit
    ): String {
        val estimatedSize = totalMessages * 200
        val exportBuilder = StringBuilder(estimatedSize)
        
        // Header
        exportBuilder.appendLine("# Chat Export: ${conversation.title}")
        exportBuilder.appendLine()
        exportBuilder.appendLine("**Date:** ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        exportBuilder.appendLine("**Model:** ${conversation.model} (${conversation.provider})")
        exportBuilder.appendLine("**Total messages:** $totalMessages")
        exportBuilder.appendLine()
        exportBuilder.appendLine("---")
        exportBuilder.appendLine()
        
        // Process messages in small chunks with progress
        val chunkSize = 50 // Larger chunks OK in WorkManager (real background)
        var processed = 0
        
        messages.chunked(chunkSize).forEach { chunk ->
            chunk.forEach { message ->
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(message.createdAt))
                val role = when (message.role) {
                    com.yourown.ai.domain.model.MessageRole.USER -> "## 👤 User"
                    com.yourown.ai.domain.model.MessageRole.ASSISTANT -> "## 🤖 Assistant"
                    com.yourown.ai.domain.model.MessageRole.SYSTEM -> "## ⚙️ System"
                }
                val likeIndicator = if (message.isLiked) " ❤️" else ""
                
                exportBuilder.appendLine("$role$likeIndicator")
                exportBuilder.appendLine("*$timestamp*")
                exportBuilder.appendLine()
                exportBuilder.appendLine(message.content)
                exportBuilder.appendLine()
                exportBuilder.appendLine("---")
                exportBuilder.appendLine()
            }
            
            processed += chunk.size
            onProgress(processed, totalMessages)
            
            // Small yield for responsiveness
            kotlinx.coroutines.yield()
        }
        
        return exportBuilder.toString()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Chat Export",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for chat export progress"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createForegroundInfo(processed: Int, total: Int): ForegroundInfo {
        val progress = if (total > 0) (processed * 100) / total else 0
        
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Exporting chat")
            .setContentText(if (processed == 0) "Starting..." else "$processed / $total messages ($progress%)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(total, processed, processed == 0)
            .setOngoing(true)
            .build()
        
        // For Android 14+ (API 34+), we need to specify foreground service type
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID, 
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
