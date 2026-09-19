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
