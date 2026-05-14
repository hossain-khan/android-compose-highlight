import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    jacoco
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
            enableUnitTestCoverage = true
        }
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
        managedDevices {
            localDevices {
                create("pixel2api30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                }
            }
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
}

kotlin {
    jvmToolchain(17)
}

jacoco {
    toolVersion = "0.8.14"
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
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)

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

// Maven Central publishing via vanniktech/gradle-maven-publish-plugin.
// Credentials are read from ORG_GRADLE_PROJECT_* environment variables (set in CI).
// Signing credentials: ORG_GRADLE_PROJECT_signingInMemoryKeyId,
//   ORG_GRADLE_PROJECT_signingInMemoryKey (ASCII-armored, full -----BEGIN block-----),
//   ORG_GRADLE_PROJECT_signingInMemoryKeyPassword
// Maven Central credentials: ORG_GRADLE_PROJECT_mavenCentralUsername,
//   ORG_GRADLE_PROJECT_mavenCentralPassword (Central Portal user token)
mavenPublishing {
    // Only sign when publishing to Maven Central (credentials provided in CI).
    // Local publishToMavenLocal does not require signing.
    if (hasProperty("signingInMemoryKey")) {
        signAllPublications()
    }

    coordinates(
        groupId    = "dev.hossain",
        artifactId = "compose-highlight",
        version    = requireNotNull(findProperty("VERSION_NAME") as String?) {
            "VERSION_NAME must be set in gradle.properties before publishing"
        },
    )

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

// JaCoCo test coverage report
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    val buildDir = layout.buildDirectory
    
    reports {
        csv.required.set(false)
        xml.required.set(true)
        xml.outputLocation.set(buildDir.file("reports/jacoco/jacocoTestReport.xml"))
        html.required.set(true)
        html.outputLocation.set(buildDir.dir("reports/jacoco/html"))
    }
    
    sourceDirectories.setFrom(files("$projectDir/src/main/kotlin"))
    classDirectories.setFrom(
        fileTree(buildDir) {
            include(
                "intermediates/compile_library_classes_jar/debug/**/*.class",
                "intermediates/javac/debug/**/*.class",
                "intermediates/classes/debug/**/*.class",
                "intermediates/built_in_kotlinc/debug/**/*.class"
            )
            exclude("**/R.class", "**/R\$*.class", "**/BuildConfig.*")
        }
    )
    // Use the actual coverage file location generated by enableUnitTestCoverage
    // Collect unit test (.exec), connected test (.ec), and managed device (.ec) coverage
    executionData.setFrom(
        fileTree(buildDir) {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "outputs/code_coverage/debugAndroidTest/connected/**/*.ec",
                "outputs/managed_device_code_coverage/**/pixel2api30/**/*.ec"
            )
        }
    )
}


