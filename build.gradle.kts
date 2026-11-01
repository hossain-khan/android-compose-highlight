import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

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
val libraryVersion =
    requireNotNull(findProperty("VERSION_NAME") as String?) {
        "VERSION_NAME must be set in gradle.properties"
    }

tasks.register("publishedArtifactConsumerCheck") {
    group = "verification"
    description =
        "Verifies Maven POM and Gradle module metadata scopes, and compiles debug + minified release variants of a standalone consumer app using only the published artifact."
    dependsOn(
        ":compose-highlight:generatePomFileForMavenPublication",
        ":compose-highlight:generateMetadataFileForMavenPublication",
        ":compose-highlight:publishMavenPublicationToBuildLocalRepository",
    )

    val pomFile = project(":compose-highlight").layout.buildDirectory.file("publications/maven/pom-default.xml")
    val metadataFile = project(":compose-highlight").layout.buildDirectory.file("publications/maven/module.json")
    val localRepoDir = layout.buildDirectory.dir("local-maven-repo")
    val consumerProjectDir = layout.buildDirectory.dir("consumer-smoke-test")
    val rootProjectDir = layout.projectDirectory

    doLast {
        // 1. Validate generated Maven POM dependencyManagement and dependency scopes.
        val pom = pomFile.get().asFile
        check(pom.isFile) { "Missing generated POM file: $pom" }
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom)
        doc.documentElement.normalize()

        val managedBomDeps = mutableSetOf<String>()
        val depMgmtNodes = doc.documentElement.getElementsByTagName("dependencyManagement")
        for (i in 0 until depMgmtNodes.length) {
            val depMgmtNode = depMgmtNodes.item(i) as? Element ?: continue
            if (depMgmtNode.parentNode != doc.documentElement) continue
            val depsList = depMgmtNode.getElementsByTagName("dependency")
            for (j in 0 until depsList.length) {
                val dep = depsList.item(j) as? Element ?: continue
                val group = dep.getElementsByTagName("groupId").item(0)?.textContent.orEmpty()
                val artifact = dep.getElementsByTagName("artifactId").item(0)?.textContent.orEmpty()
                val type = dep.getElementsByTagName("type").item(0)?.textContent.orEmpty()
                val scope = dep.getElementsByTagName("scope").item(0)?.textContent.orEmpty()
                managedBomDeps.add("$group:$artifact:$type:$scope")
            }
        }
        check("androidx.compose:compose-bom:pom:import" in managedBomDeps) {
            "Expected POM <dependencyManagement> to import androidx.compose:compose-bom:pom:import, but found: $managedBomDeps"
        }

        val pomScopes = mutableMapOf<String, String>()
        val dependenciesNodes = doc.documentElement.getElementsByTagName("dependencies")
        for (i in 0 until dependenciesNodes.length) {
            val dependenciesNode = dependenciesNodes.item(i)
            if (dependenciesNode.parentNode != doc.documentElement) continue
            val children = dependenciesNode.childNodes
            for (j in 0 until children.length) {
                val dep = children.item(j) as? Element ?: continue
                if (dep.tagName != "dependency") continue
                val group = dep.getElementsByTagName("groupId").item(0)?.textContent.orEmpty()
                val artifact = dep.getElementsByTagName("artifactId").item(0)?.textContent.orEmpty()
                val scope = dep.getElementsByTagName("scope").item(0)?.textContent.orEmpty()
                pomScopes["$group:$artifact"] = scope
            }
        }

        val expectedCompileDeps =
            setOf(
                "androidx.compose.ui:ui",
                "androidx.compose.foundation:foundation",
                "org.jetbrains.kotlinx:kotlinx-coroutines-core",
                "org.jetbrains.kotlin:kotlin-stdlib",
            )
        val expectedRuntimeOnlyDeps =
            setOf(
                "androidx.core:core-ktx",
                "androidx.webkit:webkit",
                "org.jetbrains.kotlinx:kotlinx-coroutines-android",
                "androidx.compose.material3:material3",
                "androidx.compose.ui:ui-tooling-preview",
            )

        expectedCompileDeps.forEach { coord ->
            check(pomScopes[coord] == "compile") {
                "Expected POM dependency '$coord' to have scope 'compile', but found '${pomScopes[coord]}' in $pomScopes"
            }
        }
        expectedRuntimeOnlyDeps.forEach { coord ->
            check(pomScopes[coord] == "runtime") {
                "Expected implementation-only POM dependency '$coord' to have scope 'runtime', but found '${pomScopes[coord]}' in $pomScopes"
            }
        }
        val expectedAllPomDeps = expectedCompileDeps + expectedRuntimeOnlyDeps
        check(pomScopes.keys == expectedAllPomDeps) {
            "Unexpected POM dependencies. Expected $expectedAllPomDeps, but found ${pomScopes.keys}"
        }

        // 2. Validate generated Gradle Module Metadata (java-api variant).
        val moduleJson = metadataFile.get().asFile.readText()
        val apiVariantBlock =
            moduleJson.substringAfter("\"name\": \"releaseVariantReleaseApiPublication\"", missingDelimiterValue = "")
                .substringBefore("\"files\":", missingDelimiterValue = "")
        check(apiVariantBlock.isNotEmpty()) {
            "Could not find releaseVariantReleaseApiPublication in ${metadataFile.get().asFile}"
        }

        val depObjectRegex = Regex("""\{\s*"group":\s*"([^"]+)",\s*"module":\s*"([^"]+)"[^}]*\}""")
        val apiVariantCoords =
            depObjectRegex.findAll(apiVariantBlock)
                .map { match -> "${match.groupValues[1]}:${match.groupValues[2]}" }
                .toSet()

        val expectedApiVariantCoords =
            setOf(
                "androidx.compose:compose-bom",
                "androidx.compose.ui:ui",
                "androidx.compose.foundation:foundation",
                "org.jetbrains.kotlinx:kotlinx-coroutines-core",
                "org.jetbrains.kotlin:kotlin-stdlib",
            )
        check(apiVariantCoords == expectedApiVariantCoords) {
            "Expected java-api variant in module.json to contain exact dependencies $expectedApiVariantCoords, " +
                "but found $apiVariantCoords in:\n$apiVariantBlock"
        }

        // 3. Build a clean standalone Android consumer project (debug + minified release)
        // using ONLY the published `dev.hossain:compose-highlight` Maven artifact.
        val consumerDir = consumerProjectDir.get().asFile
        consumerDir.deleteRecursively()
        consumerDir.mkdirs()

        val localPropertiesFile = rootProjectDir.file("local.properties").asFile
        if (localPropertiesFile.isFile) {
            val props = Properties()
            localPropertiesFile.inputStream().use { props.load(it) }
            listOf("sdk.dir", "ndk.dir", "cmake.dir").forEach { key ->
                val rawPath = props.getProperty(key) ?: return@forEach
                val resolved = rootProjectDir.asFile.resolve(rawPath).canonicalFile
                props.setProperty(key, resolved.absolutePath)
            }
            consumerDir.resolve("local.properties").outputStream().use { out ->
                props.store(out, "Normalized from root local.properties")
            }
        }

        val repoUri = localRepoDir.get().asFile.toURI()
        val agpVersion = agpVersionProvider.get()
        val kotlinVersion = kotlinVersionProvider.get()

        consumerDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    maven(url = uri("$repoUri"))
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "consumer-smoke-test"
            """.trimIndent(),
        )

        consumerDir.resolve("gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
            android.useAndroidX=true
            """.trimIndent(),
        )

        consumerDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.android.application") version "$agpVersion"
                id("org.jetbrains.kotlin.plugin.compose") version "$kotlinVersion"
            }

            android {
                namespace = "dev.hossain.highlight.consumer"
                compileSdk = 37

                defaultConfig {
                    applicationId = "dev.hossain.highlight.consumer"
                    minSdk = 24
                    targetSdk = 37
                    versionCode = 1
                    versionName = "1.0"
                }

                buildTypes {
                    release {
                        isMinifyEnabled = true
                        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
                    }
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                buildFeatures {
                    compose = true
                }
            }

            kotlin {
                jvmToolchain(17)
            }

            dependencies {
                // Intentionally declare ONLY the published library artifact to verify that all
                // ABI-exposed types (Compose UI, Foundation, Coroutines StateFlow) are provided transitively.
                implementation("dev.hossain:compose-highlight:$libraryVersion")
            }
            """.trimIndent(),
        )

        val mainDir = consumerDir.resolve("src/main")
        mainDir.mkdirs()
        mainDir.resolve("AndroidManifest.xml").writeText(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application android:label="ConsumerSmoke">
                    <activity
                        android:name=".ConsumerSmokeActivity"
                        android:exported="true" />
                </application>
            </manifest>
            """.trimIndent(),
        )

        val srcDir = mainDir.resolve("kotlin/dev/hossain/highlight/consumer")
        srcDir.mkdirs()
        srcDir.resolve("ConsumerSmokeActivity.kt").writeText(
            """
            package dev.hossain.highlight.consumer

            import android.app.Activity
            import android.os.Bundle
            import androidx.compose.foundation.ScrollState
            import androidx.compose.foundation.layout.PaddingValues
            import androidx.compose.foundation.text.KeyboardOptions
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.platform.ComposeView
            import androidx.compose.ui.text.AnnotatedString
            import androidx.compose.ui.text.SpanStyle
            import androidx.compose.ui.text.TextStyle
            import androidx.compose.ui.text.input.TextFieldValue
            import androidx.compose.ui.unit.dp
            import dev.hossain.highlight.engine.HighlightEngine
            import dev.hossain.highlight.engine.HighlightResult
            import dev.hossain.highlight.engine.HighlightTheme
            import dev.hossain.highlight.engine.HljsSelectors
            import dev.hossain.highlight.ui.CodeBlockStyle
            import dev.hossain.highlight.ui.ExperimentalHighlightApi
            import dev.hossain.highlight.ui.HighlightThemeProvider
            import dev.hossain.highlight.ui.StreamingSyntaxHighlightedCode
            import dev.hossain.highlight.ui.SyntaxHighlightedCode
            import dev.hossain.highlight.ui.SyntaxHighlightedTextEditor
            import kotlinx.coroutines.flow.StateFlow

            class ConsumerSmokeActivity : Activity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    val engine = HighlightEngine(applicationContext)
                    val initializedFlow: StateFlow<Boolean> = engine.isInitialized
                    check(!initializedFlow.value)
                    engine.close()

                    setContentView(
                        ComposeView(this).apply {
                            setContent {
                                ConsumerSmokeScreen()
                            }
                        },
                    )
                }
            }

            @OptIn(ExperimentalHighlightApi::class)
            @Composable
            fun ConsumerSmokeScreen() {
                val customTheme =
                    HighlightTheme.fromColorMap(
                        name = "consumer-theme",
                        colorMap = mapOf(HljsSelectors.KEYWORD to SpanStyle(color = Color.Blue)),
                    )
                val style =
                    CodeBlockStyle.Default.copy(
                        padding = PaddingValues(12.dp),
                        textStyle = TextStyle(color = Color.White),
                    )
                HighlightThemeProvider(
                    lightHighlightTheme = customTheme,
                    darkHighlightTheme = HighlightTheme.draculaDark(),
                ) {
                    SyntaxHighlightedCode(
                        code = "val x = 1",
                        language = "kotlin",
                        modifier = Modifier,
                        style = style,
                        scrollState = ScrollState(0),
                        onHighlightComplete = { result: HighlightResult ->
                            val annotated: AnnotatedString = result.annotated
                            check(annotated.text.isNotEmpty())
                        },
                    )
                    StreamingSyntaxHighlightedCode(
                        code = "val y = 2",
                        language = "kotlin",
                        modifier = Modifier,
                    )
                    SyntaxHighlightedTextEditor(
                        value = TextFieldValue("val z = 3"),
                        onValueChange = {},
                        language = "kotlin",
                        keyboardOptions = KeyboardOptions.Default,
                    )
                }
            }
            """.trimIndent(),
        )

        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val gradlew = rootProjectDir.file(if (isWindows) "gradlew.bat" else "gradlew").asFile.absolutePath
        val process =
            ProcessBuilder(
                gradlew,
                "--project-dir",
                consumerDir.absolutePath,
                "--no-daemon",
                "assembleDebug",
                "assembleRelease",
            )
                .directory(consumerDir)
                .redirectErrorStream(true)
                .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException(
                "Standalone consumer build failed with exit code $exitCode:\n$output",
            )
        }
    }
}
