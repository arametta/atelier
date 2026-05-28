# Atelier — Project Instructions

## Override notice
The parent `/Users/andrea.rametta/Downloads/CLAUDE.md` targets a different project (Java/Spring/JRuby/TQL/Fastorm, GitLab, RELEX Plan). **Ignore it for this repo** except for two rules that still apply:
- Google Kotlin Style, trim trailing whitespace
- Be concise; bluntness over politeness; ask if the prompt is ambiguous

Everything else in the parent file (Spring, JRuby, GitLab, multi-module Gradle, OWASP Java rules) does **not** apply here.

## Project identity
Atelier is an **Android Kotlin + Jetpack Compose** app for tracking a personal fragrance collection (bottles, notes, spray-by-spray usage logs, cost analytics). Single-module Gradle. Single Activity. Local Room database. Gemini API used for metadata auto-fill.

## Domain model
- `Bottle`: `house, name, concentration, sizeMl, price, currency, purchaseDate, mlPerSpray, imageUrl, topNotes, middleNotes, baseNotes, family, year, description, perfumer, personalNotes`
- `LogEntity`: `bottleId` (logical reference to `Bottle.id` — no DB-level `@ForeignKey` declared, cascade is done manually in `FragranceRepository.deleteBottle`), `date`, `sprays`, `notes`
- Derived values live on `BottleWithLogs` in the ViewModel (`combine(bottlesList, logsList)`) — never stored as columns: `totalSprays, spraysUsed, spraysRemaining, mlRemaining, percentRemaining, costPerSpray, lastUsedDate, sessionsCount`

## Tech stack (see `gradle/libs.versions.toml` for pinned versions)
- Kotlin 2.2.10, AGP 9.1.1, Compose BOM 2024.09.00, Java 11 source
- AGP/Kotlin/Compose-compiler versions are tightly coupled — bumps risk silent Compose breakage; do not upgrade without a full test pass
- Room 2.7.0 via KSP
- Retrofit + Moshi + OkHttp for the Gemini REST API
- Coil for image loading
- Robolectric + Roborazzi for unit + screenshot tests
- Secrets Gradle Plugin injects `.env` values into `BuildConfig`

## Architecture
- MVVM: `AndroidViewModel` + `StateFlow`, observed with `collectAsStateWithLifecycle()`
- Data flow: `FragranceDao` → `FragranceRepository` → `FragranceViewModel` → Compose
- DB writes on `Dispatchers.IO`
- One-shot events via `MutableSharedFlow` (e.g. toast pattern)
- Navigation is **state-based** today (no Navigation Compose) — if you introduce nav, also restructure the screen files
- No DI framework — `RoomDbHelper` is a hand-rolled singleton

## Code style
- Google Kotlin Style, 2-space indent (match existing files)
- One Composable per logical screen; **do not extend the existing god-files** (`MainScreen.kt`, `PerfumeDetailScreen.kt`) — when touching either file, split the specific composable you are modifying into its own file first, then make your change
- Default to no comments; only add one when the *why* is non-obvious
- Prefer editing existing files over creating new ones

## Build & test
- `./gradlew :app:assembleDebug` — debug build
- `./gradlew :app:testDebugUnitTest` — unit + Robolectric tests
- `./gradlew :app:recordRoborazziDebug` — record screenshot baselines
- `./gradlew :app:verifyRoborazziDebug` — verify against baselines

## Security
- Never commit `.env` or any API key
- Never log API keys, request bodies, or Gemini responses
- Treat Gemini JSON output as untrusted — keep `cleanJsonString` + Moshi schema validation
- The Gemini API key currently ships inside the APK via `BuildConfig` — acceptable for personal use, **must** move behind a backend proxy or Firebase AI Logic before any public release
- `allowBackup=true` is on — user's fragrance DB syncs to their Google Drive; decide before release

## Known issues (do not build on top of broken state — fix or work around)
- Room uses `fallbackToDestructiveMigration()` and `exportSchema=false` — **any schema change will silently wipe all user data with no warning and no recovery**. Fix this before shipping. Until it is fixed: do not change any Room entity or DAO without writing a proper migration first.
- `firebase-bom` is included but no Firebase product is actually used — remove or wire up.
- `firebase-ai` and several `camera-*` / `navigation-compose` / `datastore-preferences` / `accompanist-permissions` / `play-services-location` libraries are declared in `libs.versions.toml` but commented out in `app/build.gradle.kts` — don't re-add unless actually used.

## Don't
- Don't introduce Firebase products unless wiring them in
- Don't add backend/server code, JRuby, Spring, TQL — wrong project
- Don't enable `isMinifyEnabled = true` without first adding proguard rules for Moshi and Retrofit
- Don't write multi-paragraph KDoc; one line max
- Don't create planning/analysis `.md` files unless asked
- Don't change the Room schema without writing a migration — `fallbackToDestructiveMigration()` is active and will destroy user data silently

## VCS — GitHub
- Branch from `main`; PR back into `main`
- Commit messages: conventional style matches the existing history (`feat:`, `build:`, etc.)
- Use `gh` CLI for issues, PRs, checks, releases
- Don't push or open PRs without explicit user instruction
