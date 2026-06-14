# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Fixed

- **Fixed 8-digit CSS hex color parsing order** - `ThemeParser` now interprets `#RRGGBBAA`
  as specified by CSS Color Module Level 4, instead of the Android-style `#AARRGGBB`
  order. Fixes #321.
- **Fixed live editor state reset on rotation in sample app** - `LiveEditorSection` now uses
  `rememberSaveable` with `TextFieldValue.Saver`, so typed content survives configuration
  changes and stays scoped to the selected language. Fixes #325.

## [0.30.2] - 2026-06-13

### Removed

- **Removed `treeWalk` timing property from `HighlightTimings`** - Since the SAX-style
  single-pass parser optimization eliminates the separate tree walk phase, the `treeWalk`
  metric has been removed entirely from the public timing breakdown model.

## [0.30.1] - 2026-06-13

### Performance

- **Optimized HTML-to-AnnotatedString pipeline with SAX-style single-pass parsing** -
  Replaced the two-phase approach (parse HTML into intermediate `CustomNode` tree, then walk
  the tree to build `AnnotatedString`) with SAX-style `parseAndBuild`/`parseAndBuildBoth`
  functions that parse HTML and emit spans in a single pass, eliminating all intermediate tree
  allocations. Combined with substring avoidance in tag parsing, in-place attribute extraction,
  lazy entity decoding, and allocation-free numeric character reference parsing.
  Benchmarks show **29-57% faster single-theme** and **5-39% faster dual-theme** conversions
  across all six real-world fixtures (100 warmup, 50 measurement iterations).

## [0.30.0] - 2026-06-13

### Changed

