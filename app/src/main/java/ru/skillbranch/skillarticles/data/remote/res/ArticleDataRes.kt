package ru.skillbranch.skillarticles.data.remote.res

import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
data class ArticleDataRes(
    val id: String,
    val date: Date,
    val author: AuthorRes,
    val title: String,
    val description: String,
    val poster: String,
    val category: CategoryRes,
    val tags: List<String> = listOf()
)