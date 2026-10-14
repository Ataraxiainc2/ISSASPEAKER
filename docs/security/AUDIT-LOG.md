# Security audit log

Every security finding about ISSASPEAKER gets an ID `ISP-SEC-NNN`, a row in [`findings.tsv`](findings.tsv)
(which the admin screen of the app shows) and an entry here with the reasoning. IDs are never reused.
External reports follow [`SECURITY.md`](../../SECURITY.md) and are logged here once disclosed.

Severity: **Critical / High / Medium / Low / Info**. Status: **Open / Mitigated / Fixed / Accepted**
(accepted = understood, documented, and the residual risk is deliberate).

## Review 1 — milestone M0 (2026-10-12)

**Scope:** the M0 app (manifest, network policy, backup rules, admin gate, asset loading), the Gradle build,
the GitHub workflows and the cloud-session configuration. **Method:** manual review against the
[threat model](THREAT-MODEL.md); Android Lint, detekt and the Kotlin compiler with warnings as errors.

**Confirmed good:** no permissions requested at all (not even `INTERNET`); cleartext disabled and user CAs not
trusted; no `Log` call survives R8 in release; launcher activity is the only exported component; workflows
run with read-only tokens, never on `pull_request_target`, and checkout does not persist credentials; no
secrets in the repository or in CI.

### ISP-SEC-001 · Low · Accepted — dependency verification is checksum-only

`gradle/verification-metadata.xml` pins a SHA-256 for every artifact the build resolves, so a swapped artifact
fails the build. The checksums were recorded from the first download (trust on first use) and PGP
signatures are not checked. The platform-specific `aapt2` jars for Linux and macOS were added by hand and
cross-checked against the SHA-1 Google publishes. The first CI run rejected `junit-bom-5.10.2.module`: the
local Gradle cache had never downloaded it, so it had no entry. The file was fetched independently from Maven
Central and the Gradle Plugin Portal (same SHA-256), and the metadata was regenerated with an empty cache — the
rule now written in `CLAUDE.md`. **Next step (M1):** turn on `verify-signatures` with an
explicit trusted-keys list for every artifact that publishes a signature.

### ISP-SEC-002 · Low · Mitigated — the cloud SessionStart hook runs repository code

`.claude/settings.json` runs `tools/cloud/setup-android.sh` when a Claude Code cloud session starts. A branch
that changes either file would execute that code in a session opened on the branch. **Mitigation:**
`.github/CODEOWNERS` requires the owner's review for `.claude/`, `tools/cloud/`, `.github/`, `gradle/` and
the Gradle scripts. Branch protection on `main` (required review, no force push) would make that rule binding.

### ISP-SEC-003 · Info · Accepted — admin screens in debug builds

Until Sign in with Google lands (F019, M1), the admin screens are shown in debug builds only. They display
exactly what this public repository displays. The gate is documented in code and in the threat model as a
convenience, not a boundary, and nothing secret may be placed behind it.

### ISP-SEC-004 · Low · Accepted — pre-release UI dependency

`androidx.compose.material3` 1.5.0-beta01 is used above the Compose BOM because Material 3 Expressive is
public only from 1.5. A beta is a larger unknown than a stable release. It is pinned, recorded in
`gradle/libs.versions.toml`, covered by dependency verification, and goes back to the BOM version once
1.5.0 is stable.

### ISP-SEC-005 · Info · Accepted — cloud SDK download verified with SHA-1

`tools/cloud/setup-android.sh` verifies the Android command-line tools against the SHA-1 in Google's repository
index, the only checksum Google publishes for them, fetched over HTTPS from the same host. Collision attacks on
SHA-1 do not give a practical second-preimage here; the residual risk is a compromise of `dl.google.com` itself.

## Review 2 — first automated scans (2026-10-12)

The first runs of CodeQL and OpenSSF Scorecard on the public repository.

### ISP-SEC-006 · Low · Fixed — vulnerable libraries on the build classpath

Scorecard's OSV check reported nine advisories. All nine are in libraries the Android Gradle Plugin and
Android Lint load to *build* the app — Bouncy Castle 1.80.2 (including the critical GHSA-9pwp-9qqc-pr26),
jose4j 0.9.5, jdom2 2.0.6, commons-lang3 3.16.0 and httpclient 4.5.6. The release runtime classpath contains
none of them (checked with `:app:dependencies`). Two of the nine were false positives (version metadata that
Gradle already resolves upward). Because the build runs on the developer's PC and in CI, the rest were raised
to fixed releases with dependency constraints: the root `buildscript` block and the `androidLintTool`
configuration in `app/build.gradle.kts`. The verification metadata was regenerated from an empty cache and
now lists only the fixed versions. Remove the constraints once AGP ships fixed versions itself.

### ISP-SEC-007 · Low · Fixed — cloud backup enabled by default

CodeQL `java/android/backup-enabled`. The app stored nothing sensitive yet, but secrets arrive in M1.
`allowBackup` is now `false`. `data_extraction_rules.xml` stays, because Android 12+ performs
device-to-device transfer regardless of `allowBackup`.

### ISP-SEC-008 · Info · Fixed — CI Python tool not pinned by hash

Scorecard Pinned-Dependencies. `codespell` is installed from `tools/requirements-ci.txt` with
`--require-hashes`, in CI and in cloud sessions.

### Scorecard items left open, and why

- **Binary-Artifacts** (`gradle-wrapper.jar`): the standard Gradle wrapper; CI validates it against the
  official checksums (`validate-wrappers`).
