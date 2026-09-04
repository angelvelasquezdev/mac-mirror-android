package com.angelsoft.macmirror.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.angelsoft.macmirror.R
import com.angelsoft.macmirror.ui.SettingsViewModel
import com.angelsoft.macmirror.ui.components.CupertinoRow
import com.angelsoft.macmirror.ui.components.CupertinoSection
import com.angelsoft.macmirror.ui.components.CupertinoSegmentedControl
import com.angelsoft.macmirror.ui.components.CupertinoSwitch
import com.angelsoft.macmirror.ui.theme.AppleBlue
import com.angelsoft.macmirror.ui.theme.AppleGreen
import com.angelsoft.macmirror.ui.theme.AppleIndigo
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    onShowOnboarding: (() -> Unit)? = null,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lowLatencyMode by viewModel.lowLatencyMode.collectAsState()
    val keepAlivePersistentService by viewModel.keepAlivePersistentService.collectAsState()
    val isBatteryOptimizationIgnored by viewModel.isBatteryOptimizationIgnored.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val scrollState = rememberScrollState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                viewModel.refreshBatteryOptimizationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = bottomBar
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Section: Apariencia
            CupertinoSection(
                title = stringResource(R.string.section_appearance_title),
                caption = stringResource(R.string.section_appearance_caption)
            ) {
                val themeOptions = listOf(
                    stringResource(R.string.theme_system),
                    stringResource(R.string.theme_light),
                    stringResource(R.string.theme_dark)
                )
                Box(modifier = Modifier.padding(14.dp)) {
                    CupertinoSegmentedControl(
                        options = themeOptions,
                        selectedIndex = themeMode,
                        onOptionSelected = { viewModel.setThemeMode(it) }
                    )
                }
            }

            // Section: Rendimiento y Red
            CupertinoSection(
                title = stringResource(R.string.section_network_title),
                caption = stringResource(R.string.section_network_caption)
            ) {
                // Fila: Ahorro de Batería
                CupertinoRow(
                    title = stringResource(R.string.row_battery_optimization_title),
                    subtitle = if (isBatteryOptimizationIgnored) {
                        stringResource(R.string.battery_status_unrestricted)
                    } else {
                        stringResource(R.string.battery_status_optimized)
                    },
                    icon = Icons.Default.Info,
                    iconColor = if (isBatteryOptimizationIgnored) AppleGreen else Color(0xFFFF9500),
                    showDivider = true,
                    onClick = { viewModel.requestIgnoreBatteryOptimization(context) }
                )

                // Fila: Servicio en Segundo Plano
                CupertinoRow(
                    title = stringResource(R.string.row_keep_alive_title),
                    subtitle = if (keepAlivePersistentService) {
                        stringResource(R.string.row_keep_alive_active)
                    } else {
                        stringResource(R.string.row_keep_alive_inactive)
                    },
                    icon = Icons.Default.Notifications,
                    iconColor = AppleIndigo,
                    showDivider = true,
                    trailingContent = {
                        CupertinoSwitch(
                            checked = keepAlivePersistentService,
                            onCheckedChange = { viewModel.toggleKeepAlivePersistentService(it) }
                        )
                    }
                )

                // Fila: Baja Latencia
                CupertinoRow(
                    title = stringResource(R.string.row_low_latency_title),
                    subtitle = if (lowLatencyMode) {
                        stringResource(R.string.row_low_latency_active)
                    } else {
                        stringResource(R.string.row_low_latency_inactive)
                    },
                    icon = Icons.Default.Refresh,
                    iconColor = AppleBlue,
                    trailingContent = {
                        CupertinoSwitch(
                            checked = lowLatencyMode,
                            onCheckedChange = { viewModel.toggleLowLatencyMode(it) }
                        )
                    }
                )
            }

            // Section: Privacidad y Seguridad
            CupertinoSection(
                title = stringResource(R.string.section_privacy_title),
                caption = stringResource(R.string.section_privacy_caption)
            ) {
                CupertinoRow(
                    title = stringResource(R.string.row_e2ee_title),
                    subtitle = stringResource(R.string.row_e2ee_subtitle),
                    icon = Icons.Default.Lock,
                    iconColor = AppleGreen,
                    showDivider = true
                )
                CupertinoRow(
                    title = stringResource(R.string.row_keystore_title),
                    subtitle = stringResource(R.string.row_keystore_subtitle),
                    icon = Icons.Default.CheckCircle,
                    iconColor = AppleIndigo
                )
            }

            // Section: Acerca de y Guía
            CupertinoSection(
                title = stringResource(R.string.section_about_title),
                caption = stringResource(R.string.section_about_caption)
            ) {
                if (onShowOnboarding != null) {
                    CupertinoRow(
                        title = stringResource(R.string.row_open_onboarding_title),
                        subtitle = stringResource(R.string.row_open_onboarding_subtitle),
                        icon = Icons.Default.Notifications,
                        iconColor = AppleBlue,
                        showDivider = true,
                        onClick = onShowOnboarding
                    )
                }
                CupertinoRow(
                    title = stringResource(R.string.row_about_app),
                    subtitle = stringResource(R.string.row_about_version),
                    icon = Icons.Default.Info,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
