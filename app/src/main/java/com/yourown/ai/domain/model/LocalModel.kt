package com.yourown.ai.domain.model

/**
 * Local AI models that can be downloaded and run on device
 */
enum class LocalModel(
    val displayName: String,
    val modelName: String,
    val sizeInMB: Int,
    val huggingFaceUrl: String,
    val description: String
) {
    QWEN_1_7B(
        displayName = "Qwen 2.5 1.7B",
        modelName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
        sizeInMB = 950,
        huggingFaceUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
        description = "Fast and efficient, good for quick responses"
    ),
    LLAMA_3_2_3B(
        displayName = "Llama 3.2 3B",
        modelName = "llama-3.2-3b-instruct-q4_k_m.gguf",
        sizeInMB = 1900,
        huggingFaceUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
        description = "More capable, better quality responses"
    );
    
    fun getSizeFormatted(): String {
        return if (sizeInMB >= 1000) {
            "%.1f GB".format(sizeInMB / 1024f)
        } else {
            "$sizeInMB MB"
        }
    }
}

/**
 * Download status for local models
 */
sealed class DownloadStatus {
    object NotDownloaded : DownloadStatus()
    object Queued : DownloadStatus() // Waiting in queue for download to start
    data class Downloading(val progress: Int) : DownloadStatus() // 0-100
    object Downloaded : DownloadStatus()
    data class Failed(val error: String) : DownloadStatus()
}

/**
 * Local model info
 */
data class LocalModelInfo(
    val model: LocalModel,
    val status: DownloadStatus = DownloadStatus.NotDownloaded,
    val filePath: String? = null
)
