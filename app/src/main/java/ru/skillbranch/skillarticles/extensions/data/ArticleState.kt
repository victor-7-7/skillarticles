package ru.skillbranch.skillarticles.extensions.data

import ru.skillbranch.skillarticles.data.AppSettings
import ru.skillbranch.skillarticles.data.ArticlePersonalInfo
import ru.skillbranch.skillarticles.viewmodels.ArticleState

fun ArticleState.toAppSettings() = AppSettings(isDarkMode, isBigText)

fun ArticleState.toArticlePersonalInfo() =
    ArticlePersonalInfo(isLike, isBookmark)
