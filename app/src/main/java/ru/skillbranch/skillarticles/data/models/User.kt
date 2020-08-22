package ru.skillbranch.skillarticles.data.models

//@JsonClass(generateAdapter = true) // look at video (lecture 11, time code 01:57:00)
data class User(
    val id: String,
    val name: String,
    val avatar: String?,
    val rating: Int = 0,
    val respect: Int = 0,
    val about: String = ""
)


