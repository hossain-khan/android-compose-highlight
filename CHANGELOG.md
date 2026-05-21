# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Performance
- `HtmlToAnnotatedString.convertTimed()` and theme color map resolution now run on `Dispatchers.Default` instead of the main thread. For large code blocks with hundreds of spans, this eliminates the risk of dropped frames during parsing. WebView JS calls remain on `Dispatchers.Main` as required by Android.
- `highlightAuto()` now releases the serializing mutex before the CPU-intensive HTML parsing step, allowing other highlight calls to proceed sooner.

### Fixed
- Added jsoup keep rules to `consumer-rules.pro` so consuming apps with R8 minification enabled
  do not encounter runtime crashes (`ClassNotFoundException`, `NoSuchMethodError`) from stripped
  or obfuscated jsoup classes. Rules cover `org.jsoup.Jsoup`, `org.jsoup.parser.**`,
  `org.jsoup.nodes.**`, and `org.jsoup.select.**`.

## [0.20.0] - 2026-05-20

### Added
- `HighlightLanguage.fromExtension(ext)` utility - maps file extensions to Highlight.js language names without a WebView round-trip (e.g. `"kt"` -> `"kotlin"`)
- `HighlightEngine.highlightAuto()` - highlights code with automatic language detection via `hljs.highlightAuto()`, returns `AutoHighlightResult` with `detectedLanguage`
- `HighlightEngine.getLanguage()` - looks up a language by name or alias via `hljs.getLanguage()`, returns `HighlightLanguageInfo` (name + aliases list) or null if not found
- `AutoHighlightResult` data class - result of `highlightAuto()` with `detectedLanguage`, `annotated`, `spanCount`, `durationMs`, and `timings`
- `HighlightLanguageInfo` data class - result of `getLanguage()` with `name` and `aliases`
- Documentation and sample app demos for the language discoverability APIs, including a new language selection guide, `HighlightLanguage` reference, and interactive sample tab covering `fromExtension()`, `getLanguage()`, and `highlightAuto()`
- **Bridge validation workflow** - added `.github/workflows/bridge-validation.yml` to validate
  `compose-highlight/src/main/assets/compose-highlight/bridge.html` in CI. The workflow runs
  HTML syntax checks and enforces the JS bridge contract (`highlightCode`, `listLanguages`,
  `hljsVersion`) plus the expected WebView `bridge.html` load URL.

## [0.19.1] - 2026-05-18

### Fixed
- **`CodeBlockStyle.copyButtonSize` now takes effect on the default copy button** - previously
  the value was stored in `CodeBlockStyle` but never forwarded to `SyntaxHighlightedCodeDefaults.CopyButton`,
  so changing it had no visible impact. The default button now correctly reads `style.copyButtonSize`.
- **`SyntaxHighlightedCodeDefaults.CopyButton` - `size` parameter now scales the icon glyph** -
  previously the `⧉` glyph was rendered at a hardcoded `16.sp` regardless of the `size` argument.
  Font size is now derived proportionally from `size` (`size * 0.5f`), so the glyph visually
  scales when `copyButtonSize` is changed via `CodeBlockStyle` or passed directly.

## [0.19.0] - 2026-05-17

### Changed (Breaking)
- **`SyntaxHighlightedCode` - slot API hardening** - replaced boolean visibility flags and partial
  slot parameters with full composable slots:
  - `showLanguageLabel: Boolean` removed; replaced by `languageLabelContent: (@Composable () -> Unit)?`
    (`null` = hide, non-null = custom content; default shows language identifier in dimmed style).
  - `showCopyButton: Boolean`, `copyButtonIcon: (@Composable (tint: Color) -> Unit)?`, and
    `copyButtonContentDescription: String` removed; replaced by
    `copyButtonContent: (@Composable (onClick: () -> Unit) -> Unit)?`
    (`null` = hide; slot receives a pre-wired `onClick` action; default uses
    `SyntaxHighlightedCodeDefaults.CopyButton`).
  - `Surface` now explicitly sets `contentColor = textColor` so `LocalContentColor.current` resolves
    correctly inside both slots.

### Added
- **`SyntaxHighlightedCodeDefaults.CopyButton`** - public composable helper that renders the
  default `⧉` copy icon inside an `IconButton`. Accepts `onClick`, `tint`, `contentDescription`,
  and `size` parameters so callers can compose on top of the default look.
- **`SyntaxHighlightedCodeDefaults.LanguageLabel`** - public composable helper that renders
  the language badge. Accepts `language`, `color`, and `fontSize` parameters. Useful for
  toggling visibility at runtime without reconstructing the full default style.

### Fixed
- **Private `LineNumberedCode` composable** - now accepts `modifier: Modifier = Modifier` and
  applies it to the root `Row`, consistent with Compose modifier conventions.

## [0.18.0] - 2026-05-16

