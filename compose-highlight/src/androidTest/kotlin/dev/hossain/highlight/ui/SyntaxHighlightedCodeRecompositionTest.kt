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
 * Recomposition stability tests for [SyntaxHighlightedCode] using Dejavu.
 *
 * Enforces Compose compiler skipping contracts to ensure that parent recompositions,
 * sibling state changes, and shared providers do not cause unnecessary recomposition churn.
 */
@RunWith(AndroidJUnit4::class)
class SyntaxHighlightedCodeRecompositionTest {
    @get:Rule
    val composeTestRule = createRecompositionTrackingRule()

    @Test
    fun parentStateChange_syntaxHighlightedCodeSkipsRecomposition() {
        composeTestRule.setContent {
            var counter by remember { mutableIntStateOf(0) }

            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                Column {
                    Button(
                        onClick = { counter++ },
                        modifier = Modifier.testTag("code_increment_button"),
                    ) {
                        Text("Increment")
                    }

                    CodeCounterLabel(count = counter)

                    SyntaxHighlightedCode(
                        code = "val message = \"Hello, World!\"",
                        language = "kotlin",
                        modifier = Modifier.testTag("syntax-highlighted-code"),
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        // Trigger a recomposition in the parent via counter state change
        composeTestRule.onNodeWithTag("code_increment_button").performClick()
        composeTestRule.waitForIdle()

        // The counter text should have recomposed
        composeTestRule.onNodeWithTag("code_counter_text").assertRecompositions(exactly = 1)

        // SyntaxHighlightedCode parameters did not change - must skip recomposition entirely
        composeTestRule.onNodeWithTag("syntax-highlighted-code").assertStable()
    }

    @Test
    fun codeParameterChange_recomposesExactlyOnce() {
        var codeState by mutableStateOf("val x = 1")

        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                SyntaxHighlightedCode(
                    code = codeState,
                    language = "kotlin",
                    modifier = Modifier.testTag("syntax-highlighted-code"),
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        // Change the code parameter
        composeTestRule.runOnIdle {
            codeState = "val x = 2"
        }
        composeTestRule.waitForIdle()

        // Must recompose exactly once in response to the input change
        composeTestRule.onNodeWithTag("syntax-highlighted-code").assertRecompositions(exactly = 1)
    }

    @Test
    fun multipleCodeBlocksInSharedProvider_oneBlockUpdateDoesNotRecomposeSibling() {
        var codeA by mutableStateOf("val a = 10")
        val codeB = "val b = 20"

        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                Column {
                    SyntaxHighlightedCode(
                        code = codeA,
                        language = "kotlin",
                        modifier = Modifier.testTag("code_block_a"),
                    )

                    SyntaxHighlightedCode(
                        code = codeB,
                        language = "kotlin",
                        modifier = Modifier.testTag("code_block_b"),
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()

        // Mutate block A's input
        composeTestRule.runOnIdle {
            codeA = "val a = 99"
        }
        composeTestRule.waitForIdle()

        // Block A should recompose once
        composeTestRule.onNodeWithTag("code_block_a").assertRecompositions(exactly = 1)

        // Block B's inputs were unchanged - must remain completely stable
        composeTestRule.onNodeWithTag("code_block_b").assertStable()
    }

    @Composable
    private fun CodeCounterLabel(count: Int) {
        Text(
            text = "Counter: $count",
            modifier = Modifier.testTag("code_counter_text"),
        )
    }
}
