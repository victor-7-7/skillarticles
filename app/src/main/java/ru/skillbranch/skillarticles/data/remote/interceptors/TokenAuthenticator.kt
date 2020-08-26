package ru.skillbranch.skillarticles.data.remote.interceptors

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okio.IOException
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.remote.NetworkManager
import ru.skillbranch.skillarticles.data.remote.req.RefreshReq

// https://medium.com/knowing-android/headers-interceptors-and-authenticators-with-retrofit-1a00fed0d5eb
// http://sangsoonam.github.io/2019/03/06/okhttp-how-to-refresh-access-token-efficiently.html#:~:text=OkHttp%20supports%20token-based%20authentication,is%20an%20HTTP%20unauthorized%20error.
// https://medium.com/@sandeeptengale/problem-solved-2-access-token-refresh-with-okhttp-authenticator-5ccb798ede70

// https://www.lordcodes.com/articles/authorization-of-web-requests-for-okhttp-and-retrofit
class TokenAuthenticator : Authenticator {
    private val network = NetworkManager.api

    private val Response.retryCount: Int
        get() {
            var currentResponse = priorResponse
            var result = 0
            while (currentResponse != null) {
                result++
                currentResponse = currentResponse.priorResponse
            }
            return result
        }

    // Authenticator is only called when there is an HTTP unauthorized error,
    // so you don’t need to check response code
    /**
     * Authenticator for when the authToken need to be refresh and updated
     * every time we get a 401 error code
     */
    @Throws(IOException::class)
    override fun authenticate(route: Route?, response: Response): Request? {
        /*var requestAvailable: Request? = null
        try {
            requestAvailable = response.request.newBuilder()
                .addHeader("AUTH_TOKEN", PrefManager.refreshToken)
                .build()
            return requestAvailable
        } catch (ex: Exception) { }
        return requestAvailable*/

        if (response.retryCount > 2) return null

        val originReq = response.request
        runBlocking {
            val auth = network.refresh(
                RefreshReq(
                    PrefManager.refreshToken,
                    PrefManager.accessToken
                )
            )
            PrefManager.accessToken = "Bearer ${auth.accessToken}"
            PrefManager.refreshToken = auth.refreshToken
        }
        val request = originReq.newBuilder()
            .addHeader("Authorization", PrefManager.accessToken)
            .build()

        return request
    }
}