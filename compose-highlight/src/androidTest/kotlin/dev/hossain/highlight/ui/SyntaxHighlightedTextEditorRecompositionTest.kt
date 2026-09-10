package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import dejavu.resetRecompositionCounts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recomposition stability tests for [SyntaxHighlightedTextEditor] using Dejavu.
 *
 * Verifies that the editor composable skips recompositions when parent state updates,
 * and recomposes only when its input [TextFieldValue] or parameters change.
 */
@OptIn(ExperimentalHighlightApi::class)
@RunWith(AndroidJUnit4::class)
class SyntaxHighlightedTextEditorRecompositionTest {
    @get:Rule
    val composeTestRule = createRecompositionTrackingRule()

    @Test
    fun parentStateChange_editorSkipsRecomposition() {
        var editorValue by mutableStateOf(TextFieldValue("val x = 1"))

        composeTestRule.setContent {
            var counter by remember { mutableIntStateOf(0) }

            HighlightThemeProvider {
                Column {
                    Button(
                        onClick = { counter++ },
                        modifier = Modifier.testTag("increment_button"),
                    ) {
                        Text("Increment")
                    }

                    CounterLabel(count = counter)

                    SyntaxHighlightedTextEditor(
                        value = editorValue,
                        onValueChange = { editorValue = it },
                        language = "kotlin",
                        modifier = Modifier.testTag("syntax-highlighted-text-editor"),
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        // Trigger a recomposition in parent via counter
        composeTestRule.onNodeWithTag("increment_button").performClick()
        composeTestRule.waitForIdle()

        // Counter text recomposes once
        composeTestRule.onNodeWithTag("counter_text").assertRecompositions(exactly = 1)

        // Editor inputs did not change - must skip recomposition entirely
        composeTestRule.onNodeWithTag("syntax-highlighted-text-editor").assertStable()
    }

    @Test
    fun valueChange_recomposesExactlyOnce() {
        var editorValue by mutableStateOf(TextFieldValue("val x = 1"))

        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedTextEditor(
                    value = editorValue,
                    onValueChange = { editorValue = it },
                    language = "kotlin",
                    modifier = Modifier.testTag("syntax-highlighted-text-editor"),
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        // Simulate typing
        composeTestRule.runOnIdle {
            editorValue = TextFieldValue("val x = 2")
        }
        composeTestRule.waitForIdle()

        // Recomposes once for the new TextFieldValue
        composeTestRule.onNodeWithTag("syntax-highlighted-text-editor").assertRecompositions(exactly = 1)
    }

    @androidx.compose.runtime.Composable
    private fun CounterLabel(count: Int) {
        androidx.compose.material3.Text(
            text = "Counter: $count",
            modifier =
                androidx.compose.ui.Modifier
                    .testTag("counter_text"),
        )
    }
}
