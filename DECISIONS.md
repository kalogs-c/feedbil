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

## 2026-11-18 — Room for local persistence

**Decision:** Feeds and articles are stored in Room 2.8.5 (runtime plus
the KSP compiler) in the on-device database `feedbil.db`, version 1.
Repositories own all database access. `room-testing` is used for
storage tests and schemas are exported to `app/schemas`.

**Alternatives considered:**

1. SQLDelight — SQL-first with compile-checked queries and no
   annotation processing, but less conventional for an Android-only
   app and a different migration story for no MVP benefit.
2. Hand-written SQLite via `androidx.sqlite` — no codegen, but cursor
   mapping and the retention queries would be hand-rolled in several
   places.
3. JSON/DataStore files — trivial to start, but no querying or
   ordering; retention and duplicate detection become app logic and
   every write rewrites the whole file.

**Rationale:** Retention is a query problem ("expired by age" and "keep
the newest N"), and `@Transaction` lets an insert batch and the
following prune commit atomically. Room also gives observable queries
for Milestone 4 and migration tooling we must document anyway.

**Consequences:** The build gains its first annotation processor. KSP
2.3.1+ is required for AGP 9's built-in Kotlin; the project uses KSP
2.3.14 and verified the build on 2026-11-18.

## 2026-11-18 — Article identity and duplicate detection

**Decision:** Local identity is a `Long` autoincrement primary key. An
entry is a duplicate when the feed already holds the same `link` or the
same non-null `guid`, enforced by two unique indexes —
`(feedId, link)` and `(feedId, guid)` — with inserts using
`OnConflictStrategy.IGNORE`. SQLite treats `NULL` as distinct in unique
indexes, so articles without a guid coexist freely. Existing rows are
never overwritten on refresh, and entries without a link are not stored
because the MVP cannot open or share them.

**Alternatives considered:**

1. Synthetic `dedupeKey` column (`guid` else `link`) — one index and one
   rule, but it buries the article URL inside a derived string and adds
   a column that describes nothing the user sees.
2. `link`-only uniqueness — simplest, but an article re-published under
   a new URL (tracking parameters, `http`→`https`, edited slug) would be
   stored again, which is exactly what `guid` exists to prevent.
3. Repository-level "check then insert" — flexible matching, but the
   invariant would live in code rather than storage.
4. UUID string identities — portable identity with no benefit while
   there is no sync, at the cost of larger indexes.

**Rationale:** Two indexes over real columns keep the database as the
authority, so no write path can bypass duplicate detection.

**Consequences:** Two entries with different guids but the same link
merge into one row. Normalization is trim-only, so the same article
under a URL with changed query parameters is stored twice; recorded in
`LIMITATIONS.md`.

## 2026-11-18 — Publication date policy

**Decision:** `articles.publishedAt` is always present. It holds the
feed-provided date when one parses, and otherwise the time the entry was
stored. Future dates are accepted as given. The 7-day rule deletes rows
strictly older than the cutoff; a row exactly 7 days old is retained.

**Alternatives considered:**

1. Nullable `publishedAt` with a separate observation timestamp used
   only for ordering and retention — the most honest model, but it was
   rejected in favor of always having a date to display.
2. Treat dates more than 24h in the future as missing — protects against
   misconfigured feeds at the cost of another special case.
3. Drop entries without a usable date — loses many real articles.

**Rationale:** Ordering and retention need a value for every article;
undated feeds are common. Freezing the fallback at insert time (instead
of evaluating "now" when querying) keeps undated articles from looking
permanently fresh.

**Consequences:** This is a deliberate exception to "never invent feed
data": the list may show the observation time as an article's date, and
the stored value cannot be distinguished from a real one later. Noted in
`LIMITATIONS.md`.

## 2026-11-18 — Read tracking and unread-first retention (scope addition)

**Decision:** Articles carry `lastReadAt: Long?` — `null` means the user
has never opened the article, any value is when they last did. When a
feed exceeds 100 articles, retention keeps unread articles in
preference to read ones, then the newest by `publishedAt`. The 7-day
expiry ignores read state.

**Alternatives considered:**

1. Boolean `isRead` — less information for the same column cost; the
   timestamp also answers "when" for future features.
2. No read tracking in the MVP (the original scope) — retention would be
   purely recency-based.

**Rationale:** Requested during Milestone 2 so that the cap does not
discard articles the user has not seen yet.

**Consequences:** Read state is an approved MVP addition; `ROADMAP.md`
§0 and §5 were updated. Opening an article must record `lastReadAt`
(Milestone 5) and the list should reflect read state (Milestone 4).
Retention ordering is intentionally not the same as list ordering.

## 2026-11-18 — Domain models separate from Room entities

**Decision:** `domain/model` holds plain `Feed` and `Article` classes;
Room entities live in `data/local/entity` with small extension-function
mappers. Repositories expose domain types only.

**Alternatives considered:** Exposing entities directly — fewer files,
but Room annotations would leak into UI, AI, and parsing code, and the
storage shape would become the app's model.

**Rationale:** Milestone 4's parser needs a storage-free target, and
storage details (indexes, nullability of `lastReadAt`, etc.) should not
dictate the UI model. Two classes and two mappers are cheap now.

**Consequences:** Mapping must be updated whenever the schema changes.

## 2026-11-18 — Migrations: exported schemas, no destructive fallback

**Decision:** Room exports schemas to `app/schemas` via the KSP
`room.schemaLocation` argument, and version 1 is committed. Every
version bump ships a hand-written migration. There is no
`fallbackToDestructiveMigration()`.

**Alternatives considered:** Destructive fallback during development
(faster, but a released build would silently delete the user's feeds),
and Room auto-migrations (useful later, but they cannot express every
change and are not needed for version 1).

**Rationale:** Losing local-only data is unrecoverable — there is no
account or backup to restore from — so failing loudly is preferable.

**Consequences:** Schema JSON files are reviewed like code. Milestone 4's
`lastFetchedAt` column will exercise the first real migration.

## 2026-11-18 — Storage tests on the JVM with Robolectric

**Decision:** DAO, repository, and retention behavior are tested with
Robolectric 4.18.1 and in-memory Room in `testDebugUnitTest`. A
file-backed database test closes and reopens the database to prove data
survives a restart. No instrumented database tests are added for now.

**Alternatives considered:** Instrumented `androidTest` only — real
SQLite, but the documented default test command would not cover storage
and every run would need a device. Running both — more confidence, more
setup, duplicated coverage.

**Rationale:** The tests the roadmap requires (retention, duplicates,
relations) run in the default command and stay deterministic.

**Consequences:** The first test run downloads Robolectric's Android
simulator jars. Tests follow the project's `targetSdk` (currently 37);
if a future SDK is not yet supported by Robolectric, pin
`@Config(sdk = [...])`.
