package com.nguyendevs.stkh.model

import com.nguyendevs.stkh.database.HistoryEntity

data class HistoryItem(
    val id: Int = 0,
    val content: String,
    val date: Long,
    val lang: String
) {
    companion object {
        fun fromEntity(entity: HistoryEntity) = HistoryItem(
            id = entity.id,
            content = entity.content,
            date = entity.date,
            lang = entity.lang
        )
    }

    fun toEntity() = HistoryEntity(id = id, content = content, date = date, lang = lang)
}
