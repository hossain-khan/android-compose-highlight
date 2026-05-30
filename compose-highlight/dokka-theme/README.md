# Dokka theme — Zensical / MkDocs Material parity

This directory makes the Dokka-generated API reference at `/api/` look like the
Zensical / MkDocs Material site at `/`. Same header chrome, same nested left
sidebar, same palette toggle, same fonts, same footer.

The Dokka **content area** (API tables, type signatures, parameter lists,
package indexes, Members tab strip) keeps Dokka's own DOM and styling. Only the
surrounding chrome is replaced.

## Files

| File | Role |
|---|---|
| `zensical-overrides.css` | Loads Inter + JetBrains Mono fonts. Overrides Dokka's CSS variables for both light + dark schemes (Material indigo / slate). Hides Dokka's native top header + sidebar. Styles the injected nested nav (chevrons, type-icons, indent, density). Widens the primary sidebar to fit long Kotlin type names. Hides Dokka's stock footer. |
| `zensical-assets/main.fba56155.min.css` | Frozen copy of the MkDocs Material chrome stylesheet from Zensical 0.0.43. Provides every `.md-*` selector our injected DOM relies on. |
| `zensical-assets/palette.dfe2e883.min.css` | Frozen copy of MkDocs Material's palette stylesheet. Defines `[data-md-color-scheme="default"]` and `[data-md-color-scheme="slate"]` color variables. |
| `dokka-zensical-chrome.js` | Runs at `DOMContentLoaded` on every page. Fetches `navigation.html`, builds Material `md-header` / `md-nav--primary` / `md-content` / `md-footer-meta` around Dokka's `#content`, wires the palette toggle (synced with `html.theme-dark` + localStorage), delegates the Material search icon to Dokka's inner `#pages-search` button. |

## How it ships

`compose-highlight/build.gradle.kts` (the `dokka { }` block) feeds the three CSS
files to `pluginsConfiguration.html.customStyleSheets` and the JS file to
`customAssets`. Dokka emits `<link>` and `<script>` tags into the `<head>` of
every generated page automatically — Dokka 2.x's `customAssets` mechanism
auto-injects `<script type="text/javascript" src="..." async="async">` tags
without needing a `templatesDir` overlay (verified at setup time via probe).

## Reproducible CI flow

The full deploy pipeline runs on `main` push via `.github/workflows/docs.yml`:

1. CI checks out the repo (which includes `compose-highlight/dokka-theme/`).
2. Runs `./gradlew :compose-highlight:dokkaGeneratePublicationHtml` — Dokka
   reads the `dokka { }` block, copies our CSS + JS into the output, emits the
   `<link>` and `<script>` tags into every generated page.
3. Runs `zensical build --clean` — Zensical passes the rethemed `docs/api/`
   tree through verbatim into `site/api/`.
4. Uploads `site/` to GitHub Pages.

So **changes to anything in this directory are picked up automatically on the
next push to `main`** — no manual steps. The Dokka generation step doesn't
require Zensical, so the API ref is rethemed even if Zensical hasn't been built
locally.

## Enabling and disabling

The retheme is enabled by the presence of the `pluginsConfiguration.html` block
in `compose-highlight/build.gradle.kts`:

```kotlin
dokka {
    // …
    pluginsConfiguration.html {
        customStyleSheets.from(
            layout.projectDirectory.file("dokka-theme/zensical-overrides.css"),
            layout.projectDirectory.file("dokka-theme/zensical-assets/main.fba56155.min.css"),
            layout.projectDirectory.file("dokka-theme/zensical-assets/palette.dfe2e883.min.css"),
        )
        customAssets.from(layout.projectDirectory.file("dokka-theme/dokka-zensical-chrome.js"))
    }
}
```

To **disable** the retheme and revert to Dokka's stock JetBrains chrome,
comment out or delete the entire `pluginsConfiguration.html { … }` block, then
regenerate. No file deletions needed — Dokka simply stops loading the assets.

To **re-enable**, uncomment the block. The four files in this directory remain
the source of truth.

## Local preview

Generated `docs/api/` is served by a static file server. The injection script
uses `fetch()` to load `navigation.html`, which **does not work over the
`file://` protocol** — you must serve over HTTP.

```bash
./gradlew :compose-highlight:dokkaGenerate
cd docs && python3 -m http.server 8765
# Open http://localhost:8765/api/index.html
```

