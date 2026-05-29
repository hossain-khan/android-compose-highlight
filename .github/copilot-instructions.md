# Copilot Instructions - android-compose-highlight

## Build, test, and lint commands

```bash
# Lint (ktlint via kotlinter) - must pass before commit
./gradlew lintKotlin

# Auto-fix formatting
./gradlew formatKotlin

# Lint markdown (markdownlint-cli) - validates CHANGELOG.md and other markdown files
npx markdownlint CHANGELOG.md

# Auto-fix markdown formatting
npx markdownlint --fix CHANGELOG.md

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

## CHANGELOG.md Markdown Formatting

**Markdown linting usage:**
- Run `npx markdownlint CHANGELOG.md` to validate all formatting rules before committing
- The repository includes `.markdownlintrc` configuration that enforces changelog-specific rules

**.markdownlintrc configuration:**
- `MD013`: Line length limit of 120 characters (vs. default 80) to accommodate detailed technical changelog entries
- `MD024`: `siblings_only: true` - Prevents duplicate section headings (e.g., multiple `### Fixed`) within the same release version. Allows the same heading across different versions, which is normal for changelogs

**CHANGELOG.md formatting rules:**
1. **No duplicate headings within a version** — Each release (`## [X.Y.Z]`) should have at most one of each section heading type (`### Fixed`, `### Added`, `### Changed`, etc.). If your changes span multiple categories, consolidate them under a single section heading with multiple bullet items. This rule is enforced by MD024 with `siblings_only: true`.
2. **Blank lines around section headings** — Every heading (`### Fixed`, `### Added`, `### Changed`, `### Performance`, `### Infrastructure`, etc.) should be surrounded by blank lines (one before, one after).
3. **Blank lines around code fences** — All code blocks (triple backticks) should be surrounded by blank lines.
4. **Line length** — Keep lines reasonably wrapped for readability; long bullet items should wrap to multiple indented lines instead of becoming hard to review. The 120-character limit allows technical explanations to stay together without excessive wrapping.
5. **Long bullet wrapping pattern** — Convert long single-line items like:

   ```
   - **Item** - [500+ char explanation with multiple concepts]
   ```

   To indented multi-line format:

   ```
   - **Item** - [Explanation line 1]
     [Explanation line 2 indented by 2 spaces]
     [Explanation line 3 indented by 2 spaces]
   ```

6. **No spaces inside code spans** — Code spans must not have spaces between backticks and content: `` `code` `` not `` ` code ` ``.
7. **Run `markdownlint` before committing** — Always verify no violations remain: `npx markdownlint CHANGELOG.md`

## Docs Changelog Sync

**Keep `docs/changelog.md` in sync with root `CHANGELOG.md`.** The docs site file serves as a curated summary for visitors - it should always contain brief highlights of the **last 5 releases** only. After each release:

1. Add a new `### X.Y.Z - Brief Title` section at the top of the "Recent highlights" block in `docs/changelog.md`
2. Extract 3-5 key bullet points from the root `CHANGELOG.md` for that release (focus on user-facing features and major fixes)
3. Keep descriptions brief (1-2 lines per item, no wrapped multi-line format)
4. Remove the oldest release entry to maintain the "last 5 releases" limit
5. No markdown linting required for `docs/changelog.md` — it's documentation, not a strict changelog

Example format:
```markdown
### 0.25.0 - New theme support

- Added 10 new built-in themes (Dracula, Solarized, etc.)
- Fixed WebView crash on Android 12 devices
- Improved theme parsing performance by 40%
```

## Architecture

The library has two layers - `engine/` (internal) and `ui/` (public):

```
SyntaxHighlightedCode   ← primary public composable
 └── rememberHighlightedCode / rememberHighlightEngine  (RememberHelpers.kt)
       └── HighlightEngine              ← public, suspend-based pipeline
             ├── WebViewManager         ← internal, owns the hidden WebView
             ├── HighlightTheme         ← public, CSS-backed theme model
             ├── ThemeParser            ← internal, CSS → Map<selector, SpanStyle>
             ├── HtmlToAnnotatedString  ← internal, jsoup → AnnotatedString
             └── unescapeJsString()     ← internal package-level fun, JSON unescape

HighlightThemeProvider  ← creates ONE shared HighlightEngine for its subtree
 └── LocalHighlightEngine (internal CompositionLocal) ← carries the shared engine
 └── LocalHighlightTheme  (public CompositionLocal)  ← carries the active theme
```

