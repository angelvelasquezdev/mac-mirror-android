package com.angelsoft.macmirror.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NsdHelper(context: Context) {

    companion object {
        private const val TAG = "NsdHelper"
        private const val SERVICE_TYPE = "_macmirror._tcp."
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val _resolvedServerUrl = MutableStateFlow<String?>(null)
    val resolvedServerUrl: StateFlow<String?> = _resolvedServerUrl

    fun startDiscovery() {
        if (discoveryListener != null) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "Service discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service found: $serviceInfo")
                if (serviceInfo?.serviceType == SERVICE_TYPE || serviceInfo?.serviceType?.startsWith(SERVICE_TYPE) == true) {
                    resolveService(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service lost: $serviceInfo")
                _resolvedServerUrl.value = null
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service discovery", e)
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping discovery", e)
            }
            discoveryListener = null
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service resolved: $resolvedInfo")
                if (resolvedInfo != null) {
                    val host = resolvedInfo.host.hostAddress
                    val port = resolvedInfo.port
                    val url = "http://$host:$port"
                    Log.d(TAG, "Resolved macOS Server: $url")
                    _resolvedServerUrl.value = url
                }
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving service", e)
        }
    }
}
