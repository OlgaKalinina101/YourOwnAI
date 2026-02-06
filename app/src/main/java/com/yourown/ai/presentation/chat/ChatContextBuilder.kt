package com.yourown.ai.presentation.chat

import android.util.Log
import com.yourown.ai.data.local.entity.DocumentChunkEntity
import com.yourown.ai.data.repository.DocumentEmbeddingRepository
import com.yourown.ai.data.repository.MemoryRepository
import com.yourown.ai.data.repository.PersonaRepository
import com.yourown.ai.domain.model.*
import com.yourown.ai.domain.service.AIService
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * Enhanced context result with breakdown
 */
data class EnhancedContextResult(
    val fullContext: String,
    val deepEmpathyAnalysis: String? = null,
    val memoriesUsed: List<String> = emptyList(),
    val ragChunksUsed: List<String> = emptyList()
)

/**
 * Builds enhanced context with Deep Empathy, Memory, and RAG
 */
class ChatContextBuilder @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val documentEmbeddingRepository: DocumentEmbeddingRepository,
    private val aiService: AIService,
    private val personaRepository: PersonaRepository
) {
    
    /**
     * Build enhanced context with base context, memories, and RAG chunks
     *
     * Format:
     * [Base Context]
     *
     * Твои воспоминания:
     * - Memory 1
     * - Memory 2
     *
     * Твоя база знаний:
     * - Chunk 1
     * - Chunk 2
     */
    suspend fun buildEnhancedContext(
        baseContext: String,
        userMessage: String,
        config: AIConfig,
        selectedModel: ModelProvider,
        conversationId: String,
        swipeMessage: Message? = null,
        personaId: String? = null
    ): EnhancedContextResult {
        val parts = mutableListOf<String>()
        
        // Deep Empathy, Memory and RAG work ONLY with API models
        val isApiModel = selectedModel is ModelProvider.API
        
        // Получаем настройки Persona если она активна
        val persona = personaId?.let { personaRepository.getPersonaById(it) }
        
        android.util.Log.d("ChatContextBuilder", "Building context: personaId=$personaId, persona loaded=${persona?.name}, useOnlyPersonaMemories=${persona?.useOnlyPersonaMemories}")
        
        // Определяем, включены ли Memory и RAG (с учетом настроек Persona)
        val isMemoryEnabled = persona?.memoryEnabled ?: config.memoryEnabled
        val isRagEnabled = persona?.ragEnabled ?: config.ragEnabled
        
        // Analyze Deep Empathy focus points if enabled (ONLY for API models)
        val deepEmpathyFocusPrompt = if (config.deepEmpathy && isApiModel) {
            analyzeDeepEmpathyFocus(
                userMessage = userMessage,
                selectedModel = selectedModel,
                config = config,
                conversationId = conversationId
            )
        } else {
            null
        }
        
        // Deep Empathy Focus (if present, add at the very beginning)
        if (!deepEmpathyFocusPrompt.isNullOrBlank()) {
            parts.add(deepEmpathyFocusPrompt.trim())
        }
        
        // Swipe Message (reply) - add right after Deep Empathy
        if (swipeMessage != null && config.swipeMessagePrompt.isNotBlank()) {
            val swipePrompt = config.swipeMessagePrompt.replace("{swipe_message}", swipeMessage.content)
            parts.add(swipePrompt.trim())
        }
        
        // Get relevant memories if Memory is enabled (ONLY for API models)
        val relevantMemories = if (isMemoryEnabled && isApiModel) {
            val memoryLimit = persona?.memoryLimit ?: config.memoryLimit
            val memoryMinAgeDays = persona?.memoryMinAgeDays ?: config.memoryMinAgeDays
            
            android.util.Log.d("ChatContextBuilder", "Building memory context: personaId=$personaId, persona=${persona?.name}, useOnlyPersonaMemories=${persona?.useOnlyPersonaMemories}")
            
            if (personaId != null && persona != null) {
                // Если активна Persona - загружаем память с учетом её настроек
                if (persona.useOnlyPersonaMemories) {
                    // Изолированные воспоминания - только память персоны
                    memoryRepository.findSimilarMemoriesByPersona(
                        query = userMessage,
                        personaId = personaId,
                        limit = memoryLimit,
                        minAgeDays = memoryMinAgeDays
                    )
                } else {
                    // Загружаем память персоны + глобальную
                    val personaMemories = memoryRepository.findSimilarMemoriesByPersona(
                        query = userMessage,
                        personaId = personaId,
                        limit = memoryLimit,
                        minAgeDays = memoryMinAgeDays
                    )
                    android.util.Log.d("ChatContextBuilder", "Found ${personaMemories.size} persona memories for personaId=$personaId")
                    
                    val globalMemories = memoryRepository.findSimilarGlobalMemories(
                        query = userMessage,
                        limit = memoryLimit,
                        minAgeDays = memoryMinAgeDays
                    )
                    android.util.Log.d("ChatContextBuilder", "Found ${globalMemories.size} global memories")
                    
                    val combined = (personaMemories + globalMemories)
                        .distinctBy { it.id }
                        .take(memoryLimit)
                    android.util.Log.d("ChatContextBuilder", "Combined to ${combined.size} total memories")
                    combined
                }
            } else {
                // Нет Persona - загружаем только глобальную память
                memoryRepository.findSimilarGlobalMemories(
                    query = userMessage,
                    limit = memoryLimit,
                    minAgeDays = memoryMinAgeDays
                )
            }
        } else {
            emptyList()
        }
        
        // Get relevant RAG chunks if RAG is enabled (ONLY for API models)
        val relevantChunks = if (isRagEnabled && isApiModel) {
            val ragChunkLimit = persona?.ragChunkLimit ?: config.ragChunkLimit
            
            if (personaId != null) {
                // Если активна Persona - загружаем документы Persona + глобальные
                val personaChunks = documentEmbeddingRepository.searchSimilarChunksByPersona(
                    query = userMessage,
                    personaId = personaId,
                    topK = ragChunkLimit
                ).map { it.first }
                
                val globalChunks = documentEmbeddingRepository.searchSimilarGlobalChunks(
                    query = userMessage,
                    topK = ragChunkLimit
                ).map { it.first }
                
                (personaChunks + globalChunks)
                    .distinctBy { it.id }
                    .take(ragChunkLimit)
            } else {
                // Нет Persona - загружаем только глобальные документы
                documentEmbeddingRepository.searchSimilarGlobalChunks(
                    query = userMessage,
                    topK = ragChunkLimit
                ).map { it.first }
            }
        } else {
            emptyList()
        }
        
        // Получаем настройки для форматирования из Persona или config
        val contextInstructions = persona?.contextInstructions ?: config.contextInstructions
        val memoryTitle = persona?.memoryTitle ?: config.memoryTitle
        val ragTitle = persona?.ragTitle ?: config.ragTitle
        val memoryInstructions = persona?.memoryInstructions ?: config.memoryInstructions
        val ragInstructions = persona?.ragInstructions ?: config.ragInstructions
        
        // Add context instructions ONLY if Memory OR RAG are enabled and have content
        if ((relevantMemories.isNotEmpty() || relevantChunks.isNotEmpty()) 
            && contextInstructions.isNotBlank()) {
            parts.add(contextInstructions.trim())
        }
        
        // Base context (персона, настройки и т.п.)
        if (baseContext.isNotBlank()) {
            parts.add(baseContext.trim())
        }
        
        // Memories (grouped by time)
        if (relevantMemories.isNotEmpty()) {
            val memoriesText = buildMemoriesText(relevantMemories, memoryTitle, memoryInstructions)
            parts.add(memoriesText)
        }
        
        // RAG chunks
        if (relevantChunks.isNotEmpty()) {
            val ragText = buildRAGText(relevantChunks, ragTitle, ragInstructions)
            parts.add(ragText)
        }
        
        val fullContext = parts.joinToString("\n\n")
        
        // Return result with breakdown
        return EnhancedContextResult(
            fullContext = fullContext,
            deepEmpathyAnalysis = deepEmpathyFocusPrompt,
            memoriesUsed = relevantMemories.map { it.fact },
            ragChunksUsed = relevantChunks.map { it.content }
        )
    }
    
    /**
     * Analyze Deep Empathy focus points using AI
     */
    private suspend fun analyzeDeepEmpathyFocus(
        userMessage: String,
        selectedModel: ModelProvider,
        config: AIConfig,
        conversationId: String
    ): String? {
        return try {
            // Replace {text} placeholder with actual message
            val analysisPrompt = config.deepEmpathyAnalysisPrompt.replace("{text}", userMessage)
            
            if (selectedModel !is ModelProvider.API) {
                Log.w("ChatContextBuilder", "Deep Empathy only works with API models")
                return null
            }
            
            // Deep Empathy analysis only for API models
            val analysisResponseBuilder = StringBuilder()
            aiService.generateResponse(
                provider = selectedModel,
                messages = listOf(
                    Message(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        role = MessageRole.USER,
                        content = analysisPrompt,
                        createdAt = System.currentTimeMillis()
                    )
                ),
                systemPrompt = "Ты - аналитик смысла.",
                userContext = null,
                config = config.copy(temperature = 0.3f) // Lower temperature for structured output
            ).collect { chunk ->
                analysisResponseBuilder.append(chunk)
            }
            
            val analysisResponse = analysisResponseBuilder.toString()
            
            // Parse JSON response
            val focusPoints = parseDeepEmpathyFocus(analysisResponse)
            
            // If focus points found, format them and insert into deepEmpathyPrompt
            if (focusPoints.isNotEmpty()) {
                val focusText = focusPoints.joinToString(", ")
                config.deepEmpathyPrompt.replace("{dialogue_focus}", focusText)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ChatContextBuilder", "Deep Empathy analysis failed", e)
            null
        }
    }
    
    /**
     * Parse Deep Empathy focus points from JSON response
     * Returns only focus points where is_strong_focus = true
     * Expected format: {"focus_points": ["...", "..."], "is_strong_focus": [true, false]}
     */
    private fun parseDeepEmpathyFocus(jsonResponse: String): List<String> {
        return try {
            // Extract JSON from response (handle cases where model adds text before/after)
            val jsonStart = jsonResponse.indexOf("{")
            val jsonEnd = jsonResponse.lastIndexOf("}") + 1
            
            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                Log.w("ChatContextBuilder", "No JSON found in Deep Empathy response")
                return emptyList()
            }
            
            val jsonString = jsonResponse.substring(jsonStart, jsonEnd)
            
            // Parse focus_points array
            val focusPointsMatch = Regex(""""focus_points"\s*:\s*\[(.*?)]""").find(jsonString)
            
            if (focusPointsMatch == null) {
                Log.w("ChatContextBuilder", "No focus_points found in JSON")
                return emptyList()
            }
            
            val focusPointsStr = focusPointsMatch.groupValues[1]
            val points = Regex(""""([^"]+)"""").findAll(focusPointsStr)
                .map { it.groupValues[1] }
                .toList()
            
            // Parse is_strong_focus array
            val isStrongFocusMatch = Regex(""""is_strong_focus"\s*:\s*\[(.*?)]""").find(jsonString)
            
            if (isStrongFocusMatch == null) {
                Log.w("ChatContextBuilder", "No is_strong_focus found in JSON")
                return emptyList()
            }
            
            val isStrongFocusStr = isStrongFocusMatch.groupValues[1]
            val strongFlags = isStrongFocusStr.split(",")
                .map { it.trim().lowercase() == "true" }
            
            // Filter: return only points where is_strong_focus = true
            val strongFocusPoints = points.filterIndexed { index, _ ->
                index < strongFlags.size && strongFlags[index]
            }
            
            if (strongFocusPoints.isEmpty()) {
                Log.d("ChatContextBuilder", "No strong focus points found")
            } else {
                Log.d("ChatContextBuilder", "Deep Empathy strong focus points: $strongFocusPoints")
            }
            
            return strongFocusPoints
        } catch (e: Exception) {
            Log.e("ChatContextBuilder", "Error parsing Deep Empathy JSON", e)
            return emptyList()
        }
    }
    
    /**
     * Build memories text with time grouping
     */
    private fun buildMemoriesText(
        memories: List<com.yourown.ai.domain.model.MemoryEntry>,
        memoryTitle: String,
        memoryInstructions: String
    ): String {
        return buildString {
            // Add title only if not blank
            if (memoryTitle.isNotBlank()) {
                appendLine("${memoryTitle}:")
            }
            // Add instructions only if not blank
            if (memoryInstructions.isNotBlank()) {
                appendLine(memoryInstructions)
                appendLine()
            }
            
            // Group memories by time and add timestamps
            val groupedMemories = groupMemoriesByTime(memories)
            groupedMemories.forEach { (timeLabel, memoryList) ->
                appendLine("$timeLabel:")
                memoryList.forEach { memory ->
                    appendLine("  • ${memory.fact}")
                }
                appendLine()
            }
        }.trim()
    }
    
    /**
     * Group memories by time periods with human-readable labels
     * 
     * Time periods:
     * - Days: "1 день назад", "2 дня назад", ..., "7 дней назад" (1-7 days)
     * - Weeks: "1 неделю назад", "2 недели назад", "3 недели назад" (7-21 days)
     * - Months: "1 месяц назад", "2 месяца назад", ..., "5 месяцев назад" (21-150 days)
     * - "Полгода назад" (150-180 days)
     * - "Давно" (> 180 days / ~6 months)
     */
    private fun groupMemoriesByTime(
        memories: List<com.yourown.ai.domain.model.MemoryEntry>
    ): Map<String, List<com.yourown.ai.domain.model.MemoryEntry>> {
        val currentTime = System.currentTimeMillis()
        val groups = mutableMapOf<String, MutableList<com.yourown.ai.domain.model.MemoryEntry>>()
        
        memories.forEach { memory ->
            val ageMillis = currentTime - memory.createdAt
            val ageDays = ageMillis / (24 * 60 * 60 * 1000)
            
            val timeLabel = when {
                // Days: 1-7 дней
                ageDays < 1 -> "1 день назад"
                ageDays < 2 -> "2 дня назад"
                ageDays < 3 -> "3 дня назад"
                ageDays < 4 -> "4 дня назад"
                ageDays < 5 -> "5 дней назад"
                ageDays < 6 -> "6 дней назад"
                ageDays < 7 -> "7 дней назад"
                
                // Weeks: 1-3 недели (7-21 days)
                ageDays < 14 -> "1 неделю назад"
                ageDays < 21 -> "2 недели назад"
                ageDays < 28 -> "3 недели назад"
                
                // Months: 1-5 месяцев (approximating 1 month = 30 days)
                ageDays < 60 -> "1 месяц назад"    // ~30-60 days
                ageDays < 90 -> "2 месяца назад"   // ~60-90 days
                ageDays < 120 -> "3 месяца назад"  // ~90-120 days
                ageDays < 150 -> "4 месяца назад"  // ~120-150 days
                ageDays < 180 -> "5 месяцев назад" // ~150-180 days
                
                // Half a year and beyond
                ageDays < 365 -> "Полгода назад"   // ~180-365 days
                else -> "Давно"                     // > 365 days (1 year+)
            }
            
            groups.getOrPut(timeLabel) { mutableListOf() }.add(memory)
        }
        
        // Sort by time (newest to oldest)
        val timeOrder = listOf(
            // Days
            "1 день назад",
            "2 дня назад",
            "3 дня назад",
            "4 дня назад",
            "5 дней назад",
            "6 дней назад",
            "7 дней назад",
            // Weeks
            "1 неделю назад",
            "2 недели назад",
            "3 недели назад",
            // Months
            "1 месяц назад",
            "2 месяца назад",
            "3 месяца назад",
            "4 месяца назад",
            "5 месяцев назад",
            // Beyond
            "Полгода назад",
            "Давно"
        )
        
        return groups.toSortedMap(compareBy { timeOrder.indexOf(it) })
    }
    
    /**
     * Build RAG text
     */
    private fun buildRAGText(
        ragChunks: List<DocumentChunkEntity>,
        ragTitle: String,
        ragInstructions: String
    ): String {
        return buildString {
            // Add title only if not blank
            if (ragTitle.isNotBlank()) {
                appendLine("${ragTitle}:")
            }
            // Add instructions only if not blank
            if (ragInstructions.isNotBlank()) {
                appendLine(ragInstructions)
                appendLine()
            }
            ragChunks.forEachIndexed { index, chunk ->
                appendLine("${index + 1}. ${chunk.content}")
            }
        }.trim()
    }
}
