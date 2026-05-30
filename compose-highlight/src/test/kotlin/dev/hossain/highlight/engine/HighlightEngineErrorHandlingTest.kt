package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test

/**
 * Unit tests for [withEngineErrorHandling] - the shared exception-mapping helper used by
 * [HighlightEngine.highlightToHtml], [HighlightEngine.supportedLanguages], and
 * [HighlightEngine.highlightJsVersion].
 *
 * These tests run on the JVM (no WebView or device required) by exercising the helper directly
 * with controlled exception inputs. They verify that:
 * - [TimeoutCancellationException] is mapped to [HighlightException.Timeout] (not [HighlightException.JsExecutionFailed])
 * - [CancellationException] is rethrown to respect structured concurrency (not silently swallowed)
 * - [HighlightException] subtypes are preserved as-is
 * - Arbitrary [Exception]s are wrapped in [HighlightException.JsExecutionFailed]
 * - Successful blocks return [Result.success]
 */
class HighlightEngineErrorHandlingTest {
    // ── TimeoutCancellationException → Timeout ────────────────────────────────

    @Test
    fun `TimeoutCancellationException is mapped to HighlightException Timeout`() =
        runTest {
            // Generate a real TimeoutCancellationException via withTimeout so it carries the
            // correct coroutine context expected by the Kotlin coroutines runtime.
            var tce: TimeoutCancellationException? = null
            try {
                withTimeout(1L) { delay(10_000L) }
            } catch (e: TimeoutCancellationException) {
                tce = e
            }
            requireNotNull(tce) { "Expected TimeoutCancellationException to be thrown" }

            val result = withEngineErrorHandling<Unit> { throw tce }

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(HighlightException.Timeout::class.java)
        }

    @Test
    fun `Timeout result is not a JsExecutionFailed wrapping a TimeoutCancellationException`() =
        runTest {
            var tce: TimeoutCancellationException? = null
            try {
                withTimeout(1L) { delay(10_000L) }
            } catch (e: TimeoutCancellationException) {
                tce = e
            }
            requireNotNull(tce)

            val result = withEngineErrorHandling<Unit> { throw tce }

            assertThat(result.exceptionOrNull()).isNotInstanceOf(HighlightException.JsExecutionFailed::class.java)
        }

    // ── CancellationException → rethrown ──────────────────────────────────────

    @Test
    fun `CancellationException is rethrown, not converted to Result failure`() {
        // withEngineErrorHandling must rethrow CancellationException to respect structured
        // concurrency. We verify by catching it outside and confirming result was never set.
        var result: Result<Unit>? = null
        try {
            kotlinx.coroutines.runBlocking {
                result = withEngineErrorHandling { throw CancellationException("parent cancelled") }
            }
        } catch (e: CancellationException) {
            // Expected: the exception propagated out instead of being swallowed as Result.failure
        }

        assertThat(result).isNull()
    }

    @Test
    fun `CancellationException cause is not lost when rethrown`() {
        val cause = CancellationException("test cancellation")
        var caught: CancellationException? = null
        try {
            kotlinx.coroutines.runBlocking {
                withEngineErrorHandling<Unit> { throw cause }
            }
        } catch (e: CancellationException) {
            caught = e
        }

        assertThat(caught).isSameInstanceAs(cause)
    }

    // ── HighlightException → preserved ───────────────────────────────────────

    @Test
    fun `HighlightException JsExecutionFailed is preserved as Result failure`() =
        runTest {
            val ex = HighlightException.JsExecutionFailed(RuntimeException("js error"))

            val result = withEngineErrorHandling<Unit> { throw ex }

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isSameInstanceAs(ex)
        }

    @Test
    fun `HighlightException WebViewInitFailed is preserved as Result failure`() =
        runTest {
            val ex = HighlightException.WebViewInitFailed(RuntimeException("init failure"))

            val result = withEngineErrorHandling<Unit> { throw ex }

            assertThat(result.exceptionOrNull()).isSameInstanceAs(ex)
        }

    @Test
    fun `HighlightException HtmlParseFailed is preserved as Result failure`() =
        runTest {
            val ex = HighlightException.HtmlParseFailed(RuntimeException("parse error"))

            val result = withEngineErrorHandling<Unit> { throw ex }

            assertThat(result.exceptionOrNull()).isSameInstanceAs(ex)
        }

    // ── generic Exception → JsExecutionFailed ─────────────────────────────────

    @Test
    fun `generic Exception is wrapped in JsExecutionFailed`() =
        runTest {
            val cause = RuntimeException("unexpected failure")

            val result = withEngineErrorHandling<Unit> { throw cause }

            assertThat(result.isFailure).isTrue()
            val ex = result.exceptionOrNull()
            assertThat(ex).isInstanceOf(HighlightException.JsExecutionFailed::class.java)
            assertThat(ex?.cause).isSameInstanceAs(cause)
        }

    @Test
    fun `IllegalStateException is wrapped in JsExecutionFailed`() =
        runTest {
            val result = withEngineErrorHandling<Unit> { throw IllegalStateException("bad state") }

            assertThat(result.exceptionOrNull()).isInstanceOf(HighlightException.JsExecutionFailed::class.java)
        }

    // ── success path ──────────────────────────────────────────────────────────

    @Test
    fun `successful block returns Result success with the value`() =
        runTest {
            val result = withEngineErrorHandling { Result.success(42) }

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isEqualTo(42)
        }

    @Test
    fun `successful block returning Result failure preserves the failure`() =
        runTest {
            val ex = HighlightException.ThemeNotFound("themes/missing.css")

            val result = withEngineErrorHandling<Unit> { Result.failure(ex) }

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isSameInstanceAs(ex)
        }
}
