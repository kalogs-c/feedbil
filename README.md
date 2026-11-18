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
- Android SDK with:
  - `platforms;android-37.0` (compileSdk/targetSdk 37)
  - `build-tools;36.0.0`
  - `platform-tools`
  - Package ids may differ between channels; check
    `sdkmanager --list | grep -E "platforms/android-37|build-tools;36"`.
- `local.properties` (not committed) must point `sdk.dir` at the SDK.
  Note: an SDK installed on Windows is **not** usable for Linux builds
  (build-tools/aapt are Windows binaries); install a Linux SDK instead.

## Build and test

```sh
./gradlew :app:assembleDebug      # build the debug APK
./gradlew :app:testDebugUnitTest  # run local unit tests
```

Instrumented tests (`./gradlew :app:connectedDebugAndroidTest`) require a
running emulator or a physical device with USB debugging enabled.

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