### Added
- **`HighlightTimings` data class** - Per-layer timing breakdown for a single highlight call.
  Each field is a `kotlin.time.Duration` measured via `measureTimedValue` at each pipeline stage:
  - `jsBridge` - `evaluateJavascript()` round-trip into WebView running highlight.js
  - `jsonUnescape` - `unescapeJsString()` character-by-character pass over the returned JSON string
  - `htmlParse` - `Jsoup.parseBodyFragment()` - HTML to DOM
  - `treeWalk` - DOM node walk and `SpanStyle` lookup from theme color map
  - `themeParse` - `ThemeParser.parse()` - first-use only; `Duration.ZERO` on cache hits
  - `total` - end-to-end time for the full highlight call
- **`HighlightResult.timings`** - `HighlightTimings` field on `HighlightResult`; always populated.
  Existing `durationMs` is preserved (equals `timings.total.inWholeMilliseconds`).
- **`ThemedHighlightResult.timings`** - `HighlightTimings` field on `ThemedHighlightResult`; always
  populated. `themeParse` covers both light and dark theme parse times combined.
- **`HtmlHighlightResult.jsBridgeDuration` / `jsonUnescapeDuration`** - `Duration` fields on
  `HtmlHighlightResult` exposing JS bridge and JSON unescape timings directly for callers that
  use `highlightToHtml()`. Default to `Duration.ZERO` if not populated (backwards compatible).

## [0.17.2] - 2026-05-16

### Fixed
- **`ThemeParser` - split rules for same selector lost earlier properties** - themes that
  define a selector in multiple CSS rules (e.g. `nord` sets `.hljs { background }` and then
  `.hljs { color }` separately) caused the second rule to silently overwrite the first, losing
  the background color. Rules for the same selector are now merged: later explicit values win,
  but earlier values for unset properties are preserved.
- **`ThemeParser` - descendant selector with non-hljs element leakage** - selectors like
  `.hljs mark { background:#555 }` and `.hljs a { color:inherit }` were not filtered as
  descendant rules because only selectors with two or more `.hljs-*` tokens were skipped.
  A selector containing any whitespace is now unconditionally skipped, since whitespace
  always indicates a descendant/combinator relationship that is context-specific. Without
  this fix, `.hljs mark { background:#555 }` in the `agate` theme overwrote the real `.hljs`
  background (`#333333`) with the mark-highlight color (`#555555`).

## [0.17.1] - 2026-05-16

### Fixed
- **`ThemeParser` - CSS named color support** - colors specified by name (e.g. `color: red`,
  `color: green`, `color: grey`) were silently ignored, causing tokens that use named colors
  to render in the default text color. Added a map of 21 named CSS colors covering all colors
  used in bundled highlight.js themes (including `red`, `green`, `grey`, `silver`, `navy`,
  `teal`, `purple`, `maroon`, `gold`, `orange`, etc.).
- **`ThemeParser` - 4-digit hex color support** - colors specified as `#rgba` (e.g. `#444a`)
  were not parsed; each digit is now expanded by doubling identical to 3-digit `#rgb` handling.
- **`ThemeParser` - `@media` at-rule block leakage** - inner rules inside `@media` (and other
  at-rule) blocks (e.g. `@media screen and (-ms-high-contrast:active)`) were mistakenly parsed
  as top-level rules and could overwrite earlier correct color entries. For example,
  `a11y-light` has `.hljs-keyword { font-weight:700 }` inside a `@media` block that overwrote
  the real `.hljs-keyword { color:#7928a1 }`, causing keywords to appear dark instead of purple.
  All `@` at-rule blocks and their content are now stripped before rule extraction. CSS comments
  are also stripped first to avoid misreading author email addresses (e.g. `@ericwbailey`) as
  at-rule markers.

## [0.17.0] - 2026-05-16

### Fixed
- **`ThemeParser` — pseudo-element/pseudo-class selector leak** — selectors containing `::` or `:`
  (e.g. `.hljs::selection`, `.hljs:hover`) were being stripped of the pseudo-part and incorrectly
  stored as the `.hljs` base entry. This caused the selection-highlight background color
  (e.g. `#516d7b` in `base16/atelier-lakeside`) to overwrite the real theme background (`#161b1d`),
  making code blocks appear with the wrong background color. All `::selection`, `:hover`, etc. rules
  are now skipped during parsing. Affects all 4 bundled themes (`tomorrow`, `tomorrow-night` had
  `::selection` rules) and any custom `fromAsset()` / `fromCss()` theme with similar rules.

### Changed
- **Sample app — "All Themes" tab** — new tab that bundles all 256 highlight.js 11.11.1 theme CSS
  files as sample app assets. A searchable dropdown lets users pick any theme and instantly
  live-preview it on a code block, similar to the highlightjs.org/demo experience.
