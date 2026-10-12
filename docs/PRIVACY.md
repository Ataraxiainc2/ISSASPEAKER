---
title: ISSASPEAKER privacy policy
---

# ISSASPEAKER privacy policy

**Effective:** 2026-10-12 · **Developer:** ISSASODA · **Contact:** sat.tecnologias@gmail.com

ISSASPEAKER is a personal assistant app for Android. This policy explains what data it handles, where that data
goes and how to delete it. The app's source code is public at
[github.com/Ataraxiainc2/ISSASPEAKER](https://github.com/Ataraxiainc2/ISSASPEAKER), so every statement here can
be checked.

## The short version

- **ISSASODA runs no server for this app.** We never receive, store or sell your data.
- Your data stays **on your phone** and, where you allow it, **in your own Google account**.
- Data leaves your phone only to the services **you connect and choose** (for example Google, an AI provider,
  Spotify or Last.fm), and only to do what you asked.
- No advertising, no analytics, no tracking.

## What the app handles

| Data | Why | Where it is kept |
|---|---|---|
| Google account name, email and photo | To sign you in and show your account | On your phone |
| Google data you authorize (for example a Google Sheet, your calendar, Drive files, Gmail) | To read birthdays, reminders and summaries you ask for | Read from Google when needed; results kept on your phone |
| API keys you enter (AI providers, Last.fm) | To call those services for you | Encrypted on your phone, and an encrypted copy in a hidden app folder of **your own** Google Drive so you can restore them |
| Conversation history with the AI, routine history | To show you your history | Encrypted on your phone |
| Music listening data (Spotify, Last.fm) | To show your statistics and control playback | On your phone |
| Settings (language, theme) | To remember your choices | On your phone |

Each Google permission is requested only when you first use the feature that needs it. You can review and
revoke permissions in the app and at [myaccount.google.com/permissions](https://myaccount.google.com/permissions).

## Who receives data

- **Google**, for sign-in and the Google services you authorize.
- **The AI provider you choose** (for example Anthropic, Google, OpenAI or xAI) receives the text of the requests
  you make, under that provider's own terms. You choose the provider and use your own key.
- **xAI**, if you enable X Radar, receives the search requests built from your interest profile.
- **Spotify and Last.fm**, if you connect them.
- **Your Google Home speaker**, on your home network, receives the audio of announcements.

ISSASODA receives nothing.

## Google user data

ISSASPEAKER's use of information received from Google APIs adheres to the
[Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy),
including the Limited Use requirements. Google data is used only to provide the features you request, is not
transferred to others except as described above to provide those features, is not used for advertising, and is
not read by humans.

## Security

Keys and personal data are encrypted on the phone with the Android Keystore. The copy of your keys in Google
Drive is encrypted before it leaves the phone, with a key that is not stored in Drive or in the app. All
network traffic uses HTTPS. Details: [threat model](security/THREAT-MODEL.md).

## Deleting your data

- In the app: **Settings → Erase everything** removes keys, history and settings, deletes the Drive copy and
  signs you out.
- Uninstalling the app removes everything stored on the phone.
- You can revoke the app's Google access at any time at
  [myaccount.google.com/permissions](https://myaccount.google.com/permissions).

## Children

The app is not directed at children under 13.

## Changes

Changes to this policy are published on this page with a new effective date. The history is visible in the
public repository.

## Contact

sat.tecnologias@gmail.com
