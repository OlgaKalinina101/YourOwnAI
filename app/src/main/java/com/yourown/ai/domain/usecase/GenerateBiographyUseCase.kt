package com.yourown.ai.domain.usecase

import com.yourown.ai.domain.model.*
import com.yourown.ai.domain.service.AIService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Use case for generating user biography from memory clusters
 */
@Singleton
class GenerateBiographyUseCase @Inject constructor(
    private val aiService: AIService,
    private val apiKeyRepository: com.yourown.ai.data.repository.ApiKeyRepository
) {
    
    private val _generationStatus = MutableStateFlow<BiographyGenerationStatus>(BiographyGenerationStatus.Idle)
    val generationStatus: StateFlow<BiographyGenerationStatus> = _generationStatus.asStateFlow()
    
    /**
     * Generate biography from memory clusters
     */
    suspend fun generateBiography(
        clusters: List<MemoryCluster>,
        selectedModel: ModelProvider,
        currentBiography: UserBiography? = null
    ): Result<UserBiography> {
        return try {
            _generationStatus.value = BiographyGenerationStatus.Processing(0, clusters.size, "Starting...")
            
            // Get API key for model
            val provider = when (selectedModel) {
                is ModelProvider.API -> selectedModel.provider
                is ModelProvider.Local -> {
                    // Local models not supported for biography generation yet
                    return Result.failure(Exception("Local models not supported for biography generation"))
                }
            }
            
            val apiKey = apiKeyRepository.getApiKey(provider)
                ?: return Result.failure(Exception("No API key found for ${provider.name}"))
            
            var biography = currentBiography ?: UserBiography()
            val currentDate = getCurrentDateFormatted()
            
            // Process each cluster
            clusters.forEachIndexed { index, cluster ->
                // Check if coroutine is cancelled (throws CancellationException if true)
                coroutineContext.ensureActive()
                
                _generationStatus.value = BiographyGenerationStatus.Processing(
                    currentCluster = index + 1,
                    totalClusters = clusters.size,
                    step = "Processing cluster ${index + 1}/${clusters.size}"
                )
                
                // Generate prompt for this cluster
                val prompt = buildBiographyPrompt(
                    cluster = cluster,
                    currentBiography = biography,
                    currentDate = currentDate
                )
                
                // Call AI
                val response = callAI(prompt, selectedModel, apiKey)
                
                // Parse response and update biography
                biography = parseAndUpdateBiography(biography, response)
                biography = biography.copy(processedClusters = index + 1)
            }
            
            _generationStatus.value = BiographyGenerationStatus.Completed(biography)
            Result.success(biography)
            
        } catch (e: CancellationException) {
            // Cancelled by user - reset status
            android.util.Log.i("BiographyGeneration", "Biography generation cancelled by user")
            _generationStatus.value = BiographyGenerationStatus.Idle
            throw e // Re-throw to properly cancel coroutine
        } catch (e: Exception) {
            android.util.Log.e("BiographyGeneration", "Error generating biography", e)
            _generationStatus.value = BiographyGenerationStatus.Failed(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    /**
     * Reset status to Idle
     */
    fun resetStatus() {
        _generationStatus.value = BiographyGenerationStatus.Idle
    }
    
    /**
     * Build prompt for biography generation
     */
    private fun buildBiographyPrompt(
        cluster: MemoryCluster,
        currentBiography: UserBiography,
        currentDate: String
    ): String {
        val memoriesText = formatMemoriesWithTime(cluster.memories, currentDate)
        
        return if (currentBiography.isEmpty()) {
            // First cluster - create initial biography
            """
Ты — помощник цифрового партнёра, который создает цифровой портрет пользователя.

**Сегодня:** $currentDate

**Задача:** На основе этих воспоминаний создай краткий структурированный портрет пользователя.
Это не хроника событий, а срез личности сейчас.

**Воспоминания (кластер ${cluster.id + 1}):**
$memoriesText

**Формат ответа (JSON):**
```json
{
  "values": "Что важно для пользователя: ценности, убеждения, взгляды, принципы",
  "profile": "Кто человек: профессия, отношения, ключевые роли",
  "painPoints": "Что беспокоит, проблемы, сложности",
  "joys": "Что радует, достижения, приятные моменты",
  "fears": "Страхи, опасения, тревоги",
  "loves": "Интересы, увлечения, что любит",
  "currentSituation": "Что происходит сейчас в жизни пользователя"
}
```

**Важно:** 
- Пиши сжато, без воды и повторений
- Используй только актуальные факты из воспоминаний
- Старые события (>60 дней) упоминай только если они сформировали личность
- Пиши в настоящем времени ("работает", а не "работала")
- БЕЗ временных маркеров ("усилилась", "добавилась", "укрепилась")
- Если нет информации по категории — пиши пустую строку ""
- TOTAL объём всех полей: ~1500 слов максимум

**Принцип:** Представь, что описываешь человека другу, который его не знает. 
Краткий портрет, а не биография.""
""".trimIndent()
        } else {
            // Update existing biography
            """
Ты — помощник цифрового партнёра, который обновляет цифровой портрет пользователя.

**Сегодня:** $currentDate

**Текущий портрет пользователя:**
${currentBiography.toFormattedText()}

**Новые воспоминания (кластер ${cluster.id + 1}):**
$memoriesText

**Задача:** Дополни или переосмысли портрет с учётом новых воспоминаний. 

**Формат ответа (JSON):**
```json
{
  "values": "Что важно для пользователя: ценности, убеждения, взгляды, принципы",
  "profile": "Кто человек: профессия, отношения, ключевые роли",
  "painPoints": "Что беспокоит, проблемы, сложности",
  "joys": "Что радует, достижения, приятные моменты",
  "fears": "Страхи, опасения, тревоги",
  "loves": "Интересы, увлечения, что любит",
  "currentSituation": "Что происходит сейчас в жизни пользователя"
}
```

**Важно - правила обновления:**

1. **Заменяй, а не добавляй:**
   - Если новое противоречит старому → оставь только новое
   - Если тема повторяется → обобщи в одно предложение
   - Если старое стало неактуальным → удали полностью

2. **Убирай временные маркеры:**
   - Не пиши: "Усилилась ценность...", "Добавилась...", "Укрепилась..."
   - Пиши: "Ценит...", "Важно..."
   
3. **Агрегируй похожее:**
   - Было: "Ценит стабильность. Ценит безопасность. Ценит поддержку."
   - Стало: "Ценит стабильность, безопасность и поддержку близких."

4. **Удаляй устаревшее:**
   - Если проблема решена → убери из painPoints
   - Если страх прошёл → убери из fears
   - Если ситуация изменилась → обнови currentSituation

5. **Фокус на главном:**
   - Топ-n проблемы, а не все мелкие неудобства
   - Ключевые ценности, а не всё подряд
   - Текущая ситуация, а не вся биография

6. **Без хронологии:**
   - Это портрет человека сейчас, а не история изменений
   - Не нужно описывать "путь" — только итог

**ЛИМИТЫ:**
- TOTAL объём всех полей: ~1500 слов максимум
- Если превышаешь — сокращай менее важное

**Принцип:** Ты не летописец, а портретист. Ты описываешь человека близкому другу, который о нем не знает. 
Твоя задача — показать, кто этот человек сейчас, а не что с ним происходило.
""".trimIndent()
        }
    }
    
    /**
     * Format memories with time labels (like in ChatContextBuilder)
     */
    private fun formatMemoriesWithTime(
        memories: List<MemoryWithAge>,
        currentDate: String
    ): String {
        val grouped = groupMemoriesByTime(memories)
        
        return grouped.entries
            .sortedBy { getTimePeriodOrder(it.key) }
            .joinToString("\n\n") { (timeLabel, mems) ->
                "**$timeLabel:**\n" + mems.joinToString("\n") { mem ->
                    "- ${mem.memory.fact}"
                }
            }
    }
    
    /**
     * Group memories by time periods (from ChatContextBuilder logic)
     */
    private fun groupMemoriesByTime(
        memories: List<MemoryWithAge>
    ): Map<String, List<MemoryWithAge>> {
        val groups = mutableMapOf<String, MutableList<MemoryWithAge>>()
        
        memories.forEach { memory ->
            val ageDays = memory.ageDays.toLong()
            
            val timeLabel = when {
                ageDays < 1 -> "1 день назад"
                ageDays < 2 -> "2 дня назад"
                ageDays < 3 -> "3 дня назад"
                ageDays < 4 -> "4 дня назад"
                ageDays < 5 -> "5 дней назад"
                ageDays < 6 -> "6 дней назад"
                ageDays < 7 -> "7 дней назад"
                ageDays < 14 -> "1 неделю назад"
                ageDays < 21 -> "2 недели назад"
                ageDays < 28 -> "3 недели назад"
                ageDays < 60 -> "1 месяц назад"
                ageDays < 90 -> "2 месяца назад"
                ageDays < 120 -> "3 месяца назад"
                ageDays < 150 -> "4 месяца назад"
                ageDays < 180 -> "5 месяцев назад"
                ageDays < 365 -> "Полгода назад"
                else -> "Давно"
            }
            
            groups.getOrPut(timeLabel) { mutableListOf() }.add(memory)
        }
        
        return groups
    }
    
    /**
     * Get order for time periods (for sorting)
     */
    private fun getTimePeriodOrder(label: String): Int = when (label) {
        "1 день назад" -> 1
        "2 дня назад" -> 2
        "3 дня назад" -> 3
        "4 дня назад" -> 4
        "5 дней назад" -> 5
        "6 дней назад" -> 6
        "7 дней назад" -> 7
        "1 неделю назад" -> 8
        "2 недели назад" -> 9
        "3 недели назад" -> 10
        "1 месяц назад" -> 11
        "2 месяца назад" -> 12
        "3 месяца назад" -> 13
        "4 месяца назад" -> 14
        "5 месяцев назад" -> 15
        "Полгода назад" -> 16
        "Давно" -> 17
        else -> 18
    }
    
    /**
     * Get current date formatted
     */
    private fun getCurrentDateFormatted(): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("ru"))
        return dateFormat.format(Date())
    }
    
    /**
     * Call AI model
     */
    private suspend fun callAI(
        prompt: String,
        model: ModelProvider,
        apiKey: String
    ): String {
        val messages = listOf(
            Message(
                id = "prompt",
                conversationId = "biography",
                role = MessageRole.USER,
                content = prompt,
                createdAt = System.currentTimeMillis()
            )
        )
        
        // Create AI config with lower temperature for more consistent output
        val config = AIConfig(
            temperature = 0.3f,
            topP = 0.8f,
            maxTokens = 2000
        )
        
        // Collect response from Flow
        val responseBuilder = StringBuilder()
        aiService.generateResponse(
            provider = model,
            messages = messages,
            systemPrompt = "", // Empty system prompt for biography generation
            userContext = null,
            config = config,
            webSearchEnabled = false,
            xSearchEnabled = false
        ).collect { chunk ->
            responseBuilder.append(chunk)
        }
        
        return responseBuilder.toString()
    }
    
    /**
     * Parse AI response and update biography
     */
    private fun parseAndUpdateBiography(
        currentBiography: UserBiography,
        response: String
    ): UserBiography {
        return try {
            // Try to extract JSON from response
            val jsonMatch = Regex("```json\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
                .find(response)?.groupValues?.get(1)
                ?: Regex("\\{.*?\\}", RegexOption.DOT_MATCHES_ALL).find(response)?.value
                ?: response
            
            // Simple JSON parsing (можно заменить на Gson/Moshi если нужно)
            val values = extractJsonField(jsonMatch, "values")
            val profile = extractJsonField(jsonMatch, "profile")
            val painPoints = extractJsonField(jsonMatch, "painPoints")
            val joys = extractJsonField(jsonMatch, "joys")
            val fears = extractJsonField(jsonMatch, "fears")
            val loves = extractJsonField(jsonMatch, "loves")
            val currentSituation = extractJsonField(jsonMatch, "currentSituation")
            
            UserBiography(
                values = values.ifBlank { currentBiography.values },
                profile = profile.ifBlank { currentBiography.profile },
                painPoints = painPoints.ifBlank { currentBiography.painPoints },
                joys = joys.ifBlank { currentBiography.joys },
                fears = fears.ifBlank { currentBiography.fears },
                loves = loves.ifBlank { currentBiography.loves },
                currentSituation = currentSituation.ifBlank { currentBiography.currentSituation },
                lastUpdated = System.currentTimeMillis(),
                processedClusters = currentBiography.processedClusters
            )
        } catch (e: Exception) {
            android.util.Log.e("BiographyParsing", "Error parsing response", e)
            currentBiography // Return unchanged on error
        }
    }
    
    /**
     * Extract field from JSON string
     */
    private fun extractJsonField(json: String, fieldName: String): String {
        val regex = Regex("\"$fieldName\"\\s*:\\s*\"(.*?)\"(?=,|\\})", RegexOption.DOT_MATCHES_ALL)
        return regex.find(json)?.groupValues?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?: ""
    }
    
}
