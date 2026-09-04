#!/usr/bin/env node

/**
 * ==============================================================================
 * MacMirror Google Play Store Listing & Assets Synchronization CLI
 * ==============================================================================
 * 
 * Synchronizes store listing metadata (Title, Short Description, Full Description),
 * official app icon, feature graphic, and high-resolution marketing screenshots
 * with Google Play Console using the Google Play Developer Publishing API v3.
 * 
 * Usage:
 *   node android/scripts/playstore-sync.mjs [options]
 * 
 * Options:
 *   --key <path>           Path to Service Account JSON key (default: android/.secrets/play-store-key.json)
 *   --package <id>         Application package ID (default: com.angelsoft.macmirror)
 *   --dry-run              Validates changes and uploads without committing to Play Console
 *   --metadata-only        Syncs only text metadata (skips all images)
 *   --screenshots-only     Uploads only phone screenshots (skips metadata, icon, feature graphic)
 *   --graphics-only        Uploads only app icon and feature graphic
 *   --lang <code,code>     Target specific language codes (e.g. --lang en-US,es-ES)
 *   --help, -h             Displays this help message
 * 
 * Environment Variables:
 *   GOOGLE_PLAY_KEY_PATH   Alternative path to Service Account JSON key
 *   GOOGLE_PLAY_KEY_JSON   Raw JSON string of the Service Account credentials
 */

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const ANDROID_DIR = path.resolve(__dirname, '..');
const PLAY_STORE_DIR = path.join(ANDROID_DIR, 'play_store');

// ==============================================================================
// METADATA & ASSET CONFIGURATION (Loaded directly from play_store/ directory)
// ==============================================================================

function loadListingData(sourceDir, targetLangCode) {
  const titlePath = path.join(sourceDir, 'title.txt');
  const shortDescPath = path.join(sourceDir, 'short_description.txt');
  const fullDescPath = path.join(sourceDir, 'full_description.txt');

  if (!fs.existsSync(titlePath) || !fs.existsSync(shortDescPath) || !fs.existsSync(fullDescPath)) {
    throw new Error(`Missing text metadata in: ${sourceDir}`);
  }

  return {
    language: targetLangCode,
    title: fs.readFileSync(titlePath, 'utf8').trim(),
    shortDescription: fs.readFileSync(shortDescPath, 'utf8').trim(),
    fullDescription: fs.readFileSync(fullDescPath, 'utf8').trim(),
    sourceDir
  };
}

// Map Google Play languages to their source directories
const SOURCE_MAP = {
  'en-US': path.join(PLAY_STORE_DIR, 'en-US'),
  'es-419': path.join(PLAY_STORE_DIR, 'es-ES'), // Latin America uses Spanish assets
  'es-ES': path.join(PLAY_STORE_DIR, 'es-ES')   // Spain uses Spanish assets
};

// ==============================================
// CLI OPTIONS PARSER
// ==============================================

function parseArgs() {
  const args = process.argv.slice(2);
  const options = {
    keyPath: process.env.GOOGLE_PLAY_KEY_PATH || path.join(ANDROID_DIR, '.secrets', 'play-store-key.json'),
    keyJson: process.env.GOOGLE_PLAY_KEY_JSON || null,
    packageName: 'com.angelsoft.macmirror',
    dryRun: false,
    metadataOnly: false,
    screenshotsOnly: false,
    graphicsOnly: false,
    languages: ['en-US', 'es-419', 'es-ES'],
    help: false
  };

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (arg === '--help' || arg === '-h') options.help = true;
    else if (arg === '--dry-run') options.dryRun = true;
    else if (arg === '--metadata-only') options.metadataOnly = true;
    else if (arg === '--screenshots-only') options.screenshotsOnly = true;
    else if (arg === '--graphics-only') options.graphicsOnly = true;
    else if (arg === '--key' && i + 1 < args.length) options.keyPath = args[++i];
    else if (arg === '--package' && i + 1 < args.length) options.packageName = args[++i];
    else if (arg === '--lang' && i + 1 < args.length) {
      options.languages = args[++i].split(',').map(s => s.trim());
    }
  }

  return options;
}

