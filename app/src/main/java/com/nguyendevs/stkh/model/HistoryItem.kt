package com.nguyendevs.stkh.model

import com.nguyendevs.stkh.database.HistoryEntity

/**
 * UI model đại diện cho một mục lịch sử.
 * Tách biệt Room Entity và UI model để giữ clean architecture.
 */
data class HistoryItem(
    val id: Int = 0,
    val content: String,
    val date: Long,        // epoch milliseconds
    val lang: String
) {
    companion object {
        /** Map từ Room Entity sang UI model */
        fun fromEntity(entity: HistoryEntity) = HistoryItem(
            id = entity.id,
            content = entity.content,
            date = entity.date,
            lang = entity.lang
        )
    }

    /** Map từ UI model sang Room Entity để lưu DB */
    fun toEntity() = HistoryEntity(
        id = id,
        content = content,
        date = date,
        lang = lang
    )
}
