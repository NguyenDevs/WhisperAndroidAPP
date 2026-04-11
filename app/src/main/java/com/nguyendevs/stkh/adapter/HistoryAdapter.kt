package com.nguyendevs.stkh.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.stkh.R
import com.nguyendevs.stkh.databinding.ItemHistoryBinding
import com.nguyendevs.stkh.model.HistoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HistoryAdapter - RecyclerView.Adapter cho danh sách lịch sử.
 * Migration từ ArrayAdapter + ListView sang ListAdapter + RecyclerView.
 * Dùng ListAdapter để hỗ trợ DiffUtil tự động animate changes.
 */
class HistoryAdapter(
    private val onItemClick: (HistoryItem) -> Unit,
    private val onMenuCopy: (HistoryItem) -> Unit,
    private val onMenuView: (HistoryItem) -> Unit,
    private val onMenuEdit: (HistoryItem) -> Unit,
    private val onMenuDelete: (HistoryItem) -> Unit,
    private val onMenuExport: (HistoryItem) -> Unit
) : ListAdapter<HistoryItem, HistoryAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HistoryItem>() {
            override fun areItemsTheSame(old: HistoryItem, new: HistoryItem) =
                old.id == new.id   // Room auto-generated primary key

            override fun areContentsTheSame(old: HistoryItem, new: HistoryItem) =
                old == new
        }

        private val DATE_FORMAT = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    }

    inner class ViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryItem) {
            // Preview text (max 60 chars)
            binding.historyText.text = if (item.content.length > 60)
                item.content.substring(0, 60) + "…"
            else item.content

            // Language badge
            val langShort = when (item.lang) {
                "Vietnamese" -> "vi"
                "English" -> "en"
                "Japanese" -> "ja"
                "Russian" -> "ru"
                "French" -> "fr"
                "Spanish" -> "es"
                "German" -> "de"
                "Chinese" -> "zh"
                "Korean" -> "ko"
                "Italian" -> "it"
                else -> item.lang.take(2).lowercase()
            }
            binding.tvHistoryLang.text = langShort

            // Date formatted from Long epoch ms
            binding.tvHistoryDate.text = DATE_FORMAT.format(Date(item.date))

            // Item click
            binding.root.setOnClickListener { onItemClick(item) }

            // 3-dot menu
            binding.btnHistoryMenu.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.apply {
                    add(0, 1, 0, view.context.getString(R.string.history_copy))
                    add(0, 2, 0, view.context.getString(R.string.history_view))
                    add(0, 3, 0, view.context.getString(R.string.history_edit))
                    add(0, 4, 0, view.context.getString(R.string.history_delete))
                    add(0, 5, 0, view.context.getString(R.string.history_export))
                }
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> { onMenuCopy(item); true }
                        2 -> { onMenuView(item); true }
                        3 -> { onMenuEdit(item); true }
                        4 -> { onMenuDelete(item); true }
                        5 -> { onMenuExport(item); true }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
