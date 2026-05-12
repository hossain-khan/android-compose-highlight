plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinter) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.nexus.publish)
}

// Publishes to Sonatype's OSSRH Staging API compatibility layer, which forwards
// to the Central Publisher Portal (central.sonatype.com). Credentials must be
// Central Portal user tokens — NOT legacy OSSRH credentials.
// Generate tokens at: https://central.sonatype.com/account
nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
            password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
        }
    }
}
