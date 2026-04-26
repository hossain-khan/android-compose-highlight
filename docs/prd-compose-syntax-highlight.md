# PRD: compose-highlight

> A Kotlin Multiplatform-ready Android library that provides real syntax highlighting for Jetpack Compose using Highlight.js, the same engine used by Claude, Perplexity, and ChatGPT in production.

---

## 1. Problem Statement

Android apps that display source code (AI assistants, documentation browsers, code editors, developer tools) need syntax highlighting. The current options are:

- **WebView-only rendering**: Easy to set up but loses native text selection, accessibility, theming integration, and Compose interop.
- **Regex-based tokenizers**: Fragile, limited language support, and impossible to maintain at scale.
- **Custom parsers per language**: Enormous engineering effort, never reaches parity with established libraries.

Production apps (Claude, Perplexity, ChatGPT) have converged on a hidden-WebView pattern: run Highlight.js off-screen for tokenization, then convert the output to native spans. But each app re-implements this pipeline from scratch with significant engineering effort.

**compose-highlight** extracts this proven pattern into a reusable library with a single-line Compose API.

---

## 2. Background: Production Evidence

This library design is based on reverse-engineering five production Android APKs. Key findings:

| App | Engine | Version | Languages | Rendering |
|-----|--------|---------|-----------|-----------|
| ChatGPT | Highlight.js | v11.9.0 | 41 | WebView (stays in WebView) |
| Claude | Highlight.js | v11.10.0 | 192 | Native Compose spans |
| Perplexity | Highlight.js | v11.11.1 | 192 | Native Compose spans |
| GitHub | Server-side | N/A | All | Native custom StyleSpans |
| Gemini | None (shell) | N/A | N/A | N/A |

**Key architectural insights from production apps:**

1. **Perplexity** produces both light AND dark AnnotatedStrings from a single JS call — the HTML is parsed twice with different color maps. Theme switching is instant.
2. **Perplexity** parses CSS theme files at runtime into a `LinkedHashMap<String, SpanStyle>` — no hardcoded color values. Any hljs theme CSS works.
3. **Claude** uses Base64 `data:` URLs to load assets into the WebView. Perplexity uses `WebViewAssetLoader` with the `appassets.androidplatform.net` scheme (cleaner, recommended by Google).
4. **Both Claude and Perplexity** cache the WebView singleton and reuse it across all highlighting calls.
5. **Perplexity** uses jsoup to parse Highlight.js HTML output and recursively walks the DOM tree, pushing/popping Compose `SpanStyle` for each `<span class="hljs-*">` element.
6. **Perplexity** wraps failures in a dedicated `MarkdownException$HighlightJsFailed` exception class and falls back to unstyled code.

---

