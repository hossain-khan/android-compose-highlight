package dev.hossain.highlight.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [rememberSyntaxHighlightedEditorValue] exercising the helper
 * in isolation - without a [Surface] or [BasicTextField] in the tree.
 */
@OptIn(ExperimentalHighlightApi::class)
@RunWith(AndroidJUnit4::class)
class RememberSyntaxHighlightedEditorValueTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleCode = "fun hello() = println(\"Hello!\")"

    @Test
    fun returnsPlainTextWithNoSpansBeforeHighlightArrives() {
        // Capture the value returned synchronously in the first composition frame, before
        // the async highlight pipeline has had a chance to produce a result.
        var capturedValue: TextFieldValue? = null

        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                capturedValue =
                    rememberSyntaxHighlightedEditorValue(
                        value = TextFieldValue(sampleCode),
                        language = "kotlin",
                    )
            }
        }

        // After the first composition frame the text must be present but no highlight spans
        // have been written yet (the LaunchedEffect hasn't started, let alone completed).
        composeTestRule.runOnIdle {
            assertThat(capturedValue).isNotNull()
            assertThat(capturedValue!!.text).isEqualTo(sampleCode)
            assertThat(capturedValue!!.annotatedString.spanStyles).isEmpty()
        }
    }

    @Test
    fun returnsHighlightedSpansAfterDebounce() {
        var capturedValue: TextFieldValue? = null
        var highlightCallbackFired = false

        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                capturedValue =
                    rememberSyntaxHighlightedEditorValue(
                        value = TextFieldValue(sampleCode),
                        language = "kotlin",
                        onHighlightComplete = { highlightCallbackFired = true },
                    )
            }
        }

        // Wait for the async highlight pipeline (debounce + WebView + recomposition) to finish.
        composeTestRule.waitUntil(timeoutMillis = 10_000L) { highlightCallbackFired }

        // After the highlight cycle completes, the returned TextFieldValue must carry spans
        // and preserve the original text content.
        composeTestRule.runOnIdle {
            assertThat(capturedValue!!.text).isEqualTo(sampleCode)
            assertThat(capturedValue!!.annotatedString.spanStyles).isNotEmpty()
        }
    }

    @Test
    fun preservesCursorPositionInReturnedValue() {
        // The highlight pipeline must not move the cursor. Cursor position lives in the
        // TextFieldValue selection field; copying annotatedString must leave it untouched.
        val cursorOffset = 5
        val inputValue = TextFieldValue(text = sampleCode, selection = TextRange(cursorOffset))
        var capturedValue: TextFieldValue? = null
        var highlightDone = false

        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                capturedValue =
                    rememberSyntaxHighlightedEditorValue(
                        value = inputValue,
                        language = "kotlin",
                        onHighlightComplete = { highlightDone = true },
                    )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000L) { highlightDone }

        composeTestRule.runOnIdle {
            assertThat(capturedValue!!.selection).isEqualTo(TextRange(cursorOffset))
        }
    }

    @Test
    fun returnsPlainTextImmediatelyAfterLanguageChange() {
        // When the language changes, the cached snapshot is stale. The composable must detect
        // this in-composition (snapshot.language != language) and fall back to plain text
        // immediately - before the new highlight cycle has completed.
        var capturedAnnotated: AnnotatedString? = null
        var currentLanguage by mutableStateOf("kotlin")
        var capturedValue: TextFieldValue? = null

        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                capturedValue =
                    rememberSyntaxHighlightedEditorValue(
                        value = TextFieldValue("val x = 1"),
                        language = currentLanguage,
                        onHighlightComplete = { capturedAnnotated = it },
                    )
            }
        }

        // Wait for the initial highlight to confirm spans exist
        composeTestRule.waitUntil(timeoutMillis = 10_000L) { capturedAnnotated != null }
        capturedAnnotated = null

        // Switch language - the snapshot is now stale, stale detection is in-composition
        composeTestRule.runOnIdle { currentLanguage = "sql" }

        // Immediately after the recomposition (before new highlight arrives), spans should
        // be absent because the snapshot is for "kotlin", not "sql".
        composeTestRule.runOnIdle {
            assertThat(capturedValue!!.annotatedString.spanStyles).isEmpty()
        }

        // Confirm the new highlight cycle eventually fires with the new language
        composeTestRule.waitUntil(timeoutMillis = 10_000L) { capturedAnnotated != null }
        assertThat(capturedAnnotated!!.text).isEqualTo("val x = 1")
        assertThat(capturedAnnotated!!.spanStyles).isNotEmpty()
    }
}
