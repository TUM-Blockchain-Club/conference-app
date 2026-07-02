.PHONY: android-run ios-run build clean help

help: ## List all targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

android-run: ## Build, install, and launch on connected Android emulator/device
	./gradlew :app:androidApp:installDebug
	$(ANDROID_HOME)/platform-tools/adb shell am start -n com.conference.asmara.android/.MainActivity

ios-run: ## Build and run iOS app on simulator
	xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath app/iosApp/build
	xcrun simctl boot "iPhone 17" 2>/dev/null || true
	open -a Simulator
	xcrun simctl install booted app/iosApp/build/Build/Products/Debug-iphonesimulator/iosApp.app
	xcrun simctl launch booted com.conference.asmara

build: ## Full compilation check
	./gradlew build

clean: ## Clean all build outputs
	./gradlew clean
