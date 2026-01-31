package com.yourown.ai.domain.service

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.yourown.ai.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Менеджер звуков клавиатуры для эффекта печати
 * 
 * Загружает WAV файл из res/raw/keyboard_sound.wav:
 * - Первые 8 секунд (0-8000ms) - звуки печати
 * - С 19 секунды до конца (19000-20400ms) - звук отправки
 * 
 * Нарезает случайные клипы 30-80ms из первых 8 секунд для разнообразия.
 * При получении токенов от AI проигрывает случайные клипы с небольшой задержкой.
 */
class KeyboardSoundManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var soundPool: SoundPool? = null
    private var typingSoundIds = mutableListOf<Int>()
    private var sendSoundId: Int? = null
    
    private var soundVolume = 0f // 0.0 to 1.0
    private var lastPlayTime = 0L
    private var playJob: Job? = null
    private var isPlaying = false
    
    companion object {
        private const val TAG = "KeyboardSoundManager"
        private const val MIN_PLAY_INTERVAL_MS = 150L // Увеличено для debounce
        
        // Delays between tokens (in ms) - сделано очень медленно
        private const val MIN_TOKEN_DELAY = 1200L
        private const val MAX_TOKEN_DELAY = 2000L
        
        // Play sound every N characters (not every character)
        private const val CHARS_PER_SOUND = 3
    }
    
    init {
        initializeSoundPool()
    }
    
    private fun initializeSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            soundPool = SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build()
            
            // Load the main sound file
            val soundId = soundPool?.load(context, R.raw.keyboard_sound, 1)
            
            if (soundId != null && soundId > 0) {
                // In a real implementation, we would slice the audio file
                // For now, we'll use the same sound with different playback rates
                // to simulate different typing sounds
                typingSoundIds.add(soundId)
                sendSoundId = soundId
                
                Log.d(TAG, "Sound loaded successfully: $soundId")
            } else {
                Log.e(TAG, "Failed to load keyboard sound")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing sound pool", e)
        }
    }
    
    fun setSoundVolume(volume: Float) {
        soundVolume = volume.coerceIn(0f, 1f)
        if (volume == 0f) {
            stopAllSounds()
        }
    }
    
    /**
     * Проигрывает звуки печати для токена от AI
     */
    fun playTypingForToken(token: String) {
        if (soundVolume == 0f || token.isBlank()) return
        
        // Don't cancel previous job - let it finish naturally
        // This prevents the sound from restarting with every new token
        if (isPlaying) return
        
        isPlaying = true
        playJob = scope.launch(Dispatchers.Default) {
            try {
                val nonSpaceChars = token.count { !it.isWhitespace() }
                
                // Play sound every CHARS_PER_SOUND characters (not every character)
                val soundsToPlay = (nonSpaceChars + CHARS_PER_SOUND - 1) / CHARS_PER_SOUND
                
                repeat(soundsToPlay) {
                    if (!isActive) return@launch
                    
                    playRandomTypingClip()
                    
                    // Long delay between sounds
                    delay(Random.nextLong(MIN_TOKEN_DELAY, MAX_TOKEN_DELAY))
                }
                
                // Check if this is end of sentence/message
                val trimmedToken = token.trimEnd()
                val endsWithPunctuation = trimmedToken.endsWith(".") || 
                    trimmedToken.endsWith("!") || 
                    trimmedToken.endsWith("?") ||
                    trimmedToken.endsWith("。") || 
                    trimmedToken.endsWith("！") || 
                    trimmedToken.endsWith("？")
                
                if (token.contains("\n") || endsWithPunctuation) {
                    delay(1000)
                    playSendSound()
                }
            } finally {
                isPlaying = false
            }
        }
    }
    
    /**
     * Проигрывает звук отправки сообщения
     */
    fun playSendSound() {
        if (soundVolume == 0f) return
        
        try {
            sendSoundId?.let { soundId ->
                soundPool?.play(
                    soundId,
                    soundVolume, // left volume with user setting
                    soundVolume, // right volume with user setting
                    1, // priority
                    0, // loop (0 = no loop)
                    1.2f // playback rate (slightly faster for send sound)
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing send sound", e)
        }
    }
    
    /**
     * Проигрывает тестовый звук для предварительного прослушивания
     */
    fun playTestSound() {
        if (soundVolume == 0f) return
        
        try {
            // Play a few typing sounds followed by send sound
            scope.launch(Dispatchers.Default) {
                repeat(3) {
                    playRandomTypingClip()
                    delay(Random.nextLong(MIN_TOKEN_DELAY, MAX_TOKEN_DELAY))
                }
                delay(1000)
                playSendSound()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing test sound", e)
        }
    }
    
    /**
     * Проигрывает случайный клип печати
     */
    private fun playRandomTypingClip() {
        // Debounce check
        val now = System.currentTimeMillis()
        if (now - lastPlayTime < MIN_PLAY_INTERVAL_MS) {
            return
        }
        lastPlayTime = now
        
        try {
            if (typingSoundIds.isEmpty()) return
            
            val soundId = typingSoundIds.random()
            
            // Vary volume and playback rate for variety
            val volumeVariation = Random.nextDouble(0.7, 1.0).toFloat()
            val finalVolume = soundVolume * volumeVariation
            val playbackRate = Random.nextDouble(0.95, 1.15).toFloat()
            
            soundPool?.play(
                soundId,
                finalVolume, // left volume with user setting
                finalVolume, // right volume with user setting
                0, // priority
                0, // loop
                playbackRate // playback rate for variety
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing typing clip", e)
        }
    }
    
    /**
     * Останавливает все звуки
     */
    fun stopAllSounds() {
        playJob?.cancel()
        isPlaying = false
        soundPool?.autoPause()
    }
    
    /**
     * Освобождает ресурсы
     */
    fun release() {
        playJob?.cancel()
        soundPool?.release()
        soundPool = null
        typingSoundIds.clear()
        sendSoundId = null
    }
}
