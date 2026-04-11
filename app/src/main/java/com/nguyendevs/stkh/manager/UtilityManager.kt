package com.nguyendevs.stkh.manager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.EditText
import com.nguyendevs.stkh.util.showToast

class UtilityManager(
    private val context: Context,
    private val txtResult: EditText
) {

    fun copyToClipboard(text: String) {
        if (text.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)
            context.showToast("Đã sao chép vào clipboard!")
        } else {
            context.showToast("Không có nội dung để sao chép!")
        }
    }

    fun shareText() {
        val text = txtResult.text.toString()
        if (text.isNotEmpty()) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"))
        } else {
            context.showToast("Không có nội dung để chia sẻ!")
        }
    }

    fun searchOnGoogle() {
        var query = txtResult.text.toString().trim()
        if (query.isNotEmpty()) {
            query = when {
                query.startsWith("http://") || query.startsWith("https://") -> query
                query.contains(".") -> "https://$query"
                else -> "https://www.google.com/search?q=${Uri.encode(query)}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(query))
            context.startActivity(intent)
        } else {
            context.showToast("Không có nội dung để tìm kiếm!")
        }
    }
}
