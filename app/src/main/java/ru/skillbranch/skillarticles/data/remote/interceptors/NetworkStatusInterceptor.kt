package ru.skillbranch.skillarticles.data.remote.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import ru.skillbranch.skillarticles.data.remote.NetworkMonitor
import ru.skillbranch.skillarticles.data.remote.err.NoNetworkError

class NetworkStatusInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Если сети нет, то кидаем исключение
        if (!NetworkMonitor.isConnected) throw NoNetworkError()
        // Если мы в сети, то отдаем цепочку дальше
        return chain.proceed(chain.request())
    }
}