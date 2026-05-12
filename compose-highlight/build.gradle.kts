plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.dokka)
    `maven-publish`
    `signing`
}

android {
    namespace = "dev.hossain.highlight"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
            "DEBUGGABLE,EMULATOR,LOW-BATTERY,UNLOCKED"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Expose the release variant as a Maven component for JitPack / maven-publish.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.webkit)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.coroutines.android)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core) // force upgrade from 3.5.0 → 3.7.0
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.benchmark.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

dokka {
    moduleName.set("compose-highlight")
    // Include MODULE.md as the module-level documentation page in the generated API docs.
    dokkaSourceSets.configureEach {
        includes.from(layout.projectDirectory.file("MODULE.md"))
    }
    dokkaPublications.html {
        // Output to docs/api/ so GitHub Pages can serve from the docs/ folder
        outputDirectory.set(rootDir.resolve("docs/api"))
    }
}

// Maven Central requires a -javadoc.jar artifact. Kotlin projects commonly ship
// Dokka HTML output as the javadoc artifact since there is no Javadoc equivalent.
val dokkaHtmlJar by tasks.registering(Jar::class) {
    dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
    from(rootDir.resolve("docs/api"))
    archiveClassifier.set("javadoc")
}

// Maven Central publishing. JitPack also uses this block — it overrides groupId and version
// at build time from the git tag, so both registries are served from the same publication.
// Repository config lives in root build.gradle.kts (nexusPublishing block).
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifact(tasks["dokkaHtmlJar"])
                groupId    = "dev.hossain"
                artifactId = "compose-highlight"
                version    = requireNotNull(findProperty("VERSION_NAME") as String?) {
                    "VERSION_NAME must be set in gradle.properties before publishing"
                }

                pom {
                    name.set("Android Compose Syntax Highlight")
                    description.set("Jetpack Compose syntax highlighting powered by Highlight.js")
                    url.set("https://github.com/hossain-khan/android-compose-highlight")
                    inceptionYear.set("2026")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("hossain-khan")
                            name.set("Hossain Khan")
                            email.set("hello@hossain.dev")
                            url.set("https://hossain.dev")
                            organization.set("Independent")
                            roles.add("developer")
                            roles.add("maintainer")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/hossain-khan/android-compose-highlight.git")
                        developerConnection.set("scm:git:ssh://github.com/hossain-khan/android-compose-highlight.git")
                        url.set("https://github.com/hossain-khan/android-compose-highlight")
                    }
                }
            }
        }
    }

    signing {
        // SIGNING_KEY must be the ASCII-armored private key — the literal text starting with
        // "-----BEGIN PGP PRIVATE KEY BLOCK-----". Export it with:
        //   gpg --export-secret-keys --armor <FINGERPRINT>
        // Store that output directly as the SIGNING_KEY secret (no base64 encoding).
        val signingKeyId = findProperty("signing.keyId") as String? ?: System.getenv("SIGNING_KEY_ID")
        val signingKey = findProperty("signing.key") as String? ?: System.getenv("SIGNING_KEY")
        val signingPassword = findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")
        isRequired = signingKeyId != null && signingKey != null && signingPassword != null
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications["release"])
    }
}
