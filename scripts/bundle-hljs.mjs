#!/usr/bin/env node

/**
 * bundle-hljs.mjs - Bundles the full Highlight.js bundle for Android Compose Highlight.
 *
 * Highlight.js on CDN only provides ~36 common languages in its standard bundle (~129 KB).
 * This script installs a specified (or latest) highlight.js version from npm and bundles
 * all 190+ languages into a single self-contained IIFE bundle (`var hljs = ...`) using esbuild.
 *
 * Usage:
 *   node scripts/bundle-hljs.mjs [version]
 *   ./scripts/bundle-hljs.mjs 11.12.0
 */

import { execSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '..');
const OUTPUT_FILE = path.join(REPO_ROOT, 'compose-highlight/src/main/assets/compose-highlight/highlight.min.js');

const targetVersion = process.argv[2] || 'latest';

const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'hljs-bundle-'));
console.log(`Working directory: ${tempDir}`);

try {
  console.log(`Installing highlight.js@${targetVersion} and esbuild...`);
  execSync(`npm install --no-package-lock --no-audit --no-fund highlight.js@${targetVersion} esbuild@0.28.2`, {
    cwd: tempDir,
    stdio: 'inherit',
  });

  const hljsPkgPath = path.join(tempDir, 'node_modules/highlight.js/package.json');
  const hljsPkg = JSON.parse(fs.readFileSync(hljsPkgPath, 'utf8'));
  const installedVersion = hljsPkg.version;
  console.log(`Installed highlight.js version: ${installedVersion}`);

  const entryPoint = path.join(tempDir, 'node_modules/highlight.js/lib/index.js');
  if (!fs.existsSync(entryPoint)) {
    throw new Error(`Entry point not found: ${entryPoint}`);
  }

  // Ensure output directory exists
  fs.mkdirSync(path.dirname(OUTPUT_FILE), { recursive: true });

  console.log(`Bundling with esbuild to ${path.relative(REPO_ROOT, OUTPUT_FILE)}...`);
  const esbuildPath = path.join(tempDir, 'node_modules/.bin/esbuild');
  execSync(
    `"${esbuildPath}" "${entryPoint}" --bundle --minify --format=iife --global-name=hljs --outfile="${OUTPUT_FILE}"`,
    { stdio: 'inherit' }
  );

  // Validate resulting bundle
  const bundledCode = fs.readFileSync(OUTPUT_FILE, 'utf8');
  const sandbox = {};
  vm.runInNewContext(bundledCode, sandbox);

  if (!sandbox.hljs || typeof sandbox.hljs.highlight !== 'function') {
    throw new Error('Verification failed: sandbox.hljs does not export expected highlight() function');
  }

  const detectedVersion = sandbox.hljs.versionString;
  const languageCount = sandbox.hljs.listLanguages().length;
  const stats = fs.statSync(OUTPUT_FILE);

  console.log('');
  console.log('Bundle verification passed:');
  console.log(`  - Version: ${detectedVersion}`);
  console.log(`  - Languages: ${languageCount}`);
  console.log(`  - File size: ${(stats.size / 1024).toFixed(1)} KB (${stats.size} bytes)`);
  console.log(`  - Output: ${OUTPUT_FILE}`);
} finally {
  try {
    fs.rmSync(tempDir, { recursive: true, force: true });
  } catch (cleanupErr) {
    console.warn(`Warning: failed to clean up temporary directory ${tempDir}:`, cleanupErr.message);
  }
}
