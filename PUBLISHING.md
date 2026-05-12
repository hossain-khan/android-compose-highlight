# Publishing Guide

This library is distributed through **Maven Central** with coordinates `dev.hossain:compose-highlight:<version>`.

---

## Prerequisites

Complete these steps once before your first Maven Central release.

### 1. Sonatype Central Portal account

1. Sign up at [central.sonatype.com](https://central.sonatype.com)
2. Verify the `dev.hossain` namespace (Settings → Namespaces)
3. Generate a user token: Account → Generate User Token
   - Save the **token username** → `OSSRH_USERNAME` secret
   - Save the **token password** → `OSSRH_PASSWORD` secret

### 2. GPG signing key

Generate a key (if you don't have one):
```bash
gpg --gen-key
# Choose Ed25519, set name/email, set a passphrase
```

Upload the public key to keyservers:
```bash
gpg --list-keys               # find your FULL_FINGERPRINT (40 hex chars)
gpg --keyserver keys.openpgp.org --send-keys <FULL_FINGERPRINT>
gpg --keyserver pgp.mit.edu   --send-keys <FULL_FINGERPRINT>
```

Export the **ASCII-armored** private key for CI:
```bash
gpg --export-secret-keys --armor <FULL_FINGERPRINT>
```

> ⚠️ Copy the entire output — including the `-----BEGIN PGP PRIVATE KEY BLOCK-----`
> and `-----END PGP PRIVATE KEY BLOCK-----` lines — directly into the `SIGNING_KEY`
> GitHub Secret. **Do not base64-encode it.**

### 3. GitHub Secrets

Add the following secrets in **Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `SIGNING_KEY_ID` | Short key ID — last 8 hex chars of the fingerprint (e.g. `F17804D7`) |
| `SIGNING_KEY` | ASCII-armored private key (full `-----BEGIN/END-----` block) |
| `SIGNING_PASSWORD` | Passphrase used when generating the key |
| `OSSRH_USERNAME` | Central Portal token username |
| `OSSRH_PASSWORD` | Central Portal token password |

The workflow maps these to `ORG_GRADLE_PROJECT_*` environment variables that the vanniktech plugin reads automatically.

---

## Releasing a new version

### Step 1 — Bump the version

Update `VERSION_NAME` in [`gradle.properties`](gradle.properties):
```properties
VERSION_NAME=0.12.0
```

Also update these per the [project conventions](README.md#releasing):
- `CHANGELOG.md` — rename `[Unreleased]` to `[0.12.0] - YYYY-MM-DD`
- `sample/build.gradle.kts` — `versionName` and `versionCode`
- `README.md` — dependency snippet version

Commit, push, and create the git tag:
```bash
git tag 0.12.0
git push origin 0.12.0
```

### Step 2 — Dry run (recommended)

Run the workflow in dry-run mode first to validate all artifacts are produced
and signed correctly **without uploading anything** to Maven Central:

1. Go to **Actions → Publish to Maven Central**
2. Click **Run workflow**
3. Enter the tag (e.g. `0.12.0`)
4. ✅ Check **Dry run**
5. Click **Run workflow**

Expected output from the validation step:
```
✅  compose-highlight-0.12.0.aar
✅  compose-highlight-0.12.0.aar.asc
✅  compose-highlight-0.12.0.pom
✅  compose-highlight-0.12.0.pom.asc
✅  compose-highlight-0.12.0-sources.jar
✅  compose-highlight-0.12.0-sources.jar.asc
✅  compose-highlight-0.12.0-javadoc.jar
✅  compose-highlight-0.12.0-javadoc.jar.asc
── Result: 8 passed, 0 failed
```

### Step 3 — Publish

Once the dry run is green:

1. Go to **Actions → Publish to Maven Central**
2. Click **Run workflow**
3. Enter the tag (e.g. `0.12.0`)
4. Leave **Dry run** unchecked
5. Click **Run workflow**

The workflow will:
1. Verify the git tag exists
2. Confirm the version isn't already on Maven Central
3. Check out that exact tag
4. Build, sign, and upload all artifacts
5. Close and release the staging repository automatically

The release typically appears on Maven Central within **10–30 minutes**.

### Step 4 — Verify

Check the release is live:
```
https://repo1.maven.org/maven2/dev/hossain/compose-highlight/<version>/
```

Or search on [central.sonatype.com](https://central.sonatype.com/artifact/dev.hossain/compose-highlight).

---

## Local dry run (without CI)

You can also validate signing and artifact generation locally:

```bash
export ORG_GRADLE_PROJECT_signingInMemoryKeyId=<YOUR_KEY_ID>
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --export-secret-keys --armor <FULL_FINGERPRINT>)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=<your-passphrase>

./gradlew :compose-highlight:publishToMavenLocal -PVERSION_NAME=0.12.0
```

Inspect the output:
```bash
ls -lh ~/.m2/repository/dev/hossain/compose-highlight/0.12.0/
```

---

## Gradle publish tasks reference

| Task | Description |
|---|---|
| `publishToMavenLocal` | Publishes to `~/.m2` — no upload, useful for local testing |
| `publishAllPublicationsToMavenCentralRepository` | Uploads and releases to Maven Central (used in CI) |
| `releaseSonatypeStagingRepository` | Releases (publishes) a closed staging repo |
