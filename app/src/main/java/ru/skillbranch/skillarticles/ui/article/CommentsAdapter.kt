package ru.skillbranch.skillarticles.ui.article

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import ru.skillbranch.skillarticles.data.models.CommentItemData
import ru.skillbranch.skillarticles.ui.custom.CommentItemView

class CommentsAdapter(private val listener: (CommentItemData) -> Unit) :
    PagedListAdapter<CommentItemData, CommentVH>(CommentsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentVH {
        val containerView = CommentItemView(parent.context)
        return CommentVH(containerView, listener)
    }

    override fun onBindViewHolder(holder: CommentVH, position: Int) {
        holder.bind(getItem(position))
    }
}

//============================================================================

class CommentVH(
    override val containerView: View,
    val listener: (CommentItemData) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: CommentItemData?) {
        (containerView as CommentItemView).bind(item)
        if (item != null) itemView.setOnClickListener { listener(item) }
    }
}

//============================================================================

class CommentsDiffCallback() : DiffUtil.ItemCallback<CommentItemData>() {
    override fun areItemsTheSame(
        oldItem: CommentItemData,
        newItem: CommentItemData
    ) = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: CommentItemData,
        newItem: CommentItemData
    ) = oldItem == newItem
}

