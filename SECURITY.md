# Security Policy

ISSASPEAKER is maintained by ISSASODA. We take reports seriously and we answer every one.

## Reporting a vulnerability

**Do not open a public issue for a security problem.**

Use either of these:

- **GitHub private vulnerability reporting** — the *Report a vulnerability* button under the
  [Security tab](../../security/advisories/new). This is the preferred route; it keeps the report
  private until a fix ships and produces a public advisory afterwards.
- **Email** — sat.tecnologias@gmail.com with `[SECURITY] ISSASPEAKER` in the subject.

### What to include

- the affected component — Android app, Wear OS app, local Cast audio server, AI key handling,
  Google account handling, or the build and release pipeline;
- the app version or commit hash;
- the device and Android version, if the issue is device-specific;
- steps to reproduce, and a crash log or stack trace if there is one;
- what an attacker gains, in one sentence.

A proof of concept helps, but a clear description of the defect is enough. Please do not include
other people's data in a report.

## What we commit to

| Stage | Target |
|---|---|
| We acknowledge the report | 72 hours |
| We tell you our assessment and a fix plan | 10 days |
| Fix released for a critical issue | 30 days |
| Fix released for everything else | 90 days |

If we miss one of these, we will say so and why rather than go quiet.

## Disclosure

We publish a GitHub Security Advisory for every confirmed vulnerability once a fix is available, and
we credit the reporter by the name or handle they choose. Tell us if you would rather stay anonymous.
If you plan to publish your own write-up, we ask for coordinated timing, and we will not ask you to
delay past 90 days.

## Scope

ISSASPEAKER holds a user's AI provider keys and access to their Google account, and it talks to devices
on their home network. Anything that moves one of those outside the phone matters most.

**In scope**

- This repository: the Android and Wear OS applications and their data layer.
- Storage of AI provider keys and Google tokens at rest, and anything that could expose them
  (backups, logs, screenshots, other apps, the network).
- The local audio server used to send speech to a Cast device: anything that lets another device on
  the network read, replace or inject audio, or reach other files.
- Prompt injection: content from mail, sheets, documents, feeds or the web that makes the app take an
  action the user did not confirm, or leak data to a third party.
- OAuth handling: scopes broader than needed, tokens that survive revocation or sign-out.
- Secrets or credentials exposed in the repository, a release artifact or the CI pipeline.

**Out of scope**

- Vulnerabilities in Google, Spotify, Last.fm, X or AI provider services themselves — report those to
  the vendor.
- Attacks that need a rooted or already compromised device, or physical access to an unlocked phone.
- Missing hardening that has no demonstrated impact.

## Safe harbor

We will not pursue or support legal action against good-faith research that follows this policy,
avoids privacy violations and service disruption, and only touches accounts and devices you own.
