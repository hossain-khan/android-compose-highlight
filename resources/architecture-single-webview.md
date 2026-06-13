# Architecture: Single-WebView Sharing Under `HighlightThemeProvider`

How `compose-highlight` keeps one hidden `WebView` per screen even when many `SyntaxHighlightedCode` blocks are on it — and the routing logic that makes the same library work seamlessly without a provider.

## The layers, top to bottom

```
HighlightThemeProvider                       (Composable, public)
  ├── remember { HighlightEngine(appCtx) }   one engine per provider
  ├── DisposableEffect(engine) onDispose { engine.destroy() }
  └── CompositionLocalProvider {
        LocalHighlightEngine provides engine    ← internal
        LocalHighlightTheme  provides theme     ← public
        ...
        content()
      }

      └── content tree (descendants)
            └── SyntaxHighlightedCode / SyntaxHighlightedTextEditor
                  └── rememberHighlightedCode / rememberSyntaxHighlightedEditorValue
                        └── rememberHighlightEngine()  ← the routing point

                            HighlightEngine                    (1 instance)
                              ├── Mutex                        serializes JS calls
                              └── WebViewManager
                                    ├── WebView (singleton)    1 hidden view
                                    └── readyDeferred          page-load gate
```

## Key insight: the routing happens in `rememberHighlightEngine()`

`RememberHighlightEngine.kt:37-53` is the whole sharing mechanism. Two paths:

**Inside a provider:**

```kotlin
val sharedEngine = LocalHighlightEngine.current  // non-null
return sharedEngine ?: standaloneEngine!!         // returns shared
```

- `LocalHighlightEngine` is non-null because `HighlightThemeProvider` provided it.
- The `remember(sharedEngine) { if (sharedEngine == null) ... else null }` block returns `null` for the standalone — no engine is created.
- `DisposableEffect(standaloneEngine)` keys on `null`, so its `onDispose` is a no-op.
- All callers in the subtree get the same engine instance back.

**Outside a provider:**

```kotlin
val sharedEngine = LocalHighlightEngine.current  // null (default)
val standaloneEngine = remember(sharedEngine) { HighlightEngine(appCtx) }
return sharedEngine ?: standaloneEngine!!  // returns standalone
```

- Each call site creates its own engine, with its own WebView.
- `DisposableEffect` destroys the standalone engine when the call site leaves composition.

## How the single WebView gets created and serialized

**Creation is lazy** (`HighlightEngine` constructor):

```kotlin
class HighlightEngine(context: Context) : Closeable {
    private val manager = WebViewManager(context.applicationContext)
    private val mutex = Mutex()
    // No WebView yet. Just a lazy holder + mutex.
}
```

The actual `WebView(context)` allocation happens on the first `highlight()` call inside `manager.initialize()` (`WebViewManager.kt:97-157`), dispatched to `Dispatchers.Main`. After that, `manager.getReadyWebView()` suspends until `bridge.html` finishes loading (`onPageFinished` completes `readyDeferred`).

**Serialization** (`HighlightEngine.highlightToHtml`, lines 234-250):

```kotlin
manager.initialize()                       // idempotent; runs once
val webView = manager.getReadyWebView()    // suspends until ready

mutex.withLock {                           // 1 JS call at a time
    withTimeout(TIMEOUT_SECONDS * 1000L) {
        executeJs(webView, code, language) // evaluateJavascript on Main
    }
}
```

The mutex is what makes "1 WebView serving N composables" safe. WebView itself can only handle one `evaluateJavascript` at a time, so the engine queues calls; concurrent `highlight()` invocations from different composables in the subtree don't race.

## Theme handling in the same composition

The provider also publishes themes via the same composition-local mechanism, on top of the engine sharing:

```kotlin
val activeTheme = if (darkTheme) darkHighlightTheme else lightHighlightTheme
CompositionLocalProvider(
    LocalHighlightTheme provides activeTheme,
    LocalLightHighlightTheme provides lightHighlightTheme,
    LocalDarkHighlightTheme provides darkHighlightTheme,
    LocalHighlightEngine provides engine,
) { content() }
```

When `darkTheme` flips (system dark mode toggle), `HighlightThemeProvider` itself recomposes, `activeTheme` is reassigned, and `CompositionLocalProvider` re-provides. The engine doesn't change — same WebView, same mutex — but every descendant's `rememberHighlightedCode(code, language, theme)` re-runs its `LaunchedEffect(code, language, theme)` and re-highlights against the new theme.

## Lifecycle: who owns the WebView

The `DisposableEffect(engine) { onDispose { engine.destroy() } }` at line 169 is the only place the engine gets destroyed. `engine.destroy()` calls `WebViewManager.destroy()`, which:

1. Sets `webView = null` (release the strong reference).
2. Cancels any pending `readyDeferred` (so suspended `getReadyWebView()` callers wake up with cancellation).
3. Resets `readyDeferred` to a fresh deferred (so a future re-init works).
4. Posts `wv.destroy()` to the Main thread (WebView destroy must run on its creating thread).

