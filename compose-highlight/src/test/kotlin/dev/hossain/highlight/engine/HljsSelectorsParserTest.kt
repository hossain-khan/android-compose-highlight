package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * JVM unit tests verifying that [ThemeParser] correctly parses all selectors
 * defined in [HljsSelectors]. Each test uses an inline CSS snippet to confirm
 * the parser extracts the expected key and style properties.
 */
class HljsSelectorsParserTest {
    // ----- Base -----

    @Test
    fun `parses base hljs selector`() {
        val css = ".hljs { color: #24292e; background: #ffffff }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.BASE]?.color).isEqualTo(Color(0xFF24292e))
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFFffffff))
    }

    // ----- General purpose -----

    @Test
    fun `parses keyword selector`() {
        val css = ".hljs-keyword { color: #d73a49 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFFd73a49))
    }

    @Test
    fun `parses built_in selector`() {
        val css = ".hljs-built_in { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.BUILT_IN]?.color).isEqualTo(Color(0xFF005cc5))
    }

    @Test
    fun `parses type selector`() {
        val css = ".hljs-type { color: #d73a49 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TYPE]?.color).isEqualTo(Color(0xFFd73a49))
    }

    @Test
    fun `parses literal selector`() {
        val css = ".hljs-literal { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.LITERAL]?.color).isEqualTo(Color(0xFF005cc5))
    }

    @Test
    fun `parses number selector`() {
        val css = ".hljs-number { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.NUMBER]?.color).isEqualTo(Color(0xFF005cc5))
    }

    @Test
    fun `parses operator selector`() {
        val css = ".hljs-operator { color: #d73a49 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.OPERATOR]?.color).isEqualTo(Color(0xFFd73a49))
    }

    @Test
    fun `parses punctuation selector`() {
        val css = ".hljs-punctuation { color: #6a737d }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.PUNCTUATION]?.color).isEqualTo(Color(0xFF6a737d))
    }

    @Test
    fun `parses property selector`() {
        val css = ".hljs-property { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.PROPERTY]?.color).isEqualTo(Color(0xFF005cc5))
    }

    @Test
    fun `parses regexp selector`() {
        val css = ".hljs-regexp { color: #032f62 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.REGEXP]?.color).isEqualTo(Color(0xFF032f62))
    }

    @Test
    fun `parses string selector`() {
        val css = ".hljs-string { color: #032f62 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF032f62))
    }

    @Test
    fun `parses char escape underscore selector`() {
        val css = ".hljs-char.escape_ { color: #032f62 }"
        val result = ThemeParser.parse(css)
        // ThemeParser publishes both the compound key and the primary key (substringBefore '.')
        assertThat(result[HljsSelectors.CHAR_ESCAPE]?.color).isEqualTo(Color(0xFF032f62))
        assertThat(result[HljsSelectors.CHAR]?.color).isEqualTo(Color(0xFF032f62))
    }

    @Test
    fun `parses subst selector`() {
        val css = ".hljs-subst { color: #24292e }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.SUBST]?.color).isEqualTo(Color(0xFF24292e))
    }

    @Test
    fun `parses symbol selector`() {
        val css = ".hljs-symbol { color: #e36209 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.SYMBOL]?.color).isEqualTo(Color(0xFFe36209))
    }

    @Test
    fun `parses variable selector`() {
        val css = ".hljs-variable { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.VARIABLE]?.color).isEqualTo(Color(0xFF005cc5))
    }

    @Test
    fun `parses variable_language underscore selector`() {
        val css = ".hljs-variable.language_ { color: #d73a49 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.VARIABLE_LANGUAGE]?.color).isEqualTo(Color(0xFFd73a49))
    }

    @Test
    fun `parses variable_constant underscore selector`() {
        val css = ".hljs-variable.constant_ { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.VARIABLE_CONSTANT]?.color).isEqualTo(Color(0xFF005cc5))
    }

    @Test
    fun `parses title selector`() {
        val css = ".hljs-title { color: #6f42c1; font-weight: bold }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TITLE]?.color).isEqualTo(Color(0xFF6f42c1))
        assertThat(result[HljsSelectors.TITLE]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `parses params selector`() {
        val css = ".hljs-params { color: #24292e }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.PARAMS]?.color).isEqualTo(Color(0xFF24292e))
    }

    @Test
    fun `parses comment selector`() {
        val css = ".hljs-comment { color: #6a737d; font-style: italic }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.COMMENT]?.color).isEqualTo(Color(0xFF6a737d))
        assertThat(result[HljsSelectors.COMMENT]?.fontStyle).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun `parses doctag selector`() {
        val css = ".hljs-doctag { color: #d73a49 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.DOCTAG]?.color).isEqualTo(Color(0xFFd73a49))
    }

    // ----- Title subscopes -----

    @Test
    fun `parses title_class underscore selector`() {
        val css = ".hljs-title.class_ { color: #6f42c1 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TITLE_CLASS]?.color).isEqualTo(Color(0xFF6f42c1))
    }

    @Test
    fun `parses title_class_inherited double-underscore selector`() {
        val css = ".hljs-title.class_.inherited__ { color: #6f42c1 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TITLE_CLASS_INHERITED]?.color).isEqualTo(Color(0xFF6f42c1))
    }

    @Test
    fun `parses title_function underscore selector`() {
        val css = ".hljs-title.function_ { color: #6f42c1 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TITLE_FUNCTION]?.color).isEqualTo(Color(0xFF6f42c1))
    }

    @Test
    fun `parses title_function_invoke underscore selector`() {
        val css = ".hljs-title.function.invoke_ { color: #6f42c1 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TITLE_FUNCTION_INVOKE]?.color).isEqualTo(Color(0xFF6f42c1))
    }

    // ----- Meta -----

    @Test
    fun `parses meta selector`() {
        val css = ".hljs-meta { color: #6a737d }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.META]?.color).isEqualTo(Color(0xFF6a737d))
    }

    @Test
    fun `parses meta-keyword selector`() {
        val css = ".hljs-meta-keyword { color: #d73a49 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.META_KEYWORD]?.color).isEqualTo(Color(0xFFd73a49))
    }

    @Test
    fun `parses meta-string selector`() {
        val css = ".hljs-meta-string { color: #032f62 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.META_STRING]?.color).isEqualTo(Color(0xFF032f62))
    }

    @Test
    fun `parses meta prompt compound selector`() {
        val css = ".hljs-meta.prompt { color: #6a737d }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.META_PROMPT]?.color).isEqualTo(Color(0xFF6a737d))
    }

    // ----- Tags, attributes, configs -----

    @Test
    fun `parses tag selector`() {
        val css = ".hljs-tag { color: #22863a }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TAG]?.color).isEqualTo(Color(0xFF22863a))
    }

    @Test
    fun `parses name selector`() {
        val css = ".hljs-name { color: #22863a }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.NAME]?.color).isEqualTo(Color(0xFF22863a))
    }

    @Test
    fun `parses attr selector`() {
        val css = ".hljs-attr { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.ATTR]?.color).isEqualTo(Color(0xFF005cc5))
    }

    @Test
    fun `parses attribute selector`() {
        val css = ".hljs-attribute { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.ATTRIBUTE]?.color).isEqualTo(Color(0xFF005cc5))
    }

    @Test
    fun `parses section selector`() {
        val css = ".hljs-section { color: #005cc5; font-weight: bold }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.SECTION]?.color).isEqualTo(Color(0xFF005cc5))
        assertThat(result[HljsSelectors.SECTION]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    // ----- CSS selectors -----

    @Test
    fun `parses selector-tag selector`() {
        val css = ".hljs-selector-tag { color: #22863a }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.SELECTOR_TAG]?.color).isEqualTo(Color(0xFF22863a))
    }

    @Test
    fun `parses selector-id selector`() {
        val css = ".hljs-selector-id { color: #6f42c1 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.SELECTOR_ID]?.color).isEqualTo(Color(0xFF6f42c1))
    }

    @Test
    fun `parses selector-class selector`() {
        val css = ".hljs-selector-class { color: #6f42c1 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.SELECTOR_CLASS]?.color).isEqualTo(Color(0xFF6f42c1))
    }

    @Test
    fun `parses selector-attr selector`() {
        val css = ".hljs-selector-attr { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.SELECTOR_ATTR]?.color).isEqualTo(Color(0xFF005cc5))
    }

    @Test
    fun `parses selector-pseudo selector`() {
        val css = ".hljs-selector-pseudo { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.SELECTOR_PSEUDO]?.color).isEqualTo(Color(0xFF005cc5))
    }

    // ----- Text markup -----

    @Test
    fun `parses bullet selector`() {
        val css = ".hljs-bullet { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.BULLET]?.color).isEqualTo(Color(0xFF005cc5))
    }

    @Test
    fun `parses code selector`() {
        val css = ".hljs-code { color: #24292e }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.CODE]?.color).isEqualTo(Color(0xFF24292e))
    }

    @Test
    fun `parses emphasis selector`() {
        val css = ".hljs-emphasis { color: #24292e; font-style: italic }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.EMPHASIS]?.fontStyle).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun `parses strong selector`() {
        val css = ".hljs-strong { color: #24292e; font-weight: bold }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.STRONG]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `parses formula selector`() {
        val css = ".hljs-formula { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.FORMULA]?.color).isEqualTo(Color(0xFF005cc5))
    }

    @Test
    fun `parses link selector`() {
        val css = ".hljs-link { color: #0366d6 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.LINK]?.color).isEqualTo(Color(0xFF0366d6))
    }

    @Test
    fun `parses quote selector`() {
        val css = ".hljs-quote { color: #6a737d; font-style: italic }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.QUOTE]?.fontStyle).isEqualTo(FontStyle.Italic)
    }

    // ----- Templates -----

    @Test
    fun `parses template-tag selector`() {
        val css = ".hljs-template-tag { color: #d73a49 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TEMPLATE_TAG]?.color).isEqualTo(Color(0xFFd73a49))
    }

    @Test
    fun `parses template-variable selector`() {
        val css = ".hljs-template-variable { color: #005cc5 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TEMPLATE_VARIABLE]?.color).isEqualTo(Color(0xFF005cc5))
    }

    // ----- Diff -----

    @Test
    fun `parses addition selector`() {
        val css = ".hljs-addition { color: #22863a; background: #f0fff4 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.ADDITION]?.color).isEqualTo(Color(0xFF22863a))
        assertThat(result[HljsSelectors.ADDITION]?.background).isEqualTo(Color(0xFFf0fff4))
    }

    @Test
    fun `parses deletion selector`() {
        val css = ".hljs-deletion { color: #b31d28; background: #ffeef0 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.DELETION]?.color).isEqualTo(Color(0xFFb31d28))
        assertThat(result[HljsSelectors.DELETION]?.background).isEqualTo(Color(0xFFffeef0))
    }

    // ----- Other -----

    @Test
    fun `parses atrule selector`() {
        val css = ".hljs-atrule { color: #d73a49 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.ATRULE]?.color).isEqualTo(Color(0xFFd73a49))
    }

    // ----- Compound selectors with multiple classes -----

    @Test
    fun `parses comma-separated selector list with new selectors`() {
        val css = ".hljs-punctuation, .hljs-operator { color: #6a737d }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.PUNCTUATION]?.color).isEqualTo(Color(0xFF6a737d))
        assertThat(result[HljsSelectors.OPERATOR]?.color).isEqualTo(Color(0xFF6a737d))
    }

    @Test
    fun `parses combined general purpose selectors in single rule`() {
        val css =
            ".hljs-keyword, .hljs-built_in, .hljs-type { color: #d73a49; font-weight: bold }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFFd73a49))
        assertThat(result[HljsSelectors.KEYWORD]?.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result[HljsSelectors.BUILT_IN]?.color).isEqualTo(Color(0xFFd73a49))
        assertThat(result[HljsSelectors.BUILT_IN]?.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result[HljsSelectors.TYPE]?.color).isEqualTo(Color(0xFFd73a49))
        assertThat(result[HljsSelectors.TYPE]?.fontWeight).isEqualTo(FontWeight.Bold)
    }
}
