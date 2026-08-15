# CLAUDE.md

## Run one emulator at a time

**Never leave the Android emulator and the iOS Simulator running at once.** Two
emulators saturate the machine and both become unusable.

Before starting one, shut the other down:

- Starting Android → shut down the iOS Simulator
- Starting iOS → shut down the Android emulator

`make android-run` and `make ios-run` already do this. Prefer them over calling
`gradlew installDebug`, `xcodebuild` or `emulator -avd` directly; if you do
bypass the Makefile, shut the other platform down yourself first.
