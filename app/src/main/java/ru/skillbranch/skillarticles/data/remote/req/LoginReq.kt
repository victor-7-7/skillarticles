package ru.skillbranch.skillarticles.data.remote.req

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginReq(
    val login: String,
    val password: String
)