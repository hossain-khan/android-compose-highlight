package dev.hossain.highlight.consumer

import android.app.Activity
import android.os.Bundle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.engine.HljsSelectors
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.StreamingSyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedTextEditor
import kotlinx.coroutines.flow.StateFlow

class ConsumerSmokeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val engine = HighlightEngine(applicationContext)
        val initializedFlow: StateFlow<Boolean> = engine.isInitialized
        check(!initializedFlow.value)
        engine.close()

        setContentView(
            ComposeView(this).apply {
                setContent {
                    ConsumerSmokeScreen()
                }
            },
        )
    }
}

@OptIn(ExperimentalHighlightApi::class)
@Composable
fun ConsumerSmokeScreen() {
    val customTheme =
        HighlightTheme.fromColorMap(
            name = "consumer-theme",
            colorMap = mapOf(HljsSelectors.KEYWORD to SpanStyle(color = Color.Blue)),
        )
    val style =
        CodeBlockStyle.Default.copy(
            padding = PaddingValues(12.dp),
            textStyle = TextStyle(color = Color.White),
        )
    HighlightThemeProvider(
        lightHighlightTheme = customTheme,
        darkHighlightTheme = HighlightTheme.draculaDark(),
    ) {
        SyntaxHighlightedCode(
            code = "val x = 1",
            language = "kotlin",
            modifier = Modifier,
            style = style,
            scrollState = ScrollState(0),
            onHighlightComplete = { result: HighlightResult ->
                val annotated: AnnotatedString = result.annotated
                check(annotated.text.isNotEmpty())
            },
        )
        StreamingSyntaxHighlightedCode(
            code = "val y = 2",
            language = "kotlin",
            modifier = Modifier,
        )
        SyntaxHighlightedTextEditor(
            value = TextFieldValue("val z = 3"),
            onValueChange = {},
            language = "kotlin",
            keyboardOptions = KeyboardOptions.Default,
        )
    }
}
