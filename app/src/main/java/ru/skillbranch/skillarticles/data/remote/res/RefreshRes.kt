package ru.skillbranch.skillarticles.data.remote.res


data class RefreshRes(
    // OAuth is a common system to use, relying on access tokens to protect our endpoints
    // and refresh tokens to obtain new access tokens once they have expired. The idea
    // is that the access token is added as an Authorization HTTP header on requests
    // to let the API know we have access to a particular resource.
    val refreshToken: String,
    val accessToken: String
)