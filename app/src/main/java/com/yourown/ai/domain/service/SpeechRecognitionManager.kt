package com.yourown.ai.domain.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manager for handling speech recognition (voice input)
 */
class SpeechRecognitionManager(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()
    
    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        initializeSpeechRecognizer()
    }
    
    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _error.value = null
                    }
                    
                    override fun onBeginningOfSpeech() {
                        // Speech input has begun
                    }
                    
                    override fun onRmsChanged(rmsdB: Float) {
                        // Volume level changed - could be used for visual feedback
                    }
                    
                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Partial results buffer
                    }
                    
                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }
                    
                    override fun onError(error: Int) {
                        _isListening.value = false
                        _error.value = getErrorMessage(error)
                    }
                    
                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                            if (matches.isNotEmpty()) {
                                _recognizedText.value = matches[0]
                                _partialText.value = "" // Clear partial text
                            }
                        }
                    }
                    
                    override fun onPartialResults(partialResults: Bundle?) {
                        // Partial recognition results - for live feedback only, don't update recognizedText
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                            if (matches.isNotEmpty()) {
                                _partialText.value = matches[0]
                            }
                        }
                    }
                    
                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // Reserved for future use
                    }
                })
            }
        }
    }
    
    /**
     * Start listening for speech input
     */
    fun startListening() {
        if (_isListening.value) return
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _error.value = "Speech recognition is not available on this device"
            return
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        _recognizedText.value = ""
        _partialText.value = ""
        _error.value = null
        speechRecognizer?.startListening(intent)
    }
    
    /**
     * Stop listening for speech input
     */
    fun stopListening() {
        if (_isListening.value) {
            speechRecognizer?.stopListening()
            _isListening.value = false
        }
    }
    
    /**
     * Cancel speech recognition
     */
    fun cancel() {
        speechRecognizer?.cancel()
        _isListening.value = false
        _recognizedText.value = ""
        _partialText.value = ""
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        _isListening.value = false
    }
    
    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
            else -> "Recognition error"
        }
    }
}
