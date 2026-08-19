import dev.hossain.highlight.build.GenerateThemesTask
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.roborazzi)
    jacoco
}

android {
    namespace = "dev.hossain.highlight"
    compileSdk = 37

    defaultConfig {
        minSdk = 24 // required by androidx.webkit
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
                    testedAbi = "x86"
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

// ────────────────────────────────────────────────────────────────────────────
// Build-time precompilation of bundled hljs themes.
//
// The eight built-in CSS themes (tomorrow, tomorrow-night, atom-one-dark, atom-one-light, github, github-dark, dracula, alucard)
// are parsed by buildSrc's CssThemeParser and emitted as a Kotlin source file so the
// runtime ThemeParser is never invoked for these themes. See buildSrc/.
// ────────────────────────────────────────────────────────────────────────────
val generateThemes = tasks.register<GenerateThemesTask>("generateThemes") {
    cssDir.set(layout.projectDirectory.dir("src/main/assets/compose-highlight/themes"))
    outputDir.set(layout.buildDirectory.dir("generated/source/themes/main"))
}

// AGP 9: use the variant Sources API. addGeneratedSourceDirectory takes a TaskProvider so
// AGP wires the task dependency for every consumer (compileKotlin, lint, dokka, annotation
// extraction, etc.) automatically.
androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(
            generateThemes,
            GenerateThemesTask::outputDir,
        )
    }
}

// kotlinter walks the source set too - exclude the generated tree so its formatting rules
// don't bicker with the emitter's output. We control its formatting at the emitter side.
// Use invariantSeparatorsPath so the substring match works on Windows where File.absolutePath
// uses backslashes.
private val GENERATED_THEMES_PATH_FRAGMENT = "generated/source/themes"
tasks.withType<org.jmailen.gradle.kotlinter.tasks.LintTask>().configureEach {
    exclude { it.file.invariantSeparatorsPath.contains(GENERATED_THEMES_PATH_FRAGMENT) }
}
tasks.withType<org.jmailen.gradle.kotlinter.tasks.FormatTask>().configureEach {
    exclude { it.file.invariantSeparatorsPath.contains(GENERATED_THEMES_PATH_FRAGMENT) }
}

// ────────────────────────────────────────────────────────────────────────────
// Roborazzi screenshot regression tests.
//
// Goldens live under src/test/snapshots/images/ and are committed to git.
// The pixel-diff change threshold is configured per-test inside
// HighlightScreenshotTestHelpers.kt (the Gradle DSL only owns outputDir).
//
// Workflow:
//   ./gradlew :compose-highlight:recordRoborazziDebug   - rewrites goldens
//   ./gradlew :compose-highlight:verifyRoborazziDebug   - fails on drift
// See compose-highlight/SCREENSHOT_TESTS.md for the full guide.
// ────────────────────────────────────────────────────────────────────────────
roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/test/snapshots/images"))
}

// Refresh the hand-captured highlight.js HTML fixtures used by the screenshot tests.
// The script loads the bundled highlight.min.js via Node's vm module and writes one *.html
// per snippet in snippets.json. Run this whenever snippets.json changes or after upgrading
// the bundled highlight.min.js. See compose-highlight/SCREENSHOT_TESTS.md.
tasks.register<Exec>("refreshHljsFixtures") {
    group = "verification"
    description = "Regenerate src/test/resources/highlight-fixtures/*.html from the bundled highlight.min.js. Requires Node.js 18+."
    workingDir = projectDir
    commandLine("node", "scripts/generate-hljs-fixtures.js")
    inputs.file("scripts/generate-hljs-fixtures.js")
    inputs.file("src/test/resources/highlight-fixtures/snippets.json")
    inputs.file("src/main/assets/compose-highlight/highlight.min.js")
    outputs.dir("src/test/resources/highlight-fixtures")
}

// JVM microbenchmark for HtmlToAnnotatedString (hand-rolled, no plugin).
//
// The benchmark class is dev.hossain.highlight.benchmark.HtmlParserBenchmark in
// src/test and is skipped by default via Assume.assumeTrue. Opt in by passing
// -PrunBenchmark=true (Gradle property) - see the configuration block below.
//
// Workflow for a parser swap:
//   ./gradlew :compose-highlight:testDebugUnitTest \
//     --tests "dev.hossain.highlight.benchmark.HtmlParserBenchmark" \
//     -PrunBenchmark=true --rerun-tasks
//   cp compose-highlight/build/reports/benchmarks/html-parser-baseline-*.json /tmp/bench-baseline.json
//   # ... swap parser ...
//   # re-run, diff JSON.
if (providers.gradleProperty("runBenchmark").orNull == "true") {
    // tasks.withType<Test>() configures all Test tasks (testDebugUnitTest is registered
    // lazily by AGP, so tasks.named would fail at configuration time).
    tasks.withType<Test>().configureEach {
        systemProperty("runBenchmark", "true")
        outputs.upToDateWhen { false }
        testLogging {
            showStandardStreams = true
            events("passed", "skipped", "failed", "standardOut")
        }
    }
}

if (providers.gradleProperty("composeReports").orNull == "true") {
    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}

jacoco {
    toolVersion = "0.8.14"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.webkit)
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
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    // Provides the real Java implementation of JSONObject for pure JVM unit tests.
    // This overrides the unmocked Android SDK stub at test runtime without requiring Robolectric.
    // Source: https://mvnrepository.com/artifact/org.json/json
    testImplementation(libs.json)

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
    // Suppress internal packages so they don't appear in the published API site - they hold
    // implementation-only helpers that callers must not reference directly.
    dokkaSourceSets.configureEach {
        includes.from(layout.projectDirectory.file("MODULE.md"))
        perPackageOption {
            matchingRegex.set(""".*\.internal(\..*)?""")
            suppress.set(true)
        }
    }
    dokkaPublications.html {
        // Output to docs/api/ - Zensical passes non-Markdown files through verbatim,
        // so this ends up at /api/ on the deployed GitHub Pages site.
        outputDirectory.set(rootDir.resolve("docs/api"))
    }
    // Theme overrides + chrome rewrite that align Dokka's HTML output with the Zensical
    // Material site at /. This uses Dokka's official HTML customization extension points:
    // https://kotlinlang.org/docs/dokka-html.html#customization
    // - customStyleSheets: CSS overrides for Dokka variables/chrome
    // - customAssets: JS wrapper that rebuilds page chrome around #main
    // See compose-highlight/dokka-theme/README.md for refresh procedure and breakage notes.
    pluginsConfiguration.html {
        customStyleSheets.from(
            layout.projectDirectory.file("dokka-theme/zensical-overrides.css"),
            layout.projectDirectory.file("dokka-theme/zensical-assets/main.20815dad.min.css"),
            layout.projectDirectory.file("dokka-theme/zensical-assets/palette.dfe2e883.min.css"),
        )
        customAssets.from(layout.projectDirectory.file("dokka-theme/dokka-zensical-chrome.js"))
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

