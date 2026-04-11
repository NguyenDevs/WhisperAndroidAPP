package com.nguyendevs.stkh.repository

import com.nguyendevs.stkh.database.AppDatabase
import com.nguyendevs.stkh.database.HistoryEntity
import com.nguyendevs.stkh.model.HistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomHistoryRepository(db: AppDatabase) : IHistoryRepository {

    private val dao = db.historyDao()

    override fun observeAll(): Flow<List<HistoryItem>> =
        dao.getAllHistory().map { it.map(HistoryItem::fromEntity) }

    override suspend fun insert(content: String, lang: String) =
        dao.insert(HistoryEntity(content = content, date = System.currentTimeMillis(), lang = lang))

    override suspend fun delete(item: HistoryItem) = dao.delete(item.toEntity())

    override suspend fun clearAll() = dao.clearAll()
}
