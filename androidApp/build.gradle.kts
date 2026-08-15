plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
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
}
