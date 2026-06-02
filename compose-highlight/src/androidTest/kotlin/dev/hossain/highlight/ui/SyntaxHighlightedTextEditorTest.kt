package dev.hossain.highlight.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalHighlightApi::class)
@RunWith(AndroidJUnit4::class)
class SyntaxHighlightedTextEditorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleCode = "fun hello() = println(\"Hello!\")"

    @Test
    fun rendersWithoutCrash() {
        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                SyntaxHighlightedTextEditor(
                    value = TextFieldValue(sampleCode),
                    onValueChange = {},
                    language = "kotlin",
                )
            }
        }
        // Plain text should be visible immediately before any async highlight result
        composeTestRule.onNodeWithText(sampleCode, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun emptyCodeRendersWithoutCrash() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedTextEditor(
                    value = TextFieldValue(""),
                    onValueChange = {},
                    language = "kotlin",
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun composableHasTestTag() {
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedTextEditor(
                    value = TextFieldValue(sampleCode),
                    onValueChange = {},
                    language = "kotlin",
                )
            }
        }
        composeTestRule.onNodeWithTag("syntax-highlighted-text-editor").assertIsDisplayed()
    }

    @Test
    fun onValueChangeFiresWhenTextChanges() {
        var received: TextFieldValue? = null
        var editorValue by mutableStateOf(TextFieldValue(""))

        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedTextEditor(
                    value = editorValue,
                    onValueChange = {
                        received = it
                        editorValue = it
                    },
                    language = "kotlin",
                )
            }
        }

        // Drive real text input through the UI test API - this exercises the actual BasicTextField
        // wiring and confirms onValueChange is called in response to user input.
        composeTestRule
            .onNode(hasSetTextAction(), useUnmergedTree = true)
            .performTextInput("val x = 1")
        composeTestRule.waitForIdle()

        assertThat(received).isNotNull()
        assertThat(received!!.text).isEqualTo("val x = 1")
    }

    @Test
    fun onHighlightCompleteCallbackFiresAfterDebounce() {
        var capturedResult: HighlightResult? = null

        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                SyntaxHighlightedTextEditor(
                    value = TextFieldValue(sampleCode),
                    onValueChange = {},
                    language = "kotlin",
                    onHighlightComplete = { capturedResult = it },
                )
            }
        }

        // Wait for the async highlight pipeline to complete
        composeTestRule.waitUntil(timeoutMillis = 10_000L) { capturedResult != null }

        // Result should carry the full HighlightResult shape - matching rememberHighlightedCode.
        // This locks in the public callback contract introduced for issue #277.
        val result = checkNotNull(capturedResult)
        assertThat(result.annotated.text).isEqualTo(sampleCode)
        assertThat(result.annotated.spanStyles).isNotEmpty()
        assertThat(result.language).isEqualTo("kotlin")
        assertThat(result.spanCount).isGreaterThan(0)
        assertThat(result.durationMs).isAtLeast(0L)
        // timings is non-null and total matches durationMs (both reflect the same elapsed time).
        assertThat(result.timings.total.inWholeMilliseconds).isEqualTo(result.durationMs)
    }

    @Test
    fun onHighlightCompleteFiresAgainWhenLanguageChanges() {
        var callCount = 0
        var currentLanguage by mutableStateOf("kotlin")

        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                SyntaxHighlightedTextEditor(
                    value = TextFieldValue("val x = 1"),
                    onValueChange = {},
                    language = currentLanguage,
                    onHighlightComplete = { callCount++ },
                )
            }
        }

        // Wait for the first highlight cycle to complete
        composeTestRule.waitUntil(timeoutMillis = 10_000L) { callCount >= 1 }

        // Switch language - should trigger a fresh highlight cycle
        composeTestRule.runOnIdle { currentLanguage = "python" }
        composeTestRule.waitUntil(timeoutMillis = 10_000L) { callCount >= 2 }

        assertThat(callCount).isAtLeast(2)
    }

    @Test
    fun staleSpansNotShownAfterLanguageChange() {
        // When language changes the old snapshot is for a different language, so the composable
        // falls back to plain text in-composition until the new highlight arrives.
        var capturedResult: HighlightResult? = null
        var currentLanguage by mutableStateOf("kotlin")

        composeTestRule.setContent {
            HighlightThemeProvider(
                lightHighlightTheme = HighlightTheme.tomorrow(),
                darkHighlightTheme = HighlightTheme.tomorrowNight(),
            ) {
                SyntaxHighlightedTextEditor(
                    value = TextFieldValue("val x = 1"),
                    onValueChange = {},
                    language = currentLanguage,
                    onHighlightComplete = { capturedResult = it },
                )
            }
        }

        // Wait for initial highlight to confirm spans are applied
        composeTestRule.waitUntil(timeoutMillis = 10_000L) { capturedResult != null }
        capturedResult = null

        // Switch to a different language - the old snapshot is now stale
        composeTestRule.runOnIdle { currentLanguage = "sql" }

        // Immediately after language change (before debounce + highlight complete), the composable
        // detects in-composition that snapshot.language != language and falls back to plain text.
        // The LaunchedEffect has restarted but is still inside delay(debounceMs), so no new spans
        // have arrived yet. Assert that no span styles are present in the displayed text.
        val editableText =
            composeTestRule
                .onNode(hasSetTextAction(), useUnmergedTree = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
        assertThat(editableText.spanStyles).isEmpty()

        // Now wait for the re-highlight cycle to complete with the new language
        composeTestRule.waitUntil(timeoutMillis = 10_000L) { capturedResult != null }
        assertThat(capturedResult!!.language).isEqualTo("sql")
        assertThat(capturedResult!!.annotated.text).isEqualTo("val x = 1")
        assertThat(capturedResult!!.annotated.spanStyles).isNotEmpty()
    }

    @Test
    fun keyboardOptionsPropagateToBasicTextField() {
        // Pass a non-default imeAction. If keyboardOptions is wired through, the underlying
        // text field's semantics will report that imeAction. If it isn't wired, the field would
        // report KeyboardOptions.Default's ImeAction.Default value instead. This is the issue
        // #265 acceptance criterion that the editor honours caller-supplied keyboardOptions.
        // The default-constants assertions live in the JVM-side
        // SyntaxHighlightedTextEditorDefaultsTest where they cost milliseconds, not seconds.
        composeTestRule.setContent {
            HighlightThemeProvider {
                SyntaxHighlightedTextEditor(
                    value = TextFieldValue(sampleCode),
                    onValueChange = {},
                    language = "kotlin",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
            }
        }

        val imeAction =
            composeTestRule
                .onNode(hasSetTextAction(), useUnmergedTree = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.ImeAction]
        assertThat(imeAction).isEqualTo(ImeAction.Search)
    }
}
