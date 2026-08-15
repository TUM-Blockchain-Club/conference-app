# CLAUDE.md

Kotlin Multiplatform / Compose Multiplatform app for the TBC conference —
schedule browsing and an indoor venue map, on Android and iOS. Data lives in
Supabase Postgres and is cached on-device with SQLDelight; the UI always reads
from the cache, so the app works offline. See `README.md` for setup.

## Modules

| Module | Contents |
|---|---|
| `shared/` | Everything real: Compose UI, Voyager navigation, Koin DI, data layer. `commonMain` plus small `androidMain`/`iosMain` actuals (HTTP engine, SQLDelight driver). Tests in `commonTest` (+ `iosTest` for DI wiring). |
| `androidApp/` | Android host — `MainActivity` and manifest only. |
| `iosApp/` | Swift host — `iOSApp.swift` + `ContentView.swift`, which embeds the Compose view controller. |

UI work: read `.claude/skills/design-system` **first**. Colour, spacing, type
and radius values come from tokens, not literals; `ui/theme/Palette.kt` is the
only file allowed to contain hex colours. Longer form in `docs/DESIGN.md`;
floor-plan authoring in `docs/VENUE-MAP.md`.

## Before finishing any task

```bash
make format        # Kotlin — applies ktlint formatting
make format-swift  # only if you touched iosApp/*.swift (macOS)
make check         # lint + JVM tests + Android build
make ci            # on macOS: the above plus Swift lint, iOS tests, framework link
```

**Do not report a task complete while any of those fail.**

CI (`.github/workflows/ci.yml`) runs the same commands on every PR but only ever
*checks* — it never formats and never auto-commits. Unformatted code fails the
PR; applying the formatting is your job, here, before you push.

Narrower targets, for iterating without paying for the whole suite:

| Target | Does |
|---|---|
| `make lint` / `make lint-swift` | Format check only, no rewrite |
| `make test` / `make test-ios` | Shared test suite on JVM / iOS simulator |
| `make build-android` / `make build-ios` | Debug APK / shared framework link |

Kotlin formatting is **ratcheted to `origin/main`**: Spotless only checks and
only rewrites files that differ from `origin/main`. Untouched files keep their
existing formatting no matter what state it is in, so `make lint` is green on a
clean tree and a PR only ever has to format what it actually changed.

Rules ktlint cannot auto-fix are disabled in `.editorconfig` (Composable and
design-token naming, dangling file-level KDoc) — leaving them on would break
`spotlessApply`, not just the check. If you hit another such rule, disable it
there rather than hand-editing code to satisfy it.

> Editing `.editorconfig` does not take effect until `./gradlew --stop` — the
> Gradle daemon caches it for the life of the daemon.

## Run one emulator at a time

**Never leave the Android emulator and the iOS Simulator running at once.** Two
emulators saturate the machine and both become unusable.

Before starting one, shut the other down:

- Starting Android → shut down the iOS Simulator
- Starting iOS → shut down the Android emulator

`make android-run` and `make ios-run` already do this. Prefer them over calling
`gradlew installDebug`, `xcodebuild` or `emulator -avd` directly; if you do
bypass the Makefile, shut the other platform down yourself first.
