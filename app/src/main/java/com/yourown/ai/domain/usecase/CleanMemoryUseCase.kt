package com.yourown.ai.domain.usecase

import com.yourown.ai.domain.model.*
import com.yourown.ai.domain.service.AIService
import com.yourown.ai.data.repository.MemoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Use case for cleaning memory based on biography
 * Removes outdated memories and merges duplicates
 */
@Singleton
class CleanMemoryUseCase @Inject constructor(
    private val aiService: AIService,
    private val memoryRepository: MemoryRepository,
    private val apiKeyRepository: com.yourown.ai.data.repository.ApiKeyRepository
) {
    
    private val _cleaningStatus = MutableStateFlow<MemoryCleaningStatus>(MemoryCleaningStatus.Idle)
    val cleaningStatus: StateFlow<MemoryCleaningStatus> = _cleaningStatus.asStateFlow()
    
    /**
     * Clean memories based on biography and clusters
     */
    suspend fun cleanMemories(
        biography: UserBiography,
        clusters: List<MemoryCluster>,
        selectedModel: ModelProvider
    ): Result<Unit> {
        return try {
            _cleaningStatus.value = MemoryCleaningStatus.Processing(0, clusters.size, "Starting...")
            
            // Get API key for model
            val provider = when (selectedModel) {
                is ModelProvider.API -> selectedModel.provider
                is ModelProvider.Local -> {
                    return Result.failure(Exception("Local models not supported for memory cleaning"))
                }
            }
            
            val apiKey = apiKeyRepository.getApiKey(provider)
                ?: return Result.failure(Exception("No API key found for ${provider.name}"))
            
            val currentDate = getCurrentDateFormatted()
            var totalRemoved = 0
            var totalMerged = 0
            var totalMemories = 0
            
            // Process each cluster
            clusters.forEachIndexed { index, cluster ->
                coroutineContext.ensureActive()
                
                _cleaningStatus.value = MemoryCleaningStatus.Processing(
                    currentCluster = index + 1,
                    totalClusters = clusters.size,
                    step = "Processing cluster ${index + 1}/${clusters.size}"
                )
                
                totalMemories += cluster.memories.size
                
                // Generate prompt for cleaning this cluster
                val prompt = buildCleaningPrompt(
                    biography = biography,
                    cluster = cluster,
                    currentDate = currentDate
                )
                
                // Call AI
                val response = callAI(prompt, selectedModel, apiKey)
                
                // Parse response and apply changes (atomic transaction)
                val result = parseAndApplyChanges(cluster.memories, response)
                totalRemoved += result.first
                totalMerged += result.second
                
                // Wait for Room's InvalidationTracker to settle before next cluster
                delay(1000)
            }
            
            _cleaningStatus.value = MemoryCleaningStatus.Completed(
                totalMemories = totalMemories,
                removedMemories = totalRemoved,
                mergedMemories = totalMerged
            )
            Result.success(Unit)
            
        } catch (e: CancellationException) {
            android.util.Log.i("MemoryCleaning", "Memory cleaning cancelled by user")
            _cleaningStatus.value = MemoryCleaningStatus.Idle
            throw e
        } catch (e: Exception) {
            android.util.Log.e("MemoryCleaning", "Error cleaning memories", e)
            _cleaningStatus.value = MemoryCleaningStatus.Failed(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    /**
     * Build prompt for cleaning memories
     */
    private fun buildCleaningPrompt(
        biography: UserBiography,
        cluster: MemoryCluster,
        currentDate: String
    ): String {
        val memoriesText = cluster.memories.joinToString("\n\n") { memWithAge ->
            val ageText = when {
                memWithAge.ageDays <= 7 -> "${memWithAge.ageDays} дней назад"
                memWithAge.ageDays <= 30 -> "${memWithAge.ageDays / 7} недель назад"
                memWithAge.ageDays <= 180 -> "${memWithAge.ageDays / 30} месяцев назад"
                else -> "Больше полугода назад"
            }
            "ID: ${memWithAge.memory.id}\n$ageText: ${memWithAge.memory.fact}"
        }
        
        return """
Ты — помощник цифрового партнёра, который наводит порядок в памяти на основе биографии.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📅 **Сегодня:** $currentDate

👤 **Биография пользователя:**
${biography.toFormattedText()}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📦 **Воспоминания для анализа** (кластер ${cluster.id + 1}, всего ${cluster.memories.size} записей):

$memoriesText

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎯 **ЦЕЛЬ:** Сократить память в 2-3 раза, оставив только значимое.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 **ЗАДАЧА 1: Удалить устаревшие**

✅ **Удаляй:**
   • Ситуативные мелочи ("сегодня устал", "вчера хорошо поспал")
   • Временные планы, которые уже реализованы или отменены
   • Решённые технические проблемы
   • Противоречия биографии ("не знаю Python" ➔ биография: "Senior Python dev")
   • Общие дубли (повторяется без новых деталей)

❌ **НЕ удаляй (даже если старое!):**
   • Формирующие события: первая встреча, начало проекта, важное решение
   • Травмы и кризисы: объясняют страхи/паттерны из биографии
   • Поворотные моменты: смена работы, переезд, прорыв
   • Уникальную конкретику: детали, которых НЕТ в биографии

   💡 Пример: Биография "работает с AI" ≠ дубль воспоминания "починил баг в DeepSeek API"
      (первое — общее, второе — конкретный факт)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 **ЗАДАЧА 2: Объединить похожие**

✅ **Объединяй:**
   • Несколько воспоминаний об одном событии/проблеме
   • Серию мелких достижений в одной области  
   • Похожие эмоции/мысли в один период времени

📐 **Правила объединения:**
   • keepId = ID с самой СВЕЖЕЙ датой
   • newFact = сжатая суть ВСЕХ воспоминаний (не копия одного!)
   • Агрегируй: "3 раза работал с bug X" ➔ "Боролся с bug X, нашёл решение"
   • Сохраняй ключевые детали, не делай простой append

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

⚖️ **ВАЖНЫЙ БАЛАНС:**
   • Старое ≠ плохое (формирующие события важны)
   • Новое ≠ важное (может быть ситуативная мелочь)
   • Стратегия: **объединяй агрессивно, удаляй осторожно**

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📤 **ФОРМАТ ОТВЕТА** (строго JSON):

```json
{
  "remove": ["id1", "id2", "id3"],
  "merge": [
    {
      "ids": ["id4", "id5", "id6"],
      "keepId": "id4",
      "newFact": "Объединённое воспоминание (краткое, информативное)"
    }
  ],
  "reasoning": "Краткое объяснение: что удалил и почему, что объединил"
}
```

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✓ **ЧЕКЛИСТ ПЕРЕД ОТПРАВКОЙ:**
   □ Не удалил формирующие события?
   □ Объединённые факты содержат суть ВСЕХ исходных?
   □ Сохранил уникальные детали, которых нет в биографии?

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
""".trimIndent()
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
                conversationId = "memory_cleaning",
                role = MessageRole.USER,
                content = prompt,
                createdAt = System.currentTimeMillis()
            )
        )
        
        val config = AIConfig(
            temperature = 0.3f,
            topP = 0.8f,
            maxTokens = 2000
        )
        
        val responseBuilder = StringBuilder()
        aiService.generateResponse(
            provider = model,
            messages = messages,
            systemPrompt = "",
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
     * Parse AI response and apply memory changes
     * Returns: Pair(removedCount, mergedCount)
     */
    private suspend fun parseAndApplyChanges(
        memories: List<MemoryWithAge>,
        response: String
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        try {
            // Extract JSON from response
            val jsonMatch = Regex("```json\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
                .find(response)?.groupValues?.get(1)
                ?: response
            
            val json = JSONObject(jsonMatch)
            
            // Parse remove list
            val removeIds = mutableListOf<String>()
            if (json.has("remove")) {
                val removeArray = json.getJSONArray("remove")
                for (i in 0 until removeArray.length()) {
                    removeIds.add(removeArray.getString(i))
                }
            }
            
            // Parse merge groups
            val mergeGroups = mutableListOf<Triple<List<String>, String, String>>()
            if (json.has("merge")) {
                val mergeArray = json.getJSONArray("merge")
                for (i in 0 until mergeArray.length()) {
                    val mergeObj = mergeArray.getJSONObject(i)
                    val ids = mutableListOf<String>()
                    val idsArray = mergeObj.getJSONArray("ids")
                    for (j in 0 until idsArray.length()) {
                        ids.add(idsArray.getString(j))
                    }
                    val keepId = mergeObj.getString("keepId")
                    val newFact = mergeObj.getString("newFact")
                    mergeGroups.add(Triple(ids, keepId, newFact))
                }
            }
            
            android.util.Log.d("MemoryCleaning", "Remove: ${removeIds.size}, Merge groups: ${mergeGroups.size}")
            android.util.Log.d("MemoryCleaning", "Reasoning: ${json.optString("reasoning", "")}")
            
            // Prepare updates list: Pair(MemoryEntry, newFact)
            val updates = mutableListOf<Pair<MemoryEntry, String>>()
            var mergedCount = 0
            mergeGroups.forEach { (ids, keepId, newFact) ->
                val keptMemory = memories.find { it.memory.id == keepId }?.memory
                if (keptMemory != null) {
                    updates.add(Pair(keptMemory, newFact))
                }
                mergedCount += ids.size - 1
            }
            
            // Collect ALL IDs to delete
            val allIdsToDelete = mutableSetOf<String>()
            allIdsToDelete.addAll(removeIds)
            mergeGroups.forEach { (ids, keepId, _) ->
                allIdsToDelete.addAll(ids.filter { it != keepId })
            }
            
            // Execute ALL changes in a SINGLE Room transaction
            // This triggers InvalidationTracker only ONCE after commit,
            // preventing CursorWindow crashes from concurrent reads
            memoryRepository.batchUpdateAndDelete(updates, allIdsToDelete.toList())
            
            Pair(removeIds.size, mergedCount)
            
        } catch (e: Exception) {
            android.util.Log.e("MemoryCleaning", "Error parsing AI response", e)
            Pair(0, 0)
        }
    }
    
    /**
     * Reset status to Idle
     */
    fun resetStatus() {
        _cleaningStatus.value = MemoryCleaningStatus.Idle
    }
}
