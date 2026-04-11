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
            override fun areItemsTheSame(old: HistoryItem, new: HistoryItem) = old.id == new.id
            override fun areContentsTheSame(old: HistoryItem, new: HistoryItem) = old == new
        }
        private val DATE_FORMAT = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    }

    inner class ViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryItem) {
            binding.historyText.text = if (item.content.length > 60)
                item.content.substring(0, 60) + "…" else item.content

            binding.tvHistoryLang.text = when (item.lang) {
                "Vietnamese" -> "vi"; "English" -> "en"; "Japanese" -> "ja"
                "Russian" -> "ru"; "French" -> "fr"; "Spanish" -> "es"
                "German" -> "de"; "Chinese" -> "zh"; "Korean" -> "ko"; "Italian" -> "it"
                else -> item.lang.take(2).lowercase()
            }

            binding.tvHistoryDate.text = DATE_FORMAT.format(Date(item.date))
            binding.root.setOnClickListener { onItemClick(item) }

            binding.btnHistoryMenu.setOnClickListener { view ->
                PopupMenu(view.context, view).apply {
                    menu.apply {
                        add(0, 1, 0, view.context.getString(R.string.history_copy))
                        add(0, 2, 0, view.context.getString(R.string.history_view))
                        add(0, 3, 0, view.context.getString(R.string.history_edit))
                        add(0, 4, 0, view.context.getString(R.string.history_delete))
                        add(0, 5, 0, view.context.getString(R.string.history_export))
                    }
                    setOnMenuItemClickListener {
                        when (it.itemId) {
                            1 -> { onMenuCopy(item); true }
                            2 -> { onMenuView(item); true }
                            3 -> { onMenuEdit(item); true }
                            4 -> { onMenuDelete(item); true }
                            5 -> { onMenuExport(item); true }
                            else -> false
                        }
                    }
                    show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}
