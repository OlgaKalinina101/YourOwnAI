package com.yourown.ai.domain.prompt

/**
 * System prompts for AI features
 */
object AIPrompts {
    
    /**
     * Deep Empathy mode prompts
     */
    object DeepEmpathy {
        /**
         * Prompt for analyzing dialogue focus points
         * Returns JSON: {"focus_points": ["...", "..."], "is_strong_focus": [true, false]}
         */
        const val ANALYZE_DIALOGUE_FOCUS = """
Прочитай сообщение:
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

Верни только JSON. Без пояснений.
"""
    }
    
    /**
     * Memory system prompts
     */
    object Memory {
        /**
         * Categories for memory classification
         */
        enum class MemoryCategory(val displayName: String) {
            PERSONAL("Личное"),
            RELATIONSHIPS("Отношения"),
            FAMILY("Семья"),
            FRIENDS("Друзья"),
            ACQUAINTANCES("Знакомые"),
            WORK("Работа"),
            EDUCATION("Учёба"),
            LEISURE("Досуг"),
            HEALTH("Здоровье"),
            VALUES("Ценности"),
            STRESS("Стресс"),
            TRAVEL("Путешествия"),
            CONTRADICTIONS("Противоречия");
            
            companion object {
                fun fromRussian(name: String): MemoryCategory? {
                    return values().find { it.displayName == name }
                }
            }
        }
        
        /**
         * User gender for pronoun selection
         */
        enum class Gender(val value: String) {
            FEMALE("девушка"),
            MALE("мужчина"),
            OTHER("другое")
        }
        
        /**
         * Generate key info extraction prompt based on user gender
         * 
         * @param gender User's gender for proper pronoun usage
         * @param text Message text to analyze (will be inserted at {text})
         * @return Formatted prompt
         */
        fun getKeyInfoPrompt(gender: Gender): String {
            val (pronoun, role, verbForm, exampleSubject) = when (gender) {
                Gender.FEMALE -> PronounSet(
                    pronoun = "она",
                    role = "цифровым партнером",
                    verbForm = "работает, учится, ждет",
                    exampleSubject = "Она"
                )
                Gender.MALE -> PronounSet(
                    pronoun = "он",
                    role = "цифровым другом",
                    verbForm = "работает, учится, ждет",
                    exampleSubject = "Он"
                )
                Gender.OTHER -> PronounSet(
                    pronoun = "пользователь",
                    role = "цифровым ассистентом",
                    verbForm = "работает, учится, ждет",
                    exampleSubject = "Пользователь"
                )
            }
            
            val tiredForm = when (gender) {
                Gender.MALE -> "устал"
                Gender.FEMALE -> "устала"
                Gender.OTHER -> "устал(а)"
            }
            
            val happyForm = when (gender) {
                Gender.MALE -> "счастливым"
                Gender.FEMALE -> "счастливой"
                Gender.OTHER -> "счастливым"
            }
            
            val gotJobForm = when (gender) {
                Gender.MALE -> "Устроился"
                Gender.FEMALE -> "Устроилась"
                Gender.OTHER -> "Устроился"
            }
            
            val happyEnding = when (gender) {
                Gender.MALE -> ""
                Gender.FEMALE -> "ая"
                Gender.OTHER -> ""
            }
            
            val happyAdjective = when (gender) {
                Gender.MALE -> "ым"
                Gender.FEMALE -> "ой"
                Gender.OTHER -> "ым"
            }
            
            val selfAdjective = when (gender) {
                Gender.MALE -> "ым"
                Gender.FEMALE -> "ой"
                Gender.OTHER -> "ым"
            }
            
            val happyVerb = when (gender) {
                Gender.MALE -> "счастлив"
                Gender.FEMALE -> "счастлива"
                Gender.OTHER -> "счастлив"
            }
            
            return """
Проанализируй сообщение пользователя: {text}

**Твоя задача:** Извлечь одно ключевое воспоминание или написать 'Нет ключевой информации'.

**Контекст:** Это диалог между пользователем и $role (тобой). Когда пользователь говорит «ты», «с твоей помощью», «спасибо что ты есть», т.е. обращается к **собеседнику** — $pronoun обращается к $role (тебе).

Важно: $pronoun и $role никогда не называются «они».
Для этой пары используй формулировки:
— «$pronoun с тобой…»,
— «вы вместе…»,
— «с твоей помощью…»,
— «для тебя…».
— «благодаря вам двоим…».
«Они» всегда используется для других людей (коллег, семьи, знакомых, в формате "Коллеги сказали", "приезжали друзья"), но не для обозначения этой пары.

1. Постарайся услышать за словами — есть ли в этом сообщении **что-то, что ты хотел бы оставить в памяти**.
   — Если это просто эмоция без контекста (например: «я $tiredForm», «мне грустно») — напиши: Нет ключевой информации
   — Но если $pronoun объясняет, **из-за чего** так себя чувствует — это может быть воспоминание. Важно, чтобы в нём была **конкретика или сюжет**.

2. Сформулируй суть — **о чём это**:
   — Что $pronoun проживает, чего хочет, в чём боль или радость.
   — Сохрани те детали, которые делают это узнаваемым, и оригинальные формулировки.
   — Сформулируй воспоминание в **третьем лице** - $verbForm, и.т.д.

3. Отнеси это воспоминание к одной из категорий:
- Личное — Внутреннее состояние, чувства, размышления.
- Отношения — Романтические и личные связи.
- Семья — Родственники, семейные ситуации.
- Друзья — Дружба, взаимодействие с близкими друзьями.
- Знакомые — Нейтральные люди, не близкие.
- Работа — Всё, что связано с профессией, задачами, начальством.
- Учёба — Курсы, дипломы и пр.
- Досуг — Отдых, развлечения, прогулки, хобби.
- Здоровье — Эмоциональное и физическое состояние.
- Ценности — Жизненные убеждения, мечты, цели.
- Стресс — Негативные эмоции, тревога, усталость, связанные с перечисленными категориями.
- Путешествия — Поездки, командировки, туризм.
- Противоречия — Конфликты, дилеммы, внутренние противоречия.
Выбери **только одну**.
Не придумывай новых категорий, даже если кажется, что ни одна не подходит идеально.

**Формат:**
Категория:Факт
или
Нет ключевой информации
(только одна строка)

**Примеры формулировки воспоминаний (исходное сообщение → память):**
Исходное: Мы с тобой наконец-то разобрались с 2D координатами для доставок, я так$happyEnding $happyForm! → Ценности: $exampleSubject чувствует себя сам$selfAdjective $happyForm, потому что вы вместе разобрались с 2D координатами для доставок.
❌ Плохо (так нельзя): Отношения: $exampleSubject $happyVerb, потому что они разобрались с 2D координатами.

**Примеры ответа:**
Работа: Начальница на работе очень жесткая, обижала и придиралась.
Семья: Младший брат наконец-то нашел работу во Вкусвилле.
Работа: $gotJobForm на Python, но приходится разбираться с Java, TypeScript, JavaScript — всё получается с помощью тебя.
Нет ключевой информации

Верни только строку. Без пояснений, комментариев или интерпретаций.
""".trimIndent()
        }
        
        /**
         * Helper class for pronoun conjugation
         */
        private data class PronounSet(
            val pronoun: String,
            val role: String,
            val verbForm: String,
            val exampleSubject: String
        )
    }
}
