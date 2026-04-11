package com.nguyendevs.stkh.repository

import com.nguyendevs.stkh.database.AppDatabase
import com.nguyendevs.stkh.database.HistoryEntity
import com.nguyendevs.stkh.model.HistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * RoomHistoryRepository - Implementation của IHistoryRepository dùng Room.
 *
 * SOLID:
 * - SRP: Chỉ lo việc CRUD với Room, không biết gì về UI hay TTS
 * - OCP: Nếu cần đổi sang DataStore/Remote API, chỉ tạo class mới implement interface
 * - LSP: Thay thế hoàn toàn cho IHistoryRepository mà không phá vỡ logic
 */
class RoomHistoryRepository(db: AppDatabase) : IHistoryRepository {

    private val dao = db.historyDao()

    override fun observeAll(): Flow<List<HistoryItem>> =
        dao.getAllHistory().map { entities ->
            entities.map { HistoryItem.fromEntity(it) }
        }

    override suspend fun insert(content: String, lang: String) {
        dao.insert(
            HistoryEntity(
                content = content,
                date = System.currentTimeMillis(),
                lang = lang
            )
        )
    }

    override suspend fun delete(item: HistoryItem) {
        dao.delete(item.toEntity())
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}
