package dev.hossain.highlight.engine.internal

import dev.hossain.highlight.engine.HighlightException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

/**
 * Executes [block] and maps common exceptions to [Result] failure types, while correctly
 * propagating [CancellationException] for structured concurrency.
 *
 * - [TimeoutCancellationException] (from internal `withTimeout`) → [HighlightException.Timeout]
 * - [CancellationException] (parent scope cancellation) → rethrown
 * - [HighlightException] → preserved as [Result.failure]
 * - Any other [Exception] → wrapped in [HighlightException.JsExecutionFailed]
 *
 * This helper eliminates the repeated catch chain in
 * [dev.hossain.highlight.engine.HighlightEngine.highlightToHtml],
 * [dev.hossain.highlight.engine.HighlightEngine.supportedLanguages], and
 * [dev.hossain.highlight.engine.HighlightEngine.highlightJsVersion], and enables direct unit
 * testing of the exception-mapping logic without a real WebView.
 */
internal suspend fun <T> withEngineErrorHandling(block: suspend () -> Result<T>): Result<T> =
    try {
        block()
    } catch (e: TimeoutCancellationException) {
        Result.failure(HighlightException.Timeout())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HighlightException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(HighlightException.JsExecutionFailed(e))
    }

/**
 * Executes [block] and maps exceptions from the HTML parsing and theme resolution pipeline to
 * [Result] failure types, while correctly propagating [CancellationException].
 *
 * - [CancellationException] → rethrown (preserves structured concurrency)
 * - Any other [Exception] → wrapped in [HighlightException.HtmlParseFailed]
 *
 * Used by [dev.hossain.highlight.engine.HighlightEngine.highlight],
 * [dev.hossain.highlight.engine.HighlightEngine.highlightBothThemes], and
 * [dev.hossain.highlight.engine.HighlightEngine.highlightAuto] inside their
 * `withContext(Dispatchers.Default)` blocks to handle errors from theme resolution and HTML parser
 * conversion. Extracted as an internal helper to eliminate the repeated catch chain and enable
 * direct unit testing without a real WebView.
 */
internal suspend fun <T> withHtmlParsingErrorHandling(block: suspend () -> Result<T>): Result<T> =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(HighlightException.HtmlParseFailed(e))
    }
