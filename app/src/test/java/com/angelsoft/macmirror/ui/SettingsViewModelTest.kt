package com.angelsoft.macmirror.ui

import android.content.Context
import com.angelsoft.macmirror.data.PreferencesManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk(relaxed = true)
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val autoStartFlow = MutableStateFlow(false)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { preferencesManager.autoStartOnBootFlow } returns autoStartFlow
        every { preferencesManager.lowLatencyModeFlow } returns MutableStateFlow(false)
        every { preferencesManager.keepAlivePersistentServiceFlow } returns MutableStateFlow(false)
        every { preferencesManager.themeModeFlow } returns MutableStateFlow(0)
        every { preferencesManager.optOutAppsFlow } returns MutableStateFlow(emptySet())

        viewModel = SettingsViewModel(context, preferencesManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun testAutoStartOnBootReflectsPreferences() = runTest(testDispatcher) {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.autoStartOnBoot.collect {}
        }

        testScheduler.advanceUntilIdle()
        assertEquals(false, viewModel.autoStartOnBoot.value)

        autoStartFlow.value = true
        testScheduler.advanceUntilIdle()
        assertEquals(true, viewModel.autoStartOnBoot.value)

        job.cancel()
    }

    @Test
    fun testToggleAutoStartOnBootCallsPreferences() = runTest(testDispatcher) {
        coEvery { preferencesManager.setAutoStartOnBoot(any()) } just Runs

        viewModel.toggleAutoStartOnBoot(true)
        testScheduler.advanceUntilIdle()

        coVerify { preferencesManager.setAutoStartOnBoot(true) }

        viewModel.toggleAutoStartOnBoot(false)
        testScheduler.advanceUntilIdle()

        coVerify { preferencesManager.setAutoStartOnBoot(false) }
    }
}
