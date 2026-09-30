package com.angelsoft.macmirror.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.content.ContextCompat
import com.angelsoft.macmirror.data.PreferencesManager
import com.angelsoft.macmirror.service.NotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i(TAG, "Boot event received: $action")
            val pendingResult = goAsync()

            val preferencesManager: PreferencesManager = try {
                getKoin().get()
            } catch (e: Exception) {
                PreferencesManager(context.applicationContext)
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isPaired = preferencesManager.isPairedFlow.first()
                    val autoStart = preferencesManager.autoStartOnBootFlow.first()
                    val keepAlive = preferencesManager.keepAlivePersistentServiceFlow.first()

                    Log.d(TAG, "isPaired: $isPaired, autoStart: $autoStart, keepAlive: $keepAlive")

                    if (isPaired && autoStart) {
                        Log.i(TAG, "Auto-start on boot is active for paired device.")

                        // 1. Request rebind of the NotificationListenerService
                        try {
                            NotificationListenerService.requestRebind(
                                ComponentName(context, NotificationListener::class.java)
                            )
                            Log.i(TAG, "NotificationListenerService rebind requested on boot.")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to requestRebind of NotificationListenerService on boot", e)
                        }

                        // 2. If persistent service is enabled, launch the foreground service
                        if (keepAlive) {
                            try {
                                val serviceIntent = Intent(context, NotificationListener::class.java)
                                ContextCompat.startForegroundService(context, serviceIntent)
                                Log.i(TAG, "Persistent foreground service started on boot.")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to start foreground service on boot", e)
                            }
                        }
                    } else {
                        Log.d(TAG, "Auto-start skipped on boot: not paired or auto-start disabled.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing boot broadcast", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
