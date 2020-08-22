package ru.skillbranch.skillarticles.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData

object NetworkMonitor {
    var isConnected = false
    val isConnectedLive = MutableLiveData(false)
    val networkTypeLive = MutableLiveData(NetworkType.NONE)
    private lateinit var cm: ConnectivityManager

    fun registerNetworkMonitor(ctx: Context) {
        cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        obtainNetworkType(cm.activeNetwork?.let {
            cm.getNetworkCapabilities(it)
        }).also {
            networkTypeLive.postValue(it)
        }
        cm.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    networkTypeLive.postValue(obtainNetworkType(capabilities))
                }

                override fun onLost(network: Network) {
                    isConnected = false
                    isConnectedLive.postValue(false)
                    networkTypeLive.postValue(NetworkType.NONE)
                }

                override fun onAvailable(network: Network) {
                    isConnected = true
                    isConnectedLive.postValue(true)
                }
            }
        )
    }

    private fun obtainNetworkType(capabilities: NetworkCapabilities?): NetworkType =
        when {
            capabilities == null -> NetworkType.NONE
            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) -> NetworkType.WIFI
            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ) -> NetworkType.CELLULAR
            else -> NetworkType.UNKNOWN
        }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    // Для тестов
    fun setNetworkIsConnected(isConnected: Boolean = true) {
        this.isConnected = isConnected
    }
}

// cellular - сотовая связь
enum class NetworkType {
    NONE, UNKNOWN, WIFI, CELLULAR
}


