# TBC Conference App

Kotlin Multiplatform (KMP) Compose Multiplatform app targeting Android and iOS, with a Spring Boot backend sharing types via a common module.

## Tech Stack

| Layer | Library |
|-------|---------|
| UI | Compose Multiplatform 1.11.1 |
| Navigation | Voyager 1.1.0-beta03 |
| Networking (client) | Ktor 3.5.0 (OkHttp on Android, Darwin on iOS) |
| Networking (server) | Spring Boot 3.5.3 |
| Database | Exposed 0.61.0 + PostgreSQL |
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

### 3. Open the project

- **Android:** Open the project root directory in Android Studio
- **iOS:** Open `app/iosApp/iosApp.xcodeproj` in Xcode (the shared KMP framework builds automatically)

## Project Structure

```
conference-app/
├── common/                             # Shared KMP module (JVM + Android + iOS)
│   └── src/commonMain/
│       └── kotlin/.../common/
│           ├── model/                  # Session, Speaker data classes
│           └── api/                    # Routes (shared endpoint paths)
├── server/                             # Spring Boot backend
│   └── src/main/
│       ├── kotlin/.../server/
│       │   ├── Application.kt
│       │   ├── config/                 # DatabaseConfig, WebConfig
│       │   └── controller/             # REST endpoints
│       └── resources/
│           ├── application.yml         # Default profile config
│           └── application-dev.yml     # Dev profile (no PostgreSQL)
├── app/
│   ├── shared/                         # KMP shared module (UI, navigation, networking, DI)
│   │   └── src/
│   │       ├── commonMain/             # Cross-platform code
│   │       ├── androidMain/            # Android-specific (OkHttp engine, server URL)
│   │       └── iosMain/                # iOS-specific (Darwin engine, server URL)
│   ├── androidApp/                     # Android application module
│   └── iosApp/                         # Native iOS app (Swift wrapper)
├── scripts/                            # Dev scripts
│   └── start-dev.sh                    # Starts server + launches Android app
├── gradle/libs.versions.toml           # Version catalog
├── Makefile                            # Build & run shortcuts
└── build.gradle.kts                    # Root build configuration
```

## Build & Run

### Quick start (dev)

Start server and Android app together:

```bash
make dev
```

This launches the Spring Boot server (dev profile, no PostgreSQL needed), waits for it to be ready, then builds and launches the Android app. Press Ctrl+C to stop.

### Using the Makefile

```bash
make help          # Show all available targets
make dev           # Start server + Android app for development
make android-run   # Build, install, and launch on Android emulator/device
make ios-run       # Build and run on iOS Simulator
make server-run    # Start Spring Boot server
make build         # Full compilation check
make clean         # Clean all build outputs
```

### Using Gradle directly

```bash
# Build everything
./gradlew build

# Build Android app
./gradlew :app:androidApp:assembleDebug

# Install on connected Android device/emulator
./gradlew :app:androidApp:installDebug

# Start server (dev profile, no DB required)
./gradlew :server:bootRun
```

### Running on Android

1. Start an Android emulator from Android Studio (or connect a physical device with USB debugging enabled)
2. Run:
   ```bash
   make dev
   ```
   Or use the Run button in Android Studio with the `androidApp` configuration (start the server separately with `make server-run`).

### Running on iOS

1. Start the server: `make server-run`
2. Open `app/iosApp/iosApp.xcodeproj` in Xcode
3. Select a simulator (e.g. iPhone 17)
4. Press Run (⌘R)

Or from the terminal:

```bash
make server-run &
make ios-run
```

> **Note:** iOS builds require a Mac with Xcode installed. The shared KMP framework is compiled as a static framework for `iosArm64` and `iosSimulatorArm64`.

## Server API

The server runs on `http://localhost:8080` with dev profile (no PostgreSQL). Endpoints use shared types from the `common` module.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/sessions` | List sessions (hardcoded test data) |
| GET | `/api/v1/speakers` | List speakers (hardcoded test data) |
| POST | `/api/v1/sessions` | Echo session with `[received]` appended to title |

Test with curl:

```bash
curl http://localhost:8080/api/v1/sessions
curl -X POST -H 'Content-Type: application/json' \
  -d '{"id":"t","title":"Test","description":"d","speakerIds":[],"startTime":"2026-01-01T00:00:00Z","endTime":"2026-01-01T01:00:00Z","room":"A"}' \
  http://localhost:8080/api/v1/sessions
```

## Configuration

| File | Purpose |
|------|---------|
| `gradle/libs.versions.toml` | Centralized dependency versions |
| `gradle.properties` | JVM args, Kotlin style, AndroidX settings |
| `local.properties` | Local Android SDK path (gitignored) |
| `app/shared/build.gradle.kts` | KMP targets, shared dependencies |
| `app/androidApp/build.gradle.kts` | Android app config (SDK versions, app ID) |
| `server/build.gradle.kts` | Server dependencies (Spring Boot, Exposed) |
| `common/build.gradle.kts` | Shared model/API module config |

## Troubleshooting

### Gradle sync fails

- Ensure JDK 21+ is installed and configured: `java -version`
- Confirm Android SDK API 36 is installed via Android Studio → SDK Manager
- Run `./gradlew --stop` to kill stale Gradle daemons, then retry

### iOS build fails

- Ensure Xcode command line tools are set: `sudo xcode-select -s /Applications/Xcode.app`
- Clean the KMP framework: `./gradlew :app:shared:cleanIosSimulatorArm64Binaries`
- In Xcode: Product → Clean Build Folder (⇧⌘K)

### Server won't start

- Dev profile (default): no PostgreSQL needed, should start clean
- Prod profile: ensure PostgreSQL is running on `localhost:5432` with database `asmara`

### General environment issues

- Accept Xcode license: `sudo xcodebuild -license accept`
- Set `JAVA_HOME`: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`
