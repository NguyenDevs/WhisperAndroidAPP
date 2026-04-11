package com.nguyendevs.stkh.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY date DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY date DESC")
    suspend fun getAllHistoryOnce(): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryEntity)

    @Delete
    suspend fun delete(item: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int
}
