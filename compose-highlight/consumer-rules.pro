# Keep public API of compose-highlight library
-keep public class dev.hossain.highlight.** { public *; }
-keep public interface dev.hossain.highlight.** { *; }

# jsoup - HTML parser used internally by HtmlToAnnotatedString for AnnotatedString conversion.
# jsoup does not ship its own consumer ProGuard rules, so we keep the runtime types from
# being removed in minified release builds while still allowing name obfuscation.
-keep,allowobfuscation class org.jsoup.Jsoup { *; }
-keep,allowobfuscation class org.jsoup.parser.** { *; }
-keep,allowobfuscation class org.jsoup.nodes.** { *; }
-keep,allowobfuscation class org.jsoup.select.** { *; }
