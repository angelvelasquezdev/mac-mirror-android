package com.angelsoft.macmirror.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.angelsoft.macmirror.data.PreferencesManager
import com.angelsoft.macmirror.security.CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationListener : NotificationListenerService(), KoinComponent {

    companion object {
        private const val TAG = "NotificationListener"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val preferencesManager: PreferencesManager by inject()
    private val cryptoManager: CryptoManager by inject()
    private val httpClient = OkHttpClient.Builder()
        .pingInterval(java.time.Duration.ofSeconds(30))
        .build()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocket: okhttp3.WebSocket? = null
    @Volatile
    private var isWebSocketConnected = false
    private var currentWsUrl: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationListener service created.")
        
        // Start watching pairing status, low latency mode, and server url changes
        serviceScope.launch {
            combine(
                preferencesManager.isPairedFlow,
                preferencesManager.lowLatencyModeFlow,
                preferencesManager.serverUrlFlow
            ) { paired, lowLatency, url ->
                Triple(paired, lowLatency, url)
            }.collect { (paired, lowLatency, url) ->
                if (paired && lowLatency && url != null) {
                    val wsUrl = getWebSocketUrl(url)
                    if (wsUrl != currentWsUrl || webSocket == null) {
                        connectWebSocket(wsUrl)
                    }
                } else {
                    disconnectWebSocket()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectWebSocket()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName

        // Skip our own app's notifications to prevent infinite loops, unless it's a test notification
        val isTest = sbn.notification?.extras?.getBoolean("is_test_notification", false) ?: false
        if (packageName == this.packageName && !isTest) return

        serviceScope.launch {
            try {
                // Check if device is paired
                val isPaired = preferencesManager.isPairedFlow.first()
                if (!isPaired) return@launch

                val serverUrl = preferencesManager.serverUrlFlow.first() ?: return@launch
                val encryptedKey = preferencesManager.encryptedSharedKeyFlow.first() ?: return@launch

                // Check opt-out preference
                val optOutApps = preferencesManager.optOutAppsFlow.first()
                if (optOutApps.contains(packageName)) {
                    Log.d(TAG, "Skipping opted-out notification from $packageName")
                    return@launch
                }

                // Extract notification text metadata
                val extras = sbn.notification.extras
                val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                
                // Fetch app name label
                val appLabel = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(packageName, 0)
                    ).toString()
                } catch (e: Exception) {
                    packageName
                }

                // Skip notification if both title and text are empty
                if (title.isEmpty() && text.isEmpty()) return@launch

                // Create plaintext JSON
                val payloadJson = JSONObject().apply {
                    put("id", sbn.key)
                    put("packageName", packageName)
                    put("appName", appLabel)
                    put("title", title)
                    put("text", text)
                    put("postTime", sbn.postTime)
                    
                    val appIconBase64 = getAppIconBase64(packageName)
                    put("appIcon", appIconBase64 ?: "")
                }

                // Decrypt session key and encrypt payload
                val sessionKey = cryptoManager.decryptSessionKey(encryptedKey)
                val encryptedResult = cryptoManager.encryptPayload(payloadJson.toString(), sessionKey)

                // Transmit payload to macOS server
                sendNotificationToServer(serverUrl, encryptedResult)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification posted", e)
            }
        }
    }

    private fun getAppIconBase64(packageName: String): String? {
        return try {
            val icon = packageManager.getApplicationIcon(packageName)
            val size = 96 // Max size to ensure fast transmission
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            icon.setBounds(0, 0, size, size)
            icon.draw(canvas)

            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            val bytes = outputStream.toByteArray()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun sendNotificationToServer(serverUrl: String, encrypted: CryptoManager.EncryptedResult) {
        val payloadJson = JSONObject().apply {
            put("iv", encrypted.iv)
            put("ciphertext", encrypted.ciphertext)
            put("tag", encrypted.tag)
            put("timestamp", System.currentTimeMillis() / 1000)
        }

        // Try to send via WebSocket if connected
        val sentViaWs = if (isWebSocketConnected) {
            webSocket?.send(payloadJson.toString()) == true
        } else {
            false
        }

        if (sentViaWs) {
            Log.d(TAG, "Notification mirrored successfully via WebSocket")
            return
        }

        // Fallback to HTTP POST
        Log.d(TAG, "WebSocket offline or disabled. Falling back to HTTP POST...")
        val request = Request.Builder()
            .url("$serverUrl/notification")
            .post(payloadJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.code == 401) {
                    Log.w(TAG, "Server returned 401 Unauthorized. Auto-clearing pairing on Android.")
                    serviceScope.launch {
                        preferencesManager.clearPairing()
                    }
                } else if (!response.isSuccessful) {
                    Log.e(TAG, "Notification mirroring request failed via HTTP fallback: code ${response.code}")
                } else {
                    Log.d(TAG, "Notification mirrored successfully via HTTP fallback: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed network delivery of notification via HTTP fallback", e)
        }
    }

    private fun getWebSocketUrl(httpUrl: String): String {
        val host = httpUrl.removePrefix("http://").substringBefore(":")
        return "ws://$host:50002/ws"
    }

    private fun connectWebSocket(url: String) {
        disconnectWebSocket()
        currentWsUrl = url
        Log.i(TAG, "Connecting to WebSocket at $url")

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = httpClient.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                Log.i(TAG, "WebSocket connection opened to macOS server.")
                isWebSocketConnected = true
            }

            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                Log.d(TAG, "Received message from WebSocket: $text")
                if (text.contains("\"action\":\"unpair\"")) {
                    Log.i(TAG, "Server requested unpair via WebSocket. Auto-clearing pairing.")
                    serviceScope.launch {
                        preferencesManager.clearPairing()
                    }
                }
            }

            override fun onClosing(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closing: $code / $reason")
                isWebSocketConnected = false
            }

            override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closed: $code / $reason")
                isWebSocketConnected = false
                retryConnection()
            }

            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                isWebSocketConnected = false
                retryConnection()
            }
        })
    }

    private fun disconnectWebSocket() {
        webSocket?.close(1000, "Disconnecting")
        webSocket = null
        isWebSocketConnected = false
        currentWsUrl = null
    }

    private fun retryConnection() {
        val url = currentWsUrl ?: return
        serviceScope.launch {
            kotlinx.coroutines.delay(5000)
            val isPaired = preferencesManager.isPairedFlow.first()
            val lowLatency = preferencesManager.lowLatencyModeFlow.first()
            val currentUrl = preferencesManager.serverUrlFlow.first()
            if (isPaired && lowLatency && currentUrl != null && getWebSocketUrl(currentUrl) == url) {
                Log.i(TAG, "Retrying WebSocket connection to $url")
                connectWebSocket(url)
            }
        }
    }
}
