package com.yourown.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yourown.ai.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for memory operations
 */
@Dao
interface MemoryDao {
    
    /**
     * Get all memories for a conversation
     */
    @Query("SELECT * FROM memories WHERE conversation_id = :conversationId AND is_archived = 0 ORDER BY created_at DESC")
    fun getMemoriesForConversation(conversationId: String): Flow<List<MemoryEntity>>
    
    /**
     * Get all memories across all conversations
     */
    @Query("SELECT * FROM memories WHERE is_archived = 0 ORDER BY created_at DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>
    
    /**
     * Get memories for a specific persona
     */
    @Query("SELECT * FROM memories WHERE persona_id = :personaId AND is_archived = 0 ORDER BY created_at DESC")
    fun getMemoriesByPersonaId(personaId: String): Flow<List<MemoryEntity>>
    
    /**
     * Get global memories (not tied to any persona)
     */
    @Query("SELECT * FROM memories WHERE persona_id IS NULL AND is_archived = 0 ORDER BY created_at DESC")
    fun getGlobalMemories(): Flow<List<MemoryEntity>>
    
    /**
     * Get memory by ID
     */
    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: String): MemoryEntity?
    
    /**
     * Get memory for a specific message (if exists)
     */
    @Query("SELECT * FROM memories WHERE message_id = :messageId LIMIT 1")
    suspend fun getMemoryForMessage(messageId: String): MemoryEntity?
    
    /**
     * Search memories by fact content
     */
    @Query("SELECT * FROM memories WHERE fact LIKE '%' || :query || '%' AND is_archived = 0 ORDER BY created_at DESC")
    fun searchMemories(query: String): Flow<List<MemoryEntity>>
    
    /**
     * Get total memory count
     */
    @Query("SELECT COUNT(*) FROM memories WHERE is_archived = 0")
    suspend fun getTotalMemoryCount(): Int
    
    /**
     * Insert new memory
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)
    
    /**
     * Insert multiple memories
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<MemoryEntity>)
    
    /**
     * Update memory
     */
    @Update
    suspend fun updateMemory(memory: MemoryEntity)
    
    /**
     * Archive memory (soft delete)
     */
    @Query("UPDATE memories SET is_archived = 1 WHERE id = :id")
    suspend fun archiveMemory(id: String)
    
    /**
     * Delete memory permanently
     */
    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)
    
    /**
     * Delete all memories for a conversation
     */
    @Query("DELETE FROM memories WHERE conversation_id = :conversationId")
    suspend fun deleteMemoriesForConversation(conversationId: String)
    
    /**
     * Delete all archived memories
     */
    @Query("DELETE FROM memories WHERE is_archived = 1")
    suspend fun deleteArchivedMemories()
    
    /**
     * Delete all memories (for debugging/testing)
     */
    @Query("DELETE FROM memories")
    suspend fun deleteAllMemories()
}
