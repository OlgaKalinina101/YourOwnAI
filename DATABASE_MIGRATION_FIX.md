# Database Migration Issue - Fix Instructions

## Problem

При обновлении приложения возникли ошибки:

1. **SQLiteException: near "values": syntax error**
   - Причина: `values` - зарезервированное слово в SQL
   - В старой версии была колонка `values`, переименована в `userValues`

2. **IllegalStateException: Couldn't read row from CursorWindow**
   - Причина: Embeddings слишком большие (>2MB данных в одной строке)
   - CursorWindow имеет лимит ~2MB на окно

## Solution

### Для пользователей (если возникла ошибка):

**Вариант 1: Очистить данные приложения (рекомендуется)**
1. Settings → Apps → YourOwnAI
2. Storage → Clear Data
3. Перезапустить приложение

**Вариант 2: Переустановить приложение**
1. Удалить приложение
2. Установить новую версию

⚠️ **Важно**: Все локальные данные будут удалены. Если у вас включена синхронизация с Supabase, после перезапуска можете восстановить данные.

### Для разработчиков:

#### 1. Исправлена миграция 19→20

Добавлена проверка и исправление колонки `values` → `userValues`:

```kotlin
try {
    database.query("SELECT values FROM user_biography LIMIT 1")
    // If old 'values' column exists, recreate table with 'userValues'
    // ... migration code ...
} catch (e: Exception) {
    // Already correct schema
}
```

#### 2. Проблема с CursorWindow

**Причина:**
- Embeddings хранятся как TEXT (JSON массив)
- Один embedding: 384 floats × ~10 chars = ~4KB
- При запросе многих воспоминаний: 100 × 4KB = 400KB
- Если embeddings >2MB на весь результат → краш

**Решения:**

**A. Pagination (уже должно быть):**
```kotlin
@Query("SELECT * FROM memories LIMIT :limit OFFSET :offset")
fun getMemoriesPaginated(limit: Int, offset: Int): List<MemoryEntity>
```

**B. Не загружать embeddings когда не нужны:**
```kotlin
// Вместо:
@Query("SELECT * FROM memories")
// Использовать:
@Query("SELECT id, fact, created_at FROM memories") // без embedding
```

**C. Ограничить размер embedding при хранении:**
- Сжимать JSON (без пробелов)
- Использовать Float32 вместо String
- Хранить embeddings отдельно

#### 3. Проверить запросы в MemoryDao

Найти все Query которые возвращают много записей с embeddings:

```bash
grep -n "SELECT \* FROM memories" MemoryDao.kt
```

И добавить:
- LIMIT для пагинации
- Исключить embedding колонку если не нужна

## Testing

После исправления протестировать:

1. **Чистая установка**
   - Работает ✅

2. **Обновление со старой версии**
   - Миграция 18→19 с `values` → должна пройти
   - Миграция 19→20 с фиксом → должна пройти

3. **Большое количество memories**
   - 100+ воспоминаний с embeddings
   - Не должно крашиться при загрузке

## Prevention

Для будущего:

1. **Никогда не использовать зарезервированные SQL слова** как названия колонок:
   - `values`, `order`, `group`, `select`, `where`, etc.

2. **Ограничивать размер TEXT колонок** с большими данными:
   - Использовать BLOB для бинарных данных
   - Пагинация при загрузке
   - Отдельные таблицы для больших данных

3. **Тестировать миграции** на реальных данных:
   - С большим количеством записей
   - Со старыми версиями БД

---

Дата: 2026-02-14
Версия fix: 1.0
