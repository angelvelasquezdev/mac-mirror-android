package com.angelsoft.macmirror.ui

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angelsoft.macmirror.data.PreferencesManager
import com.angelsoft.macmirror.network.NsdHelper
import com.angelsoft.macmirror.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainViewModel(
    private val preferencesManager: PreferencesManager,
    private val cryptoManager: CryptoManager,
    val nsdHelper: NsdHelper
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient = OkHttpClient()

    val isPaired: StateFlow<Boolean> = preferencesManager.isPairedFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val pairedDeviceName: StateFlow<String?> = preferencesManager.pairedDeviceNameFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val savedServerUrl: StateFlow<String?> = preferencesManager.serverUrlFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val discoveredServerUrl: StateFlow<String?> = nsdHelper.resolvedServerUrl

    private val _isServerReachable = MutableStateFlow(false)

    val isConnected: StateFlow<Boolean> = combine(
        isPaired,
        discoveredServerUrl,
        _isServerReachable
    ) { paired, discovered, reachable ->
        paired && reachable
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isBatteryOptimizationIgnored = MutableStateFlow(true)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored

    fun refreshBatteryOptimizationStatus(context: Context) {
        _isBatteryOptimizationIgnored.value = com.angelsoft.macmirror.util.PermissionUtils.isBatteryOptimizationIgnored(context)
    }

    fun requestIgnoreBatteryOptimization(context: Context) {
        com.angelsoft.macmirror.util.PermissionUtils.requestIgnoreBatteryOptimization(context)
    }

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState

    init {
        nsdHelper.startDiscovery()
        viewModelScope.launch {
            combine(isPaired, discoveredServerUrl, savedServerUrl) { paired, discovered, saved ->
                if (paired) {
                    val targetUrl = discovered ?: saved
                    if (targetUrl != null) {
                        if (discovered != null && discovered != saved) {
                            preferencesManager.updateServerUrl(discovered)
                        }
                        checkServerStatus(targetUrl)
                    } else {
                        _isServerReachable.value = false
                    }
                } else {
                    _isServerReachable.value = false
                }
            }.collect {}
        }

        // Periodic reachability heartbeat every 8 seconds when paired
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(8000)
                val paired = preferencesManager.isPairedFlow.first()
                if (paired) {
                    val targetUrl = discoveredServerUrl.value ?: savedServerUrl.value
                    if (targetUrl != null) {
                        checkServerStatus(targetUrl)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        nsdHelper.stopDiscovery()
    }

    fun startDiscovery() {
        nsdHelper.startDiscovery()
    }

    fun restartDiscovery() {
        nsdHelper.stopDiscovery()
        nsdHelper.startDiscovery()
    }

    fun pairDevice(pin: String, customUrl: String? = null) {
        val targetUrl = customUrl?.takeIf { it.isNotBlank() }?.let { raw ->
            val clean = raw.trim()
            if (clean.startsWith("http://") || clean.startsWith("https://")) clean
            else "http://$clean:50001"
        } ?: discoveredServerUrl.value

        if (targetUrl == null) {
            _pairingState.value = PairingState.Error("No macOS device discovered", com.angelsoft.macmirror.R.string.err_no_mac_discovered)
            return
        }

        if (pin.length != 6) {
            _pairingState.value = PairingState.Error("PIN must be 6 digits", com.angelsoft.macmirror.R.string.err_pin_must_be_6_digits)
            return
        }

        _pairingState.value = PairingState.Loading

        viewModelScope.launch {
            try {
                val result = performPairingHandshake(targetUrl, pin)
                if (result) {
                    _pairingState.value = PairingState.Success
                } else {
                    _pairingState.value = PairingState.Error("Pairing confirmation failed", com.angelsoft.macmirror.R.string.err_pairing_failed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pairing error", e)
                _pairingState.value = PairingState.Error(e.localizedMessage ?: "Connection error", com.angelsoft.macmirror.R.string.err_connection_error)
            }
        }
    }

    private suspend fun performPairingHandshake(serverUrl: String, pin: String): Boolean = withContext(Dispatchers.IO) {
        // 1. Generate Ephemeral KeyPair
        val keyPair = cryptoManager.generateEphemeralKeyPair()
        val publicKeyBytes = keyPair.public.encoded
        val publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)

        // 2. POST /pair/initiate
        val initiateJson = JSONObject().apply {
            put("client_ephemeral_pub_key", publicKeyBase64)
            put("device_name", android.os.Build.MODEL)
        }

        val request = Request.Builder()
            .url("$serverUrl/pair/initiate")
            .post(initiateJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "Initiate failed with code: ${response.code}")
            return@withContext false
        }

        val responseBody = response.body?.string() ?: return@withContext false
        val responseJson = JSONObject(responseBody)
        val serverPublicKeyBase64 = responseJson.getString("server_ephemeral_pub_key")
        val serverDeviceName = responseJson.optString("device_name", "macOS Device")

        val serverPublicKeyBytes = Base64.decode(serverPublicKeyBase64, Base64.NO_WRAP)

        // 3. Compute ECDH Shared Secret
        val sharedSecret = cryptoManager.computeSharedSecret(keyPair.private, serverPublicKeyBytes)

        // 4. Derive Key
        val sessionKey = cryptoManager.deriveSymmetricKey(sharedSecret, pin)

        // 5. Send /pair/confirm with verification payload (e.g., verifying client name encrypted)
        val verificationText = "MacMirrorVerify:${android.os.Build.MODEL}"
        val encryptedResult = cryptoManager.encryptPayload(verificationText, sessionKey)

        val confirmJson = JSONObject().apply {
            put("iv", encryptedResult.iv)
            put("ciphertext", encryptedResult.ciphertext)
            put("tag", encryptedResult.tag)
        }

        val confirmRequest = Request.Builder()
            .url("$serverUrl/pair/confirm")
            .post(confirmJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val confirmResponse = httpClient.newCall(confirmRequest).execute()
        if (confirmResponse.isSuccessful) {
            // Save Pairing Information
            val encryptedKeyStr = cryptoManager.encryptSessionKey(sessionKey)
            preferencesManager.savePairingInfo(
                deviceName = serverDeviceName,
                serverUrl = serverUrl,
                encryptedKeyBase64 = encryptedKeyStr
            )
            // Enable Persistent Service by default upon pairing (per user choice in /grill-me)
            preferencesManager.setKeepAlivePersistentService(true)
            _isServerReachable.value = true
            true
        } else {
            Log.e(TAG, "Confirmation failed: ${confirmResponse.code}")
            false
        }
    }

    fun unpair() {
        val serverUrl = savedServerUrl.value ?: discoveredServerUrl.value
        viewModelScope.launch {
            if (serverUrl != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val request = Request.Builder()
                            .url("$serverUrl/pair/unpair")
                            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                            .build()
                        httpClient.newCall(request).execute().close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send unpair request to macOS", e)
                    }
                }
            }
            preferencesManager.clearPairing()
            _isServerReachable.value = false
            _pairingState.value = PairingState.Idle
        }
    }

    private suspend fun checkServerStatus(serverUrl: String) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$serverUrl/status")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val serverPaired = json.optBoolean("paired", false)
                        if (!serverPaired) {
                            Log.i(TAG, "Server reports it is not paired. Auto-clearing local pairing.")
                            preferencesManager.clearPairing()
                            _isServerReachable.value = false
                        } else {
                            _isServerReachable.value = true
                        }
                    } else {
                        _isServerReachable.value = false
                    }
                } else {
                    _isServerReachable.value = false
                    if (response.code == 401) {
                        Log.i(TAG, "Server responded with 401. Auto-clearing local pairing.")
                        preferencesManager.clearPairing()
                    }
                }
            }
        } catch (e: Exception) {
            _isServerReachable.value = false
            Log.w(TAG, "Could not check server status at $serverUrl: ${e.message}")
        }
    }

    fun resetPairingState() {
        _pairingState.value = PairingState.Idle
    }

    fun sendTestNotification(context: android.content.Context) {
        // Ensure the notification listener service is bound and active
        com.angelsoft.macmirror.util.PermissionUtils.rebindNotificationListener(context)

        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        val channelName = context.getString(com.angelsoft.macmirror.R.string.test_notification_channel_name)
        val channelDesc = context.getString(com.angelsoft.macmirror.R.string.test_notification_channel_desc)
        val title = context.getString(com.angelsoft.macmirror.R.string.test_notification_title)
        val text = context.getString(com.angelsoft.macmirror.R.string.test_notification_text)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "test_channel",
                channelName,
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = androidx.core.app.NotificationCompat.Builder(context, "test_channel")
            .setSmallIcon(com.angelsoft.macmirror.R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .addExtras(android.os.Bundle().apply {
                putBoolean("is_test_notification", true)
            })
            .build()
            
        notificationManager.notify(999, notification)
    }

    sealed interface PairingState {
        object Idle : PairingState
        object Loading : PairingState
        object Success : PairingState
        data class Error(val message: String, val stringResId: Int? = null) : PairingState
    }
}
