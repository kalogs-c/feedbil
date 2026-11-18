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
theme so there is no light flash before Compose draws.

## Pending discussion (inherited from the Android Studio template)

The following were fixed by the project template, not actively chosen.
They are recorded here as **unconfirmed** and pending a dedicated
discussion; do not treat them as approved decisions until then.

- Minimum Android version: `minSdk = 24`
- Single-module project (`:app`)
- AGP 9.5.1 with built-in Kotlin support (no `kotlin-android` plugin
  applied separately)
- `compileSdk`/`targetSdk` = 37
- Java 11 source/target compatibility, Gradle 9.6 wrapper, Gradle daemon
  JVM toolchain 25 with foojay auto-provisioning
- Configuration cache enabled; `kotlin.code.style=official`
- Launcher icon: default Android Studio placeholder
