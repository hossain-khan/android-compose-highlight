package dev.composehighlight.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.composehighlight.engine.HighlightEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Microbenchmarks for the full [HighlightEngine] pipeline.
 *
 * Measures the end-to-end cost of a single highlight call: JS execution in the hidden
 * WebView, JSON-unescaping the result, and returning the raw HTML string.
 *
 * The WebView is initialized once in [@Before] to avoid counting cold-start overhead —
 * use [HighlightEngineWarmupBenchmark] for measuring first-call latency if needed.
 *
 * Note: these benchmarks include async IPC overhead (instrumentation thread → Main thread
 * → WebView → back), so individual iterations are slower than [ThemeParserBenchmark] and
 * [HtmlToAnnotatedStringBenchmark]. The value is tracking regressions in JS execution speed.
 *
 * Run with:
 *   ./gradlew :compose-highlight:connectedAndroidTest -P android.testInstrumentationRunnerArguments.class=dev.composehighlight.benchmark.HighlightEngineBenchmark
 */
@RunWith(AndroidJUnit4::class)
class HighlightEngineBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var engine: HighlightEngine

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        engine = HighlightEngine(context)
        // Pre-warm WebView so benchmark iterations measure steady-state performance
        runBlocking { engine.initialize() }
    }

    @After
    fun tearDown() {
        engine.destroy()
    }

    @Test
    fun highlightPythonToHtml() = benchmarkRule.measureRepeated {
        runBlocking {
            engine.highlightToHtml(PYTHON_CODE, "python").getOrThrow()
        }
    }

    @Test
    fun highlightKotlinToHtml() = benchmarkRule.measureRepeated {
        runBlocking {
            engine.highlightToHtml(KOTLIN_CODE, "kotlin").getOrThrow()
        }
    }

    @Test
    fun highlightSqlToHtml() = benchmarkRule.measureRepeated {
        runBlocking {
            engine.highlightToHtml(SQL_CODE, "sql").getOrThrow()
        }
    }

    companion object {
        private val PYTHON_CODE = """
            def fibonacci(n: int) -> int:
                # Returns the nth Fibonacci number
                if n <= 1:
                    return n
                a, b = 0, 1
                for _ in range(n - 1):
                    a, b = b, a + b
                return b

            result = fibonacci(10)
            print(f"Result: {result}")
        """.trimIndent()

        private val KOTLIN_CODE = """
            data class User(val name: String, val age: Int)

            fun List<User>.filterAdults(minAge: Int): List<User> =
                filter { it.age >= minAge }

            val users = listOf(
                User("Alice", 30),
                User("Bob", 25),
            )
            val adults = users.filterAdults(18)
            println(adults)
        """.trimIndent()

        private val SQL_CODE = """
            SELECT
                u.id,
                u.name,
                COUNT(o.id) AS order_count,
                SUM(o.total) AS revenue
            FROM users u
            LEFT JOIN orders o ON o.user_id = u.id
            WHERE u.created_at >= '2024-01-01'
            GROUP BY u.id, u.name
            HAVING order_count > 0
            ORDER BY revenue DESC
            LIMIT 10;
        """.trimIndent()
    }
}
