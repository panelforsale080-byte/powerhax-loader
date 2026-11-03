# POWERHAX / ASTRAL Loader (Android)

Android loader app from **POWERHAX LOADER.zip**. Fetches library catalog from the panel server, validates license, downloads `libexternal.so`, then injects into the selected game package (root / method options).

## Get the APK

### From GitHub Actions (recommended)

1. Open **Actions** → **Build APK** → latest green run.
2. Download artifact **powerhax-loader-debug** (`.apk` inside).

Or after push, wait for CI on `main`.

### Build on your PC

Requirements: **JDK 17**, **Android SDK** (API 35), internet.

```bash
cd powerhax-loader
chmod +x gradlew
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Release (needs `app/signing-key.jks` and passwords in `app/build.gradle.kts`):

```bash
./gradlew assembleRelease
```

## Install

1. Copy APK to the phone.
2. Enable **Install unknown apps** for your file manager.
3. Install **ASTRAL LOADER** (`astral.loader`).

Android **8+** (minSdk 28). Internet required.

## How to use

1. Open the app (splash → main screen).
2. Allow **notifications** if asked.
3. Pick a **library / game** from the list (drawer / spinner). Maintenance entries are blocked.
4. Enter your **license key** (panel key for that library).
5. Tap **LOGIN** — app validates against the panel (`PANEL_URL` in code) and downloads the cheat library if needed.
6. When the button shows **INJECT**:
   - Choose **Auto** (launch game then inject after delay) or **Manual**.
   - Set **delay** and **injection method** (root / non-root options depend on device).
7. Tap **INJECT** and wait for **Injection completed**.

**Root:** Some methods need root; status is shown on screen (`root_status`).

**VPN:** If a VPN is detected at splash, download path may differ (see `SplashActivity`).

**Re-inject:** After success you can use **RE-INJECT** without full login if the library is already cached.

## Config (developers)

| Item | Location |
|------|----------|
| Server list URL | `MainActivity.SERVER_LIST_URL` |
| Panel / license URL | `MainActivity.PANEL_URL` |
| App id | `astral.loader` |

Do **not** commit real `signing-key.jks` or license keys to public repos.

## Repo

Built and published from this project; APK artifacts are in GitHub Actions.
