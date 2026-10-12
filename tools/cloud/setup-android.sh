#!/usr/bin/env bash
# Prepares a Claude Code CLOUD session to build ISSASPEAKER. Runs from the SessionStart hook in
# .claude/settings.json and does nothing anywhere else (the developer's PC already has Android Studio).
#
# Installs the Android command-line tools (checksum verified against Google's repository index),
# accepts the SDK licenses and writes local.properties. Gradle downloads the platform and build tools
# it needs on the first build. Also installs codespell for the spelling gate.
set -euo pipefail

[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0

SDK="${ANDROID_HOME:-$HOME/android-sdk}"
PROJECT="${CLAUDE_PROJECT_DIR:-$(pwd)}"
SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"

if [ ! -x "$SDKMANAGER" ]; then
  work="$(mktemp -d)"
  # Pick the newest Linux command-line tools archive and its SHA-1 from Google's repository index.
  read -r zip sha1 < <(curl -fsSL https://dl.google.com/android/repository/repository2-3.xml | python3 -c '
import re, sys, xml.etree.ElementTree as ET
best = None
for archive in ET.parse(sys.stdin).getroot().iter("archive"):
    url = archive.findtext("complete/url") or ""
    match = re.fullmatch(r"commandlinetools-linux-(\d+)_latest\.zip", url)
    if match and (best is None or int(match[1]) > best[0]):
        best = (int(match[1]), url, archive.findtext("complete/checksum"))
print(best[1], best[2])
')
  curl -fsSL -o "$work/tools.zip" "https://dl.google.com/android/repository/$zip"
  echo "$sha1  $work/tools.zip" | sha1sum -c --quiet -
  mkdir -p "$SDK/cmdline-tools"
  unzip -q "$work/tools.zip" -d "$work"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$work/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$work"
fi

yes | "$SDKMANAGER" --licenses > /dev/null || true
"$SDKMANAGER" --install "platform-tools" > /dev/null

echo "sdk.dir=$SDK" > "$PROJECT/local.properties"
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export ANDROID_HOME=\"$SDK\"" >> "$CLAUDE_ENV_FILE"
fi

python3 -m pip install --quiet --user --require-hashes -r "$PROJECT/tools/requirements-ci.txt" \
  || echo "codespell not installed: run python3 -m pip install --require-hashes -r tools/requirements-ci.txt"

java_major="$(java -version 2>&1 | sed -nE 's/.*version "([0-9]+).*/\1/p' | head -1)"
if [ -z "$java_major" ] || [ "$java_major" -lt 17 ]; then
  echo "WARNING: JDK 17 or newer is needed to build (found: ${java_major:-none})." >&2
fi
echo "Android SDK ready at $SDK"
