package com.nguyendevs.stkh.manager

import android.content.Context
import android.os.Environment
import android.widget.EditText
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nguyendevs.stkh.adapter.HistoryAdapter
import com.nguyendevs.stkh.model.HistoryItem
import com.nguyendevs.stkh.repository.IHistoryRepository
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
 * HistoryManager - Nghiệp vụ quản lý lịch sử và điều phối UI.
 *
 * SOLID:
 * - SRP: Chỉ lo orchestrate history-related UI actions
 * - DIP: Phụ thuộc vào IHistoryRepository, ITextToSpeech, ITranslator (interfaces), không phụ thuộc Room/TTS cụ thể
 * - ISP: Nhận đúng interface mà nó cần, không nhận toàn bộ AppDatabase
 */
class HistoryManager(
    private val context: Context,
    private val repository: IHistoryRepository,        // DIP
    private val txtResult: EditText,
    private val drawerLayout: DrawerLayout,
    val historyAdapter: HistoryAdapter,
    private val ttsManager: ITextToSpeech,             // DIP
    private val translator: ITranslator,               // DIP
    private val permissionManager: PermissionManager,
    private val lifecycleScope: CoroutineScope
) {

    private var editingLang = "unknown"

    // Callbacks để update UI từ MainActivity → HistoryManager không import MainActivity
    var onHistoryCountChanged: ((count: Int) -> Unit)? = null
    var onSaveButtonShow: ((visible: Boolean) -> Unit)? = null
    var onTtsControlsShow: ((visible: Boolean) -> Unit)? = null
    var onCopyRequest: ((text: String) -> Unit)? = null
    var onShowSnackbar: ((message: String) -> Unit)? = null

    /** Đăng ký collect Room Flow → tự động update adapter */
    fun observeHistory() {
        lifecycleScope.launch {
            repository.observeAll().collect { items ->
                historyAdapter.submitList(items)
                onHistoryCountChanged?.invoke(items.size)
            }
        }
    }

    fun saveEditedContent() {
        translator.resetOriginalTextSnapshot()
        val content = txtResult.text.toString().trim()
        if (content.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                repository.insert(content, editingLang)
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

    // ============ Adapter callbacks ============

    fun onItemClick(item: HistoryItem) = showHistoryDetail(item)

    fun onMenuCopy(item: HistoryItem) {
        onCopyRequest?.invoke(item.content)
        onShowSnackbar?.invoke("Đã sao chép!")
    }

    fun onMenuView(item: HistoryItem) {
        translator.resetOriginalTextSnapshot()
        txtResult.setText(item.content)
        setReadOnly()
        onTtsControlsShow?.invoke(true)
        editingLang = item.lang
        ttsManager.setLanguage(item.lang)
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    fun onMenuEdit(item: HistoryItem) {
        translator.resetOriginalTextSnapshot()
        txtResult.setText(item.content)
        setEditable()
        onSaveButtonShow?.invoke(true)
        editingLang = item.lang
        ttsManager.setLanguage(item.lang)
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    fun onMenuDelete(item: HistoryItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            repository.delete(item)
            withContext(Dispatchers.Main) { onShowSnackbar?.invoke("Đã xóa mục lịch sử") }
        }
    }

    fun onMenuExport(item: HistoryItem) {
        permissionManager.checkWriteStoragePermission { showExportFormatDialog(item) }
    }

    fun clearAllHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            repository.clearAll()
            withContext(Dispatchers.Main) { onShowSnackbar?.invoke("Đã xóa toàn bộ lịch sử") }
        }
    }

    // ============ Private helpers ============

    private fun showHistoryDetail(item: HistoryItem) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val formattedDate = sdf.format(Date(item.date))
        val langDisplay = LANG_DISPLAY_MAP[item.lang] ?: item.lang
        val message = "${item.content}\n\n🌐 Ngôn ngữ: $langDisplay\n🕐 Lưu lúc: $formattedDate"

        MaterialAlertDialogBuilder(context)
            .setTitle("Chi tiết bản ghi")
            .setMessage(message)
            .setPositiveButton("Đóng") { d, _ -> d.dismiss() }
            .setNeutralButton("Đọc lại") { _, _ ->
                editingLang = item.lang
                ttsManager.setLanguage(item.lang)
                ttsManager.speakText(item.content)
            }
            .show()
    }

    private fun showExportFormatDialog(item: HistoryItem) {
        val formats = arrayOf("📄 Text (.txt)", "📝 Word (.docx)")
        MaterialAlertDialogBuilder(context)
            .setTitle("Xuất file lịch sử")
            .setItems(formats) { _, which ->
                val fileName = "History_${item.date}${if (which == 0) ".txt" else ".docx"}"
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                val file = File(dir, fileName)
                if (which == 0) exportAsTxt(file, item.content)
                else exportAsDocx(file, item.content)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun exportAsTxt(file: File, content: String) =
        runCatching { file.writeText(content, Charsets.UTF_8); onShowSnackbar?.invoke("Đã xuất: ${file.name}") }
            .onFailure { onShowSnackbar?.invoke("Lỗi xuất file: ${it.message}") }

    private fun exportAsDocx(file: File, content: String) {
        val document = XWPFDocument()
        try {
            document.createParagraph().createRun().setText(content)
            FileOutputStream(file).use { document.write(it) }
            onShowSnackbar?.invoke("Đã xuất: ${file.name}")
        } catch (e: IOException) {
            onShowSnackbar?.invoke("Lỗi xuất file: ${e.message}")
        } finally { runCatching { document.close() } }
    }

    private fun setReadOnly() {
        txtResult.apply {
            isFocusable = false; isFocusableInTouchMode = false
            isCursorVisible = false; isLongClickable = false; keyListener = null
        }
    }

    private fun setEditable() {
        txtResult.apply {
            isEnabled = true; isFocusable = true; isFocusableInTouchMode = true
            isCursorVisible = true; isLongClickable = true
            keyListener = android.text.method.TextKeyListener.getInstance()
            requestFocus()
        }
    }

    companion object {
        private val LANG_DISPLAY_MAP = mapOf(
            "Vietnamese" to "Tiếng Việt", "English" to "Tiếng Anh",
            "Japanese" to "Tiếng Nhật", "Russian" to "Tiếng Nga",
            "French" to "Tiếng Pháp", "Spanish" to "Tiếng Tây Ban Nha",
            "German" to "Tiếng Đức", "Chinese" to "Tiếng Trung",
            "Korean" to "Tiếng Hàn", "Italian" to "Tiếng Ý"
        )
    }
}
