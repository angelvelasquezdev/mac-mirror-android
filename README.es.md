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
- 🛡️ **Ejecución Fiable en Segundo Plano**:
  - **Exención de Ahorro de Batería (Doze Mode)**: Solicitud nativa en un toque para omitir optimizaciones de batería (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), evitando que Android corte el tráfico Wi-Fi local con la pantalla apagada o en reposo. Incluye tarjeta inteligente de advertencia en la pantalla principal que se oculta automáticamente al concederlo.
  - **Servicio en Primer Plano y Notificación Persistente Silenciosa**: Servicio opcional mediante el tipo moderno `connectedDevice` (`FOREGROUND_SERVICE_CONNECTED_DEVICE`) con notificación silenciosa (`IMPORTANCE_LOW`). Previene el cierre del proceso ante asesinos de tareas agresivos de fabricantes (MIUI/HyperOS, One UI, ColorOS) y muestra el estado en tiempo real ("Conectado a Mac" / "En espera").
  - **Auto-Protección para Baja Latencia**: Al activar el modo de Baja Latencia (WebSockets), se enciende automáticamente el servicio en segundo plano para garantizar la estabilidad del socket 24/7.
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
2. **Vinculación**: Handshake seguro sobre HTTP (`POST /pair/initiate` y `POST /pair/confirm`). La Mac genera un PIN criptográfico de 6 dígitos mostrado en la barra de menú. Ambos dispositivos verifican su identidad mediante ECDH P-256 y derivan la clave de sesión AES-256-GCM almacenada en Android Keystore / DataStore.
3. **Reflejo y Fiabilidad en Segundo Plano**:
   - El servicio `NotificationListenerService` captura las alertas de la barra de estado, filtra según tus preferencias, cifra el contenido (título, texto e ícono) y lo transmite por WebSocket (o fallback HTTP) a macOS.
   - Al activarse el switch de **Servicio en segundo plano**, `NotificationListener` se eleva a Foreground Service con tipo `connectedDevice`, mostrando una notificación silenciosa y evitando que el sistema cierre el proceso por falta de memoria.
   - Con el **Ahorro de Batería desactivado**, el Doze Mode de Android permite mantener abiertos los sockets de red y transmitir alertas incluso con el móvil bloqueado en reposo.

---

## 📋 Requisitos del Sistema

- **Dispositivo Android**: Android 9.0 (API Nivel 28) o superior (totalmente compatible con Android 14, 15 y 16).
- **Red Local**: El teléfono y la Mac deben estar conectados a la misma red Wi-Fi (o subred accesible).
- **Permisos Necesarios**:
  - `Acceso a Notificaciones` (`android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`): Para capturar las notificaciones recibidas.
  - `Exención de Ahorro de Batería` (`android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`): Previene que Doze Mode corte la conexión de red en reposo.
  - `Servicio en Primer Plano y Dispositivo Conectado` (`android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE`): Requerido en Android 14+ para otorgar alta prioridad a la conexión.
  - `Mostrar Notificaciones` (`android.permission.POST_NOTIFICATIONS`): Para el indicador silencioso del servicio en primer plano y alertas de prueba.
  - `Acceso a Wi-Fi e Internet`: Para establecer comunicación local por sockets con la Mac.

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
