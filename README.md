# feedbil

Local-first Android RSS reader built with Kotlin and Jetpack Compose.
No account, no cloud sync, no app-owned backend: feeds and articles are
stored on the device, and AI summaries are generated on-device with
LiteRT.

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
  point and home placeholder screen
- `app/src/main/java/com/kalogsc/feedbil/ui/theme/` — OLED-friendly
  always-dark Material 3 theme

## Documentation

- [ROADMAP.md](ROADMAP.md) — MVP scope and milestone plan (status is kept
  up to date as milestones complete)
- [AGENTS.md](AGENTS.md) — working agreement for AI-assisted development
- [DECISIONS.md](DECISIONS.md) — approved technical decisions and their
  rationale
- [LIMITATIONS.md](LIMITATIONS.md) — known MVP limitations and device
  requirements
