.PHONY: android-run ios-run stop-android stop-ios build clean seed seed-venue help

# ANDROID_HOME is not exported by every shell profile, so fall back to the
# location Android Studio installs into. Overridable: `make ADB=/path/to/adb`.
ANDROID_SDK ?= $(if $(ANDROID_HOME),$(ANDROID_HOME),$(HOME)/Library/Android/sdk)
ADB ?= $(ANDROID_SDK)/platform-tools/adb

help: ## List all targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

# Run one emulator at a time. Two of them saturate the machine and both become
# unusable, so each run target shuts the other platform down first. Every step
# is prefixed with `-` : "nothing was running" is the common case, not a
# failure. See CLAUDE.md.
stop-android: ## Shut down any running Android emulator
	-@[ -x "$(ADB)" ] && $(ADB) devices | awk '/^emulator-/ {print $$1}' | xargs -I{} $(ADB) -s {} emu kill || true

stop-ios: ## Shut down the iOS Simulator
	-@command -v xcrun >/dev/null && xcrun simctl shutdown all || true
	-@osascript -e 'quit app "Simulator"' >/dev/null 2>&1 || true

android-run: stop-ios ## Build, install, and launch on connected Android emulator/device (stops the iOS Simulator first)
	./gradlew :androidApp:installDebug
	$(ADB) shell am start -n com.conference.asmara.android/.MainActivity

ios-run: stop-android ## Build and run iOS app on simulator (stops the Android emulator first)
	xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath iosApp/build
	xcrun simctl boot "iPhone 17" 2>/dev/null || true
	open -a Simulator
	xcrun simctl install booted iosApp/build/Build/Products/Debug-iphonesimulator/iosApp.app
	xcrun simctl launch booted com.conference.asmara

build: ## Full compilation check
	./gradlew build

clean: ## Clean all build outputs
	./gradlew clean

seed: ## Seed Supabase from supabase/seed/schedule.json (needs SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)
	cd scripts && npm install && npm run seed

seed-venue: ## Seed the venue map from supabase/seed/venue.json (same env vars; run `seed` first — features reference locations)
	cd scripts && npm install && npm run seed:venue
