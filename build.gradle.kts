plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.bcv.bridge) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinter) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.binary.compatibility.validator)
}

apiValidation {
    ignoredProjects.add("sample")

    // Internal implementation packages are not part of the public binary API surface.
    // Although declarations in these packages use Kotlin `internal` visibility, the Compose
    // compiler generates synthetic `public final class ComposableSingletons$...` classes
    // without Kotlin `internal` metadata, which would otherwise leak into the API dump.
    ignoredPackages.addAll(
        listOf(
            "dev.hossain.highlight.engine.internal",
            "dev.hossain.highlight.ui.internal",
        ),
    )

    // Exclude Compose compiler-generated `ComposableSingletons$<File>Kt` classes in the
    // public `ui` package. BCV's `ignoredClasses` matches exact binary class names (no
    // wildcards), so we register the generated singleton class name for each Kotlin source
    // file in `dev.hossain.highlight.ui`. If a new source file outside `dev.hossain.highlight.ui`
    // introduces `@Composable` lambdas, add its generated `ComposableSingletons$<File>Kt` class here.
    file("compose-highlight/src/main/kotlin/dev/hossain/highlight/ui")
        .listFiles { file -> file.isFile && file.extension == "kt" }
        ?.sortedBy { it.name }
        ?.forEach { file ->
            ignoredClasses.add("dev.hossain.highlight.ui.ComposableSingletons\$${file.nameWithoutExtension}Kt")
        }
}

tasks.register("apiDump") {
    group = "verification"
    description = "Updates the committed public API dump for all library modules."
    dependsOn(":compose-highlight:releaseApiDump")
}

tasks.register("apiCheck") {
    group = "verification"
    description = "Checks that the public API dump matches the compiled library code."
    dependsOn(":compose-highlight:releaseApiCheck")
}

val agpVersionProvider = libs.versions.agp
val kotlinVersionProvider = libs.versions.kotlin
val publishedLibraryVersion =
    requireNotNull(findProperty("VERSION_NAME") as String?) {
        "VERSION_NAME must be set in gradle.properties"
    }

tasks.register<dev.hossain.highlight.build.PublishedArtifactConsumerCheckTask>("publishedArtifactConsumerCheck") {
    group = "verification"
    description =
        "Verifies Maven POM and Gradle module metadata scopes, and compiles debug + minified release variants of a standalone consumer app using only the published artifact."
    dependsOn(
        ":compose-highlight:generatePomFileForMavenPublication",
        ":compose-highlight:generateMetadataFileForMavenPublication",
        ":compose-highlight:publishMavenPublicationToBuildLocalRepository",
    )

    pomFile.set(project(":compose-highlight").layout.buildDirectory.file("publications/maven/pom-default.xml"))
    metadataFile.set(project(":compose-highlight").layout.buildDirectory.file("publications/maven/module.json"))
    localRepoDir.set(layout.buildDirectory.dir("local-maven-repo"))
    templateDir.set(layout.projectDirectory.dir("tests/consumer-smoke-test"))
    consumerProjectDir.set(layout.buildDirectory.dir("consumer-smoke-test"))
    rootProjectDirectory.set(layout.projectDirectory)
    libraryVersion.set(publishedLibraryVersion)
    agpVersion.set(agpVersionProvider)
    kotlinVersion.set(kotlinVersionProvider)
}