## 3. Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    compose-highlight                     │
│                                                          │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐ │
│  │  Asset Bundle │   │   Engine     │   │  Compose UI  │ │
│  │              │   │              │   │              │ │
│  │ highlight.js │   │ WebView mgr  │   │ SyntaxText() │ │
│  │ bridge.html  │   │ JS bridge    │   │ CodeBlock()  │ │
│  │ themes/*.css │   │ ThemeParser  │   │ Line numbers │ │
│  │              │   │ HtmlToSpans  │   │ Copy button  │ │
│  └──────────────┘   └──────────────┘   └──────────────┘ │
│                                                          │
│  Public API:                                             │
│  ─────────────────────────────────────────────────       │
│  SyntaxHighlightedCode(code, language, theme)            │
│  rememberHighlightEngine()                               │
│  HighlightEngine.highlight(code, lang) → AnnotatedString │
└─────────────────────────────────────────────────────────┘
```

### Data Flow

```
Code ("def foo():") + lang ("python")
        │
        ▼
┌─ HighlightEngine ──────────────────────────────┐
│                                                  │
│  1. Ensure WebView is ready (create if needed)   │
│  2. Escape code string (', \n, \r)               │
│  3. Build JS: highlightCode('escaped', 'python') │
│  4. evaluateJavascript() → HTML string           │
│  5. Unescape response (<→<, \"→", \n→LF)   │
│                                                  │
└──────────────────┬───────────────────────────────┘
                   │ HTML: <span class="hljs-keyword">def</span> ...
                   ▼
┌─ ThemeParser ────────────────────────────────────┐
│                                                  │
│  Read CSS file → parse rules → extract colors    │
│  Build Map<String, SpanStyle>                    │
│  "hljs-keyword" → SpanStyle(color = #8959a8)    │
│  "hljs-string"  → SpanStyle(color = #718c00)    │
│  Cached via Lazy (parsed once per theme)         │
│                                                  │
└──────────────────┬───────────────────────────────┘
                   │
                   ▼
┌─ HtmlToAnnotatedString ─────────────────────────┐
│                                                  │
│  Jsoup.parse(html)                               │
│  Recursive tree walk:                            │
│    <span class="hljs-keyword">                  │
│      → pushStyle(map["hljs-keyword"])            │
│      → recurse children                          │
│      → pop()                                     │
│    TextNode                                      │
│      → append(text)                              │
│                                                  │
│  Output: AnnotatedString (light + dark)          │
│                                                  │
└──────────────────┬───────────────────────────────┘
                   │
                   ▼
┌─ Compose UI ────────────────────────────────────┐
│                                                  │
│  Text(annotatedString, fontFamily = Monospace)   │
│  + SelectionContainer for native text selection  │
│  + Optional line numbers                         │
│  + Optional copy-to-clipboard button             │
│  + Language label badge                          │
│  + Horizontal scroll for long lines              │
│                                                  │
└──────────────────────────────────────────────────┘
```

---

## 4. Module Specifications

### 4.1 Asset Bundle

**Files to include in `src/main/assets/compose-highlight/`:**

| File | Description | Size |
|------|-------------|------|
| `highlight.min.js` | Highlight.js v11.11.1, full 192-language bundle | ~1.04 MB |
| `bridge.html` | HTML page with `highlightCode()` JS function | ~300 bytes |
| `themes/tomorrow.css` | Base16 Tomorrow (light theme) | ~3 KB |
| `themes/tomorrow-night.css` | Base16 Tomorrow Night (dark theme) | ~3 KB |

**bridge.html** (exact implementation — proven in Perplexity production):

```html
<!DOCTYPE html>
<html><body>
<pre><code id="code"></code></pre>
<script src="highlight.min.js"></script>
<script>
function highlightCode(code, lang) {
  const block = document.getElementById("code");
  block.textContent = code;
  block.className = lang;
  delete block.dataset.highlighted;
  hljs.highlightElement(block);
  return block.innerHTML;
}
</script>
</body></html>
```

**Theme CSS files**: Use the standard Highlight.js Base16 theme format. The library's CSS parser works with any hljs theme CSS file — users can drop in custom themes.

**Downloading highlight.min.js**: Download from https://highlightjs.org/ — select "all 192 languages" when building the bundle, or use the CDN full bundle.

---

### 4.2 HighlightEngine

The core class that manages the hidden WebView and executes highlighting.

```kotlin
package dev.composehighlight.engine

class HighlightEngine internal constructor(
    private val context: Context,
) {
    private var webView: WebView? = null
    private val readyDeferred = CompletableDeferred<WebView>()
    private val mutex = Mutex()

    // Initialize WebView on Main thread, load bridge.html
    // Must be called before highlight()
    // Returns immediately if already initialized
    suspend fun initialize()

    // Highlight code and return raw HTML with <span class="hljs-*"> tokens
    // Runs JS in the cached WebView via evaluateJavascript()
    // Thread-safe: uses Mutex to serialize concurrent calls
    suspend fun highlightToHtml(code: String, language: String): Result<String>

    // Full pipeline: highlight + parse + apply theme → AnnotatedString
    // Convenience method that combines highlightToHtml + ThemeParser + HtmlToAnnotatedString
    suspend fun highlight(
        code: String,
        language: String,
        theme: HighlightTheme,
    ): Result<AnnotatedString>

    // Produce both light and dark results from a single JS call (Perplexity pattern)
    suspend fun highlightBothThemes(
        code: String,
        language: String,
        lightTheme: HighlightTheme,
        darkTheme: HighlightTheme,
    ): Result<ThemedHighlightResult>

    // Release WebView resources
    fun destroy()
}

data class ThemedHighlightResult(
    val light: AnnotatedString,
    val dark: AnnotatedString,
)
```

**WebView initialization sequence** (matches Perplexity's `ra/d` class):

1. Create `WebView(context)` on Main thread
2. Enable JavaScript: `settings.javaScriptEnabled = true`
3. Create `WebViewAssetLoader` with `AssetsPathHandler` mapped to `compose-highlight/`
4. Set a custom `WebViewClient` that:
   - Overrides `shouldInterceptRequest()` to serve local assets via the loader
   - Overrides `onPageFinished()` to detect when `bridge.html` has loaded and complete the `readyDeferred`
5. Load `https://appassets.androidplatform.net/assets/compose-highlight/bridge.html`
6. Cache the WebView in the instance field for reuse

**JavaScript execution** (matches Perplexity's `ra/c` class):

1. Escape the code string: `'` → `\'`, `\n` → `\\n`, `\r` → `\\r`
2. Build the JS call: `(function() { return highlightCode('ESCAPED_CODE', 'LANG'); })()`
3. Call `webView.evaluateJavascript(js, callback)`
4. The callback receives a JSON-encoded string — unescape: `<` → `<`, `\"` → `"`, `\\n` → `\n`
5. On failure, return `Result.failure()` with a descriptive exception

**Thread safety:**
- WebView MUST be created and accessed on the Main thread
- Use `withContext(Dispatchers.Main)` for all WebView operations
- Use a `Mutex` to serialize concurrent `evaluateJavascript()` calls (only one can run at a time in a WebView)
- `highlightToHtml()` is safe to call from any coroutine dispatcher

---

### 4.3 ThemeParser

Parses Highlight.js CSS theme files into a color map at runtime (matches Perplexity's `gn/d.c()` method).

```kotlin
package dev.composehighlight.engine

object ThemeParser {
    // Parse a CSS theme file from assets into a map of hljs class → SpanStyle
    // Results should be cached (Lazy) — only parse once per theme
    fun parse(context: Context, cssAssetPath: String): Map<String, SpanStyle>

    // Parse CSS text directly (for themes provided as strings)
    fun parse(cssText: String): Map<String, SpanStyle>
}
```

**Parsing algorithm:**

1. Read the CSS file from assets as a UTF-8 string
2. For each CSS rule block:
   - Extract selectors (e.g., `.hljs-keyword`, `.hljs-string`)
   - Extract `color:` property value (hex color)
   - Also extract `font-weight:` and `font-style:` if present
3. Build a `SpanStyle` for each selector:
   - `color` → `SpanStyle(color = Color(parsedHex))`
   - `font-weight: bold` → `SpanStyle(fontWeight = FontWeight.Bold)`
   - `font-style: italic` → `SpanStyle(fontStyle = FontStyle.Italic)`
4. Store in map keyed by the class name (e.g., `"hljs-keyword"`)
5. Handle compound selectors (`.hljs-title.function_`) — store with the most specific class

**Implementation detail**: Perplexity uses a full CSS parser library (`ti0/a` in obfuscated form). For this library, a regex-based parser is sufficient since hljs theme CSS files follow a strict, predictable format. However, consider using a lightweight CSS parser like `css4j` if robustness is a concern.

**Supported CSS properties to extract:**
- `color` (required)
- `font-weight` (bold keywords)
- `font-style` (italic emphasis)
- `background` (some themes set background on `.hljs`)

---

### 4.4 HtmlToAnnotatedString

Converts Highlight.js HTML output into Compose `AnnotatedString` (matches Perplexity's `gn/d.b()` method).

```kotlin
package dev.composehighlight.engine

object HtmlToAnnotatedString {
    // Convert highlighted HTML to AnnotatedString using the given color map
    fun convert(html: String, colorMap: Map<String, SpanStyle>): AnnotatedString
}
```

**Algorithm** (recursive tree walk — exactly what Perplexity does):

```
function walkNode(node, colorMap, builder):
    if node is Element:
        if node.tagName == "span":
            style = colorMap[node.className]
            if style != null:
                builder.pushStyle(style)
        for child in node.childNodes:
            walkNode(child, colorMap, builder)
        if style was pushed:
            builder.pop()
    else if node is TextNode:
        builder.append(node.wholeText)
```

**Key details:**
- Use jsoup (`org.jsoup:jsoup`) for HTML parsing — proven in both Claude and Perplexity
- Handle nested spans (e.g., `<span class="hljs-string"><span class="hljs-subst">...</span></span>`)
- TextNode text must be appended verbatim including whitespace
- The input is a fragment (not a full document) — use `Jsoup.parseBodyFragment(html)`

---

### 4.5 HighlightTheme

Represents a syntax highlighting theme with lazy color map initialization.

```kotlin
package dev.composehighlight.engine

class HighlightTheme private constructor(
    val name: String,
    private val colorMapProvider: () -> Map<String, SpanStyle>,
) {
    // Lazy-initialized color map (parsed once, cached forever)
    val colorMap: Map<String, SpanStyle> by lazy { colorMapProvider() }

    // Background color extracted from .hljs rule
    val backgroundColor: Color by lazy {
        // Parse from CSS .hljs { background: #xxx }
    }

    // Default text color extracted from .hljs rule
    val defaultTextColor: Color by lazy {
        // Parse from CSS .hljs { color: #xxx }
    }

    companion object {
        // Built-in themes
        fun tomorrow(context: Context): HighlightTheme
        fun tomorrowNight(context: Context): HighlightTheme

        // Custom theme from an asset CSS file path
        fun fromAsset(context: Context, assetPath: String, name: String): HighlightTheme

        // Custom theme from raw CSS text
        fun fromCss(cssText: String, name: String): HighlightTheme
    }
}
```

---

### 4.6 Compose UI Components

#### 4.6.1 SyntaxHighlightedCode (primary composable)

```kotlin
package dev.composehighlight.ui

@Composable
fun SyntaxHighlightedCode(
    code: String,
    language: String,
    modifier: Modifier = Modifier,
    theme: HighlightTheme = LocalHighlightTheme.current,
    style: CodeBlockStyle = CodeBlockStyle.Default,
    showLineNumbers: Boolean = false,
    showLanguageLabel: Boolean = true,
    showCopyButton: Boolean = true,
    onCopyClick: ((String) -> Unit)? = null,  // null = use default clipboard
    fontFamily: FontFamily = FontFamily.Monospace,
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 20.sp,
)
```

**Component structure:**

```
┌─ Surface (rounded corners, theme background) ───────────────┐
│ ┌─ Header Row ────────────────────────────────────────────┐ │
│ │  [language badge]                         [copy button] │ │
│ │  "python"                                     📋        │ │
│ └─────────────────────────────────────────────────────────┘ │
│ ┌─ HorizontalScroll ─────────────────────────────────────┐ │
│ │  ┌─ Row ─────────────────────────────────────────────┐ │ │
│ │  │ Line#│  Code content                              │ │ │
│ │  │  1   │  def foo():                                │ │ │
│ │  │  2   │      return "hello"                        │ │ │
│ │  │  3   │                                            │ │ │
│ │  └──────┴────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

**Behavior:**
- Shows unstyled monospace code immediately while highlighting runs async
- Replaces with styled AnnotatedString when highlighting completes
- No visible flicker — use `Crossfade` or `AnimatedContent` for smooth transition
- Horizontal scrolling for lines that exceed the container width
- Native text selection via `SelectionContainer`
- Copy button copies raw code (not styled), shows a brief "Copied!" confirmation
- Language label shows the language name in a small badge

#### 4.6.2 CodeBlockStyle

```kotlin
data class CodeBlockStyle(
    val shape: Shape = RoundedCornerShape(8.dp),
    val padding: PaddingValues = PaddingValues(16.dp),
    val headerPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    val lineNumberColor: Color = Color.Unspecified,  // Unspecified = derive from theme
    val lineNumberWidth: Dp = 32.dp,
    val copyButtonSize: Dp = 32.dp,
) {
    companion object {
        val Default = CodeBlockStyle()
        val Compact = CodeBlockStyle(
            padding = PaddingValues(12.dp),
            headerPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
```

#### 4.6.3 Theme Provider

```kotlin
// CompositionLocal for providing theme to all SyntaxHighlightedCode instances
val LocalHighlightTheme = staticCompositionLocalOf<HighlightTheme> {
    error("No HighlightTheme provided. Wrap your content in HighlightThemeProvider.")
}

@Composable
fun HighlightThemeProvider(
    darkTheme: Boolean = isSystemInDarkTheme(),
    lightTheme: HighlightTheme = HighlightTheme.tomorrow(LocalContext.current),
    darkTheme: HighlightTheme = HighlightTheme.tomorrowNight(LocalContext.current),
    content: @Composable () -> Unit,
)
```

#### 4.6.4 Remember helpers

```kotlin
// Create and remember a HighlightEngine scoped to the composition
@Composable
fun rememberHighlightEngine(): HighlightEngine

// Pre-highlight code and remember the result
@Composable
fun rememberHighlightedCode(
    code: String,
    language: String,
    theme: HighlightTheme = LocalHighlightTheme.current,
): State<AnnotatedString?>
```

---

## 5. Public API Summary

### Minimal usage (1 line):

```kotlin
// In your theme setup (once, at app root):
HighlightThemeProvider {
    // Anywhere in your Compose tree:
    SyntaxHighlightedCode(
        code = "def hello():\n    print('world')",
        language = "python",
    )
}
```

### Engine-only usage (for custom UI):

```kotlin
val engine = rememberHighlightEngine()
val theme = HighlightTheme.tomorrow(LocalContext.current)

LaunchedEffect(code) {
    val result = engine.highlight(code, "kotlin", theme)
    result.onSuccess { annotatedString ->
        // Use annotatedString in your own Text() composable
    }
}
```

### Custom theme:

```kotlin
val customTheme = HighlightTheme.fromAsset(
    context = context,
    assetPath = "my-custom-theme.css",
    name = "My Theme",
)
```

---

## 6. Project Structure

```
compose-highlight/
├── compose-highlight/                    # Library module
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   └── compose-highlight/
│   │   │   │       ├── bridge.html
│   │   │   │       ├── highlight.min.js
│   │   │   │       └── themes/
│   │   │   │           ├── tomorrow.css
│   │   │   │           └── tomorrow-night.css
│   │   │   ├── kotlin/
│   │   │   │   └── dev/composehighlight/
│   │   │   │       ├── engine/
│   │   │   │       │   ├── HighlightEngine.kt
│   │   │   │       │   ├── HighlightTheme.kt
│   │   │   │       │   ├── ThemeParser.kt
│   │   │   │       │   ├── HtmlToAnnotatedString.kt
│   │   │   │       │   └── WebViewManager.kt
│   │   │   │       └── ui/
│   │   │   │           ├── SyntaxHighlightedCode.kt
│   │   │   │           ├── CodeBlockStyle.kt
│   │   │   │           ├── HighlightThemeProvider.kt
│   │   │   │           └── RememberHelpers.kt
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                         # Unit tests
│   │   │   └── kotlin/dev/composehighlight/
│   │   │       ├── engine/
│   │   │       │   ├── ThemeParserTest.kt
│   │   │       │   └── HtmlToAnnotatedStringTest.kt
│   │   │       └── ui/
│   │   └── androidTest/                  # Instrumented tests
│   │       └── kotlin/dev/composehighlight/
│   │           ├── engine/
│   │           │   └── HighlightEngineTest.kt
│   │           └── ui/
│   │               └── SyntaxHighlightedCodeTest.kt
│   └── consumer-rules.pro
├── sample/                               # Demo app module
│   ├── build.gradle.kts
│   └── src/main/kotlin/dev/composehighlight/sample/
│       ├── MainActivity.kt
│       └── SampleScreen.kt
├── build.gradle.kts                      # Root build file
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 7. Dependencies

```kotlin
// compose-highlight/build.gradle.kts
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")

    // WebView asset loading (Perplexity's approach)
    implementation("androidx.webkit:webkit:1.11.0")

    // HTML parsing (used by both Claude and Perplexity)
    implementation("org.jsoup:jsoup:1.18.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

**compileSdk**: 35  
**minSdk**: 24  
**targetSdk**: 35  
**Kotlin**: 2.0+  
**AGP**: 8.5+

---

## 8. Threading Model

```
Caller coroutine (any dispatcher)
    │
    ├─ mutex.withLock {                    // Serialize highlight calls
    │      │
    │      ├─ withContext(Dispatchers.Main) {   // WebView requires Main
    │      │      webView.evaluateJavascript(js, callback)
    │      │  }
    │      │
    │      ├─ suspendCancellableCoroutine { }  // Bridge callback → coroutine
    │      │
    │      └─ // Back to caller's dispatcher
    │
    ├─ ThemeParser.parse()                 // Pure computation, any thread
    │
    ├─ HtmlToAnnotatedString.convert()     // Pure computation, any thread
    │
    └─ Return AnnotatedString
```

**Critical rule**: `WebView` MUST be created and accessed on the Android Main thread. All `evaluateJavascript()` calls must use `withContext(Dispatchers.Main)`.

**Concurrency**: `evaluateJavascript()` is asynchronous but only one call can execute at a time in a single WebView. Use a `Mutex` to serialize concurrent highlight requests. Do NOT create multiple WebViews — they are expensive (~2-4 MB RAM each).

---

## 9. Error Handling

Follow Perplexity's pattern: graceful degradation to unstyled code.

```kotlin
// Engine returns Result<T> for all operations
val result = engine.highlight(code, "python", theme)

result.fold(
    onSuccess = { annotatedString ->
        // Render highlighted code
    },
    onFailure = { error ->
        // Log error, render plain unstyled code
        // The composable handles this automatically
    }
)
```

**Error scenarios and handling:**

| Scenario | Handling |
|----------|----------|
| WebView fails to initialize | Return `Result.failure(HighlightException.WebViewInitFailed)` |
| JavaScript execution error | Return `Result.failure(HighlightException.JsExecutionFailed)` |
| Unknown language identifier | Highlight.js returns un-highlighted HTML — still works, just no colors |
| CSS theme file not found | Return `Result.failure(HighlightException.ThemeNotFound)` |
| HTML parsing failure | Return `Result.failure(HighlightException.HtmlParseFailed)` |
| Timeout (JS takes too long) | Cancel after 5 seconds, return failure |

**Exception hierarchy** (modeled after Perplexity's `MarkdownException`):

```kotlin
sealed class HighlightException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class WebViewInitFailed(cause: Throwable) : HighlightException("WebView initialization failed", cause)
    class JsExecutionFailed(cause: Throwable) : HighlightException("JavaScript execution failed", cause)
    class ThemeNotFound(path: String) : HighlightException("Theme CSS not found: $path")
    class HtmlParseFailed(cause: Throwable) : HighlightException("HTML parsing failed", cause)
    class Timeout : HighlightException("Highlighting timed out after 5 seconds")
}
```

---

## 10. Performance & Caching Strategy

| What | Strategy | Rationale |
|------|----------|-----------|
| WebView instance | Singleton, created once per `HighlightEngine` | WebView creation costs ~100-200ms and ~2-4 MB RAM |
| Theme color maps | `Lazy` initialization (parsed once per theme) | CSS parsing is ~1-2ms but no reason to repeat |
| JS bridge ready state | `CompletableDeferred`, awaited before first call | `onPageFinished` signals bridge.html is loaded |
| Highlighted results | Caller's responsibility (use `remember {}`) | Library doesn't know the caller's caching policy |
| HTML → AnnotatedString | No cache (fast enough: <1ms for typical code) | Jsoup parsing + tree walk is negligible |

**Expected latency per highlight call:**
- First call: ~200-400ms (WebView init + JS warm-up)
- Subsequent calls: ~10-50ms (JS execution + HTML parsing)
- Theme parsing: ~1-2ms (one-time, cached)

**Memory budget:**
- WebView instance: ~2-4 MB
- highlight.min.js loaded in WebView: ~1 MB
- Theme color map: ~1-2 KB per theme
- AnnotatedString per code block: proportional to code size

---

## 11. Testing Strategy

### Unit Tests (JVM, no Android framework)

| Test | What it verifies |
|------|------------------|
| `ThemeParserTest` | Parses tomorrow.css correctly, extracts all hljs-* classes with correct colors |
| `ThemeParserTest` | Handles compound selectors (`.hljs-title.function_`) |
| `ThemeParserTest` | Extracts font-weight and font-style properties |
| `ThemeParserTest` | Returns empty map for invalid/empty CSS |
| `HtmlToAnnotatedStringTest` | Converts simple `<span class="hljs-keyword">if</span>` to colored span |
| `HtmlToAnnotatedStringTest` | Handles nested spans correctly |
| `HtmlToAnnotatedStringTest` | Preserves whitespace in text nodes |
| `HtmlToAnnotatedStringTest` | Returns plain text for HTML with no recognized classes |
| `HtmlToAnnotatedStringTest` | Handles empty input |

### Instrumented Tests (require Android device/emulator)

| Test | What it verifies |
|------|------------------|
| `HighlightEngineTest` | WebView initializes successfully |
| `HighlightEngineTest` | `highlightToHtml("def foo():", "python")` returns HTML with hljs spans |
| `HighlightEngineTest` | Concurrent highlight calls don't crash |
| `HighlightEngineTest` | Unknown language returns unhighlighted HTML (no crash) |
| `HighlightEngineTest` | Code with special characters (quotes, newlines, unicode) roundtrips correctly |
| `HighlightEngineTest` | `destroy()` releases WebView without crash |
| `SyntaxHighlightedCodeTest` | Composable renders without crash |
| `SyntaxHighlightedCodeTest` | Copy button copies raw code to clipboard |

### Sample App Manual Testing

The `sample/` module provides a demo screen with:
- Multiple languages (Python, Kotlin, Java, JavaScript, SQL, JSON, XML)
- Light/dark theme toggle
- Long code blocks (scroll testing)
- Code with special characters and unicode
- Empty code block edge case
- Rapid theme switching (no flicker)

---

## 12. Non-Goals (Explicitly Out of Scope)

- **Language auto-detection**: User must specify the language. Highlight.js supports auto-detect but it's slow and unreliable for short snippets.
- **Line-level diff highlighting**: No git-diff-style added/removed line coloring.
- **Code editing / input**: This is a read-only display library, not an editor.
- **Custom language grammar definitions**: Use whatever Highlight.js bundles. Adding custom grammars would require rebuilding the JS file.
- **Kotlin Multiplatform (KMP)**: The initial version is Android-only. The engine layer (ThemeParser, HtmlToAnnotatedString) is pure Kotlin and could be extracted to KMP later, but WebView is Android-specific.
- **Android View (non-Compose) support**: Only Jetpack Compose is supported. Legacy apps can use `ComposeView` to embed the composable.
- **Custom font bundling**: The library uses `FontFamily.Monospace` by default. Users can pass any `FontFamily` but the library doesn't ship fonts.
- **Syntax highlighting in EditText / TextField**: This is for displaying code, not editing it.

---

## 13. Reference Implementation Evidence

This PRD is based on reverse-engineering production Android apps. For detailed evidence with smali bytecode excerpts, CSS analysis, and architecture diagrams, see:

| Document | Description |
|----------|-------------|
| [Perplexity Analysis Report](perplexity-analysis-report.html) | Full architecture analysis — closest to what this library implements |
| [Perplexity Technical Reference](perplexity-technical-reference.html) | 12 evidence sections with smali code for every pipeline stage |
| [ChatGPT & Claude Analysis Report](syntax-on-3p-app-analysis-report.html) | Comparison of two Highlight.js approaches |
| [ChatGPT & Claude Technical Reference](technical-reference.html) | Evidence for both apps' implementations |
| [GitHub Analysis Report](github-analysis-report.html) | Alternative approach: server-side highlighting |
| [All Reports Home](index.html) | Index of all analysis documents |

**Key smali files that informed this design (from Perplexity APK):**

| Obfuscated Class | Role | PRD Component |
|-------------------|------|---------------|
| `ra/d` | WebView singleton with lazy init, caches instance | `HighlightEngine` / `WebViewManager` |
| `ra/c` | Suspend lambda: escapes code, builds JS, calls evaluateJavascript | `HighlightEngine.highlightToHtml()` |
| `eu0/f` | WebViewClient: onPageFinished resumes coroutine, shouldInterceptRequest serves assets | `WebViewManager` internal client |
| `gn/d` | "jsHighlighter": CSS parser + recursive HTML span walker | `ThemeParser` + `HtmlToAnnotatedString` |
| `gn/a` | Theme switcher: packed-switch selects light/dark CSS file | `HighlightTheme` companion factory |
| `gn/e` | Data class holding light + dark AnnotatedString pair | `ThemedHighlightResult` |
| `ra/b` | ValueCallback → SafeContinuation bridge | Internal coroutine bridge |

---

## 14. Implementation Order

Build in this sequence — each step is independently testable:

1. **Project setup** — Create Gradle project, add dependencies, configure build
2. **Asset bundle** — Download highlight.min.js, create bridge.html, add theme CSS files
3. **ThemeParser** — Implement + unit test (no Android deps needed)
4. **HtmlToAnnotatedString** — Implement + unit test (uses jsoup only)
5. **HighlightEngine** — Implement WebView manager + JS bridge + instrumented tests
6. **HighlightTheme** — Wire up lazy theme maps with the parser
7. **SyntaxHighlightedCode** — Build the composable with all UI features
8. **HighlightThemeProvider** — CompositionLocal + auto dark/light switching
9. **Sample app** — Demo screen with multiple languages and themes
10. **README** — Usage docs, screenshots, API reference

---

## 15. Success Criteria

The library is complete when:

- [ ] `SyntaxHighlightedCode(code = "...", language = "python")` renders colored code in a Compose app with a single line of code
- [ ] All 192 Highlight.js languages produce highlighted output
- [ ] Light and dark themes switch instantly without re-highlighting
- [ ] Code is natively selectable (long-press to select text)
- [ ] Copy button works and copies raw (unstyled) code
- [ ] Line numbers display correctly and align with code lines
- [ ] Horizontal scrolling works for long lines without wrapping
- [ ] Unknown language gracefully falls back to unstyled monospace
- [ ] WebView failure gracefully falls back to unstyled monospace
- [ ] Custom CSS themes work by dropping a file into assets
- [ ] All unit tests and instrumented tests pass
- [ ] Sample app demonstrates all features across multiple languages
- [ ] Library size overhead is <1.5 MB (mostly highlight.min.js)
