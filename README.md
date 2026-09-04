# MacMirror for Android

<p align="center">
  <strong>Seamless, private, real-time Android notification mirroring to macOS over local Wi-Fi.</strong>
</p>

<p align="center">
  <a href="README.md">English</a> • <a href="README.es.md">Español</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%209.0%2B%20(API%2028%2B)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android Version" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License: MIT" />
  <a href="https://github.com/angelvelasquezdev/mac-mirror-macos"><img src="https://img.shields.io/badge/Companion%20App-macOS%20MenuBar-000000?style=flat-square&logo=apple&logoColor=white" alt="macOS Repo" /></a>
</p>

---

## Overview

**MacMirror for Android** is the mobile companion client for MacMirror. It captures incoming notifications on your Android device and mirrors them directly to your Mac in real time using local WebSockets and zero cloud servers.

Built with **Jetpack Compose** following an **Apple HIG Inset-Grouped** design system, it delivers a smooth, clean, and distraction-free native experience.

> [!IMPORTANT]
> This app requires the companion macOS menu bar app to function:
> 👉 **[MacMirror for macOS Repository](https://github.com/angelvelasquezdev/mac-mirror-macos)**

---

## ✨ Features

- 🔒 **End-to-End Encryption (E2EE)**:
  - Ephemeral ECDH (Curve P-256) key agreement.
  - HKDF-SHA256 session key derivation.
  - AES-256-GCM authenticated payload encryption.
- ⚡ **Zero Cloud Dependency**: Operates exclusively over your local Wi-Fi network via Bonjour / mDNS network discovery and secure WebSockets. No data ever leaves your local network.
- 🛡️ **Reliable Background Execution**:
  - **Doze Mode & Battery Saver Exemption**: One-tap native prompt to disable battery optimization (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), preventing Android from cutting local Wi-Fi transmission when the device is locked/sleeping. Includes an intelligent, auto-dismissing warning card on the main screen.
  - **Foreground Service & Silent Persistent Notification**: Optional background service using Android's modern `connectedDevice` foreground service type (`FOREGROUND_SERVICE_CONNECTED_DEVICE`) with a silent notification (`IMPORTANCE_LOW`). Keeps the process alive against aggressive OEM task killers (MIUI/HyperOS, One UI, ColorOS) and dynamically reflects connection status ("Connected to Mac" / "Standby").
  - **Auto-Protection for Low Latency**: Enabling Low Latency (WebSockets) automatically turns on the persistent service to ensure the socket stays connected 24/7.
- 📱 **Per-App Filter Controls**: Selectively enable or disable notification mirroring per application with fast, asynchronous icon caching and real-time search.
- 🎨 **Apple HIG-Inspired Design**: Native Inset-Grouped card layout, custom iOS-style segmented navigation bar, smooth transitions, and high-contrast accessible typography (WCAG AA compliant).
- 🔄 **Bidirectional Unpairing**: Unpairing from Android automatically resets the macOS server, and unpairing from macOS immediately notifies Android.
- 🌐 **Full Internationalization (i18n)**: Out-of-the-box support for English and Spanish.

---

## 🏗 Architecture & Security Flow

```
┌─────────────────┐                                  ┌─────────────────┐
│ Android Device  │                                  │   Mac (Server)  │
└────────┬────────┘                                  └────────┬────────┘
         │                                                    │
         │ 1. Discover via Bonjour / mDNS (_macmirror._tcp)   │
         │───────────────────────────────────────────────────>│
         │                                                    │
         │ 2. ECDH P-256 Key Exchange + 6-digit PIN Auth      │
         │<──────────────────────────────────────────────────>│
         │    [Derive Shared Symmetric Key via HKDF-SHA256]   │
         │                                                    │
         │ 3. WebSocket Connection (ws://<mac-ip>:50002)      │
         │───────────────────────────────────────────────────>│
         │                                                    │
         │ 4. Encrypted Notification Stream (AES-256-GCM)     │
         │    Payload: { iv, ciphertext, tag }                │
         │───────────────────────────────────────────────────>│
         │                                                    │
```

1. **Discovery**: Uses `NsdManager` (Network Service Discovery) to detect the Mac on the local network (`_macmirror._tcp`).
2. **Pairing**: Secure handshake over HTTP (`POST /pair/initiate` and `POST /pair/confirm`). The Mac generates a 6-digit cryptographic PIN displayed on the Menu Bar. Both devices verify identity using ECDH P-256 and derive an AES-256-GCM key stored in Android Keystore / DataStore.
3. **Mirroring & Background Reliability**:
   - Android's `NotificationListenerService` captures status bar events, checks the per-app whitelist, encrypts the title, text, and icon, and sends them via WebSocket (or HTTP fallback) to macOS.
   - When elevated via the **Background Service** toggle, `NotificationListener` runs as a Foreground Service with type `connectedDevice`, displaying a quiet status notification and preventing process termination under memory pressure.
   - With **Battery Optimization disabled**, Android's Doze Mode allows network sockets to remain active and send notifications even when the screen is turned off.

---

## 📋 Requirements

- **Android Device**: Android 9.0 (API Level 28) or later (fully compatible with Android 14, 15, and 16).
- **Local Network**: Android device and Mac must be connected to the same Wi-Fi network (or reachable subnet).
- **Permissions Required**:
  - `Notification Listener Access` (`android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`): To capture status bar notifications.
  - `Battery Optimization Exemption` (`android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`): Prevents Doze Mode from freezing network transmission during sleep.
  - `Foreground Service & Connected Device` (`android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE`): Required on Android 14+ for background connection priority.
  - `Post Notifications` (`android.permission.POST_NOTIFICATIONS`): For silent foreground service status indicator and test alerts.
  - `Access Wi-Fi State & Internet`: For local socket communication with the Mac.

---

## 🚀 Getting Started & Building

### 1. Clone the Repository
```bash
git clone https://github.com/angelvelasquezdev/mac-mirror-android.git
cd mac-mirror-android
```

### 2. Build via Gradle
You can open the project in **Android Studio** or build from the command line:

```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test

# Install directly to a connected device
./gradlew installDebug
```

---

## 🔗 Related Projects

| Project | Description | Repository |
| :--- | :--- | :--- |
| **MacMirror (macOS)** | Native macOS Menu Bar receiver application | [angelvelasquezdev/mac-mirror-macos](https://github.com/angelvelasquezdev/mac-mirror-macos) |

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

Copyright (c) 2026 Ángel Velásquez
