package com.yourown.ai.data.repository

import android.content.Context
import com.yourown.ai.di.DownloadClient
import com.yourown.ai.domain.model.DownloadStatus
import com.yourown.ai.domain.model.LocalModel
import com.yourown.ai.domain.model.LocalModelInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalModelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @DownloadClient private val okHttpClient: OkHttpClient
) {
    private val _models = MutableStateFlow<Map<LocalModel, LocalModelInfo>>(
        LocalModel.entries.associateWith { LocalModelInfo(it) }
    )
    val models: StateFlow<Map<LocalModel, LocalModelInfo>> = _models.asStateFlow()
    
    // Mutex to ensure only one model downloads at a time
    private val downloadMutex = Mutex()
    
    private val modelsDir = File(context.filesDir, "models").apply {
        if (!exists()) mkdirs()
    }
    
    init {
        // Check which models are already downloaded
        checkDownloadedModels()
        
        // Log OkHttpClient info
        android.util.Log.d("LocalModelRepository", "Initialized with OkHttpClient: ${okHttpClient.hashCode()}, interceptors: ${okHttpClient.interceptors.size}")
    }
    
    private fun checkDownloadedModels() {
        LocalModel.entries.forEach { model ->
            val file = File(modelsDir, model.modelName)
            // Check if file exists AND has correct size (within 5% tolerance)
            val expectedSize = model.sizeInMB * 1024 * 1024L
            val minSize = (expectedSize * 0.95).toLong() // Allow 5% tolerance
            
            if (file.exists() && file.length() >= minSize) {
                // File has correct size, but verify it's a valid GGUF file
                if (isValidGGUFFile(file)) {
                    _models.update { map ->
                        map.toMutableMap().apply {
                            this[model] = LocalModelInfo(
                                model = model,
                                status = DownloadStatus.Downloaded,
                                filePath = file.absolutePath
                            )
                        }
                    }
                } else {
                    // File is corrupt - delete it
                    android.util.Log.w("LocalModelRepository", 
                        "Model ${model.displayName} is CORRUPT (invalid GGUF header). Deleting.")
                    file.delete()
                    _models.update { map ->
                        map.toMutableMap().apply {
                            this[model] = LocalModelInfo(
                                model = model,
                                status = DownloadStatus.Failed("Corrupt file detected and removed - please redownload")
                            )
                        }
                    }
                }
            } else if (file.exists() && file.length() < minSize) {
                // File exists but is incomplete - DELETE IT and mark as failed
                android.util.Log.w("LocalModelRepository", 
                    "Model ${model.displayName} is incomplete: ${file.length()} bytes, expected ~$expectedSize bytes. Deleting corrupt file.")
                file.delete()
                _models.update { map ->
                    map.toMutableMap().apply {
                        this[model] = LocalModelInfo(
                            model = model,
                            status = DownloadStatus.Failed("Incomplete download detected and removed - please redownload")
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Check if file is a valid GGUF file by reading magic bytes
     * GGUF files start with "GGUF" magic bytes (0x47 0x47 0x55 0x46)
     */
    private fun isValidGGUFFile(file: File): Boolean {
        return try {
            file.inputStream().use { stream ->
                val magic = ByteArray(4)
                val bytesRead = stream.read(magic)
                
                // Check if we could read 4 bytes and they match "GGUF"
                bytesRead == 4 && 
                magic[0] == 0x47.toByte() && 
                magic[1] == 0x47.toByte() && 
                magic[2] == 0x55.toByte() && 
                magic[3] == 0x46.toByte()
            }
        } catch (e: Exception) {
            android.util.Log.e("LocalModelRepository", "Failed to check GGUF header", e)
            false
        }
    }
    
    suspend fun downloadModel(model: LocalModel) = withContext(Dispatchers.IO) {
        try {
            // Set status to Queued BEFORE trying to acquire mutex
            _models.update { map ->
                map.toMutableMap().apply {
                    this[model] = LocalModelInfo(model, DownloadStatus.Queued)
                }
            }
            android.util.Log.d("LocalModelRepository", "Model ${model.displayName} added to queue")
            
            // Use mutex to ensure only one download at a time
            downloadMutex.withLock {
                android.util.Log.d("LocalModelRepository", "Starting download: ${model.displayName}")
                
                // Update status to downloading (progress 0%)
                _models.update { map ->
                    map.toMutableMap().apply {
                        this[model] = LocalModelInfo(model, DownloadStatus.Downloading(0))
                    }
                }
                
                val request = Request.Builder()
                    .url(model.huggingFaceUrl)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw Exception("Download failed: ${response.code}")
                }
                
                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()
                
                android.util.Log.d("LocalModelRepository", "Content length: $contentLength bytes")
                
                val file = File(modelsDir, model.modelName)
                val outputStream = FileOutputStream(file)
                
                // Use smaller buffer (4KB) to reduce memory pressure
                val buffer = ByteArray(4096)
                var downloaded = 0L
                var bytesRead: Int
                var lastProgressUpdate = -1  // Changed to -1 to ensure first update at 0%
                var lastUpdateTime = System.currentTimeMillis()
                
                body.byteStream().use { inputStream ->
                    outputStream.use { output ->
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            
                            val currentTime = System.currentTimeMillis()
                            
                            // Update progress every 1% OR every 500ms (whichever comes first)
                            val progress = if (contentLength > 0) {
                                ((downloaded * 100) / contentLength).toInt()
                            } else {
                                0
                            }
                            
                            if (progress > lastProgressUpdate || currentTime - lastUpdateTime >= 500) {
                                lastProgressUpdate = progress
                                lastUpdateTime = currentTime
                                
                                _models.update { map ->
                                    map.toMutableMap().apply {
                                        this[model] = LocalModelInfo(
                                            model,
                                            DownloadStatus.Downloading(progress)
                                        )
                                    }
                                }
                                
                                android.util.Log.d("LocalModelRepository", "Progress: $progress% ($downloaded / $contentLength bytes)")
                                
                                // Request GC every 10% to prevent OOM
                                if (progress % 10 == 0 && progress > 0) {
                                    System.gc()
                                }
                            }
                        }
                    }
                }
                
                // Verify file size
                val finalSize = file.length()
                val expectedSize = model.sizeInMB * 1024 * 1024L
                val minSize = (expectedSize * 0.95).toLong()
                
                android.util.Log.d("LocalModelRepository", "Download complete. Final size: $finalSize, expected: $expectedSize")
                
                if (finalSize < minSize) {
                    file.delete()
                    throw Exception("Download incomplete: got $finalSize bytes, expected ~$expectedSize bytes")
                }
                
                // Download complete
                _models.update { map ->
                    map.toMutableMap().apply {
                        this[model] = LocalModelInfo(
                            model = model,
                            status = DownloadStatus.Downloaded,
                            filePath = file.absolutePath
                        )
                    }
                }
                
                android.util.Log.d("LocalModelRepository", "Download successful: ${model.displayName}")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("LocalModelRepository", "Download failed: ${model.displayName}", e)
            
            // Delete incomplete file on error
            val file = File(modelsDir, model.modelName)
            if (file.exists()) {
                android.util.Log.d("LocalModelRepository", "Deleting incomplete file: ${file.absolutePath}")
                file.delete()
            }
            
            _models.update { map ->
                map.toMutableMap().apply {
                    this[model] = LocalModelInfo(
                        model,
                        DownloadStatus.Failed(e.message ?: "Unknown error")
                    )
                }
            }
        }
    }
    
    suspend fun deleteModel(model: LocalModel) = withContext(Dispatchers.IO) {
        val file = File(modelsDir, model.modelName)
        if (file.exists()) {
            file.delete()
        }
        
        _models.update { map ->
            map.toMutableMap().apply {
                this[model] = LocalModelInfo(model, DownloadStatus.NotDownloaded)
            }
        }
    }
    
    fun getModelPath(model: LocalModel): String? {
        return _models.value[model]?.filePath
    }
    
    /**
     * Force delete ALL models and clear cache
     * Useful for cleaning up corrupt files
     */
    suspend fun forceDeleteAll() = withContext(Dispatchers.IO) {
        android.util.Log.d("LocalModelRepository", "Force deleting all models...")
        
        // Delete all files in models directory
        modelsDir.listFiles()?.forEach { file ->
            android.util.Log.d("LocalModelRepository", "Deleting: ${file.name}")
            file.delete()
        }
        
        // Reset all statuses
        _models.update {
            LocalModel.entries.associateWith { model -> 
                LocalModelInfo(model, DownloadStatus.NotDownloaded)
            }
        }
        
        android.util.Log.d("LocalModelRepository", "All models deleted")
    }
}
