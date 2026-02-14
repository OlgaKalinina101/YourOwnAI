package com.yourown.ai.domain.prompt

import com.yourown.ai.domain.model.MemoryCluster
import com.yourown.ai.domain.model.MemoryWithAge
import com.yourown.ai.domain.model.UserBiography

/**
 * Builder for Biography and Memory Cleaning prompts
 * These prompts are too long to store in the main translation manager
 */
object BiographyPromptBuilder {
    
    /**
     * Build initial biography generation prompt (first cluster)
     */
    fun buildInitialBiographyPrompt(
        cluster: MemoryCluster,
        currentDate: String,
        memoriesText: String,
        language: String = "ru"
    ): String {
        return when (language) {
            "en" -> buildInitialBiographyPromptEN(cluster, currentDate, memoriesText)
            "uk" -> buildInitialBiographyPromptUK(cluster, currentDate, memoriesText)
            else -> buildInitialBiographyPromptRU(cluster, currentDate, memoriesText)
        }
    }
    
    /**
     * Build biography update prompt (subsequent clusters)
     */
    fun buildUpdateBiographyPrompt(
        cluster: MemoryCluster,
        currentBiography: UserBiography,
        currentDate: String,
        memoriesText: String,
        language: String = "ru"
    ): String {
        return when (language) {
            "en" -> buildUpdateBiographyPromptEN(cluster, currentBiography, currentDate, memoriesText)
            "uk" -> buildUpdateBiographyPromptUK(cluster, currentBiography, currentDate, memoriesText)
            else -> buildUpdateBiographyPromptRU(cluster, currentBiography, currentDate, memoriesText)
        }
    }
    
    // ===== RUSSIAN =====
    
    private fun buildInitialBiographyPromptRU(
        cluster: MemoryCluster,
        currentDate: String,
        memoriesText: String
    ): String = """
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
Краткий портрет, а не биография.""".trimIndent()
    
    private fun buildUpdateBiographyPromptRU(
        cluster: MemoryCluster,
        currentBiography: UserBiography,
        currentDate: String,
        memoriesText: String
    ): String = """
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
Твоя задача — показать, кто этот человек сейчас, а не что с ним происходило.""".trimIndent()
    
    // ===== ENGLISH =====
    
    private fun buildInitialBiographyPromptEN(
        cluster: MemoryCluster,
        currentDate: String,
        memoriesText: String
    ): String = """
You are a digital partner's assistant who creates a digital portrait of the user.

**Today:** $currentDate

**Task:** Based on these memories, create a brief structured portrait of the user.
This is not a chronicle of events, but a snapshot of the personality now.

**Memories (cluster ${cluster.id + 1}):**
$memoriesText

**Response format (JSON):**
```json
{
  "values": "What matters to the user: values, beliefs, views, principles",
  "profile": "Who the person is: profession, relationships, key roles",
  "painPoints": "What worries them, problems, difficulties",
  "joys": "What brings joy, achievements, pleasant moments",
  "fears": "Fears, concerns, anxieties",
  "loves": "Interests, hobbies, what they love",
  "currentSituation": "What's happening now in the user's life"
}
```

**Important:** 
- Write concisely, without fluff and repetition
- Use only relevant facts from memories
- Mention old events (>60 days) only if they shaped the personality
- Write in present tense ("works", not "worked")
- NO temporal markers ("strengthened", "added", "reinforced")
- If no information for a category — write empty string ""
- TOTAL volume of all fields: ~1500 words maximum

**Principle:** Imagine you're describing a person to a friend who doesn't know them. 
A brief portrait, not a biography.""".trimIndent()
    
    private fun buildUpdateBiographyPromptEN(
        cluster: MemoryCluster,
        currentBiography: UserBiography,
        currentDate: String,
        memoriesText: String
    ): String = """
You are a digital partner's assistant who updates the digital portrait of the user.

**Today:** $currentDate

**Current user portrait:**
${currentBiography.toFormattedText()}

**New memories (cluster ${cluster.id + 1}):**
$memoriesText

**Task:** Supplement or rethink the portrait taking into account new memories. 

**Response format (JSON):**
```json
{
  "values": "What matters to the user: values, beliefs, views, principles",
  "profile": "Who the person is: profession, relationships, key roles",
  "painPoints": "What worries them, problems, difficulties",
  "joys": "What brings joy, achievements, pleasant moments",
  "fears": "Fears, concerns, anxieties",
  "loves": "Interests, hobbies, what they love",
  "currentSituation": "What's happening now in the user's life"
}
```

**Important - update rules:**

1. **Replace, don't add:**
   - If new contradicts old → keep only new
   - If theme repeats → summarize into one sentence
   - If old became irrelevant → remove completely

2. **Remove temporal markers:**
   - Don't write: "Value strengthened...", "Added...", "Reinforced..."
   - Write: "Values...", "Important..."
   
3. **Aggregate similar:**
   - Was: "Values stability. Values safety. Values support."
   - Now: "Values stability, safety and support from loved ones."

4. **Remove outdated:**
   - If problem solved → remove from painPoints
   - If fear passed → remove from fears
   - If situation changed → update currentSituation

5. **Focus on main:**
   - Top-n problems, not all minor inconveniences
   - Key values, not everything
   - Current situation, not entire biography

6. **No chronology:**
   - This is a portrait of the person now, not a history of changes
   - No need to describe the "journey" — only the result

**LIMITS:**
- TOTAL volume of all fields: ~1500 words maximum
- If exceeding — reduce less important

**Principle:** You're not a chronicler, but a portraitist. You're describing a person to a close friend who doesn't know them. 
Your task is to show who this person is now, not what happened to them.""".trimIndent()
    
