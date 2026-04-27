package dev.composehighlight.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.text.SpanStyle
import dev.composehighlight.engine.HtmlToAnnotatedString
import dev.composehighlight.engine.ThemeParser
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Microbenchmarks for [HtmlToAnnotatedString].
 *
 * Measures the time to convert Highlight.js HTML output (with `<span class="hljs-*">` tokens)
 * into a Compose [AnnotatedString] using a pre-built color map.
 *
 * This is called on every highlight request after the JS step, so it's on the hot path.
 *
 * Run with:
 *   ./gradlew :compose-highlight:connectedAndroidTest -P android.testInstrumentationRunnerArguments.class=dev.composehighlight.benchmark.HtmlToAnnotatedStringBenchmark
 */
@RunWith(AndroidJUnit4::class)
class HtmlToAnnotatedStringBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var colorMap: Map<String, SpanStyle>

    // Realistic hljs HTML output for a Python function (from actual device log)
    private val pythonHtml = """
        <span class="hljs-keyword">def</span> <span class="hljs-title function_">fibonacci</span>(<span class="hljs-params">n: <span class="hljs-built_in">int</span></span>) -&gt; <span class="hljs-built_in">int</span>:
            <span class="hljs-comment"># Returns the nth Fibonacci number</span>
            <span class="hljs-keyword">if</span> n &lt;= <span class="hljs-number">1</span>:
                <span class="hljs-keyword">return</span> n
            a, b = <span class="hljs-number">0</span>, <span class="hljs-number">1</span>
            <span class="hljs-keyword">for</span> _ <span class="hljs-keyword">in</span> <span class="hljs-built_in">range</span>(n - <span class="hljs-number">1</span>):
                a, b = b, a + b
            <span class="hljs-keyword">return</span> b
    """.trimIndent()

    // Realistic hljs HTML output for Kotlin code
    private val kotlinHtml = """
        <span class="hljs-keyword">data</span> <span class="hljs-keyword">class</span> <span class="hljs-title class_">User</span>(<span class="hljs-keyword">val</span> name: String, <span class="hljs-keyword">val</span> age: <span class="hljs-built_in">Int</span>)

        <span class="hljs-function"><span class="hljs-keyword">fun</span> List&lt;User&gt;.<span class="hljs-title">filterAdults</span><span class="hljs-params">(minAge: <span class="hljs-type">Int</span>)</span></span>: List&lt;User&gt; =
            <span class="hljs-built_in">filter</span> { it.age &gt;= minAge }

        <span class="hljs-keyword">val</span> users = <span class="hljs-built_in">listOf</span>(
            <span class="hljs-title class_">User</span>(<span class="hljs-string">"Alice"</span>, <span class="hljs-number">30</span>),
            <span class="hljs-title class_">User</span>(<span class="hljs-string">"Bob"</span>, <span class="hljs-number">25</span>),
        )
        <span class="hljs-keyword">val</span> adults = users.filterAdults(<span class="hljs-number">18</span>)
        <span class="hljs-built_in">println</span>(adults)
    """.trimIndent()

    // Larger code block — SQL with many token classes
    private val sqlHtml = """
        <span class="hljs-keyword">SELECT</span>
            u.id,
            u.name,
            <span class="hljs-built_in">COUNT</span>(o.id) <span class="hljs-keyword">AS</span> order_count,
            <span class="hljs-built_in">SUM</span>(o.total) <span class="hljs-keyword">AS</span> revenue
        <span class="hljs-keyword">FROM</span> users u
        <span class="hljs-keyword">LEFT</span> <span class="hljs-keyword">JOIN</span> orders o <span class="hljs-keyword">ON</span> o.user_id = u.id
        <span class="hljs-keyword">WHERE</span> u.created_at &gt;= <span class="hljs-string">'2024-01-01'</span>
        <span class="hljs-keyword">GROUP</span> <span class="hljs-keyword">BY</span> u.id, u.name
        <span class="hljs-keyword">HAVING</span> order_count &gt; <span class="hljs-number">0</span>
        <span class="hljs-keyword">ORDER</span> <span class="hljs-keyword">BY</span> revenue <span class="hljs-keyword">DESC</span>
        <span class="hljs-keyword">LIMIT</span> <span class="hljs-number">10</span>;
    """.trimIndent()

    @Before
    fun loadTheme() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        colorMap = ThemeParser.parse(context, "compose-highlight/themes/tomorrow.css")
    }

    @Test
    fun convertPythonHtml() = benchmarkRule.measureRepeated {
        HtmlToAnnotatedString.convert(pythonHtml, colorMap)
    }

    @Test
    fun convertKotlinHtml() = benchmarkRule.measureRepeated {
        HtmlToAnnotatedString.convert(kotlinHtml, colorMap)
    }

    @Test
    fun convertSqlHtml() = benchmarkRule.measureRepeated {
        HtmlToAnnotatedString.convert(sqlHtml, colorMap)
    }
}
