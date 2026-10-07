plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices) apply false
}

// Push is credential-optional. The google-services plugin *fails the build*
// when google-services.json is missing, so applying it unconditionally would
// break CI and every contributor without a Firebase project. It is applied
// only when the file exists; :shared's generated PushConfig keys off the same
// file, so the two cannot disagree. See README.md#push-notifications.
if (rootProject.file("androidApp/google-services.json").exists()) {
    apply(plugin = libs.plugins.googleServices.get().pluginId)
}

android {
    namespace = "com.conference.asmara.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.conference.asmara.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    // :shared transitively supplies Compose Multiplatform's Android artifacts,
    // which publish under the same androidx.compose.* coordinates as the
    // androidx BOM. Declaring the BOM here as well put material3 and ui on the
    // classpath at two pinned versions at once — the classic source of
    // "renders correctly on iOS, subtly wrong on Android". Only the host-side
    // pieces CMP does not provide belong in this block.
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    // For AsmaraMessagingService. Inert without google-services.json: no
    // default FirebaseApp is configured, and nothing calls into Firebase
    // unless PushConfig.ENABLED bound the Firebase registrar.
    implementation(project.dependencies.platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}
