package com.yourown.ai.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Utility for compressing and encoding images for API upload
 */
object ImageCompressor {
    
    private const val MAX_IMAGE_SIZE_KB = 4096 // 4MB max for most APIs
    private const val INITIAL_QUALITY = 90
    private const val MIN_QUALITY = 60
    private const val MAX_DIMENSION = 2048 // Max width/height
    
    /**
     * Compress image from URI and return Base64 encoded string
     */
    fun compressAndEncode(context: Context, uri: Uri): String? {
        return try {
            val bitmap = loadBitmapFromUri(context, uri) ?: return null
            val compressed = compressBitmap(bitmap)
            encodeToBase64(compressed)
        } catch (e: Exception) {
            android.util.Log.e("ImageCompressor", "Error compressing image", e)
            null
        }
    }
    
    /**
     * Save compressed image to app's cache directory
     */
    fun saveCompressedImage(context: Context, uri: Uri): String? {
        return try {
            val bitmap = loadBitmapFromUri(context, uri) ?: return null
            val compressed = compressBitmap(bitmap)
            
            val cacheDir = File(context.cacheDir, "images")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val file = File(cacheDir, "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                compressed.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ImageCompressor", "Error saving image", e)
            null
        }
    }
    
    /**
     * Load bitmap from URI with proper orientation
     */
    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            // Fix orientation
            val orientationCorrectedBitmap = fixOrientation(context, uri, bitmap)
            orientationCorrectedBitmap
        } catch (e: Exception) {
            android.util.Log.e("ImageCompressor", "Error loading bitmap", e)
            null
        }
    }
    
    /**
     * Fix image orientation based on EXIF data
     */
    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            android.util.Log.e("ImageCompressor", "Error fixing orientation", e)
            bitmap
        }
    }
    
    /**
     * Compress bitmap to target size
     */
    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        // Resize if too large
        val resized = if (bitmap.width > MAX_DIMENSION || bitmap.height > MAX_DIMENSION) {
            val ratio = Math.min(
                MAX_DIMENSION.toFloat() / bitmap.width,
                MAX_DIMENSION.toFloat() / bitmap.height
            )
            val newWidth = (bitmap.width * ratio).toInt()
            val newHeight = (bitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
        
        // Compress to target file size
        var quality = INITIAL_QUALITY
        var outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        
        while (outputStream.size() > MAX_IMAGE_SIZE_KB * 1024 && quality > MIN_QUALITY) {
            outputStream = ByteArrayOutputStream()
            quality -= 10
            resized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }
        
        return resized
    }
    
    /**
     * Encode bitmap to Base64 string
     */
    private fun encodeToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    
    /**
     * Get image dimensions without loading full bitmap
     */
    fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            null
        }
    }
}
