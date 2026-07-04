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
  |- ThemeParser                        CSS selector to SpanStyle parsing (recursive descent)
  |- GeneratedThemes                    Build-time precompiled color maps for the bundled built-ins
  |- HtmlToAnnotatedString / HtmlParser  Custom HTML parsing and AnnotatedString conversion
  \- escapeForJs() / unescapeJsString() and related helpers
```

**How highlighting works:**
1. `WebViewManager` loads `bridge.html` from `assets/compose-highlight/` into a hidden `WebView` on the Main thread.
2. `HighlightEngine` serializes JS calls with a `Mutex` and invokes Highlight.js through `evaluateJavascript()`.
3. `HighlightTheme` resolves its color map. The built-in themes (`tomorrow`, `tomorrowNight`, `atomOneDark`, `atomOneLight`, `github`, `githubDark`, `dracula`, `alucard`) read from precompiled `GeneratedThemes` constants and never touch the runtime parser. Custom themes loaded through `HighlightTheme.fromAsset` or `HighlightTheme.fromCss` lazily parse their CSS via `ThemeParser` on first `colorMap` access.
4. `HtmlToAnnotatedString` walks the returned HTML and applies theme styles to build a Compose `AnnotatedString`.

**Shared engine via `HighlightThemeProvider`:** one provider creates one `HighlightEngine`, which means one hidden `WebView` for the entire subtree. `rememberHighlightEngine()` reuses that shared engine inside the provider and creates a standalone engine only when used outside one.

**Asset loading:** `WebViewAssetLoader` maps `https://appassets.androidplatform.net/assets/` to the app's packaged assets. This avoids the script restrictions that apply to `file://` URLs.

## Themes

Three runtime paths produce a `HighlightTheme`. Pick the one that matches where the CSS comes from:

| Factory | Takes `Context`? | When to use |
|---|---|---|
| `HighlightTheme.tomorrow()` etc. | no | One of the bundled themes. Precompiled at build time, no runtime CSS parsing. |
| `HighlightTheme.fromAsset(context, assetPath, name)` | yes | CSS file shipped in the consumer app's `assets/` folder. Parsed lazily on first `colorMap` access. |
| `HighlightTheme.fromCss(cssText, name)` | no | CSS string fetched at runtime (network, generated, etc.). Parsed lazily. |
| `HighlightTheme.fromColorMap(name, colorMap, ...)` | no | Theme built programmatically (Material 3 dynamic color, custom palettes, etc.). No parsing. |

The built-in themes (`tomorrow`, `tomorrowNight`, `atomOneDark`, `atomOneLight`, `github`, `githubDark`, `dracula`, `alucard`) and their `remember*Theme()` Compose helpers do not require a `Context` because their color maps are baked into compiled bytecode at build time.

### Built-in theme precompilation pipeline

Built-in themes follow this build-time path:

```
Source CSS (committed)                    src/main/assets/compose-highlight/themes/*.css
        |
        v Gradle: :compose-highlight:generateThemes
        |   uses buildSrc/CssThemeParser (mirrors runtime ThemeParser)
        v
GeneratedThemes.kt (gitignored)           build/generated/source/themes/main/.../GeneratedThemes.kt
        |
        v Kotlin compile (wired via AGP variant Sources API)
        v
GeneratedThemes.class                     classes.jar inside the published AAR
```

The bundled CSS files are still shipped in the AAR so the runtime `fromAsset(...)` path keeps working for anyone who references those asset paths directly. None of the built-in factory methods reads them at runtime.

A parity test (`GeneratedThemesParityTest`) compares each precompiled map against `ThemeParser.parseAsset(...)` output for the matching CSS file, and asserts that the embedded `*_IDENTITY` hash equals what runtime `contentDigest64("asset", path)` would compute. The two parsers cannot be unified (the runtime parser depends on `androidx.compose.ui` types unavailable to a Gradle build classpath), so the parity test is the only thing keeping them in sync. If the test fails after editing the runtime parser, regenerate the file with `./gradlew :compose-highlight:generateThemes`. If it fails after editing the buildSrc parser, the buildSrc parser has drifted and that drift is the bug.

### Adding a new built-in theme

The codegen task (`buildSrc/.../GenerateThemesTask.kt`) does not auto-discover CSS files. It iterates a hardcoded `THEME_INPUTS` list, so dropping a CSS file into the assets folder ships it in the AAR but does not produce a precompiled constant or factory.

To add another built-in (replace `dracula` with the actual theme name):

