---
name: android-google-fonts
description: How ISSASODA Android apps (Kotlin/Compose) use Google Fonts — downloadable fonts from the Play Services provider with font_certs.xml, never bundled TTFs. Use whenever a new or existing Android app needs custom fonts/typography, when choosing typefaces, or when a Google font "doesn't load".
---

# Google Fonts in ISSASODA Android apps

**Rule (user decision, 2026-09-24):** every app uses Google Fonts through the **Play Services
downloadable-font provider**, exactly like ISSAGRAPPH. Do NOT bundle .ttf files in `res/font`,
do NOT switch to bundled fonts because the emulator fails (see "Emulator trap"). Confirm the
typeface choice with the user when starting a new app.

Reference implementation: `C:\Users\24\AndroidStudioProjects\ISSAGRAPPH\app\src\main\java\com\issasoda\issagrapph\ui\theme\Fonts.kt`
and `...\ISSAGRAPPH\app\src\main\res\values\font_certs.xml`. Second example: ISSASTUDENT `ui/theme/Fonts.kt`.

## Steps

1. **Dependency** (Compose BOM gives the version):
   - `gradle/libs.versions.toml`: `compose-ui-text-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts" }`
   - `app/build.gradle.kts`: `implementation(libs.compose.ui.text.google.fonts)`
2. **Certificates:** copy ISSAGRAPPH's `res/values/font_certs.xml` as is (array `issa_fonts_certs`
   = dev + prod certificate arrays of the GMS font provider). Without the right certs every request fails.
3. **Fonts.kt** (package `ui.theme`):
   ```kotlin
   private val googleFontProvider = GoogleFont.Provider(
       providerAuthority = "com.google.android.gms.fonts",
       providerPackage = "com.google.android.gms",
       certificates = R.array.issa_fonts_certs,
   )
   private fun googleFamily(name: String, vararg weights: FontWeight) = FontFamily(
       weights.map { androidx.compose.ui.text.googlefonts.Font(GoogleFont(name), googleFontProvider, it) },
   )
   val BodyFamily = googleFamily("Atkinson Hyperlegible", FontWeight.Normal, FontWeight.Bold)
   ```
   - Name = exact family name on fonts.google.com ("Source Sans 3", "Baloo 2", "Plus Jakarta Sans").
   - Declare only the weights the typography actually uses (each weight = one request).
   - Use the families in `Typography(...)` in Theme.kt. While a font downloads (or if missing),
     Compose draws system sans-serif; the UI never blocks. No manual preload needed.
4. **Verification test** (androidTest, e.g. `GoogleFontsTest`): for every family+weight used,
   call `FontsContractCompat.requestFont(context, FontRequest("com.google.android.gms.fonts",
   "com.google.android.gms", "name=Lexend&weight=700", R.array.issa_fonts_certs), callback, handler)`
   and assert `onTypefaceRetrieved`. Log `onTypefaceRequestFailed(reason)`. ISSASTUDENT has a
   complete one: `app/src/androidTest/java/com/issasoda/issastudent/GoogleFontsTest.kt`.
   Run it on a **real phone** (`ANDROID_SERIAL=<serial> ./gradlew connectedDebugAndroidTest
   -Pandroid.testInstrumentationRunnerArguments.class=...GoogleFontsTest`).
   Note: connectedAndroidTest uninstalls the app afterwards (the user must sign in again).
5. Visually confirm on the real phone (screenshot): titles, body and numbers show the chosen faces.

## Emulator trap

The Android Studio emulator's Play Services font directory is stale: it answers
`FAIL_REASON_FONT_NOT_FOUND` (reason=1) for Lexend, Inter, JetBrains Mono, Atkinson Hyperlegible,
Source Sans 3, Figtree… while old families (Nunito Sans, Noto Sans) load. Same GMS version on the
S25 (real phone) serves all of them. So on the emulator the app shows system sans — that's expected,
NOT a reason to bundle fonts. Always judge fonts on a real phone.

## Choosing faces (what has worked)

- Reading text: Atkinson Hyperlegible (distinguishes I/l/1, O/0), Lexend, Inter, Source Sans 3.
- Titles: Lexend, Space Grotesk, Outfit, Sora.
- Numbers/stats/code: JetBrains Mono (aligned digits).
- Kids: Fredoka, Baloo 2, Nunito.
- Avoid for small reading text: display serifs (Playfair, DM Serif Display), monospace, Quicksand.

## In this repository (ISSASPEAKER)

This skill is copied from the user-level skill so cloud sessions can read it. The ISSAGRAPPH and
ISSASTUDENT paths above exist only on the developer's PC. Once M0 copies `font_certs.xml` into
`app/src/main/res/values/`, that file is the reference here. Font checks on a real phone are done
from the PC session (the cloud has no device).
