package com.angelsoft.macmirror.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angelsoft.macmirror.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RawAppInfo(
    val packageName: String,
    val label: String
)

data class AppItem(
    val packageName: String,
    val label: String,
    val isOptedOut: Boolean
)

class SettingsViewModel(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val packageManager: PackageManager = context.packageManager
    private val _installedApps = MutableStateFlow<List<RawAppInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Pure in-memory filtering: 0 IPC calls during searches and toggles!
    val appList: StateFlow<List<AppItem>> = combine(
        _installedApps,
        preferencesManager.optOutAppsFlow,
        _searchQuery
    ) { apps, optOuts, query ->
        val filtered = if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        filtered.map { app ->
            AppItem(
                packageName = app.packageName,
                label = app.label,
                isOptedOut = optOuts.contains(app.packageName)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                try {
                    val rawApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA).filter { app ->
                        // Only show apps that are launchable
                        packageManager.getLaunchIntentForPackage(app.packageName) != null
                    }
                    // Extract labels once in background thread
                    rawApps.map { app ->
                        RawAppInfo(
                            packageName = app.packageName,
                            label = app.loadLabel(packageManager).toString()
                        )
                    }.sortedBy { it.label.lowercase() }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            _installedApps.value = apps
        }
    }

    val lowLatencyMode: StateFlow<Boolean> = preferencesManager.lowLatencyModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val keepAlivePersistentService: StateFlow<Boolean> = preferencesManager.keepAlivePersistentServiceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isBatteryOptimizationIgnored = MutableStateFlow(com.angelsoft.macmirror.util.PermissionUtils.isBatteryOptimizationIgnored(context))
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored

    val themeMode: StateFlow<Int> = preferencesManager.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun refreshBatteryOptimizationStatus() {
        _isBatteryOptimizationIgnored.value = com.angelsoft.macmirror.util.PermissionUtils.isBatteryOptimizationIgnored(context)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAppOptOut(packageName: String, currentOptedOut: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAppOptOut(packageName, !currentOptedOut)
        }
    }

    fun toggleAllApps(muteAll: Boolean) {
        viewModelScope.launch {
            val allPackages = _installedApps.value.map { it.packageName }
            allPackages.forEach { pkg ->
                preferencesManager.setAppOptOut(pkg, muteAll)
            }
        }
    }

    fun toggleLowLatencyMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setLowLatencyMode(enabled)
            if (enabled) {
                preferencesManager.setKeepAlivePersistentService(true)
            }
        }
    }

    fun toggleKeepAlivePersistentService(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setKeepAlivePersistentService(enabled)
        }
    }

    fun requestIgnoreBatteryOptimization(ctx: Context) {
        com.angelsoft.macmirror.util.PermissionUtils.requestIgnoreBatteryOptimization(ctx)
    }

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }
}