- **Sample app — interactive Styling section** — replaced three static `CodeBlockStyle` demo blocks
  with a single live-preview code block. An `ExtendedFloatingActionButton` (bottom-right) opens a
  `ModalBottomSheet` where all `CodeBlockStyle` parameters can be adjusted and instantly reflected
  in the code block behind it.
- **Sample app — Themes section** — trimmed to one built-in theme demo and one `fromAsset()` demo
  that shows its own loading code as the highlighted snippet.

## [0.16.0] - 2026-05-16

### Fixed
- **`CancellationException` is no longer swallowed in `HighlightEngine`** — `highlightToHtml`,
  `supportedLanguages`, and `highlightJsVersion` previously caught all `Exception` types,
  silently converting parent coroutine cancellations into `Result.failure(JsExecutionFailed(...))`.
  They now rethrow `CancellationException` to respect structured concurrency.
- **`HighlightException.Timeout` is now actually thrown on timeout** — the same broad
  `catch (e: Exception)` was swallowing `TimeoutCancellationException` (from `withTimeout`),
  making `HighlightException.Timeout` dead code. Timeout is now correctly caught and converted
  to `Result.failure(HighlightException.Timeout())` in all three methods.

### Performance
- **`SyntaxHighlightedCode` is now `restartable skippable`** — Derived colors and `TextStyle`
  values (`backgroundColor`, `textColor`, `lineNumberColor`, `themedCodeStyle`,
  `themedLineNumStyle`, `languageLabelStyle`) are now wrapped in `remember(theme, style)` blocks
  so they are only recomputed when the theme or style changes, not on every recomposition.
  This eliminates 6 unnecessary `TextStyle.copy()` / `Color` allocations per recomposition and
  allows the Compose compiler to classify `SyntaxHighlightedCode`, `LineNumberedCode`, and
  `CopyButton` as `skippable` (previously none were skippable — verified via compiler reports).
  Result: `knownUnstableArguments` dropped from 4 → 0; skippable composables increased from 0 → 7.

## [0.15.0] - 2026-05-14

### Changed
- **`HighlightEngine.initialize()` now returns `Result<Unit>`** instead of throwing
  `HighlightException.WebViewInitFailed`. This aligns `initialize()` with the library's
  documented `Result`-based public error model. Migration: replace bare `engine.initialize()`
  calls with `engine.initialize().onFailure { /* handle */ }` or
  `engine.initialize().getOrThrow()` if you want failure to propagate as an exception.
- **`highlightBothThemes` now parses the HTML once** — `HtmlToAnnotatedString.convertBothThemes()`
  replaces the prior double-`convert()` call in `HighlightEngine.highlightBothThemes()`. The HTML
  fragment is parsed into a DOM once and walked once, with two `AnnotatedString.Builder` instances
  updated in parallel (one per theme). This removes a redundant Jsoup parse and a redundant DOM
  traversal on every dual-theme highlight call. Closes #82.

### Added
- **Robolectric JVM tests for Compose UI** — Added 13 new unit tests in `src/test/` that run on
  the JVM without an emulator using Robolectric 4.16.1 and the v2 Compose testing APIs
  (`androidx.compose.ui.test.junit4.v2.createComposeRule`).
  - `SyntaxHighlightedCodeRobolectricTest` (8 tests): verifies preview fallback rendering, test
    tags, language label, copy button visibility, copy button content description, custom copy
    button content description, copy click callback, and default language label display.
  - `HighlightThemeProviderRobolectricTest` (5 tests): verifies theme provision, light/dark theme
    access, dark-mode selection, light-mode selection, and the expected error when accessed without
    a provider.

### Fixed
- **`HighlightTheme` context factories now normalize to `applicationContext` internally** —
  `tomorrow`, `tomorrowNight`, `atomOneDark`, `atomOneLight`, and `fromAsset` defensively resolve
  `context.applicationContext` before retaining it in lazy theme providers, preventing accidental
  Activity-context retention when a theme instance outlives an Activity lifecycle.
- **Sample app preserves top-level demo selections across recreation** — `SampleScreen` now uses
  `rememberSaveable` for the light/dark toggle, selected demo tab, and selected theme family so
  those user-facing choices survive configuration changes.

## [0.14.0] - 2026-05-13

### Added
- **LeakCanary integrated into the sample app** — `com.squareup.leakcanary:leakcanary-android:2.14`
  is added as a `debugImplementation` dependency in `sample/build.gradle.kts`. LeakCanary
  automatically detects memory leaks in debug builds and displays a notification with a heap dump
  analysis when a leak is found. No code changes are required — LeakCanary installs itself via
  its `ContentProvider`.

### Changed
- **`SelectionContainer` moved inside `AnimatedContent`** in `SyntaxHighlightedCode` — during the
  plain-text → highlighted crossfade, `SelectionContainer` now wraps only the currently visible
  content rather than both states simultaneously, preventing potential disruption to active text
  selection during the transition animation.
