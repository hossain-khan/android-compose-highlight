package dev.hossain.highlight.build

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.w3c.dom.Element
import java.io.File
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Gradle task that verifies publication metadata (Maven POM and Gradle Module Metadata)
 * and verifies standalone consumer compilation against the published library artifact.
 *
 * Verifications performed:
 * 1. Maven POM: Ensures ABI-exposed dependencies have `compile` scope, implementation-only
 *    dependencies have `runtime` scope, and `compose-bom` is imported in `<dependencyManagement>`.
 * 2. Gradle Module Metadata: Ensures `releaseVariantReleaseApiPublication` (`java-api`) variant
 *    exposes only compile-time API dependencies and no implementation-only dependencies.
 * 3. Standalone Consumer Build: Copies the template consumer app from [templateDir] to
 *    [consumerProjectDir], populates local repository and version coordinates, and compiles
 *    both `assembleDebug` and minified R8 `assembleRelease` variants without declaring
 *    transitive compile dependencies.
 */
@DisableCachingByDefault(because = "Executes external Gradle process on generated consumer project and validates publication outputs")
abstract class PublishedArtifactConsumerCheckTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val pomFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val metadataFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localRepoDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val templateDir: DirectoryProperty

    @get:OutputDirectory
    abstract val consumerProjectDir: DirectoryProperty

    @get:Internal
    abstract val rootProjectDirectory: DirectoryProperty

    @get:Input
    abstract val libraryVersion: Property<String>

    @get:Input
    abstract val agpVersion: Property<String>

    @get:Input
    abstract val kotlinVersion: Property<String>

    @TaskAction
    fun check() {
        validatePom(pomFile.get().asFile)
        validateGradleMetadata(metadataFile.get().asFile)
        buildStandaloneConsumerApp(
            consumerDir = consumerProjectDir.get().asFile,
            templateSourceDir = templateDir.get().asFile,
            rootProjectDir = rootProjectDirectory.get().asFile,
        )
    }

    private fun validatePom(pom: File) {
        check(pom.isFile) { "Missing generated POM file: ${pom.absolutePath}" }
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
    }

    @Suppress("UNCHECKED_CAST")
    private fun validateGradleMetadata(metadataFile: File) {
        val root =
            JsonSlurper().parse(metadataFile) as? Map<String, Any?>
                ?: throw GradleException("Failed to parse Gradle Module Metadata as JSON map: ${metadataFile.absolutePath}")

        val variants =
            root["variants"] as? List<Map<String, Any?>>
                ?: throw GradleException("Missing 'variants' array in Gradle Module Metadata: ${metadataFile.absolutePath}")

        val apiVariant =
            variants.firstOrNull { it["name"] == "releaseVariantReleaseApiPublication" }
                ?: throw GradleException("Could not find releaseVariantReleaseApiPublication in ${metadataFile.absolutePath}")

        val dependencies =
            apiVariant["dependencies"] as? List<Map<String, Any?>>
                ?: throw GradleException("Missing 'dependencies' in releaseVariantReleaseApiPublication: ${metadataFile.absolutePath}")

        val apiVariantCoords =
            dependencies.mapNotNull { dep ->
                val group = dep["group"] as? String ?: return@mapNotNull null
                val module = dep["module"] as? String ?: return@mapNotNull null
                "$group:$module"
            }.toSet()

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
                "but found $apiVariantCoords"
        }
    }

    private fun buildStandaloneConsumerApp(
        consumerDir: File,
        templateSourceDir: File,
        rootProjectDir: File,
    ) {
        consumerDir.deleteRecursively()
        consumerDir.mkdirs()

        val repoUri = localRepoDir.get().asFile.toURI().toString()
        val agp = agpVersion.get()
        val kotlin = kotlinVersion.get()
        val library = libraryVersion.get()

        templateSourceDir.walkTopDown().forEach { file ->
            if (file.isDirectory) return@forEach
            if (file.extension == "md") return@forEach

            val relativePath = file.relativeTo(templateSourceDir).path
            val targetFile = consumerDir.resolve(relativePath)
            targetFile.parentFile.mkdirs()

            when (file.name) {
                "settings.gradle.kts" -> {
                    val content = file.readText().replace("__LOCAL_REPO_URL__", repoUri)
                    targetFile.writeText(content)
                }
                "build.gradle.kts" -> {
                    val content =
                        file.readText()
                            .replace("__AGP_VERSION__", agp)
                            .replace("__KOTLIN_VERSION__", kotlin)
                            .replace("__LIBRARY_VERSION__", library)
                    targetFile.writeText(content)
                }
                else -> {
                    file.copyTo(targetFile, overwrite = true)
                }
            }
        }

        val localPropertiesFile = rootProjectDir.resolve("local.properties")
        if (localPropertiesFile.isFile) {
            val props = Properties()
            localPropertiesFile.inputStream().use { props.load(it) }
            listOf("sdk.dir", "ndk.dir", "cmake.dir").forEach { key ->
                val rawPath = props.getProperty(key) ?: return@forEach
                val resolved = rootProjectDir.resolve(rawPath).canonicalFile
                props.setProperty(key, resolved.absolutePath)
            }
            consumerDir.resolve("local.properties").outputStream().use { out ->
                props.store(out, "Normalized from root local.properties")
            }
        }

        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val gradlew = rootProjectDir.resolve(if (isWindows) "gradlew.bat" else "gradlew").absolutePath
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
        logger.lifecycle("Published artifact consumer verification passed (debug + minified release).")
    }
}