// ==============================================
// OAUTH2 JWT AUTHENTICATION
// ==============================================

function base64UrlEncode(str) {
  return Buffer.from(str)
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

async function getAccessToken(credentials) {
  console.log(`🔐 Authenticating as Service Account: ${credentials.client_email}...`);
  
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: 'RS256', typ: 'JWT' };
  const payload = {
    iss: credentials.client_email,
    scope: 'https://www.googleapis.com/auth/androidpublisher',
    aud: 'https://oauth2.googleapis.com/token',
    exp: now + 3600,
    iat: now
  };

  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedPayload = base64UrlEncode(JSON.stringify(payload));
  const signatureInput = `${encodedHeader}.${encodedPayload}`;

  const signer = crypto.createSign('RSA-SHA256');
  signer.update(signatureInput);
  const signature = signer.sign(credentials.private_key, 'base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');

  const jwt = `${signatureInput}.${signature}`;

  const response = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: jwt
    })
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Google OAuth2 Token Exchange Failed (${response.status}): ${errorText}`);
  }

  const data = await response.json();
  return data.access_token;
}

// ==============================================
// API HELPER FUNCTIONS
// ==============================================

async function uploadImage(uploadUrl, imageBuffer, contentType, token) {
  const res = await fetch(uploadUrl, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': contentType
    },
    body: imageBuffer
  });

  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`Upload failed (${res.status}): ${errText}`);
  }

  return await res.json();
}

async function clearImages(deleteUrl, token) {
  const res = await fetch(deleteUrl, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });

  // 404 is acceptable when clearing non-existent image sets
  if (!res.ok && res.status !== 404) {
    const errText = await res.text();
    console.warn(`      ⚠️ Note clearing images (${res.status}): ${errText}`);
  }
}

// ==============================================
// MAIN EXECUTION FLOW
// ==============================================

async function main() {
  const opts = parseArgs();

  if (opts.help) {
    console.log(`
MacMirror Google Play Store Publishing CLI

Usage:
  node android/scripts/playstore-sync.mjs [options]

