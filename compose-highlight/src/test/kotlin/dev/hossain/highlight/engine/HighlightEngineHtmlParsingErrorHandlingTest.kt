package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.internal.withHtmlParsingErrorHandling
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [withHtmlParsingErrorHandling] - the error-mapping helper used by
 * [HighlightEngine.highlight], [HighlightEngine.highlightBothThemes], and
 * [HighlightEngine.highlightAuto] inside their `withContext(Dispatchers.Default)` blocks.
 *
 * These tests run on the JVM (no WebView or device required) by exercising the helper directly
 * with controlled exception inputs. They verify that:
 * - [CancellationException] is rethrown to respect structured concurrency
 * - Arbitrary [Exception]s are wrapped in [HighlightException.HtmlParseFailed]
 * - Successful blocks return [Result.success] with the value
 */
class HighlightEngineHtmlParsingErrorHandlingTest {
    // ----- CancellationException rethrown -----

    @Test
    fun `CancellationException is rethrown, not converted to HtmlParseFailed`() =
        runTest {
            var result: Result<Unit>? = null
            try {
                result = withHtmlParsingErrorHandling { throw CancellationException("cancelled") }
            } catch (e: CancellationException) {
                // Expected: exception propagated out instead of being swallowed as Result.failure
            }

            assertThat(result).isNull()
        }

    @Test
    fun `CancellationException identity is preserved when rethrown`() =
        runTest {
            val cause = CancellationException("test cancellation")
            var caught: CancellationException? = null
            try {
                withHtmlParsingErrorHandling<Unit> { throw cause }
            } catch (e: CancellationException) {
                caught = e
            }

            assertThat(caught).isSameInstanceAs(cause)
        }

    // ----- Exception wrapped in HtmlParseFailed -----

    @Test
    fun `generic Exception is wrapped in HtmlParseFailed`() =
        runTest {
            val cause = RuntimeException("jsoup parse error")

            val result = withHtmlParsingErrorHandling<Unit> { throw cause }

            assertThat(result.isFailure).isTrue()
            val ex = result.exceptionOrNull()
            assertThat(ex).isInstanceOf(HighlightException.HtmlParseFailed::class.java)
            assertThat(ex?.cause).isSameInstanceAs(cause)
        }

    @Test
    fun `IllegalStateException is wrapped in HtmlParseFailed`() =
        runTest {
            val result = withHtmlParsingErrorHandling<Unit> { throw IllegalStateException("bad state") }

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(HighlightException.HtmlParseFailed::class.java)
        }

    @Test
    fun `NullPointerException is wrapped in HtmlParseFailed`() =
        runTest {
            val result = withHtmlParsingErrorHandling<Unit> { throw NullPointerException("null ref") }

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(HighlightException.HtmlParseFailed::class.java)
        }

    @Test
    fun `HtmlParseFailed cause is the original exception`() =
        runTest {
            val cause = RuntimeException("original")

            val result = withHtmlParsingErrorHandling<Unit> { throw cause }

            val ex = result.exceptionOrNull() as? HighlightException.HtmlParseFailed
            assertThat(ex).isNotNull()
            assertThat(ex!!.cause).isSameInstanceAs(cause)
        }

    // ----- success path -----

    @Test
    fun `successful block returns Result success with the value`() =
        runTest {
            val result = withHtmlParsingErrorHandling { Result.success(42) }

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isEqualTo(42)
        }

    @Test
    fun `successful block returning Result failure preserves the failure`() =
        runTest {
            val ex = HighlightException.HtmlParseFailed(RuntimeException("pre-wrapped"))

            val result = withHtmlParsingErrorHandling<Unit> { Result.failure(ex) }

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isSameInstanceAs(ex)
        }
}
