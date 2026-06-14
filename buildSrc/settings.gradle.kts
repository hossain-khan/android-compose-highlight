plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

// settings.gradle.kts for buildSrc is required in Gradle 9 to avoid warnings.
// Note: buildSrc is treated as an isolated included build and does not inherit
// pluginManagement or dependencyResolutionManagement from the root project settings.
rootProject.name = "buildSrc"
