package com.angelsoft.macmirror.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.angelsoft.macmirror.R
import com.angelsoft.macmirror.ui.MainViewModel
import com.angelsoft.macmirror.ui.components.AppleButton
import com.angelsoft.macmirror.ui.components.CupertinoCard
import com.angelsoft.macmirror.ui.components.CupertinoRow
import com.angelsoft.macmirror.ui.components.CupertinoSection
import com.angelsoft.macmirror.ui.components.PinInputView
import com.angelsoft.macmirror.ui.theme.AppleBlue
import com.angelsoft.macmirror.ui.theme.AppleGreen
import com.angelsoft.macmirror.ui.theme.AppleRed
import com.angelsoft.macmirror.util.PermissionUtils
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    viewModel: MainViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPaired by viewModel.isPaired.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val pairedDeviceName by viewModel.pairedDeviceName.collectAsState()
    val savedServerUrl by viewModel.savedServerUrl.collectAsState()
    val discoveredServerUrl by viewModel.discoveredServerUrl.collectAsState()
    val pairingState by viewModel.pairingState.collectAsState()
    val diagnosticState by viewModel.diagnosticState.collectAsState()

    val postNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.runDiagnosticTest(context)
            }
        }
    )

    var permissionGranted by remember {
        mutableStateOf(PermissionUtils.isNotificationServiceEnabled(context))
    }
    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(PermissionUtils.isBatteryOptimizationIgnored(context))
    }

    // Auto-detect when returning to the app from System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                if (PermissionUtils.isNotificationServiceEnabled(context)) {
                    permissionGranted = true
                    PermissionUtils.rebindNotificationListener(context)
                }
                isBatteryOptimizationIgnored = PermissionUtils.isBatteryOptimizationIgnored(context)
                viewModel.refreshBatteryOptimizationStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Active polling while permission not granted
    LaunchedEffect(permissionGranted) {
        while (!permissionGranted) {
            if (PermissionUtils.isNotificationServiceEnabled(context)) {
                permissionGranted = true
                PermissionUtils.rebindNotificationListener(context)
                break
            }
            delay(350)
        }
    }

    // Active polling while battery optimization not ignored
    LaunchedEffect(isBatteryOptimizationIgnored) {
        while (!isBatteryOptimizationIgnored) {
            if (PermissionUtils.isBatteryOptimizationIgnored(context)) {
                isBatteryOptimizationIgnored = true
                viewModel.refreshBatteryOptimizationStatus(context)
                break
            }
            delay(500)
        }
    }

    var pinInput by remember { mutableStateOf("") }
    var showManualIpDialog by remember { mutableStateOf(false) }
    var manualIpInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppleBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = AppleBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 1. Friendly Privacy & Permission Card (Auto-hides as soon as permission is granted)
            AnimatedVisibility(
                visible = !permissionGranted,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                CupertinoCard(cornerRadius = 20.dp) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AppleBlue.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AppleBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = stringResource(R.string.permission_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.permission_tag),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppleGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.permission_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp
                        )

                        AppleButton(
                            text = stringResource(R.string.permission_button),
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            },
                            isPrimary = true
                        )
                    }
                }
            }

            // 2. Battery Optimization Advisory Card (Auto-hides as soon as optimization is ignored)
            AnimatedVisibility(
                visible = permissionGranted && isPaired && !isBatteryOptimizationIgnored,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                CupertinoCard(cornerRadius = 20.dp) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF9500).copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9500),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = stringResource(R.string.battery_optimization_card_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.battery_optimization_card_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp
                        )

                        AppleButton(
                            text = stringResource(R.string.battery_optimization_card_button),
                            onClick = {
                                viewModel.requestIgnoreBatteryOptimization(context)
                            },
                            isPrimary = true
                        )
                    }
                }
            }

            // 3. Main Connection Experience
            if (isPaired) {
                // PAIRED STATE
                CupertinoSection(
                    title = stringResource(R.string.active_connection_title),
                    caption = stringResource(R.string.active_connection_caption)
                ) {
                    val connectionStatusTitle = if (isConnected) {
                        stringResource(R.string.status_connected_syncing)
                    } else {
                        stringResource(R.string.status_standby_reconnecting)
                    }
                    val connectionSubtitle = if (isConnected) {
                        stringResource(R.string.status_low_latency_active)
                    } else {
                        stringResource(R.string.status_verifying_server)
                    }
                    val connectionIcon = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Refresh
                    val connectionColor = if (isConnected) AppleGreen else Color(0xFFFF9500)

                    CupertinoRow(
                        title = connectionStatusTitle,
                        subtitle = connectionSubtitle,
                        icon = connectionIcon,
                        iconColor = connectionColor,
                        showDivider = true
                    )

                    CupertinoRow(
                        title = pairedDeviceName ?: stringResource(R.string.paired_mac_title),
                        subtitle = stringResource(R.string.paired_mac_subtitle),
                        icon = Icons.Default.Phone,
                        iconColor = AppleBlue,
                        showDivider = true
                    )

                    CupertinoRow(
                        title = stringResource(R.string.server_address_title),
                        subtitle = savedServerUrl ?: stringResource(R.string.server_address_auto),
                        icon = Icons.Default.Share,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Actions
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppleButton(
                        text = stringResource(R.string.btn_send_test_notification),
                        onClick = {
                            viewModel.runDiagnosticTest(context)
                        },
                        isPrimary = true
                    )

                    AppleButton(
                        text = stringResource(R.string.btn_unpair_mac),
                        onClick = { viewModel.unpair() },
                        isPrimary = false,
                        isDestructive = true
                    )
                }
            } else {
                // UNPAIRED STATE (Frictionless Onboarding)
                if (discoveredServerUrl == null) {
                    // Searching State
                    CupertinoCard(cornerRadius = 22.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(46.dp),
                                strokeWidth = 3.5.dp,
                                color = AppleBlue
                            )

                            Text(
                                text = stringResource(R.string.searching_mac_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = stringResource(R.string.searching_mac_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                TextButton(onClick = { viewModel.restartDiscovery() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.btn_retry),
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppleBlue
                                    )
                                }

                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.outline
                                )

                                TextButton(onClick = { showManualIpDialog = true }) {
                                    Text(
                                        text = stringResource(R.string.btn_connect_by_ip),
                                        fontWeight = FontWeight.Medium,
                                        color = AppleBlue
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Discovered -> Fast PIN Entry
                    CupertinoCard(cornerRadius = 22.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Found Device Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CapsuleShape)
                                    .background(AppleGreen.copy(alpha = 0.12f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(AppleGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.mac_found_tag),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.enter_pin_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.enter_pin_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // 6-digit iOS PIN view
                            PinInputView(
                                pin = pinInput,
                                onPinChange = { pinInput = it },
                                onPinComplete = { completedPin ->
                                    viewModel.pairDevice(completedPin)
                                }
                            )

                            // State Feedback (Loading / Error)
                            AnimatedVisibility(
                                visible = pairingState !is MainViewModel.PairingState.Idle,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                when (val state = pairingState) {
                                    is MainViewModel.PairingState.Loading -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AppleBlue)
                                            Text(
                                                text = stringResource(R.string.establishing_secure_channel),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AppleBlue
                                            )
                                        }
                                    }
                                    is MainViewModel.PairingState.Error -> {
                                        val errorMsg = if (state.stringResId != null) {
                                            stringResource(state.stringResId)
                                        } else {
                                            state.message
                                        }
                                        Text(
                                            text = errorMsg,
                                            color = AppleRed,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    else -> {}
                                }
                            }

                            AppleButton(
                                text = if (pairingState is MainViewModel.PairingState.Loading) {
                                    stringResource(R.string.btn_pairing)
                                } else {
                                    stringResource(R.string.btn_pair_with_mac)
                                },
                                onClick = { viewModel.pairDevice(pinInput) },
                                enabled = pinInput.length == 6 && pairingState !is MainViewModel.PairingState.Loading,
                                isPrimary = true
                            )

                            TextButton(
                                onClick = { showManualIpDialog = true },
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_switch_to_manual_ip),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppleBlue
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Manual IP Dialog
    if (showManualIpDialog) {
        AlertDialog(
            onDismissRequest = { showManualIpDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.manual_ip_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.manual_ip_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = manualIpInput,
                        onValueChange = { manualIpInput = it },
                        placeholder = { Text(stringResource(R.string.manual_ip_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showManualIpDialog = false
                        if (manualIpInput.isNotBlank() && pinInput.length == 6) {
                            viewModel.pairDevice(pinInput, customUrl = manualIpInput)
                        }
                    },
                    enabled = manualIpInput.isNotBlank()
                ) {
                    Text(
                        text = stringResource(R.string.btn_connect),
                        fontWeight = FontWeight.Bold,
                        color = AppleBlue
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualIpDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Interactive Diagnostic Modal BottomSheet
    if (diagnosticState.isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissDiagnostic() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            DiagnosticBottomSheetContent(
                diagnosticState = diagnosticState,
                onRepeat = { viewModel.runDiagnosticTest(context) },
                onClose = { viewModel.dismissDiagnostic() },
                onFixPermissions = {
                    if (!PermissionUtils.isNotificationServiceEnabled(context)) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } else if (android.os.Build.VERSION.SDK_INT >= 33) {
                        postNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }
    }
}

@Composable
fun DiagnosticBottomSheetContent(
    diagnosticState: MainViewModel.DiagnosticState,
    onRepeat: () -> Unit,
    onClose: () -> Unit,
    onFixPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.diag_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.diag_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Card with 7 Steps
        CupertinoCard(cornerRadius = 18.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                diagnosticState.steps.forEachIndexed { index, step ->
                    val showDivider = index < diagnosticState.steps.size - 1

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Status Icon
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (step.status) {
                                MainViewModel.StepStatus.PENDING -> {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                }
                                MainViewModel.StepStatus.RUNNING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = AppleBlue
                                    )
                                }
                                MainViewModel.StepStatus.SUCCESS -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AppleGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                MainViewModel.StepStatus.ERROR -> {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AppleRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Step Titles
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(step.titleResId),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val detailText = when (step.status) {
                                MainViewModel.StepStatus.SUCCESS -> step.detailResId?.let { stringResource(it) }
                                MainViewModel.StepStatus.ERROR -> step.errorDetail ?: step.detailResId?.let { stringResource(it) }
                                else -> null
                            }
                            if (detailText != null) {
                                val detailColor = if (step.status == MainViewModel.StepStatus.SUCCESS) AppleGreen else AppleRed
                                Text(
                                    text = detailText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = detailColor,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Actionable button if permission error
                        if (step.id == MainViewModel.DiagnosticStepId.PERMISSIONS && step.status == MainViewModel.StepStatus.ERROR) {
                            TextButton(
                                onClick = onFixPermissions,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_retry),
                                    color = AppleBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (showDivider) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 54.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Summary Banner
        val summaryBgColor = when {
            diagnosticState.isRunning -> AppleBlue.copy(alpha = 0.12f)
            diagnosticState.isCompleted && !diagnosticState.hasError -> AppleGreen.copy(alpha = 0.12f)
            diagnosticState.hasError -> AppleRed.copy(alpha = 0.12f)
            else -> Color.Transparent
        }
        val summaryContentColor = when {
            diagnosticState.isRunning -> AppleBlue
            diagnosticState.isCompleted && !diagnosticState.hasError -> AppleGreen
            diagnosticState.hasError -> AppleRed
            else -> MaterialTheme.colorScheme.onSurface
        }
        val summaryText = when {
            diagnosticState.isRunning -> stringResource(R.string.diag_status_testing)
            diagnosticState.isCompleted && !diagnosticState.hasError -> stringResource(R.string.diag_status_passed)
            diagnosticState.hasError -> stringResource(R.string.diag_status_failed)
            else -> ""
        }

        if (summaryText.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(summaryBgColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (diagnosticState.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = summaryContentColor
                    )
                } else if (diagnosticState.isCompleted && !diagnosticState.hasError) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = summaryContentColor,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = summaryContentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = summaryContentColor
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AppleButton(
                    text = stringResource(R.string.diag_btn_repeat),
                    onClick = onRepeat,
                    isPrimary = diagnosticState.isCompleted || diagnosticState.hasError,
                    leadingIcon = Icons.Default.Refresh
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                AppleButton(
                    text = stringResource(R.string.diag_btn_close),
                    onClick = onClose,
                    isPrimary = false
                )
            }
        }
    }
}

val CapsuleShape = RoundedCornerShape(percent = 50)

fun isNotificationServiceEnabled(context: Context): Boolean = PermissionUtils.isNotificationServiceEnabled(context)
