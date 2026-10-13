#!/usr/bin/env bash
# Owner-only: copies each app's release inputs from the owner's PC into the "play" environment secrets of its
# repository (never repository-level secrets). Values are piped to gh and never printed.
#   bash tools/play/set-release-secrets.sh                # every app below
#   bash tools/play/set-release-secrets.sh EmuGaMind      # one repository
# Sources: D:\SC\AK\signing\<app>-upload.jks/.properties (see D:\SC\AK\README.md), the app's own git-ignored
# app/google-services.json and, per app, the extra inputs its build needs (listed in its release.yml header).
set -euo pipefail
projects=/c/Users/24/AndroidStudioProjects
signing=/d/SC/AK/signing
all="EmuGaMind:emugamind ISSAGRAPPH:issagrapph ISSASTUDENT:issastudent BookCompass:bookcompass ISSASPEAKER:issaspeaker"
for entry in $all; do
  name="${entry%%:*}"; app="${entry#*:}"
  if [ $# -gt 0 ] && [[ " $* " != *" $name "* ]]; then continue; fi
  repo="Ataraxiainc2/$name"; dir="$projects/$name"; props="$signing/$app-upload.properties"
  get() { grep -m1 "^$1=" "$props" | cut -d= -f2- | tr -d '\r\n'; }
  put() { gh secret set "$1" --env play --repo "$repo" > /dev/null; }
  base64 -w0 "$signing/$app-upload.jks" | put UPLOAD_KEYSTORE_B64
  get storePassword | put UPLOAD_STORE_PASSWORD
  get keyAlias | put UPLOAD_KEY_ALIAS
  get keyPassword | put UPLOAD_KEY_PASSWORD
  if [ -f "$dir/app/google-services.json" ]; then base64 -w0 "$dir/app/google-services.json" | put GOOGLE_SERVICES_JSON_B64; fi
  case "$app" in
    emugamind)
      grep -m1 '^ADMIN_UID=' "$dir/local.properties" | cut -d= -f2- | tr -d '\r\n' | put ADMIN_UID ;;
    bookcompass)
      python -I -c "import sys; sys.dont_write_bytecode = True; sys.path.insert(0, r'D:\SC\AK'); import issa_keys as k; sys.stdout.write(';'.join(k.keys('google_books')))" \
        | put GOOGLE_BOOKS_KEYS ;;
  esac
  echo "$repo play secrets: $(gh secret list --env play --repo "$repo" | cut -f1 | tr '\n' ' ')| repository-level: $(gh secret list --repo "$repo" | wc -l)"
done
