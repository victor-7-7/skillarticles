package ru.skillbranch.skillarticles.ui.articles

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import ru.skillbranch.skillarticles.data.models.ArticleItemData
import ru.skillbranch.skillarticles.ui.custom.ArticleItemView
import ru.skillbranch.skillarticles.ui.custom.CheckableImageView

class ArticlesAdapter(
    private val bookmarkListener: (ArticleItemData) -> Unit,
    private val listener: (ArticleItemData) -> Unit
) : PagedListAdapter<ArticleItemData, ArticleVH>(ArticleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleVH {
//        val containerView = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_article, parent, false)
        val containerView = ArticleItemView(parent.context)
        return ArticleVH(containerView)
    }

    override fun onBindViewHolder(holder: ArticleVH, position: Int) {
        holder.bind(getItem(position), listener, bookmarkListener)
    }
}

//============================================================================

class ArticleDiffCallback : DiffUtil.ItemCallback<ArticleItemData>() {
    override fun areItemsTheSame(oldItem: ArticleItemData, newItem: ArticleItemData) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ArticleItemData, newItem: ArticleItemData) =
        oldItem == newItem
}

//============================================================================

class ArticleVH(
    override val containerView: View
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(
        item: ArticleItemData?,
        listener: (ArticleItemData) -> Unit,
        bookmarkListener: (ArticleItemData) -> Unit
    ) {
        // if use placeholder item may be null
        if (item != null) {
            (containerView as ArticleItemView).bind(item)
            itemView.setOnClickListener { listener(item) }
            containerView.getBookmark().setOnClickListener { view ->
                bookmarkListener(item)
                (view as CheckableImageView).toggle()
            }
        }
    }
}

