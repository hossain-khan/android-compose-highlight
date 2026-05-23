# Module compose-highlight

A Jetpack Compose syntax-highlighting library powered by [Highlight.js](https://highlightjs.org/) running in a hidden WebView. Highlight.js produces tokenized HTML, and the library converts that output into native Compose `AnnotatedString` values instead of maintaining custom lexers or grammars.

For user-facing docs, see the [Getting Started guide](https://hossain-khan.github.io/android-compose-highlight/getting-started/) and the [generated API reference](https://hossain-khan.github.io/android-compose-highlight/api/).

## Architecture

The public surface spans a Compose UI layer and a lower-level engine layer:

```text
UI layer (public)
  |- SyntaxHighlightedCode              Compose code block
  |- HighlightThemeProvider             Shared engine + active theme for a subtree
  |- rememberHighlightEngine()          Lifecycle-aware engine access
  |- rememberHighlightedCode()          Single-theme state helper
  \- rememberHighlightedCodeBothThemes() Dual-theme state helper

Engine layer (public)
  |- HighlightEngine                    Hidden WebView orchestration
  |  |- highlight()
  |  |- highlightBothThemes()
  |  |- highlightAuto()
  |  |- highlightToHtml()
  |  |- supportedLanguages()
  |  |- getLanguage()
  |  \- highlightJsVersion()
  |- HighlightTheme                     Lazy CSS-backed theme model
  |- HighlightException                 Sealed failure hierarchy
  |- HighlightResult / HtmlHighlightResult / ThemedHighlightResult / AutoHighlightResult
  \- HighlightLanguage / HighlightLanguageInfo / HighlightTimings

Internal implementation
  |- WebViewManager                     Hidden WebView lifecycle + bridge page
  |- ThemeParser                        CSS selector to SpanStyle parsing
  |- HtmlToAnnotatedString              jsoup HTML to AnnotatedString conversion
  \- escapeForJs() / unescapeJsString() and related helpers
```

**How highlighting works:**
1. `WebViewManager` loads `bridge.html` from `assets/compose-highlight/` into a hidden `WebView` on the Main thread.
2. `HighlightEngine` serializes JS calls with a `Mutex` and invokes Highlight.js through `evaluateJavascript()`.
3. `HighlightTheme` lazily parses CSS into a selector-to-`SpanStyle` map on first use.
4. `HtmlToAnnotatedString` walks the returned HTML and applies theme styles to build a Compose `AnnotatedString`.

**Shared engine via `HighlightThemeProvider`:** one provider creates one `HighlightEngine`, which means one hidden `WebView` for the entire subtree. `rememberHighlightEngine()` reuses that shared engine inside the provider and creates a standalone engine only when used outside one.

**Asset loading:** `WebViewAssetLoader` maps `https://appassets.androidplatform.net/assets/` to the app's packaged assets. This avoids the script restrictions that apply to `file://` URLs.

## Implementation conventions

**Public vs internal boundary:** public API lives in `ui/` plus the public engine entry points, result types, metadata types, and timing types in `engine/`. WebView management, CSS parsing, HTML conversion, and JS-string helpers stay `internal`.

**Public suspend engine methods return `Result<T>`.** Methods like `highlight()`, `highlightBothThemes()`, `highlightAuto()`, `highlightToHtml()`, `supportedLanguages()`, `getLanguage()`, and `highlightJsVersion()` report failures through `Result.failure(HighlightException(...))` instead of throwing. Add new failure cases to `HighlightException` rather than introducing ad hoc exception types.

**`android.util.Log` is banned in library code paths used by JVM tests.** Android logging calls in JVM-tested paths trigger "Method ... in android.util.Log not mocked" failures.

**Always prefer `applicationContext`.** `HighlightEngine` retains a `Context` through `WebViewManager`, and `HighlightTheme` factories may retain one through lazy providers. Internals defensively normalize to `applicationContext`, but call sites should still pass `context.applicationContext`.

**WebView work stays on the Main thread.** `WebViewManager` initialization, destruction, and JS evaluation are all dispatched to the Main thread. Theme parsing and HTML-to-`AnnotatedString` conversion run off the Main thread on `Dispatchers.Default`.

**`rememberHighlightEngine()` owns Compose lifecycle behavior.** Inside `HighlightThemeProvider`, it returns the shared engine. Outside the provider, it creates a standalone engine and destroys it with `DisposableEffect` when the composable leaves composition.

**`SyntaxHighlightedCode` needs a theme source.** Its `theme` parameter defaults to `LocalHighlightTheme.current`, which throws if there is no `HighlightThemeProvider`. Wrap usage in `HighlightThemeProvider { ... }` or pass an explicit `theme`.

**`HighlightTheme` is lazy.** CSS parsing happens on first `colorMap` access, not when the theme instance is constructed.

**CompositionLocals use `staticCompositionLocalOf`.** `LocalHighlightTheme`, `LocalLightHighlightTheme`, `LocalDarkHighlightTheme`, and internal `LocalHighlightEngine` all use static locals because they hold long-lived objects rather than frequently mutating reactive values.

**Assets live under `assets/compose-highlight/`.** Theme CSS files are stored under `assets/compose-highlight/themes/` to avoid collisions when the library is consumed by an app.

**Public API requires KDoc.** Dokka publishes the public API docs from KDoc, so all public classes, functions, and properties need KDoc, with usage examples on non-trivial APIs.

**Testing split:** JVM tests live in `src/test/` and should use `ThemeParser.parse(cssString)` and direct `unescapeJsString(...)` calls where possible. Instrumented tests and benchmarks live in `src/androidTest/`.

## Contributor workflow

**Formatting and validation:**
```bash
./gradlew formatKotlin
./gradlew :compose-highlight:assembleDebug :sample:assembleDebug
./gradlew :compose-highlight:test
```

**Git workflow:** create new commits only. Do not use `git commit --amend`, `git push --force`, or `git push --force-with-lease`.

**Release preparation:** use `./scripts/prepare-release.sh <new-version>` to update `gradle.properties`, `README.md`, `sample/build.gradle.kts`, and `CHANGELOG.md` together before opening the release PR.

**Tags:** release tags must not use a `v` prefix.

**Publishing:** Maven Central publishing is a manual two-step workflow after the release PR is merged and the tag is pushed: run the publish workflow in dry-run mode first, then run it again without dry-run.

**CHANGELOG:** keep `CHANGELOG.md` updated under `[Unreleased]` for features, fixes, and breaking changes.

**Writing style:** never use the em dash character in comments, docs, KDoc, commit messages, or changelog entries. Use a regular hyphen instead.
