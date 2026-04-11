package com.nguyendevs.stkh.manager

import android.content.Context
import android.os.Environment
import android.widget.EditText
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nguyendevs.stkh.adapter.HistoryAdapter
import com.nguyendevs.stkh.model.HistoryItem
import com.nguyendevs.stkh.repository.IHistoryRepository
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

class HistoryManager(
    private val context: Context,
    private val repository: IHistoryRepository,
    private val txtResult: EditText,
    private val drawerLayout: DrawerLayout,
    val historyAdapter: HistoryAdapter,
    private val ttsManager: ITextToSpeech,
    private val translator: ITranslator,
    private val permissionManager: PermissionManager,
    private val lifecycleScope: CoroutineScope
) {
    private var editingLang = "unknown"

    var onHistoryCountChanged: ((Int) -> Unit)? = null
    var onSaveButtonShow: ((Boolean) -> Unit)? = null
    var onTtsControlsShow: ((Boolean) -> Unit)? = null
    var onCopyRequest: ((String) -> Unit)? = null
    var onShowSnackbar: ((String) -> Unit)? = null

    /** Bắt đầu collect Room Flow để tự động cập nhật adapter. */
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

    fun onItemClick(item: HistoryItem) = showDetail(item)

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

    fun onMenuExport(item: HistoryItem) =
        permissionManager.checkWriteStoragePermission { showExportDialog(item) }

    fun clearAllHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            repository.clearAll()
            withContext(Dispatchers.Main) { onShowSnackbar?.invoke("Đã xóa toàn bộ lịch sử") }
        }
    }

    private fun showDetail(item: HistoryItem) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val lang = LANG_MAP[item.lang] ?: item.lang
        MaterialAlertDialogBuilder(context)
            .setTitle("Chi tiết bản ghi")
            .setMessage("${item.content}\n\n🌐 Ngôn ngữ: $lang\n🕐 Lưu lúc: ${sdf.format(Date(item.date))}")
            .setPositiveButton("Đóng") { d, _ -> d.dismiss() }
            .setNeutralButton("Đọc lại") { _, _ ->
                editingLang = item.lang
                ttsManager.setLanguage(item.lang)
                ttsManager.speakText(item.content)
            }
            .show()
    }

    private fun showExportDialog(item: HistoryItem) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Xuất file lịch sử")
            .setItems(arrayOf("📄 Text (.txt)", "📝 Word (.docx)")) { _, which ->
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                val file = File(dir, "History_${item.date}${if (which == 0) ".txt" else ".docx"}")
                if (which == 0) exportTxt(file, item.content) else exportDocx(file, item.content)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun exportTxt(file: File, content: String) =
        runCatching { file.writeText(content, Charsets.UTF_8); onShowSnackbar?.invoke("Đã xuất: ${file.name}") }
            .onFailure { onShowSnackbar?.invoke("Lỗi: ${it.message}") }

    private fun exportDocx(file: File, content: String) {
        val doc = XWPFDocument()
        try {
            doc.createParagraph().createRun().setText(content)
            FileOutputStream(file).use { doc.write(it) }
            onShowSnackbar?.invoke("Đã xuất: ${file.name}")
        } catch (e: IOException) {
            onShowSnackbar?.invoke("Lỗi: ${e.message}")
        } finally { runCatching { doc.close() } }
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
        private val LANG_MAP = mapOf(
            "Vietnamese" to "Tiếng Việt", "English" to "Tiếng Anh",
            "Japanese" to "Tiếng Nhật", "Russian" to "Tiếng Nga",
            "French" to "Tiếng Pháp", "Spanish" to "Tiếng Tây Ban Nha",
            "German" to "Tiếng Đức", "Chinese" to "Tiếng Trung",
            "Korean" to "Tiếng Hàn", "Italian" to "Tiếng Ý"
        )
    }
}
