# Decisions

Two lists: decisions already taken (D), and decisions that need the CIO's approval (C) because they involve
accounts, money, publishing, legal terms or trade-offs only the owner can make. Each C item has a
recommendation; the CIO approves, changes or rejects it.

## Taken

| ID | Decision | Date |
|---|---|---|
| D1 | MIT license | 2026-10-12 |
| D2 | High-legibility Google Fonts from a pool of at least 100 (133 today) | 2026-10-12 |
| D3 | X: no paid X API and no scraping. X Radar uses the xAI `x_search` tool, plus sharing from the X app and notifications | 2026-10-12 |
| D4 | Shikaka.net: ShikakaStats has no public API; the same deep statistics are computed from the full Last.fm history | 2026-10-12 |
| D5 | AI providers: no fixed list. Live registry (models.dev) plus a supplement; every provider the owner has a key for, and any other the user adds | 2026-10-12 |
| D6 | New Google Cloud project `issaspeaker`, no Firebase | 2026-10-12 |
| D7 | Galaxy Watch OS version: check on the watch before M5 | open |
| D8 | Conversation in English; code documentation in English | 2026-10-12 |
| D9 | Fonts always random, never pinned; a different family per text role (titles, headings, text, labels), drawn at every start; Settings lists them | 2026-10-12 |
| D10 | Theme: light, dark or phone setting | 2026-10-12 |
| D11 | Branch ruleset on `main`: no force push, no deletion. Direct pushes still allowed | 2026-10-12 |
| D12 | C1 done: Google Cloud project `issaspeaker` (OAuth Android clients for the debug and upload keys, Web client for sign-in; Sheets, Drive, Play Developer, IAM Credentials, STS enabled) | 2026-10-12 |
| D13 | C2 done: OAuth consent "In production", External, unverified; privacy policy and home page on GitHub Pages | 2026-10-12 |
| D14 | C3 and C4 approved: key vault (Drive appDataFolder + Block Store + recovery code) and the models.dev registry with pinned hosts; implementation in M1 | 2026-10-12 |
| D15 | C6: Last.fm keys stored as one line `lastfm:key1;key2;...` (all 25 are interchangeable) | 2026-10-12 |
| D16 | C8 done: Spotify app "Syngc" has the ISSASPEAKER package with debug and upload SHA-1 and redirect `issaspeaker://spotify-callback`; owner account is Premium (Student) — a payment error is pending on Spotify's side | 2026-10-12 |
| D17 | C9 done: Spotify client secret rotated (ISP-SEC-009) | 2026-10-12 |
| D18 | C20: fonts change every time the app opens or comes back from another app, never on rotation | 2026-10-12 |
| D19 | C10 (urgent, owner): GitHub Actions gets keyless access to Google Play (Workload Identity Federation, environment `play`) | 2026-10-12 |
| D20 | C21 done: `play-publisher@issaspeaker.iam.gserviceaccount.com` is an account-level Play Console user for all apps, current and future, without Admin, production releases or order management | 2026-10-13 |
| D21 | Play access for every Ataraxiainc2 repository, current and future: Google trusts owner id 79027743 + environment `play` + branch `main`; environments created in EmuGaMind, ISSAGRAPPH, ISSASTUDENT, BookCompass | 2026-10-13 |
| D22 | C22 done: first Sign in with Google on the S25 accepted by the owner | 2026-10-13 |
| D23 | C11 done: ISSASPEAKER created in Play Console (Productivity, privacy policy on GitHub Pages); 0.1.0 on internal testing; store listing published by the owner. Data safety says "no data collected" and must be updated when M1 ships | 2026-10-13 |
| D24 | Upload keystores of every app have one master copy in the owner's key folder (`D:\SC\AK\signing`); in CI they are `play` environment secrets, never repository secrets | 2026-10-13 |
| D25 | Releases from GitHub: manual workflow, testing tracks only, dry run by default; production stays manual in Play Console; nothing goes to Play without the CIO's approval | 2026-10-13 |
| D26 | Google trusts only ISSASPEAKER's `play-access.yml` / `play-upload.yml` at pinned commits (ISP-SEC-012); callers pin the same commit | 2026-10-13 |

## Waiting for the CIO

| ID | Question | Recommendation | Why it is yours |
|---|---|---|---|
| C5 | Share the AI-provider and key-vault code across ISSASODA apps as a separate public library (`issasoda-ai`) | Approve after M1 is stable | Affects every app and repository |
| C7 | Last.fm key handling: user-entered (vault) or built into the app from `D:\SC\AK\A` | User-entered, like AI keys: nothing of yours ships in a public APK | Exposure of your key |
| C12 | OpenSSF Best Practices badge: sign in at bestpractices.dev with GitHub and register the project | Approve; answers are prepared in [OPENSSF-BEST-PRACTICES.md](security/OPENSSF-BEST-PRACTICES.md) | Account creation and OAuth (I cannot do it for you) |
| C14 | X Radar daily cap | 200 posts per day (about USD 1 per day at USD 5 per 1,000) | Recurring cost |
| C15 | AI spending cap per provider per month | USD 10 per provider, warning at 80 % | Recurring cost |
| C16 | Gmail read access (restricted scope) for personal use, without a CASA security assessment | Accept | Google policy |
| C17 | Pre-release `material3 1.5.0-beta01` for Material 3 Expressive (ISP-SEC-004) | Accept until 1.5.0 stable | Stability trade-off |
| C18 | Last.fm write features (love a track, scrobble) need the application's shared secret | Read-only first; decide later | Your Last.fm account |
| C19 | Required pull-request reviews on `main` would raise the Scorecard "Code-Review" score but block direct pushes | Keep off while there is one developer | Workflow |
| C23 | GitHub sudo confirmation to review the Claude GitHub App, and its pending "permission update request" | Review it yourself at github.com/settings/installations | Your GitHub account |
| C24 | Fix the Spotify payment error so Premium (and developer mode) does not lapse | Update the payment method | Payment |
