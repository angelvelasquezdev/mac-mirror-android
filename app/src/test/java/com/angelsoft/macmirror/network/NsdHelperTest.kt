package com.angelsoft.macmirror.network

import android.content.Context
import android.net.nsd.NsdManager
import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test

class NsdHelperTest {

    private val nsdManager: NsdManager = mockk(relaxed = true)
    private val context: Context = mockk {
        every { getSystemService(Context.NSD_SERVICE) } returns nsdManager
    }
    private lateinit var nsdHelper: NsdHelper

    @Before
    fun setUp() {
        nsdHelper = NsdHelper(context)
    }

    @Test
    fun `restartDiscovery stops an already-running session before starting a new one`() {
        // Given a discovery session that is already running
        nsdHelper.startDiscovery()

        // When restarting discovery
        nsdHelper.restartDiscovery()

        // Then the old session is explicitly stopped before a new one starts, rather than
        // being skipped by startDiscovery()'s own already-running guard
        verifyOrder {
            nsdManager.discoverServices(any<String>(), any<Int>(), any<NsdManager.DiscoveryListener>())
            nsdManager.stopServiceDiscovery(any<NsdManager.DiscoveryListener>())
            nsdManager.discoverServices(any<String>(), any<Int>(), any<NsdManager.DiscoveryListener>())
        }
    }

    @Test
    fun `restartDiscovery starts a fresh session even when nothing was running`() {
        // Given no discovery session has been started yet

        // When restarting discovery
        nsdHelper.restartDiscovery()

        // Then it starts one session and never attempts to stop a session that never existed
        verify(exactly = 0) { nsdManager.stopServiceDiscovery(any<NsdManager.DiscoveryListener>()) }
        verify(exactly = 1) { nsdManager.discoverServices(any<String>(), any<Int>(), any<NsdManager.DiscoveryListener>()) }
    }

    @Test
    fun `startDiscovery is a no-op while a session is already running`() {
        // Given a discovery session that is already running
        nsdHelper.startDiscovery()

        // When starting discovery again without restarting
        nsdHelper.startDiscovery()

        // Then the second call is skipped, unlike restartDiscovery()
        verify(exactly = 1) { nsdManager.discoverServices(any<String>(), any<Int>(), any<NsdManager.DiscoveryListener>()) }
    }
}