- **`CodeBlockStyle` annotated `@Stable`** — Compose can now skip recomposition of callers that
  pass an unchanged `CodeBlockStyle`. For custom styles constructed inline in a composable, wrap
  them in `remember` to avoid unnecessary recompositions:
  ```kotlin
  val myStyle = remember { CodeBlockStyle(padding = PaddingValues(8.dp)) }
  ```

### Fixed
- **`WebViewManager`: race condition between `initialize()` and `destroy()` can no longer hang
  the engine permanently.** If `destroy()` fires from another thread while `initialize()` is
  creating the WebView (after `webView = view` but before `onPageFinished`), `destroy()` would
  cancel the captured `CompletableDeferred` and replace `readyDeferred` with a fresh instance.
  `onPageFinished` would then skip completing (cancelled deferred is already "completed"), leaving
  the new deferred permanently unresolved — all subsequent highlight calls would suspend forever.
  Two targeted fixes applied (Option C from the issue report):
  - `readyDeferred` is now `@Volatile` so writes by `destroy()` (any thread) are immediately
    visible to `getReadyWebView()` and `initialize()` on other threads (ARM weak memory model).
  - `onPageFinished` now checks `webView == null` before completing the deferred. If `destroy()`
    ran while the page was loading, the callback returns early; the next `initialize()` call
    picks up the fresh deferred and completes it normally, fully recovering the engine.
- **U+2028/U+2029 escaping in JS template** — `executeJs` now escapes Unicode Line Separator
  (U+2028) and Paragraph Separator (U+2029) before interpolating code into the JS call.
  Pre-ES2019 WebView engines (Android < 10) treat these as line terminators inside string literals,
  causing a `SyntaxError` / `JsExecutionFailed`. The escape logic has been extracted into a
  testable package-level `escapeForJs` function.
- `SyntaxHighlightedCode` no longer crashes in Android Studio `@Preview`. When
  `LocalInspectionMode.current` is `true`, the composable renders a plain-text
  monospace fallback and skips WebView initialization entirely.
- `rememberHighlightedCode` and `rememberHighlightedCodeBothThemes` skip the
  `LaunchedEffect` (and thus never call the WebView engine) when running inside
  an Android Studio Preview, preventing crashes in preview-only code paths.
- **`WebViewManager.webView` marked `@Volatile`** — prevents stale-read on ARM's weak memory model
  when `initialize()` checks the field before switching to the Main thread.
- **`language` parameter escaped in JS template** — `executeJs` now escapes backslashes and single
  quotes in `language` before interpolating into the `highlightCode(...)` JS call, closing a minor
  JS-injection vector (defense-in-depth; the WebView has no access to sensitive data).
- **Accessibility: copy button `contentDescription`** — The copy-to-clipboard `IconButton` inside
  `SyntaxHighlightedCode` now carries `contentDescription = "Copy code"` so TalkBack announces it
  meaningfully instead of the generic "Button".
- **Accessibility: `testTag` on outer container** — `SyntaxHighlightedCode` now applies
  `testTag("syntax-highlighted-code")` on its outer `Surface`, giving UI-test consumers a stable
  node handle without relying on fragile text or structure queries.
- **`HighlightThemeProvider` default themes are now remembered** — default parameters now use
  `rememberTomorrowTheme()` / `rememberTomorrowNightTheme()` so recomposition no longer allocates
  new `HighlightTheme` wrapper instances when callers rely on defaults.

## [0.13.0] - 2026-05-11

### Infrastructure
- Migrated publishing from `gradle-nexus/publish-plugin` to
  `com.vanniktech:gradle-maven-publish-plugin` 0.36.0, which natively supports
  the Sonatype Central Portal API for new accounts.
- Removed JitPack distribution; library is now published exclusively to
  Maven Central (`dev.hossain:compose-highlight`).

## [0.12.0] - 2026-05-11

### Added
- **Maven Central publishing** — library is now published to Maven Central (`dev.hossain:compose-highlight`)
  in addition to JitPack. GPG signing, sources JAR, and Dokka HTML javadoc JAR are all included.
- **`PUBLISHING.md`** — end-to-end guide covering prerequisites (Sonatype, GPG, GitHub Secrets),
  release steps, dry-run instructions, local dry run, and Gradle task reference.

### Infrastructure
- Added `gradle-nexus/publish-plugin` v2.0.0 for Central Portal staging API integration.
- Added `publish.yml` GitHub Actions workflow with `workflow_dispatch` (`tag` + `dry_run` inputs),
  pre-flight tag/already-published checks, artifact validation, and one-click real publish.

## [0.11.0] - 2026-05-11

