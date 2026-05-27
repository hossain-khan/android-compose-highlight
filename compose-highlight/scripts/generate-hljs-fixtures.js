// Generate HTML token fixtures for the Roborazzi screenshot tests by feeding the snippets in
// `snippets.json` to the exact same `highlight.min.js` bundle the library ships at runtime.
//
// The bundled file is an IIFE (`var hljs=(()=>{...})()`), not a CommonJS or UMD module - it
// assigns to a `var hljs` in its own scope and never sets `module.exports`. So a plain
// `require()` returns an empty object. We use `vm.runInNewContext` to execute the bundle in a
// sandbox and pull `hljs` out of the resulting global object.
//
// Usage:
//   node scripts/generate-hljs-fixtures.js
//
// Or via Gradle (preferred so contributors do not have to remember the cwd):
//   ./gradlew :compose-highlight:refreshHljsFixtures
//
// Requires Node.js 18+.

const fs = require('fs');
const path = require('path');
const vm = require('vm');

const REPO_ROOT = path.resolve(__dirname, '..');
const HLJS_BUNDLE = path.join(REPO_ROOT, 'src/main/assets/compose-highlight/highlight.min.js');
const FIXTURES_DIR = path.join(REPO_ROOT, 'src/test/resources/highlight-fixtures');
const MANIFEST = path.join(FIXTURES_DIR, 'snippets.json');

function loadHljs() {
    const code = fs.readFileSync(HLJS_BUNDLE, 'utf8');
    const sandbox = {};
    vm.runInNewContext(code, sandbox, { filename: HLJS_BUNDLE });
    if (!sandbox.hljs || typeof sandbox.hljs.highlight !== 'function') {
        throw new Error(`Failed to load hljs from ${HLJS_BUNDLE}: sandbox.hljs is not the expected object`);
    }
    return sandbox.hljs;
}

function generate() {
    const hljs = loadHljs();
    console.log(`Loaded highlight.js ${hljs.versionString} from ${path.relative(REPO_ROOT, HLJS_BUNDLE)}`);

    if (!fs.existsSync(MANIFEST)) {
        throw new Error(`Manifest not found: ${MANIFEST}`);
    }
    const manifest = JSON.parse(fs.readFileSync(MANIFEST, 'utf8'));

    fs.mkdirSync(FIXTURES_DIR, { recursive: true });

    let count = 0;
    for (const [name, entry] of Object.entries(manifest)) {
        const { language, code } = entry;
        if (!language || typeof code !== 'string') {
            throw new Error(`Manifest entry "${name}" must have string fields "language" and "code"`);
        }

        // Reject unknown languages loudly. `hljs.highlight()` throws an error for unregistered
        // languages, but the message is buried; surface it with the entry name for clarity.
        if (!hljs.getLanguage(language)) {
            throw new Error(`Unknown language "${language}" for fixture "${name}". ` +
                `Check that the bundled highlight.min.js registers it; see hljs.listLanguages().`);
        }

        const result = hljs.highlight(code, { language, ignoreIllegals: false });
        const outFile = path.join(FIXTURES_DIR, `${name}.html`);
        fs.writeFileSync(outFile, result.value);
        console.log(`  ${name}.html  (${language}, ${result.value.length} bytes)`);
        count++;
    }
    console.log(`\nWrote ${count} fixture(s) to ${path.relative(REPO_ROOT, FIXTURES_DIR)}/`);
}

try {
    generate();
} catch (err) {
    console.error(`\nERROR: ${err.message}`);
    process.exit(1);
}
