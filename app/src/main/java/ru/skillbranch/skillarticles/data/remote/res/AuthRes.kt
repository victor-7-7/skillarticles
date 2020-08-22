package ru.skillbranch.skillarticles.data.remote.res

import com.squareup.moshi.JsonClass
import ru.skillbranch.skillarticles.data.models.User

@JsonClass(generateAdapter = true)
data class AuthRes(
    val user: User,
    val refreshToken: String,
    val accessToken: String
)