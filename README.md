# MyLauncher Studio

A lightweight Android home launcher project for Android 8.0+.

## Current milestone: 0.1.0
- Android HOME launcher entry point
- Native Kotlin + Jetpack Compose UI
- Launchable app discovery and app drawer grid
- Minimal glass-style search/drawer bar
- GitHub Actions debug APK build

## Build
Requires JDK 17, Android SDK 35 and Gradle 8.9.

```bash
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Device limitations
This app is a third-party launcher. Android/realme system lock screen, secure authentication, status bar and system-wide fonts remain controlled by Android. Set MyLauncher Studio as the Home app in system settings to use it.

## Roadmap
1. Home pages, editable dock and folders
2. Searchable app drawer, A-Z index and hidden apps
3. Theme designer, presets, icon shapes and wallpaper controls
4. Gestures and media-session controls
5. Lock-screen companion (not a replacement for the secure system lock screen)
6. Layout and theme import/export
