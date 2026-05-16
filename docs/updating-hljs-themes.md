# Re-downloading highlight.js Themes

The sample app bundles **256 minified CSS theme files** for highlight.js 11.11.1
under `sample/src/main/assets/themes/`. Use the script below to refresh them
when upgrading to a newer highlight.js version.

## Script

```bash
VERSION="11.11.1"   # update to the new version
DEST="sample/src/main/assets/themes"
mkdir -p "$DEST/base16"

# Fetch full file list from cdnjs and download all .min.css themes in parallel
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

## After running

1. Update the `VERSION` comment in `sample/src/main/assets/themes/README.md`
2. Update the highlight.js version reference in `AllThemesSection.kt` KDoc
3. Verify the count shown in the **All Themes** tab matches the downloaded total
4. Commit all new/changed/deleted `.min.css` files along with the README update
