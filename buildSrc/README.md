# buildSrc

Build-time logic for the `compose-highlight` library. Currently houses the theme precompilation
pipeline that turns the eight bundled hljs CSS files into a Kotlin source map at build time, so
the runtime parser is never invoked for built-in themes.

> **Important:** read [`compose-highlight/MODULE.md`](../compose-highlight/MODULE.md#themes) for
> the runtime side of this story (which factory to call, what ships in the AAR, etc.). This
> README is the build-side companion.

## Why this exists

The runtime `ThemeParser` lives in `compose-highlight/src/main/kotlin/.../engine/ThemeParser.kt`
and parses CSS into `Map<String, SpanStyle>`. For the eight bundled themes shipped with the
library, that work was happening on the device on first theme access:

1. Open asset stream
2. Read CSS bytes
3. Run the recursive-descent CSS parser
4. Resolve color formats (hex / `rgb()` / named)
5. Build `SpanStyle` objects

That cost is small per theme but completely unnecessary for content the library already controls
at build time. This module shifts that work into the Gradle build so the runtime cost for the
eight built-ins drops to "read a static field."

## What it produces

A single Kotlin source file emitted at:

```
compose-highlight/build/generated/source/themes/main/
└── dev/hossain/highlight/engine/
    └── GeneratedThemes.kt
```

Shape (truncated):

```kotlin
internal object GeneratedThemes {
    const val TOMORROW_IDENTITY: Long = 4492418159385501631L
    val TOMORROW: Map<String, SpanStyle> = mapOf(
        "hljs"         to SpanStyle(color = Color(0xFF4D4D4C), background = Color(0xFFFFFFFF)),
        "hljs-comment" to SpanStyle(color = Color(0xFF8E908C)),
        // ~40 more entries per theme
    )
    // x8 themes
}
```

The file is gitignored (lives under `build/`), regenerated whenever any source CSS changes, and
compiled into the published AAR alongside the rest of the library code.

## Pipeline

```
Source CSS (committed)        compose-highlight/src/main/assets/compose-highlight/themes/*.css
        |
        v   :compose-highlight:generateThemes  (cacheable Gradle task)
        |
        +-- buildSrc/CssThemeParser     mirrors runtime ThemeParser, returns ParsedStyle
        +-- buildSrc/ThemeSourceEmitter renders Kotlin source with mapOf(SpanStyle(...))
        v
GeneratedThemes.kt (gitignored)   compose-highlight/build/generated/source/themes/main/...
        |
        v   Kotlin compile (wired via AGP variant Sources API)
        v
GeneratedThemes.class   classes.jar inside the published AAR
```

The CSS files still ship in the AAR (about 4 KB total) so the runtime `fromAsset(...)` path
keeps working if anyone references those asset paths directly. None of the eight built-in factory
methods reads them at runtime.

## Module layout

```
buildSrc/
├── build.gradle.kts                    Kotlin DSL plugin, JVM toolchain 17
├── settings.gradle.kts                 Avoids the "missing settings" warning under Gradle 9
└── src/main/kotlin/dev/hossain/highlight/build/
    ├── CssThemeParser.kt               JVM-only CSS parser (mirrors runtime ThemeParser)
    ├── ThemeSourceEmitter.kt           Renders Kotlin source from parsed entries
    └── GenerateThemesTask.kt           @CacheableTask Gradle task tying the two together
```

### `CssThemeParser`

A duplicate of the runtime parser's logic. It cannot share code with the runtime parser because:

- The runtime parser depends on `androidx.compose.ui.graphics.Color` and
  `androidx.compose.ui.text.SpanStyle`, which are Android types unavailable in a Gradle build
  classpath.
- The runtime parser returns Compose-shaped objects we would need to consume; the build-time
  parser produces an intermediate (`ParsedStyle`) that the emitter renders as Kotlin source.

The duplication is intentional and load-bearing. Both parsers must produce semantically identical
output for the four bundled themes. The parity test
(`compose-highlight/.../GeneratedThemesParityTest.kt`) is the only thing keeping them in sync.

### `ThemeSourceEmitter`

Walks `List<Pair<String, ParsedStyle>>` and renders Kotlin source as a string. Output choices:

- Plain `mapOf(...)` literals over more compact representations because they are debuggable in
  the IDE, easy to diff when CSS changes, and bytecode size is trivial at this scale.
- Source order preserved (`CssThemeParser` uses `LinkedHashMap`) so generated diffs match CSS
  ordering.
- Colors rendered as `Color(0xAARRGGBB)` to match the form used elsewhere in the codebase.
- The file starts with `@file:Suppress("ktlint", "detekt:all", "RedundantSuppression")` so
  generated code does not need to satisfy formatting rules. Style of generated code is owned
  by the emitter, not by ktlint.

### `GenerateThemesTask`

A `@CacheableTask` with `PathSensitivity.RELATIVE` inputs:

- `cssDir: DirectoryProperty` (input, the assets/themes directory)
- `outputDir: DirectoryProperty` (output)

The task body is straightforward: iterate the hardcoded `THEME_INPUTS` list, parse each named
file, build a `ThemeBuildInput`, and pass the lot to `ThemeSourceEmitter.emit(...)`.

Wired into `compose-highlight/build.gradle.kts` via the AGP variant Sources API:

```kotlin
androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(
            generateThemes,
            GenerateThemesTask::outputDir,
        )
    }
}
```

`addGeneratedSourceDirectory` takes the task provider directly, so AGP automatically declares
the dependency on `generateThemes` for every consumer (`compileKotlin`, `lintAnalyze`,
`extractAnnotations`, `dokka`, etc.). Earlier attempts using `tasks.matching { ... }.dependsOn`
left lint and annotation extraction without the dependency declared, breaking
`assembleRelease` in CI.

## Theme inventory

The codegen task does **not** auto-discover CSS files. It iterates a hardcoded list inside
`GenerateThemesTask.kt`:

```kotlin
val THEME_INPUTS = listOf(
    "TOMORROW"        to "tomorrow.css",
    "TOMORROW_NIGHT"  to "tomorrow-night.css",
    "ATOM_ONE_DARK"   to "atom-one-dark.css",
    "ATOM_ONE_LIGHT"  to "atom-one-light.css",
)
```

First entry is the Kotlin constant name in `GeneratedThemes`. Second is the filename inside
`cssDir`.

### What happens when you drop a new CSS file

The task's `@InputFiles` covers the entire `cssDir`, so up-to-date checks see the new file and
re-run the task. But `THEME_INPUTS` only references the four it knows about, so nothing happens
for the new file:

```
compose-highlight/src/main/assets/compose-highlight/themes/
├── tomorrow.css         in THEME_INPUTS, gets compiled
├── tomorrow-night.css   in THEME_INPUTS, gets compiled
├── atom-one-dark.css    in THEME_INPUTS, gets compiled
├── atom-one-light.css   in THEME_INPUTS, gets compiled
└── dracula.css          NEW: ships in the AAR, ignored at build time
```

The new CSS still ships in the AAR (because the asset folder is bundled wholesale) so it is
still loadable at runtime via:

```kotlin
HighlightTheme.fromAsset(context, "compose-highlight/themes/dracula.css", "dracula")
```

It just does not get a precompiled constant or a factory method.

### Adding a fifth built-in (manual, by design)

Five places to update for `dracula`:

1. Drop the CSS at
   `compose-highlight/src/main/assets/compose-highlight/themes/dracula.css`.
2. Add to `THEME_INPUTS` in `GenerateThemesTask.kt`:

   ```kotlin
   "DRACULA" to "dracula.css",
   ```

3. Add the factory in
   `compose-highlight/src/main/kotlin/.../engine/HighlightTheme.kt`:

   ```kotlin
   fun dracula(): HighlightTheme =
       HighlightTheme(
           name = "dracula",
           colorMapProvider = { GeneratedThemes.DRACULA },
           contentIdentity = GeneratedThemes.DRACULA_IDENTITY,
       )
   ```

4. Add `rememberDraculaTheme()` in
   `compose-highlight/src/main/kotlin/.../ui/HighlightThemeComposables.kt`.
5. Add a parity test entry in
   `compose-highlight/src/test/kotlin/.../engine/GeneratedThemesParityTest.kt`.

The friction is intentional. A new built-in is an API decision (factory shape, naming, light /
dark pairing) and not "I dropped a file in the folder." Themes that should be available to
consumers but do not warrant a built-in factory can stay as plain CSS in the assets folder and
ship via `fromAsset`.

## Parity safety net

`compose-highlight/src/test/kotlin/.../engine/GeneratedThemesParityTest.kt` is the only thing
keeping the buildSrc parser and the runtime parser in sync. It runs as part of
`testDebugUnitTest` and asserts:

1. **Color map equality.** For each of the four bundled themes, `ThemeParser.parseAsset(...)` at
   test time produces a `Map<String, SpanStyle>` byte-identical to the precompiled
   `GeneratedThemes.*` constant.
2. **Identity hash equality.** Each built-in factory's `HighlightTheme` equals
   `HighlightTheme.fromAsset(context, sameAssetPath, sameName)`. Equality compares
   `(name, contentIdentity)`, so this exercises the buildSrc-vs-runtime hash equivalence
   directly. If either side ever drifts, the test fails.

When the parity test fails, the fix depends on which parser changed:

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| You edited the runtime parser, parity test fails | Generated file is stale | `./gradlew :compose-highlight:generateThemes` |
| You edited the buildSrc parser, parity test fails | buildSrc parser drifted from the runtime parser | Fix the divergence in buildSrc, do not edit the test |
| You added a CSS rule the runtime parser handles differently than buildSrc | Behavior gap between the two parsers | Decide which behavior is correct, port to the other |

## Manual invocation

```bash
# Regenerate (idempotent if inputs unchanged thanks to up-to-date checks)
./gradlew :compose-highlight:generateThemes

# Force regenerate
./gradlew :compose-highlight:generateThemes --rerun-tasks

# Inspect the output
cat compose-highlight/build/generated/source/themes/main/dev/hossain/highlight/engine/GeneratedThemes.kt
```

The task is `@CacheableTask` so output is reproducible across machines and CI agents.

## Why not a full CSS parsing library?

Considered and rejected during the audit (see PR #203 for the parser refactor and PR #204 for
the precompilation refactor). Real CSS parsers like ph-css would add 500 KB to 1 MB to the
compile classpath. hljs themes use about 5% of CSS, and the test corpus (17 community themes)
exercises the parser broadly enough to catch real bugs. The recursive-descent parser is small
enough to maintain in two places and the parity test catches divergence automatically.
