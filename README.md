# TBC Conference App

Kotlin Multiplatform (KMP) Compose Multiplatform app targeting Android and iOS.

## Tech Stack

| Layer | Library |
|-------|---------|
| UI | Compose Multiplatform 1.11.1 |
| Navigation | Voyager 1.1.0-beta03 |
| Networking | Ktor 3.5.0 (OkHttp on Android, Darwin on iOS) |
| Backend | Supabase (supabase-kt 3.5.0, Postgrest) |
| Offline cache | SQLDelight 2.3.2 |
| DI | Koin 4.2.1 |
| Serialization | Kotlin Serialization |
| Language | Kotlin 2.3.20 |

## Prerequisites

### Required

| Tool | Version | Notes |
|------|---------|-------|
| **JDK** | 21+ | Required for Gradle and Kotlin compilation. Install via [SDKMAN](https://sdkman.io/) (`sdk install java 21-tem`) or [Adoptium](https://adoptium.net/) |
| **Android Studio** | Latest stable (Ladybug or newer) | Includes Android SDK, emulator, and Gradle tooling |
| **Android SDK** | API 36 (compileSdk/targetSdk), min API 26 | Install via Android Studio → SDK Manager |
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

### 3. Configure Supabase

Add your project's URL and publishable key to the same `local.properties`:

```properties
supabase.url=https://your-project-ref.supabase.co
supabase.publishableKey=sb_publishable_xxxxxxxxxxxxxxxxxxxxxxxx
```

See [Supabase](#supabase) below for details.

### 4. Open the project

- **Android:** Open the `conference-app` root directory in Android Studio
- **iOS:** Open `iosApp/iosApp.xcodeproj` in Xcode (the shared KMP framework builds automatically)

## Project Structure

```
conference-app/
├── shared/                          # KMP shared module
│   └── src/
│       ├── commonMain/              # Cross-platform code (UI, navigation, data layer, DI)
│       │   └── sqldelight/          # Local SQLite schema (offline cache)
│       ├── commonTest/              # Mapper/local/repository/integration tests
│       ├── androidMain/             # Android-specific implementations (OkHttp engine, SQLDelight driver)
│       ├── iosMain/                 # iOS-specific implementations (Darwin engine, SQLDelight driver)
│       └── iosTest/                 # DI wiring test (needs the iOS platform module)
├── androidApp/                      # Android application module
│   └── src/main/
│       └── kotlin/.../MainActivity.kt
├── iosApp/                          # Native iOS app (Swift wrapper)
│   └── iosApp/
│       ├── iOSApp.swift             # App entry point
│       └── ContentView.swift        # Hosts Compose UI via UIViewControllerRepresentable
├── supabase/                        # Migrations, RLS policies, import RPC, seed data + schema
├── scripts/                         # Node seed script (validates + imports supabase/seed/schedule.json)
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

## Supabase

The schedule (tracks, locations, speakers, events) lives in Supabase Postgres and is
cached locally on-device with SQLDelight; the app always reads from that cache
(`ScheduleRepository.observeSchedule()`) and only hits the network to refresh it.

### Project layout

| Path | Purpose |
|------|---------|
| `supabase/config.toml` | CLI project config, committed alongside migrations |
| `supabase/migrations/` | Schema, RLS policies, grants, and the `import_schedule()` RPC |
| `supabase/seed/schedule.schema.json` | JSON Schema for the seed document |
| `supabase/seed/schedule.json` | Editable programme data (slug-keyed, no UUIDs) |
| `scripts/seed-supabase.mjs` | Validates `schedule.json` and imports it via `import_schedule()` |

### Configuring the app

`SupabaseConfig.kt` is generated at build time by the `:shared:generateSupabaseConfig`
Gradle task, not hand-edited. It reads from `local.properties` (already gitignored, same
file as `sdk.dir`), falling back to a placeholder if the keys are missing:

```properties
supabase.url=https://your-project-ref.supabase.co
supabase.publishableKey=sb_publishable_xxxxxxxxxxxxxxxxxxxxxxxx
```

The publishable key is safe to have on disk; access is controlled by RLS, not by keeping
it secret.

### Linking a hosted project

```bash
# from the repo root, with the Supabase CLI installed
supabase link --project-ref <your-project-ref>
supabase db push   # applies supabase/migrations/*.sql
```

### Seeding data

```bash
export SUPABASE_URL=https://<your-project-ref>.supabase.co
export SUPABASE_SERVICE_ROLE_KEY=<service-role-key>   # bypasses RLS — keep out of git
make seed
```

`make seed` validates `supabase/seed/schedule.json` against the JSON Schema and checks
its track/location/speaker cross-references locally — before any network call. It then
calls the transactional `import_schedule()` RPC, which upserts by slug and prunes rows no
longer present in the file, so the JSON is the full source of truth for each run.

> **Note:** `SUPABASE_SERVICE_ROLE_KEY` bypasses Row Level Security entirely. Never commit
> it, put it in Kotlin source, or add it to `schedule.json`.

### Running against a local Supabase (Docker)

```bash
supabase start   # from the repo root; applies supabase/migrations/*.sql automatically
```

Prints an API URL and `anon`/`service_role` keys. Put the `anon` key in `local.properties`
as `supabase.publishableKey`. For `supabase.url`, use the emulator's alias for the host
loopback — **not** `127.0.0.1`, which resolves to the emulator itself:

```properties
supabase.url=http://10.0.2.2:54321
```

Android blocks plain HTTP by default; `androidApp` ships a network security config
(`res/xml/network_security_config.xml`) that allows cleartext to `10.0.2.2`/`localhost`
only, so this works without weakening the release build's HTTPS-only policy elsewhere.

For [seeding](#seeding-data), run it from the host (not the emulator), so use `127.0.0.1`
there and the `service_role` key printed by `supabase start` (not the `anon` key — that one
bypasses RLS and must never end up in `local.properties` or the app):

```bash
SUPABASE_URL=http://127.0.0.1:54321 SUPABASE_SERVICE_ROLE_KEY=<service_role_key> make seed
```

### Offline caching

`ScheduleRepository` treats the on-device SQLDelight database as the source of truth.
`refresh()` fetches from Supabase and replaces the cache in one transaction, but
`observeSchedule()` always emits from the cache regardless of whether that refresh
succeeded — so the UI keeps showing the last known programme when offline.

## Configuration

| File | Purpose |
|------|---------|
| `gradle/libs.versions.toml` | Centralized dependency versions |
| `gradle.properties` | JVM args, Kotlin style, AndroidX settings |
| `local.properties` | Local Android SDK path (gitignored) |
| `local.properties` (`supabase.*`) | Supabase URL + publishable key (see [Supabase](#supabase)) |
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
