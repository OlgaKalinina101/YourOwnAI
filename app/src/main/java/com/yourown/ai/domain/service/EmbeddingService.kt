package com.yourown.ai.domain.service

import com.yourown.ai.domain.model.LocalEmbeddingModel

/**
 * Service for embedding model inference
 * Supports both local models (via llama.cpp) and API embeddings (OpenAI, OpenRouter)
 * Used for semantic search, RAG, and similarity matching
 */
interface EmbeddingService {
    
    /**
     * Load an embedding model from local storage
     */
    suspend fun loadModel(model: LocalEmbeddingModel): Result<Unit>
    
    /**
     * Unload current embedding model
     */
    suspend fun unloadModel()
    
    /**
     * Check if an embedding model is currently loaded
     */
    fun isModelLoaded(): Boolean
    
    /**
     * Get currently loaded embedding model
     */
    fun getCurrentModel(): LocalEmbeddingModel?
    
    /**
     * Generate embedding vector for a single text
     * Automatically uses API or local model based on configuration
     * @param text Input text to embed
     * @return Result containing embedding vector (FloatArray)
     */
    suspend fun generateEmbedding(text: String): Result<FloatArray>
    
    /**
     * Generate embedding vectors for multiple texts (batch processing)
     * Automatically uses API or local model based on configuration
     * @param texts List of texts to embed
     * @return Result containing list of embedding vectors
     */
    suspend fun generateEmbeddings(texts: List<String>): Result<List<FloatArray>>
    
    /**
     * Generate embedding using API (OpenAI or OpenRouter)
     * @param text Input text to embed
     * @param provider API provider ("openai" or "openrouter")
     * @param model Model identifier (e.g., "text-embedding-3-small")
     * @return Result containing embedding vector
     */
    suspend fun generateApiEmbedding(
        text: String,
        provider: String,
        model: String
    ): Result<FloatArray>
    
    /**
     * Generate embeddings using API for batch processing
     * @param texts List of texts to embed
     * @param provider API provider ("openai" or "openrouter")
     * @param model Model identifier
     * @return Result containing list of embedding vectors
     */
    suspend fun generateApiEmbeddings(
        texts: List<String>,
        provider: String,
        model: String
    ): Result<List<FloatArray>>
    
    /**
     * Calculate cosine similarity between two embedding vectors
     * @return Similarity score from -1.0 to 1.0 (higher = more similar)
     */
    fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float
}
