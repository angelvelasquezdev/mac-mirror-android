# Google Play Store Listing & Metadata — MacMirror

This document contains the official store listing copy and asset specifications for **MacMirror** on the Google Play Store in both **English (en-US)** and **Spanish (es-ES)**.

---

## 📱 Quick Reference & Asset Specifications

| Asset | Requirement / Limit | Status / File Path |
|---|---|---|
| **App Icon** | 512 × 512 px (PNG 32-bit, max 1024KB) | `app_icon.png` (root, en-US, es-ES) |
| **App Title** | Max 30 characters | `en-US/title.txt`, `es-ES/title.txt` |
| **Short Description** | Max 80 characters | `en-US/short_description.txt`, `es-ES/short_description.txt` |
| **Full Description** | Max 4000 characters | `en-US/full_description.txt`, `es-ES/full_description.txt` |
| **Feature Graphic** | 1024 × 500 px (PNG/JPEG, no alpha) | `en-US/feature_graphic.png`, `es-ES/feature_graphic.png` |
| **Screenshots (5)** | 1080 × 2400 px (16:9 to 20:9 ratio) | `en-US/screenshot_*.png`, `es-ES/screenshot_*.png` |
| **Category** | Productivity / Tools | Selected in Play Console |
| **Content Rating** | Everyone | Questionnaire in Play Console |

---

## 🇺🇸 English Listing (`en-US`)

### Title (25 / 30 chars)
```
MacMirror: Android to Mac
```

### Short Description (76 / 80 chars)
```
Mirror Android notifications to your Mac menu bar securely over local Wi-Fi.
```

### Full Description (2,729 / 4,000 chars)
```
MacMirror bridges your Android smartphone and your Mac with unmatched elegance, speed, and privacy. Stream incoming notifications directly to your macOS menu bar in real time—without relying on third-party cloud servers or external accounts.

IMPORTANT REQUIREMENT:
MacMirror for Android is the companion app to MacMirror for macOS. To receive notifications, you must install and run the MacMirror menu bar companion app on your Mac connected to the same Wi-Fi network.
MacMirror for macOS is free and open source:
👉 https://github.com/angelvelasquezdev/mac-mirror-macos

100% PRIVATE, LOCAL & ZERO-CLOUD
Unlike standard push notification services, MacMirror operates strictly within your local Wi-Fi network. Your personal alerts, messages, and calls never pass through cloud servers or third-party databases.

MILITARY-GRADE END-TO-END ENCRYPTION (E2EE)
Security is built into MacMirror by design:
• Ephemeral ECDH (Curve P-256) cryptographic key agreement during pairing.
• HKDF-SHA256 session key derivation.
• AES-256-GCM authenticated payload encryption.
• Hardware-backed key protection using Android KeyStore.

SEAMLESS PAIRING WITH ZERO CONFIGURATION
No complicated IP setups required. MacMirror uses Bonjour (mDNS) to automatically discover your Mac on the local network. Simply enter the 6-digit PIN displayed in your Mac's menu bar to establish an encrypted, paired connection in seconds.

GRANULAR PER-APP NOTIFICATION CONTROL
You decide what reaches your desktop. Enable or disable mirroring on a per-application basis:
• Instant search across all installed apps.
• One-tap "Select All" or "Select None" filters.
• Fast asynchronous app icon caching for smooth scrolling.

ULTRA-LOW LATENCY WITH WEBSOCKETS
Built with persistent, lightweight WebSockets for sub-second notification delivery. When battery saving is prioritized, MacMirror seamlessly adapts to efficient HTTP mode.

ELEGANT NATIVE DESIGN
Crafted with Jetpack Compose following Apple Human Interface Guidelines (HIG) Inset-Grouped cards and Material You accents. Enjoy fluid transitions, high contrast, and full Dark Mode support.

100% OPEN SOURCE & TRANSPARENT
MacMirror is proud to be open-source under the MIT License. Both the Android client and macOS companion app codebases are fully auditable:
• Android Client: https://github.com/angelvelasquezdev/mac-mirror-android
• macOS Server: https://github.com/angelvelasquezdev/mac-mirror-macos

PERMISSIONS DISCLOSURE:
• Notification Listener Access: Required to read incoming notifications and forward them to your authorized Mac. No notification contents are ever stored or uploaded to external servers.
• Local Network / Wi-Fi: Required to communicate securely with your Mac on the local network.
```

