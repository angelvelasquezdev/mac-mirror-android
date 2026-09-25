# Contribuir a MacMirror para Android 🤖

<p align="center">
  <a href="CONTRIBUTING.md">English</a> • <a href="CONTRIBUTING.es.md">Español</a>
</p>

¡Gracias por tu interés en contribuir a **MacMirror para Android**! Agradecemos reportes de errores, propuestas de nuevas funcionalidades, traducciones y pull requests.

---

## 🗺️ Hoja de Ruta y Por Dónde Empezar

¿Buscas algo en lo que trabajar?
- Revisa las issues abiertas etiquetadas con [`good first issue`](https://github.com/angelvelasquezdev/mac-mirror-android/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) y [`help wanted`](https://github.com/angelvelasquezdev/mac-mirror-android/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22).
- Funcionalidades planificadas:
  - 🔘 Mosaico de Ajustes Rápidos (Quick Settings Tile) de Android para activar/desactivar rápidamente el servicio de reflejo.
  - 🔋 Carga útil de telemetría que contenga el porcentaje de batería y estado de carga.
  - 🚫 Lista negra / filtrado de notificaciones por aplicación en `AppsScreen.kt`.
  - 🌐 Localizaciones adicionales (francés, alemán, portugués, italiano, japonés).
  - 📋 Opción de sincronización de portapapeles.

---

## 🛠️ Configuración del Entorno de Desarrollo

### Requisitos previos
- Android Studio Hedgehog (2023.1.1) o más reciente / Ladybug / Koala.
- JDK 17 o JDK 21.
- Android SDK 34 / 35.
- Un dispositivo físico Android con Android 9+ (API 28+) o un emulador con puente de red Wi-Fi.

### Obtener el código
```bash
git clone https://github.com/angelvelasquezdev/mac-mirror-android.git
cd mac-mirror-android
```

### Compilar desde la línea de comandos
```bash
# Compilar APK de depuración (Debug)
./gradlew assembleDebug

# Ejecutar pruebas unitarias
./gradlew test

# Instalar en el dispositivo conectado
./gradlew installDebug
```

---

## 📐 Arquitectura del Proyecto y Paquetes Clave

- `service/NotificationListener.kt`: Captura las notificaciones de la barra de estado mediante `NotificationListenerService` y opcionalmente se ejecuta en primer plano (Foreground Service).
- `network/NsdHelper.kt`: Descubre el receptor de macOS a través de Network Service Discovery (mDNS `_macmirror._tcp`).
- `security/CryptoManager.kt`: Genera pares de claves ECDH y cifra los paquetes de notificación usando AES-256-GCM.
- `data/PreferencesManager.kt`: Administra la configuración del usuario y claves de emparejamiento con Jetpack DataStore / EncryptedSharedPreferences.
- `ui/screens/`: Interfaz declarativa pura en Jetpack Compose (`MainScreen.kt`, `AppsScreen.kt`, `SettingsScreen.kt`, `OnboardingScreen.kt`).
- `ui/components/CupertinoComponents.kt`: Componentes inset-grouped estilo iOS que siguen estrictamente la guía `DESIGN_GUIDE.md`.

---

## 🌐 Internacionalización (i18n)

MacMirror aplica estrictamente i18n sin cadenas de texto hardcodeadas:
- Define todas las cadenas visibles para el usuario en:
  - `app/src/main/res/values/strings.xml` (Inglés por defecto)
  - `app/src/main/res/values-es/strings.xml` (Español)
- En Compose, obtén siempre las cadenas mediante `stringResource(R.string.tu_clave)`.

---

## 🔀 Enviar un Pull Request

1. **Haz un fork del repositorio** y crea tu rama a partir de `develop`:
   ```bash
   git checkout -b feat/nombre-de-tu-funcionalidad
   ```
2. **Sigue las mejores prácticas de Kotlin y Compose**:
   - Cada pantalla debe gestionar su propio `Scaffold`.
   - Mantén contraste accesible WCAG AA en temas Claro y Oscuro.
3. **Realiza commits claros**:
   - Sigue los Conventional Commits: `feat: ...`, `fix: ...`, `docs: ...`, `refactor: ...`.
4. **Prueba tus cambios**:
   - Verifica que `./gradlew test` pase y `./gradlew assembleDebug` compile sin advertencias.
5. **Abre un Pull Request**:
   - Haz referencia a cualquier issue relacionada (ej. `Closes #2`).
