package dev.hossain.highlight.screenshot

/**
 * Code samples used as the visual content of screenshot regression tests.
 *
 * Each snippet is short enough to eyeball-diff (about 6 to 10 lines) and exercises a mix of
 * keywords, strings, numbers, comments, and at least one nested structure. The associated
 * highlight.js token output for each snippet lives in [TestHljsFixtures].
 */
internal object TestSnippets {
    val KOTLIN_SAMPLE =
        """
        // Greets the caller by name.
        fun greet(name: String) {
            val message = "Hello, ${'$'}name!"
            println(message)
        }
        greet("World")
        """.trimIndent()

    val PYTHON_SAMPLE =
        """
        # Compute factorial recursively.
        def factorial(n: int) -> int:
            if n <= 1:
                return 1
            return n * factorial(n - 1)

        print(factorial(5))  # 120
        """.trimIndent()

    val JSON_SAMPLE =
        """
        {
          "name": "compose-highlight",
          "version": "0.5.0",
          "tags": ["kotlin", "compose", "syntax"],
          "stable": true,
          "minSdk": 24
        }
        """.trimIndent()
}
