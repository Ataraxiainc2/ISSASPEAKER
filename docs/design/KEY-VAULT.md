# Key vault: user keys in the user's own Google Drive

**Status:** approved (C3, D14). Phase 1 implemented on 2026-10-14: vault key wrapped by the Keystore and backed up
in Block Store, AI keys sealed on the phone (`ai/KeyVault.kt`, `ai/AiKeyStore.kt`). Phase 2, still to do: the
sealed keys synced to Drive appDataFolder (F423) and the recovery code (F424) · catalog F013, F017, F423, F424

## Requirement

AI keys (and other user-owned keys such as Last.fm) live in the user's Google account, so they survive a
reinstall or a new phone. ISSASTUDENT, EmuGaMind and BookCompass already store them in the Drive
`appDataFolder`.

## What the existing apps do, and why ISSASPEAKER needs more

BookCompass and ISSASTUDENT encrypt each key with AES-256-GCM under a key derived (HKDF) from **an app secret
embedded in the APK** plus the user id. Their own comments are honest about it: "friction, not a vault".
Anyone with the Drive file and a decompiled APK can decrypt it.

ISSASPEAKER is **open source**: the "app secret" would be published on GitHub, so it would protect nothing.
The encryption key must not be derivable from the app.

## Design

Three layers, each with one job:

| Layer | Holds | Protected by |
|---|---|---|
| Phone (at rest) | The decrypted keys, in the encrypted database | Android Keystore, StrongBox on the S25 (F013, F014) |
| Google Drive `appDataFolder` | `vault.json`: every key encrypted with the **vault key** | AES-256-GCM; Drive only ever sees ciphertext |
| Block Store (Google Play services) | The 32-byte **vault key** | End-to-end encryption with the phone's screen lock; Google cannot read it |

- **Vault key:** 32 random bytes, generated once on the first phone. It never appears in the APK, the
  repository, logs or Drive.
- **Drive file:** AES-256-GCM per entry, with the account id and provider id as associated data, so a
  ciphertext cannot be moved to another account or provider. Same `appDataFolder` and scope
  (`drive.appdata`) as the other apps, requested only when the user first saves a key (incremental
  authorization).
- **Block Store:** stores the vault key with cloud backup on. Google encrypts it end to end with the screen
  lock (`isEndToEndEncryptionAvailable()`); it comes back on a reinstall and on a new phone restored from the
  Google backup.
- **Recovery code** (F424): shown once at setup (24 words or a base32 string), for a new phone set up
  *without* restoring the Google backup. Typing it on the new phone restores the vault key.

## Flows

- **First key:** generate the vault key → Block Store → encrypt the key → write `vault.json` → keep the plain
  key only in the Keystore-protected database → show the recovery code.
- **Reinstall or restored phone:** sign in → read `vault.json` → vault key from Block Store → decrypt → done,
  without typing any key.
- **New phone without backup restore:** sign in → `vault.json` found, Block Store empty → ask for the recovery
  code → done.
- **Erase everything** (F020): delete `vault.json`, clear Block Store, wipe the database, revoke the Drive
  token.

## What this protects against

| Threat | Result |
|---|---|
| Drive file leaked or read by a Google Takeout export | Ciphertext only; useless without the vault key |
| Decompiled APK (the code is public anyway) | Contains no secret |
| Lost phone with a screen lock | Keystore keys are locked; Block Store is end-to-end encrypted |
| Another app on the phone | Sandbox, plus Keystore |

**Residual risk, stated plainly:** someone who knows the screen lock *and* controls the Google account can
restore the vault on their own phone. That is the same trust boundary as the user's passwords in Google
Password Manager.

## Alternatives considered

- **App secret in the APK** (current pattern): rejected for a public repository.
- **User passphrase:** strong, but one more thing to remember; kept as the recovery-code fallback instead.
- **Passkey with the PRF extension:** elegant and synced, but it needs a website domain with Digital Asset
  Links; worth revisiting if ISSASODA gets a domain.

## Migration for the other apps

The same vault can replace the embedded-secret scheme in ISSASTUDENT, EmuGaMind and BookCompass. Each app has
its own `appDataFolder` (separate Google Cloud projects), so keys are not shared between apps unless that is
designed explicitly (decision C5).
