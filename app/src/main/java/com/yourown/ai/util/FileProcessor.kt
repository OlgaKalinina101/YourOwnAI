package com.yourown.ai.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Utility for handling file attachments (PDF, TXT, DOC, DOCX)
 */
object FileProcessor {
    
    private const val TAG = "FileProcessor"
    private const val MAX_FILE_SIZE_MB = 50 // 50MB max per OpenAI docs
    private const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024
    
    /**
     * Save file from URI to app's cache directory
     * @return File path or null if failed
     */
    fun saveFileToCache(context: Context, uri: Uri): String? {
        return try {
            // Get file info
            val fileInfo = getFileInfo(context, uri) ?: return null
            
            // Check file size
            if (fileInfo.second > MAX_FILE_SIZE_BYTES) {
                Log.e(TAG, "File too large: ${fileInfo.second / 1024 / 1024}MB (max: ${MAX_FILE_SIZE_MB}MB)")
                return null
            }
            
            // Create cache directory
            val cacheDir = File(context.cacheDir, "files")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            // Generate unique filename
            val fileName = fileInfo.first
            val timestamp = System.currentTimeMillis()
            val outputFile = File(cacheDir, "${timestamp}_$fileName")
            
            // Copy file to cache
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file to cache", e)
            null
        }
    }
    
    /**
     * Get file name and size from URI
     * @return Pair<fileName, sizeBytes> or null
     */
    fun getFileInfo(context: Context, uri: Uri): Pair<String, Long>? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex) ?: "unknown"
                    val size = cursor.getLong(sizeIndex)
                    Pair(name, size)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file info", e)
            null
        }
    }
    
    /**
     * Get file extension from file name
     */
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }
    
    /**
     * Encode file to Base64 for API upload
     */
    fun encodeFileToBase64(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: $filePath")
                return null
            }
            
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding file to base64", e)
            null
        }
    }
    
    /**
     * Get file size in MB
     */
    fun getFileSizeMB(filePath: String): Float {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.length() / (1024f * 1024f)
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * Delete file from cache
     */
    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            false
        }
    }
    
    /**
     * Clean up old cached files (older than 7 days)
     */
    fun cleanupOldFiles(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "files")
            if (!cacheDir.exists()) return
            
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            
            cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < sevenDaysAgo) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old files", e)
        }
    }
}
