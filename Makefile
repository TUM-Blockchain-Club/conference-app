.PHONY: android-run ios-run build clean seed seed-venue help

help: ## List all targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

android-run: ## Build, install, and launch on connected Android emulator/device
	./gradlew :androidApp:installDebug
	$(ANDROID_HOME)/platform-tools/adb shell am start -n com.conference.asmara.android/.MainActivity

ios-run: ## Build and run iOS app on simulator
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
