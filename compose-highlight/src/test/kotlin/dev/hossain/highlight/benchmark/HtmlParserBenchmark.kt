package dev.hossain.highlight.benchmark

import androidx.compose.ui.text.SpanStyle
import dev.hossain.highlight.engine.internal.HtmlToAnnotatedString
import dev.hossain.highlight.engine.internal.ThemeParser
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.math.sqrt
import kotlin.system.measureNanoTime

/**
 * JVM microbenchmark for [HtmlToAnnotatedString.convert] and [convertBothThemes][HtmlToAnnotatedString.convertBothThemes]
 * against the real-world hljs HTML fixtures shipped under `src/test/resources/highlight-fixtures/`.
 *
 * Captures a baseline before swapping the parser implementation. Re-run after the swap and diff the
 * JSON report to detect regressions.
 *
 * **Skipped by default** so `./gradlew :compose-highlight:test` stays fast. Enable with:
 *
 * ```
 * ./gradlew :compose-highlight:test \
 *   --tests "dev.hossain.highlight.benchmark.HtmlParserBenchmark" \
 *   -DrunBenchmark=true \
 *   --rerun-tasks
 * ```
 *
 * Report file:
 *   `compose-highlight/build/reports/benchmarks/html-parser-baseline-<epoch-ms>.json`
 *
 * The report contains, per benchmark: warmup/measurement counts, mean/stddev/min/max times in
 * microseconds, and the theme + fixture name. Diff two reports with `jq` after the parser swap.
 */
class HtmlParserBenchmark {
    @Test fun convertKotlin() = bench("convertKotlin") { HtmlToAnnotatedString.convert(kotlinHtml, lightMap) }

    @Test fun convertC() = bench("convertC") { HtmlToAnnotatedString.convert(cHtml, lightMap) }

    @Test fun convertRust() = bench("convertRust") { HtmlToAnnotatedString.convert(rustHtml, lightMap) }

    @Test fun convertGo() = bench("convertGo") { HtmlToAnnotatedString.convert(goHtml, lightMap) }

    @Test fun convertCsharp() = bench("convertCsharp") { HtmlToAnnotatedString.convert(csharpHtml, lightMap) }

    @Test fun convertSql() = bench("convertSql") { HtmlToAnnotatedString.convert(sqlHtml, lightMap) }

    @Test fun convertBothKotlin() = bench("convertBothKotlin") { HtmlToAnnotatedString.convertBothThemes(kotlinHtml, lightMap, darkMap) }

    @Test fun convertBothC() = bench("convertBothC") { HtmlToAnnotatedString.convertBothThemes(cHtml, lightMap, darkMap) }

    @Test fun convertBothRust() = bench("convertBothRust") { HtmlToAnnotatedString.convertBothThemes(rustHtml, lightMap, darkMap) }

    @Test fun convertBothGo() = bench("convertBothGo") { HtmlToAnnotatedString.convertBothThemes(goHtml, lightMap, darkMap) }

    @Test fun convertBothCsharp() = bench("convertBothCsharp") { HtmlToAnnotatedString.convertBothThemes(csharpHtml, lightMap, darkMap) }

    @Test fun convertBothSql() = bench("convertBothSql") { HtmlToAnnotatedString.convertBothThemes(sqlHtml, lightMap, darkMap) }

    private fun <T> bench(
        name: String,
        block: () -> T,
    ) {
        // Always allow this test class to be collected, but skip the body unless explicitly enabled.
        // Using assumeTrue so the test reports as "skipped" rather than failing.
        assumeTrue("Set -DrunBenchmark=true to enable", System.getProperty("runBenchmark") == "true")

        // Warmup - lets the JIT compile the hot path.
        val sink = mutableListOf<T>()
        repeat(WARMUP_ITERATIONS) { sink += block() }
        sink.clear()

        // Measurement.
        val timesNs = LongArray(MEASUREMENT_ITERATIONS)
        for (i in 0 until MEASUREMENT_ITERATIONS) {
            timesNs[i] = measureNanoTime { sink += block() }
        }
        sink.clear() // ensure live but not retained between runs

        recordResult(name, timesNs)
    }

