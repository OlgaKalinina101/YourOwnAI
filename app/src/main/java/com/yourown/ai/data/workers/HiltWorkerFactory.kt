package com.yourown.ai.data.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.yourown.ai.data.repository.ConversationRepository
import com.yourown.ai.data.repository.MessageRepository
import com.yourown.ai.presentation.chat.ExportChatWorker
import javax.inject.Inject

/**
 * Custom WorkerFactory for Hilt dependency injection in Workers
 */
class HiltWorkerFactory @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            ExportChatWorker::class.java.name -> {
                ExportChatWorker(
                    appContext,
                    workerParameters,
                    conversationRepository,
                    messageRepository
                )
            }
            else -> null
        }
    }
}
