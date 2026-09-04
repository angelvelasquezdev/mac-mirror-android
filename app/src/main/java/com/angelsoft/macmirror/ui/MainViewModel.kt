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

    val isConnected: StateFlow<Boolean> = combine(
        isPaired,
        discoveredServerUrl
    ) { paired, discovered ->
        paired && discovered != null
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
                if (paired && discovered != null) {
                    if (discovered != saved) {
                        preferencesManager.updateServerUrl(discovered)
                    }
                    checkServerStatus(discovered)
                }
            }.collect {}
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
            _pairingState.value = PairingState.Error("No macOS device discovered on local network.")
            return
        }

        if (pin.length != 6) {
            _pairingState.value = PairingState.Error("PIN must be 6 digits.")
            return
        }

        _pairingState.value = PairingState.Loading

        viewModelScope.launch {
            try {
                val result = performPairingHandshake(targetUrl, pin)
                if (result) {
                    _pairingState.value = PairingState.Success
                } else {
                    _pairingState.value = PairingState.Error("Pairing confirmation failed. Verify the PIN.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pairing error", e)
                _pairingState.value = PairingState.Error("Connection error: ${e.localizedMessage}")
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
                        }
                    }
                } else if (response.code == 401) {
                    Log.i(TAG, "Server responded with 401. Auto-clearing local pairing.")
                    preferencesManager.clearPairing()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not check server status at $serverUrl", e)
        }
    }

    fun resetPairingState() {
        _pairingState.value = PairingState.Idle
    }

    fun sendTestNotification(context: android.content.Context) {
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "test_channel",
                "Test Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Used to test MacMirror end-to-end connectivity"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = androidx.core.app.NotificationCompat.Builder(context, "test_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Test Notification")
            .setContentText("Hello from MacMirror! End-to-end integration test succeeded.")
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
