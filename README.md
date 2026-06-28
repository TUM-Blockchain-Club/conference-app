# TBC Conference App

Kotlin Multiplatform (KMP) Compose Multiplatform app targeting Android and iOS.

## Tech Stack

| Layer | Library |
|-------|---------|
| UI | Compose Multiplatform 1.11.1 |
| Navigation | Voyager 1.1.0-beta03 |
| Networking | Ktor 3.5.0 (OkHttp on Android, Darwin on iOS) |
| DI | Koin 4.2.1 |
| Serialization | Kotlin Serialization |
| Language | Kotlin 2.3.20 |

## Prerequisites

### Required

| Tool | Version | Notes |
|------|---------|-------|
| **JDK** | 21+ | Required for Gradle and Kotlin compilation. Install via [SDKMAN](https://sdkman.io/) (`sdk install java 21-tem`) or [Adoptium](https://adoptium.net/) |
| **Android Studio** | Latest stable (Ladybug or newer) | Includes Android SDK, emulator, and Gradle tooling |
| **Android SDK** | API 36 (compileSdk/targetSdk), min API 24 | Install via Android Studio → SDK Manager |
| **Xcode** | 16+ | Required for iOS builds. Install from the Mac App Store |
| **Xcode Command Line Tools** | Latest | `xcode-select --install` |

### Recommended

- [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform) for Android Studio

## Setup

### 1. Clone the repository

```bash
git clone <repo-url>
cd conference-app
```

### 2. Configure Android SDK path

Create or verify `local.properties` in the project root:

```properties
sdk.dir=/Users/<your-username>/Library/Android/sdk
```

Android Studio creates this automatically when you open the project.

### 4. Open the project

- **Android:** Open the `conference-app` root directory in Android Studio
- **iOS:** Open `iosApp/iosApp.xcodeproj` in Xcode (the shared KMP framework builds automatically)

## Project Structure

```
conference-app/
├── shared/                          # KMP shared module
│   └── src/
│       ├── commonMain/              # Cross-platform code (UI, navigation, networking, DI)
│       ├── androidMain/             # Android-specific implementations (OkHttp engine)
│       └── iosMain/                 # iOS-specific implementations (Darwin engine, MainViewController)
├── androidApp/                      # Android application module
│   └── src/main/
│       └── kotlin/.../MainActivity.kt
├── iosApp/                          # Native iOS app (Swift wrapper)
│   └── iosApp/
│       ├── iOSApp.swift             # App entry point
│       └── ContentView.swift        # Hosts Compose UI via UIViewControllerRepresentable
├── gradle/libs.versions.toml        # Version catalog
├── Makefile                         # Build & run shortcuts
└── build.gradle.kts                 # Root build configuration
```

## Build & Run

### Using the Makefile

```bash
make help          # Show all available targets
make android-run   # Build, install, and launch on Android emulator/device
make ios-run       # Build and run on iOS Simulator
make build         # Full compilation check
make clean         # Clean all build outputs
```

### Using Gradle directly

```bash
# Build everything
./gradlew build

# Build Android app
./gradlew :androidApp:assembleDebug

# Install on connected Android device/emulator
./gradlew :androidApp:installDebug
```

### Running on Android

1. Start an Android emulator from Android Studio (or connect a physical device with USB debugging enabled)
2. Run:
   ```bash
   make android-run
   ```
   Or use the Run button in Android Studio with the `androidApp` configuration.

### Running on iOS

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select a simulator (e.g. iPhone 17)
3. Press Run (⌘R)

Or from the terminal:

```bash
make ios-run
```

> **Note:** iOS builds require a Mac with Xcode installed. The shared KMP framework is compiled as a static framework for `iosArm64` and `iosSimulatorArm64`.

## Configuration

| File | Purpose |
|------|---------|
| `gradle/libs.versions.toml` | Centralized dependency versions |
| `gradle.properties` | JVM args, Kotlin style, AndroidX settings |
| `local.properties` | Local Android SDK path (gitignored) |
| `shared/build.gradle.kts` | KMP targets, shared dependencies |
| `androidApp/build.gradle.kts` | Android app config (SDK versions, app ID) |

## Troubleshooting

### Gradle sync fails

- Ensure JDK 21+ is installed and configured: `java -version`
- Confirm Android SDK API 36 is installed via Android Studio → SDK Manager
- Run `./gradlew --stop` to kill stale Gradle daemons, then retry

### iOS build fails

- Ensure Xcode command line tools are set: `sudo xcode-select -s /Applications/Xcode.app`
- Clean the KMP framework: `./gradlew :shared:cleanIosSimulatorArm64Binaries`
- In Xcode: Product → Clean Build Folder (⇧⌘K)

### General environment issues

- Accept Xcode license: `sudo xcodebuild -license accept`
- Set `JAVA_HOME`: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`
