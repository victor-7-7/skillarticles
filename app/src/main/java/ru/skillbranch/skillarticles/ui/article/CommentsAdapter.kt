package ru.skillbranch.skillarticles.ui.article

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import ru.skillbranch.skillarticles.data.remote.res.CommentRes
import ru.skillbranch.skillarticles.ui.custom.CommentItemView
import javax.inject.Inject


class CommentsAdapter @Inject constructor(
    private val listener: IArticleView
) : PagingDataAdapter<CommentRes, CommentVH>(CommentsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentVH {
        val containerView = CommentItemView(parent.context)
        return CommentVH(containerView, listener::clickOnComment)
    }

    override fun onBindViewHolder(holder: CommentVH, position: Int) {
        holder.bind(getItem(position))
    }
}

//============================================================================

class CommentVH(
    override val containerView: View,
    val listener: (CommentRes) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: CommentRes?) {
        (containerView as CommentItemView).bind(item)
        // Мы разрешили плейсхолдеры, поэтому item может быть null
        if (item != null) itemView.setOnClickListener { listener(item) }
    }
}

//============================================================================

class CommentsDiffCallback : DiffUtil.ItemCallback<CommentRes>() {
    override fun areItemsTheSame(
        oldItem: CommentRes,
        newItem: CommentRes
    ) = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: CommentRes,
        newItem: CommentRes
    ) = oldItem == newItem
}