Serving from `docs/` (not `docs/api/`) is required so the logo at
`../assets/images/logo.png` resolves above the `/api/` root. This matches the
deployed GitHub Pages layout, where the same `assets/images/logo.png` is
reachable above the API ref.

## Refresh procedure (when Zensical is upgraded)

The two assets in `zensical-assets/` are pinned by hash, so a Zensical version
bump will start regenerating the `site/` directory with new filenames
(e.g. `main.NEWHASH.min.css`). When that happens:

1. `python -m zensical build` (or `.venv/bin/zensical build`) to regenerate
   `site/`.
2. Copy the new hashed files from `site/assets/stylesheets/modern/` into
   `compose-highlight/dokka-theme/zensical-assets/`. Delete the old files.
3. Update the file references in `compose-highlight/build.gradle.kts` to match
   the new hashes.
4. Regenerate the API docs:
   `./gradlew :compose-highlight:dokkaGenerate`.
5. Eyeball both sites side-by-side (`/` vs `/api/`); the Material chrome class
   names are stable across MkDocs Material releases, so functional regressions
   are unlikely. Visual drift (spacing, palette tweaks) is what to watch.

## Breakage points

The retheme depends on three sets of selectors. If any change in Dokka or
Material upgrades, the script silently falls back rather than crashing the
page — but the Material chrome won't render correctly until the script is
updated.

**Dokka selectors** (in generated `docs/api/*.html`):

- `#main` — outer wrapper. Script appends our chrome inside it.
- `#content` (= `.main-content`) — the Dokka content area. Script moves it
  inside `.md-content__inner`.
- `#leftColumn`, `#navigation-wrapper`, `.library-name`,
  `.navigation-controls--break`, `#main > .footer` — Dokka's stock chrome.
  Hidden via `display: none` in `zensical-overrides.css`.
- `#filter-section` and `#searchBar` — kept in the DOM but pushed off-screen
  with `position: absolute; left: -10000px`. **Critical:** these must remain
  reachable so Dokka's `platform-content-handler.js` can populate filters on
  `window.load` (otherwise the Members section gets hidden as "all
  documentation is filtered"), and so search delegation can `.click()` Dokka's
  inner `#pages-search` button.
- `#pages-search` — the inner Ring-UI button inside `#searchBar`. Material's
  search icon programmatically clicks this to open Dokka's existing search
  popup. (Clicking the outer `#searchBar` doesn't work — Ring UI ignores
  synthetic clicks on the wrapper.)
- `navigation.html` (sibling of `index.html` at the API root) — fetched by the
  script for the sidebar tree. DOM structure:
  `.toc--part > .toc--row > .toc--link > .toc--link-grid > .toc--icon.<type>`
  with `data-nesting-level`. Type icons: `class-kt`, `exception-class`,
  `object`, `function`, `enum-class`, `interface-kt`, `annotation-kt`.
- `images/arrow-down.svg` — used as the sidebar chevron. The SVG ships
  white-filled (intended for dark theme); we apply `filter: invert(1)` in
  light mode.

**Material selectors** (in the injected DOM):

- `.md-header.md-header--shadow > .md-header__inner.md-grid` — top header.
- `.md-container > .md-main > .md-main__inner.md-grid` — body grid.
- `.md-sidebar.md-sidebar--primary` — left nav (width override: `16rem`).
- `.md-nav.md-nav--primary > .md-nav__list > .md-nav__item.md-nav__item--nested`
   — sidebar tree items. Each nested item: `<input.md-nav__toggle>` (hidden
   checkbox) + `<div.md-nav__row>` (`<a.md-nav__link>` + `<label.md-nav__chevron>`)
   + `<nav.md-nav>` (children). Chevron rotation driven by
   `:has(> .md-nav__toggle:checked)` on the parent `<li>`.
- `.md-content > article.md-content__inner.md-typeset` — Dokka content wrap.
- `.md-footer-meta > .md-footer-meta__inner.md-grid > .md-copyright` — footer.
- `<button data-palette-target="default|slate">` — palette toggle buttons.
  Click handlers swap `data-md-color-scheme` on `<body>`, `theme-dark` class on
  `<html>`, and persist the choice in localStorage.

**LocalStorage palette key shape:**

```
key   = `${apiRoot.pathname}.__palette`     // e.g. "/android-compose-highlight/api/.__palette"
value = JSON.stringify({
    color: { media: "none", scheme: "default"|"slate", primary: "indigo", accent: "indigo" }
})
```

`apiRoot` is computed by stripping `images/dokka-zensical-chrome.js` from the
script's own `src` attribute (Dokka emits relative URLs that vary with page
depth). The script reads/writes only this api-root key — separate from
Material's own per-directory scoping on `/`. See "Scoping" below.