This fires when the provider leaves composition — typically when the screen is destroyed. The "1 WebView per screen" property comes from this scoping: each screen-level provider has its own engine with its own WebView, destroyed independently.

## Why `staticCompositionLocalOf` is correct here

`LocalHighlightEngine` uses `staticCompositionLocalOf` (`internal/LocalHighlightEngine.kt:11`) because:

- The engine instance literally never changes for the lifetime of a provider — `remember { HighlightEngine(...) }` keys on nothing.
- `static` means reading `LocalHighlightEngine.current` doesn't subscribe the reader to recomposition. That's correct: there's nothing to recompose for; the engine reference is stable forever.
- Same logic applies to `LocalHighlightTheme` — when the theme changes, the **provider** recomposes (because `darkTheme` is a parameter of the provider's caller), and `content()` is invoked again with the new value. Static-vs-mutable composition local doesn't matter for that path; what matters is that the provider itself recomposes.

## What this gives you, measured

From the provider KDoc at `HighlightThemeProvider.kt:71-73`:

> Pixel 8 Pro debug build, cold start, 17 blocks:
> - With provider: ~224 ms total, ~15 MB heap (1 WebView)
> - Without provider: ~866 ms total, ~34 MB heap (17 WebViews)
>
> Roughly 4× faster, 55% less heap.

The savings come from:

1. **One WebView allocation** instead of N — each WebView is a heavy Android view with its own renderer process (or shared Chromium process with its own page state).
2. **One bridge.html parse + highlight.min.js load** instead of N — the JS engine warm-up is the bulk of first-call latency.
3. **One persistent Chromium connection** — subsequent JS calls reuse the warm V8 isolate.

## The four "shapes" of consumption

| Caller | Engine source |
|---|---|
| `SyntaxHighlightedCode` inside provider | shared (provider's) |
| `SyntaxHighlightedCode` outside provider | standalone (per-call-site) |
| `rememberHighlightedCode` (and friends) inside provider | shared (via `rememberHighlightEngine`) |
| `rememberHighlightedCode` outside provider | standalone (per-call-site) |

All four go through `rememberHighlightEngine()` as the single decision point. That's why the provider works: just publishing `LocalHighlightEngine.provides(engine)` is enough — every public composable in the library opts into sharing automatically by calling `rememberHighlightEngine()` instead of constructing their own engine.

## What you'd want to know if you change this

- **Don't move engine creation out of `HighlightThemeProvider`.** Anything else (singletons, `Application`-scoped engines) breaks the screen-leaves → WebView-destroyed property and starts leaking renderers.
- **The mutex in `HighlightEngine` is essential** for the shared case. With N composables hitting the same engine, you absolutely need serialization or `evaluateJavascript` calls clobber each other.
- **`LocalHighlightEngine` is `internal`** — that's deliberate. External code can only get the engine via `rememberHighlightEngine()`, which enforces the routing logic. If it became public, callers could provide their own engine and bypass the lifecycle scoping.
- **Theme changes don't restart the engine** — they only re-trigger highlight calls in descendants. The WebView and its loaded `bridge.html` survive theme toggles, which is why dark/light mode switching is fast.

## Sequence: a single highlight call inside a provider

```
SyntaxHighlightedCode("val x = 42", "kotlin")
   │
   ├─ rememberHighlightedCode(code, language, theme)
   │     │
   │     ├─ rememberHighlightEngine()
   │     │     └─ returns sharedEngine from LocalHighlightEngine
   │     │
   │     └─ LaunchedEffect(code, language, theme):
   │           engine.highlight(code, language, theme)
   │             │
   │             ├─ manager.initialize()                   (idempotent; first call only)
   │             │   └─ withContext(Main) { new WebView(); load bridge.html }
   │             │       └─ onPageFinished: readyDeferred.complete(view)
   │             │
   │             ├─ manager.getReadyWebView()              suspend until ready
   │             │
   │             ├─ mutex.withLock {
   │             │     withTimeout(TIMEOUT) {
   │             │         executeJs(webView, code, language)    // Main + evaluateJavascript
   │             │     } → JsResult(html, durations)
   │             │   }
   │             │
   │             └─ withContext(Default) {
   │                   theme.timedColorMap()                       // CSS parse, lazy
   │                   HtmlToAnnotatedString.convert(html, colors) // custom parser
   │                   → HighlightResult(annotated, spans, ...)
   │                }
   │
   └─ Text(annotated)
```

## Files referenced

| File (`compose-highlight/src`) | Role |
|---|---|
| `highlight/ui/HighlightThemeProvider.kt` | Provider composable, owns engine and themes. |
| `ui/RememberHighlightEngine.kt` | Routing point (shared vs standalone). |
| `ui/internal/LocalHighlightEngine.kt` | Internal `staticCompositionLocalOf<HighlightEngine?>`, default `null`. |
| `engine/HighlightEngine.kt` | Engine: mutex + WebViewManager + JS bridge calls. |
| `engine/internal/WebViewManager.kt` | Single `WebView` lifecycle, init/destroy, `readyDeferred`. |
