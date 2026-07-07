# Radiance

A modern Android application for intelligent brightness management with automatic media playback detection.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Material3](https://img.shields.io/badge/Material%203-Dynamic%20Colors-purple.svg)](https://m3.material.io/)
[![License](https://img.shields.io/badge/License-AGPL%20v3-orange.svg)](LICENSE)
[![APK Size](https://img.shields.io/badge/APK%20Size-16.66%20MB-blue.svg)]()

## Overview

Radiance is a lightweight brightness control application for Android that intelligently manages screen brightness during media playback. Built entirely with **Jetpack Compose** and **Material 3** with dynamic system colors, it provides a modern, responsive interface with zero bloat.

**Key Achievement:** 76% APK size reduction compared to Flutter (69.5 MB → 16.66 MB)

## Features

### System Brightness Control
- **Manual brightness adjustment** with precise 0-255 slider control
- **Quick presets** (Min, Mid, Max) for common brightness levels
- **Automatic/Manual mode toggle** for system brightness adaptation
- **Real-time percentage display** of current brightness level
- **Smooth animations** during transitions

### Window-Level Brightness Override
- **Instant window brightness control** (no permissions required)
- **Overrides system settings** at the application window level
- **Quick action buttons** (Max, Medium, Reset)
- **Independent from system brightness** for granular control

### Media Playback Monitoring
- **Automatic detection** of media playback via MediaSession API
- **Auto-boost brightness** during playback (customizable)
- **Auto-restore brightness** when playback stops
- **Foreground service** with persistent notification
- **Auto-start on boot** (optional)

### Modern UI/UX
- **Material 3 design system** with dynamic system colors (Android 12+)
- **Dark/Light mode** automatically detected from system settings
- **Elegant card-based layout** with smooth animations
- **Responsive design** adapting to all screen sizes
- **FilterChips** for mode selection with visual feedback
- **ElevatedCards** with shadows for depth
- **Real-time status bar** with animated transitions

### Settings & Persistence
- **SharedPreferences** for settings persistence
- **Auto-start on device boot** toggle
- **Automatic state restoration** after app restart
- **Permission state tracking** with real-time UI updates

## Screenshots

_Coming soon_

## Getting Started

### Prerequisites

**Runtime Requirements:**
- Android 7.0 (API 24) or higher
- `WRITE_SETTINGS` permission (granted via system settings)
- Optional: `FOREGROUND_SERVICE` for media monitoring

**Development Requirements:**
- Android Studio 2024.1.1 or later
- JDK 23
- Android SDK 35+
- Gradle 8.12

### Installation

#### From Source

1. Clone the repository:
```bash
git clone https://github.com/VishaL6i9/RadianceAndroid.git
cd RadianceAndroid
```

2. Build the debug APK:
```bash
cd android
./gradlew assembleDebug
```

3. Install on your device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or build and install in one step:
```bash
./gradlew installDebug
```

#### APK Download

Download the latest APK from [Releases](https://github.com/VishaL6i9/RadianceAndroid/releases)

#### First Launch

1. Open the app
2. Grant `WRITE_SETTINGS` permission when prompted
3. Adjust system brightness using the slider
4. (Optional) Enable media monitoring for auto-brightness boost

## Usage

### Adjusting System Brightness

1. Open the app
2. Use the **System Brightness** slider to adjust (0-255)
3. Or tap one of the quick buttons:
   - **Min** - Set to minimum brightness (0)
   - **Mid** - Set to middle brightness (128)
   - **Max** - Set to maximum brightness (255)
4. Toggle **Manual/Auto** mode using the FilterChips

### Window Brightness Override

1. Navigate to **Window Brightness** card
2. Tap **Max** to force maximum window brightness
3. Tap **Mid** for medium window brightness
4. Tap **Reset** to remove the override

### Auto-Brightness Monitoring

1. Navigate to **Media Monitoring** card
2. Tap **Start** to enable media playback detection
3. The app will automatically boost brightness during playback
4. (Optional) Enable **Auto-start on boot** for persistent monitoring

### Permissions

The app requires the following permissions:

- **WRITE_SETTINGS** - To modify system brightness (requested on first launch)
- **FOREGROUND_SERVICE** - To run media monitoring in the background
- **POST_NOTIFICATIONS** - To show persistent monitoring notification
- **RECEIVE_BOOT_COMPLETED** - To auto-start on device boot

## Architecture

### Project Structure

```
force_max_brightness/
├── android/
│   └── app/
│       ├── src/main/
│       │   ├── kotlin/com/example/force_max_brightness/
│       │   │   ├── MainActivity.kt          # Entry point (Compose Activity)
│       │   │   ├── MediaMonitorService.kt   # Foreground service
│       │   │   ├── BootReceiver.kt          # Auto-start receiver
│       │   │   └── ui/
│       │   │       ├── Screens.kt           # Main UI composables
│       │   │       ├── MainActivity.kt      # Compose activity setup
│       │   │       └── theme/
│       │   │           ├── Theme.kt         # Material 3 + dynamic colors
│       │   │           └── Type.kt          # Typography definitions
│       │   └── AndroidManifest.xml
│       ├── build.gradle.kts
│       └── ...
└── ...
```

### Architecture Pattern

```
MainActivity (Compose Activity)
├── BrightnessControlScreen (Main Composable)
│   ├── PermissionCard          # Request & display permission status
│   ├── BrightnessControlCard   # Slider + Quick preset buttons
│   ├── WindowBrightnessCard    # Window override controls
│   ├── MediaMonitorCard        # Service management + auto-start
│   └── StatusBar               # Real-time status display
├── MediaMonitorService         # Foreground service for playback detection
├── BootReceiver               # Auto-start on device boot
└── Theme (Material 3 + Dynamic)
```

### Key APIs Used

| API | Purpose |
|-----|---------|
| `Settings.System.SCREEN_BRIGHTNESS` | Get/set system brightness (0-255) |
| `Settings.System.SCREEN_BRIGHTNESS_MODE` | Control automatic brightness mode |
| `Window.LayoutParams.screenBrightness` | Window-level brightness override |
| `MediaSessionManager` | Detect media playback |
| `ActivityResultLauncher` | Permission handling |
| `SharedPreferences` | Settings persistence |
| `ForegroundService` | Background media monitoring |

## Technical Details

### Theming

The app uses Material 3 with dynamic system colors:

- **Android 12+**: Colors extracted from device wallpaper (Material You)
- **Android 11 & below**: Fallback Material 3 color palette (orange/amber)
- **Dark/Light mode**: Automatically detected from system settings via `isSystemInDarkTheme()`

### Performance

- **APK Size**: 16.66 MB (76% smaller than Flutter equivalent)
- **Build Time**: ~6 seconds (debug build)
- **Startup Time**: <1 second
- **Memory Usage**: Minimal (Compose-only, no runtime engine)

### APK Breakdown

| Component | Size |
|-----------|------|
| classes.dex | 12.1 MB |
| resources.arsc | 0.6 MB |
| Native code | 0.01 MB |
| Other assets | 3.98 MB |
| **Total** | **16.66 MB** |

## Limitations & Future Enhancements

### Current Limitations
1. No HDR-specific detection (all video playback triggers boost)
2. Foreground service requires notification access on some devices
3. OEM-specific brightness APIs (e.g., OnePlus HBM) require special handling
4. Unrooted devices cannot modify low-level system brightness parameters

### Planned Enhancements
- [ ] HDR-specific content detection
- [ ] App-specific brightness profiles
- [ ] Scheduled brightness control
- [ ] Advanced notification controls
- [ ] Battery optimization analysis
- [ ] Custom brightness curves
- [ ] Multi-user support

## Development

### Build Configuration

- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 35 (Android 16)
- **compileSdk**: 35
- **Kotlin Version**: 2.1.0
- **Gradle Version**: 8.12

### Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug

# Run on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## License

This project is licensed under the **AGPL v3 License** - see the [LICENSE](LICENSE) file for details.

### License Choice Rationale

The AGPL v3 license was chosen to ensure that improvements to this brightness control system—especially any enhancements involving networked features (such as the MediaSession monitoring)—remain available to the community. This copyleft enforcement is more appropriate than GPL v3 for an app with potential remote/networked aspects.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## Acknowledgements

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Designed with [Material 3](https://m3.material.io/)
- Powered by [Kotlin](https://kotlinlang.org/)
- Android development community

## Contact & Support

- **Issues**: [GitHub Issues](https://github.com/VishaL6i9/RadianceAndroid/issues)
- **Discussions**: [GitHub Discussions](https://github.com/VishaL6i9/RadianceAndroid/discussions)

---

**Disclaimer:** This application is provided as-is for personal use. Always use technology responsibly and in accordance with local laws and regulations.