**Scoping note:** Material's own init script per-pathname-scopes the palette
key (a flip on `/foo/` doesn't carry to `/bar/`). The chrome script
deliberately deviates: it anchors the key to `/api/`'s root, so the toggle is
sticky throughout the API reference regardless of which class detail page the
user is on. The Zensical site at `/` continues to use Material's own
per-directory scope, so a flip on `/` does NOT carry into `/api/` — users
toggle once per site.

## Out of scope

- **Material's `navigation.instant` page swaps.** `/api/` does full reloads.
  Visual parity is preserved; only the swap animation differs.
- **Material's Lunr search modal.** The Material search icon delegates to
  Dokka's existing search popup (which has its own pages.json index).
- **Right-side "On this page" TOC.** Dokka pages don't have enough heading
  depth (≥2 H2/H3 inside `#content`) to justify it.
- **Per-page edit-on-GitHub link.** Dokka pages are generated from KDoc, not
  markdown — there's no clean source-file mapping.
- **Mobile drawer behavior** (the `for="__drawer"` label + `#__drawer`
  checkbox toggle below ~76rem viewport). Untested; if you browse the API
  reference on mobile, this needs validation.

## Common pitfalls and how to debug them

This section captures the lessons learned from iterating on the retheme. When
you find a new visual or behavioral issue, start here.

### Symptom → likely cause table

| Symptom | Likely cause | Where to fix |
|---|---|---|
| Visual style (font, spacing, color) doesn't apply | Material's `.md-typeset` rule wins the cascade — same specificity, loaded after ours | Add `!important` or write a more specific selector in `zensical-overrides.css` |
| Element renders in unexpected position | A Ring UI / Dokka script applies inline styles or computed offsets at runtime | Use DevTools to inspect the live element's `style="..."` attribute and computed style; override with a CSS rule that uses higher specificity + `!important` |
| Element is missing from the page | Element is being hidden by Dokka's stock CSS, OR our hide rule is too aggressive (e.g. `#filter-section` hidden = Members section breaks) | Check console for errors; verify the element is in the DOM via DevTools; if found, check what's hiding it |
| Sidebar paints over the footer / content | `.md-sidebar__scrollwrap` height is unconstrained — sticky sidebars need explicit height clamp | Bound height via `calc(100vh - <header-height>)` and `overflow-y: auto` |
| Popup or modal opens at wrong viewport coordinates | Ring UI / React component renders into a body portal target with JS-applied inline `top`/`left`/`margin` | Match the portal element's data attributes (e.g. `[data-test="ring-popup"]`) and override with `position: fixed` + explicit viewport coords + `!important` |
| Click on a Dokka-internal element (search button, filter, etc.) does nothing | Element is positioned with `pointer-events: none` OR a parent has it; OR Dokka uses a Ring UI wrapper that ignores synthetic clicks | Find the inner functional element (e.g. `#pages-search` inside `#searchBar`) and click *that* instead |
| Members / tabs / filter buttons fail to render after page load | Dokka's `platform-content-handler.js` ran during `window.load` and threw because something it expected to find was missing or detached | Check console for `Cannot read properties of undefined`. Common causes: hiding `#filter-section` with `display: none` (use off-screen positioning instead), or moving `#content` out of `#main` during an `await` |
| Icon/chevron is invisible | Dokka SVG uses white fill (intended for its dark theme); on our light theme it disappears | Apply `filter: invert(1)` in light mode, no filter in `.theme-dark` |
| Asset (logo, font, image) 404s when previewing locally but works on GitHub Pages | Local server is rooted at `docs/api/` instead of `docs/` | Always serve from `docs/`: `cd docs && python3 -m http.server 8765` then open `/api/index.html` |
| Sidebar/header positioning works at desktop but breaks on narrow viewports | Material's media query at `~76.234375em` (1219px) switches the sidebar to mobile drawer mode; our overrides don't account for this breakpoint | Out of scope for current implementation; mobile drawer requires the `#__drawer` checkbox + label wiring |

### Standard debugging workflow

When something looks off, follow this sequence:

1. **Reproduce locally** (`./gradlew :compose-highlight:dokkaGenerate` then `cd docs && python3 -m http.server 8765`).
2. **Inspect with DevTools.** Right-click the broken element → Inspect. Look at:
   - **Computed** tab: which CSS property has the wrong value?
   - **Styles** tab: which rule(s) are applying that value? Material's bundle (`main.fba56155.min.css`) vs ours (`zensical-overrides.css`)? Inline `style=""` from JS?
   - **Click "Show all" or check filter**: is our rule there but crossed out (overridden)?
3. **Check the console** for JS errors. If `platform-content-handler.js` or another Dokka script throws, the page fell back to broken state mid-init. The error stack tells you which selector failed.
4. **Identify the layer**:
   - **Pure CSS issue**: edit `zensical-overrides.css`. Use `.md-typeset` scope and `!important` if Material is fighting back.
   - **DOM structure issue**: edit `dokka-zensical-chrome.js`. The script runs at `DOMContentLoaded` and rebuilds chrome before Dokka's `window.load` scripts run.
   - **Dokka script side-effect**: an internal Dokka script ran later and modified the DOM. Either prevent it (by keeping the element it expects) or work around it (override its result via post-load CSS or JS).
5. **Use Playwright Agent CLI** for repeatable validation (the project's tooling preference):
   ```bash
   playwright-cli -s=debug open http://localhost:8765/api/<failing-page>
   playwright-cli -s=debug eval "/* IIFE returning JSON.stringify(...) */"
   playwright-cli -s=debug screenshot
   ```
   Don't trust visual inspection alone — measure with `getBoundingClientRect()`, computed styles, etc.

### Specificity cheat-sheet

When fighting Material's bundle for a property:

| Material rule (typical) | Specificity | Beat with |
|---|---|---|
| `.md-typeset li` | 0,1,1 | `.md-typeset ol > li` (still 0,1,2) + `!important`, OR raise to `.md-content__inner .md-typeset li` |
| `.md-sidebar--primary` (no descendant) | 0,1,0 | `.md-sidebar.md-sidebar--primary` (0,2,0) — naturally wins |
| Inline `style=""` from Ring UI | 1,0,0,0 | Always need `!important`; CSS specificity can't beat inline without it |
| `.theme-dark` + descendant | 0,1+ | We're already inside `.theme-dark` for dark mode rules |

### Things that are dangerous to "fix"

A few rules in `zensical-overrides.css` exist to prevent regressions and look weird in isolation. Don't simplify them without understanding why:

- **`#filter-section` and `#searchBar` use `position: absolute; left: -10000px`, NOT `display: none`.** Hiding them with `display: none` makes Dokka's filter init throw and the Members section disappears. Off-screen positioning keeps them addressable so Dokka's scripts don't fail.
- **The chrome script `await fetch('navigation.html')` BEFORE moving `#content`.** Reversing this order opens a race window where Dokka's `window.load` scripts can observe `#content` detached from the document. Trust the existing ordering.
- **`.md-sidebar.md-sidebar--primary { height: calc(100vh - 3rem) }`.** The sticky sidebar needs an explicit height clamp; without it, the scrollwrap takes its full intrinsic height and overlaps the footer on short pages.
- **`#searchBar` is *not* hidden inside `#main`'s overflow.** If you wrap `#searchBar` in something with `overflow: hidden`, the Ring UI search popup may fail to detach into the body portal correctly.

## Architecture decisions worth knowing

**Async-fetch ordering.** The chrome script `await fetch('navigation.html')`
*before* moving Dokka's `#content` into the new article scaffolding. This
prevents a race where Dokka's `platform-content-handler.js` could observe
`.main-content` detached from the document during the fetch yield, throw,
and abort `initializeFiltering()` — leaving the Members section hidden.

**Auto-expand path to active page.** When rendering the nav tree, the script
tracks which `<li>` ancestors contain the active page and pre-checks their
toggle inputs. The user lands with a visible breadcrumb of context rather
than having to drill down each time.

**Dokka's white SVG chevron.** `arrow-down.svg` is filled with
`rgba(255,255,255,0.96)` because Dokka's stock theme is dark. On our light
chrome we apply `filter: invert(1)` so the arrow shows. In dark mode, no
filter — the arrow appears as authored.

**Frozen Zensical CSS.** Material's class names are extremely stable, but
Zensical may renumber its bundle hash on each release. We embed a frozen
hashed copy here rather than using a CDN, both for offline builds and for
deterministic CI output.