**How highlighting works end-to-end:**
1. `WebViewManager` creates a hidden (off-screen) `WebView` on the Main thread and loads `bridge.html` from `assets/compose-highlight/`. This page loads `highlight.min.js` and exposes `highlightCode(code, lang)`.
2. `HighlightEngine` serializes calls with a `Mutex` and calls `evaluateJavascript()` to invoke `highlightCode`, getting back HTML with `<span class="hljs-*">` tokens.
3. `ThemeParser` lazily parses a Highlight.js CSS file into a `Map<String, SpanStyle>` (selector → style), cached per `HighlightTheme` instance.
4. `HtmlToAnnotatedString` uses jsoup to walk the HTML and applies the theme's `SpanStyle` map to produce a Compose `AnnotatedString`.

**Shared engine via `HighlightThemeProvider`:** The provider creates a single `HighlightEngine` (one hidden WebView) for its entire subtree and exposes it via the internal `LocalHighlightEngine` CompositionLocal. `rememberHighlightEngine()` reads it when inside the provider - no extra WebView is created. Outside the provider, `rememberHighlightEngine()` creates a standalone engine that it destroys itself via `DisposableEffect`. This means N code blocks inside a provider share 1 WebView instead of N.

**Why `https://appassets.androidplatform.net`:** `WebViewAssetLoader` intercepts requests to this reserved fake domain and maps `/assets/` to the app's `assets/` folder. This is required because `file://` URLs block `<script>` execution via Same-Origin Policy.

## Key conventions

**Public vs internal:** Only `ui/` and `engine/{HighlightEngine,HighlightTheme,HighlightException}.kt` are public API. All `engine/` helpers (`WebViewManager`, `ThemeParser`, `HtmlToAnnotatedString`, `unescapeJsString`) are `internal`. Note that `unescapeJsString` is a package-level `internal fun` (not a member of `HighlightEngine`) so it can be tested directly from JVM unit tests without a real `Context`.

**`android.util.Log` is banned from the library.** Any `Log.*` call in code paths executed by JVM unit tests causes `RuntimeException: Method d in android.util.Log not mocked`. Remove all debug logging before committing.

**All `HighlightEngine` results use `Result<T>`.** Never throw from public engine methods; wrap failures in `Result.failure(HighlightException(...))`. `HighlightException` is a sealed class - add new variants there rather than throwing raw exceptions.

**Always use `applicationContext`.** Never pass an Activity `Context` to `HighlightEngine` or `HighlightTheme` factory functions - both hold the context beyond the Activity's lifecycle (the engine in `WebViewManager`, the theme in its `colorMapProvider` lambda). Always call `context.applicationContext` at the call site.

**WebView must run on the Main thread.** `WebViewManager.initialize()` and `destroy()` dispatch to `Dispatchers.Main` and `Handler(Looper.getMainLooper())` respectively. Never call WebView APIs off the Main thread.

**`rememberHighlightEngine()` for lifecycle management.** In Compose, always use `rememberHighlightEngine()` (not bare `HighlightEngine(context)`). When called inside `HighlightThemeProvider`, it returns the provider's shared engine (no extra WebView, no extra lifecycle handling). When called outside a provider, it creates a standalone engine and destroys it via `DisposableEffect` when the composable leaves composition.

**`SyntaxHighlightedCode` requires a theme.** Its `theme` parameter defaults to `LocalHighlightTheme.current`, which throws if no `HighlightThemeProvider` ancestor exists. Always wrap usage in `HighlightThemeProvider { }` or pass an explicit `theme =` argument.

**`HighlightTheme` is lazy.** CSS parsing happens on first use of `colorMap`, not at factory-call time. This means `fromAsset()` errors surface when the theme is first applied, not when the factory is called.

**Formatting:** ktlint via `org.jmailen.kotlinter`. The `.editorconfig` suppresses the function-naming rule for `@Composable`-annotated functions (`ktlint_function_naming_ignore_when_annotated_with = Composable`). Run `./gradlew formatKotlin` before committing.

