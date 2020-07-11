package ru.skillbranch.skillarticles.extensions.data

import ru.skillbranch.skillarticles.data.local.entities.ArticlePersonalInfo
import ru.skillbranch.skillarticles.data.models.AppSettings
import ru.skillbranch.skillarticles.viewmodels.article.ArticleState

fun ArticleState.toAppSettings() = AppSettings(isDarkMode, isBigText)

fun ArticleState.toArticlePersonalInfo(articleId: String) =
    ArticlePersonalInfo(articleId, isLike, isBookmark)

