# Decisions

Approved technical decisions, in the order they were made. Each entry
records the date, the alternatives considered, and the rationale.

## 2026-11-18 — Always-dark OLED theme

**Decision:** The app ships a single, always-dark theme built on true
black (`#000000` background/surface) with muted, low-saturation accents.
Dynamic (wallpaper-based) color is not used.

**Alternatives considered:**

1. System-following light/dark with a custom OLED-friendly dark palette.
   Respects the system setting, but doubles the surface to design,
   test, and maintain for an MVP, and light mode adds bright-surface
   risk the product explicitly wants to avoid.
2. System-following with dynamic color on Android 12+ (template
   default), falling back to a custom scheme below that. Pros: coherent
   with the user's wallpaper on modern devices. Cons: loses control over
   darkness — dynamic dark schemes are not true-black, which defeats the
   OLED goal on exactly the devices that matter most.
3. Always-dark OLED (chosen). Pros: simplest, consistent with the
   product constraint ("OLED-friendly dark theme; avoid unnecessary
   bright surfaces"), no light-theme audit burden. Cons: ignores the
   system setting; users who prefer light mode have no in-app option in
   the MVP.

**Rationale:** The OLED/true-black requirement is a stated product
constraint (ROADMAP.md §0). One theme means one set of colors to verify
for contrast, focus order, and font scaling (milestone 8). A light theme
can be added post-MVP if user demand justifies it.

**Consequences:** `FeedbilTheme` has no `darkTheme`/`dynamicColor`
parameters. The window theme (`themes.xml`) parents a dark platform
theme so there is no light flash before Compose draws. The default
Android Studio launcher icon is intentionally kept as a placeholder
(confirmed with the toolchain baseline on 2026-11-18).

## 2026-11-18 — Toolchain baseline confirmed; minSdk raised to 26

**Decision:** The Android Studio template toolchain is confirmed as-is,
with one active change: `minSdk` is raised from 24 to 26 (Android 8.0,
"Oreo"). Single `:app` module. AGP 9.5.1 with built-in Kotlin (no
separate `kotlin-android` plugin), `compileSdk`/`targetSdk` = 37, Java
11 source/target compatibility, Gradle 9.6 wrapper, Gradle daemon JVM
toolchain 25 with foojay auto-provisioning, configuration cache,
`kotlin.code.style=official`. Build/tests/launch verified on Windows
(see Milestone 1 in `ROADMAP.md`).

**Alternatives considered (minSdk):**

1. Keep 24: widest reach, but `java.time` (needed for RSS publication
   dates) requires API 26 or core-library desugaring.
2. 28+: more modern APIs, but no need for an RSS reader, and cuts real
   users for no benefit.
3. **26 (chosen):** native `java.time` with zero desugaring setup;
   Android 7.x is <2% of devices in 2026.

**Alternatives considered (project structure):** multi-module (`:core`,
`:data`, `:ui`) — stronger isolation, but wiring/config overhead with
no MVP payoff; boundaries can be extracted later. Single module kept.

**Rationale:** The template builds and runs on the target Windows
machine; each change from it adds maintenance without MVP benefit. minSdk
26 is the one change with a concrete payoff (date-time parsing).

## 2026-11-18 — Light layered architecture, manual construction (no DI)

**Decision:** The app uses a light layered architecture: Compose UI
observes ViewModel state, small repository classes hide Room and the
network stack. No domain/use-case layer until complexity demands it.
Dependencies are constructed manually (app-level container / constructor
defaults); no DI framework.

**Alternatives considered (architecture):** plain Compose state +
ViewModel without repositories — storage/network/AI would blur by
Milestone 3 and testing suffers; full Clean Architecture — premature
interfaces and mapping for MVP scope.

**Alternatives considered (DI):** Hilt — compile-time checked but adds
KSP/generated code and learning curve; Koin — lighter, still a runtime
framework. Manual construction is transparent and sufficient for the
handful of objects this app creates; tests can substitute fakes.

**Consequences:** Screens never touch Room or the HTTP client directly.
If the object graph grows, Hilt can be adopted later without changing
the layer boundaries.
