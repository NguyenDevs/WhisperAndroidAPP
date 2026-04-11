package com.nguyendevs.stkh.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val content: String,
    val date: Long = System.currentTimeMillis(),
    val lang: String
)
