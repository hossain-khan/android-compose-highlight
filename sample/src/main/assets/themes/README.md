# Highlight.js Themes

This directory contains **256 minified CSS theme files** for
[highlight.js](https://highlightjs.org/) version **11.11.1**, used by the
`android-compose-highlight` sample app to power the **All Themes** demo tab.

## Source

All themes were downloaded from the
[cdnjs CDN](https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/)
using the cdnjs API:

```
https://api.cdnjs.com/libraries/highlight.js/11.11.1?fields=files
```

Only the **minified** variants (`*.min.css`) are included — one file per theme.
The full list of available themes is also browsable at the
[highlight.js GitHub repository](https://github.com/highlightjs/highlight.js/tree/main/src/styles).

## Directory structure

```
themes/
├── a11y-dark.min.css          # 80 root-level themes
├── atom-one-dark.min.css
├── github.min.css
├── ...
└── base16/                    # 176 Base16-family themes
    ├── 3024.min.css
    ├── dracula.min.css
    └── ...
```

> **Note:** The `compose-highlight` library also bundles 4 themes as its own
> assets (`atom-one-dark`, `atom-one-light`, `tomorrow`, `tomorrow-night`).
> Those live in the library module under
> `compose-highlight/src/main/assets/compose-highlight/themes/` and are
> separate from this directory.

## How themes are loaded in the sample app

Theme files are loaded at runtime using [`HighlightTheme.fromAsset()`](../../../../../../../../compose-highlight/src/main/kotlin/dev/hossain/highlight/engine/HighlightTheme.kt),
which reads the CSS file via `AssetManager` and lazily parses it into a
`Map<String, SpanStyle>` the first time the theme is applied to a code block.

```kotlin
// Discover all theme names at runtime from AssetManager
val root  = context.assets.list("themes")
              ?.filter { it.endsWith(".min.css") }
              ?.map    { it.removeSuffix(".min.css") }
              ?: emptyList()

val base16 = context.assets.list("themes/base16")
              ?.filter { it.endsWith(".min.css") }
              ?.map    { "base16/${it.removeSuffix(".min.css")}" }
              ?: emptyList()

val allThemes = (root + base16).sorted()   // 256 themes

// Load a specific theme by name
val theme = HighlightTheme.fromAsset(
    context   = context.applicationContext,
    assetPath = "themes/$selectedThemeName.min.css",
    name      = selectedThemeName,
)

// Pass it to the composable
SyntaxHighlightedCode(
    code     = code,
    language = "javascript",
    theme    = theme,
)
```

See [`AllThemesSection.kt`](../../../kotlin/dev/hossain/highlight/sample/sections/AllThemesSection.kt)
for the full implementation.

## Re-downloading themes

To refresh these themes (e.g. for a newer highlight.js version), run:

```bash
VERSION="11.11.1"
DEST="sample/src/main/assets/themes"
mkdir -p "$DEST/base16"

curl -s "https://api.cdnjs.com/libraries/highlight.js/$VERSION?fields=files" \
  | python3 -c "
import json, sys
files = json.load(sys.stdin).get('files', [])
themes = sorted(set(
    f.split('styles/')[1].replace('.min.css','')
    for f in files
    if 'styles/' in f and f.endswith('.min.css')
))
print('\n'.join(themes))
" | while IFS= read -r theme; do
    url="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/$VERSION/styles/${theme}.min.css"
    outfile="$DEST/${theme}.min.css"
    mkdir -p "$(dirname "$outfile")"
    curl -s -o "$outfile" "$url" &
done
wait
echo "Downloaded $(find $DEST -name '*.min.css' | wc -l) themes"
```
