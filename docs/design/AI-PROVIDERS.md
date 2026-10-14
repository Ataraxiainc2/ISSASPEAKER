# AI providers: a live registry instead of a fixed list

**Status:** implemented on 2026-10-14 (registry, key check, live model list, test call, several providers,
Ask AI); supplement (F417), native Bedrock (F420) and model ranking (F421) still to do · catalog F056, F416–F422

## As built (2026-10-14)

The code follows the Movie Recommendation app, whose AI setup the owner rated as working very well:

1. **AI tab → Add a provider:** the live list, labs that the most other providers serve first (the registry's
   `canonical_model_id` links, never a hand-made ranking), 12 shown until "Show all", search over all of them.
   Each tile shows the logo (models.dev), the name, the **host the key would go to** and the model count.
2. **Key:** pasted into a hidden field on a secure window (no screenshots). Checked by calling the provider's
   `/models` with it: a wrong key is rejected with the provider's own message.
3. **Model:** the provider's own list for that key, enriched with registry details (family chips, release date,
   context, "New" badge). Choosing one sends a one-word test prompt; only an answer saves it.
4. **Saved:** the key is sealed with the vault key and stored with the endpoint it was checked against
   (`ai/AiKeyStore.kt`). The first provider becomes the default; any other can be made the default, have its
   model changed (the list is read live with the stored key) or be removed.
5. **Ask AI:** a text conversation with the default provider, kept in memory only and sent with each question
   (last 20 turns).

Differences from the Movie Recommendation app: several providers instead of one, keys stored on the phone (no
Firebase in this app) instead of Firestore, and pinned hosts: a reviewed snapshot of every provider's host ships in
the APK (`tools/ai/pinned_hosts.py`); a changed host hides the provider and a provider newer than the snapshot
needs the owner's confirmation (ISP-SEC-013). Both apps share the same
registry, client and vault code by copy until decision C5 (a shared library) is taken.


## Goal

The app does not ship a hand-picked list of AI providers. It shows every provider that exists, as reported by
a source that is updated continuously, lets the user enter a key for any of them, and then lists the models
that key can actually use. The same component should serve every ISSASODA app (decision C5).

## Source of truth: models.dev

[models.dev](https://models.dev) is an open-source (MIT) database of AI providers and models
(`https://models.dev/api.json`), maintained on GitHub (`anomalyco/models.dev`) and used by opencode.

Measured on 2026-10-12: **227 providers, 8,568 models, 30 commits that day**. Per provider it gives an id,
name, documentation URL, the environment-variable names of its key, the SDK family (`npm`) and, for 202 of
them, an OpenAI-compatible base URL (`api`). Per model: context and output limits, price per million tokens,
modalities, tool calling, reasoning, release date.

21 of the 26 providers in the owner's key file are listed. SambaNova, Reka, Yandex, Baidu and Inflection
are not, so a small **bundled supplement** (same schema) fills the gap (F417).

## How the app uses it

1. **Daily refresh** (WorkManager, unmetered network): download `api.json`, validate it against the expected
   schema, keep it in the encrypted database. Without network, the last good copy is used; a bundled snapshot
   covers the first launch.
2. **Provider list** = registry + supplement, searchable. Providers with a key are shown first.
3. **Add a key**: the user picks a provider and pastes the key (stored by the key vault,
   [KEY-VAULT.md](KEY-VAULT.md)).
4. **Models for that key**: the app calls the provider's own model-list endpoint with the user's key
   (`GET /models` for OpenAI-compatible APIs; native endpoints for Anthropic, Gemini, Cohere, Bedrock). That
   list is authoritative for what the key can use; registry data adds context size, price and modalities.
5. **Per-task model choice** (F053): chat, summaries, X Radar briefs and announcements can each use a
   different provider and model.

## Clients

- **One OpenAI-compatible client** covers about 190 registry providers (`npm` = `@ai-sdk/openai-compatible`
  plus OpenAI itself), using the registry's base URL.
- **Native clients** for the APIs that differ: Anthropic Messages, Google Gemini, Cohere, Amazon Bedrock
  (SigV4 signing). xAI and Groq are OpenAI-compatible.
- Direct REST over OkHttp and kotlinx.serialization; no vendor SDKs.

## Security: the registry is untrusted input

A registry that tells the app *where to send the user's API key* is a target. If someone got a malicious
change merged into models.dev, a provider's base URL could point to their server and harvest keys.

Controls (F418, threat model §5.3a):

- **Pinned hosts.** The app ships a list of known hosts for the major providers (for example
  `api.anthropic.com`, `generativelanguage.googleapis.com`, `api.openai.com`, `api.x.ai`). A key for a
  pinned provider is never sent anywhere else, whatever the registry says.
- **Trust on first use, with confirmation.** For other providers the host seen when the key was added is
  remembered. If the registry later reports a different host, the app stops using that key and asks the user,
  showing the old and new host.
- **HTTPS only**, schema validation, size limits on the download, and no registry field is ever executed or
  rendered as markup.
- The registry decides *what is listed*, never *what is trusted*.

## Reuse across ISSASODA apps (decision C5)

The registry client, the supplement, the provider clients and the key vault form one module with no
ISSASPEAKER-specific code. Options: a separate public repository published as a library, or a module copied
into each app. Recommendation: a separate repository (`issasoda-ai`) once M1 is stable.

## Optional: model quality

The owner has an Artificial Analysis key. Their benchmark data could rank models per task (F421, M7).
