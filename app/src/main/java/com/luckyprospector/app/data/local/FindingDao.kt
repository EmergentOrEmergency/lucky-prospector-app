package com.luckyprospector.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FindingDao {
    @Query("SELECT * FROM findings ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<FindingEntity>>

    @Query("SELECT * FROM findings WHERE id = :id LIMIT 1")
    suspend fun get(id: String): FindingEntity?

    @Upsert
    suspend fun upsert(finding: FindingEntity)

    @Query("DELETE FROM findings WHERE id = :id")
    suspend fun deleteById(id: String)
}
