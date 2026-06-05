# Getting Started

## 1. Add the dependency

In your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("dev.hossain:compose-highlight:0.27.0")
}
```

Check [Maven Central](https://search.maven.org/artifact/dev.hossain/compose-highlight) or the [releases page](https://github.com/hossain-khan/android-compose-highlight/releases) for the latest version.

---

## 2. Wrap your screen in `HighlightThemeProvider`

Place `HighlightThemeProvider` once near the root of your composition - inside `setContent {}` or at the top of your screen composable. It creates **one shared WebView** for the entire subtree and automatically selects the light or dark theme based on the system setting.

```kotlin
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.rememberTomorrowTheme
import dev.hossain.highlight.ui.rememberTomorrowNightTheme

setContent {
    MyAppTheme {
        HighlightThemeProvider(
            lightHighlightTheme = rememberTomorrowTheme(),
            darkHighlightTheme  = rememberTomorrowNightTheme(),
        ) {
            MyAppContent()
        }
    }
}
```

---

## 3. Display a code block

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = """
        fun greet(name: String): String {
            return "Hello, ${'$'}name!"
        }
    """.trimIndent(),
    language = "kotlin",
)
```

The composable shows unstyled monospace text immediately and fades in highlighted colors once the WebView finishes processing - no visible flicker.

---

## 4. Common customizations

### Show line numbers

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code            = snippet,
    language        = "kotlin",
    showLineNumbers = true,
)
```

### Custom font size

```kotlin
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    style    = CodeBlockStyle(
        textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontSize = 15.sp),
    ),
)
```

### Hide header elements

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code          = snippet,
    language      = "kotlin",
    languageLabel = null,  // hide language badge
    copyButton    = null,  // hide copy button
)
```

---

## 5. (Optional) Pre-warm the WebView

The hidden WebView initializes lazily on the first highlight call. To reduce first-call latency, pre-warm the WebView renderer process in `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            WebViewCompat.startUpWebView(
                applicationContext,
                WebViewStartUpConfig.Builder(mainExecutor).build(),
                WebViewOutcomeReceiver { /* no-op */ },
            )
        }
    }
}
```

Requires `androidx.webkit:webkit:1.16+` (already a transitive dependency of this library).

---

## Next steps

- [Theming guide](guides/theming.md) - built-in themes, custom CSS, dynamic colors
- [Customization guide](guides/customization.md) - custom language label and copy button slots
- [Performance guide](guides/performance.md) - shared engine, warm-up, timing callbacks
- [Language selection guide](guides/language-selection.md) - resolve language from file extensions, validate names, auto-detect
- [API reference](reference/) - full parameter documentation
