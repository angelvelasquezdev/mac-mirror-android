package com.angelsoft.macmirror.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

interface NetworkMonitor {
    val isWifiConnected: StateFlow<Boolean>
    fun isWifiConnectedSync(): Boolean
}

class LiveNetworkMonitor(
    private val context: Context
) : NetworkMonitor {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val connectedWifiNetworks = Collections.synchronizedSet(mutableSetOf<Network>())

    private val _isWifiConnected = MutableStateFlow(isWifiConnectedSync())
    override val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            connectedWifiNetworks.add(network)
            _isWifiConnected.value = true
        }

        override fun onLost(network: Network) {
            connectedWifiNetworks.remove(network)
            _isWifiConnected.value = connectedWifiNetworks.isNotEmpty() || isWifiConnectedSync()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            if (hasWifi) {
                connectedWifiNetworks.add(network)
            } else {
                connectedWifiNetworks.remove(network)
            }
            _isWifiConnected.value = connectedWifiNetworks.isNotEmpty() || isWifiConnectedSync()
        }
    }

    init {
        registerCallback()
    }

    private fun registerCallback() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()
        try {
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            android.util.Log.w("LiveNetworkMonitor", "Could not register network callback: ${e.message}")
        }
    }

    override fun isWifiConnectedSync(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