- **Replaced Jsoup with a lightweight custom HTML parser** - Replaced the JVM-only Jsoup dependency
  with a single-pass pure-Kotlin HTML tokenizer (`HtmlParser.kt`) scoped to the narrow subset of
  HTML that highlight.js emits (nested `<span class="hljs-*">` elements, comments, text, and the
  six common named entities plus numeric character references). The HTML-to-`AnnotatedString`
  walker (`HtmlToAnnotatedString`) was updated to use the new lightweight node tree.

  **Why this matters for downstream apps**:
  - **Removes the Jsoup transitive dependency** (`org.jsoup:jsoup:1.22.2`) from the published POM.
    Consumers no longer pull the ~501 KB unshrunk jar (329 classes) at build or runtime.
  - **Removes 4 R8/ProGuard `-keep` rules** from `consumer-rules.pro` (`org.jsoup.Jsoup`,
    `org.jsoup.parser.**`, `org.jsoup.nodes.**`, `org.jsoup.select.**`). One fewer transitive
    library for downstream R8/ProGuard pipelines to analyze; one fewer source of shrinker bugs.
  - **Sample APK is 128.7 KB smaller (-5.49%)** post-R8 — measured by diffing the published
    sample APKs for 0.29.0 vs 0.30.0 with `diffuse`. The dex shrunk by 268 KB uncompressed
    (-271 classes, -2,298 methods); other APK sections (resources, manifest, assets) are
    byte-identical. The library AAR itself grows by ~7 KB (546 KB → 553 KB) because the
    parser code now ships in the module — but the AAR never bundled Jsoup, so the net
    consumer-side footprint is meaningfully lighter.
  - **Prepares the codebase for Kotlin Multiplatform (KMP)** - Jsoup is JVM-only; the new
    parser is pure Kotlin with no JVM-specific APIs.

  **Performance** (JVM benchmark on the six real-world fixtures, median of 3 runs at the `min`
  data point):
  - Dual-theme `convertBothThemes` (the path the engine uses) is **1.27×-1.97× faster** across
    all six fixtures.
  - Single-theme `convert` is mostly faster (1.27×-1.98× on C#/Go/C); SQL/Kotlin/Rust were
    within run-to-run noise on this hardware.

  **Behavioral change** - the new parser uses HTML5-style implicit-close recovery for malformed
  inputs (e.g. `<span><b>x</span>`), matching what Jsoup did. highlight.js does not emit such
  inputs; this only affects callers who feed non-hljs HTML directly to internal API.

### Added

- **Real-world language HTML parsing test coverage** - Added comprehensive test coverage for
  Kotlin, C, Rust, Go, C#, and SQL source code snippets taken from popular open-source libraries.
- **JVM microbenchmark for `HtmlToAnnotatedString`** - `HtmlParserBenchmark` in
  `src/test/kotlin/.../benchmark/`, opt-in via `-PrunBenchmark=true`, runs 5 warmup + 30
  measurement iterations against the six real-world fixtures and emits a JSON report at
  `compose-highlight/build/reports/benchmarks/html-parser-baseline-<epoch-ms>.json`. Skipped by
  default, so `:test` stays fast.

## [0.29.0] - 2026-06-12

### Added

- **Tab and auto-indent support in SyntaxHighlightedTextEditor** - Added `indentation`, `autoIndentEnabled`,
  and `tabKeyInterceptionEnabled` parameters. Features include Tab key hardware interception to insert spaces, and
  auto-indentation copy of the previous line's leading whitespace on hardware/virtual keyboards (e.g. Gboard).

### Fixed

- **Arrow key focus escape in SyntaxHighlightedTextEditor** - Prevented arrow keys (Up, Down, Left, Right) from
  navigating focus out of the code editor.

## [0.28.0] - 2026-06-06

### Changed

- **Default copy icon replaced with vector drawable** - `SyntaxHighlightedCodeDefaults.CopyButton`
  now renders a vector copy icon (`copy_code_block.xml`) instead of the `⧉` Unicode text character.
  The icon size scales proportionally to the button `size` (60 % of the touch target) so callers
  who customize the button size get a matching icon automatically. Tinting works correctly via
  `LocalContentColor`.

### Added

- **Info banner in sample app Languages tab** - the first item in the Languages tab now shows an
  info banner with the library name, version number (read from `gradle.properties` via
  `BuildConfig.LIB_VERSION_NAME`), a brief description, and an "Open Docs" button that launches
  the documentation site via `LocalUriHandler`.

- **`LIB_VERSION_NAME` build config field** - sample app now exposes the library version from
  `gradle.properties` as `BuildConfig.LIB_VERSION_NAME`, keeping the displayed version in sync
  with the published artifact version without manual updates.

### Sample app

- **`TogglesSection` updated** - the custom copyButton example now uses `copy_content_alt_rounded`
  vector icon. Added a new demo showing a large 56.dp copy button with the same icon.
- **`EngineInfoSection` polished** - replaced `OutlinedTextField` with `OutlinedCard` wrapping a
  borderless `TextField` so the search box outline matches the version and language count cards.
  Language chips now use vector icons (`check_24dp` for copied state, `copy_content_alt_rounded`
  for default) at 18.dp instead of text characters.

### Docs

- **KDoc updated for `CopyButton`** - removed reference to `⧉` text icon, updated to describe
  the vector icon with proportional scaling behavior.

## [0.27.0] - 2026-06-02

### Changed (Breaking - experimental API)

- **Editor ships code-friendly `keyboardOptions` by default** - `SyntaxHighlightedTextEditor`
  now defaults its `keyboardOptions` to `SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions`
  (autocorrect off, autocapitalization off, Ascii keyboard). Existing callers who relied on the
  previous behavior - which inherited `BasicTextField`'s `KeyboardOptions.Default` and left
  autocorrect on - will see identifiers no longer get rewritten by the IME as the user types.
  This is the intended behavior for a source-code editor; the `KeyboardOptions.Default`
  behavior is recoverable by passing it explicitly. Marked `@ExperimentalHighlightApi`.

- **Editor `onHighlightComplete` callback shape aligned with `rememberHighlightedCode`** -
  `SyntaxHighlightedTextEditor` and `rememberSyntaxHighlightedEditorValue` now invoke
  `onHighlightComplete` with a `HighlightResult` instead of a bare `AnnotatedString`. Callers
  gain access to `spanCount`, `language`, `durationMs`, and the per-layer `HighlightTimings`
  breakdown - the same shape `rememberHighlightedCode` already exposes - so observability is
  consistent across read-only and editable code surfaces. Callers must update lambdas from
  `{ annotated -> use(annotated) }` to `{ result -> use(result.annotated) }`. Both APIs are
  marked `@ExperimentalHighlightApi`, so no stable surface is broken. Fixes #277.

### Added

- **`SyntaxHighlightedTextEditor.keyboardOptions` parameter** - new `keyboardOptions:
  KeyboardOptions` parameter forwarded to the underlying `BasicTextField`. Defaults to the new
  `SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions` constant. Override at the call site
  to customise IME action or keyboard type while keeping autocorrect/autocapitalization off:
  `keyboardOptions = SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions.copy(imeAction =
  ImeAction.Search)`. Part of #265.

- **`SyntaxHighlightedTextEditor.cursorBrush` parameter** - new `cursorBrush: Brush?` parameter
  forwarded to the underlying `BasicTextField`. Defaults to `null`, in which case the cursor is
  painted using a `SolidColor` derived from the theme's `defaultTextColor` - so the cursor is
  visible on both light and dark themes out of the box. `BasicTextField`'s own default
  (`SolidColor(Color.Black)`) is invisible on dark themes; passing an explicit brush overrides
  the theme-derived default. Part of #265.

- **`SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions` constant** - pre-allocated
  `KeyboardOptions` tuned for source-code input (autocorrect off, autocapitalization off,
  `KeyboardType.Ascii`). Used as the default for the editor's new `keyboardOptions` parameter.
  Marked `@ExperimentalHighlightApi`.

### Fixed

- **`ThemeParser` silent overload no longer swallows parser bugs** - the internal
  `ThemeParser.parse(context, cssAssetPath)` overload (used by benchmarks and a single test)
  previously caught **any** `Exception`, conflating "missing asset" with "parser regression".
  Narrowed to `catch (IOException)` so I/O errors still return the documented empty map, but
  any other exception (e.g. a parser bug raising `IllegalStateException`) propagates instead
  of masquerading as `ThemeNotFound` at the `HighlightTheme.fromAsset` layer. No production
  behavior change - production goes through `ThemeParser.parseAsset` which already throws
  on I/O. Closes #275.

### Performance

- **`ThemeParser` regex hoisting** - the `[\w-]+: [^;]+` declaration matcher and the `\s+`
  whitespace splitter used in modern `rgb(R G B)` parsing were being allocated as fresh
  `Regex` instances on every CSS rule and every space-separated color value. Hoisted both to
  module-level `private val` constants (matching the existing `HLJS_SELECTOR_REGEX` and
  `PSEUDO_CLASS_REGEX` pattern in the same file). Pure refactor; same regex semantics, fewer
  allocations on first-highlight latency for asset/CSS-backed themes. Closes #276.

### Tests

- **`cursorBrush` resolution test coverage** - extracted the editor's null-fallback resolver to
  an `internal fun resolveEditorCursorBrush(cursorBrush, textColor)` so the contract can be
  pinned by JVM unit tests instead of needing a Compose UI harness. Five tests cover: null
  falls back to `SolidColor(textColor)` on both light and dark themes (fixing the
  `BasicTextField` default of `SolidColor(Color.Black)` that's invisible on dark themes),
  explicit `SolidColor` is returned verbatim, explicit `Brush.horizontalGradient(...)` passes
  through unchanged, and `Color.Unspecified` falls through without crashing. No behavior change.

### Internal

- **`WebViewManager` threading invariants documented and test-covered** - the manager's class
  KDoc now spells out which methods run on Main, where each field is written from, and why the
  `DisposableEffect`-owned lifecycle structurally prevents the racy-on-paper sequences from
  happening in practice. New `WebViewManagerThreadingTest` (JVM/Robolectric) pins three
  invariants: destroy-during-init does not leave the next initialize hung, concurrent
  initialize calls create exactly one WebView, and a getReadyWebView await resumes with
  cancellation when destroy fires (never hangs). No functional change. Closes #278.

- **Sample app's Live Editor section now demos the Tier 1 editor params from #265** - adds a
  "Custom cursor color" toggle chip that flips `cursorBrush` between `null` (theme-derived
  default) and `SolidColor(MaterialTheme.colorScheme.primary)`. The editor's `keyboardOptions`
  is now set to `CodeKeyboardOptions.copy(imeAction = ImeAction.Done)`, demonstrating the
  recommended `.copy(...)` pattern for customising one IME field while keeping autocorrect /
  autocapitalization off. Sample-only change.

## [0.26.0] - 2026-06-01

### Changed (Breaking)

- **Library file layout reorganized for stable release** - implementation-only types now
  live in `.internal` subpackages, and a few grab-bag files were split so each public
  symbol is in a file matching its name. No public API changed package; this affects only
  in-tree references to `internal`-modifier symbols (which by definition could not be
  used by external consumers across module boundaries). Mechanical reorganization with
  no behavior changes.

  **Engine** (`dev.hossain.highlight.engine.*`):
  - `WebViewManager`, `HtmlToAnnotatedString`, `ThemeParser` moved to
    `dev.hossain.highlight.engine.internal.*`. Extends the previous "Restrict engine
    helper visibility" change by making the implementation-detail nature explicit in
    the package path, and by suppressing these from the published Dokka site.
  - `escapeForJs`, `unescapeJsString` extracted to
    `dev.hossain.highlight.engine.internal.JsStringEscape.kt`.
  - `withEngineErrorHandling`, `withHtmlParsingErrorHandling` extracted to
    `dev.hossain.highlight.engine.internal.EngineErrorHandling.kt`.
  - `ThemedHighlightResult` extracted from the bottom of `HighlightEngine.kt` to its
    own `engine/ThemedHighlightResult.kt` file (matching the file-per-result-type
    convention used by `HighlightResult`, `HtmlHighlightResult`, `AutoHighlightResult`).
  - `HighlightLanguageInfo` merged into `engine/HighlightLanguage.kt`; the standalone
    file is gone. Public type, same package.
  - `HighlightEngine.kt` shrinks from 1023 to 795 lines.

  **UI** (`dev.hossain.highlight.ui.*`):
  - `LocalHighlightEngine` (internal `CompositionLocal`) moved to
    `dev.hossain.highlight.ui.internal.LocalHighlightEngine.kt`.
  - `applySnapshotSpans` (internal editor span-transfer algorithm) moved to
    `dev.hossain.highlight.ui.internal.ApplySnapshotSpans.kt`.
  - `RememberHelpers.kt` split into `RememberHighlightEngine.kt`,
    `RememberHighlightedCode.kt` (which also holds `rememberHighlightedCodeBothThemes`),
    and `RememberSyntaxHighlightedEditorValue.kt`.
  - The four `rememberXxxTheme()` factories merged into `HighlightThemeProvider.kt`;
    the standalone `HighlightThemeComposables.kt` file is gone.

  **Dokka** (`compose-highlight/build.gradle.kts`): now suppresses `*.internal*`
  packages from the published API site via `perPackageOption`.

### Fixed

- **`HighlightTheme` content identity now uses full SHA-256 digest** - replaced 64-bit
  truncated identity (`Long`) with full 256-bit identity (4 x `Long`) for `equals` and
  `hashCode` inputs. This removes practical collision risk that could suppress Compose
  re-highlight triggers (`remember` / `LaunchedEffect`) when two distinct themes shared
  the same 64-bit prefix. Updated build-time theme generator to emit full digest identity
  arrays for built-in themes so runtime and generated identities stay parity-aligned.
  Fixes #263.

### Added

- **Enhanced `HljsSelectors` with all official hljs scopes** - reorganized constants by official
  highlight.js categories (General purpose, Title subscopes, Meta, Tags/attributes, CSS selectors,
  Text markup, Templates, Diff, Other) and added 22 new selectors missing from the original
  extraction: `PUNCTUATION`, `PROPERTY`, `CHAR`, `CHAR_ESCAPE`, `SUBST`, `VARIABLE_LANGUAGE`,
  `VARIABLE_CONSTANT`, `TITLE_CLASS`, `TITLE_CLASS_INHERITED`, `TITLE_FUNCTION_INVOKE`,
  `META_KEYWORD`, `META_STRING`, `META_PROMPT`, `SELECTOR_ID`, `SELECTOR_CLASS`,
  `SELECTOR_ATTR`, `SELECTOR_PSEUDO`, `CODE`, `TEMPLATE_TAG`, `TEMPLATE_VARIABLE`, `ATRULE`,
  `DOCTAG`. Added `@see` links to official hljs CSS Classes Reference and Theme Guide.
  Documented which scopes are newer/not universal per official docs. Made `HljsSelectors`
  public so consuming apps can use the constants when building color maps for
  `HighlightTheme.fromColorMap`. Closes #261.

### Performance

- **`escapeForJs` single-pass rewrite** - replaced chained `String.replace` calls with a
  single character-by-character `StringBuilder` pass in `JsStringEscape.kt`. This avoids
  repeated full-string rescans and intermediate allocations in the editor highlight loop.
  Removed the now-unnecessary `CONTROL_CHAR_REGEX`. Added boundary test coverage for mixed
  ASCII + emoji pass-through. Fixes #266.

### Tests

- **`HljsSelectorsParserTest`** - New JVM unit test class verifying that `ThemeParser` correctly
  parses every selector defined in `HljsSelectors`. Tests cover color, background, fontWeight,
  and fontStyle extraction for each selector, plus compound selector tests for comma-separated
  rules. Organized by official hljs categories matching `HljsSelectors` structure.

## [0.25.0] - 2026-05-30

### Fixed

- **`SyntaxHighlightedTextEditor` / `rememberSyntaxHighlightedEditorValue` - preserve span
  suffix tail on three-region mid-text edits** - when a single span covered the unchanged
  prefix, the edited region, AND the unchanged suffix (common for multi-line strings, block
  comments, and template literals), the algorithm clipped the span to the prefix end and
  silently dropped the unchanged suffix tail. During the debounce window the trailing portion
  of the token visibly lost its colour, even though the suffix characters and span coordinates
  were recoverable by shifting with delta. Fixed by adding a fourth `when` branch that emits
  both unchanged tails: the prefix tail at original coordinates and the suffix tail shifted by
  delta. Two new unit tests in `ApplySnapshotSpansTest` lock down the new branch (one with
  delta=0, one with delta+2). The existing
  `insert in middle - span straddling edit point is clipped to prefix` test was renamed to
  `insert in middle - span straddling edit keeps both unchanged tails` and updated to assert
  the new (correct) behaviour, plus a new
  `insert in middle - span ending at the edit point is clipped to prefix only` test keeps the
  prefix-only-clip branch covered.

- **Editor `debounceMs` KDoc was misleading** - both `SyntaxHighlightedTextEditor` and
  `rememberSyntaxHighlightedEditorValue` claimed *"the new delay is picked up on the next
  highlight cycle without restarting the effect."* In practice the `LaunchedEffect` is keyed
  on `value.text`, so it restarts on every keystroke; `delay(currentDebounceMs)` reads the
  value at suspension and a mid-sleep change is captured for that delay. KDoc corrected to:
  *"If `debounceMs` changes, the new value is used on the next keystroke. The currently running
  debounce window is unaffected."*

### Added

- **`onError` callback on `SyntaxHighlightedTextEditor` and `rememberSyntaxHighlightedEditorValue`** -
  optional `((HighlightException) -> Unit)?` parameter, defaults to `null`. Mirrors the shape
  already used by `rememberHighlightedCode` and `rememberHighlightedCodeBothThemes`. The editor
  still falls back to plain text on failure regardless of whether the callback is set; this is
  purely observational. Callers can use it to log failures, surface a snackbar, or record
  analytics. Wired with `rememberUpdatedState` so a changed lambda is invoked without
  restarting the effect. Unit-tested via a new `RememberSyntaxHighlightedEditorValueRobolectricTest`
  that drives the `ShadowWebView` callback with `null` to deterministically trigger
  `HighlightException.JsExecutionFailed`.

### Tests

- **Seven new unit tests for `applySnapshotSpans`** covering edges that were previously
  untested: prepend at start (suffix branch with delta>0), identical text (no-op),
  both-empty strings, span starting in changed region extending into suffix (deliberately
  dropped because the start position is invalidated), zero-width span at the prefix
  boundary (preserved), zero-width span strictly inside the changed region (dropped), and
  the prefix+suffix overlap clamp inside `applySnapshotSpans`. Test count for this file
  went from 13 to 20.
- **`SyntaxHighlightedTextEditorRobolectricTest`** - new Robolectric test class that mirrors
  the parity layer `SyntaxHighlightedCodeRobolectricTest` provides for the read-only viewer.
  Four tests: `LocalInspectionMode` short-circuit (no `LaunchedEffect` fires in `@Preview`),
  test tag on the outer `Surface`, no-provider error throwing, and `onError` forwarding from
  the editor to the helper (asserts the editor's `onError = onError` wiring is intact, which
  the existing `RememberSyntaxHighlightedEditorValueRobolectricTest` test on the helper
  alone wouldn't catch).
- **`preservesNonCollapsedSelectionAcrossHighlightCycle`** - new instrumented test in
  `RememberSyntaxHighlightedEditorValueTest`. The existing
  `preservesCursorPositionInReturnedValue` only covers a collapsed cursor (`TextRange(5)`);
  this test uses `TextRange(start = 3, end = 9)` to assert both anchor and focus survive
  `.copy(annotatedString = ...)`, plus that spans are present afterward (proving the assertion
  runs on a real post-highlight value).
- **`SyntaxHighlightedTextEditorScreenshotTest`** - new Roborazzi screenshot regression test
  class. Four goldens cover the editor's editor-specific surface: default light + dark theme
  chrome, the rounded `shape` + `contentPadding` interaction documented in the editor's KDoc,
  and the unique "plain text while highlight is loading" debounce-window state (not present
  on the read-only viewer). `captureHighlightedScreenshot` now accepts a `testTag` parameter
  defaulting to `"syntax-highlighted-code"` for backwards compat with existing tests; editor
  tests pass `"syntax-highlighted-text-editor"`. Screenshot count went from 13 to 17.

### Internal

- **`SyntaxHighlightedTextEditorDefaults` object** with pre-allocated `DefaultTextStyle`
  (monospace) and `DEBOUNCE_MS = 150L` constants. The editor's `textStyle` parameter
  default previously evaluated `TextStyle(fontFamily = FontFamily.Monospace)` on every
  parent recomposition - a fresh allocation per keystroke (the worst time to allocate).
  Now both `SyntaxHighlightedTextEditor.textStyle` and `*.debounceMs` route through the
  singleton, mirroring the pattern `SyntaxHighlightedCodeDefaults` already establishes
  for the read-only viewer. Annotated `@ExperimentalHighlightApi`. Callers can `copy(...)`
  the defaults to derive customised styles.

### Infrastructure

- **Upgraded Zensical to 0.0.43** - latest documentation site generator with improved link
  validation edge cases (dollar signs, GitHub callouts, TOC markers), BOM stripping for
  UTF-8 files, and theme directory watching improvements.
- **Dokka site retheme overhaul for Zensical parity** - rebuilt Dokka `/api/` chrome to match
  the main docs site (`/`) using Dokka HTML customization (`customStyleSheets` and
  `customAssets`), with Material-style header/sidebar/footer injection, shared light/dark
  palette state between `/` and `/api/`, "Back to Docs" sidebar link, fully expanded API nav,
  spacing polish for section headers, and expanded retheme architecture/troubleshooting
  documentation in `compose-highlight/dokka-theme/README.md`.

## [0.24.1] - 2026-05-28

### Fixed

- **`SyntaxHighlightedTextEditor` / `rememberSyntaxHighlightedEditorValue` - fix wrong span
  colors during debounce after mid-text edits** - the previous span-clipping used
  `coerceAtMost(currentText.length)` which only handled append-at-end safely. Mid-text
  insertions or deletions shift characters after the edit point, causing old positional spans to
  cover the wrong characters, and dropped colors on all lines below the edit point. Fixed with
  prefix/suffix analysis: spans in the unchanged prefix are preserved
  as-is, spans in the unchanged suffix (lines below the edit) are shifted by the length delta,
  and spans in the edited region are dropped. Spans straddling the prefix/edit boundary are
  clipped to the prefix. The logic is extracted into `internal fun applySnapshotSpans` for
  direct JVM unit testing; 10 new tests cover append-at-end (no regression), insert-in-middle
  with suffix shift, span-straddling-edit-point clipping, delete-from-middle with suffix shift,
  multi-line edits preserving colors on lines below, and full replacement/empty cases. Fixes
  [#217](https://github.com/hossain-khan/android-compose-highlight/issues/217).

- **`SyntaxHighlightedTextEditor` - eliminate non-suspending `LaunchedEffect` for stale-span
  clearing** - introduced a private `HighlightSnapshot` data class that carries the `language`
  and `theme` that produced the cached spans. Stale detection now happens in-composition via a
  field comparison in the `when` block, removing the separate
  `LaunchedEffect(language, theme) { highlighted = null }` that set state without suspending.

- **`SyntaxHighlightedTextEditor` - fix `debounceMs` stale capture** - wrapped `debounceMs`
  with `rememberUpdatedState` so a changed value is picked up on the next highlight cycle
  without restarting the `LaunchedEffect` (which would reset the debounce window mid-keystroke).
  Same pattern applied to the new `onHighlightComplete` callback to avoid stale captures.

### Added

- **`SyntaxHighlightedTextEditor` - `onHighlightComplete` callback** - optional
  `(AnnotatedString) -> Unit` fired on each successful highlight cycle. Mirrors the
  `onHighlightComplete` API on `SyntaxHighlightedCode` and enables deterministic
  `waitUntil { }` patterns in instrumented tests.

- **`SyntaxHighlightedTextEditor` - test tag on root `Surface`** -
  `testTag("syntax-highlighted-text-editor")` added to the outer `Surface`, consistent with
  the `syntax-highlighted-code` tag on `SyntaxHighlightedCode`.

- **`SyntaxHighlightedTextEditorTest`** - 7 new instrumented tests covering: no-crash
  rendering, empty code, test-tag presence, `onValueChange` contract,
  `onHighlightComplete` callback after debounce, re-fires on language change, and stale-span
  invalidation on language switch.

- **`rememberSyntaxHighlightedEditorValue()`** - new public `@Composable` helper that extracts
  the debounce + engine pipeline from `SyntaxHighlightedTextEditor` into a standalone function
  mirroring the existing `rememberHighlightedCode` pattern. Returns `TextFieldValue` directly
  with syntax spans applied and cursor/selection preserved. Enables callers to bring their own
  layout (`OutlinedTextField`, third-party editor, etc.) without forking the composable.
  `SyntaxHighlightedTextEditor` now delegates to this helper and acts as a thin layout shell.

## [0.24.0] - 2026-05-27

### Added

- **`SyntaxHighlightedTextEditor`** - new public composable built on `BasicTextField` that
  provides live syntax highlighting as the user types. Keystrokes are debounced (default 150 ms)
  before triggering a highlight call. Cursor position and text selection are always preserved.
  Falls back to plain monospace text while a highlight result is in flight or on error.
  A new **Live Editor** demo tab in the sample app showcases the composable across
  6 languages (Kotlin, Python, JavaScript, SQL, JSON, XML).
  Marked `@ExperimentalHighlightApi` - callers must opt in with
  `@OptIn(ExperimentalHighlightApi::class)`.

- **`ExperimentalHighlightApi`** - new `@RequiresOptIn` annotation for APIs that are not yet
  stable. Applied to `SyntaxHighlightedTextEditor` for now; may be applied to future APIs
  before they graduate to stable.

## [0.23.0] - 2026-05-27

### Tests

- **Roborazzi screenshot regression suite for `SyntaxHighlightedCode`** - 13 goldens
  committed under `compose-highlight/src/test/snapshots/images/` and verified by CI on every
  PR with a 0.05% pixel-diff tolerance. Coverage:
  - 4 built-in themes (Tomorrow, Tomorrow Night, Atom One Dark, Atom One Light) on the same
    Kotlin sample so diffs only reflect color-map differences.
  - 4 layout variants on the Tomorrow theme: default, line numbers on, headerless,
    language-label-only.
  - 3 languages on Tomorrow: Kotlin, Python, JSON, exercising token-class breadth.
  - 2 non-happy-path render states: error fallback (highlight failed, plain code shown
    inside the Surface) and custom placeholder (highlight in flight, caller-supplied
    placeholder shown with line-number gutter preserved).
- **HTML token fixtures captured from the bundled `highlight.min.js`** instead of driving
  the real WebView from JVM tests. This isolates "is the visual rendering of
  `AnnotatedString` stable?" from "does highlight.js still tokenize correctly?" - the latter
  remains covered by managed-device instrumented tests.
  - `src/test/resources/highlight-fixtures/snippets.json` is the single source of truth for
    snippet code.
  - `compose-highlight/scripts/generate-hljs-fixtures.js` loads the bundled hljs via Node's
    `vm` module and writes one `<name>.html` per snippet. Wrapped by the
    `refreshHljsFixtures` Gradle task. Requires Node.js 18+.
  - Contributors run `./gradlew :compose-highlight:refreshHljsFixtures` after editing
    snippets or upgrading `highlight.min.js`; both `*.html` and `*.png` are committed to git.
    CI only verifies.
- **`record-screenshots.yml` `workflow_dispatch` job** re-records goldens on
  `ubuntu-latest` so contributors who hit macOS-vs-Linux Skia rendering drift can refresh
  the canonical baseline without needing Docker locally.
- **`compose-highlight/SCREENSHOT_TESTS.md`** documents the full workflow.
- **`HighlightEngine.webViewForTest()`** - new `@VisibleForTesting(otherwise = NONE)
  internal fun` exposes the underlying WebView to tests without reflection. Replaces the
  brittle `getDeclaredField`-based reflection helpers in `SyntaxHighlightedCodeRobolectricTest`
  and the screenshot test helper. Production code cannot call it; Android Lint flags any
  non-test call site.
- **Wall-clock-bounded post-callback drain** in
  `HighlightScreenshotTestHelpers.captureHighlightedScreenshot`. The previous
  `repeat(200) { idleMainLooper + waitForIdle }` cap is replaced with: (1) wait until
  `engine.isInitialized` flips to `true`, bounded by a 5-second wall-clock timeout; (2)
  pump 200 more idle cycles so the State write, recomposition, and AnimatedContent fade
  settle. Goldens stay byte-identical; failures now surface as a clear timeout error
  instead of cryptic "spans = 0" symptoms.

### Refactored

- **`ThemeParser` - replace regex CSS parser with a small recursive-descent implementation.**
  No public API change; no AAR size impact beyond ~200 LOC of parser code.
  - **Removed:** three brittle regexes (one to strip CSS comments, one to strip `@`-rule
    blocks, one to extract rules) plus post-processing to filter pseudo-classes,
    pseudo-elements, and descendant combinators.
  - **Added:** a hand-written tokenizer that skips comments and at-rule blocks via
    arbitrary-depth balanced-brace tracking, plus a recursive-descent walker that visits
    each top-level `selectors { declarations }` rule and validates individual selectors
    against a single small `HLJS_SELECTOR_REGEX`.
  - **Behavior preserved verbatim:** `@media` / `@supports` / `@keyframes` blocks skipped,
    `::pseudo` and `:pseudo` selectors rejected, descendant combinators rejected, compound
    selectors handled, comma-separated selector lists with parenthesis-aware splitting,
    split-rule `mergeSpanStyle` accumulation. All 385 existing parser tests pass on first
    compile-clean run.

## [0.22.1] - 2026-05-24

### Fixed

- **`ThemeParser` - close InputStream after reading CSS assets** - `context.assets.open(...)`
  streams in both `parse(Context, String)` and `parseAsset(Context, String)` were not explicitly
  closed. Under repeated theme loading (configuration changes, multi-screen apps), unclosed streams
  could exhaust file descriptors. Both now use `.use { it.readText() }` for deterministic cleanup.

- **`SyntaxHighlightedCode` - reset horizontal scroll position on code change** -
  `rememberScrollState()` preserved scroll position when the `code` parameter changed. A user
  who scrolled right on a long line would stay scrolled on the next code snippet. A
  `LaunchedEffect(code)` now calls `scrollTo(0)` when code changes, resetting the position while
  preserving state restoration across configuration changes (rememberScrollState is saveable).

- **`HtmlToAnnotatedString` - remove redundant private `buildAnnotatedString` shadow** - A private
  function reimplemented the standard `androidx.compose.ui.text.buildAnnotatedString` verbatim
  with a misleading comment ("without Compose runtime"). The standard library function is already
  available and is now used directly.

### Performance

- **`SyntaxHighlightedCode` - stable lambda instances for slot defaults**
  The `effectiveCopyButton` lambda and the default `languageLabel` lambda were allocated fresh on
  every recomposition, preventing the copy button and language label subtrees from being skipped by
  the Compose runtime. Both are now resolved via a sentinel pattern and wrapped in `remember`, keeping
  lambda instances stable across recompositions. This reduces unnecessary recomposition work in
  `LazyColumn` scenarios with many code blocks on screen.

## [0.22.0] - 2026-05-22

### Fixed

- **`WebViewManager` - WebView unavailability now surfaces as `WebViewInitFailed` not `JsExecutionFailed`**
  When WebView is not available on the device (Android Go, MDM-disabled, mid-system-update), the
  `WebView(context)` constructor throws a `RuntimeException`. Previously this raw exception bubbled
  through `withEngineErrorHandling` and was incorrectly wrapped as `HighlightException.JsExecutionFailed`,
  making the real cause opaque to callers. The fix wraps the entire WebView construction block in
  `WebViewManager.initialize()` with a targeted `catch (e: Exception)` that rethrows as
  `HighlightException.WebViewInitFailed`. Additionally, `HighlightEngine.initialize()` now adds a
  dedicated `catch (e: HighlightException)` branch to preserve the already-typed exception and avoid
  double-wrapping it. Both `initialize()` and all `highlight*()` methods now correctly return
  `Result.failure(WebViewInitFailed(...))` when WebView is unavailable.
- **`unescapeJsString` - surrogate pair handling for emoji and supplementary Unicode**
  `unescapeJsString` decoded each `\uXXXX` sequence independently via `codePoint.toChar()`.
  Characters above U+FFFF (emoji, mathematical symbols, CJK Extension B, etc.) are encoded by
  `evaluateJavascript` as UTF-16 surrogate pairs - two consecutive `\uXXXX` sequences. The old code
  emitted two lone surrogate `Char` values, which Android text rendering (Skia/HarfBuzz) treats as
  invalid and renders as replacement characters (U+FFFD) or drops silently. Source code containing
  emoji in comments or string literals (`// TODO: fix this 🐛`) was silently corrupted in the
  highlighted output. The `'u'` branch now detects a high surrogate (U+D800-U+DBFF) followed by a
  low surrogate `\uXXXX` (U+DC00-U+DFFF) and combines them into the correct supplementary code point
  via `appendCodePoint`. Lone surrogates without a valid pair are still emitted as-is (best effort, no crash).

- **`escapeForJs` - null byte and control character escaping** - `escapeForJs` did not
  escape null bytes (U+0000) or control characters U+0001-U+001F (excluding `\n`, `\r`,
  `\t`). A null byte could silently truncate the JS string inside the WebView V8 engine,
  producing incorrect or partial highlight output with no error signal. ANSI escape codes
  (U+001B) from terminal output were passed through unescaped. All control characters are
  now escaped as `\uXXXX` sequences, and tab (`\t`, U+0009) is now explicitly escaped as
  `\\t`.
- **`HighlightTheme` - content-aware equality** - `equals()` and `hashCode()` previously
  compared themes by `name` only, so two themes with the same name but different CSS
  content were considered equal. This caused `LaunchedEffect(theme)` and `remember(theme)`
  in `rememberHighlightedCode` to silently skip re-highlighting when switching between
  same-named themes with different colors. Equality now includes a precomputed
  `contentIdentity` digest derived from the effective theme content:
  - `fromCss(cssText, name)` - identity uses a SHA-256-based digest of `cssText`
  - `fromAsset(context, assetPath, name)` - identity uses a SHA-256-based digest of `assetPath`
  - `fromColorMap(name, colorMap, ...)` - identity uses a SHA-256-based digest of the effective
    color map after optional `.hljs` background/text overrides are applied
  - Built-in factories (`tomorrow`, `tomorrowNight`, `atomOneDark`, `atomOneLight`) - identity
    uses their fixed asset path

  Two themes with the same name and the same content are still considered equal, preserving
  memoization for the common case of re-creating the same theme across recompositions.

- **`ThemeParser` - CSS4 `rgb()` space-separated syntax support** - `rgb(R G B)` and
  `rgb(R G B / alpha)` (CSS Color Level 4) were silently ignored, causing color values in
  modern hljs themes to fall back to `Color.Unspecified`. The parser now handles both the
  legacy comma syntax (`rgb(R, G, B)`, `rgba(R, G, B, A)`) and the modern space/slash
  syntax (`rgb(R G B)`, `rgb(R G B / A)`). Percentage channel values (e.g.
  `rgb(100% 0% 50%)`) and percentage alpha (e.g. `rgb(100% 0% 50% / 50%)`) are also
  supported.

## [0.21.0] - 2026-05-22

### Changed (Breaking)

- **`SyntaxHighlightedCode` slot parameter rename** - `languageLabelContent` renamed to
  `languageLabel` and `copyButtonContent` renamed to `copyButton` to match Material 3 naming
  conventions (`TextField.label`, `Scaffold.topBar`, etc.). The `@Composable` type annotation
  already communicates that these are content slots; the `Content` suffix was redundant.
  `placeholder` is unchanged - it already followed the shorter convention.

  | Before | After |
  | --- | --- |
  | `languageLabelContent` | `languageLabel` |
  | `copyButtonContent` | `copyButton` |

  Update call sites:

  ```kotlin
  // Before
  SyntaxHighlightedCode(
      code = snippet,
      language = "kotlin",
      languageLabelContent = null,
      copyButtonContent = { onClick -> MyButton(onClick) },
  )
  // After
  SyntaxHighlightedCode(
      code = snippet,
      language = "kotlin",
      languageLabel = null,
      copyButton = { onClick -> MyButton(onClick) },
  )
  ```

### Added

- `CodeBlockStyle` gains two new fields: `fallbackBackgroundColor: Color` (default `Color(0xFF1E1E1E)`)
  and `fallbackTextColor: Color` (default `Color(0xFFCCCCCC)`). These are used by
  `SyntaxHighlightedCode` when the active theme's CSS has no `.hljs { background: ... }` or
  `.hljs { color: ... }` rule. Previously the fallback values were hardcoded inline; they are now
  configurable via `CodeBlockStyle`. This changes the public `CodeBlockStyle` data class constructor
  signature and is binary-incompatible for already compiled consumers.

  ```kotlin
  // Light fallback for a custom light-only theme that has no .hljs base rule
  SyntaxHighlightedCode(
      code = snippet,
      language = "kotlin",
      style = CodeBlockStyle(
          fallbackBackgroundColor = Color.White,
          fallbackTextColor = Color.Black,
      ),
  )
  ```

  Corresponding constants `SyntaxHighlightedCodeDefaults.fallbackBackgroundColor` and
  `SyntaxHighlightedCodeDefaults.fallbackTextColor` are also exposed.

### Fixed

- **`ThemeParser` font-weight numeric values** - Previously only `font-weight: bold` and
  `font-weight: 700` mapped to `FontWeight.Bold`; other numeric values (e.g. `600`, `800`, `900`)
  were silently ignored. Now any numeric weight >= 600 maps to `FontWeight.Bold` and any numeric
  weight < 600 maps to `FontWeight.Normal`, matching standard browser bold-threshold behavior. The
  explicit keywords `bold` and `normal` continue to work as before.

- **`bridge.html` hljs error handling** - `highlightCode`, `highlightAuto`, and `getLanguage` are
  now wrapped in `try/catch`. `highlightCode` and `highlightAuto` now return a consistent JSON envelope
  (`{ error: false, html: ... }` on success and `{ error: true, message: ... }` on failure), and
  `getLanguage` returns either language JSON, `null`, or the same error envelope. `HighlightEngine`
  parses these responses and surfaces `HighlightException.JsExecutionFailed` with the actual
  JavaScript error message, replacing the previous opaque `"JS returned null"` error.

- **`HighlightTheme.timedColorMap()` race and attribution fix** - `timedColorMap()` now uses
  `AtomicBoolean.compareAndSet` around `measureTimedValue` so at most one concurrent caller reports
  non-zero parse time. If `colorMap` was initialized earlier via incidental access
  (`backgroundColor` or `defaultTextColor`), `timedColorMap()` now returns `Duration.ZERO` to keep
  `HighlightTimings.themeParse` attributed only to first-call work performed by `HighlightEngine`.

- `placeholder: (@Composable (code: String) -> Unit)? = null` parameter added to
  `SyntaxHighlightedCode`. When provided, the placeholder composable is rendered while async
  highlighting is in progress (state is null) and transitions to the highlighted output via
  `AnimatedContent` once ready. When `null` (default), the existing behavior is preserved - raw
  unstyled code is shown until highlighting completes. The `code` string is passed to the
  placeholder so it can optionally display it styled differently (e.g., dimmed or with a shimmer).

  ```kotlin
  SyntaxHighlightedCode(
      code = myCode,
      language = "kotlin",
      placeholder = { rawCode ->
          Text(
              text = rawCode,
              color = Color.Gray.copy(alpha = 0.5f),
              fontFamily = FontFamily.Monospace,
          )
      },
  )
  ```

- `HighlightEngine` now implements `java.io.Closeable`. A new `close()` method delegates to
  `destroy()`, improving IDE resource-leak inspections and explicit cleanup for non-Compose
  usage (e.g. ViewModel). `destroy()` remains available and both methods are
  idempotent - safe to call multiple times.

- `onError: ((HighlightException) -> Unit)? = null` callback added to `SyntaxHighlightedCode`,
  `rememberHighlightedCode`, and `rememberHighlightedCodeBothThemes`. When highlighting fails
  (timeout, JS error, WebView init failure, or HTML parse failure) the callback receives the typed
  `HighlightException` subtype. Plain-text fallback behavior is unchanged - `onError` is purely
  observational. Use it to log failures, record analytics events, or show a snackbar.

  ```kotlin
  SyntaxHighlightedCode(
      code = myCode,
      language = userInput,
      onError = { error ->
          Log.w("Highlight", "Failed: ${error.message}")
      },
  )
  ```

- Added jsoup keep rules to `consumer-rules.pro` so consuming apps with R8 minification
  enabled do not encounter runtime crashes (`ClassNotFoundException`, `NoSuchMethodError`)
  from stripped or obfuscated jsoup classes. Rules cover `org.jsoup.Jsoup`,
  `org.jsoup.parser.**`, `org.jsoup.nodes.**`, and `org.jsoup.select.**`.

### Performance

- `HtmlToAnnotatedString.convertTimed()` and theme color map resolution now run on
  `Dispatchers.Default` instead of the main thread. For large code blocks with hundreds of
  spans, this eliminates the risk of dropped frames during parsing. WebView JS calls remain
  on `Dispatchers.Main` as required by Android.
- `highlightAuto()` now releases the serializing mutex before the CPU-intensive HTML
  parsing step, allowing other highlight calls to proceed sooner.

## [0.20.0] - 2026-05-20

### Added

- `HighlightLanguage.fromExtension(ext)` utility - maps file extensions to Highlight.js
  language names without a WebView round-trip (e.g. `"kt"` -> `"kotlin"`)
- `HighlightEngine.highlightAuto()` - highlights code with automatic language detection
  via `hljs.highlightAuto()`, returns `AutoHighlightResult` with `detectedLanguage`
- `HighlightEngine.getLanguage()` - looks up a language by name or alias via
  `hljs.getLanguage()`, returns `HighlightLanguageInfo` (name + aliases list) or null if
  not found
- `AutoHighlightResult` data class - result of `highlightAuto()` with
  `detectedLanguage`, `annotated`, `spanCount`, `durationMs`, and `timings`
- `HighlightLanguageInfo` data class - result of `getLanguage()` with `name` and `aliases`
- Documentation and sample app demos for the language discoverability APIs, including a
  new language selection guide, `HighlightLanguage` reference, and interactive sample tab
  covering `fromExtension()`, `getLanguage()`, and `highlightAuto()`
- **Bridge validation workflow** - added `.github/workflows/bridge-validation.yml` to
  validate `compose-highlight/src/main/assets/compose-highlight/bridge.html` in CI. The
  workflow runs HTML syntax checks and enforces the JS bridge contract (`highlightCode`,
  `listLanguages`, `hljsVersion`) plus the expected WebView `bridge.html` load URL.

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
- **`SyntaxHighlightedCodeDefaults.CopyButton`** - public composable helper that renders the
  default `⧉` copy icon inside an `IconButton`. Accepts `onClick`, `tint`, `contentDescription`,
  and `size` parameters so callers can compose on top of the default look.
- **`SyntaxHighlightedCodeDefaults.LanguageLabel`** - public composable helper that renders
  the language badge. Accepts `language`, `color`, and `fontSize` parameters. Useful for
  toggling visibility at runtime without reconstructing the full default style.

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

- **`ThemeParser` - pseudo-element/pseudo-class selector leak** - selectors containing `::` or `:`
  (e.g. `.hljs::selection`, `.hljs:hover`) were being stripped of the pseudo-part and incorrectly
  stored as the `.hljs` base entry. This caused the selection-highlight background color
  (e.g. `#516d7b` in `base16/atelier-lakeside`) to overwrite the real theme background (`#161b1d`),
  making code blocks appear with the wrong background color. All `::selection`, `:hover`, etc. rules
  are now skipped during parsing. Affects all 4 bundled themes (`tomorrow`, `tomorrow-night` had
  `::selection` rules) and any custom `fromAsset()` / `fromCss()` theme with similar rules.

### Changed

- **Sample app - "All Themes" tab** - new tab that bundles all 256 highlight.js 11.11.1 theme CSS
  files as sample app assets. A searchable dropdown lets users pick any theme and instantly
  live-preview it on a code block, similar to the highlightjs.org/demo experience.
- **Sample app - interactive Styling section** - replaced three static `CodeBlockStyle` demo blocks
  with a single live-preview code block. An `ExtendedFloatingActionButton` (bottom-right) opens a
  `ModalBottomSheet` where all `CodeBlockStyle` parameters can be adjusted and instantly reflected
  in the code block behind it.
- **Sample app - Themes section** - trimmed to one built-in theme demo and one `fromAsset()` demo
  that shows its own loading code as the highlighted snippet.

## [0.16.0] - 2026-05-16

### Fixed

- **`CancellationException` is no longer swallowed in `HighlightEngine`** - `highlightToHtml`,
  `supportedLanguages`, and `highlightJsVersion` previously caught all `Exception` types,
  silently converting parent coroutine cancellations into `Result.failure(JsExecutionFailed(...))`.
  They now rethrow `CancellationException` to respect structured concurrency.
- **`HighlightException.Timeout` is now actually thrown on timeout** - the same broad
  `catch (e: Exception)` was swallowing `TimeoutCancellationException` (from `withTimeout`),
  making `HighlightException.Timeout` dead code. Timeout is now correctly caught and converted
  to `Result.failure(HighlightException.Timeout())` in all three methods.

### Performance

- **`SyntaxHighlightedCode` is now `restartable skippable`** - Derived colors and `TextStyle`
  values (`backgroundColor`, `textColor`, `lineNumberColor`, `themedCodeStyle`,
  `themedLineNumStyle`, `languageLabelStyle`) are now wrapped in `remember(theme, style)` blocks
  so they are only recomputed when the theme or style changes, not on every recomposition.
  This eliminates 6 unnecessary `TextStyle.copy()` / `Color` allocations per recomposition and
  allows the Compose compiler to classify `SyntaxHighlightedCode`, `LineNumberedCode`, and
  `CopyButton` as `skippable` (previously none were skippable - verified via compiler reports).
  Result: `knownUnstableArguments` dropped from 4 → 0; skippable composables increased from 0 → 7.

## [0.15.0] - 2026-05-14

### Changed

- **`HighlightEngine.initialize()` now returns `Result<Unit>`** instead of throwing
  `HighlightException.WebViewInitFailed`. This aligns `initialize()` with the library's
  documented `Result`-based public error model. Migration: replace bare `engine.initialize()`
  calls with `engine.initialize().onFailure { /* handle */ }` or
  `engine.initialize().getOrThrow()` if you want failure to propagate as an exception.
- **`highlightBothThemes` now parses the HTML once** - `HtmlToAnnotatedString.convertBothThemes()`
  replaces the prior double-`convert()` call in `HighlightEngine.highlightBothThemes()`. The HTML
  fragment is parsed into a DOM once and walked once, with two `AnnotatedString.Builder` instances
  updated in parallel (one per theme). This removes a redundant Jsoup parse and a redundant DOM
  traversal on every dual-theme highlight call. Closes #82.

### Added

- **Robolectric JVM tests for Compose UI** - Added 13 new unit tests in `src/test/` that run on
  the JVM without an emulator using Robolectric 4.16.1 and the v2 Compose testing APIs
  (`androidx.compose.ui.test.junit4.v2.createComposeRule`).
  - `SyntaxHighlightedCodeRobolectricTest` (8 tests): verifies preview fallback rendering, test
    tags, language label, copy button visibility, copy button content description, custom copy
    button content description, copy click callback, and default language label display.
  - `HighlightThemeProviderRobolectricTest` (5 tests): verifies theme provision, light/dark theme
    access, dark-mode selection, light-mode selection, and the expected error when accessed without
    a provider.

### Fixed

- **`HighlightTheme` context factories now normalize to `applicationContext` internally** -
  `tomorrow`, `tomorrowNight`, `atomOneDark`, `atomOneLight`, and `fromAsset` defensively resolve
  `context.applicationContext` before retaining it in lazy theme providers, preventing accidental
  Activity-context retention when a theme instance outlives an Activity lifecycle.
- **Sample app preserves top-level demo selections across recreation** - `SampleScreen` now uses
  `rememberSaveable` for the light/dark toggle, selected demo tab, and selected theme family so
  those user-facing choices survive configuration changes.

## [0.14.0] - 2026-05-13

### Added

- **LeakCanary integrated into the sample app** - `com.squareup.leakcanary:leakcanary-android:2.14`
  is added as a `debugImplementation` dependency in `sample/build.gradle.kts`. LeakCanary
  automatically detects memory leaks in debug builds and displays a notification with a heap dump
  analysis when a leak is found. No code changes are required - LeakCanary installs itself via
  its `ContentProvider`.

### Changed

- **`SelectionContainer` moved inside `AnimatedContent`** in `SyntaxHighlightedCode` - during the
  plain-text → highlighted crossfade, `SelectionContainer` now wraps only the currently visible
  content rather than both states simultaneously, preventing potential disruption to active text
  selection during the transition animation.
- **`CodeBlockStyle` annotated `@Stable`** - Compose can now skip recomposition of callers that
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
  the new deferred permanently unresolved - all subsequent highlight calls would suspend forever.
  Two targeted fixes applied (Option C from the issue report):
  - `readyDeferred` is now `@Volatile` so writes by `destroy()` (any thread) are immediately
    visible to `getReadyWebView()` and `initialize()` on other threads (ARM weak memory model).
  - `onPageFinished` now checks `webView == null` before completing the deferred. If `destroy()`
    ran while the page was loading, the callback returns early; the next `initialize()` call
    picks up the fresh deferred and completes it normally, fully recovering the engine.

- **U+2028/U+2029 escaping in JS template** - `executeJs` now escapes Unicode Line Separator
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
- **`WebViewManager.webView` marked `@Volatile`** - prevents stale-read on ARM's weak memory model
  when `initialize()` checks the field before switching to the Main thread.
- **`language` parameter escaped in JS template** - `executeJs` now escapes backslashes and single
  quotes in `language` before interpolating into the `highlightCode(...)` JS call, closing a minor
  JS-injection vector (defense-in-depth; the WebView has no access to sensitive data).
- **Accessibility: copy button `contentDescription`** - The copy-to-clipboard `IconButton` inside
  `SyntaxHighlightedCode` now carries `contentDescription = "Copy code"` so TalkBack announces it
  meaningfully instead of the generic "Button".
- **Accessibility: `testTag` on outer container** - `SyntaxHighlightedCode` now applies
  `testTag("syntax-highlighted-code")` on its outer `Surface`, giving UI-test consumers a stable
  node handle without relying on fragile text or structure queries.
- **`HighlightThemeProvider` default themes are now remembered** - default parameters now use
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

- **Maven Central publishing** - library is now published to Maven Central (`dev.hossain:compose-highlight`)
  in addition to JitPack. GPG signing, sources JAR, and Dokka HTML javadoc JAR are all included.
- **`PUBLISHING.md`** - end-to-end guide covering prerequisites (Sonatype, GPG, GitHub Secrets),
  release steps, dry-run instructions, local dry run, and Gradle task reference.

### Infrastructure

- Added `gradle-nexus/publish-plugin` v2.0.0 for Central Portal staging API integration.
- Added `publish.yml` GitHub Actions workflow with `workflow_dispatch` (`tag` + `dry_run` inputs),
  pre-flight tag/already-published checks, artifact validation, and one-click real publish.

## [0.11.0] - 2026-05-11

### Added

- **`HtmlHighlightResult` data class** - `HighlightEngine.highlightToHtml()` now returns
  `Result<HtmlHighlightResult>` instead of `Result<String>`. The result pairs the raw HTML with
  timing data so callers no longer need to measure JS round-trip time manually:
  - `html: String` - raw HTML with `<span class="hljs-*">` tokens (same content as before, via `.html`)
  - `durationMs: Long` - JavaScript round-trip time, measured after the WebView is ready
    and the internal mutex is acquired (excludes WebView warm-up and queue-wait time)

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

- **`HighlightEngine.isInitialized: StateFlow<Boolean>`** - changed from a plain
  `Boolean` property to a `StateFlow<Boolean>`, enabling Compose composables to observe
  engine initialization reactively without a separate `var engineReady` flag:

  ```kotlin
  val isReady by engine.isInitialized.collectAsState()
  if (isReady) { /* WebView is warm */ }
  ```

  **Migration:** replace `engine.isInitialized` with `engine.isInitialized.value` in
  non-Compose contexts; use `engine.isInitialized.collectAsState()` in Compose.

- **`rememberHighlightedCodeBothThemes` now accepts `onHighlightComplete`** - added
  `onHighlightComplete: ((ThemedHighlightResult) -> Unit)?` callback parameter for
  consistency with `rememberHighlightedCode`. Fires after the state is updated on success.

- **`rememberHighlightedCodeBothThemes` now works inside `HighlightThemeProvider`** -
  `lightTheme` and `darkTheme` parameters now default to `LocalLightHighlightTheme.current`
  and `LocalDarkHighlightTheme.current` respectively, so callers inside a
  `HighlightThemeProvider` no longer need to pass themes explicitly.

- **`LocalLightHighlightTheme` and `LocalDarkHighlightTheme` CompositionLocals** -
  `HighlightThemeProvider` now provides both the individual light and dark themes via these
  new public CompositionLocals (in addition to the existing `LocalHighlightTheme` for the
  active theme). Useful when composables need both variants simultaneously.

- **`@Composable` theme helpers** - new `rememberTomorrowTheme()`,
  `rememberTomorrowNightTheme()`, `rememberAtomOneDarkTheme()`,
  `rememberAtomOneLightTheme()` functions resolve `LocalContext` internally, removing the
  need for `val context = LocalContext.current` boilerplate at call sites:

  ```kotlin
  // Before
  val theme = remember(context) { HighlightTheme.tomorrow(context.applicationContext) }

  // After
  val theme = rememberTomorrowTheme()
  ```

### Fixed

- **`HtmlHighlightResult.durationMs` now measures JS round-trip only** - the timer
  previously started before WebView initialisation and mutex acquisition, so it included
  warm-up and queue-wait time. It now starts immediately before `evaluateJavascript()` is
  called, after the WebView is ready and the internal mutex is held.

### Sample app improvements

- **Code samples moved to asset files** - 17 language samples previously hardcoded as Kotlin raw
  strings in `SampleData.kt` are now individual files in `assets/samples/` (e.g. `01_fibonacci.py`,
  `08_WeatherApp.kt`). Adding a new language sample only requires dropping a file in that folder -
  no Kotlin changes needed. Each file has a real extension so IDEs apply syntax highlighting when
  viewing or editing them.
- **`sample/README.md`** - documents the sample app structure, what each tab demonstrates, and
  how to add new language samples or custom themes.
- **Sample app organisation** - `DemoSections.kt` split into a `sections/` package (one file per
  tab); tab routing uses a `DemoTab` sealed class instead of integer indices.
- **Fixed: Engine tab language list now scrollable** - the 192-language list was clipped at a
  fixed height with no scroll. Fixed by adding `verticalScroll` to the list container.
- **Fixed: App crash on launch (NPE in tab bar)** - `DemoTab.all` companion `val` was evaluated
  during class init before the `data object` instances were set, resulting in a list of nulls.
  Fixed with `by lazy { }`.

## [0.10.0] - 2026-05-10

### Added

- **`HighlightResult` data class** - `HighlightEngine.highlight()` now returns
  `Result<HighlightResult>` instead of `Result<AnnotatedString>`. The result carries:
  - `annotated: AnnotatedString` - the highlighted text (same as before, via `.annotated`)
  - `spanCount: Int` - number of highlight spans; `0` signals a silent failure (unsupported
    language or empty input) without an exception
  - `language: String` - the language identifier that was requested
  - `durationMs: Long` - pure highlight time (JS call + HTML conversion), excluding
    coroutine-scheduling overhead  

  **Migration:** replace `.onSuccess { it }` with `.onSuccess { it.annotated }`.

- **`HighlightEngine.isInitialized: Boolean`** - `true` once the hidden WebView has loaded
  `bridge.html`. Removes the need for a manual `var engineReady` flag in calling code.

- **`ThemedHighlightResult.durationMs: Long`** - timing is now included in the result returned
  by `highlightBothThemes` and `rememberHighlightedCodeBothThemes`. Read it directly from the
  state value instead of using a separate callback.

- **`HighlightEngine.supportedLanguages(): Result<List<String>>`** - returns the sorted list of
  language identifiers supported by the bundled Highlight.js (190+ languages). Result is fetched
  from the JS engine on the first call and cached for subsequent calls.

  ```kotlin
  engine.supportedLanguages().onSuccess { languages ->
      val isKotlinSupported = "kotlin" in languages  // true
  }
  ```

- **`HighlightEngine.highlightJsVersion(): Result<String>`** - returns the version string of the
  bundled Highlight.js library (e.g. `"11.11.1"`). Cached after the first call.

  ```kotlin
  engine.highlightJsVersion().onSuccess { version ->
      println("Using Highlight.js $version")
  }
  ```

- **Sample app: Engine tab** - new tab in the demo app showcasing `highlightJsVersion()` and
  `supportedLanguages()`. Displays the bundled Highlight.js version string and a scrollable,
  numbered list of all 192 supported language identifiers.

### Changed

- **`onHighlightComplete` callback now receives `HighlightResult`** - both
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

- **`rememberHighlightedCode` timing is now measured inside the engine** - `durationMs` in
  `HighlightResult` reflects pure highlight time (JS round-trip + HTML parse), not
  coroutine-scheduling overhead.
- **`CodeBlockStyle` gains a `textStyle: TextStyle` property** - font family, font size,
  and line height are now configured via `CodeBlockStyle.textStyle` (defaulting to
  `SyntaxHighlightedCodeDefaults.codeTextStyle`: monospace, 13 sp, 20 sp line height).
- **`SyntaxHighlightedCode`: removed `fontFamily`, `fontSize`, `lineHeight` parameters**
  - these three top-level parameters are replaced by `CodeBlockStyle.textStyle`.
  Consolidating typography into `CodeBlockStyle` follows established Compose library
  patterns (e.g. Material 3, Haze) where all visual style is expressed through a single
  style object.

  **Migration:** replace individual parameters with `style = CodeBlockStyle(textStyle = ...)`:

  ```kotlin
  // Before
  SyntaxHighlightedCode(code = snippet, language = "kotlin", fontSize = 15.sp,
                        lineHeight = 24.sp)

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

### Removed

- **`onHighlightComplete` removed from `rememberHighlightedCodeBothThemes`** - timing is now
  available directly on `ThemedHighlightResult.durationMs`, so a separate callback is not
  needed. Read timing from the state value you already hold:

  ```kotlin
  val result by rememberHighlightedCodeBothThemes(...)
  val timing = result?.durationMs   // available once result is non-null
  ```

## [0.9.0] - 2026-05-10

### Added

- **`SyntaxHighlightedCodeDefaults` object** - new top-level object that exposes all default
  values used by `SyntaxHighlightedCode` and `CodeBlockStyle` (`codeTextStyle`, `shape`,
  `padding`, `headerPadding`, `lineNumberWidth`, `copyButtonSize`). Callers can now discover
  and override individual defaults without hard-coding magic numbers:

  ```kotlin
  CodeBlockStyle(
      textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontSize = 15.sp),
  )
  ```

### Changed

- **`CodeBlockStyle` gains a `textStyle: TextStyle` property** - font family, font size,
  and line height are now configured via `CodeBlockStyle.textStyle` (defaulting to
  `SyntaxHighlightedCodeDefaults.codeTextStyle`: monospace, 13 sp, 20 sp line height).
- **`SyntaxHighlightedCode`: removed `fontFamily`, `fontSize`, `lineHeight` parameters**
  - these three top-level parameters are replaced by `CodeBlockStyle.textStyle`.
  Consolidating typography into `CodeBlockStyle` follows established Compose library
  patterns (e.g. Material 3, Haze) where all visual style is expressed through a single
  style object.

  **Migration:** replace individual parameters with `style = CodeBlockStyle(textStyle = ...)`:

  ```kotlin
  // Before
  SyntaxHighlightedCode(code = snippet, language = "kotlin", fontSize = 15.sp,
                        lineHeight = 24.sp)

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

- **Updated target SDK from 37 to 36** - Android 17 (API 37) is in late beta; using
  stable API 36 (Android 12) for production compatibility while maintaining minSdk 24.

## [0.7.0] - 2026-05-09

### Added

- **`SyntaxHighlightedCode`: `copyButtonIcon` composable slot** - optional parameter
  `copyButtonIcon: (@Composable (tint: Color) -> Unit)?` that replaces the default `⧉`
  text icon with any composable. Receives the theme-derived `tint` color so custom icons
  blend naturally with the code block background. Defaults to `null` (original `⧉`
  behaviour).
- **Sample app: performance benchmark screen** - new `PerfActivity`/`PerfScreen` that
  highlights all language samples and displays per-block timing (ms), line count, and
  character count as metric chips. Includes a dark/light toggle that clears and re-runs
  all benchmarks so theme changes are reflected in measurements.
- **Sample app: 8 new language samples** - TypeScript, Rust, Go, Swift, C++, C#, Bash,
  and CSS added to `SampleData`, each using constructs that stress different highlighter
  token types (generics, lifetimes, goroutines, template literals, etc.).
- **Sample app: Snackbar copy confirmation** - the Languages tab now defines a shared
  `onCopyClick` handler that copies code to the system clipboard and shows a
  `"Successfully copied source code to clipboard"` Snackbar, demonstrating
  caller-owned copy feedback.

### Fixed

- **Sample app: edge-to-edge insets on `LazyColumn`s** - both `SampleScreen` and
  `PerfScreen` now pass bottom (and top) system bar insets to the list's `contentPadding`
  parameter and use `consumeWindowInsets` on the parent container. Previously the inset was
  applied as `Modifier.padding(innerPadding)` on the container, which clipped the list and
  prevented the last item from scrolling clear of the navigation bar.

### Changed

- **`SyntaxHighlightedCode`: removed internal copy confirmation UI** - the library no
  longer manages a 2-second "Copied!" flash internally. The `onCopyClick` callback is the
  signal that a copy occurred; callers own the feedback UX (Snackbar, Toast, animated
  indicator, etc.). This is a **behavioural change**: apps that relied on the built-in
  "Copied!" text will need to implement their own confirmation via `onCopyClick`.
- **Sample app: vector icons throughout** - emoji placeholders in the TopAppBar (theme
  picker, light/dark toggle, benchmark launcher) and perf screen metric chips replaced with
  Material Design vector drawables (`palette_24dp`, `light_mode_24dp`, `mode_night_24dp`,
  `speed_24dp`, `timer_24dp`, `format_line_spacing_24dp`, `type_specimen_24dp`).
- **Sample app: Atom One dark as default** - both the main screen and the benchmark
  screen now open in dark mode with the Atom One theme instead of the system default.
- **Sample app: `copyButtonIcon` used in Languages tab** - all code blocks in the
  Languages tab now render a `content_copy_24dp` vector icon instead of the default `⧉`
  character.

## [0.6.0] - 2026-05-09

### Added

- **Sample app: comprehensive customization demo** - The sample app now has tabbed
  navigation with seven sections showcasing every public API feature:
  - **Languages**: original multi-language highlight demo (Python, Kotlin, Java, SQL, etc.)
  - **Styling**: `CodeBlockStyle.Default`, `CodeBlockStyle.Compact`, and a custom style
    with shape, padding, gutter width, and copy-button size
  - **Typography**: `fontSize`, `lineHeight`, and `fontFamily` variants
  - **Toggles**: 2×2 combinations of `showLineNumbers`/`showLanguageLabel` plus
    `showCopyButton` on/off
  - **Callbacks**: `onHighlightComplete` (displays millisecond duration) and `onCopyClick`
    (custom handler with inline feedback)
  - **Themes**: all `HighlightTheme` factory methods - built-in (tomorrow, atom-one),
    `fromAsset()` (GitHub CSS), `fromCss()` (inline Material 3 CSS), and `fromColorMap()`
    (Material 3 color map)
  - **Advanced**: `rememberHighlightedCodeBothThemes()` - pre-highlights for both light
    and dark in one JS call for instant theme switching
- JVM unit tests for `HighlightTheme`: `fromCss`, `fromColorMap`, lazy `colorMap`,
  `backgroundColor`, `defaultTextColor`, `equals`/`hashCode`/`toString`, and
  defensive-copy behavior
- JVM unit tests for all `HighlightException` variants: message content, cause
  preservation, and the `TIMEOUT_SECONDS` constant
- Additional `ThemeParser` tests: `rgb()` color format, `background-color` property,
  `font-weight: 700`, 8-digit hex colors, and descendant-selector skipping
- Additional `HtmlToAnnotatedString` tests: non-span element wrapping, HTML entity
  decoding, and base-style application

### Changed

- Bumped `androidx.compose:compose-bom` from `2026.03.01` to `2026.05.00`.
- Bumped `androidx.benchmark` from `1.3.3` to `1.4.1`.
- Bumped `org.jsoup:jsoup` from `1.18.3` to `1.22.2`.
- Bumped `androidx.webkit` from `1.13.0` to `1.16.0` (stable). Highlights of changes
  since 1.13.0 relevant to this project:
  - `startUpWebView()` and `WebViewStartUpConfig` APIs graduated to stable.
  - `NavigationListener` / `WebViewCompat.addNavigationListener()` graduated to stable.
  - `minSdk` for the webkit library increased to 24 (matches this library's `minSdk`).

### Fixed

- **`WebViewManager`: `readyDeferred` lifecycle correctness** - `readyDeferred` is now
  a `var` so it can be reset before re-initialization after `destroy()`. The
  WebViewClient closure now captures the deferred as a local variable, preventing it from
  completing a stale deferred on re-initialization. `destroy()` now cancels a pending
  (incomplete) deferred so callers awaiting the WebView in `getReadyWebView()` are not
  left suspended indefinitely.
- **`HighlightEngine`: Activity context leak** - `HighlightEngine` now always calls
  `context.applicationContext` before passing the context to `WebViewManager`, ensuring
  that a long-lived WebView never retains an Activity reference. `HighlightThemeProvider`
  and `rememberHighlightEngine()` also explicitly pass `applicationContext`.
- **`HighlightEngine`: incorrect JSON unescape ordering** - replaced the sequential
  `String.replace()` chain in `unescapeJsString` with a single character-by-character
  pass. The old approach applied `\n` → newline before `\\` → `\`, which incorrectly
  converted `\n` (a literal backslash + 'n' in JSON) to a newline instead of `\n`. The
  new implementation also adds support for `\r` → CR and `\/` → `/` escape sequences.
- **`HighlightThemeProvider`: shared engine for the whole subtree** - the provider now
  creates one `HighlightEngine` (one hidden WebView) for its entire subtree and provides
  it via an internal `LocalHighlightEngine` CompositionLocal. Previously, every
  `SyntaxHighlightedCode` and every `rememberHighlightedCode` call created its own engine.
  On a screen with N code blocks this wasted ~200 ms × N of WebView warm-up time and
  ~2–4 MB × N of memory.
- **`rememberHighlightEngine()`: uses shared engine when available** - when called inside
  `HighlightThemeProvider`, it returns the provider's shared engine (no new WebView, no
  management needed). Outside a provider the previous behavior is unchanged: a standalone
  engine is created and destroyed with the composable.

## [0.5.0] - 2026-04-27

### Fixed

- `HtmlToAnnotatedString.convert()` now applies the `.hljs` base text color as a full-range
  outer span on the resulting `AnnotatedString`. Plain tokens (identifiers, whitespace) now
  inherit the theme color rather than `LocalContentColor`, so `Text(text = highlighted)`
  works correctly without requiring a manual `color` override.

## [0.4.0] - 2026-04-26

### Added

- `HighlightTheme` now implements `equals()`/`hashCode()` based on `name` - fixes stale
  highlighting when using `LaunchedEffect(theme)` or `remember(theme)`
- `HighlightTheme` annotated `@Stable` - enables Compose skipping optimisation for composables
  that receive a theme parameter
- `rememberHighlightedCode()` and `SyntaxHighlightedCode` now accept
  `onHighlightComplete: ((Long) -> Unit)?` callback for performance metrics
- New `rememberHighlightedCodeBothThemes()` composable - highlights once for both light and
  dark themes, enabling instant theme switching

### Fixed

- `HighlightTheme.fromColorMap()` now defensively copies the provided map so later
  mutations don't affect the theme
- Built-in theme factories now throw on missing CSS assets instead of silently returning
  an unstyled theme

## [0.3.0] - 2026-04-26

### Added

- `HighlightTheme.fromColorMap()` - supply a theme from any `Map<String, SpanStyle>` (e.g.
  Material 3 dynamic color)
- Theme picker in sample app - switch between GitHub, Tomorrow, and Atom One theme families
- Sample app uses GitHub and GitHub Dark themes via `HighlightTheme.fromAsset()`,
  demonstrating user-provided custom themes

### Fixed

- `HighlightTheme.fromAsset()` now correctly throws `HighlightException.ThemeNotFound` when
  the asset file is missing (previously the error was silently swallowed)

## [0.2.0] - 2026-04-26

### Added

- JitPack publishing support - library available via `com.github.hossain-khan:android-compose-highlight:0.2.0`
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
