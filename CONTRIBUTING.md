# Contributing to MacMirror for Android 🤖

Thank you for your interest in contributing to **MacMirror for Android**! We welcome bug reports, feature proposals, localization contributions, and pull requests.

---

## 🗺️ Roadmap & Where to Start

Looking for something to work on?
- Check open issues tagged with [`good first issue`](https://github.com/angelvelasquezdev/mac-mirror-android/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) and [`help wanted`](https://github.com/angelvelasquezdev/mac-mirror-android/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22).
- Planned features:
  - 🔘 Android Quick Settings Tile to quickly toggle the mirroring service.
  - 🔋 Telemetry payload containing battery percentage and charging state.
  - 🚫 Per-app notification blacklisting / filtering in `AppsScreen.kt`.
  - 🌐 Additional localizations (French, German, Portuguese, Italian, Japanese).
  - 📋 Clipboard synchronization option.

---

## 🛠️ Development Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer / Ladybug / Koala.
- JDK 17 or JDK 21.
- Android SDK 34 / 35.
- A physical Android device running Android 9+ (API 28+) or an emulator with Wi-Fi network bridge.

### Getting the Code
\`\`\`bash
git clone https://github.com/angelvelasquezdev/mac-mirror-android.git
cd mac-mirror-android
\`\`\`

### Building from Command Line
\`\`\`bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test

# Install to connected device
./gradlew installDebug
\`\`\`

---

## 📐 Project Architecture & Key Packages

- `service/NotificationListener.kt`: Captures status bar notifications via `NotificationListenerService` and optionally runs elevated as a Foreground Service.
- `network/NsdHelper.kt`: Discovers the macOS receiver via Network Service Discovery (mDNS `_macmirror._tcp`).
- `security/CryptoManager.kt`: Generates ECDH key pairs and encrypts notification envelopes using AES-256-GCM.
- `data/PreferencesManager.kt`: Manages user settings and pairing keys using Jetpack DataStore / EncryptedSharedPreferences.
- `ui/screens/`: Pure Jetpack Compose declarative UI (`MainScreen.kt`, `AppsScreen.kt`, `SettingsScreen.kt`, `OnboardingScreen.kt`).
- `ui/components/CupertinoComponents.kt`: iOS-style inset-grouped components adhering strictly to `DESIGN_GUIDE.md`.

---

## 🌐 Internationalization (i18n)

MacMirror strictly enforces full i18n without hardcoded strings:
- Define all user-visible strings in:
  - `app/src/main/res/values/strings.xml` (Default English)
  - `app/src/main/res/values-es/strings.xml` (Spanish)
- In Compose, always retrieve strings using `stringResource(R.string.your_key)`.

---

## 🔀 Submitting a Pull Request

1. **Fork the repo** and create your branch from `develop`:
   \`\`\`bash
   git checkout -b feat/your-feature-name
   \`\`\`
2. **Follow Kotlin & Compose best practices**:
   - Every screen manages its own `Scaffold`.
   - Maintain accessible WCAG AA color contrast in Light and Dark themes.
3. **Commit clearly**:
   - Follow Conventional Commits: `feat: ...`, `fix: ...`, `docs: ...`, `refactor: ...`.
4. **Test your changes**:
   - Verify that `./gradlew test` passes and `./gradlew assembleDebug` succeeds without compiler warnings.
5. **Open a Pull Request**:
   - Reference any related issue (e.g. `Closes #2`).
