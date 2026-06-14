# Keep the library's public API surface only.
# Internal implementation packages are intentionally excluded so downstream R8 can
# still shrink and optimize those classes.
-keep public class dev.hossain.highlight.ui.* { public *; }
-keep public interface dev.hossain.highlight.ui.* { *; }
-keep public class dev.hossain.highlight.engine.* { public *; }
-keep public interface dev.hossain.highlight.engine.* { *; }
