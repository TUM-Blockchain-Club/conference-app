plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.spotless)
}

// Applied only at the root: the file globs below are repo-wide, so both modules
// are covered without configuring Spotless per project.
//
// `ratchetFrom` limits *both* spotlessCheck and spotlessApply to files that
// differ from origin/main. That keeps the baseline green without a repo-wide
// reformat commit (which would conflict with every in-flight branch) and pays
// the format debt down file by file as code is naturally touched. It also means
// CI must check out with `fetch-depth: 0` — without the full history the base
// ref cannot be resolved.
spotless {
    ratchetFrom("origin/main")
    kotlin {
        target("**/*.kt")
        // `build/` holds generated sources — SupabaseConfig.kt from
        // :shared:generateSupabaseConfig, plus SQLDelight's output.
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}
