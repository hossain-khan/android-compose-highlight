# APK Feature Analysis Playbook

A repeatable process for analyzing how Android apps implement a specific feature (e.g., syntax highlighting, markdown rendering, authentication) by extracting, decoding, and inspecting their APKs. Designed for AI agents to follow step-by-step.

---

## Prerequisites

| Tool | Purpose | How to get it |
|------|---------|---------------|
| `adb` | Extract APKs from device | Android SDK Platform Tools |
| `apktool` (3.0+) | Decode APK resources and smali | https://apktool.org/ — download the `.jar` |
| Java 17+ | Run apktool | Amazon Corretto, Azul Zulu, or any OpenJDK |

**Device setup:** A physical Android device or emulator connected via USB with the target app(s) installed.

---

## Phase 1: Extract APKs from Device

### 1.1 Identify the package name

If you know the Play Store URL, the package name is the `id` query parameter:
```
https://play.google.com/store/apps/details?id=com.example.app
                                              ^^^^^^^^^^^^^^^^
```

Otherwise, list installed packages:
```bash
# All packages
adb shell pm list packages

# Third-party only
adb shell pm list packages -3

# Search by keyword
adb shell pm list packages | grep -i "keyword"
```

### 1.2 Find APK paths on device

```bash
adb shell pm path <package-name>
```

This returns one or more paths. Modern apps use **split APKs** (App Bundles), so expect multiple results:
```
package:/data/app/.../base.apk                    # Main APK (always present)
package:/data/app/.../split_config.arm64_v8a.apk  # Native libraries
package:/data/app/.../split_config.en.apk         # Language resources
package:/data/app/.../split_config.xxhdpi.apk     # Density resources
```

### 1.3 Pull APKs to local machine

Create a directory per app and pull all APK files:
```bash
mkdir -p <app-name>-apk

# Pull each path returned by pm path
adb pull <full-path-to-base.apk> <app-name>-apk/base.apk
adb pull <full-path-to-split1.apk> <app-name>-apk/split_config.arm64_v8a.apk
# ... repeat for all splits
```

**Important:** The `base.apk` contains all code and most resources. Split APKs contain supplementary native libs, language strings, and density-specific drawables. For feature analysis, `base.apk` is the primary target.

---

## Phase 2: Decode APKs with apktool

### 2.1 Decode each app's base APK

```bash
java -jar apktool_<version>.jar d <app-name>-apk/base.apk -o <app-name>-decoded -f
```

**Note on Java:** If `java` points to a non-standard JDK that doesn't work, find an alternative:
```bash
/usr/libexec/java_home -V          # macOS: list installed JDKs
<path-to-jdk>/bin/java -jar ...    # Use specific JDK
```

### 2.2 Understand the decoded output

```
<app-name>-decoded/
├── AndroidManifest.xml     # App manifest (permissions, components, intents)
├── apktool.yml             # Decode metadata
├── assets/                 # Raw bundled files (JS, HTML, CSS, fonts, configs)
├── original/               # Original META-INF and manifest
├── res/                    # Decoded XML resources (layouts, strings, colors, drawables)
├── smali/                  # Disassembled DEX bytecode (classes.dex)
├── smali_classes2/         # From classes2.dex
├── smali_classes3/         # From classes3.dex (if present)
├── ...                     # Additional DEX files
└── unknown/                # Unrecognized files
```

### 2.3 Warnings are normal

Apktool may print `Unresolved resource reference` warnings — this is expected for split APKs where some resources live in the split config files. These do not affect analysis.

---

## Phase 3: Investigate the Feature

This is the core analysis phase. Search across all layers of the decoded APK systematically. The investigation strategy depends on the feature being analyzed, but the layers to search are always the same.

### 3.1 Layer: Assets (`assets/`)

Search for bundled third-party libraries, configuration files, and web content.

```bash
# List all files in assets
find <app-name>-decoded/assets -type f

# Search for specific file types
find <app-name>-decoded/assets -name "*.js"       # JavaScript libraries
find <app-name>-decoded/assets -name "*.css"       # Stylesheets
find <app-name>-decoded/assets -name "*.html"      # WebView content
find <app-name>-decoded/assets -name "*.json"      # Configuration files
find <app-name>-decoded/assets -name "*.wasm"      # WebAssembly modules
```

**What to look for:**
- Bundled JS libraries often have version headers in the first few lines
- HTML files may reference external CDNs or load local scripts
- Config files may list feature flags, endpoints, or library settings

