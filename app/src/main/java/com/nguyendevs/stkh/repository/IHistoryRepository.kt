package com.nguyendevs.stkh.repository

import com.nguyendevs.stkh.model.HistoryItem
import kotlinx.coroutines.flow.Flow

interface IHistoryRepository {
    fun observeAll(): Flow<List<HistoryItem>>
    suspend fun insert(content: String, lang: String)
    suspend fun delete(item: HistoryItem)
    suspend fun clearAll()
}
