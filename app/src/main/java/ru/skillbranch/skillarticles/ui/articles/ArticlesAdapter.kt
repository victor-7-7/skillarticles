package ru.skillbranch.skillarticles.ui.articles

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.ui.custom.ArticleItemView
import javax.inject.Inject


class ArticlesAdapter @Inject constructor(
    // Пример инъекции интерфейса через Hilt
    private val listener: IArticlesView
) : PagingDataAdapter<ArticleItem, ArticleVH>(ArticleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleVH {
        val containerView = ArticleItemView(parent.context)
        return ArticleVH(containerView)
    }

    override fun onBindViewHolder(holder: ArticleVH, position: Int) {
        holder.bind(getItem(position), listener::clickArticle)
    }
}

//============================================================================

class ArticleDiffCallback : DiffUtil.ItemCallback<ArticleItem>() {
    override fun areItemsTheSame(oldItem: ArticleItem, newItem: ArticleItem) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ArticleItem, newItem: ArticleItem) =
        oldItem == newItem
}

//============================================================================

class ArticleVH(
    override val containerView: View
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(
        item: ArticleItem?,
        listener: (ArticleItem, Boolean) -> Unit
    ) {
        // if use placeholder item may be null
        if (item != null) {
            (containerView as ArticleItemView).bind(item, listener)
        }
    }
}