### 3.2 Layer: Library declarations (`res/raw/`)

Many apps bundle an `aboutlibraries.json` or similar file listing their dependencies:
```bash
find <app-name>-decoded/res/raw -type f
cat <app-name>-decoded/res/raw/aboutlibraries.json  # If it exists
```

This is a goldmine — it lists library names, versions, and descriptions.

### 3.3 Layer: String resources (`res/values/`)

```bash
# Search for feature-related keywords in strings
grep -i "<keyword>" <app-name>-decoded/res/values/strings.xml

# Also check other value files
grep -ri "<keyword>" <app-name>-decoded/res/values/
```

**What to look for:**
- UI labels ("Copy code", "Expand", "Share")
- Theme/mode names ("Dark", "Light", "System")
- Language names ("python", "javascript") if the feature is language-related
- Feature names or settings labels

### 3.4 Layer: Smali bytecode (`smali*/`)

Smali is the human-readable form of Dalvik bytecode. Even with R8/ProGuard obfuscation, string literals are preserved and class/method structures are intact.

#### Search for feature-related string literals

```bash
# Case-insensitive search across all smali directories
grep -ri "<keyword>" <app-name>-decoded/smali*/

# Common patterns:
grep -ri "highlight" <app-name>-decoded/smali*/
grep -ri "syntax" <app-name>-decoded/smali*/
grep -ri "markdown" <app-name>-decoded/smali*/
grep -ri "WebView" <app-name>-decoded/smali*/
```

#### Search for known library package names

```bash
# Well-known Android libraries (adjust to your feature area)
grep -r "io/noties/markwon" <app-name>-decoded/smali*/        # Markwon markdown
grep -r "org/commonmark" <app-name>-decoded/smali*/            # CommonMark parser
grep -r "io/coil" <app-name>-decoded/smali*/                   # Coil image loading
grep -r "com/google/gson" <app-name>-decoded/smali*/           # Gson JSON
grep -r "retrofit2" <app-name>-decoded/smali*/                 # Retrofit HTTP
grep -r "okhttp3" <app-name>-decoded/smali*/                   # OkHttp
```

#### Search for Android framework APIs relevant to the feature

```bash
# Text styling (syntax highlighting)
grep -r "ForegroundColorSpan" <app-name>-decoded/smali*/
grep -r "SpannableString" <app-name>-decoded/smali*/
grep -r "TypefaceSpan" <app-name>-decoded/smali*/

# WebView usage
grep -r "loadDataWithBaseURL\|loadUrl\|evaluateJavascript" <app-name>-decoded/smali*/
grep -r "WebViewClient" <app-name>-decoded/smali*/
grep -r "addJavascriptInterface" <app-name>-decoded/smali*/

# Custom Views
grep -r "onDraw\|Canvas" <app-name>-decoded/smali*/
```

#### Search for feature flags

```bash
grep -ri "feature_flag\|feature_toggle\|experiment" <app-name>-decoded/smali*/
# Look for packed-switch statements near feature name strings
```

### 3.5 Layer: Android Manifest

```bash
cat <app-name>-decoded/AndroidManifest.xml
```

**What to look for:**
- Permissions that hint at feature implementation
- Activities, Services, Receivers, Providers related to the feature
- WebView-related activities
- Intent filters that reveal feature entry points

### 3.6 Layer: Fonts and drawables

```bash
# Bundled fonts
find <app-name>-decoded/res/font -type f 2>/dev/null
find <app-name>-decoded/assets -name "*.ttf" -o -name "*.otf" -o -name "*.woff*" 2>/dev/null

# Drawables (icons, images)
ls <app-name>-decoded/res/drawable*/
```

### 3.7 Tracing obfuscated code

When you find a relevant string in an obfuscated class (e.g., `Labc;`):

1. **Read the full smali file** to understand the class structure
2. **Look for field types and method signatures** — these reference Android framework classes which are NOT obfuscated
3. **Follow cross-references** — search for the class name in other smali files:
   ```bash
   grep -r "Labc;" <app-name>-decoded/smali*/
   ```
4. **Check static initializers** (`<clinit>`) for constants and configuration
5. **Look at the superclass** — `.super Landroid/webkit/WebViewClient;` immediately tells you what the class does

---

## Phase 4: Synthesize Findings

After investigating all layers, organize findings into:

