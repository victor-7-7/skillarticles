package ru.skillbranch.skillarticles.data.models

import java.util.*

data class ArticleItemData(
    val id: String,
    val date: Date = Date(),
    val author: String,
    val authorAvatar: String,
    val title: String,
    val description: String,
    val poster: String,
    val category: String,
    val categoryIcon: String, // ::signature is verified
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val readDuration: Int = 0,
    var isBookmark: Boolean = false
)