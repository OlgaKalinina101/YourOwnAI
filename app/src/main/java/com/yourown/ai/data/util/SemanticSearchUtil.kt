package com.yourown.ai.data.util

import kotlin.math.min
import kotlin.math.sqrt

/**
 * Semantic search utility with embedding similarity + keyword boost + exact match
 * 
 * Algorithm components:
 * 1. Multi-Query - split query into sentences, use best match
 * 2. K-NN Vector Search - cosine similarity on embeddings
 * 3. Stop-words filtering - remove common Russian words
 * 4. Keyword Boost - +0.10 per matching token (max +0.25)
 * 5. Exact Match Boost - +0.15 for identical normalized strings
 */
object SemanticSearchUtil {
    
    /**
     * Multilingual stop-words (Russian, Ukrainian, English)
     * Common words without significant meaning
     */
    private val STOP_WORDS = setOf(
        // Russian
        "а", "и", "в", "на", "с", "у", "к", "о", "из", "за", "по", "от", "до",
        "что", "как", "это", "так", "ты", "я", "мы", "он", "она", "они", "вы",
        "не", "да", "но", "же", "ли", "бы", "то", "ещё", "еще", "уже", "вот",
        "все", "всё", "мне", "меня", "тебе", "тебя", "нам", "нас", "мой", "твой",
        "если", "когда", "чтобы", "потому", "очень", "только", "просто", "прям",
        "какие", "какой", "какая", "какое", "который", "которая", "которое",
        "хочешь", "хочу", "могу", "можешь", "буду", "будет", "есть", "был", "была",
        "опять", "снова", "теперь", "сейчас", "тоже", "также", "быть", "этот",
        "эти", "этим", "этих", "того", "тому", "том", "без", "для", "про", "при",
        
        // Ukrainian
        "а", "і", "в", "на", "з", "у", "до", "від", "за", "по", "для", "про", "при",
        "що", "як", "це", "так", "ти", "я", "ми", "він", "вона", "вони", "ви",
        "не", "та", "але", "ж", "чи", "би", "те", "ще", "вже", "ось",
        "все", "всі", "мені", "мене", "тобі", "тебе", "нам", "нас", "мій", "твій",
        "якщо", "коли", "щоб", "тому", "дуже", "тільки", "просто",
        "які", "який", "яка", "яке", "котрий", "котра", "котре",
        "хочеш", "хочу", "можу", "можеш", "буду", "буде", "є", "був", "була",
        "знову", "тепер", "зараз", "теж", "також", "бути", "цей", "ці", "цим", "цих",
        "того", "тому", "тим", "без", "або",
        
        // English
        "the", "is", "at", "which", "on", "a", "an", "and", "or", "but",
        "in", "with", "to", "for", "of", "as", "by", "that", "this", "these",
        "it", "be", "are", "was", "were", "been", "have", "has", "had",
        "do", "does", "did", "will", "would", "could", "should", "may", "might",
        "i", "you", "he", "she", "we", "they", "me", "him", "her", "us", "them",
        "my", "your", "his", "her", "its", "our", "their",
        "not", "no", "yes", "can", "from", "up", "down", "out", "about",
        "into", "through", "over", "under", "again", "then", "once",
        "here", "there", "when", "where", "why", "how", "all", "each",
        "some", "such", "only", "own", "same", "so", "than", "too", "very",
        "just", "now", "also", "more", "most", "much", "any", "both"
    )
    
    /**
     * Search result with similarity score
     */
    data class SearchResult<T>(
        val item: T,
        val score: Float,
        val embeddingSimilarity: Float,
        val keywordBoost: Float,
        val exactMatchBoost: Float
    )
    
