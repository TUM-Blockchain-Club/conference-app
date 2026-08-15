.PHONY: android-run ios-run stop-android stop-ios build clean seed seed-venue rls-audit help \
        format format-swift lint lint-swift test test-ios build-android build-ios check ci

# ANDROID_HOME is not exported by every shell profile, so fall back to the
# location Android Studio installs into. Overridable: `make ADB=/path/to/adb`.
ANDROID_SDK ?= $(if $(ANDROID_HOME),$(ANDROID_HOME),$(HOME)/Library/Android/sdk)
ADB ?= $(ANDROID_SDK)/platform-tools/adb

# Homebrew's libpq is keg-only, so `psql` is commonly installed but not on PATH.
# Overridable: `make PSQL=/path/to/psql`.
PSQL ?= $(shell command -v psql 2>/dev/null || echo "$$(brew --prefix libpq 2>/dev/null)/bin/psql")

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

# ---------------------------------------------------------------------------
# Quality checks. `check` is the any-host set, `ci` adds everything that needs
# a Mac; together they are what .github/workflows/ci.yml runs on every PR.
#
# CI only ever *checks* — it never formats and never auto-commits. Applying is
# yours: run `format` (and `format-swift`) before you push.
#
# Kotlin formatting is ratcheted to origin/main: only files that differ from it
# are checked or rewritten, so untouched legacy formatting is never in scope.
# ---------------------------------------------------------------------------

format: ## Apply Kotlin formatting (ktlint via Spotless) to files changed vs origin/main
	./gradlew spotlessApply

format-swift: ## Apply Swift formatting to iosApp/ (macOS — uses Xcode's bundled swift-format)
	xcrun swift-format format --in-place --recursive iosApp

lint: ## Check Kotlin formatting without changing anything
	./gradlew spotlessCheck

lint-swift: ## Check Swift formatting without changing anything (macOS)
	xcrun swift-format lint --strict --recursive iosApp

test: ## Run the shared test suite on the JVM
	./gradlew :shared:testAndroidHostTest

test-ios: ## Run the shared test suite on the iOS simulator target (macOS)
	./gradlew :shared:iosSimulatorArm64Test

build-android: ## Assemble the debug Android APK
	./gradlew :androidApp:assembleDebug

build-ios: ## Link the shared framework for simulator and device (macOS)
	./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkIosArm64

check: lint test build-android ## Lint + test + build Android (runs on any host)

ci: check lint-swift test-ios build-ios ## Everything CI runs, including the iOS half (macOS)

seed: ## Seed Supabase from supabase/seed/schedule.json (needs SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)
	cd scripts && npm install && npm run seed

seed-venue: ## Seed the venue map from supabase/seed/venue.json (same env vars; run `seed` first — features reference locations)
	cd scripts && npm install && npm run seed:venue

# The connection string is read out of the running CLI, never stored here: no
# credential belongs in the repo. See CLAUDE.md, "Secrets".
rls-audit: ## Audit RLS + grants against the local Supabase DB (needs `supabase start`)
	@[ -x "$(PSQL)" ] || { echo "psql not found ($(PSQL)) — brew install libpq, or make PSQL=/path/to/psql"; exit 1; }
	@DB_URL="$$(supabase status -o env | sed -n 's/^DB_URL=//p' | tr -d '\"')"; \
	[ -n "$$DB_URL" ] || { echo "no DB_URL from \`supabase status\` — is the local stack running?"; exit 1; }; \
	"$(PSQL)" "$$DB_URL" -f supabase/checks/rls-audit.sql
