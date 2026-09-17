package com.angelsoft.macmirror

import com.angelsoft.macmirror.network.CompatibilityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityManagerTest {

    @Test
    fun testSameProtocolIsCompatible() {
        val result = CompatibilityManager.checkCompatibility(
            peerProtocolVersion = CompatibilityManager.CURRENT_PROTOCOL_VERSION,
            peerAppVersion = "1.0.0"
        )
        assertTrue(result.isCompatible)
        assertFalse(result.requiresCompanionUpdate)
        assertEquals("1.0.0", result.peerAppVersion)
        assertEquals(CompatibilityManager.CURRENT_PROTOCOL_VERSION, result.peerProtocolVersion)
    }

    @Test
    fun testLegacyPeerWithoutVersionDefaultsGracefully() {
        val result = CompatibilityManager.checkCompatibility(
            peerProtocolVersion = null,
            peerAppVersion = null
        )
        // Backward compatibility requirement: legacy builds must work without false warnings
        assertTrue(result.isCompatible)
        assertFalse(result.requiresCompanionUpdate)
        assertEquals(CompatibilityManager.DEFAULT_FALLBACK_PROTOCOL_VERSION, result.peerProtocolVersion)
        assertEquals(CompatibilityManager.DEFAULT_FALLBACK_APP_VERSION, result.peerAppVersion)
    }

    @Test
    fun testHigherProtocolIsCompatible() {
        // Forward compatibility: higher protocol version from peer should be accepted
        val result = CompatibilityManager.checkCompatibility(
            peerProtocolVersion = CompatibilityManager.CURRENT_PROTOCOL_VERSION + 1,
            peerAppVersion = "2.0.0"
        )
        assertTrue(result.isCompatible)
        assertFalse(result.requiresCompanionUpdate)
    }

    @Test
    fun testOlderIncompatibleProtocolTriggersUpdateRequirement() {
        // When peer protocol is strictly below minimum required
        val result = CompatibilityManager.checkCompatibility(
            peerProtocolVersion = CompatibilityManager.MIN_COMPATIBLE_MACOS_PROTOCOL - 1,
            peerAppVersion = "0.9.0"
        )
        assertFalse(result.isCompatible)
        assertTrue(result.requiresCompanionUpdate)
        assertEquals("0.9.0", result.peerAppVersion)
    }

    @Test
    fun testReleasesUrlIsConfigured() {
        assertTrue(CompatibilityManager.MACOS_RELEASES_URL.startsWith("https://github.com/"))
    }
}
