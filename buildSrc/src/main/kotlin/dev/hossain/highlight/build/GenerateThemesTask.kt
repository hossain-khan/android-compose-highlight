package dev.hossain.highlight.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task that reads the bundled hljs theme CSS files and generates a Kotlin
 * source file containing precompiled `Map<String, SpanStyle>` constants.
 *
 * Inputs: the CSS files (declared with [PathSensitivity.RELATIVE] so up-to-date
 * checks survive moves of the project root).
 *
 * Output: a single `GeneratedThemes.kt` under [outputDir].
 *
 * The task is cacheable because input → output is deterministic.
 */
@CacheableTask
abstract class GenerateThemesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val cssDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val themes = THEME_INPUTS.map { (constantName, fileName) ->
            val cssFile = cssDir.get().file(fileName).asFile
            check(cssFile.exists()) { "Bundled theme CSS missing: ${cssFile.absolutePath}" }
            val cssText = cssFile.readText(Charsets.UTF_8)
            val entries = CssThemeParser.parse(cssText)
            check(entries.isNotEmpty()) { "Parser produced no entries for $fileName - runtime would treat this theme as missing." }
            val assetPath = "compose-highlight/themes/$fileName"
            ThemeBuildInput(
                constantName = constantName,
                assetPath = assetPath,
                contentIdentity = CssThemeParser.assetContentIdentity(assetPath),
                entries = entries,
            )
        }

        val source = ThemeSourceEmitter.emit(themes)
        val pkgDir = outputDir.get().asFile.resolve("dev/hossain/highlight/engine")
        pkgDir.mkdirs()
        val outFile = pkgDir.resolve("GeneratedThemes.kt")
        outFile.writeText(source, Charsets.UTF_8)
        logger.lifecycle("Generated ${outFile.relativeTo(project.rootDir)} (${themes.size} themes)")
    }

    private companion object {
        // Ordered to match the order in HighlightTheme.kt for diff readability.
        // First = Kotlin constant name in GeneratedThemes, second = file name in cssDir.
        val THEME_INPUTS = listOf(
            "TOMORROW" to "tomorrow.css",
            "TOMORROW_NIGHT" to "tomorrow-night.css",
            "ATOM_ONE_DARK" to "atom-one-dark.css",
            "ATOM_ONE_LIGHT" to "atom-one-light.css",
            "GITHUB" to "github.css",
            "GITHUB_DARK" to "github-dark.css",
            "DRACULA" to "dracula.css",
            "ALUCARD" to "alucard.css",
        )
    }
}
