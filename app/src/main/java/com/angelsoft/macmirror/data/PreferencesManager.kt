package com.angelsoft.macmirror.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "macmirror_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val PAIRED_DEVICE_NAME = stringPreferencesKey("paired_device_name")
        private val ENCRYPTED_SHARED_KEY = stringPreferencesKey("encrypted_shared_key")
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val IS_PAIRED = booleanPreferencesKey("is_paired")
        private val OPT_OUT_APPS = stringSetPreferencesKey("opt_out_apps")
        private val LOW_LATENCY_MODE = booleanPreferencesKey("low_latency_mode")
        private val THEME_MODE = intPreferencesKey("theme_mode")
        private val COMPLETED_ONBOARDING = booleanPreferencesKey("completed_onboarding")
    }

    val hasCompletedOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[COMPLETED_ONBOARDING] ?: false
    }

    val isPairedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_PAIRED] ?: false
    }

    val serverUrlFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SERVER_URL]
    }

    val pairedDeviceNameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PAIRED_DEVICE_NAME]
    }

    val encryptedSharedKeyFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ENCRYPTED_SHARED_KEY]
    }

    val optOutAppsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[OPT_OUT_APPS] ?: emptySet()
    }

    val lowLatencyModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[LOW_LATENCY_MODE] ?: false
    }

    val themeModeFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: 0
    }

    suspend fun setLowLatencyMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[LOW_LATENCY_MODE] = enabled
        }
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode
        }
    }

    suspend fun savePairingInfo(deviceName: String, serverUrl: String, encryptedKeyBase64: String) {
        context.dataStore.edit { prefs ->
            prefs[PAIRED_DEVICE_NAME] = deviceName
            prefs[SERVER_URL] = serverUrl
            prefs[ENCRYPTED_SHARED_KEY] = encryptedKeyBase64
            prefs[IS_PAIRED] = true
        }
    }

    suspend fun updateServerUrl(serverUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL] = serverUrl
        }
    }

    suspend fun clearPairing() {
        context.dataStore.edit { prefs ->
            prefs.remove(PAIRED_DEVICE_NAME)
            prefs.remove(SERVER_URL)
            prefs.remove(ENCRYPTED_SHARED_KEY)
            prefs[IS_PAIRED] = false
        }
    }

    suspend fun setAppOptOut(packageName: String, optOut: Boolean) {
        context.dataStore.edit { prefs ->
            val currentList = prefs[OPT_OUT_APPS] ?: emptySet()
            val newList = if (optOut) {
                currentList + packageName
            } else {
                currentList - packageName
            }
            prefs[OPT_OUT_APPS] = newList
        }
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[COMPLETED_ONBOARDING] = completed
        }
    }
}