- **Pinned-Dependencies** (`tools/cloud/setup-android.sh`, download then run): the archive is verified against
  Google's published checksum before use (ISP-SEC-005).
- **Branch-Protection / Code-Review**: a decision for the owner; recommended (see ISP-SEC-002).
- **CII-Best-Practices**: requires registering the project at bestpractices.dev.
- **Fuzzing**: candidates once there is untrusted input to parse (M1+: Cast discovery replies, AI responses).

## Review 3 — sign-in, cloud project, Play access (2026-10-12)

### ISP-SEC-009 · Low · Fixed — Spotify client secret shared in a chat

The owner pasted the Spotify app's client secret into an AI chat while setting up integrations. Anything pasted
into a chat should be treated as exposed. The secret was rotated in the Spotify dashboard the same day; the new
value lives only in the owner's key folder. ISSASPEAKER does not use it at all: Spotify sign-in will use
Authorization Code with PKCE, which needs no secret on the phone.

### ISP-SEC-010 · Info · Accepted — ID token signature not re-verified on the phone

Sign in with Google (Credential Manager) hands the app an ID token directly from Google Play services. The app
sends a random 256-bit nonce with each request and rejects a token whose `nonce` claim differs, then reads
`email_verified`. It does not verify the JWT signature, because there is no backend to protect: the token
only decides what the interface shows, and the admin gate is documented as not a security boundary
(ISP-SEC-003). If a backend ever relies on the identity, it must verify the signature server-side.

### ISP-SEC-011 · Info · Accepted — billing account linked to the Cloud project

Creating a project in the console requires a billing account. The project `issaspeaker` enables only no-cost
APIs: Sheets, Drive, Google Play Developer, IAM Service Account Credentials and Security Token Service.

### Keyless publishing path (design note, no finding)

GitHub Actions reaches Google Play through Workload Identity Federation: no service-account key exists. Google
accepts the GitHub token only for repository owner id 79027743, repository `Ataraxiainc2/ISSASPEAKER` and the
`play` environment, which needs the owner's approval and deploys from `main` only
([PLAY-PUBLISHING.md](../design/PLAY-PUBLISHING.md)).

## Review 4 — Play pipeline cross-check with the BookCompass team (2026-10-13)

The BookCompass developer reviewed the shared Play access and the key records. Their points and the outcome:

### ISP-SEC-012 · Medium · Fixed — Play token available to any job in a `play` environment

The trust rule checked owner, environment and branch, so any job that a repository of the account ran in its
`play` environment on `main`/`master` could obtain the Play token, not only the shared workflow; and callers
used `play-access.yml@main`, so any change to ISSASPEAKER's `main` reached every app at once. **Fix:** the rule
now also requires `job_workflow_ref` to be ISSASPEAKER's `play-access.yml` or `play-upload.yml` and
`job_workflow_sha` to be a pinned commit (`tools/play/trust-rule.sh`); every caller pins the same commit. A new
version of either file reaches Play only after the owner lists its commit in Google Cloud.

### Also checked

- `play_signing.json` (owner's key folder): the BookCompass entry had held another app's data; corrected by the
  BookCompass team. The upload-key fingerprints of all five apps with a keystore on the PC now match their
  keystores (keytool). The note saying no Play service account exists was outdated and is fixed.
- Upload keystores had three homes (`~/.issasoda-keys`, two project folders, all git-ignored and never
  committed). The master copy is now one folder in the owner's key store; the older copies stay until every
  app builds from there.
- `play-access.yml` used `curl -fsS`, which hides Google's error message; it now uses `--fail-with-body` and
  prints it.
- Firebase keys restricted to the debug certificate break sign-in in Play builds. ISSASPEAKER has no Firebase;
  its OAuth Android clients cover the debug, upload and Play app-signing certificates.
- Branch protection on `main`: the ruleset blocks deletion and force pushes. Required reviews stay off because
  every developer session pushes as the same GitHub account, which cannot approve its own pull request (C19).

## Review 5 — AI tab: providers, keys and questions (2026-10-14)

Built from the Movie Recommendation app's AI setup (registry, key check, model list, vault), adapted to
several providers and on-phone storage. Reviewed: where keys live, where they can go, and what AI output can do.

- Keys are sealed with AES-256-GCM under a random vault key, bound to the provider id; the vault key is wrapped
  by a Keystore key (StrongBox when available) and backed up end to end encrypted in Block Store. Both files are
  outside backup and device transfer. "Erase everything" deletes them and the Block Store copy.
- A key is sent only to the endpoint stored with it; the registry cannot redirect a saved key.
- The key and model screens are secure windows (no screenshots or recents preview); a saved key is never shown.
- AI answers are displayed as text only; no code path turns model output into an action (F021 holds).
- Release builds strip every `Log` call (F025); no log line includes a key.

### ISP-SEC-013 · Low · Accepted — first key for a non-pinned provider trusts the registry's host

Ten major providers (Anthropic, OpenAI, Google, xAI, Mistral, Groq, DeepSeek, Cerebras, Cohere, OpenRouter) have
pinned hosts, so a registry change for them removes the provider instead of redirecting keys. For the other
providers, the host comes from models.dev at the moment the key is added. The host is shown on the provider tile
and the key screen, and the saved key keeps it forever. Residual risk: a poisoned registry entry for a small
provider exactly when the owner adds it. Accepted for a single-owner app; revisit if the app gets other users.