### Added
- **`HtmlHighlightResult` data class** — `HighlightEngine.highlightToHtml()` now returns
  `Result<HtmlHighlightResult>` instead of `Result<String>`. The result pairs the raw HTML with
  timing data so callers no longer need to measure JS round-trip time manually:
  - `html: String` — raw HTML with `<span class="hljs-*">` tokens (same content as before, via `.html`)
  - `durationMs: Long` — JavaScript round-trip time, measured after the WebView is ready and the internal mutex is acquired (excludes WebView warm-up and queue-wait time)

  **Migration:** replace `.onSuccess { html -> ... }` with `.onSuccess { it.html }`:
  ```kotlin
  // Before
  engine.highlightToHtml(code, "kotlin").onSuccess { html -> renderRawHtml(html) }

  // After
  engine.highlightToHtml(code, "kotlin").onSuccess { result ->
      renderRawHtml(result.html)
      log("JS round-trip: ${result.durationMs} ms")
  }
  ```

- **`HighlightEngine.isInitialized: StateFlow<Boolean>`** — changed from a plain `Boolean`
  property to a `StateFlow<Boolean>`, enabling Compose composables to observe engine
  initialization reactively without a separate `var engineReady` flag:
  ```kotlin
  val isReady by engine.isInitialized.collectAsState()
  if (isReady) { /* WebView is warm */ }
  ```

  **Migration:** replace `engine.isInitialized` with `engine.isInitialized.value` in
  non-Compose contexts; use `engine.isInitialized.collectAsState()` in Compose.

- **`rememberHighlightedCodeBothThemes` now accepts `onHighlightComplete`** — added
  `onHighlightComplete: ((ThemedHighlightResult) -> Unit)?` callback parameter for consistency
  with `rememberHighlightedCode`. Fires after the state is updated on success.

- **`rememberHighlightedCodeBothThemes` now works inside `HighlightThemeProvider`** — `lightTheme`
  and `darkTheme` parameters now default to `LocalLightHighlightTheme.current` and
  `LocalDarkHighlightTheme.current` respectively, so callers inside a `HighlightThemeProvider`
  no longer need to pass themes explicitly.

- **`LocalLightHighlightTheme` and `LocalDarkHighlightTheme` CompositionLocals** — `HighlightThemeProvider`
  now provides both the individual light and dark themes via these new public CompositionLocals
  (in addition to the existing `LocalHighlightTheme` for the active theme). Useful when
  composables need both variants simultaneously.

- **`@Composable` theme helpers** — new `rememberTomorrowTheme()`, `rememberTomorrowNightTheme()`,
  `rememberAtomOneDarkTheme()`, `rememberAtomOneLightTheme()` functions resolve `LocalContext`
  internally, removing the need for `val context = LocalContext.current` boilerplate at call sites:
  ```kotlin
  // Before
  val theme = remember(context) { HighlightTheme.tomorrow(context.applicationContext) }

  // After
  val theme = rememberTomorrowTheme()
  ```

### Fixed
- **`HtmlHighlightResult.durationMs` now measures JS round-trip only** — the timer previously
  started before WebView initialisation and mutex acquisition, so it included warm-up and
  queue-wait time. It now starts immediately before `evaluateJavascript()` is called, after the
  WebView is ready and the internal mutex is held.

### Sample app improvements
- **Code samples moved to asset files** — 17 language samples previously hardcoded as Kotlin raw
  strings in `SampleData.kt` are now individual files in `assets/samples/` (e.g. `01_fibonacci.py`,
  `08_WeatherApp.kt`). Adding a new language sample only requires dropping a file in that folder —
  no Kotlin changes needed. Each file has a real extension so IDEs apply syntax highlighting when
  viewing or editing them.
- **`sample/README.md`** — documents the sample app structure, what each tab demonstrates, and
  how to add new language samples or custom themes.
- **Sample app organisation** — `DemoSections.kt` split into a `sections/` package (one file per
  tab); tab routing uses a `DemoTab` sealed class instead of integer indices.
- **Fixed: Engine tab language list now scrollable** — the 192-language list was clipped at a
  fixed height with no scroll. Fixed by adding `verticalScroll` to the list container.
- **Fixed: App crash on launch (NPE in tab bar)** — `DemoTab.all` companion `val` was evaluated
  during class init before the `data object` instances were set, resulting in a list of nulls.
  Fixed with `by lazy { }`.

## [0.10.0] - 2026-05-10

### Added
- **`HighlightResult` data class** — `HighlightEngine.highlight()` now returns
  `Result<HighlightResult>` instead of `Result<AnnotatedString>`. The result carries:
  - `annotated: AnnotatedString` — the highlighted text (same as before, via `.annotated`)
  - `spanCount: Int` — number of highlight spans; `0` signals a silent failure (unsupported
    language or empty input) without an exception
  - `language: String` — the language identifier that was requested
  - `durationMs: Long` — pure highlight time (JS call + HTML conversion), excluding
    coroutine-scheduling overhead  

  **Migration:** replace `.onSuccess { it }` with `.onSuccess { it.annotated }`.

