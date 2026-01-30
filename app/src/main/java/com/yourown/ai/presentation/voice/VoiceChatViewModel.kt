package com.yourown.ai.presentation.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourown.ai.data.remote.xai.XAIVoiceClient
import com.yourown.ai.data.repository.ApiKeyRepository
import com.yourown.ai.domain.model.AIProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class VoiceChatUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isRecording: Boolean = false,
    val isPlaying: Boolean = false,
    val selectedVoice: XAIVoiceClient.Voice = XAIVoiceClient.Voice.ARA,
    val transcript: String = "",
    val messages: List<VoiceMessage> = emptyList(),
    val audioLevel: Float = 0f,
    val error: String? = null,
    val sessionId: String? = null,
    val hasApiKey: Boolean = false,
    val systemPrompts: List<com.yourown.ai.domain.model.SystemPrompt> = emptyList(),
    val selectedSystemPromptId: String? = null,
    val userContext: com.yourown.ai.domain.model.UserContext = com.yourown.ai.domain.model.UserContext()
)

data class VoiceMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val requestLogs: String? = null
)

enum class MessageRole {
    USER, ASSISTANT
}

@HiltViewModel
class VoiceChatViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val voiceClient: XAIVoiceClient,
    private val systemPromptRepository: com.yourown.ai.data.repository.SystemPromptRepository,
    private val aiConfigRepository: com.yourown.ai.data.repository.AIConfigRepository,
    private val voiceMessagePreferences: com.yourown.ai.data.local.preferences.VoiceMessagePreferences
) : ViewModel() {
    
    companion object {
        private const val TAG = "VoiceChatViewModel"
        private const val SAMPLE_RATE = 24000 // 24kHz for PCM16
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    private val _uiState = MutableStateFlow(VoiceChatUiState())
    val uiState: StateFlow<VoiceChatUiState> = _uiState.asStateFlow()
    
    private var voiceSessionJob: Job? = null
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null
    
    // AudioTrack with synchronization
    @Volatile
    private var audioTrack: AudioTrack? = null
    private val audioTrackLock = Any()
    
    init {
        // Check API key on initialization
        checkApiKey()
        // Observe system prompts
        observeSystemPrompts()
        // Initialize default prompts
        initializeDefaultPrompts()
        // Observe user context
        observeUserContext()
        // Load previous messages
        loadPreviousMessages()
    }
    
    private fun loadPreviousMessages() {
        viewModelScope.launch {
            val savedMessages = voiceMessagePreferences.loadMessages()
            if (savedMessages.isNotEmpty()) {
                Log.d(TAG, "Loaded ${savedMessages.size} previous voice messages")
                _uiState.update { it.copy(messages = savedMessages) }
            }
        }
    }
    
    private fun initializeDefaultPrompts() {
        viewModelScope.launch {
            systemPromptRepository.initializeDefaultPrompts()
        }
    }
    
    private fun observeSystemPrompts() {
        viewModelScope.launch {
            systemPromptRepository.getAllPrompts().collect { prompts ->
                // Filter only API prompts (Voice Chat uses xAI API)
                val apiPrompts = prompts.filter { it.type == com.yourown.ai.data.repository.PromptType.API }
                _uiState.update { state ->
                    val currentSelected = state.selectedSystemPromptId
                    val newSelected = if (currentSelected == null) {
                        // Auto-select default API prompt
                        apiPrompts.firstOrNull { it.isDefault }?.id
                    } else {
                        currentSelected
                    }
                    state.copy(
                        systemPrompts = apiPrompts,
                        selectedSystemPromptId = newSelected
                    )
                }
            }
        }
    }
    
    private fun observeUserContext() {
        viewModelScope.launch {
            aiConfigRepository.userContext.collect { context ->
                _uiState.update { it.copy(userContext = context) }
            }
        }
    }
    
    /**
     * Check if API key is set
     */
    fun checkApiKey() {
        val apiKey = apiKeyRepository.getApiKey(AIProvider.XAI)
        _uiState.update { it.copy(hasApiKey = !apiKey.isNullOrEmpty()) }
    }
    
    /**
     * Connect to voice session
     */
    fun connect() {
        Log.i(TAG, "connect() called")
        
        if (_uiState.value.isConnected || _uiState.value.isConnecting) {
            Log.w(TAG, "Already connected or connecting")
            return
        }
        
        val apiKey = apiKeyRepository.getApiKey(AIProvider.XAI)
        if (apiKey.isNullOrEmpty()) {
            _uiState.update { it.copy(error = "x.ai API key not set") }
            return
        }
        
        // Cancel previous job if any
        voiceSessionJob?.cancel()
        
        val voiceBeforeConnect = _uiState.value.selectedVoice
        Log.i(TAG, "Voice from state before connecting: ${voiceBeforeConnect.displayName} (${voiceBeforeConnect.id})")
        
        _uiState.update { it.copy(isConnecting = true, error = null) }
        
        voiceSessionJob = viewModelScope.launch {
            try {
                val voice = _uiState.value.selectedVoice
                Log.i(TAG, "Connecting to Voice API with voice: ${voice.displayName} (${voice.id})")
                
                // Get selected system prompt or default
                val promptId = _uiState.value.selectedSystemPromptId
                val prompt = if (promptId != null) {
                    systemPromptRepository.getPromptById(promptId)
                } else {
                    systemPromptRepository.getDefaultApiPrompt()
                }
                
                val baseSystemPrompt = prompt?.content 
                    ?: "You are a helpful AI assistant. Be concise and natural."
                
                // Add user context if present
                val userContextContent = _uiState.value.userContext.content
                val fullSystemPrompt = if (userContextContent.isNotBlank()) {
                    "$baseSystemPrompt\n\n$userContextContent"
                } else {
                    baseSystemPrompt
                }
                
                Log.i(TAG, "Using system prompt: ${prompt?.name ?: "default"}")
                Log.i(TAG, "User context: ${if (userContextContent.isNotBlank()) "Yes (${userContextContent.length} chars)" else "No"}")
                
                voiceClient.connect(
                    apiKey = apiKey,
                    voice = voice,
                    systemPrompt = fullSystemPrompt
                ).collect { event ->
                    handleVoiceEvent(event)
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Voice session cancelled")
                _uiState.update { it.copy(isConnecting = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Voice session error", e)
                _uiState.update { 
                    it.copy(
                        error = "Session failed: ${e.message}", 
                        isConnected = false,
                        isConnecting = false
                    ) 
                }
            }
        }
    }
    
    /**
     * Disconnect from voice session
     */
    fun disconnect() {
        voiceSessionJob?.cancel()
        voiceClient.disconnect()
        stopPlayback()
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        _uiState.update { 
            it.copy(
                isConnected = false, 
                isConnecting = false,
                isRecording = false, 
                isPlaying = false
            ) 
        }
        Log.i(TAG, "Disconnected from Voice API")
    }
    
    /**
     * Handle voice events from WebSocket
     */
    private fun handleVoiceEvent(event: XAIVoiceClient.VoiceEvent) {
        when (event) {
            is XAIVoiceClient.VoiceEvent.Connected -> {
                _uiState.update { 
                    it.copy(
                        isConnected = true,
                        isConnecting = false,
                        sessionId = event.sessionId,
                        error = null
                    ) 
                }
                Log.i(TAG, "Voice session connected: ${event.sessionId}")
            }
            
            is XAIVoiceClient.VoiceEvent.AudioReceived -> {
                playAudio(event.audioData)
            }
            
            is XAIVoiceClient.VoiceEvent.TranscriptReceived -> {
                if (event.isFinal) {
                    // User transcript (final) - saved to local preferences for history
                    val message = VoiceMessage(
                        id = System.currentTimeMillis().toString(),
                        role = MessageRole.USER,
                        text = event.text
                    )
                    _uiState.update { 
                        it.copy(
                            messages = it.messages + message,
                            transcript = ""
                        ) 
                    }
                    // Save messages to preferences
                    saveMessages()
                    Log.d(TAG, "Added user message: ${event.text}")
                } else {
                    // AI transcript (final) - saved to local preferences for history
                    // Build request logs
                    val requestLogs = buildVoiceRequestLogs()
                    
                    val message = VoiceMessage(
                        id = System.currentTimeMillis().toString(),
                        role = MessageRole.ASSISTANT,
                        text = event.text,
                        requestLogs = requestLogs
                    )
                    _uiState.update { 
                        it.copy(messages = it.messages + message) 
                    }
                    // Save messages to preferences
                    saveMessages()
                    Log.d(TAG, "Added AI message: ${event.text}")
                }
            }
            
            is XAIVoiceClient.VoiceEvent.ResponseStarted -> {
                _uiState.update { it.copy(isPlaying = true) }
                Log.d(TAG, "AI response started: ${event.responseId}")
            }
            
            is XAIVoiceClient.VoiceEvent.ResponseCompleted -> {
                _uiState.update { it.copy(isPlaying = false) }
                Log.d(TAG, "AI response completed: ${event.responseId}")
            }
            
            is XAIVoiceClient.VoiceEvent.Error -> {
                // Suppress SocketException on disconnect (normal when app closes)
                val exception = event.exception
                if (exception is java.net.SocketException && 
                    (exception.message?.contains("Connection abort") == true || 
                     exception.message?.contains("Socket closed") == true)) {
                    Log.d(TAG, "Connection closed (expected on disconnect)")
                } else {
                    _uiState.update { it.copy(error = event.message) }
                    Log.e(TAG, "Voice error: ${event.message}", exception)
                }
            }
            
            is XAIVoiceClient.VoiceEvent.Disconnected -> {
                _uiState.update { 
                    it.copy(
                        isConnected = false, 
                        isConnecting = false,
                        isRecording = false, 
                        isPlaying = false
                    ) 
                }
                Log.i(TAG, "Voice session disconnected")
            }
        }
    }
    
    /**
     * Start recording audio
     */
    fun startRecording() {
        if (_uiState.value.isRecording) return
        if (!_uiState.value.isConnected) {
            _uiState.update { it.copy(error = "Not connected to voice session") }
            return
        }
        
        recordingJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val bufferSize = AudioRecord.getMinBufferSize(
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT
                    )
                    
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufferSize
                    )
                    
                    audioRecord?.startRecording()
                    _uiState.update { it.copy(isRecording = true) }
                    Log.i(TAG, "Started recording")
                    
                    val buffer = ByteArray(bufferSize)
                    
                    while (_uiState.value.isRecording) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            voiceClient.sendAudio(buffer.copyOf(read))
                            Log.d(TAG, "Sent audio data: $read bytes")
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Recording cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Recording error", e)
                _uiState.update { it.copy(error = "Recording failed: ${e.message}", isRecording = false) }
            }
        }
    }
    
    /**
     * Stop recording audio
     */
    fun stopRecording() {
        _uiState.update { it.copy(isRecording = false) }
        recordingJob?.cancel()
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
        
        audioRecord = null
        
        // Commit audio buffer to trigger transcription
        voiceClient.commitAudio()
        Log.d(TAG, "Committed audio buffer")
        Log.i(TAG, "Stopped recording")
    }
    
    /**
     * Play audio from AI response
     */
    private fun playAudio(audioData: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                synchronized(audioTrackLock) {
                    var track = audioTrack
                    
                    // Initialize AudioTrack if needed
                    if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                        // Clean up old track if exists
                        try {
                            track?.release()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error releasing old AudioTrack", e)
                        }
                        
                        val minBufferSize = AudioTrack.getMinBufferSize(
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AUDIO_FORMAT
                        )
                        
                        if (minBufferSize <= 0) {
                            Log.e(TAG, "Invalid AudioTrack buffer size: $minBufferSize")
                            return@synchronized
                        }
                        
                        track = AudioTrack(
                            android.media.AudioManager.STREAM_MUSIC,
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AUDIO_FORMAT,
                            minBufferSize * 4,
                            AudioTrack.MODE_STREAM
                        )
                        
                        if (track.state == AudioTrack.STATE_INITIALIZED) {
                            track.play()
                            audioTrack = track
                            Log.d(TAG, "AudioTrack initialized and playing")
                        } else {
                            Log.e(TAG, "AudioTrack failed to initialize")
                            return@synchronized
                        }
                    }
                    
                    // Write audio data
                    try {
                        if (track.state == AudioTrack.STATE_INITIALIZED && 
                            track.playState != AudioTrack.PLAYSTATE_STOPPED) {
                            val written = track.write(audioData, 0, audioData.size)
                            if (written < 0) {
                                Log.e(TAG, "AudioTrack write error: $written")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error writing to AudioTrack", e)
                        // Release and recreate on next call
                        try {
                            track.release()
                        } catch (ignored: Exception) {}
                        audioTrack = null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Playback error", e)
            }
        }
    }
    
    /**
     * Stop audio playback
     */
    private fun stopPlayback() {
        synchronized(audioTrackLock) {
            try {
                audioTrack?.let { track ->
                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            track.pause()
                            track.flush()
                        }
                        track.stop()
                    }
                    track.release()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping playback", e)
            } finally {
                audioTrack = null
            }
        }
    }
    
    /**
     * Change voice
     */
    fun selectVoice(voice: XAIVoiceClient.Voice) {
        Log.i(TAG, "selectVoice called: ${voice.displayName}")
        val wasConnected = _uiState.value.isConnected
        val currentVoice = _uiState.value.selectedVoice
        
        Log.i(TAG, "Current voice: ${currentVoice.displayName}, New voice: ${voice.displayName}, Connected: $wasConnected")
        
        if (currentVoice == voice) {
            Log.i(TAG, "Same voice selected, ignoring")
            return
        }
        
        // Update voice first
        _uiState.update { it.copy(selectedVoice = voice) }
        Log.i(TAG, "Voice updated in state: ${_uiState.value.selectedVoice.displayName}")
        
        if (wasConnected) {
            // Reconnect with new voice
            Log.i(TAG, "Voice changed to ${voice.displayName}, reconnecting...")
            disconnect()
            
            // Wait for WebSocket to fully close before reconnecting
            viewModelScope.launch {
                kotlinx.coroutines.delay(1000)
                val voiceToConnect = _uiState.value.selectedVoice
                Log.i(TAG, "About to reconnect with voice from state: ${voiceToConnect.displayName}")
                connect()
            }
        } else {
            Log.i(TAG, "Not connected, voice will be used on next connect")
        }
    }
    
    /**
     * Select system prompt (like in ChatViewModel)
     */
    fun selectSystemPrompt(promptId: String) {
        Log.i(TAG, "selectSystemPrompt called: $promptId")
        val wasConnected = _uiState.value.isConnected
        val currentPromptId = _uiState.value.selectedSystemPromptId
        
        if (currentPromptId == promptId) {
            Log.i(TAG, "Same prompt selected, ignoring")
            return
        }
        
        // Update prompt
        _uiState.update { it.copy(selectedSystemPromptId = promptId) }
        Log.i(TAG, "System prompt updated in state")
        
        // Increment usage count
        viewModelScope.launch {
            systemPromptRepository.incrementUsageCount(promptId)
        }
        
        if (wasConnected) {
            // Reconnect with new prompt
            Log.i(TAG, "System prompt changed, reconnecting...")
            disconnect()
            
            // Wait for WebSocket to fully close before reconnecting
            viewModelScope.launch {
                kotlinx.coroutines.delay(1000)
                Log.i(TAG, "Reconnecting with new system prompt")
                connect()
            }
        } else {
            Log.i(TAG, "Not connected, prompt will be used on next connect")
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Clear messages
     */
    fun clearMessages() {
        _uiState.update { it.copy(messages = emptyList()) }
        // Clear from preferences too
        viewModelScope.launch {
            voiceMessagePreferences.clearMessages()
            Log.d(TAG, "Voice chat history cleared")
        }
    }
    
    private fun saveMessages() {
        viewModelScope.launch {
            val messages = _uiState.value.messages
            voiceMessagePreferences.saveMessages(messages)
            Log.d(TAG, "Saved ${messages.size} voice messages to preferences")
        }
    }
    
    /**
     * Build request logs for Voice Chat (similar to ChatViewModel)
     */
    private fun buildVoiceRequestLogs(): String {
        val state = _uiState.value
        val promptId = state.selectedSystemPromptId
        val prompt = state.systemPrompts.firstOrNull { it.id == promptId }
        val userContextContent = state.userContext.content
        
        return buildString {
            appendLine("=== Voice Chat Request ===")
            appendLine()
            appendLine("Provider: X.AI (grok-beta)")
            appendLine("Voice: ${state.selectedVoice.displayName} (${state.selectedVoice.id})")
            appendLine("Session: ${state.sessionId ?: "N/A"}")
            appendLine()
            appendLine("System Prompt:")
            appendLine("  Name: ${prompt?.name ?: "Default"}")
            appendLine("  Content: ${prompt?.content ?: "Default prompt"}")
            appendLine()
            if (userContextContent.isNotBlank()) {
                appendLine("User Context:")
                appendLine("  ${userContextContent.replace("\n", "\n  ")}")
                appendLine()
            }
            appendLine("Audio Format: PCM16 (24kHz, mono)")
            appendLine("Message Count: ${state.messages.size}")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
