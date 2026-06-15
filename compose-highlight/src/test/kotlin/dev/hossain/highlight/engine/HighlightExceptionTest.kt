package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * JVM unit tests for the [HighlightException] sealed class hierarchy.
 *
 * Verifies that each exception variant (WebViewInitFailed, JsExecutionFailed,
 * ThemeNotFound, HtmlParseFailed, Timeout) is a proper HighlightException
 * subtype, carries the correct message, and preserves or nulls the cause
 * as designed.
 */
class HighlightExceptionTest {
    // ----- WebViewInitFailed -----

    @Test
    fun `WebViewInitFailed is a HighlightException`() {
        val ex = HighlightException.WebViewInitFailed(RuntimeException("init"))
        assertThat(ex).isInstanceOf(HighlightException::class.java)
    }

    @Test
    fun `WebViewInitFailed has correct message`() {
        val ex = HighlightException.WebViewInitFailed(RuntimeException("init"))
        assertThat(ex.message).isEqualTo("WebView initialization failed")
    }

    @Test
    fun `WebViewInitFailed preserves cause`() {
        val cause = RuntimeException("root cause")
        val ex = HighlightException.WebViewInitFailed(cause)
        assertThat(ex.cause).isEqualTo(cause)
    }

    // ----- JsExecutionFailed -----

    @Test
    fun `JsExecutionFailed is a HighlightException`() {
        val ex = HighlightException.JsExecutionFailed(RuntimeException("js"))
        assertThat(ex).isInstanceOf(HighlightException::class.java)
    }

    @Test
    fun `JsExecutionFailed has correct message`() {
        val ex = HighlightException.JsExecutionFailed(RuntimeException("js"))
        assertThat(ex.message).isEqualTo("JavaScript execution failed")
    }

    @Test
    fun `JsExecutionFailed preserves cause`() {
        val cause = RuntimeException("null result")
        val ex = HighlightException.JsExecutionFailed(cause)
        assertThat(ex.cause).isEqualTo(cause)
    }

    // ----- ThemeNotFound -----

    @Test
    fun `ThemeNotFound is a HighlightException`() {
        val ex = HighlightException.ThemeNotFound("themes/test.css")
        assertThat(ex).isInstanceOf(HighlightException::class.java)
    }

    @Test
    fun `ThemeNotFound message contains the path`() {
        val path = "compose-highlight/themes/missing.css"
        val ex = HighlightException.ThemeNotFound(path)
        assertThat(ex.message).contains(path)
    }

    @Test
    fun `ThemeNotFound message explains the parse failure`() {
        val ex = HighlightException.ThemeNotFound("compose-highlight/themes/missing.css")
        assertThat(ex.message).startsWith("Theme CSS has no parseable color rules")
    }

    @Test
    fun `ThemeNotFound has null cause`() {
        val ex = HighlightException.ThemeNotFound("any/path.css")
        assertThat(ex.cause).isNull()
    }

    // ----- HtmlParseFailed -----

    @Test
    fun `HtmlParseFailed is a HighlightException`() {
        val ex = HighlightException.HtmlParseFailed(RuntimeException("parse"))
        assertThat(ex).isInstanceOf(HighlightException::class.java)
    }

    @Test
    fun `HtmlParseFailed has correct message`() {
        val ex = HighlightException.HtmlParseFailed(RuntimeException("parse"))
        assertThat(ex.message).isEqualTo("HTML parsing failed")
    }

    @Test
    fun `HtmlParseFailed preserves cause`() {
        val cause = IllegalStateException("bad html")
        val ex = HighlightException.HtmlParseFailed(cause)
        assertThat(ex.cause).isEqualTo(cause)
    }

    // ----- Timeout -----

    @Test
    fun `Timeout is a HighlightException`() {
        val ex = HighlightException.Timeout()
        assertThat(ex).isInstanceOf(HighlightException::class.java)
    }

    @Test
    fun `Timeout message mentions the timeout duration`() {
        val ex = HighlightException.Timeout()
        assertThat(ex.message).contains(HighlightException.TIMEOUT_SECONDS.toString())
    }

    @Test
    fun `Timeout has null cause`() {
        val ex = HighlightException.Timeout()
        assertThat(ex.cause).isNull()
    }

    // ----- Shared contract -----

    @Test
    fun `TIMEOUT_SECONDS constant is positive`() {
        assertThat(HighlightException.TIMEOUT_SECONDS).isGreaterThan(0)
    }
}
