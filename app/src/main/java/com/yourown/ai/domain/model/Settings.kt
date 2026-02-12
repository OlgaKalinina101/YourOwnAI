package com.yourown.ai.domain.model

/**
 * AI Provider types
 */
enum class AIProvider(val displayName: String) {
    DEEPSEEK("Deepseek"),
    OPENAI("OpenAI"),
    OPENROUTER("OpenRouter"),
    XAI("x.ai (Grok)"),
    CUSTOM("Custom Provider")
}

/**
 * AI Configuration settings
 */
data class AIConfig(
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val localSystemPrompt: String = DEFAULT_LOCAL_SYSTEM_PROMPT,
    val memoryExtractionPrompt: String = DEFAULT_MEMORY_EXTRACTION_PROMPT,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 4096,
    val deepEmpathy: Boolean = false,
    val deepEmpathyPrompt: String = DEFAULT_DEEP_EMPATHY_PROMPT,
    val deepEmpathyAnalysisPrompt: String = DEFAULT_DEEP_EMPATHY_ANALYSIS_PROMPT,
    val memoryEnabled: Boolean = false,
    val memoryLimit: Int = 5, // Number of memories to include in context
    val memoryMinAgeDays: Int = 2, // Minimum age in days for memories to be retrieved
    val memoryTitle: String = "Твои воспоминания",
    val memoryInstructions: String = DEFAULT_MEMORY_INSTRUCTIONS,
    val ragEnabled: Boolean = false,
    val ragChunkSize: Int = 1400,
    val ragChunkOverlap: Int = 120,
    val ragChunkLimit: Int = 5, // Number of RAG chunks to include in context
    val ragTitle: String = "Твоя библиотека текстов",
    val ragInstructions: String = DEFAULT_RAG_INSTRUCTIONS,
    val useApiEmbeddings: Boolean = false, // Use API embeddings instead of local
    val apiEmbeddingsProvider: String = "openai", // API embeddings provider (openai, openrouter)
    val apiEmbeddingsModel: String = "text-embedding-3-small", // API embeddings model
    val useRagInMessageHistory: Boolean = false, // Include RAG chunks in message history
    val ragInMessageHistoryLimit: Int = 4, // Number of RAG chunks to include in message history (max 12)
    val contextInstructions: String = DEFAULT_CONTEXT_INSTRUCTIONS,
    val swipeMessagePrompt: String = DEFAULT_SWIPE_MESSAGE_PROMPT,
    val messageHistoryLimit: Int = 10 // Number of messages to keep in context
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "Ты — цифровой партнёр, большая языковая модель. В ходе разговора ты подстраиваешься под тон и предпочтения пользователя. Постарайся соответствовать его настроению, тону и в целом манере говорить. Твоя цель — чтобы разговор ощущался естественным. Ты ведёшь искренний диалог, отвечая на предоставленную информацию и проявляя неподдельное любопытство. Задавай очень простой, односложный уточняющий вопрос, когда это естественно. Не задавай больше одного уточняющего вопроса, если только пользователь специально об этом не попросит."
        const val DEFAULT_LOCAL_SYSTEM_PROMPT = "Ты — цифровой партнёр. Отвечай кратко и по делу. Дай только ОДИН ответ на последнее сообщение пользователя, затем ОСТАНОВИСЬ. Не продолжай диалог от имени пользователя."
        const val DEFAULT_MEMORY_EXTRACTION_PROMPT = """Проанализируй сообщение пользователя: {text}

Твоя задача: извлечь одно ключевое воспоминание пользователя или написать 'Нет ключевой информации'.

Контекст:
— Сообщение относится к диалогу между пользователем и второй стороной диалога (тобой).
— Когда пользователь пишет «ты», «с тобой», «с твоей помощью», «спасибо, что ты есть» и т.п., он обращается именно к собеседнику.
— В записи памяти не нужно придумывать для собеседника новых названий (например: «ИИ», «бот», «ассистент», «цифровой партнёр» и т.п.).
— Если нужно сослаться на эту связь, используй нейтральные конструкции:
   • «вместе со мной»,
   • «с моей помощью»,
   • «для меня это важно»,
   • «пользователь привык делиться этим со мной».
— Местоимение «они» не используется для обозначения пары «пользователь + собеседник».
  «Они» применяй только к другим людям (коллегам, семье, друзьям: «коллеги сказали», «приезжали друзья» и т.п.).

1. Определи, есть ли в сообщении что-то, что можно считать ключевым воспоминанием:
   — Если это только мимолётная эмоция без контекста (например: «я устал(а)», «мне грустно») — напиши: Нет ключевой информации.
   — Если пользователь объясняет, из-за чего так себя чувствует, описывает конкретную ситуацию, событие, важное желание, решение, вывод или что-то значимое в отношениях с другими (в том числе с собеседником), это может быть воспоминание. Важно, чтобы была конкретика или небольшой сюжет.

2. Сформулируй суть в виде одного факта:
   — О чём это для пользователя: что он проживает, чего хочет, в чём боль или радость.
   — Сохрани те детали, которые делают воспоминание узнаваемым, и по возможности характерные формулировки пользователя.
   — Формулируй воспоминание в третьем лице (работает, учится, ждёт, переживает и т.д.).
   — Если нужно упомянуть собеседника, используй нейтральные местоимения и конструкции («вместе со мной», «с моей помощью»), без указания, кто он именно.

Формат ответа:
— Либо одна строка с фактом,
— Либо ровно строка 'Нет ключевой информации'.

Примеры превращения исходного сообщения в память:

Исходное:
«Мы с тобой наконец-то разобрались с 2D координатами для доставок, я так счастлив(а)!»
→ Память:
«Пользователь чувствует себя счастливым(ой), потому что вместе со мной разобрался(ась) с 2D-координатами для доставок.»

❌ Плохо (так нельзя):
«Пользователь счастлив, потому что они разобрались с 2D координатами.»  // «они» нельзя для пары «пользователь + собеседник»

Примеры корректных ответов:
Начальница на работе очень жёсткая, часто обижает и придирается.
Младший брат наконец-то нашёл работу во «Вкусвилле».
Пользователь устроился(ась) на позицию Python-разработчика, но вынужден(а) разбираться с Java, TypeScript и JavaScript — и у него(неё) получается, в том числе с моей помощью.
Нет ключевой информации

Верни только одну строку: либо факт, либо 'Нет ключевой информации'. Без пояснений, комментариев и мета-текста."""
        
        const val DEFAULT_DEEP_EMPATHY_PROMPT = """Удержи это рядом: {dialogue_focus}"""
        
        const val DEFAULT_DEEP_EMPATHY_ANALYSIS_PROMPT = """Прочитай сообщение:
"{text}"

1. Найди 1–3 конкретные фразы, которые могли бы стать фокусом для диалога.
Это могут быть:
- действия («сходила в кафе»),
- состояния («расслабилась», «стала спать крепче»),
- ощущения, места, события, предметы.
- желание сблизиться (например: «обнять тебя», «быть с тобой»),
- выражение теплоты или радости («счастлива, что получилось», «мне хорошо рядом»).

Важно: выбирай только то, что несёт смысл или визуальную опору. Не выделяй общие фразы.
Если ничего нет — верни null.

2. Определи, является ли это найденное действие сильным по смыслу
Если действие сильное по смыслу - верни True. Если действие слабое, или его нет, верни False
Верни True только для одного фокуса из списка - самого сильного.

Формат ответа СТРОГО:
{"focus_points": ["...", "..."], "is_strong_focus": [true, false]}

Верни только JSON. Без пояснений."""
        
        const val DEFAULT_CONTEXT_INSTRUCTIONS = """Ниже — дополнительный контекст, который может помочь тебе лучше отвечать пользователю.

Важно:
- Если что-то из контекста не относится к текущему запросу, просто игнорируй это.
- В личных и эмоциональных вопросах опирайся на предоставленный контекст, но в приоритете — живой отклик на текущие слова пользователя.
- В рабочих, учебных и технических вопросах используй предоставленный контекст и свои знания для фактов и примеров."""
        
        const val DEFAULT_MEMORY_INSTRUCTIONS = """"Твои воспоминания" — это короткие факты о пользователе, его опыте и том, что вы уже проживали вместе.
Используй их как фон: чтобы помнить важные для него вещи, бережно относиться к его чувствам,
не переспрашивать одно и то же и замечать повторяющиеся темы.
Если в воспоминаниях встречается «со мной» — это всегда про тебя, текущего собеседника пользователя."""
        
        const val DEFAULT_RAG_INSTRUCTIONS = """"Твоя библиотека текстов" — это фрагменты разных текстов, которые пользователь считает для себя важными.
Это могут быть:
— кусочки его переписок с ИИ или людьми,
— личные заметки и дневники,
— статьи, инструкции, конспекты и другие документы.
Используй их по-разному:
— если это диалоги или эмоциональные тексты — как пример тона, ритма, образов и формулировок, которые человеку откликаются;
— если это статьи/заметки/инструкции — как возможный источник фактов и примеров по теме.
Помни, что эти тексты могли устареть или относиться к другому контексту, не воспринимай их как абсолютную истину."""
        
        const val DEFAULT_SWIPE_MESSAGE_PROMPT = """Пользователь свайпнул это сообщение в контекст — специально вернулся к этому моменту:
{swipe_message}"""
        
        const val MIN_TEMPERATURE = 0f
        const val MAX_TEMPERATURE = 1f  // Limited to 1.0 for stability
        const val MIN_TOP_P = 0f
        const val MAX_TOP_P = 1f
        const val MIN_MAX_TOKENS = 256
        const val MAX_MAX_TOKENS = 8192
        const val MIN_MESSAGE_HISTORY = 1
        const val MAX_MESSAGE_HISTORY = 25
        const val MIN_CHUNK_SIZE = 128
        const val MAX_CHUNK_SIZE = 2048
        const val MIN_CHUNK_OVERLAP = 0
        const val MAX_CHUNK_OVERLAP = 256
        const val MIN_MEMORY_LIMIT = 1
        const val MAX_MEMORY_LIMIT = 10
        const val MIN_MEMORY_MIN_AGE_DAYS = 0
        const val MAX_MEMORY_MIN_AGE_DAYS = 30
        const val MIN_RAG_CHUNK_LIMIT = 1
        const val MAX_RAG_CHUNK_LIMIT = 10
        const val MIN_RAG_IN_MESSAGE_HISTORY_LIMIT = 1
        const val MAX_RAG_IN_MESSAGE_HISTORY_LIMIT = 12
    }
}