---

## 🇪🇸 Spanish Listing (`es-ES`)

### Título (29 / 30 caracteres)
```
MacMirror: Notificaciones Mac
```

### Descripción corta (78 / 80 caracteres)
```
Notificaciones de Android en tu Mac por Wi-Fi local de forma privada y segura.
```

### Descripción completa (2,755 / 4,000 caracteres)
```
MacMirror conecta tu teléfono Android y tu Mac con máxima elegancia, velocidad y privacidad. Recibe tus notificaciones en la barra de menú de macOS en tiempo real, sin depender de servidores en la nube ni cuentas de terceros.

REQUISITO INDISPENSABLE:
MacMirror para Android requiere la aplicación complementaria para macOS. Para recibir notificaciones en tu Mac, debes instalar y abrir MacMirror en tu Mac conectado a la misma red Wi-Fi.
MacMirror para macOS es gratuita y de código abierto:
👉 https://github.com/angelvelasquezdev/mac-mirror-macos

100% PRIVADO, LOCAL Y SIN NUBE
A diferencia de otros servicios de sincronización, MacMirror funciona exclusivamente dentro de tu red Wi-Fi local. Tus mensajes, llamadas y alertas nunca viajan por servidores externos ni quedan almacenados en la nube.

CIFRADO EXTREMO A EXTREMO (E2EE)
La seguridad está integrada desde el núcleo:
• Intercambio de claves efímero mediante ECDH (Curva P-256).
• Derivación de claves de sesión con HKDF-SHA256.
• Cifrado autenticado de notificaciones con AES-256-GCM.
• Protección de claves en el enclave seguro con Android KeyStore.

VINCULACIÓN RÁPIDA CON CÓDIGO PIN
Olvídate de configuraciones complicadas. MacMirror detecta automáticamente tu Mac en la red mediante Bonjour (mDNS). Solo introduce el código PIN de 6 dígitos que aparece en la barra de menú de tu Mac y la conexión cifrada quedará lista en segundos.

CONTROL SELECTIVO POR APLICACIÓN
Tú decides qué alertas llegan a tu pantalla. Controla el filtrado individualmente:
• Búsqueda instantánea entre todas tus aplicaciones instaladas.
• Botones rápidos para seleccionar o deseleccionar todas las apps.
• Caché asíncrona de iconos para un desplazamiento suave y fluido.

BAJA LATENCIA CON WEBSOCKETS
Diseñado con un canal persistente de WebSockets para una entrega de notificaciones en menos de un segundo. También incluye modo de ahorro de batería inteligente por HTTP.

DISEÑO NATIVO ELEGANTE
Creado con Jetpack Compose siguiendo las guías de diseño de Apple HIG (tarjetas Inset-Grouped) y Material You. Totalmente adaptado para Modo Claro y Modo Oscuro con contrastes óptimos.

100% CÓDIGO ABIERTO Y TRANSPARENTE
MacMirror es un proyecto libre bajo licencia MIT. Tanto el cliente Android como el servidor macOS son de código abierto y auditables:
• Repositorio Android: https://github.com/angelvelasquezdev/mac-mirror-android
• Repositorio macOS: https://github.com/angelvelasquezdev/mac-mirror-macos

TRANSPARENCIA EN PERMISOS:
• Acceso a Notificaciones: Necesario para leer las notificaciones entrantes y enviarlas a tu Mac autorizado. Ninguna notificación se almacena ni se comparte externamente.
• Red Local / Wi-Fi: Necesario para comunicarse de forma segura con tu Mac en tu red doméstica o de oficina.
```

