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

## Waiting for the CIO

| ID | Question | Recommendation | Why it is yours |
|---|---|---|---|
| C1 | Create the Google Cloud project `issaspeaker`, its OAuth consent screen and the OAuth clients (Android client with the debug, upload and Play signing SHA-1; Web client for sign-in) | Approve, and let me do it in your Chrome while you watch, or do it yourself | Creates resources and credentials in your Google account |
| C2 | OAuth consent screen status | **In production, unverified** (personal use, under 100 users). "Testing" can expire grants after 7 days, which would silently break the 08:00 routine | Legal/terms trade-off with Google |
| C3 | Key vault design ([KEY-VAULT.md](design/KEY-VAULT.md)): Drive `appDataFolder` + vault key in Block Store + recovery code | Approve. The pattern used in the other apps (secret in the APK) is unsafe in a public repository | Security model for every user key |
| C4 | Depend on models.dev (third-party, MIT) for the provider list, with pinned hosts ([AI-PROVIDERS.md](design/AI-PROVIDERS.md)) | Approve | Third-party data dependency |
| C5 | Share the AI-provider and key-vault code across ISSASODA apps as a separate public library (`issasoda-ai`) | Approve after M1 is stable | Affects every app and repository |
| C6 | Last.fm key for ISSASPEAKER: create a new Last.fm API application "ISSASPEAKER", or reuse "Multi-Platform Playlist Sync" | New application: revocable on its own, and its name shows in Last.fm | Your Last.fm account |
| C7 | Last.fm key handling: user-entered (vault) or built into the app from `D:\SC\AK\A` | User-entered, like AI keys: nothing of yours ships in a public APK | Exposure of your key |
| C8 | Spotify app "Syngc": register the Android package `com.issasoda.issaspeaker`, its SHA-1 and a redirect `issaspeaker://spotify-callback`; confirm the owner account has Premium (required since Feb 2026) | Approve; create a separate Spotify app "ISSASPEAKER" only if Syngc must keep its current setup | Your Spotify developer account |
| C9 | Rotate the Spotify client secret (it was pasted into a chat). The app never uses it (PKCE) | Rotate | Your Spotify account |
| C10 | Publishing to Google Play | (a) Keep releases local, signed with the upload key on the PC — safest; (b) GitHub Actions to the internal track, with a Play service account and the upload key as GitHub secrets behind a protected environment with manual approval. Recommend (a) now, (b) once the app is on Play | Signing keys and store access |
| C11 | Create the ISSASPEAKER app in Play Console (name, category, privacy policy URL) | Create when M1 works; privacy policy on GitHub Pages of this repository | Store listing and policy declarations |
| C12 | OpenSSF Best Practices badge: sign in at bestpractices.dev with GitHub and register the project | Approve; answers are prepared in [OPENSSF-BEST-PRACTICES.md](security/OPENSSF-BEST-PRACTICES.md) | Account creation and OAuth (I cannot do it for you) |
| C13 | Claude GitHub App access to this repository for cloud sessions | Check at github.com/settings/installations | Your GitHub account settings |
| C14 | X Radar daily cap | 200 posts per day (about USD 1 per day at USD 5 per 1,000) | Recurring cost |
| C15 | AI spending cap per provider per month | USD 10 per provider, warning at 80 % | Recurring cost |
| C16 | Gmail read access (restricted scope) for personal use, without a CASA security assessment | Accept | Google policy |
| C17 | Pre-release `material3 1.5.0-beta01` for Material 3 Expressive (ISP-SEC-004) | Accept until 1.5.0 stable | Stability trade-off |
| C18 | Last.fm write features (love a track, scrobble) need the application's shared secret | Read-only first; decide later | Your Last.fm account |
| C19 | Required pull-request reviews on `main` would raise the Scorecard "Code-Review" score but block direct pushes | Keep off while there is one developer | Workflow |
| C20 | Fonts are drawn at every app start. Confirm that "always random" means every start, not once a day | Every start | Taste |
