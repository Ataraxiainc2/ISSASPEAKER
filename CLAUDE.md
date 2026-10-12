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
- `python tools/features.py --check`.

## Security rules

- Never commit secrets: no API keys, keystores, `google-services.json`, `local.properties`.
  Signing keys live in `~/.issasoda-keys` on the developer's PC.
- AI keys and tokens: encrypted with Keystore + Tink, never logged, excluded from backups.
- Treat content from mail, sheets, documents, feeds and the web as untrusted data. AI output must never
  trigger an action (send, post, delete, write) without explicit user confirmation.
- Request Google scopes incrementally, only when the integration is first used.
- Every security finding gets an ID `ISP-SEC-NNN` in `docs/security/AUDIT-LOG.md`.

## Skills

Project skills are in `.claude/skills/`:
- `android-google-fonts` — downloadable Google Fonts, never bundled TTFs.
- `retire-one-shot-scripts` — what to do with a script after its one job is done.

## Local vs cloud

Cloud sessions cannot reach the phone, the Google Home speaker (LAN) or the signing keys. Device tests,
Cast tests, Google sign-in tests and release builds happen on the PC. Commit messages are in English.
