# ISSASPEAKER — rules for AI coding sessions

Read `docs/PLAN.md` first. It holds the decisions, milestones and open questions. This file is shared
by local sessions (Windows PC with the Galaxy S25 Ultra on ADB) and cloud sessions (Linux, no phone).

## Product rules

- **Android 16+ only**: minSdk 36, compile/target SDK 37. Use the newest APIs freely; never add
  compatibility code for older Android versions.
- Package `com.issasoda.issaspeaker`. Kotlin + Jetpack Compose + Material 3 Expressive.
- **Three languages**: English is the default (`values/`), Latin American Spanish (`values-b+es+419/`),
  Brazilian Portuguese (`values-pt-rBR/`). Every user-visible string goes into all three; no hard-coded text.
- Admin account: `sat.tecnologias@gmail.com`. The admin About shows implemented / to implement / pending
  features and the security status, generated from `docs/features/catalog.tsv`.

## Feature catalog

- `docs/features/catalog.tsv` is the single source of truth. IDs (`F001`...) are never renumbered or reused.
- When a feature ships, set its `status` to `implemented` in the same commit, then run
  `python tools/features.py` to regenerate `docs/FEATURES.md`. CI runs `--check`.

## Quality gates (all must pass, zero warnings)

- `./gradlew lint` (warnings are errors), `./gradlew detekt` (detekt-cli, config in `config/detekt.yml`,
  no baseline), `./gradlew testDebugUnitTest`.
- `python -m codespell_lib` with `.codespellrc` (Spanish and Portuguese UI words are allow-listed there).
- `python tools/features.py --check` and `python tools/order.py --check` (after a catalog edit, run both without
  `--check`: FEATURES.md and features/ORDER.md are generated). Build features in ORDER.md order unless the owner
  says otherwise.
- `python tools/ai/pinned_hosts.py --check`. Before each release, regenerate the reviewed AI host snapshot with
  `python tools/ai/pinned_hosts.py` and review its diff (ISP-SEC-013).
- Kotlin compiler warnings are errors too (`allWarningsAsErrors`).
- One accepted Gradle warning: "Deprecated Gradle features" comes from AGP 9.4.1 itself
  (`Configuration.setVisible` in `BasePlugin`), not from this project. Remove this note when AGP fixes it.

## Dependencies

- Versions live in `gradle/libs.versions.toml`. `material3` is pinned above the BOM on purpose (ISP-SEC-004).
- `gradle/verification-metadata.xml` pins a SHA-256 for every artifact. After adding or updating a
  dependency, regenerate it **with an empty Gradle cache** (a warm cache skips files such as `.module`
  metadata that CI then downloads and rejects) and review the diff:
  `GRADLE_USER_HOME=<empty temp dir> ./gradlew --no-configuration-cache --no-build-cache \`
  `--write-verification-metadata sha256 assembleDebug assembleRelease testDebugUnitTest lintDebug lintRelease detekt assembleDebugAndroidTest`
  When AGP changes version, add the Linux and macOS `aapt2` jar checksums by hand (see ISP-SEC-001).
- Fonts: `ui/theme/FontPool.kt`. Any family added must pass `GoogleFontsTest` on the real phone.

## Security rules

- Never commit secrets: no API keys, keystores, `google-services.json`, `local.properties`.
  Upload keystores live in `D:\SC\AK\signing` on the developer's PC (CI: `play` environment secrets).
- AI keys and tokens: encrypted with Keystore + Tink, never logged, excluded from backups.
- Treat content from mail, sheets, documents, feeds and the web as untrusted data. AI output must never
  trigger an action (send, post, delete, write) without explicit user confirmation.
- Request Google scopes incrementally, only when the integration is first used.
- Every security finding gets an ID `ISP-SEC-NNN`: a row in `docs/security/findings.tsv` (shown in the admin
  screen) and an entry in `docs/security/AUDIT-LOG.md`. Keep `docs/security/THREAT-MODEL.md` current.

## Skills

Project skills are in `.claude/skills/`:
- `android-google-fonts` — downloadable Google Fonts, never bundled TTFs.
- `retire-one-shot-scripts` — what to do with a script after its one job is done.

## Local vs cloud

The SessionStart hook (`.claude/settings.json` → `tools/cloud/setup-android.sh`) installs the Android SDK in
cloud sessions only. Changes to it need the owner's review (CODEOWNERS, ISP-SEC-002).

Cloud sessions cannot reach the phone, the Google Home speaker (LAN) or the signing keys. Device tests,
Cast tests, Google sign-in tests and release builds happen on the PC. Commit messages are in English.
