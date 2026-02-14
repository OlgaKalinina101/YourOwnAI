# Troubleshooting: 0 Memories After Sync

## Проблема
После синхронизации с Supabase показывает 0 memories.

## Возможные причины и решения

### 1. ❌ Старая схема в Supabase (САМАЯ ВЕРОЯТНАЯ ПРИЧИНА)

**Проблема:**
- В Supabase ещё используется старая схема с колонкой `embedding BYTEA`
- При синхронизации вниз (download) новый код пытается десериализовать DTO без `embedding`
- Supabase возвращает `embedding: <binary data>`, но наш DTO не ожидает этого поля
- Десериализация падает молча

**Решение:**

#### A. Обновить схему в Supabase:

1. Откройте Supabase Dashboard → SQL Editor

2. **ВАЖНО:** Сначала сделайте backup данных:
```sql
-- Backup memories
SELECT * FROM memories;
```
Скопируйте результат в файл.

3. Пересоздайте таблицу `memories`:
```sql
-- Drop old table
DROP TABLE IF EXISTS memories CASCADE;

-- Create new table WITHOUT embedding
CREATE TABLE IF NOT EXISTS memories (
    id TEXT PRIMARY KEY,
    conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    message_id TEXT NOT NULL,
    fact TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    persona_id TEXT,
    device_id TEXT,
    synced_at BIGINT
);

-- Recreate indexes
CREATE INDEX IF NOT EXISTS idx_memories_conversation ON memories(conversation_id);
CREATE INDEX IF NOT EXISTS idx_memories_persona ON memories(persona_id);
```

4. В приложении нажмите "Загрузить на облако" заново

#### B. Или создайте новый Supabase проект:

1. Создайте новый проект в Supabase
2. Выполните НОВУЮ схему из `supabase_schema.sql` (уже оптимизированную)
3. В приложении введите новые credentials
4. Синхронизируйте заново

---

### 2. ❌ Проверьте логи синхронизации

**В Android Studio Logcat найдите:**

```
CloudSyncRepository: Found X memories to sync
CloudSyncRepository: ✅ Uploaded X memories (NO embeddings)
```

Если `X = 0` → memories не загрузились на облако.

**Причины:**
- Memories локально удалены
- Ошибка при загрузке (проверьте exception logs)

**Решение:**
- Создайте тестовое воспоминание в чате
- Попробуйте синхронизацию снова

---

### 3. ❌ Проверьте данные в Supabase Dashboard

1. Откройте Supabase Dashboard
2. Table Editor → `memories`
3. Проверьте сколько записей там есть

**Если записей нет:**
- Memories не загрузились на облако
- Проверьте логи в Android Studio

**Если записи есть:**
- Проблема при скачивании
- Проверьте ошибки десериализации в логах

---

### 4. ❌ Несоответствие схемы DTO и Supabase

**Проверьте в Supabase:**

```sql
-- Посмотрите структуру таблицы
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'memories';
```

**Должно быть (новая схема):**
- `id` TEXT
- `conversation_id` TEXT
- `message_id` TEXT
- `fact` TEXT
- `created_at` BIGINT
- `persona_id` TEXT
- `device_id` TEXT
- `synced_at` BIGINT
- ❌ **НЕТ** `embedding` BYTEA

**Если есть `embedding`:**
- Вы используете старую схему
- Нужно обновить (см. решение #1)

---

### 5. ❌ Проблема с десериализацией PersonaId

Если `persona_id` в Supabase = `NULL`, а DTO ожидает `String?`, может быть ошибка парсинга.

**Проверьте логи:**
```
Failed to fetch memories: ...
kotlinx.serialization.SerializationException: ...
```

**Решение:**
- Все поля с `?` в DTO должны иметь `= null` default
- Уже исправлено в `MemoryDto`

---

## Quick Check Script

**Для проверки в Supabase SQL Editor:**

```sql
-- 1. Проверить структуру memories
SELECT column_name, data_type, is_nullable
FROM information_schema.columns 
WHERE table_name = 'memories'
ORDER BY ordinal_position;

-- 2. Посчитать записи
SELECT COUNT(*) as total_memories FROM memories;

-- 3. Посмотреть последние 5 memories
SELECT id, LEFT(fact, 50) as fact_preview, created_at 
FROM memories 
ORDER BY created_at DESC 
LIMIT 5;

-- 4. Проверить есть ли embedding колонка (не должно быть)
SELECT column_name 
FROM information_schema.columns 
WHERE table_name = 'memories' AND column_name = 'embedding';
-- Должен вернуть 0 строк!
```

---

## Debug Steps

### Шаг 1: Проверьте локальную БД

В Android Studio Logcat найдите:
```
MemoryRepository: Found X memory entities
```

Если X > 0 → memories есть локально

### Шаг 2: Проверьте upload

После "Загрузить на облако" найдите:
```
CloudSyncRepository: ✅ Uploaded X memories (NO embeddings)
```

### Шаг 3: Проверьте Supabase

В Supabase Table Editor откройте `memories` и проверьте количество записей.

### Шаг 4: Проверьте download

После "Скачать с облака" найдите:
```
CloudSyncRepository: Fetched from cloud: X memories (NO embeddings)
CloudSyncRepository: Merged X memories
```

### Шаг 5: Проверьте UI

В настройках откройте "Memories" и посмотрите количество.

---

## Most Likely Solution

**99% вероятность:** В Supabase используется старая схема с `embedding BYTEA`.

**Fix:**
1. Пересоздайте таблицу `memories` БЕЗ колонки `embedding` (SQL выше)
2. Загрузите данные заново из приложения

---

Дата: 2026-02-14