Options:
  --key <path>           Path to Service Account JSON key (default: android/.secrets/play-store-key.json)
  --package <id>         Application package ID (default: com.angelsoft.macmirror)
  --dry-run              Validates changes with Google Play without committing
  --metadata-only        Syncs only titles and descriptions
  --screenshots-only     Syncs only phone screenshots
  --graphics-only        Syncs only app icon and feature graphic
  --lang <codes>         Comma-separated language codes (default: en-US,es-419,es-ES)
  --help, -h             Show this help message
    `);
    process.exit(0);
  }

  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('🚀 MacMirror Google Play Store Publisher');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log(`📦 Package ID:   ${opts.packageName}`);
  console.log(`🧪 Run Mode:     ${opts.dryRun ? 'DRY-RUN (Validation only)' : 'LIVE PUBLISH'}`);
  console.log(`🌐 Languages:    ${opts.languages.join(', ')}`);
  console.log(`📁 Key File:     ${opts.keyPath}`);
  console.log('─────────────────────────────────────────────────────');

  // 1. Load Credentials
  let credentials;
  if (opts.keyJson) {
    try {
      credentials = JSON.parse(opts.keyJson);
    } catch (e) {
      console.error('❌ Error parsing GOOGLE_PLAY_KEY_JSON environment variable.');
      process.exit(1);
    }
  } else {
    if (!fs.existsSync(opts.keyPath)) {
      console.error(`\n❌ Service account credentials file not found at:\n   ${opts.keyPath}`);
      console.error(`\n👉 Please place your Google Cloud Service Account JSON key at:\n   ${opts.keyPath}`);
      console.error(`   or specify it with --key <path> or GOOGLE_PLAY_KEY_PATH=<path>\n`);
      process.exit(1);
    }
    try {
      credentials = JSON.parse(fs.readFileSync(opts.keyPath, 'utf8'));
    } catch (e) {
      console.error(`❌ Error reading credentials JSON from ${opts.keyPath}: ${e.message}`);
      process.exit(1);
    }
  }

  // 2. Obtain OAuth2 Access Token
  const token = await getAccessToken(credentials);
  console.log('✅ Google Play API Access Token obtained successfully.\n');

  const apiHeaders = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };

  // 3. Create a new Edit Session
  console.log('📝 Creating Google Play Edit Session...');
  const editRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${opts.packageName}/edits`,
    {
      method: 'POST',
      headers: apiHeaders
    }
  );

  if (!editRes.ok) {
    const errorBody = await editRes.text();
    console.error(`\n❌ Failed to create Edit Session (${editRes.status}):`);
    console.error(errorBody);
    console.error(`
💡 Troubleshooting Tips:
  1. Ensure the app "${opts.packageName}" is created in Google Play Console.
  2. Verify that service account "${credentials.client_email}" has been added under "Users and permissions" in Google Play Console with "Edit store listing" and "Release" permissions.
  3. Ensure the Google Play Android Developer API is enabled in your Google Cloud Project "${credentials.project_id}".
    `);
    process.exit(1);
  }

  const editData = await editRes.json();
  const editId = editData.id;
  console.log(`✅ Edit Session Created: [${editId}] (Expires in: ${editData.expiryTimeSeconds}s)\n`);

  try {
    // 4. Synchronize Listings
    for (const lang of opts.languages) {
      const sourceDir = SOURCE_MAP[lang];
      if (!sourceDir || !fs.existsSync(sourceDir)) {
        console.warn(`⚠️ Warning: No source directory configured for language: ${lang}`);
        continue;
      }

      console.log(`━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`);
      console.log(`🌐 Processing Language: [${lang}] (Source: ${path.basename(sourceDir)})`);
      console.log(`━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`);

      // 4.1 Metadata (Title & Descriptions)
      if (!opts.screenshotsOnly && !opts.graphicsOnly) {
        const listing = loadListingData(sourceDir, lang);
        console.log(`📋 Updating Store Listing Metadata...`);
        console.log(`   • Title:             "${listing.title}" (${listing.title.length}/30 chars)`);
        console.log(`   • Short Description: "${listing.shortDescription}" (${listing.shortDescription.length}/80 chars)`);
        console.log(`   • Full Description:  ${listing.fullDescription.length}/4000 chars`);

        const updateRes = await fetch(
          `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${opts.packageName}/edits/${editId}/listings/${lang}`,
          {
            method: 'PUT',
            headers: apiHeaders,
            body: JSON.stringify({
              language: lang,
              title: listing.title,
              shortDescription: listing.shortDescription,
              fullDescription: listing.fullDescription
            })
          }
        );

        if (!updateRes.ok) {
          const err = await updateRes.text();
          throw new Error(`Failed to update listing metadata [${lang}]: ${err}`);
        }
        console.log(`   ✅ Metadata successfully synchronized.`);
      }

      // 4.2 Official App Icon (512x512)
      if (!opts.metadataOnly && !opts.screenshotsOnly) {
        const iconPath = path.join(sourceDir, 'app_icon.png');
        const fallbackIconPath = path.join(PLAY_STORE_DIR, 'app_icon.png');
        const finalIconPath = fs.existsSync(iconPath) ? iconPath : (fs.existsSync(fallbackIconPath) ? fallbackIconPath : null);

        if (finalIconPath) {
          console.log(`🎨 Uploading Official App Icon (512x512)...`);
          const iconBuffer = fs.readFileSync(finalIconPath);

          await clearImages(
            `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${opts.packageName}/edits/${editId}/listings/${lang}/icon`,
            token
          );

          await uploadImage(
            `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${opts.packageName}/edits/${editId}/listings/${lang}/icon?uploadType=media`,
            iconBuffer,
            'image/png',
            token
          );
          console.log(`   ✅ Official App Icon uploaded (${(iconBuffer.length / 1024).toFixed(1)} KB).`);
        }
      }

      // 4.3 Feature Graphic (1024x500)
      if (!opts.metadataOnly && !opts.screenshotsOnly) {
        const fgPath = path.join(sourceDir, 'feature_graphic.png');
        if (fs.existsSync(fgPath)) {
          console.log(`🖼️ Uploading Feature Graphic (1024x500)...`);
          const fgBuffer = fs.readFileSync(fgPath);

          await clearImages(
            `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${opts.packageName}/edits/${editId}/listings/${lang}/featureGraphic`,
            token
          );

          await uploadImage(
            `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${opts.packageName}/edits/${editId}/listings/${lang}/featureGraphic?uploadType=media`,
            fgBuffer,
            'image/png',
            token
          );
          console.log(`   ✅ Feature Graphic uploaded (${(fgBuffer.length / 1024).toFixed(1)} KB).`);
        }
      }

      // 4.4 Phone Screenshots (1080x2400)
      if (!opts.metadataOnly && !opts.graphicsOnly) {
        const screenshotFiles = fs.readdirSync(sourceDir)
          .filter(f => f.startsWith('screenshot_') && (f.endsWith('.png') || f.endsWith('.jpg')))
          .sort();

        if (screenshotFiles.length > 0) {
          console.log(`📱 Synchronizing ${screenshotFiles.length} Phone Screenshots...`);
          
          await clearImages(
            `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${opts.packageName}/edits/${editId}/listings/${lang}/phoneScreenshots`,
            token
          );

          for (let idx = 0; idx < screenshotFiles.length; idx++) {
            const file = screenshotFiles[idx];
            const fullPath = path.join(sourceDir, file);
            const imageBuffer = fs.readFileSync(fullPath);
            const contentType = file.endsWith('.png') ? 'image/png' : 'image/jpeg';

            console.log(`   • Uploading (${idx + 1}/${screenshotFiles.length}): ${file} (${(imageBuffer.length / 1024 / 1024).toFixed(2)} MB)...`);
            await uploadImage(
              `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${opts.packageName}/edits/${editId}/listings/${lang}/phoneScreenshots?uploadType=media`,
              imageBuffer,
              contentType,
              token
            );
          }
          console.log(`   ✅ All ${screenshotFiles.length} phone screenshots uploaded.`);
        }
      }

      console.log('');
    }

    // 5. Validate or Commit Session
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    if (opts.dryRun) {
      console.log('🧪 Validating Edit Session with Google Play (--dry-run)...');
      const valRes = await fetch(
        `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${opts.packageName}/edits/${editId}:validate`,
        {
          method: 'POST',
          headers: apiHeaders
        }
      );

      if (!valRes.ok) {
        const err = await valRes.text();
        throw new Error(`Google Play validation failed: ${err}`);
      }
      console.log('✅ Edit validation SUCCESSFUL! All assets and metadata meet Play Console requirements.');
      console.log('💡 Note: No changes were committed to the live store (DRY-RUN mode).');
    } else {
      console.log('🚀 Committing Edit Session to Google Play Console...');
      const commitRes = await fetch(
        `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${opts.packageName}/edits/${editId}:commit`,
        {
          method: 'POST',
          headers: apiHeaders
        }
      );

      if (!commitRes.ok) {
        const err = await commitRes.text();
        throw new Error(`Commit to Play Console failed: ${err}`);
      }
      console.log('🎉 SUCCESS: All metadata, icon, feature graphic, and screenshots have been published to Google Play Console!');
    }
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

  } catch (error) {
    console.error('\n❌ ERROR during synchronization:', error.message);
    process.exit(1);
  }
}

main().catch(err => {
  console.error('\n💥 Unexpected error:', err);
  process.exit(1);
});
