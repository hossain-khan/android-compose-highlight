# Re-downloading highlight.js Themes

The sample app bundles **258 minified CSS theme files** for highlight.js 11.12.0
under `sample/src/main/assets/themes/`.

## Upgrading Highlight.js

To upgrade the entire Highlight.js bundle, fixtures, and themes in one step, use:

```bash
./scripts/upgrade-hljs.sh <version>
# Example: ./scripts/upgrade-hljs.sh 11.12.0
```

This script bundles the full 190+ language library via esbuild, downloads all matching CSS themes,
refreshes test fixtures, updates version references, and validates the bridge contract.

## Refreshing Themes Only

If you only need to re-download or refresh the sample app CSS themes from cdnjs, run:

```bash
./scripts/refresh-sample-themes.sh <version>
# Example: ./scripts/refresh-sample-themes.sh 11.12.0
```

## After running

1. Verify the count shown in the **All Themes** tab matches the downloaded total
2. Verify stability with `./gradlew :compose-highlight:assembleDebug :sample:assembleDebug :compose-highlight:test`
3. Commit all new/changed/deleted `.min.css` files along with the README update
