package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dejavu.assertRecompositions
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import dejavu.resetRecompositionCounts
import dev.hossain.highlight.engine.HighlightTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recomposition stability tests for [StreamingSyntaxHighlightedCode] using Dejavu.
 *
 * Verifies that streaming updates, debounce cycles, and parent recompositions
 * adhere to Compose compiler skipping and stability contracts.
 */
@OptIn(ExperimentalHighlightApi::class)
@RunWith(AndroidJUnit4::class)
class StreamingSyntaxHighlightedCodeRecompositionTest {
    @get:Rule
    val composeTestRule = createRecompositionTrackingRule()

    @Test
    fun parentStateChange_streamingCodeSkipsRecomposition() {
        composeTestRule.setContent {
            var counter by remember { mutableIntStateOf(0) }

            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                Column {
                    Button(
                        onClick = { counter++ },
                        modifier = Modifier.testTag("streaming_increment_button"),
                    ) {
                        Text("Increment")
                    }

                    StreamingCounterLabel(count = counter)

                    StreamingSyntaxHighlightedCode(
                        code = "fun stream() = 42",
                        language = "kotlin",
                        modifier = Modifier.testTag("streaming-syntax-highlighted-code"),
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        // Trigger a recomposition in parent via counter
        composeTestRule.onNodeWithTag("streaming_increment_button").performClick()
        composeTestRule.waitForIdle()

        // Counter text recomposes once
        composeTestRule.onNodeWithTag("streaming_counter_text").assertRecompositions(exactly = 1)

        // Streaming block parameters did not change - must skip recomposition entirely
        composeTestRule.onNodeWithTag("streaming-syntax-highlighted-code").assertStable()
    }

    @Test
    fun codeUpdate_recomposesExactlyOnce() {
        var codeState by mutableStateOf("fun hello() {")

        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                StreamingSyntaxHighlightedCode(
                    code = codeState,
                    language = "kotlin",
                    modifier = Modifier.testTag("streaming-syntax-highlighted-code"),
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        // Simulate streaming token arrival
        composeTestRule.runOnIdle {
            codeState = "fun hello() { println(\"hi\") }"
        }
        composeTestRule.waitForIdle()

        // Must recompose exactly once for the new code string
        composeTestRule.onNodeWithTag("streaming-syntax-highlighted-code").assertRecompositions(exactly = 1)
    }

    @Composable
    private fun StreamingCounterLabel(count: Int) {
        Text(
            text = "Counter: $count",
            modifier = Modifier.testTag("streaming_counter_text"),
        )
    }
}