- **`HighlightEngine.isInitialized: Boolean`** — `true` once the hidden WebView has loaded
  `bridge.html`. Removes the need for a manual `var engineReady` flag in calling code.

- **`ThemedHighlightResult.durationMs: Long`** — timing is now included in the result returned
  by `highlightBothThemes` and `rememberHighlightedCodeBothThemes`. Read it directly from the
  state value instead of using a separate callback.

- **`HighlightEngine.supportedLanguages(): Result<List<String>>`** — returns the sorted list of
  language identifiers supported by the bundled Highlight.js (190+ languages). Result is fetched
  from the JS engine on the first call and cached for subsequent calls.

  ```kotlin
  engine.supportedLanguages().onSuccess { languages ->
      val isKotlinSupported = "kotlin" in languages  // true
  }
  ```

- **`HighlightEngine.highlightJsVersion(): Result<String>`** — returns the version string of the
  bundled Highlight.js library (e.g. `"11.11.1"`). Cached after the first call.

  ```kotlin
  engine.highlightJsVersion().onSuccess { version ->
      println("Using Highlight.js $version")
  }
  ```

- **Sample app: Engine tab** — new tab in the demo app showcasing `highlightJsVersion()` and
  `supportedLanguages()`. Displays the bundled Highlight.js version string and a scrollable,
  numbered list of all 192 supported language identifiers.

### Changed
- **`onHighlightComplete` callback now receives `HighlightResult`** — both
  `SyntaxHighlightedCode` and `rememberHighlightedCode` previously passed `durationMs: Long`
  to the callback; they now pass the full `HighlightResult`. Use `result.durationMs` for
  timing, `result.spanCount` for silent-failure detection.

  **Migration:**
  ```kotlin
  // Before
  onHighlightComplete = { durationMs -> showTiming(durationMs) }

  // After
  onHighlightComplete = { result -> showTiming(result.durationMs) }
  ```

- **`rememberHighlightedCode` timing is now measured inside the engine** — `durationMs` in
  `HighlightResult` reflects pure highlight time (JS round-trip + HTML parse), not
  coroutine-scheduling overhead.

### Removed
- **`onHighlightComplete` removed from `rememberHighlightedCodeBothThemes`** — timing is now
  available directly on `ThemedHighlightResult.durationMs`, so a separate callback is not
  needed. Read timing from the state value you already hold:
  ```kotlin
  val result by rememberHighlightedCodeBothThemes(...)
  val timing = result?.durationMs   // available once result is non-null
  ```

## [0.9.0] - 2026-05-10

### Added
- **`SyntaxHighlightedCodeDefaults` object** — new top-level object that exposes all default
  values used by `SyntaxHighlightedCode` and `CodeBlockStyle` (`codeTextStyle`, `shape`,
  `padding`, `headerPadding`, `lineNumberWidth`, `copyButtonSize`). Callers can now discover
  and override individual defaults without hard-coding magic numbers:
  ```kotlin
  CodeBlockStyle(
      textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontSize = 15.sp),
  )
  ```

### Changed
- **`CodeBlockStyle` gains a `textStyle: TextStyle` property** — font family, font size, and line
  height are now configured via `CodeBlockStyle.textStyle` (defaulting to
  `SyntaxHighlightedCodeDefaults.codeTextStyle`: monospace, 13 sp, 20 sp line height).
- **`SyntaxHighlightedCode`: removed `fontFamily`, `fontSize`, `lineHeight` parameters** —
  these three top-level parameters are replaced by `CodeBlockStyle.textStyle`. Consolidating
  typography into `CodeBlockStyle` follows established Compose library patterns (e.g. Material 3,
  Haze) where all visual style is expressed through a single style object.

  **Migration:** replace individual parameters with `style = CodeBlockStyle(textStyle = ...)`:
  ```kotlin
  // Before
  SyntaxHighlightedCode(code = snippet, language = "kotlin", fontSize = 15.sp, lineHeight = 24.sp)

  // After
  SyntaxHighlightedCode(
      code     = snippet,
      language = "kotlin",
      style    = CodeBlockStyle(
          textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(
              fontSize   = 15.sp,
              lineHeight = 24.sp,
          ),
      ),
  )
  ```

## [0.8.0] - 2026-05-10

### Changed
- **Updated target SDK from 37 to 36** — Android 17 (API 37) is in late beta; using stable API 36 (Android 12) for production compatibility while maintaining minSdk 24.

## [0.7.0] - 2026-05-09

### Added
- **`SyntaxHighlightedCode`: `copyButtonIcon` composable slot** — optional parameter
  `copyButtonIcon: (@Composable (tint: Color) -> Unit)?` that replaces the default `⧉` text
  icon with any composable. Receives the theme-derived `tint` color so custom icons blend
  naturally with the code block background. Defaults to `null` (original `⧉` behaviour).
