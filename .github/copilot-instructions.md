# Copilot Instructions — android-compose-highlight

## Build, test, and lint commands

```bash
# Lint (ktlint via kotlinter) — must pass before commit
./gradlew lintKotlin

# Auto-fix formatting
./gradlew formatKotlin

# JVM unit tests (fast, no device needed)
./gradlew :compose-highlight:test

# Run a single test class
./gradlew :compose-highlight:test --tests "dev.hossain.highlight.engine.ThemeParserTest"

# Run a single test method
./gradlew :compose-highlight:test --tests "dev.hossain.highlight.engine.ThemeParserTest.parse returns non-empty map for valid CSS"

# Build the library AAR
./gradlew :compose-highlight:assembleDebug

# Build the sample app
./gradlew :sample:assembleDebug

# Run microbenchmarks on a connected device (requires physical device or emulator)
./gradlew :compose-highlight:connectedAndroidTest

# Run a specific benchmark class
./gradlew :compose-highlight:connectedAndroidTest \
  -P android.testInstrumentationRunnerArguments.class=dev.hossain.highlight.benchmark.HighlightEngineBenchmark

# Generate Dokka API docs → docs/api/
./gradlew :compose-highlight:dokkaGeneratePublicationHtml
```

## Architecture

The library has two layers — `engine/` (internal) and `ui/` (public):

```
SyntaxHighlightedCode   ← primary public composable
 └── rememberHighlightedCode / rememberHighlightEngine  (RememberHelpers.kt)
       └── HighlightEngine              ← public, suspend-based pipeline
             ├── WebViewManager         ← internal, owns the hidden WebView
             ├── HighlightTheme         ← public, CSS-backed theme model
             ├── ThemeParser            ← internal, CSS → Map<selector, SpanStyle>
             └── HtmlToAnnotatedString  ← internal, jsoup → AnnotatedString
```

**How highlighting works end-to-end:**
1. `WebViewManager` creates a hidden (off-screen) `WebView` on the Main thread and loads `bridge.html` from `assets/compose-highlight/`. This page loads `highlight.min.js` and exposes `highlightCode(code, lang)`.
2. `HighlightEngine` serializes calls with a `Mutex` and calls `evaluateJavascript()` to invoke `highlightCode`, getting back HTML with `<span class="hljs-*">` tokens.
3. `ThemeParser` lazily parses a Highlight.js CSS file into a `Map<String, SpanStyle>` (selector → style), cached per `HighlightTheme` instance.
4. `HtmlToAnnotatedString` uses jsoup to walk the HTML and applies the theme's `SpanStyle` map to produce a Compose `AnnotatedString`.

**Why `https://appassets.androidplatform.net`:** `WebViewAssetLoader` intercepts requests to this reserved fake domain and maps `/assets/` to the app's `assets/` folder. This is required because `file://` URLs block `<script>` execution via Same-Origin Policy.

## Key conventions

**Public vs internal:** Only `ui/` and `engine/{HighlightEngine,HighlightTheme,HighlightException}.kt` are public API. All `engine/` helpers (`WebViewManager`, `ThemeParser`, `HtmlToAnnotatedString`) are `internal`.

**`android.util.Log` is banned from the library.** Any `Log.*` call in code paths executed by JVM unit tests causes `RuntimeException: Method d in android.util.Log not mocked`. Remove all debug logging before committing.

**All `HighlightEngine` results use `Result<T>`.** Never throw from public engine methods; wrap failures in `Result.failure(HighlightException(...))`. `HighlightException` is a sealed class — add new variants there rather than throwing raw exceptions.

**WebView must run on the Main thread.** `WebViewManager.initialize()` and `destroy()` dispatch to `Dispatchers.Main` and `Handler(Looper.getMainLooper())` respectively. Never call WebView APIs off the Main thread.

**`rememberHighlightEngine()` for lifecycle management.** In Compose, always use `rememberHighlightEngine()` (not bare `HighlightEngine(context)`) — it calls `engine.destroy()` via `DisposableEffect` when the composable leaves composition.

**`SyntaxHighlightedCode` requires a theme.** Its `theme` parameter defaults to `LocalHighlightTheme.current`, which throws if no `HighlightThemeProvider` ancestor exists. Always wrap usage in `HighlightThemeProvider { }` or pass an explicit `theme =` argument.

**`HighlightTheme` is lazy.** CSS parsing happens on first use of `colorMap`, not at factory-call time. This means `fromAsset()` errors surface when the theme is first applied, not when the factory is called.

**Formatting:** ktlint via `org.jmailen.kotlinter`. The `.editorconfig` suppresses the function-naming rule for `@Composable`-annotated functions (`ktlint_function_naming_ignore_when_annotated_with = Composable`). Run `./gradlew formatKotlin` before committing.

**JVM unit tests vs instrumented tests:** `src/test/` contains JVM tests (fast, use `ThemeParser.parse(cssString)` overload that takes raw CSS). `src/androidTest/` contains instrumented tests (`HighlightEngineTest`) and microbenchmarks (`benchmark/`) that require a connected device and use `BenchmarkRule`.

**Asset path convention:** All library assets live under `assets/compose-highlight/` to avoid collisions when the library is consumed. CSS themes go in `assets/compose-highlight/themes/`.

**Keep CHANGELOG.md up to date.** For every PR or commit that adds a feature, fixes a bug, or makes a breaking change, add an entry under the relevant `[Unreleased]` section in `CHANGELOG.md` at the repo root. When cutting a release, rename `[Unreleased]` to the version number with the date.

**KDoc is required on all public API.** Dokka API docs are generated from KDoc and published to GitHub Pages (`.github/workflows/docs.yml`). Every public class, function, and property in `ui/` and the public `engine/` classes must have KDoc. Include at least one usage example (triple-backtick code block) on non-trivial classes and composables. Internal classes do not need KDoc but benefit from it.

**Before every commit — verify stability.** Run the following three tasks and ensure they all pass:
```bash
./gradlew formatKotlin                          # auto-fix formatting
./gradlew :compose-highlight:assembleDebug :sample:assembleDebug  # both must build
./gradlew :compose-highlight:test               # all JVM unit tests must pass
```
Do not commit if any of these fail.

**Git tags must not use a `v` prefix.** Use `0.2.0`, not `v0.2.0`. JitPack uses the tag as the dependency version, so the version string consumers write in their `build.gradle.kts` matches the tag exactly.

**Dependency coordinates (JitPack):**
```
com.github.hossain-khan.android-compose-highlight:compose-highlight:<version>
```
