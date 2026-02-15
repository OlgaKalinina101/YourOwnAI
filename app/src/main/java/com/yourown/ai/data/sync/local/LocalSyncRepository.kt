package com.yourown.ai.data.sync.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.yourown.ai.data.repository.ConversationRepository
import com.yourown.ai.data.repository.MemoryRepository
import com.yourown.ai.data.repository.MessageRepository
import com.yourown.ai.data.repository.PersonaRepository
import com.yourown.ai.data.repository.SystemPromptRepository
import com.yourown.ai.data.repository.AIConfigRepository
import com.yourown.ai.data.repository.KnowledgeDocumentRepository
import com.yourown.ai.data.local.preferences.SettingsManager
import com.yourown.ai.data.sync.local.models.ServerStatus
import com.yourown.ai.domain.service.AIService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Sync Repository
 * Manages Local Network Sync Server lifecycle
 */
@Singleton
class LocalSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val memoryRepository: MemoryRepository,
    private val personaRepository: PersonaRepository,
    private val systemPromptRepository: SystemPromptRepository,
    private val aiConfigRepository: AIConfigRepository,
    private val knowledgeDocumentRepository: KnowledgeDocumentRepository,
    private val settingsManager: SettingsManager,
    private val aiService: AIService,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "LocalSyncRepository"
        private const val DEFAULT_PORT = 8765
    }
    
    private var server: LocalSyncServer? = null
    
    private val _serverStatus = MutableStateFlow<ServerStatus?>(null)
    val serverStatus: StateFlow<ServerStatus?> = _serverStatus.asStateFlow()
    
    /**
     * Start local sync server
     */
    suspend fun startServer(deviceId: String, port: Int = DEFAULT_PORT): Boolean {
        return try {
            if (server?.isRunning() == true) {
                Log.w(TAG, "Server already running")
                return false
            }
            
            server = LocalSyncServer(
                context = context,
                conversationRepository = conversationRepository,
                messageRepository = messageRepository,
                memoryRepository = memoryRepository,
                personaRepository = personaRepository,
                systemPromptRepository = systemPromptRepository,
                aiConfigRepository = aiConfigRepository,
                knowledgeDocumentRepository = knowledgeDocumentRepository,
                settingsManager = settingsManager,
                aiService = aiService,
                deviceId = deviceId,
                gson = gson
            )
            
            val started = server?.start(port) ?: false
            
            if (started) {
                Log.i(TAG, "✅ Local Sync Server started successfully")
                updateServerStatus()
            }
            
            started
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
            false
        }
    }
    
    /**
     * Stop local sync server
     */
    fun stopServer() {
        try {
            server?.stop()
            server = null
            _serverStatus.value = null
            Log.i(TAG, "✅ Local Sync Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }
    
    /**
     * Check if server is running
     */
    fun isServerRunning(): Boolean = server?.isRunning() ?: false
    
    /**
     * Update server status
     */
    suspend fun updateServerStatus() {
        try {
            _serverStatus.value = server?.getServerStatus()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating server status", e)
        }
    }
    
    /**
     * Get current server status
     */
    suspend fun getServerStatus(): ServerStatus? {
        return try {
            server?.getServerStatus()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting server status", e)
            null
        }
    }
}
