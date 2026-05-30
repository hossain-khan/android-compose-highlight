// Dokka chrome retheme — wraps Dokka's API reference in MkDocs Material chrome so
// /api/ visually matches the Zensical site at /. Search delegates to Dokka's existing
// #searchBar; palette state is shared with Material via localStorage.
//
// Loaded as a Dokka customAsset; arrives in <head> with async=async, so we gate on
// DOMContentLoaded ourselves. See compose-highlight/dokka-theme/README.md.
(function () {
    "use strict";

    const MD_PALETTE_LS_KEY_SUFFIX = ".__palette";
    const REPO_URL = "https://github.com/hossain-khan/android-compose-highlight";
    const REPO_NAME = "hossain-khan/android-compose-highlight";
    const PROJECT_TITLE = "Highlight";
    const NAVIGATION_HTML_NAME = "navigation.html";
    const COPYRIGHT_HOLDER = "Hossain Khan";

    // Locate Dokka's relative path back to the api/ root (the directory with navigation.html).
    // Dokka emits this very script under `images/dokka-zensical-chrome.js` on every page; the
    // `../` prefix on its `src` tells us the depth.
    function findApiRoot() {
        const myScript = document.querySelector('script[src$="dokka-zensical-chrome.js"]');
        if (myScript) {
            const src = myScript.getAttribute("src") || "";
            return src.replace(/images\/dokka-zensical-chrome\.js$/, "");
        }
        return "";
    }

    // Compute the localStorage key the Material palette script uses, but anchored to /api/'s
    // root (not the current page's directory). Material itself per-pathname-scopes this key,
    // which would mean dark mode resets every time the user navigates between /api/<package>/
    // and /api/<package>/<class>/. Anchoring to /api/'s root keeps the toggle sticky
    // throughout the API reference. The Zensical site at / continues to use its own
    // per-directory scope (we don't touch that), so a flip on / won't carry into /api/ —
    // users toggle once per site, which is acceptable.
    function paletteStorageKey(apiRoot) {
        try {
            const apiScope = new URL(apiRoot || ".", location);
            let pathname = apiScope.pathname;
            if (!pathname.endsWith("/")) pathname = pathname + "/";
            return pathname + MD_PALETTE_LS_KEY_SUFFIX;
        } catch (_) {
            return "/" + MD_PALETTE_LS_KEY_SUFFIX;
        }
    }

    function readStoredPalette(apiRoot) {
        try {
            const raw = localStorage.getItem(paletteStorageKey(apiRoot));
            if (!raw) return null;
            return JSON.parse(raw);
        } catch (_) {
            return null;
        }
    }

    function writeStoredPalette(apiRoot, scheme) {
        const value = {
            color: {
                media: "none",
                scheme: scheme,
                primary: "indigo",
                accent: "indigo",
            },
        };
        try {
            localStorage.setItem(paletteStorageKey(apiRoot), JSON.stringify(value));
        } catch (_) {
            // localStorage may be unavailable (Safari private mode etc); silently ignore.
        }
    }

    function applyScheme(scheme) {
        // Material chrome reads `data-md-color-scheme` on <body>; Dokka reads `.theme-dark` on <html>.
        document.body.setAttribute("data-md-color-scheme", scheme);
        document.body.setAttribute("data-md-color-primary", "indigo");
        document.body.setAttribute("data-md-color-accent", "indigo");
        document.body.setAttribute("data-md-color-media", "none");
        document.documentElement.classList.toggle("theme-dark", scheme === "slate");
    }

    function buildHeader(apiRoot) {
        // Mirror the structure from site/index.html. Logo + title + palette toggle + search
        // delegate + GitHub source link. Inline SVGs lifted byte-for-byte from Zensical's
        // build output (Lucide icons + Font Awesome GitHub mark).
        const header = document.createElement("header");
        header.className = "md-header md-header--shadow";
        header.setAttribute("data-md-component", "header");
        header.innerHTML = `
            <nav class="md-header__inner md-grid" aria-label="Header">
                <a href="${apiRoot}index.html" title="${PROJECT_TITLE}"
                   class="md-header__button md-logo" aria-label="${PROJECT_TITLE}"
                   data-md-component="logo">
                    <img src="${apiRoot}../assets/images/logo.png" alt="${PROJECT_TITLE}">
                </a>
                <label class="md-header__button md-icon" for="__drawer" aria-label="Navigation">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor"
                         stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                         class="lucide lucide-menu" viewBox="0 0 24 24">
                        <path d="M4 5h16M4 12h16M4 19h16"/>
                    </svg>
                </label>
                <div class="md-header__title" data-md-component="header-title">
                    <div class="md-header__ellipsis">
                        <div class="md-header__topic">
                            <span class="md-ellipsis">${PROJECT_TITLE}</span>
                        </div>
                        <div class="md-header__topic" data-md-component="header-topic">
                            <span class="md-ellipsis">API Reference</span>
                        </div>
                    </div>
                </div>
                <form class="md-header__option" data-md-component="palette">
                    <button type="button" class="md-header__button md-icon"
                            title="Switch to dark mode" data-palette-target="slate"
                            data-visible-when="default">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor"
                             stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                             class="lucide lucide-sun" viewBox="0 0 24 24">
                            <circle cx="12" cy="12" r="4"/>
                            <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/>
                        </svg>
                    </button>
                    <button type="button" class="md-header__button md-icon"
                            title="Switch to light mode" data-palette-target="default"
                            data-visible-when="slate">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor"
                             stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                             class="lucide lucide-moon" viewBox="0 0 24 24">
                            <path d="M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401"/>
                        </svg>
                    </button>
                </form>
                <button type="button" class="md-header__button md-icon"
                        aria-label="Search" data-search-trigger>
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor"
                         stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                         class="lucide lucide-search" viewBox="0 0 24 24">
                        <path d="m21 21-4.34-4.34"/>
                        <circle cx="11" cy="11" r="8"/>
                    </svg>
                </button>
                <div class="md-header__source">
                    <a href="${REPO_URL}" title="Go to repository" class="md-source"
                       data-md-component="source">
                        <div class="md-source__icon md-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
                                <path fill="currentColor" d="M439.6 236.1 244 40.5c-5.4-5.5-12.8-8.5-20.4-8.5s-15 3-20.4 8.4L162.5 81l51.5 51.5c27.1-9.1 52.7 16.8 43.4 43.7l49.7 49.7c34.2-11.8 61.2 31 35.5 56.7-26.5 26.5-70.2-2.9-56-37.3L240.3 199v121.9c25.3 12.5 22.3 41.8 9.1 55-6.4 6.4-15.2 10.1-24.3 10.1s-17.8-3.6-24.3-10.1c-17.6-17.6-11.1-46.9 11.2-56v-123c-20.8-8.5-24.6-30.7-18.6-45L142.6 101 8.5 235.1C3 240.6 0 247.9 0 255.5s3 15 8.5 20.4l195.6 195.7c5.4 5.4 12.7 8.4 20.4 8.4s15-3 20.4-8.4l194.7-194.7c5.4-5.4 8.4-12.8 8.4-20.4s-3-15-8.4-20.4"/>
                            </svg>
                        </div>
                        <div class="md-source__repository">${REPO_NAME}</div>
                    </a>
                </div>
            </nav>
        `;
        return header;
    }

    // Walk Dokka's navigation.html DOM and turn `.toc--part` divs into Material `md-nav__item` lists.
    // Dokka's structure: <div class="toc--part" data-nesting-level="N"> contains a `.toc--row`
    // (the link) and zero or more child `.toc--part` divs. Mirror that with <ul><li>...
    //
    // Returns { list, openItems } where openItems is the set of `<li>` elements that contain
    // the active page somewhere in their subtree — used to mark them open by default so the
    // user lands with the path to the current page already expanded.
    function renderNavTree(rootDoc, apiRoot, currentHref) {
        const list = document.createElement("ul");
        list.className = "md-nav__list";
        const ctx = { idCounter: 0, openItems: new Set() };
        rootDoc.querySelectorAll("body > .toc--part").forEach(part => {
            const result = renderNavItem(part, apiRoot, currentHref, 0, ctx);
            if (result) list.appendChild(result.item);
        });
        return { list, openItems: ctx.openItems };
    }

    // Returns { item, hasActiveDescendant } — the caller (parent renderer) uses
    // hasActiveDescendant to know whether to mark itself open by default.
    function renderNavItem(part, apiRoot, currentHref, depth, ctx) {
        const item = document.createElement("li");
        item.className = "md-nav__item";

        const row = part.querySelector(":scope > .toc--row");
        const link = row ? row.querySelector(":scope > .toc--link") : null;
        const childParts = part.querySelectorAll(":scope > .toc--part");
        const hasChildren = childParts.length > 0;

        let isActive = false;
        let containsActive = false;

        // Compute the absolute href once so we can match against currentHref reliably.
        // Resolve relative to location.href (NOT location.origin) so apiRoot's `../`
        // segments combine with the current page's directory correctly.
        let absoluteHref = null;
        if (link) {
            const href = link.getAttribute("href") || "";
            absoluteHref = href ? new URL(apiRoot + href, location.href).pathname : null;
            const currentPath = new URL(currentHref).pathname;
            isActive = absoluteHref === currentPath;
        }

        // For nested items: emit the Material expand/collapse pattern (hidden checkbox +
        // label) so users can open/close subtrees without JS state.
        let expandToggle = null;
        let nestedNav = null;
        if (hasChildren) {
            const navId = "__nav_" + (++ctx.idCounter);
            expandToggle = document.createElement("input");
            expandToggle.className = "md-nav__toggle md-toggle";
            expandToggle.setAttribute("data-md-toggle", navId);
            expandToggle.id = navId;
            expandToggle.type = "checkbox";

            nestedNav = document.createElement("nav");
            nestedNav.className = "md-nav";
            nestedNav.setAttribute("aria-label", link ? (link.textContent || "").trim() : "");
            nestedNav.setAttribute("data-md-level", String(depth + 1));
            const subList = document.createElement("ul");
            subList.className = "md-nav__list";
            childParts.forEach(child => {
                const childResult = renderNavItem(child, apiRoot, currentHref, depth + 1, ctx);
                if (childResult) {
                    subList.appendChild(childResult.item);
                    if (childResult.containsActive) containsActive = true;
                }
            });
            nestedNav.appendChild(subList);

            item.classList.add("md-nav__item--nested");
            item.appendChild(expandToggle);
        }

        if (link) {
            const a = document.createElement("a");
            a.href = apiRoot + (link.getAttribute("href") || "");
            a.className = "md-nav__link";
            // Preserve Dokka's icon + name structure so the existing CSS in
            // ui-kit.min.css renders the type icon (.toc--icon.class-kt etc.).
            const linkGrid = link.querySelector(":scope > .toc--link-grid");
            if (linkGrid) {
                // Clone the icon span and the name span; drop <wbr> hyphenation markers.
                const iconSpan = linkGrid.querySelector(".toc--icon");
                if (iconSpan) {
                    const cloned = iconSpan.cloneNode(true);
                    cloned.classList.add("md-nav__icon-type");
                    a.appendChild(cloned);
                }
                const nameSpan = document.createElement("span");
                nameSpan.className = "md-ellipsis";
                nameSpan.textContent = textWithoutWbr(linkGrid);
                a.appendChild(nameSpan);
            } else {
                // Top-level package label has no link-grid wrapper.
                const nameSpan = document.createElement("span");
                nameSpan.className = "md-ellipsis";
                nameSpan.textContent = (link.textContent || "").trim();
                a.appendChild(nameSpan);
            }

            if (isActive) {
                a.classList.add("md-nav__link--active");
                item.classList.add("md-nav__item--active");
                containsActive = true;
            }

            // For nested items, wrap the link + chevron in a single row so the chevron's
            // `margin-left: auto` pushes it to the right edge.
            if (hasChildren) {
                const navId = expandToggle.id;
                const chevron = document.createElement("label");
                chevron.className = "md-nav__chevron";
                chevron.setAttribute("for", navId);
                chevron.setAttribute("aria-label", "Toggle nested navigation");

                const row = document.createElement("div");
                row.className = "md-nav__row";
                row.appendChild(a);
                row.appendChild(chevron);
                item.appendChild(row);
            } else {
                item.appendChild(a);
            }
        }

        if (hasChildren && nestedNav) {
            item.appendChild(nestedNav);
        }

        // Auto-expand: expand all nested items by default so users can see the full
        // navigation tree without manually drilling down.
        if (expandToggle) {
            expandToggle.checked = true;
            ctx.openItems.add(item);
        }

        return { item: item, containsActive: containsActive || isActive };
    }

    // Strip <wbr> tags but preserve the text + spaces between sibling spans.
    function textWithoutWbr(element) {
        return Array.from(element.childNodes)
            .filter(n => !(n.nodeType === Node.ELEMENT_NODE && n.tagName === "WBR"))
            .map(n => n.textContent || "")
            .join("")
            .trim();
    }

    function buildPrimarySidebar(navTree, apiRoot) {
        const sidebar = document.createElement("div");
        sidebar.className = "md-sidebar md-sidebar--primary";
        sidebar.setAttribute("data-md-component", "sidebar");
        sidebar.setAttribute("data-md-type", "navigation");
        sidebar.innerHTML = `
            <div class="md-sidebar__scrollwrap">
                <div class="md-sidebar__inner">
                    <nav class="md-nav md-nav--primary" aria-label="Navigation"
                         data-md-level="0"></nav>
                </div>
            </div>
        `;
        const nav = sidebar.querySelector(".md-nav--primary");

        // Inject "Back to Docs" link at the top of the sidebar
        const backItem = document.createElement("li");
        backItem.className = "md-nav__item";
        const backLink = document.createElement("a");
        backLink.href = apiRoot + "../index.html";
        backLink.className = "md-nav__link";
        backLink.innerHTML = `
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor"
                 stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                 class="lucide lucide-arrow-left" viewBox="0 0 24 24"
                 style="width:1.2em;height:1.2em;vertical-align:middle;margin-right:0.3em;">
                <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
            <span class="md-ellipsis">Back to Docs</span>
        `;
        backItem.appendChild(backLink);
        nav.appendChild(backItem);

        nav.appendChild(navTree);
        return sidebar;
    }

    // Material-style footer: copyright + "Made with…" line. Mirrors the
    // .md-footer-meta block from Zensical's site so /api/ ends with the same
    // baseline as /. We skip Material's prev/next strip because Dokka pages
    // have no curriculum order — the package tree is the IA.
    function buildFooter() {
        const year = new Date().getFullYear();
        const footer = document.createElement("footer");
        footer.className = "md-footer";
        footer.innerHTML = `
            <div class="md-footer-meta md-typeset">
                <div class="md-footer-meta__inner md-grid">
                    <div class="md-copyright">
                        <div class="md-copyright__highlight">
                            Copyright &copy; ${year} ${COPYRIGHT_HOLDER}
                        </div>
                        Generated by
                        <a href="https://kotlinlang.org/docs/dokka-introduction.html"
                           target="_blank" rel="noopener">Dokka</a>
                    </div>
                </div>
            </div>
        `;
        return footer;
    }

    function buildSecondaryToc(content) {
        // Build right-side "On this page" from h2/h3/h4 anchors inside #content. Skip if fewer
        // than 2 headings — pages with one heading don't benefit from a TOC.
        const headings = content.querySelectorAll("h2[id], h3[id], h4[id]");
        if (headings.length < 2) return null;

        const sidebar = document.createElement("div");
        sidebar.className = "md-sidebar md-sidebar--secondary";
        sidebar.setAttribute("data-md-component", "sidebar");
        sidebar.setAttribute("data-md-type", "toc");

        const list = document.createElement("ul");
        list.className = "md-nav__list";
        list.setAttribute("data-md-component", "toc");

        headings.forEach(h => {
            const li = document.createElement("li");
            li.className = "md-nav__item";
            const a = document.createElement("a");
            a.href = "#" + h.id;
            a.className = "md-nav__link";
            a.innerHTML = `<span class="md-ellipsis"><span class="md-typeset">${h.textContent.trim()}</span></span>`;
            li.appendChild(a);
            list.appendChild(li);
        });

        sidebar.innerHTML = `
            <div class="md-sidebar__scrollwrap">
                <div class="md-sidebar__inner">
                    <nav class="md-nav md-nav--secondary" aria-label="On this page">
                        <label class="md-nav__title">On this page</label>
                    </nav>
                </div>
            </div>
        `;
        sidebar.querySelector(".md-nav--secondary").appendChild(list);
        return sidebar;
    }

    function refreshPaletteButtonVisibility(currentScheme) {
        // Each button carries data-visible-when=<scheme>; show only the one matching the
        // currently-active scheme so users see the sun icon in light mode (offering to flip
        // to dark) and the moon icon in dark mode (offering to flip to light).
        document.querySelectorAll("[data-palette-target]").forEach(button => {
            const visibleWhen = button.getAttribute("data-visible-when");
            button.hidden = visibleWhen !== currentScheme;
        });
    }

    function wirePalette(apiRoot) {
        const stored = readStoredPalette(apiRoot);
        const initialScheme = (stored && stored.color && stored.color.scheme) || "default";
        applyScheme(initialScheme);
        refreshPaletteButtonVisibility(initialScheme);

        document.querySelectorAll("[data-palette-target]").forEach(button => {
            button.addEventListener("click", () => {
                const target = button.getAttribute("data-palette-target");
                applyScheme(target);
                writeStoredPalette(apiRoot, target);
                refreshPaletteButtonVisibility(target);
            });
        });
    }

    function wireSearchDelegation() {
        // Dokka's outer #searchBar is a Ring UI wrapper that doesn't respond to programmatic
        // clicks. The inner #pages-search <button> is what actually opens the search popup.
        document.querySelectorAll("[data-search-trigger]").forEach(button => {
            button.addEventListener("click", e => {
                e.preventDefault();
                const dokkaSearchButton = document.getElementById("pages-search");
                if (dokkaSearchButton) dokkaSearchButton.click();
            });
        });
    }

    async function rebuildChrome() {
        const apiRoot = findApiRoot();

        const main = document.getElementById("main");
        const content = document.getElementById("content");
        if (!main || !content) return;

        // 1. Fetch navigation.html FIRST. Critical: do this before mutating the DOM.
        // Dokka's platform-content-handler.js runs on `window.load` and walks
        // `getElementsByClassName('main-content')[0]` — if we've moved #content into a
        // detached <article> while awaiting fetch, that lookup returns undefined and the
        // script throws, leaving the Members section unrendered. Keeping #content in
        // place during the await keeps Dokka's init sequence stable.
        let primarySidebar = null;
        try {
            const response = await fetch(apiRoot + NAVIGATION_HTML_NAME);
            if (response.ok) {
                const html = await response.text();
                const parser = new DOMParser();
                const doc = parser.parseFromString(html, "text/html");
                const navResult = renderNavTree(doc, apiRoot, location.href);
                primarySidebar = buildPrimarySidebar(navResult.list, apiRoot);
            }
        } catch (_) {
            // Silent: if navigation.html is unreachable, we just render without the left nav.
        }

        // 2. Build the Material scaffolding off-DOM and move #content into it.
        // From this point on we run synchronously — no awaits — so the only window in which
        // #content is detached is between the article.appendChild and the final main.append.
        // Since Dokka's filter init is single-shot and tied to window.load, it has already
        // run (or is queued behind us) and won't observe the gap.
        const container = document.createElement("div");
        container.className = "md-container";
        container.setAttribute("data-md-component", "container");

        const mdMain = document.createElement("main");
        mdMain.className = "md-main";
        mdMain.setAttribute("data-md-component", "main");

        const mdMainInner = document.createElement("div");
        mdMainInner.className = "md-main__inner md-grid";

        const mdContent = document.createElement("div");
        mdContent.className = "md-content";
        mdContent.setAttribute("data-md-component", "content");
        const article = document.createElement("article");
        article.className = "md-content__inner md-typeset";
        article.appendChild(content);
        mdContent.appendChild(article);

        // 3. Right-side "On this page" TOC.
        const secondarySidebar = buildSecondaryToc(article);

        if (primarySidebar) mdMainInner.appendChild(primarySidebar);
        mdMainInner.appendChild(mdContent);
        if (secondarySidebar) mdMainInner.appendChild(secondarySidebar);
        mdMain.appendChild(mdMainInner);
        container.appendChild(mdMain);

        // 4. Atomic-ish swap: hide Dokka's surviving #main children, then append our chrome.
        // Don't innerHTML="" because Dokka's filter init may try to read remaining children
        // (e.g. .filtered-message it created). Hiding via display:none keeps them addressable.
        // The footer must be a sibling of .md-container (not a child) — Material's footer
        // CSS positions itself in normal flow below the container; placing it inside
        // .md-container puts it under the sticky sidebar's painting area, which causes
        // the sidebar to overlap the footer when the right-side content is short.
        Array.from(main.children).forEach(child => { child.style.display = "none"; });
        const header = buildHeader(apiRoot);
        main.appendChild(header);
        main.appendChild(container);
        main.appendChild(buildFooter());
        main.setAttribute("data-zensical-chrome", "active");

        // 5. Wire palette + search behavior.
        wirePalette(apiRoot);
        wireSearchDelegation();
    }

    // Pre-flight: apply the persisted scheme as early as possible so dark-mode users don't
    // see a flash of light chrome. This runs before DOMContentLoaded; the body element
    // already exists in the doc by the time the async script starts evaluating.
    function preflightApplyScheme() {
        const apiRoot = findApiRoot();
        const stored = readStoredPalette(apiRoot);
        const initialScheme = (stored && stored.color && stored.color.scheme) || "default";
        // <body> exists at this point because <script async> defers until parsing reaches it.
        if (document.body) applyScheme(initialScheme);
        else document.documentElement.classList.toggle("theme-dark", initialScheme === "slate");
    }
    preflightApplyScheme();

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", () => { rebuildChrome(); });
    } else {
        rebuildChrome();
    }
})();
