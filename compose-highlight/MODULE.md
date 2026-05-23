# Module compose-highlight

A Jetpack Compose library for beautiful syntax highlighting powered by [Highlight.js](https://highlightjs.org/) running in a hidden WebView. Tokenized HTML is converted to native Compose `AnnotatedString` — no custom lexers, no grammars to maintain: 190+ languages out of the box.

For detailed API documentation and usage examples, see the [generated API docs](../docs/api/) and [getting started guide](../docs/getting-started.md).

## Architecture

Public API is split across two layers:

```
UI Layer (public)
  ├── SyntaxHighlightedCode        ← Compose wrapper around HighlightEngine
  ├── HighlightThemeProvider       ← Provides shared engine + active theme to subtree
  ├── rememberHighlightEngine()    ← Lifecycle-aware engine factory (uses provider's engine when available)
  ├── rememberHighlightedCode()    ← State holder for single-language highlighting
  └── rememberHighlightedCodeBothThemes()  ← State holder for dual-theme highlighting

Engine Layer (public)
  ├── HighlightEngine              ← Main entry point, owns WebView and serialization
  ├── HighlightTheme               ← Lazy CSS-backed color map model
  ├── HighlightResult / HtmlHighlightResult / ThemedHighlightResult  ← Result types
  └── HighlightLanguage / HighlightLanguageInfo  ← Helper lookups

Internal (compose-highlight package-internal)
  ├── WebViewManager               ← Hidden WebView lifecycle, JS bridge
  ├── ThemeParser                  ← CSS selector → Map<String, SpanStyle> parsing
  ├── HtmlToAnnotatedString        ← jsoup-based HTML → AnnotatedString conversion
  └── Helper functions (unescapeJsString, escapeForJs, etc.)
```

**WebView threading:** All `WebView` APIs run on the Main thread. `HighlightEngine` serializes concurrent highlight calls via an internal `Mutex` and dispatches theme-parsing / HTML-conversion work to `Dispatchers.Default` off the Main thread.

**Shared engine via `HighlightThemeProvider`:** Creates one `HighlightEngine` (one hidden WebView) for its entire subtree. `rememberHighlightEngine()` detects the provider via `LocalHighlightEngine` and returns the shared engine if present; otherwise creates a standalone engine with automatic lifecycle cleanup via `DisposableEffect`.

**Asset loading:** `WebViewAssetLoader` maps requests to `https://appassets.androidplatform.net/assets/` to the app's `assets/` folder, bypassing Same-Origin Policy restrictions on `file://` URLs and enabling `<script>` execution.

## Implementation conventions

**Public vs internal API boundary:** Only `ui/*`, `engine/HighlightEngine.kt`, `engine/HighlightTheme.kt`, and `engine/HighlightException.kt` are public. All other engine helpers (`WebViewManager`, `ThemeParser`, `HtmlToAnnotatedString`, `unescapeJsString`, etc.) are `internal`. Note: `unescapeJsString` is a package-level `internal fun` so it can be tested directly in JVM unit tests without mocking Android.

**`android.util.Log` is banned.** Any `Log.*` call in code that runs in JVM tests causes `RuntimeException: Method d in android.util.Log not mocked`. Use structured logging via test assertions instead.

**Results use `Result<T>`, never throw from public methods.** All public engine methods return `Result<T>`. Failures wrap `HighlightException` (a sealed class). Add new exception variants to the sealed class rather than throwing arbitrary exceptions.

**Application context only.** Both `HighlightEngine` and `HighlightTheme` hold contexts beyond Activity lifecycle. Always pass `context.applicationContext` from call sites - never Activity contexts. Internals defensively normalize any provided context to `applicationContext`.

**Main thread threading model.** All WebView APIs run on the Main thread via `Dispatchers.Main` callbacks. `HighlightEngine` serializes concurrent highlight calls via `Mutex` and offloads theme parsing / HTML conversion to `Dispatchers.Default`.

**Lazy theme initialization.** `HighlightTheme.colorMap` is backed by `lazy {}`. CSS parsing and any asset I/O happen on first access to `colorMap`, not at factory time. Errors surface on first use, not theme construction.

**Static CompositionLocals.** `LocalHighlightTheme`, `LocalLightHighlightTheme`, `LocalDarkHighlightTheme`, and internal `LocalHighlightEngine` use `staticCompositionLocalOf` (not `compositionLocalOf`) because they carry stable objects (themes, engine) that do not change within their provider subtree. This avoids unnecessary recomposition tracking.

**Asset path structure.** All library assets live under `assets/compose-highlight/` to avoid collisions. CSS themes are in `assets/compose-highlight/themes/`.

**Formatting.** ktlint via `org.jmailen.kotlinter`. The `.editorconfig` suppresses the function-naming rule for composables. Run `./gradlew formatKotlin` before committing.

**KDoc on public API.** Every public class, function, and property must have KDoc. Non-trivial classes and composables should include usage examples in triple-backtick blocks. Dokka generates published API docs from KDoc to GitHub Pages. Internal classes benefit from KDoc but are not required.

**Test structure.** JVM unit tests in `src/test/` run fast without a device. Use `ThemeParser.parse(cssString)` for theme tests and call `unescapeJsString(...)` directly - both work without Android mocks. Use [Google Truth](https://github.com/google/truth) for assertions. Instrumented tests in `src/androidTest/` require a connected device; includes `HighlightEngineTest` and microbenchmarks using `BenchmarkRule`.

## Git workflow and release process

**Always create new commits.** Never use `git commit --amend`, `git push --force`, or similar rewriting operations. Always create a new commit for changes. This preserves attribution and clean history. If changes are needed after pushing, create a new commit with a descriptive message (e.g., "fix: address code review feedback in X").

**Before every commit - verify stability.** Run the following and ensure all pass:
```bash
./gradlew formatKotlin
./gradlew :compose-highlight:assembleDebug :sample:assembleDebug
./gradlew :compose-highlight:test
```
Do not commit if any fail.

**Git tags must not use `v` prefix.** Use `0.3.0`, not `v0.3.0`. Maven Central uses the tag as the dependency version, so the version consumers write matches the tag exactly.

**Release process uses a script.** Before tagging, use the release script to update version references atomically:
```bash
./scripts/prepare-release.sh <new-version>
# Example: ./scripts/prepare-release.sh 0.18.0
```
This script updates `gradle.properties`, `README.md`, `sample/build.gradle.kts`, and `CHANGELOG.md` in one step. Never update these manually one-by-one - you will miss files.

**Release PR workflow:** Create a release branch, run all checks, commit, push, and open a PR into `main`:
```bash
git checkout -b release/<new-version>
./gradlew formatKotlin :compose-highlight:assembleDebug :sample:assembleDebug :compose-highlight:test
git add -A && git commit -m "chore: prepare release <new-version>"
git push -u origin release/<new-version>
gh pr create --title "chore: prepare release <new-version>" --base main
```

**Publishing is a manual two-step after the PR is merged.** Only after the release PR is merged into `main`:
```bash
git checkout main && git pull
git tag <new-version> && git push origin <new-version>
```
Then manually trigger the GitHub Actions publish workflow in **dry-run mode first**, then again **without dry-run** to actually publish to Maven Central.

**CHANGELOG.md must stay current.** For every PR/commit that adds a feature, fixes a bug, or makes a breaking change, add an entry under `[Unreleased]` in `CHANGELOG.md`. When releasing, rename the `[Unreleased]` section to the version number with today's date.

**No em dashes in text.** Write `-` (regular hyphen) instead of `-` (em dash) in commit messages, code comments, KDoc, and CHANGELOG entries.