---

## 🎨 Screenshot Structure & Messaging

| # | File Name | Screen Basis | English Message (`en-US`) | Spanish Message (`es-ES`) |
|---|---|---|---|---|
| **1** | `screenshot_1.png` | Welcome Overview | **Real-Time Mac Mirroring**<br>*Stream phone notifications to macOS menu bar* | **Tus Alertas en tu Mac**<br>*Notificaciones al instante en la barra de menú* |
| **2** | `screenshot_2.png` | PIN Pairing | **Pair in Seconds via PIN**<br>*Automatic Bonjour discovery over local Wi-Fi* | **Conéctate con un PIN Seguro**<br>*Detección automática por red local Wi-Fi* |
| **3** | `screenshot_3.png` | 100% Private | **100% Private & Local**<br>*End-to-end AES-256-GCM encryption, zero cloud* | **100% Privado y Local**<br>*Cifrado extremo a extremo sin servidores externos* |
| **4** | `screenshot_4.png` | App Whitelist | **Selective App Filtering**<br>*Control which apps mirror to your Mac* | **Filtro Selectivo por App**<br>*Elige qué aplicaciones envían alertas a tu Mac* |
| **5** | `screenshot_5.png` | Settings | **WebSockets & KeyStore**<br>*Ultra-fast persistent channel & hardware keys* | **Baja Latencia y KeyStore**<br>*Canal persistente ultrarrápido y claves seguras* |

---

## 🖼 Feature Graphic Concept (1024 × 500 px)

- **Left:** Official MacMirror app icon (`ic_launcher-playstore.png`) in an elevated squircle with rounded corners (rx=20) and ambient glow.
- **Center:** Brand title **MacMirror**, localized subtitle ("Android to Mac Notification Bridge" / "Puente de Notificaciones Android a Mac"), and trust badges (`OPEN SOURCE`, `ZERO CLOUD`, `AES-256-GCM`).
- **Right:** High-tech representation of the wireless bridge streaming alerts between an Android phone and a Mac display.

---

## 🚀 Automated Google Play Synchronization Tool

The script `android/scripts/playstore-sync.mjs` allows synchronizing all store metadata, official icon, feature graphic, and screenshots directly with Google Play Console using the Google Play Developer Publishing API v3.

### Quick Commands

```bash
# 1. Validate changes with Google Play without publishing (Dry Run)
node android/scripts/playstore-sync.mjs --dry-run

# 2. Live publish all metadata, icon, feature graphic, and screenshots
node android/scripts/playstore-sync.mjs

# 3. Synchronize only text metadata (titles & descriptions)
node android/scripts/playstore-sync.mjs --metadata-only

# 4. Synchronize only graphics (icon & feature graphic)
node android/scripts/playstore-sync.mjs --graphics-only

# 5. Synchronize only screenshots
node android/scripts/playstore-sync.mjs --screenshots-only

# 6. Synchronize a specific language
node android/scripts/playstore-sync.mjs --lang en-US
```

### Google Play Console Service Account Setup
The script uses `android/.secrets/play-store-key.json` (`macmirror-codemagic-publisher@macmirror.iam.gserviceaccount.com`).
To allow the service account to update store listings:
1. Go to **Google Play Console** -> **Users and permissions** (*Usuarios y permisos*).
2. Click on `macmirror-codemagic-publisher@macmirror.iam.gserviceaccount.com`.
3. Under **App permissions**, ensure **MacMirror** (`com.angelsoft.macmirror`) is added.
4. Under **Permissions**, verify the following checkboxes are enabled:
   - ☑️ **Edit store listings, pricing, and distribution** (*Editar fichas de Play Store, precios y distribución*).
   - ☑️ **Manage app releases** / **Release apps to production** (*Lanzar aplicaciones a producción*).
5. Click **Save changes** (*Guardar cambios*).
