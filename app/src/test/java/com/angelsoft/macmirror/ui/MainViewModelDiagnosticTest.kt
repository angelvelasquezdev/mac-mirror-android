package com.angelsoft.macmirror.ui

import android.content.Context
import androidx.core.content.ContextCompat
import com.angelsoft.macmirror.R
import com.angelsoft.macmirror.data.PreferencesManager
import com.angelsoft.macmirror.network.NsdHelper
import com.angelsoft.macmirror.security.CryptoManager
import com.angelsoft.macmirror.service.NotificationListener
import com.angelsoft.macmirror.util.PermissionUtils
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelDiagnosticTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk(relaxed = true)
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val cryptoManager: CryptoManager = mockk(relaxed = true)
    private val nsdHelper: NsdHelper = mockk(relaxed = true)

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkObject(PermissionUtils)
        every { PermissionUtils.isNotificationServiceEnabled(any()) } returns true
        every { PermissionUtils.forceRebindNotificationListener(any()) } just Runs
        every { PermissionUtils.rebindNotificationListener(any()) } just Runs
        every { PermissionUtils.openNotificationListenerSettings(any()) } just Runs

        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns android.content.pm.PackageManager.PERMISSION_GRANTED

        every { preferencesManager.isPairedFlow } returns flowOf(false)
        every { preferencesManager.pairedDeviceNameFlow } returns flowOf(null)
        every { preferencesManager.serverUrlFlow } returns flowOf(null)
        every { nsdHelper.resolvedServerUrl } returns kotlinx.coroutines.flow.MutableStateFlow(null)

        NotificationListener.setServiceBoundForTesting(false)

        viewModel = MainViewModel(preferencesManager, cryptoManager, nsdHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        NotificationListener.setServiceBoundForTesting(false)
    }

    @Test
    fun testStep2SuccessWhenServiceAlreadyBound() = runTest(testDispatcher) {
        try {
            NotificationListener.setServiceBoundForTesting(true)

            viewModel.runDiagnosticTest(context)
            advanceTimeBy(300)

            val state = viewModel.diagnosticState.value
            val step2 = state.steps.first { it.id == MainViewModel.DiagnosticStepId.SERVICE_RUNNING }
            assertEquals(MainViewModel.StepStatus.SUCCESS, step2.status)
            assertEquals(R.string.diag_step2_ok, step2.detailResId)
            assertEquals(0, viewModel.listenerRebindAttempts.value)
        } finally {
            viewModel.cancelCoroutinesForTesting()
        }
    }

    @Test
    fun testStep2FailureWhenServiceRemainsUnbound() = runTest(testDispatcher) {
        try {
            NotificationListener.setServiceBoundForTesting(false)

            viewModel.runDiagnosticTest(context)
            advanceTimeBy(3000)

            val state = viewModel.diagnosticState.value
            val step2 = state.steps.first { it.id == MainViewModel.DiagnosticStepId.SERVICE_RUNNING }
            assertEquals(MainViewModel.StepStatus.ERROR, step2.status)
            assertEquals(R.string.diag_step2_err, step2.detailResId)
            assertTrue(state.hasError)
            assertFalse(state.isRunning)

            verify { PermissionUtils.forceRebindNotificationListener(context) }
        } finally {
            viewModel.cancelCoroutinesForTesting()
        }
    }

    @Test
    fun testStep2AutoHealsWhenServiceBindsDuringWait() = runTest(testDispatcher) {
        try {
            NotificationListener.setServiceBoundForTesting(false)

            viewModel.runDiagnosticTest(context)
            advanceTimeBy(300)

            // Simulate Android system binding the service after force rebind
            NotificationListener.setServiceBoundForTesting(true)
            advanceTimeBy(500)

            val state = viewModel.diagnosticState.value
            val step2 = state.steps.first { it.id == MainViewModel.DiagnosticStepId.SERVICE_RUNNING }
            assertEquals(MainViewModel.StepStatus.SUCCESS, step2.status)
            assertEquals(R.string.diag_step2_ok, step2.detailResId)
        } finally {
            viewModel.cancelCoroutinesForTesting()
        }
    }

    @Test
    fun testHandleServiceRunningActionDualBehavior() = runTest(testDispatcher) {
        try {
            assertEquals(0, viewModel.listenerRebindAttempts.value)

            // Attempt 1: should increment attempts and call forceRebindNotificationListener
            viewModel.handleServiceRunningAction(context)
            assertEquals(1, viewModel.listenerRebindAttempts.value)
            verify(atLeast = 1) { PermissionUtils.forceRebindNotificationListener(context) }

            // Attempt 2: should directly open notification listener settings
            viewModel.handleServiceRunningAction(context)
            verify(exactly = 1) { PermissionUtils.openNotificationListenerSettings(context) }
        } finally {
            viewModel.cancelCoroutinesForTesting()
        }
    }
}
