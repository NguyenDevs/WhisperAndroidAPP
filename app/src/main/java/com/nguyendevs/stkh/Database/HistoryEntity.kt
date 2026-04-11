package com.nguyendevs.stkh.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity cho bảng lịch sử nhận dạng giọng nói.
 * Thay thế SQLite thủ công: không còn cần CREATE TABLE SQL string.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val content: String,
    val date: Long = System.currentTimeMillis(),  // Lưu dạng Long (epoch ms) thay vì String
    val lang: String
)
