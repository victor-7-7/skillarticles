package ru.skillbranch.skillarticles.data.remote.res

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthorRes(
    val id: String,
    val name: String,
    val avatar: String
)

