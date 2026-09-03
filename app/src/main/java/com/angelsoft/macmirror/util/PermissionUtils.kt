package com.angelsoft.macmirror.util

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.angelsoft.macmirror.service.NotificationListener

object PermissionUtils {

    /**
     * Checks if the NotificationListenerService is enabled for this application.
     * Uses a multi-tiered approach:
     * 1. Direct NotificationManager.isNotificationListenerAccessGranted (queries system_server with 0 cache delay)
     * 2. NotificationManagerCompat.getEnabledListenerPackages
     * 3. Direct Settings.Secure flat string fallback
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
        // Method 1: Official NotificationManager API (available since API 27, minSdk is 28)
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val componentName = ComponentName(context, NotificationListener::class.java)
            if (nm != null && nm.isNotificationListenerAccessGranted(componentName)) {
                return true
            }
        } catch (e: Exception) {
            // Ignore and try fallback methods
        }

        // Method 2: NotificationManagerCompat
        try {
            val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
            if (packageNames.contains(context.packageName)) {
                return true
            }
        } catch (e: Exception) {
            // Ignore and try fallback methods
        }

        // Method 3: Direct Settings.Secure check (handles OEM ROM string variations)
        try {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            if (!flat.isNullOrBlank()) {
                val targetPkg = context.packageName
                if (flat.contains(targetPkg)) {
                    val names = flat.split(":")
                    for (name in names) {
                        val cn = ComponentName.unflattenFromString(name)
                        if (cn != null && cn.packageName == targetPkg) {
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return false
    }
}
