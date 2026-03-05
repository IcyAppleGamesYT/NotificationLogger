package com.discordnotificationlogger.ui.logs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.discordnotificationlogger.R
import com.discordnotificationlogger.database.NotificationEntity
import java.text.SimpleDateFormat
import java.util.*

sealed class LogListItem {
    data class Header(
        val title: String,
        val subtitle: String,
        val isKeywordSection: Boolean
    ) : LogListItem()

    data class NotificationItem(val entity: NotificationEntity) : LogListItem()

    object EmptyKeyword : LogListItem()
    object Divider : LogListItem()
}

class NotificationAdapter(
    private val onDelete: (NotificationEntity) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_NOTIFICATION = 1
        const val VIEW_TYPE_EMPTY = 2
        const val VIEW_TYPE_DIVIDER = 3
    }

    private var items: List<LogListItem> = emptyList()

    fun submitItems(newItems: List<LogListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is LogListItem.Header -> VIEW_TYPE_HEADER
            is LogListItem.NotificationItem -> VIEW_TYPE_NOTIFICATION
            is LogListItem.EmptyKeyword -> VIEW_TYPE_EMPTY
            is LogListItem.Divider -> VIEW_TYPE_DIVIDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_log_header, parent, false)
            )
            VIEW_TYPE_NOTIFICATION -> NotificationViewHolder(
                inflater.inflate(R.layout.item_notification, parent, false)
            )
            VIEW_TYPE_EMPTY -> EmptyViewHolder(
                inflater.inflate(R.layout.item_log_empty, parent, false)
            )
            else -> DividerViewHolder(
                inflater.inflate(R.layout.item_log_divider, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is LogListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is LogListItem.NotificationItem -> (holder as NotificationViewHolder).bind(item.entity, onDelete)
            is LogListItem.EmptyKeyword -> { /* static view */ }
            is LogListItem.Divider -> { /* static view */ }
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_section_title)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tv_section_subtitle)

        fun bind(header: LogListItem.Header) {
            tvTitle.text = header.title
            tvSubtitle.text = header.subtitle
            tvSubtitle.visibility = if (header.subtitle.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAppName: TextView = itemView.findViewById(R.id.tv_app_name)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_notif_title)
        private val tvText: TextView = itemView.findViewById(R.id.tv_notif_text)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_notif_time)
        private val tvKeywords: TextView = itemView.findViewById(R.id.tv_keywords)
        private val cardView: CardView = itemView.findViewById(R.id.card_notification)

        private val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

        fun bind(entity: NotificationEntity, onDelete: (NotificationEntity) -> Unit) {
            tvAppName.text = entity.appName
            tvTitle.text = entity.title.ifEmpty { "(no title)" }
            tvText.text = entity.text
            tvTime.text = sdf.format(Date(entity.timestamp))

            if (entity.isKeywordMatch && entity.matchedKeywords.isNotEmpty()) {
                tvKeywords.visibility = View.VISIBLE
                tvKeywords.text = "🏷 ${entity.matchedKeywords}"
            } else {
                tvKeywords.visibility = View.GONE
            }

            // Highlight keyword matches with slightly different background
            if (entity.isKeywordMatch) {
                cardView.setCardBackgroundColor(
                    itemView.context.getColor(R.color.discord_surface_highlight)
                )
            } else {
                cardView.setCardBackgroundColor(
                    itemView.context.getColor(R.color.discord_surface)
                )
            }

            cardView.setOnLongClickListener {
                onDelete(entity)
                true
            }
        }
    }

    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
