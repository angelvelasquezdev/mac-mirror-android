# MacMirror para Android

<p align="center">
  <strong>Reflejo seguro, privado y en tiempo real de notificaciones de Android a macOS a través de Wi-Fi local.</strong>
</p>

<p align="center">
  <a href="README.md">English</a> • <a href="README.es.md">Español</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Plataforma-Android%209.0%2B%20(API%2028%2B)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Versión de Android" />
  <img src="https://img.shields.io/badge/Lenguaje-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Licencia-MIT-blue?style=flat-square" alt="Licencia: MIT" />
  <a href="https://github.com/angelvelasquezdev/mac-mirror-macos"><img src="https://img.shields.io/badge/App%20Compañera-macOS%20MenuBar-000000?style=flat-square&logo=apple&logoColor=white" alt="Repo macOS" /></a>
</p>

---

## Descripción General

**MacMirror para Android** es la aplicación móvil cliente de MacMirror. Captura las notificaciones entrantes de tu dispositivo Android y las retransmite a tu Mac en tiempo real utilizando WebSockets locales sin servidores intermediarios en la nube.

Desarrollada con **Jetpack Compose** siguiendo una guía de diseño **Apple HIG Inset-Grouped**, ofrece una experiencia limpia, accesible y sin fricciones.

> [!IMPORTANT]
> Esta aplicación requiere su contraparte para macOS para funcionar:
> 👉 **[Repositorio MacMirror para macOS](https://github.com/angelvelasquezdev/mac-mirror-macos)**

---

## ✨ Características Principales

- 🔒 **Cifrado Extremo a Extremo (E2EE)**:
  - Intercambio de claves efímero ECDH (Curva P-256).
  - Derivación de clave de sesión mediante HKDF-SHA256.
  - Cifrado autenticado de notificaciones con AES-256-GCM.
- ⚡ **Cero Dependencia de la Nube**: Funciona exclusivamente a través de tu red Wi-Fi local mediante descubrimiento Bonjour / mDNS y WebSockets cifrados. Tus datos nunca salen de tu red local.
- 📱 **Control y Filtro por Aplicación**: Elige exactamente qué aplicaciones tienen permitido reflejar notificaciones, con caché asíncrona de íconos y búsqueda reactiva instantánea.
- 🎨 **Diseño Inspirado en Apple HIG**: Tarjetas agrupadas (Inset-Grouped), barra de navegación inferior segmentada estilo iOS, soporte completo de temas Claro y Oscuro, y contrastes accesibles conformes a WCAG AA.
- 🔄 **Desvinculación Bidireccional**: Si te desvinculas en Android, la app de macOS se desvincula automáticamente al instante, y viceversa.
- 🌐 **Internacionalización Completa (i18n)**: Soporte nativo para Español e Inglés.

---

## 🏗 Arquitectura y Flujo de Seguridad

```
┌─────────────────┐                                  ┌─────────────────┐
│ Dispositivo     │                                  │   Mac (Servidor)│
│ Android         │                                  │                 │
└────────┬────────┘                                  └────────┬────────┘
         │                                                    │
         │ 1. Descubrimiento por Bonjour / mDNS               │
         │───────────────────────────────────────────────────>│
         │                                                    │
         │ 2. Intercambio ECDH P-256 + Autenticación PIN      │
         │<──────────────────────────────────────────────────>│
         │    [Derivación de clave compartida: HKDF-SHA256]   │
         │                                                    │
         │ 3. Conexión WebSocket (ws://<mac-ip>:50002)        │
         │───────────────────────────────────────────────────>│
         │                                                    │
         │ 4. Flujo de Notificaciones Cifradas (AES-256-GCM)  │
         │    Carga útil: { iv, ciphertext, tag }             │
         │───────────────────────────────────────────────────>│
         │                                                    │
```

1. **Descubrimiento**: Usa `NsdManager` (Network Service Discovery) para localizar la Mac en la red local bajo el servicio `_macmirror._tcp`.
2. **Vinculación**: Handshake seguro sobre HTTP (`POST /pair/start` y `POST /pair/verify`). La Mac genera un PIN criptográfico de 6 dígitos mostrado en la barra de menú. Ambos dispositivos verifican su identidad mediante ECDH P-256 y derivan la clave de sesión AES-256-GCM almacenada en `EncryptedSharedPreferences`.
3. **Reflejo**: El servicio `NotificationListenerService` captura las alertas de la barra de estado, filtra según tus preferencias, cifra el contenido (título, texto e ícono) y lo transmite por WebSocket a macOS.

---

## 📋 Requisitos del Sistema

- **Dispositivo Android**: Android 9.0 (API Nivel 28) o superior.
- **Red Local**: El teléfono y la Mac deben estar conectados a la misma red Wi-Fi (o subred accesible).
- **Permisos Necesarios**:
  - `Acceso a Notificaciones` (`android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`): Para capturar las notificaciones recibidas.
  - `Acceso a Wi-Fi e Internet`: Para establecer comunicación local por sockets con la Mac.
  - `Mostrar Notificaciones` (Android 13+): Para el indicador del servicio de primer plano.

---

## 🚀 Compilación e Instalación

### 1. Clonar el Repositorio
```bash
git clone https://github.com/angelvelasquezdev/mac-mirror-android.git
cd mac-mirror-android
```

### 2. Compilar con Gradle
Puedes abrir la carpeta del proyecto en **Android Studio** o ejecutar en la terminal:

```bash
# Compilar el APK de depuración
./gradlew assembleDebug

# Ejecutar pruebas unitarias
./gradlew test

# Instalar directamente en un dispositivo conectado
./gradlew installDebug
```

---

## 🔗 Proyectos Relacionados

| Proyecto | Descripción | Repositorio |
| :--- | :--- | :--- |
| **MacMirror (macOS)** | Aplicación nativa para la barra de menú de macOS | [angelvelasquezdev/mac-mirror-macos](https://github.com/angelvelasquezdev/mac-mirror-macos) |

---

## 📄 Licencia

Este proyecto está bajo la **Licencia MIT**. Consulta el archivo [LICENSE](LICENSE) para más detalles.

Copyright (c) 2026 Ángel Velásquez
