package com.yourown.ai.presentation.settings.managers

import com.yourown.ai.data.repository.LocalModelRepository
import com.yourown.ai.domain.model.LocalModel
import javax.inject.Inject

/**
 * Manager for Local Models (Qwen, Llama, etc.)
 * Handles: Downloading and deleting local AI models
 */
class LocalModelManager @Inject constructor(
    private val localModelRepository: LocalModelRepository
) {
    
    /**
     * Download local model
     */
    suspend fun downloadModel(model: LocalModel) {
        localModelRepository.downloadModel(model)
    }
    
    /**
     * Delete local model
     */
    suspend fun deleteModel(model: LocalModel) {
        localModelRepository.deleteModel(model)
    }
    
    /**
     * Force delete all local models
     */
    suspend fun forceDeleteAllModels() {
        localModelRepository.forceDeleteAll()
    }
}
