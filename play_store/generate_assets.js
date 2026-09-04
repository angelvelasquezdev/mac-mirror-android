
function escapeXml(unsafe) {
  if (!unsafe) return '';
  return String(unsafe)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const BASE_DIR = path.resolve(__dirname, '..');
const SCREENSHOTS_DIR = path.join(BASE_DIR, 'screenshots');
const OUTPUT_DIR = __dirname;

// Raw screenshot files mapping
const RAW_FILES = {
  welcome: path.join(SCREENSHOTS_DIR, 'Screenshot_2026-09-04-11-21-51-230_com.angelsoft.macmirror.jpg'),
  privacy: path.join(SCREENSHOTS_DIR, 'Screenshot_2026-09-04-11-21-53-319_com.angelsoft.macmirror.jpg'),
  pairing: path.join(SCREENSHOTS_DIR, 'Screenshot_2026-09-04-11-22-20-962_com.angelsoft.macmirror.jpg'),
  apps: path.join(SCREENSHOTS_DIR, 'Screenshot_2026-09-04-11-22-40-185_com.angelsoft.macmirror.jpg'),
  settings: path.join(SCREENSHOTS_DIR, 'Screenshot_2026-09-04-11-22-45-113_com.angelsoft.macmirror.jpg')
};

// Ensure all raw files exist
for (const [key, filePath] of Object.entries(RAW_FILES)) {
  if (!fs.existsSync(filePath)) {
    console.error(`Missing raw screenshot for ${key}: ${filePath}`);
    process.exit(1);
  }
}

// Pre-read and base64 encode raw screenshots

const OFFICIAL_ICON_PATH = path.join(BASE_DIR, 'app/src/main/ic_launcher-playstore.png');
const OFFICIAL_ICON_BASE64 = fs.readFileSync(OFFICIAL_ICON_PATH).toString('base64');

const RAW_BASE64 = {};
for (const [key, filePath] of Object.entries(RAW_FILES)) {
  RAW_BASE64[key] = fs.readFileSync(filePath).toString('base64');
}

// Screenshot configurations
const SCREENSHOT_CONFIGS = [
  {
    filename: 'screenshot_1.png',
    rawKey: 'welcome',
    accentColor: '#38BDF8',
    glowColor: '#2563EB',
    en: {
      badge: 'INSTANT SYNC',
      badgeWidth: 220,
      title: 'Real-Time Mac Mirroring',
      subtitle: 'Stream phone notifications directly to macOS menu bar'
    },
    es: {
      badge: 'SINCRONIZACIÓN AL INSTANTE',
      badgeWidth: 360,
      title: 'Tus Alertas en tu Mac',
      subtitle: 'Notificaciones al instante en la barra de menú'
    }
  },
  {
    filename: 'screenshot_2.png',
    rawKey: 'pairing',
    accentColor: '#10B981',
    glowColor: '#059669',
    en: {
      badge: 'EFFORTLESS PAIRING',
      badgeWidth: 260,
      title: 'Pair in Seconds via PIN',
      subtitle: 'Automatic Bonjour discovery over local Wi-Fi'
    },
    es: {
      badge: 'VINCULACIÓN RÁPIDA',
      badgeWidth: 260,
      title: 'Conéctate con un PIN Seguro',
      subtitle: 'Detección automática por red local Wi-Fi'
    }
  },
  {
    filename: 'screenshot_3.png',
    rawKey: 'privacy',
    accentColor: '#10B981',
    glowColor: '#10B981',
    en: {
      badge: 'ZERO CLOUD',
      badgeWidth: 190,
      title: '100% Private & Local',
      subtitle: 'End-to-end AES-256-GCM encryption, zero cloud'
    },
    es: {
      badge: 'CERO NUBE',
      badgeWidth: 180,
      title: '100% Privado y Local',
      subtitle: 'Cifrado extremo a extremo sin servidores externos'
    }
  },
  {
    filename: 'screenshot_4.png',
    rawKey: 'apps',
    accentColor: '#F59E0B',
    glowColor: '#D97706',
    en: {
      badge: 'TOTAL CONTROL',
      badgeWidth: 210,
      title: 'Selective App Filtering',
      subtitle: 'Control which apps mirror to your Mac'
    },
    es: {
      badge: 'CONTROL TOTAL',
      badgeWidth: 210,
      title: 'Filtro Selectivo por App',
      subtitle: 'Elige qué aplicaciones envían alertas a tu Mac'
    }
  },
  {
    filename: 'screenshot_5.png',
    rawKey: 'settings',
    accentColor: '#818CF8',
    glowColor: '#6366F1',
    en: {
      badge: 'HIGH PERFORMANCE',
      badgeWidth: 260,
      title: 'WebSockets & KeyStore',
      subtitle: 'Ultra-fast persistent channel & hardware keys'
    },
    es: {
      badge: 'MÁXIMA VELOCIDAD',
      badgeWidth: 240,
      title: 'Baja Latencia y KeyStore',
      subtitle: 'Canal persistente ultrarrápido y claves seguras'
    }
  }
];

function buildScreenshotSvg(config, lang) {
  const content = config[lang];
  const base64 = RAW_BASE64[config.rawKey];
  const halfBadge = Math.round(content.badgeWidth / 2);

  return `
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1080" height="2400" viewBox="0 0 1080 2400">
  <defs>
    <!-- Background Base Gradient -->
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#070B14"/>
      <stop offset="40%" stop-color="#0F172A"/>
      <stop offset="100%" stop-color="#090E1A"/>
    </linearGradient>

    <!-- Ambient Mesh Glow (Dynamic per screen accent) -->
    <radialGradient id="ambientGlow" cx="50%" cy="15%" r="48%">
      <stop offset="0%" stop-color="${config.glowColor}" stop-opacity="0.32"/>
      <stop offset="55%" stop-color="${config.accentColor}" stop-opacity="0.12"/>
      <stop offset="100%" stop-color="#0F172A" stop-opacity="0"/>
    </radialGradient>

    <radialGradient id="bottomGlow" cx="50%" cy="88%" r="45%">
      <stop offset="0%" stop-color="${config.glowColor}" stop-opacity="0.18"/>
      <stop offset="100%" stop-color="#090E1A" stop-opacity="0"/>
    </radialGradient>

    <!-- Phone Chassis Gradient -->
    <linearGradient id="chassisGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#64748B"/>
      <stop offset="20%" stop-color="#334155"/>
      <stop offset="50%" stop-color="#1E293B"/>
      <stop offset="80%" stop-color="#334155"/>
      <stop offset="100%" stop-color="#0F172A"/>
    </linearGradient>

    <!-- Phone Drop Shadows -->
    <filter id="phoneShadow" x="-20%" y="-15%" width="140%" height="140%">
      <feDropShadow dx="0" dy="36" stdDeviation="42" flood-color="#000000" flood-opacity="0.75"/>
      <feDropShadow dx="0" dy="12" stdDeviation="16" flood-color="#000000" flood-opacity="0.45"/>
    </filter>

    <!-- Screen Clipping Path -->
    <clipPath id="screenClip">
      <rect x="114" y="444" width="852" height="1882" rx="46" ry="46"/>
    </clipPath>
  </defs>

  <!-- Canvas Background -->
  <rect width="1080" height="2400" fill="url(#bgGrad)"/>
  <rect width="1080" height="2400" fill="url(#ambientGlow)"/>
  <rect width="1080" height="2400" fill="url(#bottomGlow)"/>

  <!-- Subtle Ambient Dots Pattern -->
  <pattern id="dotPattern" x="0" y="0" width="40" height="40" patternUnits="userSpaceOnUse">
    <circle cx="20" cy="20" r="1.2" fill="#334155" opacity="0.25"/>
  </pattern>
  <rect width="1080" height="420" fill="url(#dotPattern)"/>

  <!-- HEADER: Category Badge Pill -->
  <g transform="translate(540, 150)">
    <rect x="-${halfBadge}" y="-22" width="${content.badgeWidth}" height="44" rx="22" fill="#1E293B" stroke="${config.accentColor}" stroke-width="1.6" opacity="0.95"/>
    <text x="0" y="7" fill="${config.accentColor}" font-family="-apple-system, SF Pro Display, Helvetica Neue, sans-serif" font-size="19" font-weight="700" letter-spacing="2" text-anchor="middle">${escapeXml(content.badge)}</text>
  </g>

  <!-- HEADER: Main Headline -->
  <text x="540" y="260" fill="#FFFFFF" font-family="-apple-system, SF Pro Display, Helvetica Neue, sans-serif" font-size="62" font-weight="800" text-anchor="middle" letter-spacing="-1">${escapeXml(content.title)}</text>

  <!-- HEADER: Subtitle -->
  <text x="540" y="325" fill="#94A3B8" font-family="-apple-system, SF Pro Text, Helvetica Neue, sans-serif" font-size="28" font-weight="500" text-anchor="middle">${escapeXml(content.subtitle)}</text>

  <!-- PHONE MOCKUP WITH SHADOW -->
  <g filter="url(#phoneShadow)">
    <!-- Outer Phone Chassis -->
    <rect x="100" y="430" width="880" height="1910" rx="60" ry="60" fill="#111827" stroke="url(#chassisGrad)" stroke-width="6"/>
    <!-- Screen Inner Bezel -->
    <rect x="110" y="440" width="860" height="1890" rx="50" ry="50" fill="#000000"/>
  </g>

  <!-- SCREEN CONTENT (Clipped) -->
  <g clip-path="url(#screenClip)">
    <image href="data:image/jpeg;base64,${base64}" x="114" y="444" width="852" height="1882" preserveAspectRatio="xMidYMid slice"/>
  </g>

  <!-- CAMERA PUNCH-HOLE -->
  <circle cx="540" cy="470" r="10" fill="#000000"/>
  <circle cx="540" cy="470" r="4" fill="#1E293B"/>

  <!-- SPECULAR GLASS HIGHLIGHT -->
  <path d="M 114 444 Q 540 540 966 444 L 966 600 Q 540 680 114 600 Z" fill="#FFFFFF" opacity="0.03" clip-path="url(#screenClip)"/>

</svg>`;
}

function buildFeatureGraphicSvg(lang = 'en') {
  const isES = lang === 'es';
  const subtitle = isES 
    ? "Puente de Notificaciones para macOS" 
    : "Android to macOS Notification Bridge";
  const tagline = isES 
    ? "100% Privado • Cifrado Local • Sin Servidores en la Nube"
    : "100% Private • End-to-End Encrypted • Zero Cloud";
  const badgeOpenSource = isES ? "CÓDIGO ABIERTO" : "OPEN SOURCE";
  const badgeLocal = isES ? "RED WI-FI LOCAL" : "LOCAL WI-FI ONLY";
  const phoneAlert = isES ? "Sincronizado" : "Synced to Mac";
  const macToastTitle = "WhatsApp";
  const macToastBody = isES ? "Mensaje entrante" : "New message received";
  const macToastSub = isES ? "vía MacMirror • Instantáneo" : "via MacMirror • Instant";
  const disclaimer = isES
    ? "App complementaria para macOS gratuita y de código abierto en GitHub"
    : "Free companion macOS Menu Bar client available on GitHub";

  return `
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1024" height="500" viewBox="0 0 1024 500">
  <defs>
    <!-- Background Gradient -->
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#070B14"/>
      <stop offset="50%" stop-color="#0F172A"/>
      <stop offset="100%" stop-color="#0A101D"/>
    </linearGradient>

    <!-- Ambient Glows -->
    <radialGradient id="blueGlow" cx="20%" cy="30%" r="50%">
      <stop offset="0%" stop-color="#2563EB" stop-opacity="0.35"/>
      <stop offset="100%" stop-color="#0F172A" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="cyanGlow" cx="80%" cy="40%" r="50%">
      <stop offset="0%" stop-color="#06B6D4" stop-opacity="0.25"/>
      <stop offset="100%" stop-color="#0F172A" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="emeraldGlow" cx="50%" cy="80%" r="40%">
      <stop offset="0%" stop-color="#10B981" stop-opacity="0.15"/>
      <stop offset="100%" stop-color="#0F172A" stop-opacity="0"/>
    </radialGradient>

    <!-- Icon Gradient -->
    <linearGradient id="iconGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#3B82F6"/>
      <stop offset="100%" stop-color="#1D4ED8"/>
    </linearGradient>

    
    <!-- Official App Icon Clipping Path -->
    <clipPath id="appIconClip">
      <rect x="0" y="0" width="88" height="88" rx="20" ry="20"/>
    </clipPath>
    <clipPath id="toastIconClip">
      <rect x="12" y="14" width="20" height="20" rx="5" ry="5"/>
    </clipPath>

    <!-- Beam Gradient -->
    <linearGradient id="beamGrad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="#3B82F6"/>
      <stop offset="50%" stop-color="#38BDF8"/>
      <stop offset="100%" stop-color="#10B981"/>
    </linearGradient>

    <!-- Shadows -->
    <filter id="cardShadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="16" stdDeviation="20" flood-color="#000000" flood-opacity="0.6"/>
      <feDropShadow dx="0" dy="4" stdDeviation="6" flood-color="#000000" flood-opacity="0.3"/>
    </filter>
    <filter id="iconShadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="8" stdDeviation="12" flood-color="#2563EB" flood-opacity="0.5"/>
    </filter>
  </defs>

  <!-- Background -->
  <rect width="1024" height="500" fill="url(#bgGrad)"/>
  <rect width="1024" height="500" fill="url(#blueGlow)"/>
  <rect width="1024" height="500" fill="url(#cyanGlow)"/>
  <rect width="1024" height="500" fill="url(#emeraldGlow)"/>

  <!-- Background Tech Grid Pattern -->
  <pattern id="grid" width="32" height="32" patternUnits="userSpaceOnUse">
    <path d="M 32 0 L 0 0 0 32" fill="none" stroke="#1E293B" stroke-width="0.8" opacity="0.35"/>
  </pattern>
  <rect width="1024" height="500" fill="url(#grid)"/>

  <!-- LEFT BRANDING SECTION -->
  <g transform="translate(60, 70)">
    <!-- Official App Icon Squircle -->
    <g filter="url(#iconShadow)">
      <rect x="0" y="0" width="88" height="88" rx="20" fill="#0080FF"/>
      <image href="data:image/png;base64,${OFFICIAL_ICON_BASE64}" x="0" y="0" width="88" height="88" preserveAspectRatio="xMidYMid slice" clip-path="url(#appIconClip)"/>
      <rect x="0" y="0" width="88" height="88" rx="20" fill="none" stroke="rgba(255, 255, 255, 0.3)" stroke-width="1.5"/>
    </g>

    <!-- Brand Name -->
    <text x="108" y="55" fill="#FFFFFF" font-family="-apple-system, SF Pro Display, Helvetica Neue, sans-serif" font-size="44" font-weight="800" letter-spacing="-0.5">MacMirror</text>
    <text x="108" y="82" fill="#38BDF8" font-family="-apple-system, SF Pro Text, Helvetica Neue, sans-serif" font-size="15" font-weight="700" letter-spacing="1.5">ANDROID → MACOS</text>

    <!-- Main Headline / Subtitle -->
    <text x="0" y="145" fill="#F8FAFC" font-family="-apple-system, SF Pro Display, Helvetica Neue, sans-serif" font-size="${isES ? '24' : '28'}" font-weight="700" letter-spacing="-0.3">${escapeXml(subtitle)}</text>

    <!-- Tagline description -->
    <text x="0" y="185" fill="#94A3B8" font-family="-apple-system, SF Pro Text, Helvetica Neue, sans-serif" font-size="17" font-weight="400">${escapeXml(tagline)}</text>

    <!-- Trust Badges -->
    <g transform="translate(0, 240)">
      <!-- Open Source Badge -->
      <rect x="0" y="0" width="${isES ? '175' : '160'}" height="38" rx="19" fill="#1E293B" stroke="#64748B" stroke-width="1.2" opacity="0.9"/>
      <path d="M 18 19 C 18 14.5 21.6 11 26 11 C 30.4 11 34 14.5 34 19 C 34 22.5 31.8 25.5 28.5 26.6 C 28.1 26.7 28 26.4 28 26.2 L 28 24.8 C 26 25.2 25.4 24.1 25.4 24.1 C 25.1 23.3 24.5 23.1 24.5 23.1 C 23.8 22.6 24.6 22.6 24.6 22.6 C 25.4 22.7 25.8 23.5 25.8 23.5 C 26.5 24.7 27.6 24.3 28 24.1 C 28.1 23.6 28.3 23.2 28.5 23 C 26.8 22.8 25.1 22.1 25.1 19.3 C 25.1 18.5 25.4 17.8 25.9 17.3 C 25.8 17.1 25.5 16.3 26 15.2 C 26 15.2 26.7 15 28.1 16 C 28.7 15.8 29.4 15.7 30 15.7 C 30.6 15.7 31.3 15.8 31.9 16 C 33.3 15 34 15.2 34 15.2 C 34.5 16.3 34.2 17.1 34.1 17.3 C 34.6 17.8 34.9 18.5 34.9 19.3 C 34.9 22.1 33.2 22.8 31.5 23 C 31.8 23.3 32 23.8 32 24.5 L 32 26.2 C 32 26.4 31.9 26.7 31.5 26.6 C 28.2 25.5 26 22.5 26 19" fill="#E2E8F0"/>
      <text x="44" y="24" fill="#F1F5F9" font-family="-apple-system, SF Pro Text, Helvetica Neue, sans-serif" font-size="12.5" font-weight="700" letter-spacing="0.5">${escapeXml(badgeOpenSource)}</text>

      <!-- Local Wi-Fi Badge -->
      <g transform="translate(${isES ? '188' : '172'}, 0)">
        <rect x="0" y="0" width="${isES ? '165' : '160'}" height="38" rx="19" fill="#1E293B" stroke="#0EA5E9" stroke-width="1.2" opacity="0.9"/>
        <path d="M 18 23 A 7 7 0 0 1 28 23 M 15 20 A 11 11 0 0 1 31 20 M 23 25 A 1 1 0 1 1 23 25.1" fill="none" stroke="#38BDF8" stroke-width="2" stroke-linecap="round"/>
        <text x="38" y="24" fill="#38BDF8" font-family="-apple-system, SF Pro Text, Helvetica Neue, sans-serif" font-size="12.5" font-weight="700" letter-spacing="0.5">${escapeXml(badgeLocal)}</text>
      </g>

      <!-- E2EE Badge -->
      <g transform="translate(${isES ? '365' : '344'}, 0)">
        <rect x="0" y="0" width="138" height="38" rx="19" fill="#1E293B" stroke="#10B981" stroke-width="1.2" opacity="0.9"/>
        <path d="M 19 19 L 19 16 C 19 13.8 20.8 12 23 12 C 25.2 12 27 13.8 27 16 L 27 19 M 16 19 L 30 19 C 31 19 31.5 19.5 31.5 20.5 L 31.5 26.5 C 31.5 27.5 31 28 30 28 L 16 28 C 15 28 14.5 27.5 14.5 26.5 L 14.5 20.5 C 14.5 19.5 15 19 16 19 Z" fill="none" stroke="#34D399" stroke-width="1.8"/>
        <text x="38" y="24" fill="#34D399" font-family="-apple-system, SF Pro Text, Helvetica Neue, sans-serif" font-size="12.5" font-weight="700" letter-spacing="0.5">AES-256-GCM</text>
      </g>
    </g>

    <!-- Subtext disclaimer -->
    <text x="0" y="315" fill="#64748B" font-family="-apple-system, SF Pro Text, Helvetica Neue, sans-serif" font-size="13" font-weight="500">${escapeXml(disclaimer)}</text>
  </g>

  <!-- RIGHT VISUAL DIAGRAM: WIRELESS BRIDGE -->
  <g transform="translate(625, 70)">

    <!-- Wireless Connection Arc Beam -->
    <path d="M 80 180 C 155 110, 235 110, 310 160" fill="none" stroke="url(#beamGrad)" stroke-width="3" stroke-dasharray="6,6" opacity="0.8"/>

    <!-- Lock Icon in the middle of beam -->
    <g transform="translate(180, 120)" filter="url(#cardShadow)">
      <circle cx="15" cy="15" r="18" fill="#0F172A" stroke="#38BDF8" stroke-width="2"/>
      <path d="M 12 14 L 12 11 C 12 9.5 13.3 8 15 8 C 16.7 8 18 9.5 18 11 L 18 14 M 10 14 L 20 14 C 20.5 14 21 14.5 21 15 L 21 20 C 21 20.5 20.5 21 20 21 L 10 21 C 9.5 21 9 20.5 9 20 L 9 15 C 9 14.5 9.5 14 10 14 Z" fill="none" stroke="#38BDF8" stroke-width="1.8"/>
    </g>

    <!-- Mini Android Phone Mockup (Left) -->
    <g transform="translate(5, 60)" filter="url(#cardShadow)">
      <rect x="0" y="0" width="128" height="255" rx="20" fill="#1E293B" stroke="#475569" stroke-width="3"/>
      <rect x="6" y="6" width="116" height="243" rx="15" fill="#0F172A"/>
      <text x="14" y="22" fill="#94A3B8" font-family="-apple-system, sans-serif" font-size="9" font-weight="600">09:41</text>
      <circle cx="64" cy="18" r="3" fill="#334155"/>
      <rect x="92" y="14" width="16" height="8" rx="2" fill="#64748B"/>

      <!-- Notification Card on Phone Screen -->
      <rect x="10" y="45" width="108" height="60" rx="8" fill="#1E293B" stroke="#3B82F6" stroke-width="1.2"/>
      <circle cx="24" cy="62" r="7" fill="#25D366"/>
      <text x="36" y="62" fill="#FFFFFF" font-family="-apple-system, sans-serif" font-size="9" font-weight="700">WhatsApp</text>
      <text x="36" y="73" fill="#94A3B8" font-family="-apple-system, sans-serif" font-size="7">${escapeXml(macToastBody)}</text>
      <rect x="18" y="85" width="92" height="12" rx="4" fill="#3B82F6"/>
      <text x="64" y="93.5" fill="#FFFFFF" font-family="-apple-system, sans-serif" font-size="7" font-weight="600" text-anchor="middle">${escapeXml(phoneAlert)}</text>

      <!-- App list representation -->
      <rect x="12" y="120" width="104" height="15" rx="4" fill="#1E293B"/>
      <rect x="12" y="141" width="104" height="15" rx="4" fill="#1E293B"/>
      <rect x="12" y="162" width="104" height="15" rx="4" fill="#1E293B"/>
      <rect x="12" y="183" width="104" height="15" rx="4" fill="#1E293B"/>

      <text x="64" y="280" fill="#94A3B8" font-family="-apple-system, sans-serif" font-size="12" font-weight="600" text-anchor="middle">Android</text>
    </g>

    <!-- Mini Mac Display Mockup (Right) -->
    <g transform="translate(190, 30)" filter="url(#cardShadow)">
      <rect x="0" y="0" width="225" height="180" rx="10" fill="#1E293B" stroke="#475569" stroke-width="2"/>
      <rect x="0" y="0" width="225" height="24" rx="10" fill="#0F172A"/>
      <rect x="0" y="14" width="225" height="10" fill="#0F172A"/>
      <circle cx="12" cy="12" r="3.5" fill="#EF4444"/>
      <circle cx="22" cy="12" r="3.5" fill="#F59E0B"/>
      <circle cx="32" cy="12" r="3.5" fill="#10B981"/>

      <!-- Menu bar right icons -->
      <path d="M 190 8 C 187 8 185 10 185 13 L 185 17 L 183 19 L 197 19 L 195 17 L 195 13 C 195 10 193 8 190 8 Z" fill="#38BDF8"/>
      <circle cx="204" cy="12" r="2" fill="#10B981"/>

      <!-- macOS Desktop Wallpaper Gradient -->
      <rect x="6" y="28" width="213" height="144" rx="6" fill="#0B132B"/>

      <!-- Synced Alert Notification Toast (Floating Top Right) -->
      <g transform="translate(14, 42)" filter="url(#cardShadow)">
        <rect x="0" y="0" width="186" height="56" rx="8" fill="#1E293B" stroke="#38BDF8" stroke-width="1.2" opacity="0.95"/>
        <image href="data:image/png;base64,${OFFICIAL_ICON_BASE64}" x="10" y="14" width="20" height="20" clip-path="url(#toastIconClip)"/>
        <text x="36" y="20" fill="#FFFFFF" font-family="-apple-system, sans-serif" font-size="10" font-weight="700">${escapeXml(macToastTitle)}</text>
        <text x="36" y="32" fill="#94A3B8" font-family="-apple-system, sans-serif" font-size="8">${escapeXml(macToastBody)}</text>
        <text x="36" y="44" fill="#38BDF8" font-family="-apple-system, sans-serif" font-size="7.5" font-weight="600">${escapeXml(macToastSub)}</text>
      </g>

      <text x="112" y="205" fill="#94A3B8" font-family="-apple-system, sans-serif" font-size="12" font-weight="600" text-anchor="middle">macOS Menu Bar</text>
    </g>

  </g>
</svg>`;
}

async function run() {
  console.log('🚀 Starting Google Play Store asset generation for MacMirror...\n');

  const languages = ['en', 'es'];
  const langFolderMap = { en: 'en-US', es: 'es-ES' };

  for (const lang of languages) {
    const targetFolder = path.join(OUTPUT_DIR, langFolderMap[lang]);
    if (!fs.existsSync(targetFolder)) {
      fs.mkdirSync(targetFolder, { recursive: true });
    }

    console.log(`📁 Generating assets for language: [${lang.toUpperCase()}] -> ${langFolderMap[lang]}`);

    // 1. Generate Feature Graphic
    const fgSvg = buildFeatureGraphicSvg(lang);
    const fgSvgPath = path.join('/tmp', `fg_${lang}.svg`);
    const fgPngPath = path.join(targetFolder, 'feature_graphic.png');
    fs.writeFileSync(fgSvgPath, fgSvg);
    execSync(`sips -s format png "${fgSvgPath}" --out "${fgPngPath}"`);
    fs.unlinkSync(fgSvgPath);
    console.log(`  ✓ Created Feature Graphic (1024x500): ${fgPngPath}`);

    // 2. Generate 5 Screenshots
    for (const config of SCREENSHOT_CONFIGS) {
      const sSvg = buildScreenshotSvg(config, lang);
      const sSvgPath = path.join('/tmp', `screen_${lang}_${config.filename}.svg`);
      const sPngPath = path.join(targetFolder, config.filename);
      fs.writeFileSync(sSvgPath, sSvg);
      execSync(`sips -s format png "${sSvgPath}" --out "${sPngPath}"`);
      fs.unlinkSync(sSvgPath);
      console.log(`  ✓ Created Screenshot (1080x2400): ${config.filename} -> ${contentSummary(config, lang)}`);
    }

    console.log('');
  }

  console.log('🎉 All assets successfully generated!');
}

function contentSummary(config, lang) {
  return `"${config[lang].title}"`;
}

run().catch(err => {
  console.error('Error generating assets:', err);
  process.exit(1);
});
