package dev.hossain.highlight.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.hossain.highlight.engine.ThemeParser
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Microbenchmarks for [ThemeParser].
 *
 * Measures the time to parse a Highlight.js CSS theme file into a [SpanStyle] color map.
 * This runs on every cold-start of [HighlightTheme] (the colorMap is lazy, cached after
 * first access), so it represents the one-time theme initialization cost.
 *
 * Run with:
 *   ./gradlew :compose-highlight:connectedAndroidTest -P android.testInstrumentationRunnerArguments.class=dev.hossain.highlight.benchmark.ThemeParserBenchmark
 */
@RunWith(AndroidJUnit4::class)
class ThemeParserBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var tomorrowCss: String
    private lateinit var tomorrowNightCss: String

    @Before
    fun loadCss() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        tomorrowCss =
            context.assets
                .open("compose-highlight/themes/tomorrow.css")
                .bufferedReader()
                .readText()
        tomorrowNightCss =
            context.assets
                .open("compose-highlight/themes/tomorrow-night.css")
                .bufferedReader()
                .readText()
    }

    @Test
    fun parseTomorrowTheme() =
        benchmarkRule.measureRepeated {
            ThemeParser.parse(tomorrowCss)
        }

    @Test
    fun parseTomorrowNightTheme() =
        benchmarkRule.measureRepeated {
            ThemeParser.parse(tomorrowNightCss)
        }
}
