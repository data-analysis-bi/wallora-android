# Wallora for Android

Kotlin + Jetpack Compose. Builds an installable APK on GitHub (no Android Studio, no developer account).

## Build
1. Create a GitHub repo and upload everything in this folder (use GitHub Desktop so `.github/` is included).
2. Repo → **Actions** → "Build Android APK" runs on every push (or *Run workflow*). ~5 min.
3. Open the finished run → **Artifacts** → download `Wallora-apk` → unzip → `app-release.apk`.

If it fails, open the step **Show errors only** and send the lines starting with `e:` to Claude.

## Install on your phone
1. Copy the APK to the phone (USB, Google Drive, or email it to yourself).
2. Open it. Android asks to allow "Install unknown apps" for your file manager/browser — allow it.
3. If Play Protect warns about an unknown developer, choose *Install anyway*.

## What's different from iOS
- **Wallpaper**: Android lets the app set Home / Lock / both directly. No Photos detour.
- **Widgets**: tap *Add widget* in the Widgets tab (or long-press Home → Widgets → Wallora). A setup screen opens: design it, tap *Add widget*.
  Long-press the widget → *Reconfigure* to change it later (Android 12+).
- **Not available**: Lock Screen widgets (most Android phones don't support them), weather, battery, analog clock.