    /**
     * Find top K similar items using semantic search with Multi-Query
     * 
     * @param query User query text
     * @param queryEmbedding Embedding of the query (full message)
     * @param items All items to search through
     * @param getText Function to extract searchable text from item
     * @param getEmbedding Function to get embedding for item
     * @param k Maximum number of results to return
     * @return List of top K results sorted by score (descending)
     */
    fun <T> findSimilar(
        query: String,
        queryEmbedding: FloatArray,
        items: List<T>,
        getText: (T) -> String,
        getEmbedding: (T) -> FloatArray?,
        k: Int = 5
    ): List<SearchResult<T>> {
        if (items.isEmpty() || queryEmbedding.isEmpty()) {
            return emptyList()
        }
        
        // Multi-Query: split query into sentences
        val sentences = splitToSentences(query)
        val normalizedQuery = normalizeText(query)
        val queryTokens = tokenize(normalizedQuery)
        
        android.util.Log.d("SemanticSearch", "Multi-Query: ${sentences.size} sentences, ${queryTokens.size} keywords: ${queryTokens.take(5)}")
        
        // Calculate scores for all items
        val results = items.mapNotNull { item ->
            val itemText = getText(item)
            val itemEmbedding = getEmbedding(item) ?: return@mapNotNull null
            
            if (itemEmbedding.isEmpty()) return@mapNotNull null
            
            // 1. Embedding similarity (cosine) - use full query embedding
            val embeddingSimilarity = cosineSimilarity(queryEmbedding, itemEmbedding)
            
            // 2. Keyword boost - using filtered tokens (no stop-words)
            val normalizedItem = normalizeText(itemText)
            val itemTokens = tokenize(normalizedItem)
            val keywordBoost = calculateKeywordBoost(queryTokens, itemTokens)
            
            // 3. Exact match boost
            val exactMatchBoost = calculateExactMatchBoost(normalizedQuery, normalizedItem, queryTokens, itemTokens)
            
            // Final score (clamped to [0, 1])
            val finalScore = min(1.0f, embeddingSimilarity + keywordBoost + exactMatchBoost)
            
            SearchResult(
                item = item,
                score = finalScore,
                embeddingSimilarity = embeddingSimilarity,
                keywordBoost = keywordBoost,
                exactMatchBoost = exactMatchBoost
            )
        }
        
        // Sort by score (descending) and take top K
        val topResults = results
            .sortedByDescending { it.score }
            .take(k)
        
        android.util.Log.d("SemanticSearch", "Found ${topResults.size} results (avg score: ${topResults.map { it.score }.average()})")
        
        return topResults
    }
    
    /**
     * Calculate cosine similarity between two embeddings
     * Result is in range [0, 1] where 1 is most similar
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        if (denominator == 0f) return 0f
        
        // Convert from [-1, 1] to [0, 1]
        val similarity = dotProduct / denominator
        return (similarity + 1f) / 2f
    }
    
    /**
     * Split message into meaningful sentences for multi-query search
     * Splits by periods, exclamation marks, question marks
     * Filters out short fragments (less than 25 characters)
     */
    private fun splitToSentences(message: String): List<String> {
        return splitTextToSentences(message, minLength = 25)
    }
    
    /**
     * Split text into sentences (public utility method)
     * @param text Text to split
     * @param minLength Minimum sentence length (0 = no filter)
     * @return List of sentences
     */
    fun splitTextToSentences(text: String, minLength: Int = 0): List<String> {
        val sentences = text.split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length >= minLength }
        
        return if (sentences.isEmpty() && text.isNotBlank()) {
            listOf(text) // Fallback to full text if no sentences found
        } else {
            sentences
        }
    }
    
    /**
     * Normalize text for comparison
     * - Lowercase
     * - Trim whitespace
     * - Remove extra spaces
     * - Remove punctuation and emojis
     */
    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ") // Keep only letters, numbers, spaces
            .trim()
            .replace(Regex("\\s+"), " ")
    }
    
    /**
     * Tokenize text into meaningful words
     * - Split by whitespace and punctuation
     * - Filter stop-words
     * - Filter short tokens (< 3 characters)
     */
    private fun tokenize(text: String): Set<String> {
        return text
            .split(Regex("[\\s,;.!?()\\[\\]{}\"']+"))
            .filter { token ->
                token.isNotBlank() && 
                token.length > 3 && // Longer than 3 characters
                token.lowercase() !in STOP_WORDS // Not a stop-word
            }
            .map { it.lowercase() }
            .toSet()
    }
    
    /**
     * Calculate keyword boost based on token overlap
     * +0.10 per matching token (max +0.25)
     */
    private fun calculateKeywordBoost(queryTokens: Set<String>, itemTokens: Set<String>): Float {
        val matchingTokens = queryTokens.intersect(itemTokens).size
        val boost = matchingTokens * 0.10f
        return min(0.25f, boost)
    }
    
    /**
     * Calculate exact match boost
     * +0.15 if normalized strings are identical
     * +0.10 if all query tokens are in item
     */
    private fun calculateExactMatchBoost(
        normalizedQuery: String,
        normalizedItem: String,
        queryTokens: Set<String>,
        itemTokens: Set<String>
    ): Float {
        var boost = 0f
        
        // Exact string match
        if (normalizedQuery == normalizedItem) {
            boost += 0.15f
        }
        
        // All query tokens present in item
        if (queryTokens.isNotEmpty() && itemTokens.containsAll(queryTokens)) {
            boost += 0.10f
        }
        
        return boost
    }
}
