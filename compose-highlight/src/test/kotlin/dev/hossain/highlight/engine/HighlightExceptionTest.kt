package dev.hossain.highlight.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightExceptionTest {
    // ── WebViewInitFailed ─────────────────────────────────────────────────────

    @Test
    fun `WebViewInitFailed is a HighlightException`() {
        val ex = HighlightException.WebViewInitFailed(RuntimeException("init"))
        assertTrue(ex is HighlightException)
    }

    @Test
    fun `WebViewInitFailed has correct message`() {
        val ex = HighlightException.WebViewInitFailed(RuntimeException("init"))
        assertEquals("WebView initialization failed", ex.message)
    }

    @Test
    fun `WebViewInitFailed preserves cause`() {
        val cause = RuntimeException("root cause")
        val ex = HighlightException.WebViewInitFailed(cause)
        assertEquals(cause, ex.cause)
    }

    // ── JsExecutionFailed ────────────────────────────────────────────────────

    @Test
    fun `JsExecutionFailed is a HighlightException`() {
        val ex = HighlightException.JsExecutionFailed(RuntimeException("js"))
        assertTrue(ex is HighlightException)
    }

    @Test
    fun `JsExecutionFailed has correct message`() {
        val ex = HighlightException.JsExecutionFailed(RuntimeException("js"))
        assertEquals("JavaScript execution failed", ex.message)
    }

    @Test
    fun `JsExecutionFailed preserves cause`() {
        val cause = RuntimeException("null result")
        val ex = HighlightException.JsExecutionFailed(cause)
        assertEquals(cause, ex.cause)
    }

    // ── ThemeNotFound ─────────────────────────────────────────────────────────

    @Test
    fun `ThemeNotFound is a HighlightException`() {
        val ex = HighlightException.ThemeNotFound("themes/test.css")
        assertTrue(ex is HighlightException)
    }

    @Test
    fun `ThemeNotFound message contains the path`() {
        val path = "compose-highlight/themes/missing.css"
        val ex = HighlightException.ThemeNotFound(path)
        assertTrue("Message should contain the path", ex.message?.contains(path) == true)
    }

    @Test
    fun `ThemeNotFound has null cause`() {
        val ex = HighlightException.ThemeNotFound("any/path.css")
        assertEquals(null, ex.cause)
    }

    // ── HtmlParseFailed ───────────────────────────────────────────────────────

    @Test
    fun `HtmlParseFailed is a HighlightException`() {
        val ex = HighlightException.HtmlParseFailed(RuntimeException("parse"))
        assertTrue(ex is HighlightException)
    }

    @Test
    fun `HtmlParseFailed has correct message`() {
        val ex = HighlightException.HtmlParseFailed(RuntimeException("parse"))
        assertEquals("HTML parsing failed", ex.message)
    }

    @Test
    fun `HtmlParseFailed preserves cause`() {
        val cause = IllegalStateException("bad html")
        val ex = HighlightException.HtmlParseFailed(cause)
        assertEquals(cause, ex.cause)
    }

    // ── Timeout ───────────────────────────────────────────────────────────────

    @Test
    fun `Timeout is a HighlightException`() {
        val ex = HighlightException.Timeout()
        assertTrue(ex is HighlightException)
    }

    @Test
    fun `Timeout message mentions the timeout duration`() {
        val ex = HighlightException.Timeout()
        assertTrue(
            "Timeout message should contain the timeout seconds",
            ex.message?.contains(HighlightException.TIMEOUT_SECONDS.toString()) == true,
        )
    }

    @Test
    fun `Timeout has null cause`() {
        val ex = HighlightException.Timeout()
        assertEquals(null, ex.cause)
    }

    // ── Shared contract ───────────────────────────────────────────────────────

    @Test
    fun `all variants are Exception subtypes`() {
        val all: List<Exception> =
            listOf(
                HighlightException.WebViewInitFailed(RuntimeException()),
                HighlightException.JsExecutionFailed(RuntimeException()),
                HighlightException.ThemeNotFound("path"),
                HighlightException.HtmlParseFailed(RuntimeException()),
                HighlightException.Timeout(),
            )
        all.forEach { ex ->
            assertTrue("${ex::class.simpleName} should be an Exception", ex is Exception)
            assertNotNull("${ex::class.simpleName} message should be non-null", ex.message)
        }
    }

    @Test
    fun `TIMEOUT_SECONDS constant is positive`() {
        assertTrue("TIMEOUT_SECONDS must be positive", HighlightException.TIMEOUT_SECONDS > 0)
    }
}
