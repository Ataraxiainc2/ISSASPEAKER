# UX principles (2026) adapted to ISSASPEAKER

The owner's checklist of current web UX practices, translated to a native Android app. Each area says what
the app does today (**done**), what is planned (**next**), or why the item does not apply (**n/a**).

## 1. Structure and architecture
- **Done:** one purpose stated on Home (hero: "Your routines, spoken at home" + tagline); four top-level
  destinations (Home, Settings, About, Admin when allowed); one design system: spacing scale `Spacing`,
  sizes `Sizes`, brand accents `BrandColors` (`ui/theme/Tokens.kt`), shared components
  (`ScreenScaffold`, `AdaptiveGrid`, `SectionCard`, `DotLine`, `AccountPanel`).
- **Done:** content ordered by importance: hero and main action first, then next steps, privacy, progress.
- **n/a:** decorative elements: none are used.

## 2. Header and navigation
- **Done:** logo in the top bar, tapping it returns Home; account photo always visible in the top bar;
  navigation adapts: bottom bar on narrow screens, rail on wide ones (Material 3 adaptive navigation suite);
  active destination highlighted; top bar collapses while scrolling and returns on scroll up.
- **Done:** language selector (Settings), help and support (About → Email support), system Back returns Home.
- **Next:** search across features once there is content to search (routines, history).
- **n/a:** breadcrumbs, footer navigation, dropdown menus (flat structure).

## 3. Home (hero)
- **Done:** visually distinct hero card with logo, concrete headline, tagline, primary action (Sign in with
  Google) and secondary action (Settings); no pop-ups; content visible at once (no splash wait).

## 4. Titles and typography
- **Done:** headings are semantic (`heading()` for screen readers); four text roles (titles, headings, text,
  labels) each with its own high-legibility Google Font, drawn at random (D9); text sizes follow the user's
  font-size setting (sp); fonts load asynchronously with system fallback; no text inside images.
- **Note:** a fixed brand typeface is a common practice; the owner chose random faces deliberately (D9).

## 5. Color
- **Done:** Material 3 Expressive color roles from the wallpaper (dynamic color), light, dark or system theme
  (D10) with the choice persisted; contrast is guaranteed by the Material color roles; error and status use
  color *and* text (never color alone); brand accents only for small markers.

## 6. Layout
- **Done:** full-width adaptive grid: as many columns as fit, capped by the number of cards so a screen never
  shows empty columns; same rules in portrait and landscape; no horizontal scrolling; edge-to-edge with safe
  insets; stable layout while data loads (placeholders, not jumps).

## 7. Cards and components
- **Done:** consistent cards (Material shapes), clear titles, outlined cards for dense lists, status labels
  in text plus color; tabs for the admin views; dialogs with explicit Cancel.

## 8. Buttons and actions
- **Done:** one filled primary button per area, outlined/text for secondary; verbs on every button ("Sign in
  with Google", "Erase", "Read the privacy policy"); 48 dp touch targets (Material default); loading state
  on sign-in (label changes, button disabled, no double submit); destructive action in error color behind a
  confirmation dialog.

## 9. Interaction and motion
- **Done:** Material 3 Expressive motion scheme; no infinite or flashing animations; Android's
  "remove animations" setting is honored by Compose.

## 10. Devices
- **Done:** phone portrait and landscape; large screens and foldables through window size classes; touch,
  keyboard and mouse through standard Compose components.
- **Next:** Wear OS companion (M5).

## 11. Accessibility (target WCAG 2.2 AA equivalents)
- **Done:** content descriptions on icons and photos, semantic headings, live region for sign-in errors,
  font scaling, 48 dp targets, contrast from Material roles, no audio autoplay.
- **Next:** automated accessibility checks in instrumented tests (Compose test semantics), TalkBack pass.

## 12. Content
- **Done:** short paragraphs, scannable lists, plain language, same terms everywhere, three languages,
  empty states ("Nothing here yet"), clear errors ("Sign-in did not complete. Check your connection…").

## 13. Images
- **Done:** vector logo (crisp at any density); profile photo loaded asynchronously with an icon fallback.

## 14. Forms
- **Done:** sign-in through Google's own sheet (no passwords typed in the app).
- **Next:** API-key forms with paste, validation and a test call before saving (M1).

## 15. SEO
- **n/a** for the app. The public pages (GitHub Pages) carry titles and descriptions.

## 16. Performance
- **Equivalent:** fast start (no splash work), assets read off the main thread, lazy lists, R8-optimized
  release. **Next:** baseline profile and startup benchmark.

## 17. Trust, privacy and security
- **Done:** privacy policy linked from Home, Settings and About; permissions explained before they are
  requested; no dark patterns; "Erase everything" always reachable; HTTPS only; see the threat model.

## 18. AI interfaces
- **Next (M1–M2):** AI output labeled as such, sources cited, confirmation before any action, stop control
  for running routines, editable history (prompt-injection guard F021).

## 19. Personalization
- **Done:** theme, language, persisted; account state visible; sign-out and erase reachable.
- **Next:** notification preferences when notifications exist.

## 20. Visual trends 2026
- **Adopted:** Material 3 Expressive, expressive typography (random curated faces), saturated brand
  accents from the logo, dark mode, voice interface (the speaker itself).
- **Not adopted:** 3D, parallax, neo-brutalism: no value for a utility app.

## 21. Maintenance
- **Done:** tokens centralized, reusable components, CI with Lint/detekt/tests, dependency verification.
- **Next:** screenshot tests for regressions.

## 22. Complete experience
- **Done:** Home, About, Settings, privacy policy (web), help (email), error and empty states, admin status.
- **Next:** onboarding (F005), routine screens, history.
