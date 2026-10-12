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
