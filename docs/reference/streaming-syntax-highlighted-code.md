# StreamingSyntaxHighlightedCode

!!! warning "Experimental API"
    This composable is annotated with [`@ExperimentalHighlightApi`](#opting-in).
    The API surface may change without a deprecation cycle.

`StreamingSyntaxHighlightedCode` is a syntax-highlighted code block composable tailored specifically
for real-time and streaming code (such as LLM responses, terminal logs, and dynamic code generation).
As new tokens arrive, newly appended text renders immediately with 0 ms UI lag while existing lines
retain their syntax colors through span-transfer snapshotting.

Full API in Dokka:

- [`StreamingSyntaxHighlightedCode`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/-streaming-syntax-highlighted-code.html)
- [`rememberStreamingHighlightedCode`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-streaming-highlighted-code.html)
- [`StreamingSyntaxHighlightedCodeDefaults`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/-streaming-syntax-highlighted-code-defaults/index.html)
- [`ExperimentalHighlightApi`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/-experimental-highlight-api/index.html)

## When to use it

- **LLM & AI Chat Streaming:** You are streaming code block responses from an AI model (e.g. Gemini, OpenAI, Claude) token-by-token.
- **Live Logs & Telemetry:** You are displaying incoming code or formatted logs in real time.
- **Rapidly Updating Text:** The input string updates multiple times per second (15-40+ Hz).

For static documentation, guide pages, or fixed code snippets, use [`SyntaxHighlightedCode`](syntax-highlighted-code.md) instead.

## How it works

Unlike `SyntaxHighlightedCode` (which uses fade-in animations and resets in-flight jobs on each string change), `StreamingSyntaxHighlightedCode` uses a **span-transfer snapshot pipeline**:

```
┌───────────────────────────────────────────────────────────────┐
│ Stream Update: New token arrives (15-40 Hz)                   │
└───────────────────────────────┬───────────────────────────────┘
                                │
               ┌────────────────┴────────────────┐
               ▼                                 ▼
┌───────────────────────────────┐ ┌─────────────────────────────┐
│ 1. Instant UI Render          │ │ 2. Debounced Engine Run     │
│ (0 ms latency, 60 fps)        │ │ (Defaults: 200 ms)          │
│                               │ │                             │
│ Render current text with      │ │ Coalesce tokens and execute │
│ spans transferred from the    │ │ WebView highlight when the  │
│ previous snapshot.            │ │ stream pauses or finishes.  │
│                               │ │                             │
│ (Prior lines stay styled;     │ │ When ready, update snapshot │
│ new tokens render instantly)  │ │ with fresh full spans.      │
└───────────────────────────────┘ └─────────────────────────────┘
```

1. **Zero UI Latency:** The composable renders the current text immediately on every frame.
2. **Span Preservation:** Syntax styling on unchanged prefixes (all previous lines) is carried forward seamlessly.
3. **Newline-Aware Progressive Backfilling:** When `triggerOnNewline` is enabled (default `true`), completing a line (`\n`) triggers a background highlight run (throttled by `minThrottleMs = 150L`), progressively snapping finished lines to full syntax colors while subsequent tokens continue streaming.
4. **Engine Debouncing:** Idle pauses are debounced (`debounceMs = 200L`), ensuring fast token streams do not overload the underlying JavaScript engine.
5. **Streaming-Aware Scroll:** Horizontal scroll position is preserved when text is appended, preventing jarring scroll resets while the user is reading streaming output.

## Key parameters

- `code` - The current source code string (growing dynamically or static).
- `language` - Highlight.js language identifier (e.g. `"kotlin"`, `"python"`, `"json"`).
- `theme` - Active theme, defaults to `LocalHighlightTheme.current`.
- `style` - Visual style configuration (`CodeBlockStyle`).
- `showLineNumbers` - Whether to show the line number gutter on the left.
- `debounceMs` - Delay in milliseconds to wait after the last token before triggering an idle highlight call. Defaults to `StreamingSyntaxHighlightedCodeDefaults.DEBOUNCE_MS` (200 ms).
- `triggerOnNewline` - Whether to trigger a background highlight run when a new newline (`\n`) is detected in the stream, progressively styling completed lines. Defaults to `true`.
- `minThrottleMs` - Minimum interval in milliseconds between consecutive newline-triggered highlight runs. Defaults to `StreamingSyntaxHighlightedCodeDefaults.MIN_THROTTLE_MS` (150 ms).
- `scrollState` - Hoisted horizontal `ScrollState`.
- `languageLabel` - Optional composable slot for the language badge in the header (`null` to hide).
- `copyButton` - Optional composable slot for the copy button in the header (`null` to hide).
- `onCopyClick` - Optional callback when the copy button is clicked.
- `onHighlightComplete` - Optional callback invoked with `HighlightResult` on successful highlight cycle.
- `onError` - Optional callback invoked with `HighlightException` on failure.

## Opting in

`StreamingSyntaxHighlightedCode`, `rememberStreamingHighlightedCode`, and `StreamingSyntaxHighlightedCodeDefaults` are annotated with `@ExperimentalHighlightApi`:

```kotlin
// Option 1 - opt in at the call site
@OptIn(ExperimentalHighlightApi::class)
@Composable
fun StreamingResponseScreen() {
    StreamingSyntaxHighlightedCode(...)
}

// Option 2 - propagate to your own API
@ExperimentalHighlightApi
@Composable
fun MyChatBubble(...) {
    StreamingSyntaxHighlightedCode(...)
}
```

## Basic usage

```kotlin
@OptIn(ExperimentalHighlightApi::class)
@Composable
fun ChatCodeSnippet(
    streamedCode: String,
    language: String,
) {
    HighlightThemeProvider(
        lightHighlightTheme = rememberTomorrowLightTheme(),
        darkHighlightTheme  = rememberAtomOneDarkTheme(),
    ) {
        StreamingSyntaxHighlightedCode(
            code            = streamedCode,
            language        = language,
            showLineNumbers = true,
        )
    }
}
```

## Streaming from a ViewModel

```kotlin
@OptIn(ExperimentalHighlightApi::class)
@Composable
fun StreamingScreen(viewModel: ChatViewModel = viewModel()) {
    val streamedCode by viewModel.codeFlow.collectAsState(initial = "")

    StreamingSyntaxHighlightedCode(
        code            = streamedCode,
        language        = "kotlin",
        showLineNumbers = true,
        debounceMs      = StreamingSyntaxHighlightedCodeDefaults.DEBOUNCE_MS,
    )
}
```

## Lower-level helper for custom layouts

If you want the highlighted `AnnotatedString` without the standard code block container, use `rememberStreamingHighlightedCode`:

```kotlin
@OptIn(ExperimentalHighlightApi::class)
@Composable
fun CustomStreamingText(code: String, language: String) {
    val annotatedString = rememberStreamingHighlightedCode(
        code       = code,
        language   = language,
        debounceMs = 200L,
    )

    Text(
        text       = annotatedString,
        fontFamily = FontFamily.Monospace,
    )
}
```
