package com.yourown.ai.data.llama

import android.content.Context
import android.util.Log
import com.llamatik.library.platform.LlamaBridge
import com.llamatik.library.platform.GenStream
import java.io.File
import java.nio.charset.Charset

/**
 * Wrapper for Llamatik library (com.llamatik:library)
 * Handles loading and running GGUF models using llama.cpp
 * Includes UTF-8 buffering to handle multi-byte characters (emojis)
 */
class LlamaCppWrapper(private val context: Context) {
    
    companion object {
        private const val TAG = "LlamaCppWrapper"
    }
    
    private var isModelLoaded = false
    private var currentModelName: String? = null
    private val lock = Any() // Synchronization lock
    
    /**
     * Load GGUF model from file
     */
    fun load(modelFile: File, contextSize: Int = 2048): Boolean {
        synchronized(lock) {
            return try {
                if (!modelFile.exists()) {
                    Log.e(TAG, "Model file not found: ${modelFile.absolutePath}")
                    return false
                }
                
                Log.i(TAG, "Loading model: ${modelFile.name}, size: ${modelFile.length() / 1024 / 1024}MB")
                
                // Unload previous model only if one is loaded
                if (isModelLoaded) {
                    unloadInternal()
                }
                
                // Llamatik expects absolute path to model file
                val success = LlamaBridge.initGenerateModel(modelFile.absolutePath)
                
                if (success) {
                    isModelLoaded = true
                    currentModelName = modelFile.name
                    Log.i(TAG, "Model loaded successfully: ${modelFile.name}")
                } else {
                    Log.e(TAG, "Failed to load model: ${modelFile.name}")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model", e)
                isModelLoaded = false
                currentModelName = null
                false
            }
        }
    }
    
    /**
     * Unload model and free memory
     */
    fun unload() {
        synchronized(lock) {
            unloadInternal()
        }
    }
    
    /**
     * Internal unload without synchronization (called from synchronized blocks)
     */
    private fun unloadInternal() {
        if (!isModelLoaded) {
            Log.d(TAG, "No model loaded, skipping unload")
            return
        }
        
        try {
            Log.i(TAG, "Unloading model: $currentModelName")
            LlamaBridge.shutdown()
            isModelLoaded = false
            currentModelName = null
            Log.i(TAG, "Model unloaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading model", e)
            // Force reset state even if shutdown fails
            isModelLoaded = false
            currentModelName = null
        }
    }
    
    /**
     * Check if model is loaded
     */
    fun isLoaded(): Boolean = isModelLoaded
    
    /**
     * Get current model name
     */
    fun getCurrentModelName(): String? = currentModelName
    
    /**
     * Generate text completion with callback-based streaming
     * Includes UTF-8 buffering to handle multi-byte characters safely
     */
    fun generateTextWithCallback(
        prompt: String,
        systemPrompt: String = "",
        contextBlock: String = "",
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        maxTokens: Int = 512,
        onToken: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isModelLoaded) {
            onError("Model not loaded")
            return
        }
        
        try {
            Log.d(TAG, "Starting generation with prompt length: ${prompt.length}")
            
            // UTF-8 buffer for incomplete multi-byte sequences
            val utf8Buffer = Utf8Buffer()
            
            // Use Llamatik's streaming API
            LlamaBridge.generateStream(
                prompt = prompt,
                callback = object : GenStream {
                    override fun onDelta(text: String) {
                        try {
                            // Buffer the text and emit only complete UTF-8 sequences
                            val safeText = utf8Buffer.append(text)
                            if (safeText.isNotEmpty()) {
                                onToken(safeText)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error processing token: ${e.message}")
                            // Try to send original text if buffering fails
                            try {
                                onToken(text)
                            } catch (e2: Exception) {
                                Log.e(TAG, "Failed to send token: ${e2.message}")
                            }
                        }
                    }
                    
                    override fun onComplete() {
                        // Flush any remaining buffered content
                        try {
                            val remaining = utf8Buffer.flush()
                            if (remaining.isNotEmpty()) {
                                onToken(remaining)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error flushing buffer: ${e.message}")
                        }
                        Log.i(TAG, "Generation completed")
                        onComplete()
                    }
                    
                    override fun onError(message: String) {
                        Log.e(TAG, "Generation error: $message")
                        onError(message)
                    }
                }
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during generation", e)
            onError(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Generate text completion (non-streaming)
     */
    fun generateText(prompt: String): String {
        if (!isModelLoaded) {
            throw IllegalStateException("Model not loaded")
        }
        
        return try {
            Log.d(TAG, "Starting non-streaming generation")
            LlamaBridge.generate(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Error during generation", e)
            throw e
        }
    }
    
    /**
     * Cancel ongoing generation
     */
    fun cancelGeneration() {
        try {
            LlamaBridge.nativeCancelGenerate()
            Log.i(TAG, "Generation cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling generation", e)
        }
    }
    
    /**
     * UTF-8 buffer to handle incomplete multi-byte sequences
     * Emojis are 4 bytes in UTF-8, and llama.cpp may split them across tokens
     */
    private class Utf8Buffer {
        private val buffer = ByteArray(8) // Max UTF-8 sequence is 6 bytes, use 8 for safety
        private var bufferSize = 0
        
        /**
         * Append text and return only complete UTF-8 sequences
         */
        fun append(text: String): String {
            if (text.isEmpty()) return ""
            
            val bytes = text.toByteArray(Charsets.UTF_8)
            
            // If we have buffered bytes, prepend them
            val allBytes = if (bufferSize > 0) {
                val combined = ByteArray(bufferSize + bytes.size)
                System.arraycopy(buffer, 0, combined, 0, bufferSize)
                System.arraycopy(bytes, 0, combined, bufferSize, bytes.size)
                bufferSize = 0
                combined
            } else {
                bytes
            }
            
            // Find where the last complete UTF-8 character ends
            val completeEnd = findCompleteUtf8End(allBytes)
            
            if (completeEnd == allBytes.size) {
                // All bytes form complete UTF-8 sequences
                return String(allBytes, Charsets.UTF_8)
            } else if (completeEnd == 0) {
                // No complete sequences, buffer everything
                System.arraycopy(allBytes, 0, buffer, 0, minOf(allBytes.size, buffer.size))
                bufferSize = minOf(allBytes.size, buffer.size)
                return ""
            } else {
                // Some complete sequences, some incomplete
                val remaining = allBytes.size - completeEnd
                System.arraycopy(allBytes, completeEnd, buffer, 0, minOf(remaining, buffer.size))
                bufferSize = minOf(remaining, buffer.size)
                return String(allBytes, 0, completeEnd, Charsets.UTF_8)
            }
        }
        
        /**
         * Flush any remaining bytes (may produce replacement characters for incomplete sequences)
         */
        fun flush(): String {
            if (bufferSize == 0) return ""
            val result = String(buffer, 0, bufferSize, Charsets.UTF_8)
            bufferSize = 0
            return result
        }
        
        /**
         * Find the index where complete UTF-8 sequences end
         */
        private fun findCompleteUtf8End(bytes: ByteArray): Int {
            if (bytes.isEmpty()) return 0
            
            var i = bytes.size - 1
            
            // Look backwards for the start of the last character
            while (i >= 0 && i >= bytes.size - 4) {
                val b = bytes[i].toInt() and 0xFF
                
                when {
                    // Single byte ASCII (0xxxxxxx)
                    b and 0x80 == 0x00 -> {
                        return bytes.size // Complete
                    }
                    // Start of 2-byte sequence (110xxxxx)
                    b and 0xE0 == 0xC0 -> {
                        val remaining = bytes.size - i
                        return if (remaining >= 2) bytes.size else i
                    }
                    // Start of 3-byte sequence (1110xxxx)
                    b and 0xF0 == 0xE0 -> {
                        val remaining = bytes.size - i
                        return if (remaining >= 3) bytes.size else i
                    }
                    // Start of 4-byte sequence (11110xxx) - emojis!
                    b and 0xF8 == 0xF0 -> {
                        val remaining = bytes.size - i
                        return if (remaining >= 4) bytes.size else i
                    }
                    // Continuation byte (10xxxxxx) - keep looking backwards
                    b and 0xC0 == 0x80 -> {
                        i--
                    }
                    else -> {
                        // Invalid UTF-8, return everything
                        return bytes.size
                    }
                }
            }
            
            // If we've looked back more than 4 bytes without finding a start, assume complete
            return bytes.size
        }
    }
}