**JVM unit tests vs instrumented tests:** `src/test/` contains JVM tests (fast, no device needed). Use `ThemeParser.parse(cssString)` for theme tests and call `unescapeJsString(...)` directly for unescape tests - both work without Android mocks. Use [Google Truth](https://github.com/google/truth) (`com.google.truth:truth`) for assertions in new tests. `src/androidTest/` contains instrumented tests (`HighlightEngineTest`) and microbenchmarks (`benchmark/`) that require a connected device and use `BenchmarkRule`.

**Asset path convention:** All library assets live under `assets/compose-highlight/` to avoid collisions when the library is consumed. CSS themes go in `assets/compose-highlight/themes/`.

**Keep CHANGELOG.md up to date.** For every PR or commit that adds a feature, fixes a bug, or makes a breaking change, add an entry under the relevant `[Unreleased]` section in `CHANGELOG.md` at the repo root. When cutting a release, rename `[Unreleased]` to the version number with the date.

**KDoc is required on all public API.** Dokka API docs are generated from KDoc and published to GitHub Pages (`.github/workflows/docs.yml`). Every public class, function, and property in `ui/` and the public `engine/` classes must have KDoc. Include at least one usage example (triple-backtick code block) on non-trivial classes and composables. Internal classes do not need KDoc but benefit from it.

**Git workflow - always create new commits.** Never use `git commit --amend`, `git push --force`, `git push --force-with-lease`, or similar rewriting operations. Always create a new commit for any changes. This keeps commit history clean, preserves attribution, and prevents accidental data loss. If changes are needed after pushing, create a new commit with a descriptive message (e.g., "fix: address code review feedback in X").

**Before every commit - verify stability.** Run the following three tasks and ensure they all pass:
```bash
./gradlew formatKotlin                          # auto-fix formatting
./gradlew :compose-highlight:assembleDebug :sample:assembleDebug  # both must build
./gradlew :compose-highlight:test               # all JVM unit tests must pass
```
Do not commit if any of these fail.

**Git tags must not use a `v` prefix.** Use `0.3.0`, not `v0.3.0`. Maven Central uses the tag as the dependency version (via `-PVERSION_NAME=<tag>` in the publish workflow), so the version string consumers write in their `build.gradle.kts` matches the tag exactly.

**Before tagging a release - use the release script to update all version references atomically:**
```bash
./scripts/prepare-release.sh <new-version>
# Example: ./scripts/prepare-release.sh 0.18.0
```
This script updates all four required files in one step:
- `gradle.properties` - `VERSION_NAME`
- `README.md` - dependency snippet
- `sample/build.gradle.kts` - `versionName` and auto-incremented `versionCode`
- `CHANGELOG.md` - `[Unreleased]` renamed to `[X.Y.Z] - YYYY-MM-DD`

Never update these files manually one-by-one - always use the script to avoid missing files.

After running the script, create a release branch, run all checks, commit, push, and open a PR into `main` (direct pushes to `main` are blocked by branch protection):
```bash
git checkout -b release/<new-version>
./gradlew formatKotlin :compose-highlight:assembleDebug :sample:assembleDebug :compose-highlight:test
git add -A && git commit -m "chore: prepare release <new-version>"
git push -u origin release/<new-version>
gh pr create --title "chore: prepare release <new-version>" --base main
```

Only create the git tag **after the release PR is merged** and `main` is pulled:
```bash
git checkout main && git pull
git tag <new-version> && git push origin <new-version>
```

**Publishing to Maven Central is a manual two-step workflow - it is NOT triggered automatically by pushing a tag.** After tagging:
1. Manually trigger the publish GitHub Actions workflow in **dry-run mode** first and verify it passes.
2. Only if the dry run succeeds, trigger the workflow again **without dry-run** to actually publish to Maven Central.

Never tell the user "the publish workflow will trigger automatically" - it won't.

**Release notes format** - when users ask for release notes, provide them in markdown format. Example:
```markdown
## v0.22.1

### Bug Fixes
- Fixed resource leak: InputStream in ThemeParser now properly closed when loading CSS assets
- Fixed scroll position persistence when code snippet changes
- Removed redundant code in HtmlToAnnotatedString

### Performance
- Improved recomposition efficiency in LazyColumn scenarios by stabilizing internal lambda instances

**Dependency:** `dev.hossain:compose-highlight:0.22.1`
```

**Automated Release APK workflow** - when a git tag is pushed, the `.github/workflows/android-release.yml` job automatically runs and attaches the Release APK to the GitHub Release. No need to build APK manually anymore.

**Dependency coordinates (Maven Central):**
```
dev.hossain:compose-highlight:<version>
```

**Updating bundled highlight.js themes in the sample app** - to refresh the 256 `.min.css` theme files under `sample/src/main/assets/themes/` (e.g. when upgrading highlight.js), follow the steps in [`resources/updating-hljs-themes.md`](../resources/updating-hljs-themes.md).

**Writing style - never use the em dash `-` character** in commit messages, code comments, KDoc, CHANGELOG entries, or any other text. Use a regular hyphen/minus `-` instead.

