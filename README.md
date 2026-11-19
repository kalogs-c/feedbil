# Feedbil

[![Release](https://github.com/kalogs-c/feedbil/actions/workflows/release.yml/badge.svg)](https://github.com/kalogs-c/feedbil/actions/workflows/release.yml)

Local-first Android RSS reader built with Kotlin and Jetpack Compose.
No account, no cloud sync, no app-owned backend: feeds and articles are
stored on the device, and AI summaries are generated on-device with
LiteRT.

## Downloads

Signed development builds are published to
[GitHub Releases](https://github.com/kalogs-c/feedbil/releases) on every
push to `main`. Download the attached `feedbil-<build>.apk` and install it
directly on an Android 8.0+ device. These are automated, unreviewed
snapshots, not a Google Play release.

The MVP scope, milestones, and acceptance criteria are defined in
[ROADMAP.md](ROADMAP.md). Read it before implementing features.

## Languages and conventions

- App UI language: Brazilian Portuguese (`pt-BR`). User-facing strings
  live in `app/src/main/res/values/strings.xml`; do not hard-code them in
  UI code.
- Source code, identifiers, comments, tests, and developer documentation:
  English.

## Requirements

- JDK: provisioned automatically by the Gradle wrapper via the
  `org.gradle.toolchains.foojay-resolver-convention` plugin (toolchain
  version 25, see `gradle/gradle-daemon-jvm.properties`).
- Android SDK installed on Windows (typically via Android Studio) with
  the API 37 platform and build-tools. Android Studio regenerates
  `local.properties` (not committed) with `sdk.dir` pointing at it.
- The project lives on the Windows filesystem (`C:\feedbil`). Build and
  run from Windows (`gradlew.bat` or Android Studio). Editing from WSL
  works, but Linux builds are not supported.

### JDK location on this machine

If `java` is not on `PATH` and `JAVA_HOME` is unset, point it at the
Android Studio JBR (OpenJDK 25) for the commands below:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
```

## Build and test

From Windows (PowerShell or cmd):

```powershell
.\gradlew.bat :app:assembleDebug      # build the debug APK
.\gradlew.bat :app:testDebugUnitTest  # run local unit tests
```

Instrumented tests (`.\gradlew.bat :app:connectedDebugAndroidTest`)
require a running emulator or a physical device with USB debugging
enabled.

## Project layout

Single-module app (`:app`), package `com.kalogsc.feedbil`:

- `app/src/main/java/com/kalogsc/feedbil/MainActivity.kt` — app entry
  point; hosts `ui/FeedbilNavHost.kt`, the four-destination navigation graph
- `app/src/main/java/com/kalogsc/feedbil/FeedbilApplication.kt`,
  `AppContainer.kt` — manual dependency container and startup retention
- `app/src/main/java/com/kalogsc/feedbil/ui/articles/` — combined article
  list, article detail screen, feed-HTML sanitizer, and their ViewModels;
  `ui/common/` holds pt-BR date formatting
- `app/src/main/java/com/kalogsc/feedbil/ui/feeds/` — feed list, add/edit
  form, and their ViewModels
- `app/src/main/java/com/kalogsc/feedbil/domain/` — feed and article
  models, retention rules, publication-date policy, feed date parsing, feed
  input validation
- `app/src/main/java/com/kalogsc/feedbil/data/local/` — Room entities,
  DAOs, and the versioned database
- `app/src/main/java/com/kalogsc/feedbil/data/` — repositories that hide
  storage details and the trigger-agnostic `FeedRefreshService`
- `app/src/main/java/com/kalogsc/feedbil/data/remote/` — shared HTTP
  client, feed fetcher, parser-to-article mapping, and typed fetch results
- `app/src/main/java/com/kalogsc/feedbil/ui/theme/` — OLED-friendly
  always-dark Material 3 theme

Database schemas are exported to `app/schemas/` and committed: every
schema change needs a migration, and there is no destructive fallback.

## Testing

`testDebugUnitTest` covers domain rules, storage and migrations, feed
fetching and mapping, refresh orchestration, validation, article HTML
sanitization, and all ViewModels.
Storage, parser, refresh, ViewModel, and Compose UI tests use Robolectric, so
they need no device; the first run downloads Robolectric's Android runtime
jars. Compose tests pin SDK 28 (Espresso's idle strategy needs an API level
Robolectric still provides). Compose tests assert accessible names, roles,
states, and key loading/empty/error content; they are not a substitute for
on-device TalkBack traversal. No test contacts a live feed: the parser runs
against fixtures in `app/src/test/resources/feeds/`.

## Documentation

- [ROADMAP.md](ROADMAP.md) — MVP scope and milestone plan (status is kept
  up to date as milestones complete)
- [AGENTS.md](AGENTS.md) — working agreement for AI-assisted development
- [DECISIONS.md](DECISIONS.md) — approved technical decisions and their
  rationale
- [LIMITATIONS.md](LIMITATIONS.md) — known MVP limitations and device
  requirements
