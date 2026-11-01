package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * JVM unit tests for [CodeBlockStyle].
 *
 * Verifies preset values (Default, Compact), equality, copy behavior,
 * and custom parameter construction for the code block styling configuration.
 */
class CodeBlockStyleTest {
    @Test
    fun `default preset uses expected shape`() {
        assertThat(CodeBlockStyle.Default.shape).isEqualTo(SyntaxHighlightedCodeDefaults.shape)
    }

    @Test
    fun `default preset uses expected padding`() {
        assertThat(CodeBlockStyle.Default.padding).isEqualTo(SyntaxHighlightedCodeDefaults.padding)
    }

    @Test
    fun `default preset has unspecified lineNumberColor`() {
        assertThat(CodeBlockStyle.Default.lineNumberColor).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `compact preset has reduced padding`() {
        assertThat(CodeBlockStyle.Compact.padding).isEqualTo(PaddingValues(12.dp))
    }

    @Test
    fun `compact preset has reduced headerPadding`() {
        assertThat(CodeBlockStyle.Compact.headerPadding)
            .isEqualTo(PaddingValues(horizontal = 12.dp, vertical = 6.dp))
    }

    @Test
    fun `default and compact are not equal`() {
        assertThat(CodeBlockStyle.Default).isNotEqualTo(CodeBlockStyle.Compact)
    }

    @Test
    fun `copy with modified field preserves other fields`() {
        val custom = CodeBlockStyle.Default.copy(lineNumberColor = Color.Red)
        assertThat(custom.lineNumberColor).isEqualTo(Color.Red)
        assertThat(custom.shape).isEqualTo(CodeBlockStyle.Default.shape)
        assertThat(custom.padding).isEqualTo(CodeBlockStyle.Default.padding)
        assertThat(custom.textStyle).isEqualTo(CodeBlockStyle.Default.textStyle)
    }

    @Test
    fun `default preset has dark fallbackBackgroundColor`() {
        assertThat(CodeBlockStyle.Default.fallbackBackgroundColor)
            .isEqualTo(SyntaxHighlightedCodeDefaults.fallbackBackgroundColor)
    }

    @Test
    fun `default preset has light gray fallbackTextColor`() {
        assertThat(CodeBlockStyle.Default.fallbackTextColor)
            .isEqualTo(SyntaxHighlightedCodeDefaults.fallbackTextColor)
    }

    @Test
    fun `custom fallbackBackgroundColor is preserved in copy`() {
        val custom = CodeBlockStyle.Default.copy(fallbackBackgroundColor = Color.White)
        assertThat(custom.fallbackBackgroundColor).isEqualTo(Color.White)
    }

    @Test
    fun `custom fallbackTextColor is preserved in copy`() {
        val custom = CodeBlockStyle.Default.copy(fallbackTextColor = Color.Black)
        assertThat(custom.fallbackTextColor).isEqualTo(Color.Black)
    }

    @Test
    fun `custom style with all parameters`() {
        val custom =
            CodeBlockStyle(
                shape = RoundedCornerShape(4.dp),
                padding = PaddingValues(8.dp),
                headerPadding = PaddingValues(4.dp),
                lineNumberColor = Color.Gray,
                lineNumberWidth = 40.dp,
                copyButtonSize = 24.dp,
                textStyle = TextStyle(fontSize = 15.sp),
            )
        assertThat(custom.lineNumberWidth).isEqualTo(40.dp)
        assertThat(custom.copyButtonSize).isEqualTo(24.dp)
    }
}

/**
 * JVM unit tests for [SyntaxHighlightedCodeDefaults].
 *
 * Verifies default values for code text style, shape, padding, line number
 * width, copy button size, and fallback colors used by the syntax highlighting
 * composables.
 */
class SyntaxHighlightedCodeDefaultsTest {
    @Test
    fun `codeTextStyle uses monospace font`() {
        assertThat(SyntaxHighlightedCodeDefaults.codeTextStyle.fontFamily)
            .isEqualTo(FontFamily.Monospace)
    }

    @Test
    fun `codeTextStyle uses 13sp fontSize`() {
        assertThat(SyntaxHighlightedCodeDefaults.codeTextStyle.fontSize).isEqualTo(13.sp)
    }

    @Test
    fun `codeTextStyle uses 20sp lineHeight`() {
        assertThat(SyntaxHighlightedCodeDefaults.codeTextStyle.lineHeight).isEqualTo(20.sp)
    }

    @Test
    fun `shape is 8dp rounded`() {
        assertThat(SyntaxHighlightedCodeDefaults.shape).isEqualTo(RoundedCornerShape(8.dp))
    }

    @Test
    fun `padding is 16dp`() {
        assertThat(SyntaxHighlightedCodeDefaults.padding).isEqualTo(PaddingValues(16.dp))
    }

    @Test
    fun `lineNumberWidth is 32dp`() {
        assertThat(SyntaxHighlightedCodeDefaults.lineNumberWidth).isEqualTo(32.dp)
    }

    @Test
    fun `copyButtonSize is 32dp`() {
        assertThat(SyntaxHighlightedCodeDefaults.copyButtonSize).isEqualTo(32.dp)
    }

    @Test
    fun `fallbackBackgroundColor is dark`() {
        assertThat(SyntaxHighlightedCodeDefaults.fallbackBackgroundColor).isEqualTo(Color(0xFF1E1E1E))
    }

    @Test
    fun `fallbackTextColor is light gray`() {
        assertThat(SyntaxHighlightedCodeDefaults.fallbackTextColor).isEqualTo(Color(0xFFCCCCCC))
    }
}