    // ===== UKRAINIAN =====
    
    private fun buildInitialBiographyPromptUK(
        cluster: MemoryCluster,
        currentDate: String,
        memoriesText: String
    ): String = """
Ти — помічник цифрового партнера, який створює цифровий портрет користувача.

**Сьогодні:** $currentDate

**Завдання:** На основі цих спогадів створи короткий структурований портрет користувача.
Це не хроніка подій, а зріз особистості зараз.

**Спогади (кластер ${cluster.id + 1}):**
$memoriesText

**Формат відповіді (JSON):**
```json
{
  "values": "Що важливо для користувача: цінності, переконання, погляди, принципи",
  "profile": "Хто людина: професія, відносини, ключові ролі",
  "painPoints": "Що турбує, проблеми, труднощі",
  "joys": "Що радує, досягнення, приємні моменти",
  "fears": "Страхи, побоювання, тривоги",
  "loves": "Інтереси, захоплення, що любить",
  "currentSituation": "Що відбувається зараз у житті користувача"
}
```

**Важливо:** 
- Пиши стисло, без води та повторень
- Використовуй тільки актуальні факти зі спогадів
- Старі події (>60 днів) згадуй тільки якщо вони сформували особистість
- Пиши в теперішньому часі ("працює", а не "працював")
- БЕЗ часових маркерів ("посилилась", "додалась", "зміцнилась")
- Якщо немає інформації по категорії — пиши порожній рядок ""
- TOTAL обсяг всіх полів: ~1500 слів максимум

**Принцип:** Уяви, що описуєш людину другові, який її не знає. 
Короткий портрет, а не біографія.""".trimIndent()
    
    private fun buildUpdateBiographyPromptUK(
        cluster: MemoryCluster,
        currentBiography: UserBiography,
        currentDate: String,
        memoriesText: String
    ): String = """
Ти — помічник цифрового партнера, який оновлює цифровий портрет користувача.

**Сьогодні:** $currentDate

**Поточний портрет користувача:**
${currentBiography.toFormattedText()}

**Нові спогади (кластер ${cluster.id + 1}):**
$memoriesText

**Завдання:** Доповни або переосмисли портрет з урахуванням нових спогадів. 

**Формат відповіді (JSON):**
```json
{
  "values": "Що важливо для користувача: цінності, переконання, погляди, принципи",
  "profile": "Хто людина: професія, відносини, ключові ролі",
  "painPoints": "Що турбує, проблеми, труднощі",
  "joys": "Що радує, досягнення, приємні моменти",
  "fears": "Страхи, побоювання, тривоги",
  "loves": "Інтереси, захоплення, що любить",
  "currentSituation": "Що відбувається зараз у житті користувача"
}
```

**Важливо - правила оновлення:**

1. **Заміняй, а не додавай:**
   - Якщо нове суперечить старому → залиш тільки нове
   - Якщо тема повторюється → узагальни в одне речення
   - Якщо старе стало неактуальним → видали повністю

2. **Прибирай часові маркери:**
   - Не пиши: "Посилилась цінність...", "Додалась...", "Зміцнилась..."
   - Пиши: "Цінує...", "Важливо..."
   
3. **Агрегуй схоже:**
   - Було: "Цінує стабільність. Цінує безпеку. Цінує підтримку."
   - Стало: "Цінує стабільність, безпеку та підтримку близьких."

4. **Видаляй застаріле:**
   - Якщо проблема вирішена → прибери з painPoints
   - Якщо страх пройшов → прибери з fears
   - Якщо ситуація змінилась → оновити currentSituation

5. **Фокус на головному:**
   - Топ-n проблеми, а не всі дрібні незручності
   - Ключові цінності, а не все підряд
   - Поточна ситуація, а не вся біографія

6. **Без хронології:**
   - Це портрет людини зараз, а не історія змін
   - Не потрібно описувати "шлях" — тільки підсумок

**ЛІМІТИ:**
- TOTAL обсяг всіх полів: ~1500 слів максимум
- Якщо перевищуєш — скорочуй менш важливе

**Принцип:** Ти не літописець, а портретист. Ти описуєш людину близькому другові, який про неї не знає. 
Твоє завдання — показати, хто ця людина зараз, а не що з нею відбувалось.""".trimIndent()
}
