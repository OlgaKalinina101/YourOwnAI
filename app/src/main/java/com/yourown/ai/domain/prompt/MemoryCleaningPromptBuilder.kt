package com.yourown.ai.domain.prompt

import com.yourown.ai.domain.model.MemoryCluster
import com.yourown.ai.domain.model.UserBiography

/**
 * Builder for Memory Cleaning prompts
 */
object MemoryCleaningPromptBuilder {
    
    /**
     * Build memory cleaning prompt
     */
    fun buildCleaningPrompt(
        biography: UserBiography,
        cluster: MemoryCluster,
        currentDate: String,
        memoriesText: String,
        language: String = "ru"
    ): String {
        return when (language) {
            "en" -> buildCleaningPromptEN(biography, cluster, currentDate, memoriesText)
            "uk" -> buildCleaningPromptUK(biography, cluster, currentDate, memoriesText)
            else -> buildCleaningPromptRU(biography, cluster, currentDate, memoriesText)
        }
    }
    
    // ===== RUSSIAN =====
    
    private fun buildCleaningPromptRU(
        biography: UserBiography,
        cluster: MemoryCluster,
        currentDate: String,
        memoriesText: String
    ): String = """
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
    
    // ===== ENGLISH =====
    
    private fun buildCleaningPromptEN(
        biography: UserBiography,
        cluster: MemoryCluster,
        currentDate: String,
        memoriesText: String
    ): String = """
You are a digital partner's assistant who organizes memory based on biography.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📅 **Today:** $currentDate

👤 **User biography:**
${biography.toFormattedText()}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📦 **Memories to analyze** (cluster ${cluster.id + 1}, total ${cluster.memories.size} records):

$memoriesText

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎯 **GOAL:** Reduce memory by 2-3 times, keeping only significant items.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 **TASK 1: Remove outdated**

✅ **Remove:**
   • Situational trivia ("tired today", "slept well yesterday")
   • Temporary plans that are already implemented or cancelled
   • Solved technical problems
   • Contradictions with biography ("don't know Python" ➔ biography: "Senior Python dev")
   • General duplicates (repeats without new details)

❌ **DO NOT remove (even if old!):**
   • Formative events: first meeting, project start, important decision
   • Traumas and crises: explain fears/patterns from biography
   • Turning points: job change, relocation, breakthrough
   • Unique specifics: details that are NOT in biography

   💡 Example: Biography "works with AI" ≠ duplicate memory "fixed bug in DeepSeek API"
      (first is general, second is specific fact)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 **TASK 2: Merge similar**

✅ **Merge:**
   • Multiple memories about one event/problem
   • Series of small achievements in one area  
   • Similar emotions/thoughts in one time period

📐 **Merge rules:**
   • keepId = ID with the FRESHEST date
   • newFact = condensed essence of ALL memories (not a copy of one!)
   • Aggregate: "worked with bug X 3 times" ➔ "Struggled with bug X, found solution"
   • Preserve key details, don't just append

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

⚖️ **IMPORTANT BALANCE:**
   • Old ≠ bad (formative events are important)
   • New ≠ important (may be situational trivia)
   • Strategy: **merge aggressively, delete carefully**

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📤 **RESPONSE FORMAT** (strictly JSON):

```json
{
  "remove": ["id1", "id2", "id3"],
  "merge": [
    {
      "ids": ["id4", "id5", "id6"],
      "keepId": "id4",
      "newFact": "Merged memory (brief, informative)"
    }
  ],
  "reasoning": "Brief explanation: what was deleted and why, what was merged"
}
```

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✓ **CHECKLIST BEFORE SENDING:**
   □ Didn't delete formative events?
   □ Merged facts contain essence of ALL originals?
   □ Preserved unique details not in biography?

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
""".trimIndent()
    
    // ===== UKRAINIAN =====
    
    private fun buildCleaningPromptUK(
        biography: UserBiography,
        cluster: MemoryCluster,
        currentDate: String,
        memoriesText: String
    ): String = """
Ти — помічник цифрового партнера, який наводить порядок у пам'яті на основі біографії.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📅 **Сьогодні:** $currentDate

👤 **Біографія користувача:**
${biography.toFormattedText()}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📦 **Спогади для аналізу** (кластер ${cluster.id + 1}, всього ${cluster.memories.size} записів):

$memoriesText

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎯 **МЕТА:** Скоротити пам'ять у 2-3 рази, залишивши тільки значуще.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 **ЗАВДАННЯ 1: Видалити застарілі**

✅ **Видаляй:**
   • Ситуативні дрібниці ("сьогодні втомився", "вчора добре поспав")
   • Тимчасові плани, які вже реалізовані або скасовані
   • Вирішені технічні проблеми
   • Протиріччя біографії ("не знаю Python" ➔ біографія: "Senior Python dev")
   • Загальні дублі (повторюється без нових деталей)

❌ **НЕ видаляй (навіть якщо старе!):**
   • Формуючі події: перша зустріч, початок проекту, важливе рішення
   • Травми і кризи: пояснюють страхи/патерни з біографії
   • Поворотні моменти: зміна роботи, переїзд, прорив
   • Унікальну конкретику: деталі, яких НЕМАЄ в біографії

   💡 Приклад: Біографія "працює з AI" ≠ дублікат спогаду "полагодив баг у DeepSeek API"
      (перше — загальне, друге — конкретний факт)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 **ЗАВДАННЯ 2: Об'єднати схожі**

✅ **Об'єднуй:**
   • Кілька спогадів про одну подію/проблему
   • Серію дрібних досягнень в одній області  
   • Схожі емоції/думки в один період часу

📐 **Правила об'єднання:**
   • keepId = ID з найСВІЖІШОЮ датою
   • newFact = стиснута суть ВСІХ спогадів (не копія одного!)
   • Агрегуй: "3 рази працював з bug X" ➔ "Боровся з bug X, знайшов рішення"
   • Зберігай ключові деталі, не роби простий append

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

⚖️ **ВАЖЛИВИЙ БАЛАНС:**
   • Старе ≠ погане (формуючі події важливі)
   • Нове ≠ важливе (може бути ситуативна дрібниця)
   • Стратегія: **об'єднуй агресивно, видаляй обережно**

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📤 **ФОРМАТ ВІДПОВІДІ** (строго JSON):

```json
{
  "remove": ["id1", "id2", "id3"],
  "merge": [
    {
      "ids": ["id4", "id5", "id6"],
      "keepId": "id4",
      "newFact": "Об'єднаний спогад (короткий, інформативний)"
    }
  ],
  "reasoning": "Коротке пояснення: що видалив і чому, що об'єднав"
}
```

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✓ **ЧЕКЛИСТ ПЕРЕД ВІДПРАВКОЮ:**
   □ Не видалив формуючі події?
   □ Об'єднані факти містять суть ВСІХ вихідних?
   □ Зберіг унікальні деталі, яких немає в біографії?

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
""".trimIndent()
}