### 4.1 Architecture summary

- What libraries/frameworks does the app use for this feature?
- Is the rendering native (Android Views/Compose), web-based (WebView), or hybrid?
- What is the data flow from input to rendered output?

### 4.2 Evidence inventory

For each claim, record:
- **File path** (relative to decoded root)
- **Line numbers** in the smali/resource file
- **The actual code or content** that supports the claim
- **Interpretation** of what the evidence means

### 4.3 Comparison matrix (if analyzing multiple apps)

Build a table comparing:
- Implementation approach
- Libraries used (with versions)
- Rendering surface (native vs WebView vs hybrid)
- Theme support
- Language/feature coverage
- External dependencies (CDN, APIs)
- Security measures
- Offline capability
- APK size impact

---

## Phase 5: Generate Reports

Produce two deliverables:

### 5.1 Analysis Report (`analysis-report.html`)

A clean, readable summary with:
- Executive summary (one paragraph)
- Per-app sections with architecture diagrams, library tables, and key findings
- Side-by-side comparison table
- Conclusions discussing tradeoffs

**Style:** Clean minimal light theme, self-contained HTML (no external dependencies), professional typography.

### 5.2 Technical Reference (`technical-reference.html`)

A detailed evidence document with:
- Every claim from the analysis report paired with its supporting code evidence
- Actual code snippets from smali, JS, XML, JSON files with file paths and line numbers
- Explanatory notes for each evidence block
- Cross-link back to the analysis report

**Style:** Same theme as analysis report. Use collapsible sections for large code blocks. Color-code file type labels (smali, json, xml, js).

---

## Quick Reference: Common Feature Investigation Queries

### Syntax Highlighting / Code Rendering
```bash
grep -ri "highlight\|syntax\|prism\|codemirror\|shiki\|treesitter\|prettify" smali*/
grep -ri "markwon\|commonmark\|markdown" smali*/
grep -ri "CodeBlock\|code_block\|codeblock" smali*/
find assets/ -name "*.js" -o -name "*.css" | head -20
grep -r "ForegroundColorSpan\|BackgroundColorSpan" smali*/
```

### Authentication / Login
```bash
grep -ri "oauth\|jwt\|bearer\|auth_token\|login\|signin\|sign_in" smali*/
grep -ri "biometric\|fingerprint\|face_id" smali*/
grep -r "AccountManager\|CredentialManager" smali*/
```

### Networking / API
```bash
grep -ri "base_url\|api_url\|endpoint" smali*/
grep -r "retrofit2\|okhttp3\|Volley\|HttpURLConnection" smali*/
grep -ri "graphql\|grpc\|websocket" smali*/
```

### Image Loading
```bash
grep -r "Glide\|Picasso\|io/coil\|Fresco" smali*/
find assets/ -name "*.webp" -o -name "*.png" -o -name "*.svg" | head -20
```

### Analytics / Tracking
```bash
grep -ri "analytics\|tracking\|event_name\|log_event" smali*/
grep -r "firebase\|amplitude\|mixpanel\|segment" smali*/
```

### Local Storage / Database
```bash
grep -r "Room\|SQLiteDatabase\|realm\|ObjectBox" smali*/
grep -ri "shared_pref\|SharedPreferences\|DataStore" smali*/
```

---

## Tips for AI Agents

1. **Start broad, then narrow.** Begin with `find` and `grep` across the whole decoded directory, then read specific files once you have leads.
2. **String literals survive obfuscation.** Even in R8-optimized code, `const-string` instructions preserve the original strings. These are your primary search targets.
3. **Library dependency files are the fastest path.** If `res/raw/aboutlibraries.json` exists, read it first — it often answers the "what libraries" question immediately.
4. **Assets tell the truth.** Bundled `.js`, `.html`, and `.css` files are unobfuscated and often contain version headers, license comments, and clear function names.
5. **Follow the framework classes.** Obfuscated class `Labc;` extending `Landroid/webkit/WebViewClient;` is a WebViewClient. The superclass, interface implementations, and Android API calls are never obfuscated.
6. **Cross-reference aggressively.** When you find a key class, search for it across all smali directories to map its callers and understand the data flow.
7. **The `base.apk` has everything you need.** Split APKs contain supplementary resources (native libs, density drawables, language strings) but all code and core resources are in `base.apk`.
8. **Run searches in parallel.** When investigating multiple apps or multiple search queries, run them concurrently to save time.
