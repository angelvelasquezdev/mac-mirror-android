package com.angelsoft.macmirror.network

import com.angelsoft.macmirror.BuildConfig

object CompatibilityManager {
    /**
     * Internal protocol version of this Android build.
     * Increment when introducing protocol or wire-format changes.
     */
    const val CURRENT_PROTOCOL_VERSION = 1

    /**
     * Minimum macOS protocol version required to communicate with this Android build.
     * Only increment if older macOS versions are strictly incapable of operating with this build.
     */
    const val MIN_COMPATIBLE_MACOS_PROTOCOL = 1

    /**
     * Fallback values for legacy companion apps that do not send protocol or app versions.
     */
    const val DEFAULT_FALLBACK_PROTOCOL_VERSION = 1
    const val DEFAULT_FALLBACK_APP_VERSION = "1.0.0"

    /**
     * URL to macOS releases page for user updates.
     */
    const val MACOS_RELEASES_URL = "https://github.com/angelvelasquezdev/mac-mirror-macos/releases"

    /**
     * Current display app version from build configuration.
     */
    val currentAppVersion: String
        get() = try {
            BuildConfig.VERSION_NAME.ifBlank { DEFAULT_FALLBACK_APP_VERSION }
        } catch (_: Throwable) {
            DEFAULT_FALLBACK_APP_VERSION
        }

    data class CompatibilityResult(
        val isCompatible: Boolean,
        val peerProtocolVersion: Int,
        val peerAppVersion: String,
        val requiresCompanionUpdate: Boolean
    )

    /**
     * Validates whether the companion macOS app is compatible with this Android app.
     * If the companion does not provide a protocol version (legacy client), it defaults
     * gracefully to version 1.0.0 / protocol 1 without blocking or raising false alarms.
     */
    fun checkCompatibility(
        peerProtocolVersion: Int?,
        peerAppVersion: String?
    ): CompatibilityResult {
        val effectiveProtocol = peerProtocolVersion ?: DEFAULT_FALLBACK_PROTOCOL_VERSION
        val effectiveVersion = peerAppVersion?.takeIf { it.isNotBlank() } ?: DEFAULT_FALLBACK_APP_VERSION

        val isCompatible = effectiveProtocol >= MIN_COMPATIBLE_MACOS_PROTOCOL
        val requiresCompanionUpdate = !isCompatible

        return CompatibilityResult(
            isCompatible = isCompatible,
            peerProtocolVersion = effectiveProtocol,
            peerAppVersion = effectiveVersion,
            requiresCompanionUpdate = requiresCompanionUpdate
        )
    }
}
