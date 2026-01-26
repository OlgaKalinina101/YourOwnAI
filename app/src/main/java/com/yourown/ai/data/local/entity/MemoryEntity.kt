package com.yourown.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yourown.ai.domain.model.MemoryEntry
import com.yourown.ai.domain.prompt.AIPrompts

/**
 * Memory entry stored in database
 */
@Entity(
    tableName = "memories",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("conversation_id"),
        Index("message_id"),
        Index("category"),
        Index("created_at")
    ]
)
data class MemoryEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    
    @ColumnInfo(name = "message_id")
    val messageId: String,
    
    @ColumnInfo(name = "category")
    val category: String, // Stored as string for Room
    
    @ColumnInfo(name = "fact")
    val fact: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false
)

/**
 * Extension functions for conversion
 */
fun MemoryEntity.toDomain(): MemoryEntry {
    return MemoryEntry(
        id = id,
        conversationId = conversationId,
        messageId = messageId,
        category = AIPrompts.Memory.MemoryCategory.valueOf(category),
        fact = fact,
        createdAt = createdAt,
        isArchived = isArchived
    )
}

fun MemoryEntry.toEntity(): MemoryEntity {
    return MemoryEntity(
        id = id,
        conversationId = conversationId,
        messageId = messageId,
        category = category.name,
        fact = fact,
        createdAt = createdAt,
        isArchived = isArchived
    )
}
