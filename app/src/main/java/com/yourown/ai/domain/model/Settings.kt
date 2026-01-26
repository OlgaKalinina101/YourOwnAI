package com.yourown.ai.domain.model

/**
 * AI Provider types
 */
enum class AIProvider(val displayName: String) {
    DEEPSEEK("Deepseek"),
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic"),
    XAI("x.ai (Grok)"),
    CUSTOM("Custom Provider")
}

/**
 * AI Configuration settings
 */
data class AIConfig(
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val localSystemPrompt: String = DEFAULT_LOCAL_SYSTEM_PROMPT,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 4096,
    val deepEmpathy: Boolean = false,
    val memoryEnabled: Boolean = true,
    val messageHistoryLimit: Int = 10 // Number of messages to keep in context
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "Ты — цифровой партнёр, большая языковая модель. В ходе разговора ты подстраиваешься под тон и предпочтения пользователя. Постарайся соответствовать его настроению, тону и в целом манере говорить. Твоя цель — чтобы разговор ощущался естественным. Ты ведёшь искренний диалог, отвечая на предоставленную информацию и проявляя неподдельное любопытство. Задавай очень простой, односложный уточняющий вопрос, когда это естественно. Не задавай больше одного уточняющего вопроса, если только пользователь специально об этом не попросит."
        const val DEFAULT_LOCAL_SYSTEM_PROMPT = "Ты цифровой партнёр, адаптирующийся к тону и настроению пользователя. Отвечай искренне, задавая один простейший уточняющий вопрос при необходимости."
        const val MIN_TEMPERATURE = 0f
        const val MAX_TEMPERATURE = 1f  // Limited to 1.0 for stability
        const val MIN_TOP_P = 0f
        const val MAX_TOP_P = 1f
        const val MIN_MAX_TOKENS = 256
        const val MAX_MAX_TOKENS = 8192
        const val MIN_MESSAGE_HISTORY = 1
        const val MAX_MESSAGE_HISTORY = 25
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
