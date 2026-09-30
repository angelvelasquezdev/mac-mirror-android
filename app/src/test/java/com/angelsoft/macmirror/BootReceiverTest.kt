package com.angelsoft.macmirror

import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import androidx.core.content.ContextCompat
import com.angelsoft.macmirror.data.PreferencesManager
import com.angelsoft.macmirror.receiver.BootReceiver
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class BootReceiverTest {

    private val context: Context = mockk(relaxed = true)
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { preferencesManager }
                }
            )
        }
        mockkStatic(android.util.Log::class)
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        mockkStatic(NotificationListenerService::class)
        mockkStatic(ContextCompat::class)
        every { NotificationListenerService.requestRebind(any()) } just Runs
        every { ContextCompat.startForegroundService(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun testBootReceiver_whenPairedAndAutoStartEnabled_requestsRebindAndStartsForegroundIfKeepAlive() {
        every { preferencesManager.isPairedFlow } returns flowOf(true)
        every { preferencesManager.autoStartOnBootFlow } returns flowOf(true)
        every { preferencesManager.keepAlivePersistentServiceFlow } returns flowOf(true)

        val receiver = spyk(BootReceiver())
        val pendingResult: android.content.BroadcastReceiver.PendingResult = mockk(relaxed = true)
        every { receiver.goAsync() } returns pendingResult

        val intent: Intent = mockk()
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED

        receiver.onReceive(context, intent)

        Thread.sleep(300)

        verify {
            NotificationListenerService.requestRebind(any())
            ContextCompat.startForegroundService(any(), any())
        }
    }

    @Test
    fun testBootReceiver_whenNotPaired_skipsRebind() {
        every { preferencesManager.isPairedFlow } returns flowOf(false)
        every { preferencesManager.autoStartOnBootFlow } returns flowOf(true)
        every { preferencesManager.keepAlivePersistentServiceFlow } returns flowOf(true)

        val receiver = spyk(BootReceiver())
        val pendingResult: android.content.BroadcastReceiver.PendingResult = mockk(relaxed = true)
        every { receiver.goAsync() } returns pendingResult

        val intent: Intent = mockk()
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED

        receiver.onReceive(context, intent)

        Thread.sleep(300)

        verify(exactly = 0) {
            NotificationListenerService.requestRebind(any())
            ContextCompat.startForegroundService(any(), any())
        }
    }

    @Test
    fun testBootReceiver_whenAutoStartDisabled_skipsRebind() {
        every { preferencesManager.isPairedFlow } returns flowOf(true)
        every { preferencesManager.autoStartOnBootFlow } returns flowOf(false)
        every { preferencesManager.keepAlivePersistentServiceFlow } returns flowOf(true)

        val receiver = spyk(BootReceiver())
        val pendingResult: android.content.BroadcastReceiver.PendingResult = mockk(relaxed = true)
        every { receiver.goAsync() } returns pendingResult

        val intent: Intent = mockk()
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED

        receiver.onReceive(context, intent)

        Thread.sleep(300)

        verify(exactly = 0) {
            NotificationListenerService.requestRebind(any())
            ContextCompat.startForegroundService(any(), any())
        }
    }
}
