package com.angelsoft.macmirror.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.angelsoft.macmirror.MainActivity
import com.angelsoft.macmirror.R
import com.angelsoft.macmirror.data.PreferencesManager
import com.angelsoft.macmirror.network.NsdHelper
import com.angelsoft.macmirror.security.CryptoManager
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
        private const val PERSISTENT_CHANNEL_ID = "macmirror_persistent_service"
        private const val PERSISTENT_NOTIFICATION_ID = 1001
        const val TEST_NOTIFICATION_ID = 999

        private val _isServiceBound = MutableStateFlow(false)
        val isServiceBound: StateFlow<Boolean> = _isServiceBound

        private val _diagnosticEvents = MutableSharedFlow<DiagnosticEvent>(extraBufferCapacity = 64)
        val diagnosticEvents: SharedFlow<DiagnosticEvent> = _diagnosticEvents

        fun postTestNotification(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelName = context.getString(R.string.test_notification_channel_name)
            val channelDesc = context.getString(R.string.test_notification_channel_desc)
            val title = context.getString(R.string.test_notification_title)
            val text = context.getString(R.string.test_notification_text)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "test_channel",
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = channelDesc
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, "test_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addExtras(android.os.Bundle().apply {
                    putBoolean("is_test_notification", true)
                })
                .build()

            notificationManager.notify(TEST_NOTIFICATION_ID, notification)
        }
    }

    sealed interface DiagnosticEvent {
        data class NotificationIntercepted(val notificationId: String, val title: String) : DiagnosticEvent
        data class NotificationEncrypted(val notificationId: String) : DiagnosticEvent
        data class NotificationSent(val notificationId: String, val viaWebSocket: Boolean) : DiagnosticEvent
        data class NotificationAckReceived(val notificationId: String) : DiagnosticEvent
        data class NotificationFailed(val notificationId: String, val reason: String) : DiagnosticEvent
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "NotificationListenerService successfully connected to Android system.")
        _isServiceBound.value = true
        serviceScope.launch {
            val keepAlive = preferencesManager.keepAlivePersistentServiceFlow.first()
            updateForegroundService(keepAlive)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "NotificationListenerService disconnected from Android system. Requesting rebind...")
        _isServiceBound.value = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, NotificationListener::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to requestRebind in onListenerDisconnected", e)
            }
        }
    }

    private val preferencesManager: PreferencesManager by inject()
    private val cryptoManager: CryptoManager by inject()
    private val nsdHelper: NsdHelper by inject()
    private val httpClient = OkHttpClient.Builder()
        .pingInterval(java.time.Duration.ofSeconds(30))
        .build()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocket: okhttp3.WebSocket? = null
    @Volatile
    private var isWebSocketConnected = false
    private var currentWsUrl: String? = null
    @Volatile
    private var isForegroundActive = false
    private var currentPairedDeviceName: String? = null
    @Volatile
    private var currentIsPaired = false

    private val pendingAcks = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationListener service created.")
        createNotificationChannel()

        // Start watching persistent service preferences and pairing state
        serviceScope.launch {
            combine(
                preferencesManager.isPairedFlow,
                preferencesManager.keepAlivePersistentServiceFlow,
                preferencesManager.pairedDeviceNameFlow
            ) { paired, keepAlive, deviceName ->
                Triple(paired, keepAlive, deviceName)
            }.collect { (paired, keepAlive, deviceName) ->
                currentPairedDeviceName = deviceName
                currentIsPaired = paired
                updateForegroundService(keepAlive)
            }
        }

        // Background NSD discovery to continuously keep server URL fresh
        serviceScope.launch {
            try {
                nsdHelper.startDiscovery()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start NSD discovery in NotificationListener", e)
            }
            combine(preferencesManager.isPairedFlow, nsdHelper.resolvedServerUrl) { paired, discoveredUrl ->
                if (paired && discoveredUrl != null) {
                    val saved = preferencesManager.serverUrlFlow.first()
                    if (discoveredUrl != saved) {
                        Log.i(TAG, "NSD discovered new server URL in background: $discoveredUrl")
                        preferencesManager.updateServerUrl(discoveredUrl)
                    }
                }
            }.collect {}
        }

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
        updateForegroundService(false)
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

                _diagnosticEvents.tryEmit(DiagnosticEvent.NotificationIntercepted(sbn.key, title))

                // Decrypt session key and encrypt payload
                val sessionKey = cryptoManager.decryptSessionKey(encryptedKey)
                val encryptedResult = cryptoManager.encryptPayload(payloadJson.toString(), sessionKey)
                _diagnosticEvents.tryEmit(DiagnosticEvent.NotificationEncrypted(sbn.key))

                // Transmit payload to macOS server
                sendNotificationToServer(serverUrl, encryptedResult, sbn.key)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification posted", e)
                _diagnosticEvents.tryEmit(DiagnosticEvent.NotificationFailed(sbn.key, e.localizedMessage ?: "Unknown error"))
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

    private fun sendNotificationToServer(
        serverUrl: String,
        encrypted: CryptoManager.EncryptedResult,
        notificationId: String
    ) {
        val payloadJson = JSONObject().apply {
            put("iv", encrypted.iv)
            put("ciphertext", encrypted.ciphertext)
            put("tag", encrypted.tag)
            put("timestamp", System.currentTimeMillis() / 1000)
        }

        // Try to send via WebSocket if connected
        val deferredAck = CompletableDeferred<Boolean>()
        pendingAcks[notificationId] = deferredAck

        var ackConfirmed = false
        if (isWebSocketConnected) {
            val sent = webSocket?.send(payloadJson.toString()) == true
            if (sent) {
                _diagnosticEvents.tryEmit(DiagnosticEvent.NotificationSent(notificationId, viaWebSocket = true))
                Log.d(TAG, "Notification enqueued to WebSocket, waiting up to 3s for ACK...")
                try {
                    runBlocking(Dispatchers.IO) {
                        withTimeout(3000) {
                            ackConfirmed = deferredAck.await()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "WebSocket ACK timed out for notification $notificationId. Falling back to HTTP.")
                    ackConfirmed = false
                }
            }
        }
        pendingAcks.remove(notificationId)

        if (ackConfirmed) {
            Log.d(TAG, "Notification mirrored successfully via WebSocket with confirmed ACK")
            _diagnosticEvents.tryEmit(DiagnosticEvent.NotificationAckReceived(notificationId))
            return
        }

        // Fallback to HTTP POST
        Log.d(TAG, "WebSocket offline, timed out, or unconfirmed. Falling back to HTTP POST...")
        val request = Request.Builder()
            .url("$serverUrl/notification")
            .post(payloadJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        var delivered = false
        for (attempt in 1..2) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.code == 401) {
                        Log.w(TAG, "Server returned 401 Unauthorized. Auto-clearing pairing on Android.")
                        serviceScope.launch {
                            preferencesManager.clearPairing()
                        }
                        _diagnosticEvents.tryEmit(DiagnosticEvent.NotificationFailed(notificationId, "401 Unauthorized"))
                        return
                    } else if (!response.isSuccessful) {
                        Log.e(TAG, "Notification mirroring request failed via HTTP fallback: code ${response.code}")
                    } else {
                        Log.d(TAG, "Notification mirrored successfully via HTTP fallback: ${response.code}")
                        _diagnosticEvents.tryEmit(DiagnosticEvent.NotificationSent(notificationId, viaWebSocket = false))
                        _diagnosticEvents.tryEmit(DiagnosticEvent.NotificationAckReceived(notificationId))
                        delivered = true
                        return
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "HTTP fallback attempt $attempt failed: ${e.message}")
                if (attempt == 1) {
                    Thread.sleep(300)
                }
            }
        }
        if (!delivered) {
            Log.e(TAG, "Failed network delivery of notification via HTTP fallback to $serverUrl")
            _diagnosticEvents.tryEmit(DiagnosticEvent.NotificationFailed(notificationId, "Network delivery failed"))
        }
    }

    private fun getWebSocketUrl(httpUrl: String): String {
        val parsed = httpUrl.toHttpUrlOrNull()
        val host = parsed?.host ?: httpUrl.removePrefix("http://").substringBefore(":")
        val formattedHost = if (host.contains(":") && !host.startsWith("[")) "[$host]" else host
        return "ws://$formattedHost:50002/ws"
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
                updatePersistentNotification()
            }

            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                Log.d(TAG, "Received message from WebSocket: $text")
                if (text.contains("\"action\":\"unpair\"")) {
                    Log.i(TAG, "Server requested unpair via WebSocket. Auto-clearing pairing.")
                    serviceScope.launch {
                        preferencesManager.clearPairing()
                    }
                } else if (text.contains("\"action\":\"trigger_test_notification\"")) {
                    Log.i(TAG, "Server requested trigger_test_notification via WebSocket.")
                    postTestNotification(this@NotificationListener)
                } else if (text.contains("\"type\":\"ack\"")) {
                    try {
                        val json = JSONObject(text)
                        val ackId = json.optString("id")
                        if (ackId.isNotEmpty()) {
                            pendingAcks[ackId]?.complete(true)
                            _diagnosticEvents.tryEmit(DiagnosticEvent.NotificationAckReceived(ackId))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse ACK payload", e)
                    }
                }
            }

            override fun onClosing(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closing: $code / $reason")
                isWebSocketConnected = false
                updatePersistentNotification()
            }

            override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closed: $code / $reason")
                isWebSocketConnected = false
                updatePersistentNotification()
                retryConnection()
            }

            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                isWebSocketConnected = false
                updatePersistentNotification()
                retryConnection()
            }
        })
    }

    private fun disconnectWebSocket() {
        webSocket?.close(1000, "Disconnecting")
        webSocket = null
        isWebSocketConnected = false
        currentWsUrl = null
        updatePersistentNotification()
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

    private fun updateForegroundService(enabled: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (enabled) {
            val notification = buildPersistentNotification(currentPairedDeviceName, isWebSocketConnected)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        PERSISTENT_NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    )
                } else {
                    startForeground(PERSISTENT_NOTIFICATION_ID, notification)
                }
                isForegroundActive = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
            }
        } else {
            if (isForegroundActive) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop foreground service", e)
                }
                notificationManager?.cancel(PERSISTENT_NOTIFICATION_ID)
                isForegroundActive = false
            }
        }
    }

    private fun updatePersistentNotification() {
        if (!isForegroundActive) return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notification = buildPersistentNotification(currentPairedDeviceName, isWebSocketConnected)
        notificationManager.notify(PERSISTENT_NOTIFICATION_ID, notification)
    }

    private fun buildPersistentNotification(pairedDeviceName: String?, isConnected: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (isConnected && !pairedDeviceName.isNullOrBlank()) {
            getString(R.string.persistent_notification_connected, pairedDeviceName)
        } else if (currentIsPaired && !pairedDeviceName.isNullOrBlank()) {
            getString(R.string.persistent_notification_searching, pairedDeviceName)
        } else if (currentIsPaired) {
            getString(R.string.persistent_notification_standby, "Mac")
        } else {
            getString(R.string.persistent_notification_unpaired)
        }

        return NotificationCompat.Builder(this, PERSISTENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PERSISTENT_CHANNEL_ID,
                getString(R.string.persistent_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.persistent_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }
}
