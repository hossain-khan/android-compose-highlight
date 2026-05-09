# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

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
