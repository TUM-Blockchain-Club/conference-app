plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.spring.boot.starter)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    runtimeOnly(libs.postgresql)
}
