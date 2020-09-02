package ru.skillbranch.skillarticles.data.remote.interceptors

import android.util.Log
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.remote.NetworkManager
import ru.skillbranch.skillarticles.data.remote.req.RefreshReq


// https://medium.com/@sandeeptengale/problem-solved-2-access-token-refresh-with-okhttp-authenticator-5ccb798ede70
// https://www.lordcodes.com/articles/authorization-of-web-requests-for-okhttp-and-retrofit

class TokenAuthenticator : Authenticator {
    private val prefs = PrefManager
    private val network by lazy { NetworkManager.api }

    // Look at video (lecture 12 time code 00:45:54)
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != 401) return null

        val resp = network.refreshAccessToken(
            RefreshReq(prefs.refreshToken)
        ).execute()

        Log.d(
            "M_S_TokenAuthenticator", "authenticate: " +
                    "refresh access token response: $resp || response.body(): ${resp.body()}"
        )

        if (!resp.isSuccessful) return null
        // save new access/refresh token
        val newAccessToken = resp.body()!!.accessToken
        prefs.accessToken = "Bearer $newAccessToken"
        prefs.refreshToken = resp.body()!!.refreshToken

        Log.d(
            "M_S_TokenAuthenticator", "authenticate: " +
                    "origin request: ${response.request} || request.body: ${response.request.body}"
        )

        // retry request with new access token
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
/*
        return response.request.newBuilder()
            .addHeader("Authorization", prefs.accessToken) // <- old access token
            .build()
        */
    }
}