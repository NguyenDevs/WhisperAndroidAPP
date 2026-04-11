package com.nguyendevs.stkh.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO cho bảng lịch sử.
 * Tất cả suspend functions - an toàn để gọi từ coroutines.
 * Flow<List<HistoryEntity>> tự động cập nhật UI khi DB thay đổi.
 */
@Dao
interface HistoryDao {

    /**
     * Lấy toàn bộ lịch sử, mới nhất trước.
     * Flow tự notify RecyclerView khi có thay đổi.
     */
    @Query("SELECT * FROM history ORDER BY date DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    /**
     * Lấy lịch sử dạng List (một lần, không reactive) - dùng trong suspend.
     */
    @Query("SELECT * FROM history ORDER BY date DESC")
    suspend fun getAllHistoryOnce(): List<HistoryEntity>

    /**
     * Thêm mục mới. OnConflictStrategy.REPLACE để tránh duplicate.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryEntity)

    /**
     * Xóa một mục theo id.
     */
    @Delete
    suspend fun delete(item: HistoryEntity)

    /**
     * Xóa tất cả lịch sử.
     */
    @Query("DELETE FROM history")
    suspend fun clearAll()

    /**
     * Đếm số mục.
     */
    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int
}
