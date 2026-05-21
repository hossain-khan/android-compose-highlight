# Keep public API of compose-highlight library
-keep public class dev.hossain.highlight.** { public *; }
-keep public interface dev.hossain.highlight.** { *; }

# jsoup - HTML parser used internally by HtmlToAnnotatedString for AnnotatedString conversion.
# jsoup does not ship its own consumer ProGuard rules, so we keep the classes used at runtime
# to prevent ClassNotFoundException or NoSuchMethodError in minified release builds.
-keep class org.jsoup.Jsoup { *; }
-keep class org.jsoup.parser.** { *; }
-keep class org.jsoup.nodes.** { *; }
-keep class org.jsoup.select.** { *; }