- **Sample app: performance benchmark screen** — new `PerfActivity`/`PerfScreen` that highlights
  all language samples and displays per-block timing (ms), line count, and character count as
  metric chips. Includes a dark/light toggle that clears and re-runs all benchmarks so theme
  changes are reflected in measurements.
- **Sample app: 8 new language samples** — TypeScript, Rust, Go, Swift, C++, C#, Bash, and CSS
  added to `SampleData`, each using constructs that stress different highlighter token types
  (generics, lifetimes, goroutines, template literals, etc.).
- **Sample app: Snackbar copy confirmation** — the Languages tab now defines a shared
  `onCopyClick` handler that copies code to the system clipboard and shows a
  `"Successfully copied source code to clipboard"` Snackbar, demonstrating caller-owned
  copy feedback.

### Fixed
- **Sample app: edge-to-edge insets on `LazyColumn`s** — both `SampleScreen` and `PerfScreen`
  now pass bottom (and top) system bar insets to the list's `contentPadding` parameter and use
  `consumeWindowInsets` on the parent container. Previously the inset was applied as
  `Modifier.padding(innerPadding)` on the container, which clipped the list and prevented the
  last item from scrolling clear of the navigation bar.

### Changed
- **`SyntaxHighlightedCode`: removed internal copy confirmation UI** — the library no longer
  manages a 2-second "Copied!" flash internally. The `onCopyClick` callback is the signal that
  a copy occurred; callers own the feedback UX (Snackbar, Toast, animated indicator, etc.).
  This is a **behavioural change**: apps that relied on the built-in "Copied!" text will need
  to implement their own confirmation via `onCopyClick`.
- **Sample app: vector icons throughout** — emoji placeholders in the TopAppBar (theme picker,
  light/dark toggle, benchmark launcher) and perf screen metric chips replaced with Material
  Design vector drawables (`palette_24dp`, `light_mode_24dp`, `mode_night_24dp`, `speed_24dp`,
  `timer_24dp`, `format_line_spacing_24dp`, `type_specimen_24dp`).
- **Sample app: Atom One dark as default** — both the main screen and the benchmark screen now
  open in dark mode with the Atom One theme instead of the system default.
- **Sample app: `copyButtonIcon` used in Languages tab** — all code blocks in the Languages tab
  now render a `content_copy_24dp` vector icon instead of the default `⧉` character.

## [0.6.0] - 2026-05-09

### Added
- **Sample app: comprehensive customization demo** — The sample app now has tabbed navigation with
  seven sections showcasing every public API feature:
  - **Languages**: original multi-language highlight demo (Python, Kotlin, Java, SQL, etc.)
  - **Styling**: `CodeBlockStyle.Default`, `CodeBlockStyle.Compact`, and a custom style with shape, padding, gutter width, and copy-button size
  - **Typography**: `fontSize`, `lineHeight`, and `fontFamily` variants
  - **Toggles**: 2×2 combinations of `showLineNumbers`/`showLanguageLabel` plus `showCopyButton` on/off
  - **Callbacks**: `onHighlightComplete` (displays millisecond duration) and `onCopyClick` (custom handler with inline feedback)
  - **Themes**: all `HighlightTheme` factory methods — built-in (tomorrow, atom-one), `fromAsset()` (GitHub CSS), `fromCss()` (inline Material 3 CSS), and `fromColorMap()` (Material 3 color map)
  - **Advanced**: `rememberHighlightedCodeBothThemes()` — pre-highlights for both light and dark in one JS call for instant theme switching

