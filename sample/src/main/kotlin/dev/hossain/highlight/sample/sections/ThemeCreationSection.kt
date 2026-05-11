package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.ui.SyntaxHighlightedCode

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
 * Exercises every [HighlightTheme] factory method side-by-side:
 * - Built-in: [HighlightTheme.tomorrow], [HighlightTheme.tomorrowNight],
 *   [HighlightTheme.atomOneLight], [HighlightTheme.atomOneDark]
 * - [HighlightTheme.fromAsset] — GitHub CSS bundled in sample assets
 * - [HighlightTheme.fromCss] — Material 3–inspired inline CSS
 * - [HighlightTheme.fromColorMap] — Material 3–inspired precomputed color map (dark variant)
 */
@Composable
internal fun ThemeCreationSection() {
    val context = LocalContext.current.applicationContext

    // Built-in themes
    val tomorrowTheme = remember(context) { HighlightTheme.tomorrow(context) }
    val tomorrowNightTheme = remember(context) { HighlightTheme.tomorrowNight(context) }
    val atomOneLightTheme = remember(context) { HighlightTheme.atomOneLight(context) }
    val atomOneDarkTheme = remember(context) { HighlightTheme.atomOneDark(context) }

    // fromAsset() — GitHub themes bundled in the sample app's assets/
    val githubTheme = remember(context) { HighlightTheme.fromAsset(context, "themes/github.css", "github") }
    val githubDarkTheme = remember(context) { HighlightTheme.fromAsset(context, "themes/github-dark.css", "github-dark") }

    // fromCss() — Material 3–inspired inline CSS string
    val material3LightTheme =
        remember {
            HighlightTheme.fromCss(
                cssText = MATERIAL3_LIGHT_CSS,
                name = "material3-light",
            )
        }

    // fromColorMap() — Material 3–inspired precomputed dark color map
    val material3DarkTheme =
        remember {
            HighlightTheme.fromColorMap(
                name = "material3-dark",
                colorMap =
                    mapOf(
                        "hljs" to SpanStyle(color = Color(0xFFE6E1E5), background = Color(0xFF1C1B1F)),
                        "hljs-keyword" to SpanStyle(color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold),
                        "hljs-built_in" to SpanStyle(color = Color(0xFFCBA6F7)),
                        "hljs-string" to SpanStyle(color = Color(0xFF6DD58C)),
                        "hljs-attr" to SpanStyle(color = Color(0xFF6DD58C)),
                        "hljs-comment" to SpanStyle(color = Color(0xFF938F99), fontStyle = FontStyle.Italic),
                        "hljs-quote" to SpanStyle(color = Color(0xFF938F99), fontStyle = FontStyle.Italic),
                        "hljs-number" to SpanStyle(color = Color(0xFF7FCFFF)),
                        "hljs-literal" to SpanStyle(color = Color(0xFF7FCFFF)),
                        "hljs-type" to SpanStyle(color = Color(0xFF80CBC4)),
                        "hljs-title" to SpanStyle(color = Color(0xFFFFB4AB), fontWeight = FontWeight.Bold),
                        "hljs-name" to SpanStyle(color = Color(0xFFFFB4AB)),
                        "hljs-selector-tag" to SpanStyle(color = Color(0xFFFFB4AB)),
                    ),
                backgroundColor = Color(0xFF1C1B1F),
                defaultTextColor = Color(0xFFE6E1E5),
            )
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Built-in themes ────────────────────────────────────────────────
        SubSectionHeader("Built-in: HighlightTheme.tomorrow() — light")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = tomorrowTheme,
        )

        SubSectionHeader("Built-in: HighlightTheme.tomorrowNight() — dark")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = tomorrowNightTheme,
        )

        SubSectionHeader("Built-in: HighlightTheme.atomOneLight() — light")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = atomOneLightTheme,
        )

        SubSectionHeader("Built-in: HighlightTheme.atomOneDark() — dark")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = atomOneDarkTheme,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── fromAsset() ────────────────────────────────────────────────────
        SubSectionHeader("fromAsset(): GitHub light (themes/github.css)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = githubTheme,
        )

        SubSectionHeader("fromAsset(): GitHub dark (themes/github-dark.css)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = githubDarkTheme,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── fromCss() ──────────────────────────────────────────────────────
        SubSectionHeader("fromCss(): Material 3 light (inline CSS)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = material3LightTheme,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── fromColorMap() ─────────────────────────────────────────────────
        SubSectionHeader("fromColorMap(): Material 3 dark (precomputed map)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = material3DarkTheme,
        )
    }
}
