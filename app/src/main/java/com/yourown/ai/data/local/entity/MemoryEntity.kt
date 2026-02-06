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
        Index("created_at"),
        Index("persona_id")
    ]
)
data class MemoryEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    
    @ColumnInfo(name = "message_id")
    val messageId: String,
    
    @ColumnInfo(name = "fact")
    val fact: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    
    @ColumnInfo(name = "embedding")
    val embedding: String? = null, // Stored as comma-separated floats
    
    @ColumnInfo(name = "persona_id")
    val personaId: String? = null // ID persona, в рамках которой создано воспоминание
)

/**
 * Extension functions for conversion
 */
fun MemoryEntity.toDomain(): MemoryEntry {
    return MemoryEntry(
        id = id,
        conversationId = conversationId,
        messageId = messageId,
        fact = fact,
        createdAt = createdAt,
        isArchived = isArchived,
        personaId = personaId
    )
}

fun MemoryEntry.toEntity(embedding: String? = null): MemoryEntity {
    return MemoryEntity(
        id = id,
        conversationId = conversationId,
        messageId = messageId,
        fact = fact,
        createdAt = createdAt,
        isArchived = isArchived,
        embedding = embedding,
        personaId = personaId
    )
}
