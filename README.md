# Radiance

An Android brightness controller for media playback. When you play a video, the app boosts your screen brightness. When playback stops, it returns to normal.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-AGPL%20v3-orange.svg)](LICENSE)
[![APK Size](https://img.shields.io/badge/APK%20Size-16.66%20MB-blue.svg)]()

## What it does

**System brightness**: Slide from 0 to 255 or tap Min/Mid/Max. Toggle between manual and automatic mode.

**Window brightness**: Override brightness at the window level without needing the WRITE_SETTINGS permission. Tap Max, Medium, or Reset.

**Media monitoring**: When you start playing a video in any app, Radiance raises brightness and restores it when you stop. Auto-start runs this service on boot if enabled.

## Build info

The APK is 16.66 MB. It started as a Flutter project at 69.5 MB. Removing Flutter (libflutter.so at 39.5 MB, kernel_blob.bin at 38.6 MB, isolate_snapshot_data at 10.3 MB) cut the size by 76%.

## Setup

**Requirements**
- Android 7.0 (API 24) or higher
- WRITE_SETTINGS permission (you grant this in system settings)
- Optional: FOREGROUND_SERVICE for media monitoring

**Build from source**

```bash
git clone https://github.com/VishaL6i9/RadianceAndroid.git
cd RadianceAndroid

cd android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or in one step:
```bash
./gradlew installDebug
```

**Download APK**: Check [Releases](https://github.com/VishaL6i9/RadianceAndroid/releases)

## How to use

**Brightness slider**: Move it left for dim, right for bright. The number updates in real time (0-255).

**Quick buttons**: Min sets it to 0, Mid to 128, Max to 255. You can also tap Manual or Auto to switch modes.

**Window override**: This doesn't need permissions. It overrides the window's brightness setting independently of system brightness. Tap Max, Mid, or Reset.

**Media monitoring**: Tap Start in the Media Monitoring card. The app listens for playback via the MediaSession API and raises brightness while video plays. When you stop the video, brightness returns to what it was. Enable Auto-start on boot if you want this to run after restart.

## Permissions

- WRITE_SETTINGS: modify system brightness
- FOREGROUND_SERVICE: keep media monitoring running
- POST_NOTIFICATIONS: show the monitoring notification
- RECEIVE_BOOT_COMPLETED: auto-start the service

## Under the hood

**Architecture**

```
MainActivity (Compose)
├── BrightnessControlScreen
│   ├── PermissionCard
│   ├── BrightnessControlCard (slider and presets)
│   ├── WindowBrightnessCard (override)
│   ├── MediaMonitorCard (service control)
│   └── StatusBar
├── MediaMonitorService (foreground service)
├── BootReceiver (auto-start)
└── Theme (Material 3 + system colors)
```

**Key APIs**

| API | Purpose |
|-----|---------|
| Settings.System.SCREEN_BRIGHTNESS | Get and set brightness (0-255) |
| Settings.System.SCREEN_BRIGHTNESS_MODE | Switch auto/manual |
| Window.LayoutParams.screenBrightness | Window brightness override |
| MediaSessionManager | Detect when video plays |
| ActivityResultLauncher | Handle permission requests |
| SharedPreferences | Save settings across restarts |
| ForegroundService | Run monitoring in the background |

**Theming**

On Android 12 and up, the app pulls colors from your wallpaper (Material You). On Android 11 and earlier, it uses a Material 3 palette (orange and amber tones). Dark and light modes follow your system setting.

**Build targets**

- minSdk: 24 (Android 7.0)
- targetSdk: 35 (Android 16)
- Kotlin: 2.1.0
- Gradle: 8.12

## Performance

| Metric | Value |
|--------|-------|
| APK size | 16.66 MB |
| Size reduction vs Flutter | 76% |
| Build time | ~6 seconds |
| Startup time | <1 second |

## What doesn't work

- The app boosts brightness during all video playback, not just HDR content. True HDR detection requires access to frame metadata that unrooted devices cannot reach.
- On some devices, the foreground service needs notification access enabled manually.
- OEM-specific brightness curves (like OnePlus HBM) require manufacturer APIs and manufacturer documentation.
- Unrooted devices cannot modify low-level system brightness state.

## What might come later

- HDR-specific detection
- Per-app brightness profiles
- Scheduled brightness control
- Advanced battery analysis
- Custom brightness curves

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Install
./gradlew installDebug

# Check size
du -h app/build/outputs/apk/debug/app-debug.apk
```

## License

AGPL v3. See [LICENSE](LICENSE).

The AGPL was chosen because the brightness system could grow to include networked or remote features. AGPL ensures improvements stay available to the community. A simpler copyleft like GPL v3 does not enforce this for network-based code.

## Contribute

Issues and pull requests welcome.

## Thanks

Built with Jetpack Compose, Material 3, and Kotlin. Thanks to the Android and open-source communities.

## Links

- [Issues](https://github.com/VishaL6i9/RadianceAndroid/issues)
- [Discussions](https://github.com/VishaL6i9/RadianceAndroid/discussions)

This tool is for personal use. Use responsibly and follow your local laws.
