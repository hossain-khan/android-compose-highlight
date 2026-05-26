// Inherit pluginManagement / dependencyResolutionManagement from the root settings.
// buildSrc shares plugin and repository config with the main build by default,
// but Gradle 9 emits a warning if settings.gradle.kts is missing.
rootProject.name = "buildSrc"
