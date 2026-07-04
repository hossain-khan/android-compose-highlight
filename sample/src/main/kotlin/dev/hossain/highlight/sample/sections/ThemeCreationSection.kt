package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.engine.HljsSelectors
import dev.hossain.highlight.sample.FROM_ASSET_SNIPPET
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberAtomOneDarkTheme

/**
 * Material 3–inspired light CSS theme used to demonstrate [HighlightTheme.fromCss].
 *
 * Colors are derived from the Material 3 color system (purple primary palette).
 */
private val MATERIAL3_LIGHT_CSS =
    """
.hljs {
  background: #FEF7FF;
  color: #1C1B1F;
}
.hljs-keyword,
.hljs-built_in {
  color: #6750A4;
  font-weight: bold;
}
.hljs-string,
.hljs-attr {
  color: #386A20;
}
.hljs-comment,
.hljs-quote {
  color: #938F99;
  font-style: italic;
}
.hljs-number,
.hljs-literal {
  color: #1565C0;
}
.hljs-name,
.hljs-selector-tag {
  color: #B3261E;
}
.hljs-title,
.hljs-section {
  color: #984061;
  font-weight: bold;
}
.hljs-type {
  color: #006874;
}
    """.trimIndent()

/**
 * Demonstrates all four [HighlightTheme] factory methods:
 * - Built-in: [HighlightTheme.atomOneDark] (one representative bundled theme)
 * - [HighlightTheme.fromAsset] - any highlight.js CSS saved in `assets/`
 * - [HighlightTheme.fromCss] - Material 3–inspired inline CSS
 * - [HighlightTheme.fromColorMap] - Material 3–inspired precomputed color map (dark variant)
 */
@Composable
internal fun ThemeCreationSection() {
    // Built-in theme - @Composable helper resolves LocalContext internally
    val atomOneDarkTheme = rememberAtomOneDarkTheme()

    // fromCss() - Material 3–inspired inline CSS string
    val material3LightTheme =
        remember {
            HighlightTheme.fromCss(
                cssText = MATERIAL3_LIGHT_CSS,
                name = "material3-light",
            )
        }

    // fromColorMap() - Material 3–inspired precomputed dark color map
    val material3DarkTheme =
        remember {
            HighlightTheme.fromColorMap(
                name = "material3-dark",
                colorMap =
                    mapOf(
                        HljsSelectors.BASE to SpanStyle(color = Color(0xFFE6E1E5), background = Color(0xFF1C1B1F)),
                        HljsSelectors.KEYWORD to SpanStyle(color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold),
                        HljsSelectors.BUILT_IN to SpanStyle(color = Color(0xFFCBA6F7)),
                        HljsSelectors.STRING to SpanStyle(color = Color(0xFF6DD58C)),
                        HljsSelectors.ATTR to SpanStyle(color = Color(0xFF6DD58C)),
                        HljsSelectors.COMMENT to SpanStyle(color = Color(0xFF938F99), fontStyle = FontStyle.Italic),
                        HljsSelectors.QUOTE to SpanStyle(color = Color(0xFF938F99), fontStyle = FontStyle.Italic),
                        HljsSelectors.NUMBER to SpanStyle(color = Color(0xFF7FCFFF)),
                        HljsSelectors.LITERAL to SpanStyle(color = Color(0xFF7FCFFF)),
                        HljsSelectors.TYPE to SpanStyle(color = Color(0xFF80CBC4)),
                        HljsSelectors.TITLE to SpanStyle(color = Color(0xFFFFB4AB), fontWeight = FontWeight.Bold),
                        HljsSelectors.NAME to SpanStyle(color = Color(0xFFFFB4AB)),
                        HljsSelectors.SELECTOR_TAG to SpanStyle(color = Color(0xFFFFB4AB)),
                    ),
                backgroundColor = Color(0xFF1C1B1F),
                defaultTextColor = Color(0xFFE6E1E5),
            )
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Built-in theme ─────────────────────────────────────────────────
        SubSectionHeader("Built-in themes")
        Text(
            text =
                "The library bundles 8 ready-to-use themes: atomOneDark, atomOneLight, " +
                    "tomorrow, tomorrowNight, githubLight, githubDark, draculaDark, and alucardLight. " +
                    "No extra assets needed - just call the corresponding factory function. " +
                    "Here is atomOneDark as an example:",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = atomOneDarkTheme,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── fromAsset() ────────────────────────────────────────────────────
        SubSectionHeader("fromAsset(): load any CSS theme from assets/")
        Text(
            text =
                "Save any highlight.js-compatible CSS file anywhere under your app's assets/ " +
                    "directory and load it at runtime with HighlightTheme.fromAsset(). " +
                    "The code block below shows you exactly how to do it:",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        SyntaxHighlightedCode(
            code = FROM_ASSET_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = atomOneDarkTheme,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── fromCss() ──────────────────────────────────────────────────────
        SubSectionHeader("fromCss(): Material 3 light (inline CSS string)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = material3LightTheme,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── fromColorMap() ─────────────────────────────────────────────────
        SubSectionHeader("fromColorMap(): Material 3 dark (precomputed SpanStyle map)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = material3DarkTheme,
        )
    }
}