1. Drop the CSS at `compose-highlight/src/main/assets/compose-highlight/themes/dracula.css`.
2. Add an entry to `THEME_INPUTS` in `buildSrc/src/main/kotlin/dev/hossain/highlight/build/GenerateThemesTask.kt`:
   ```kotlin
   "DRACULA" to "dracula.css",
   ```
3. Add the factory in `HighlightTheme.kt`:
   ```kotlin
   fun dracula(): HighlightTheme =
       HighlightTheme(
           name = "dracula",
           colorMapProvider = { GeneratedThemes.DRACULA },
           contentIdentity = GeneratedThemes.DRACULA_IDENTITY,
       )
   ```
4. Add `rememberDraculaTheme()` in `HighlightThemeProvider.kt` (the file holds both the provider composable and the `rememberXxxTheme()` factories).
5. Add a parity test entry in `GeneratedThemesParityTest.kt`.

The friction is intentional. A new built-in is an API decision, not "I dropped a file in the folder." Themes that should be available to consumers but do not warrant a built-in factory can stay as plain CSS in the assets folder and ship via `fromAsset`.

## Implementation conventions

**Public vs internal boundary:** public API lives in `ui/` plus the public engine entry points, result types, metadata types, and timing types in `engine/`. WebView management, CSS parsing, HTML conversion, and JS-string helpers stay `internal`.

**Public suspend engine methods return `Result<T>`.** Methods like `highlight()`, `highlightBothThemes()`, `highlightAuto()`, `highlightToHtml()`, `supportedLanguages()`, `getLanguage()`, and `highlightJsVersion()` report failures through `Result.failure(HighlightException(...))` instead of throwing. Add new failure cases to `HighlightException` rather than introducing ad hoc exception types.

**`android.util.Log` is banned in library code paths used by JVM tests.** Android logging calls in JVM-tested paths trigger "Method ... in android.util.Log not mocked" failures.

**Always prefer `applicationContext`.** `HighlightEngine` retains a `Context` through `WebViewManager`, and CSS-backed `HighlightTheme` factories (`fromAsset`) retain one through lazy providers. Internals defensively normalize to `applicationContext`, but call sites should still pass `context.applicationContext`. The built-in theme factories (`tomorrow()` etc.) take no `Context` since they read from precompiled constants.

**WebView work stays on the Main thread.** `WebViewManager` initialization, destruction, and JS evaluation are all dispatched to the Main thread. Theme parsing and HTML-to-`AnnotatedString` conversion run off the Main thread on `Dispatchers.Default`.

**`rememberHighlightEngine()` owns Compose lifecycle behavior.** Inside `HighlightThemeProvider`, it returns the shared engine. Outside the provider, it creates a standalone engine and destroys it with `DisposableEffect` when the composable leaves composition.

**`SyntaxHighlightedCode` needs a theme source.** Its `theme` parameter defaults to `LocalHighlightTheme.current`, which throws if there is no `HighlightThemeProvider`. Wrap usage in `HighlightThemeProvider { ... }` or pass an explicit `theme`.

**`HighlightTheme` defers work.** For CSS-backed themes (`fromAsset`, `fromCss`), CSS parsing happens on first `colorMap` access, not when the theme instance is constructed. Built-in themes (`tomorrow()` etc.) skip parsing entirely and return their precompiled `GeneratedThemes` constants on first access.

**CompositionLocals use `staticCompositionLocalOf`.** `LocalHighlightTheme`, `LocalLightHighlightTheme`, `LocalDarkHighlightTheme`, and internal `LocalHighlightEngine` all use static locals because they hold long-lived objects rather than frequently mutating reactive values.

**Assets live under `assets/compose-highlight/`.** Theme CSS files are stored under `assets/compose-highlight/themes/` to avoid collisions when the library is consumed by an app.

**Public API requires KDoc.** Dokka publishes the public API docs from KDoc, so all public classes, functions, and properties need KDoc, with usage examples on non-trivial APIs.

**Testing split:** JVM tests live in `src/test/` and should use `ThemeParser.parse(cssString)` and direct `unescapeJsString(...)` calls where possible. The opt-in `HtmlParserBenchmark` JVM microbenchmark also lives under `src/test/kotlin/dev/hossain/highlight/benchmark/` (skipped by default; enable with `-PrunBenchmark=true`). Instrumented tests and on-device benchmarks live in `src/androidTest/`. Roborazzi-driven screenshot regression tests live in `src/test/kotlin/dev/hossain/highlight/screenshot/`; see [SCREENSHOT_TESTS.md](SCREENSHOT_TESTS.md) for the workflow.

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
