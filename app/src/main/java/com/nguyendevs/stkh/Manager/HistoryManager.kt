package com.nguyendevs.stkh.manager

import android.content.Context
import android.os.Environment
import android.widget.EditText
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nguyendevs.stkh.adapter.HistoryAdapter
import com.nguyendevs.stkh.database.AppDatabase
import com.nguyendevs.stkh.database.HistoryEntity
import com.nguyendevs.stkh.model.HistoryItem
import com.nguyendevs.stkh.util.showToast
import com.nguyendevs.stkh.util.showToastLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HistoryManager - Quản lý lịch sử với Room + Coroutines.
 * - Không còn raw SQLite: sử dụng db.historyDao()
 * - Flow từ Room → tự động cập nhật RecyclerView
 * - Tất cả DB operations chạy trên Dispatchers.IO
 */
class HistoryManager(
    private val context: Context,
    private val db: AppDatabase,
    private val txtResult: EditText,
    private val drawerLayout: DrawerLayout,
    val historyAdapter: HistoryAdapter,
    private val textToSpeechManager: TextToSpeechManager,
    private val translateManager: TranslateManager,
    private val permissionManager: PermissionManager,
    private val lifecycleScope: CoroutineScope
) {

    private var editingLang = "unknown"

    // Callbacks ra ngoài MainActivity để update UI
    var onHistoryCountChanged: ((count: Int) -> Unit)? = null
    var onSaveButtonShow: ((visible: Boolean) -> Unit)? = null
    var onTtsControlsShow: ((visible: Boolean) -> Unit)? = null
    var onCopyRequest: ((text: String) -> Unit)? = null
    var onShowSnackbar: ((message: String) -> Unit)? = null

    /** Gọi lần đầu để đăng ký Flow lắng nghe tự động từ Room */
    fun observeHistory() {
        lifecycleScope.launch {
            db.historyDao().getAllHistory().collect { entities ->
                val items = entities.map { HistoryItem.fromEntity(it) }
                historyAdapter.submitList(items)
                onHistoryCountChanged?.invoke(items.size)
            }
        }
    }

    /** Lưu nội dung đã chỉnh sửa */
    fun saveEditedContent() {
        translateManager.resetOriginalTextSnapshot()
        val editedContent = txtResult.text.toString().trim()
        if (editedContent.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                db.historyDao().insert(
                    HistoryEntity(content = editedContent, lang = editingLang)
                )
                withContext(Dispatchers.Main) {
                    onShowSnackbar?.invoke("Đã lưu nội dung!")
                    onTtsControlsShow?.invoke(true)
                }
            }
        } else {
            onShowSnackbar?.invoke("Nội dung trống, không thể lưu!")
        }
        setReadOnly()
        onSaveButtonShow?.invoke(false)
    }

    // ============ Callbacks từ Adapter ============

    fun onItemClick(item: HistoryItem) {
        showHistoryDetail(item)
    }

    fun onMenuCopy(item: HistoryItem) {
        onCopyRequest?.invoke(item.content)
        onShowSnackbar?.invoke("Đã sao chép!")
    }

    fun onMenuView(item: HistoryItem) {
        translateManager.resetOriginalTextSnapshot()
        txtResult.setText(item.content)
        setReadOnly()
        onTtsControlsShow?.invoke(true)
        drawerLayout.closeDrawer(GravityCompat.START)
        editingLang = item.lang
        textToSpeechManager.setLanguage(item.lang)
    }

    fun onMenuEdit(item: HistoryItem) {
        translateManager.resetOriginalTextSnapshot()
        txtResult.setText(item.content)
        setEditable()
        onSaveButtonShow?.invoke(true)
        drawerLayout.closeDrawer(GravityCompat.START)
        editingLang = item.lang
        textToSpeechManager.setLanguage(item.lang)
    }

    fun onMenuDelete(item: HistoryItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.historyDao().delete(item.toEntity())
            withContext(Dispatchers.Main) {
                onShowSnackbar?.invoke("Đã xóa mục lịch sử")
            }
        }
    }

    fun onMenuExport(item: HistoryItem) {
        permissionManager.checkWriteStoragePermission {
            showExportFormatDialog(item)
        }
    }

    fun clearAllHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            db.historyDao().clearAll()
            withContext(Dispatchers.Main) {
                onShowSnackbar?.invoke("Đã xóa toàn bộ lịch sử")
            }
        }
    }

    // ============ Private helpers ============

    private fun showHistoryDetail(item: HistoryItem) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val formattedDate = sdf.format(Date(item.date))
        val langDisplay = when (item.lang) {
            "Vietnamese" -> "Tiếng Việt"
            "English" -> "Tiếng Anh"
            "Japanese" -> "Tiếng Nhật"
            "Russian" -> "Tiếng Nga"
            "French" -> "Tiếng Pháp"
            "Spanish" -> "Tiếng Tây Ban Nha"
            "German" -> "Tiếng Đức"
            "Chinese" -> "Tiếng Trung"
            "Korean" -> "Tiếng Hàn"
            "Italian" -> "Tiếng Ý"
            else -> item.lang
        }
        val message = "${item.content}\n\n🌐 Ngôn ngữ: $langDisplay\n🕐 Lưu lúc: $formattedDate"

        MaterialAlertDialogBuilder(context)
            .setTitle("Chi tiết bản ghi")
            .setMessage(message)
            .setPositiveButton("Đóng") { d, _ -> d.dismiss() }
            .setNeutralButton("Đọc lại") { _, _ ->
                editingLang = item.lang
                textToSpeechManager.setLanguage(item.lang)
                textToSpeechManager.speakText(item.content)
            }
            .show()
    }

    private fun showExportFormatDialog(item: HistoryItem) {
        val formats = arrayOf("📄 Text (.txt)", "📝 Word (.docx)")
        MaterialAlertDialogBuilder(context)
            .setTitle("Xuất file lịch sử")
            .setItems(formats) { _, which ->
                val ts = item.date.toString()
                val fileName = "History_$ts${if (which == 0) ".txt" else ".docx"}"
                val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                directory.mkdirs()
                val file = File(directory, fileName)
                if (which == 0) exportAsTxt(file, item.content)
                else exportAsDocx(file, item.content)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun exportAsTxt(file: File, content: String) {
        try {
            file.writeText(content, Charsets.UTF_8)
            onShowSnackbar?.invoke("Đã xuất: ${file.name}")
        } catch (e: Exception) {
            onShowSnackbar?.invoke("Lỗi xuất file: ${e.message}")
        }
    }

    private fun exportAsDocx(file: File, content: String) {
        val document = XWPFDocument()
        try {
            val paragraph = document.createParagraph()
            paragraph.createRun().setText(content)
            FileOutputStream(file).use { document.write(it) }
            onShowSnackbar?.invoke("Đã xuất: ${file.name}")
        } catch (e: IOException) {
            onShowSnackbar?.invoke("Lỗi xuất file: ${e.message}")
        } finally {
            runCatching { document.close() }
        }
    }

    private fun setReadOnly() {
        txtResult.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isCursorVisible = false
            isLongClickable = false
            keyListener = null
        }
    }

    private fun setEditable() {
        txtResult.apply {
            isEnabled = true
            isFocusable = true
            isFocusableInTouchMode = true
            isCursorVisible = true
            isLongClickable = true
            // Khôi phục key listener mặc định
            keyListener = android.text.method.TextKeyListener.getInstance()
            requestFocus()
        }
    }
}
