# OpenSSF Best Practices badge — prepared answers

To register: sign in at https://www.bestpractices.dev with the GitHub account `Ataraxiainc2`, choose
"Add project", enter `https://github.com/Ataraxiainc2/ISSASPEAKER`, then use the answers below for the
**passing** level. Each answer points to evidence in this repository. "Met" means the evidence exists today.

## Basics

| Criterion | Answer | Evidence |
|---|---|---|
| description_good | Met | README.md, first paragraph |
| interact (how to get and contribute) | Met | README.md; issues and pull requests on GitHub |
| contribution | Met | CLAUDE.md (rules and quality gates); pull requests reviewed under CODEOWNERS |
| floss_license / license_location | Met — MIT | LICENSE |
| documentation_basics / documentation_interface | Met | docs/PLAN.md, docs/design/, docs/FEATURES.md |
| sites_https | Met | GitHub only |
| discussion | Met | GitHub issues |
| english | Met | All documentation in English |
| maintained | Met | Commit history |

## Change control

| Criterion | Answer | Evidence |
|---|---|---|
| repo_public / repo_track / repo_interim | Met | Public Git repository on GitHub |
| repo_distributed | Met | Git |
| version_unique / version_semver | Met | `versionName` in app/build.gradle.kts (0.1.0) |
| release_notes | Met once the first release is tagged | GitHub Releases (planned with the first Play release) |

## Reporting

| Criterion | Answer | Evidence |
|---|---|---|
| report_process / report_tracker | Met | GitHub issues |
| vulnerability_report_process / vulnerability_report_private | Met | SECURITY.md; GitHub private vulnerability reporting is enabled |
| vulnerability_report_response | Met | SECURITY.md commits to 72 h acknowledgement |

## Quality

| Criterion | Answer | Evidence |
|---|---|---|
| build / build_common_tools | Met | Gradle wrapper; `./gradlew assembleDebug` |
| test / test_invocation | Met | `./gradlew testDebugUnitTest` (JUnit) |
| test_most / test_continuous_integration | Met | .github/workflows/ci.yml on every push and pull request |
| test_policy / tests_are_added | Met | CLAUDE.md quality gates; tests added with each feature |
| warnings / warnings_fixed / warnings_strict | Met | Lint, detekt and Kotlin warnings are errors (app/build.gradle.kts, lint.xml, config/detekt.yml) |

## Security

| Criterion | Answer | Evidence |
|---|---|---|
| know_secure_design / know_common_errors | Met | docs/security/THREAT-MODEL.md (STRIDE), docs/security/AUDIT-LOG.md |
| crypto_published / crypto_floss | Met | Only standard algorithms through platform libraries (AES-GCM, Keystore, Tink) — docs/design/KEY-VAULT.md |
| crypto_keylength / crypto_working / crypto_weaknesses | Met | AES-256-GCM; no MD5 or SHA-1 for security (SHA-1 only where Google publishes nothing else, ISP-SEC-005) |
| crypto_pfs / crypto_password_storage / crypto_random | Met / N/A / Met | TLS by the platform; no passwords stored; SecureRandom |
| delivery_mitm / delivery_unsigned | Met | HTTPS only (network_security_config.xml); dependency checksums (gradle/verification-metadata.xml) |
| vulnerabilities_fixed_60_days / vulnerabilities_critical_fixed | Met | ISP-SEC-006: build-classpath advisories fixed the day they were found |
| no_leaked_credentials | Met | No secrets in the repository; secret scanning with push protection enabled |

## Analysis

| Criterion | Answer | Evidence |
|---|---|---|
| static_analysis / static_analysis_common_vulnerabilities | Met | CodeQL (security-extended), Android Lint, detekt |
| static_analysis_fixed / static_analysis_often | Met | Run on every push; findings fixed (ISP-SEC-007) |
| dynamic_analysis | Partial | Instrumented test on the real phone (GoogleFontsTest); fuzzing planned for parsers (Scorecard note in AUDIT-LOG.md) |