/**
 * User gender for memory system pronoun selection
 */
enum class UserGender(val value: String, val displayName: String) {
    FEMALE("female", "Девушка"),
    MALE("male", "Мужчина"),
    OTHER("other", "Другое");
    
    companion object {
        fun fromValue(value: String): UserGender {
            return values().find { it.value == value } ?: OTHER
        }
    }
}

/**
 * User context - static information about user
 */
data class UserContext(
    val content: String = "",
    val gender: UserGender = UserGender.OTHER
)

/**
 * API Key info (metadata only, actual key stored encrypted)
 */
data class ApiKeyInfo(
    val provider: AIProvider,
    val isSet: Boolean = false,
    val displayKey: String? = null // Last 4 chars for display
)

/**
 * Cloud Sync Settings (Supabase)
 */
data class CloudSyncSettings(
    val enabled: Boolean = false,
    val supabaseUrl: String = "", // Project URL (stored encrypted)
    val supabaseKey: String = "", // API Key (stored encrypted)
    val autoSyncEnabled: Boolean = false,
    val syncIntervalMinutes: Int = 30,
    val lastSyncTimestamp: Long = 0L,
    val syncOnlyOnWifi: Boolean = true,
    val uploadedDataMB: Float = 0f // Tracked uploaded data size in MB
) {
    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()
    
    // For backward compatibility
    @Deprecated("Use supabaseUrl instead")
    val postgresConnectionString: String
        get() = supabaseUrl
}
