---
name: retire-one-shot-scripts
description: Decide what happens to a script after it has done its one job — delete it, keep it in tools/legacy/, or leave it in tools/. Use whenever a migration, rename, bulk edit or data campaign finishes and a script exists that produced the change.
---

# Retiring a one-shot script

A script that has been run once and cannot usefully be run again is not neutral. It is a loaded gun
in the repository: the next developer finds it, cannot tell whether it still needs running, and runs
it. In this project that would rebuild `curriculum.json` from a snapshot and destroy every `about`,
every prerequisite and every correction written since.

Run this the moment a script's job is done — not "later, when tidying". The window where somebody
remembers what it was for is the window where the decision is cheap.

## The decision

Ask one question:

> **Does the script hold reasoning that cannot be recovered from what it produced?**

- **No** → **delete it.** The artefact is the record.
- **Yes** → move it to `tools/legacy/` and write down, in `tools/legacy/README.md`, what the
  reasoning is and why re-running it would be destructive.
- **It is meant to be re-run as part of the workflow** → it is not a one-shot. Leave it in `tools/`
  and make sure `tools/README.md` says when to run it.

Nothing goes in a third place. No `_old`, no `.bak`, no commented-out call site.

### Worked examples from this repository

| Script | Question | Outcome |
|---|---|---|
| `rename_degree_ids.py` | Renamed 585 ids. **The new ids ARE the mapping** and they are in the asset | Deleted the day it ran |
| ~30 `add_<area>.py` translation scripts | Each held a table of Spanish and Portuguese, applied once. The translations are in the asset | Deleted |
| `build_curriculum.py` | Holds the strand assignment for all 63 MSC areas, the depth bands and the article table for "History of X" — **none of which is recoverable from the merged file** | `tools/legacy/`, with "read it; do not run it" |
| `format_curriculum.py` | Run after every hand edit | Stays in `tools/` |

## Before deleting

1. **Confirm the artefact is committed.** The script's output has to exist somewhere durable first.
   Deleting the script while its result is uncommitted loses both.
2. **Move the reasoning that is worth keeping into prose** — `docs/HANDOVER.md`, `tools/README.md`,
   or a memory file — and say where it went. A rule like "the mnemonic is assigned once and retired
   with its module" is not visible in the data; it has to be written somewhere a person reads.
3. **Grep for references.** A README, a roadmap or a comment that says "see `tools/foo.py`" becomes a
   dangling pointer, which is the same failure as stale documentation: a map to where the thing is
   not. Fix every one.
4. **Delete it in the same commit as the change it made.** Split across two commits, the middle state
   is a repository where the gun is loaded and the safety is off.

## Make the script refuse itself

While a one-shot still exists, it should be unable to run twice:

```python
if any(n['id'].startswith('U.') for n in nodes):
    sys.exit('already renamed: an id starts with "U." -- refusing to run twice')
```

This is not a substitute for deleting it. It is what protects the hours between running it and
retiring it.

## The rule, stated once

> A script belongs in `tools/` when re-running it is part of the workflow. If re-running it would
> corrupt an asset or do nothing, it does not belong in the repository — unless it is the only
> record of a decision, in which case it belongs in `tools/legacy/` with a warning.