    companion object {
        private const val WARMUP_ITERATIONS = 5
        private const val MEASUREMENT_ITERATIONS = 30

        private lateinit var kotlinHtml: String
        private lateinit var cHtml: String
        private lateinit var rustHtml: String
        private lateinit var goHtml: String
        private lateinit var csharpHtml: String
        private lateinit var sqlHtml: String

        private lateinit var lightMap: Map<String, SpanStyle>
        private lateinit var darkMap: Map<String, SpanStyle>

        private val results = mutableListOf<BenchmarkResult>()
        private val reportTimestamp = System.currentTimeMillis()

        @JvmStatic
        @BeforeClass
        fun setupAll() {
            // No-op when benchmarks are disabled - skip expensive resource loading.
            if (System.getProperty("runBenchmark") != "true") return

            kotlinHtml = readResource("highlight-fixtures/real-kotlin.html")
            cHtml = readResource("highlight-fixtures/real-c.html")
            rustHtml = readResource("highlight-fixtures/real-rust.html")
            goHtml = readResource("highlight-fixtures/real-go.html")
            csharpHtml = readResource("highlight-fixtures/real-csharp.html")
            sqlHtml = readResource("highlight-fixtures/real-sql.html")

            // ThemeParser.parse(cssText) is the JVM-friendly overload (no android.content.Context needed).
            // Theme CSS files live under src/main/assets/ and aren't on the test resource classpath, so
            // read them directly from the project tree.
            lightMap = ThemeParser.parse(readProjectFile("src/main/assets/compose-highlight/themes/atom-one-light.css"))
            darkMap = ThemeParser.parse(readProjectFile("src/main/assets/compose-highlight/themes/atom-one-dark.css"))

            // Register a JVM shutdown hook so the report is written even if the test runner kills us.
            Runtime.getRuntime().addShutdownHook(Thread { writeReport() })
        }

        private fun readResource(path: String): String =
            HtmlParserBenchmark::class.java.classLoader!!
                .getResourceAsStream(path)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("Missing resource: $path")

        private fun readProjectFile(relativePath: String): String {
            // Test JVM cwd varies (Gradle test runner sets it to the module dir). Try common roots.
            val candidates =
                listOf(
                    File(relativePath),
                    File("compose-highlight/$relativePath"),
                    File("../compose-highlight/$relativePath"),
                )
            val file =
                candidates.firstOrNull { it.exists() }
                    ?: error("Could not locate $relativePath. Tried: ${candidates.map { it.absolutePath }}")
            return file.readText()
        }

        @Synchronized
        fun recordResult(
            name: String,
            timesNs: LongArray,
        ) {
            val mean = timesNs.average()
            val variance = timesNs.map { (it - mean) * (it - mean) }.average()
            val stddev = sqrt(variance)
            val min = timesNs.min()
            val max = timesNs.max()

            results +=
                BenchmarkResult(
                    name = name,
                    warmupIterations = WARMUP_ITERATIONS,
                    measurementIterations = timesNs.size,
                    meanUs = mean / 1_000.0,
                    stddevUs = stddev / 1_000.0,
                    minUs = min / 1_000.0,
                    maxUs = max / 1_000.0,
                )

            // Print a one-line summary so progress is visible during a long run.
            println(
                "[bench] %-22s mean=%9.2f us  stddev=%8.2f us  min=%9.2f  max=%9.2f"
                    .format(name, mean / 1_000.0, stddev / 1_000.0, min / 1_000.0, max / 1_000.0),
            )

            writeReport() // rewrite after each test so a partial run still produces a report.
        }

        private fun writeReport() {
            if (results.isEmpty()) return

            // Gradle's Test task runs with cwd = the module directory, so build/ resolves
            // to compose-highlight/build/. Print absolute path so the user can find it.
            val outDir = File("build/reports/benchmarks").also { it.mkdirs() }
            val file = File(outDir, "html-parser-baseline-$reportTimestamp.json")
            val sb = StringBuilder()
            sb.append("{\n")
            sb.append("  \"reportTimestamp\": ").append(reportTimestamp).append(",\n")
            sb.append("  \"timeUnit\": \"microseconds\",\n")
            sb.append("  \"warmupIterations\": ").append(WARMUP_ITERATIONS).append(",\n")
            sb.append("  \"measurementIterations\": ").append(MEASUREMENT_ITERATIONS).append(",\n")
            sb.append("  \"benchmarks\": [\n")
            results.forEachIndexed { i, r ->
                sb.append("    {")
                sb.append("\"name\":\"").append(r.name).append("\",")
                sb.append("\"warmupIterations\":").append(r.warmupIterations).append(",")
                sb.append("\"measurementIterations\":").append(r.measurementIterations).append(",")
                sb.append("\"meanUs\":").append("%.3f".format(r.meanUs)).append(",")
                sb.append("\"stddevUs\":").append("%.3f".format(r.stddevUs)).append(",")
                sb.append("\"minUs\":").append("%.3f".format(r.minUs)).append(",")
                sb.append("\"maxUs\":").append("%.3f".format(r.maxUs))
                sb.append("}")
                if (i < results.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("  ]\n}\n")
            file.writeText(sb.toString())
            println("[bench] Report written to ${file.absolutePath}")
        }
    }

    private data class BenchmarkResult(
        val name: String,
        val warmupIterations: Int,
        val measurementIterations: Int,
        val meanUs: Double,
        val stddevUs: Double,
        val minUs: Double,
        val maxUs: Double,
    )
}
