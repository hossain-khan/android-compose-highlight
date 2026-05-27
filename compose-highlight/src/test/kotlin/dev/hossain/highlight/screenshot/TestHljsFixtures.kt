package dev.hossain.highlight.screenshot

/**
 * Hand-built HTML token fixtures matching what `highlight.min.js` would emit for the snippets
 * in [TestSnippets]. Used by the screenshot test helper to short-circuit the WebView round-trip
 * deterministically inside a JVM/Robolectric environment.
 *
 * ## Why hand-built and not real engine round-trips?
 *
 * The screenshot suite isolates two questions: "is the visual rendering of `AnnotatedString`
 * stable across themes and layout variants?" The orthogonal question - "does `highlight.js`
 * still tokenize Kotlin/Python/JSON correctly?" - is covered by the managed-device instrumented
 * tests in `src/androidTest/`. Driving the real WebView from a JVM screenshot test would couple
 * those two concerns and add flakiness without coverage benefit.
 *
 * ## Maintenance
 *
 * If `highlight.min.js` is upgraded and any of the four bundled themes' built-in token classes
 * change (this is rare; hljs class names like `hljs-keyword`, `hljs-string`, `hljs-comment` have
 * been stable across major versions), refresh these fixtures by:
 *
 * 1. Running the sample app with the corresponding snippet.
 * 2. Capturing the highlighted HTML via `engine.highlightToHtml(...)`.
 * 3. Pasting the result here.
 *
 * Token classes used below match `highlight.js` v11 output for the kotlin / python / json
 * languages. They were verified against the live engine output at the time of authoring.
 */
internal object TestHljsFixtures {
    val KOTLIN_SAMPLE_HTML =
        """<span class="hljs-comment">// Greets the caller by name.</span>
<span class="hljs-keyword">fun</span> <span class="hljs-title function_">greet</span>(<span class="hljs-params">name: <span class="hljs-built_in">String</span></span>) {
    <span class="hljs-keyword">val</span> message = <span class="hljs-string">&quot;Hello, <span class="hljs-subst">${'$'}name</span>!&quot;</span>
    <span class="hljs-built_in">println</span>(message)
}
<span class="hljs-built_in">greet</span>(<span class="hljs-string">&quot;World&quot;</span>)"""

    val PYTHON_SAMPLE_HTML =
        """<span class="hljs-comment"># Compute factorial recursively.</span>
<span class="hljs-keyword">def</span> <span class="hljs-title function_">factorial</span>(<span class="hljs-params">n: <span class="hljs-built_in">int</span></span>) -&gt; <span class="hljs-built_in">int</span>:
    <span class="hljs-keyword">if</span> n &lt;= <span class="hljs-number">1</span>:
        <span class="hljs-keyword">return</span> <span class="hljs-number">1</span>
    <span class="hljs-keyword">return</span> n * factorial(n - <span class="hljs-number">1</span>)

<span class="hljs-built_in">print</span>(factorial(<span class="hljs-number">5</span>))  <span class="hljs-comment"># 120</span>"""

    val JSON_SAMPLE_HTML =
        """{
  <span class="hljs-attr">&quot;name&quot;</span>: <span class="hljs-string">&quot;compose-highlight&quot;</span>,
  <span class="hljs-attr">&quot;version&quot;</span>: <span class="hljs-string">&quot;0.5.0&quot;</span>,
  <span class="hljs-attr">&quot;tags&quot;</span>: [<span class="hljs-string">&quot;kotlin&quot;</span>, <span class="hljs-string">&quot;compose&quot;</span>, <span class="hljs-string">&quot;syntax&quot;</span>],
  <span class="hljs-attr">&quot;stable&quot;</span>: <span class="hljs-literal">true</span>,
  <span class="hljs-attr">&quot;minSdk&quot;</span>: <span class="hljs-number">24</span>
}"""
}
