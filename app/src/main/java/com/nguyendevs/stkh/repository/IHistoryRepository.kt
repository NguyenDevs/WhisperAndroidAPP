package com.nguyendevs.stkh.repository

import com.nguyendevs.stkh.model.HistoryItem
import kotlinx.coroutines.flow.Flow

/**
 * IHistoryRepository - Interface tách biệt lớp data khỏi logic nghiệp vụ.
 *
 * SOLID:
 * - SRP: Chỉ định nghĩa contract của data layer, không có UI/business logic
 * - OCP: Dễ mở rộng (thêm RemoteHistoryRepository) mà không sửa consumers
 * - DIP: Các Manager phụ thuộc vào abstraction (interface) thay vì Room cụ thể
 * - ISP: Interface nhỏ gọn, chỉ chứa những gì consumer cần
 */
interface IHistoryRepository {
    /** Stream tự động cập nhật khi DB thay đổi */
    fun observeAll(): Flow<List<HistoryItem>>
    /** Lưu mục mới */
    suspend fun insert(content: String, lang: String)
    /** Xóa một mục */
    suspend fun delete(item: HistoryItem)
    /** Xóa tất cả */
    suspend fun clearAll()
}