### Changed
- Bumped `androidx.compose:compose-bom` from `2026.03.01` to `2026.05.00`.
- Bumped `androidx.benchmark` from `1.3.3` to `1.4.1`.
- Bumped `org.jsoup:jsoup` from `1.18.3` to `1.22.2`.
- Bumped `androidx.webkit` from `1.13.0` to `1.16.0` (stable). Highlights of changes since
  1.13.0 relevant to this project:
  - `startUpWebView()` and `WebViewStartUpConfig` APIs graduated to stable.
  - `NavigationListener` / `WebViewCompat.addNavigationListener()` graduated to stable.
  - `minSdk` for the webkit library increased to 24 (matches this library's `minSdk`).

### Added
- JVM unit tests for `HighlightTheme`: `fromCss`, `fromColorMap`, lazy `colorMap`, `backgroundColor`, `defaultTextColor`, `equals`/`hashCode`/`toString`, and defensive-copy behavior
- JVM unit tests for all `HighlightException` variants: message content, cause preservation, and the `TIMEOUT_SECONDS` constant
- Additional `ThemeParser` tests: `rgb()` color format, `background-color` property, `font-weight: 700`, 8-digit hex colors, and descendant-selector skipping
- Additional `HtmlToAnnotatedString` tests: non-span element wrapping, HTML entity decoding, and base-style application

### Fixed
- **`WebViewManager`: `readyDeferred` lifecycle correctness** — `readyDeferred` is now a `var`
  so it can be reset before re-initialization after `destroy()`. The WebViewClient closure now
  captures the deferred as a local variable, preventing it from completing a stale deferred on
  re-initialization. `destroy()` now cancels a pending (incomplete) deferred so callers awaiting
  the WebView in `getReadyWebView()` are not left suspended indefinitely.
- **`HighlightEngine`: Activity context leak** — `HighlightEngine` now always calls
  `context.applicationContext` before passing the context to `WebViewManager`, ensuring that a
  long-lived WebView never retains an Activity reference. `HighlightThemeProvider` and
  `rememberHighlightEngine()` also explicitly pass `applicationContext`.
- **`HighlightEngine`: incorrect JSON unescape ordering** — replaced the sequential
  `String.replace()` chain in `unescapeJsString` with a single character-by-character pass.
  The old approach applied `\\n → newline` before `\\\\ → \\`, which incorrectly converted
  `\\n` (a literal backslash + 'n' in JSON) to a newline instead of `\n`. The new
  implementation also adds support for `\\r → CR` and `\\/ → /` escape sequences.

### Changed
- **`HighlightThemeProvider`: shared engine for the whole subtree** — the provider now creates
  one `HighlightEngine` (one hidden WebView) for its entire subtree and provides it via an
  internal `LocalHighlightEngine` CompositionLocal. Previously, every `SyntaxHighlightedCode`
  and every `rememberHighlightedCode` call created its own engine. On a screen with N code
  blocks this wasted ~200 ms × N of WebView warm-up time and ~2–4 MB × N of memory.
- **`rememberHighlightEngine()`: uses shared engine when available** — when called inside
  `HighlightThemeProvider`, returns the provider's shared engine (no new WebView, no lifecycle
  management needed). Outside a provider the previous behavior is unchanged: a standalone engine
  is created and destroyed with the composable.

## [0.5.0] - 2026-04-27

### Fixed
- `HtmlToAnnotatedString.convert()` now applies the `.hljs` base text color as a full-range outer span on the resulting `AnnotatedString`. Plain tokens (identifiers, whitespace) now inherit the theme color rather than `LocalContentColor`, so `Text(text = highlighted)` works correctly without requiring a manual `color` override.

## [0.4.0] - 2026-04-26

### Added
- `HighlightTheme` now implements `equals()`/`hashCode()` based on `name` — fixes stale highlighting when using `LaunchedEffect(theme)` or `remember(theme)`
- `HighlightTheme` annotated `@Stable` — enables Compose skipping optimisation for composables that receive a theme parameter
- `rememberHighlightedCode()` and `SyntaxHighlightedCode` now accept `onHighlightComplete: ((Long) -> Unit)?` callback for performance metrics
- New `rememberHighlightedCodeBothThemes()` composable — highlights once for both light and dark themes, enabling instant theme switching

### Fixed
- `HighlightTheme.fromColorMap()` now defensively copies the provided map so later mutations don't affect the theme
- Built-in theme factories now throw on missing CSS assets instead of silently returning an unstyled theme

## [0.3.0] - 2026-04-26

### Added
- `HighlightTheme.fromColorMap()` — supply a theme from any `Map<String, SpanStyle>` (e.g. Material 3 dynamic color)
- Theme picker in sample app — switch between GitHub, Tomorrow, and Atom One theme families
- Sample app uses GitHub and GitHub Dark themes via `HighlightTheme.fromAsset()`, demonstrating user-provided custom themes

### Fixed
- `HighlightTheme.fromAsset()` now correctly throws `HighlightException.ThemeNotFound` when the asset file is missing (previously the error was silently swallowed)

## [0.2.0] - 2026-04-26

### Added
- JitPack publishing support — library available via `com.github.hossain-khan:android-compose-highlight:0.2.0`
- Comprehensive KDoc with usage examples on all public API classes
- `MODULE.md` for Dokka module-level documentation page
- Dokka API docs published to GitHub Pages via CI

## [0.1.0] - 2026-04-26

### Added
- Initial release
- `SyntaxHighlightedCode` composable with line numbers, copy button, and language badge
- `HighlightThemeProvider` for automatic light/dark theme switching via `CompositionLocal`
- `HighlightEngine` for headless/programmatic `AnnotatedString` generation
- `rememberHighlightEngine` and `rememberHighlightedCode` Compose helpers
- `CodeBlockStyle` with `Default` and `Compact` presets
- Built-in themes: Tomorrow (light), Tomorrow Night (dark), Atom One Dark, Atom One Light
- Custom theme support via `HighlightTheme.fromAsset()` and `HighlightTheme.fromCss()`
- 190+ languages via bundled Highlight.js
- GitHub Actions CI workflow (lint + unit tests + assemble)
- AndroidX Microbenchmarks for WebView JS pipeline, ThemeParser, and HtmlToAnnotatedString
