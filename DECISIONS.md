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

## 2026-11-19 — RSS/Atom parsing with rssparser 6.1.2

**Decision:** Feed XML is parsed with
`com.prof18.rssparser:rssparser` 6.1.2, used through `FeedFetcher`. The
project's own code never touches the XML parser. rssparser also supplies
the HTTP call it makes, so it is given the app's shared OkHttp client
(`RssParserBuilder(callFactory = httpClient)`).

**Alternatives considered:**

1. Android's `XmlPullParser` with hand-written RSS and Atom mapping — no
   dependency, but feeds in the wild vary (RSS 1.0/2.0, Atom, RDF,
   namespaced Media/ITunes/YouTube elements, CDATA, entity quirks), and
   mapping them correctly is a large, permanently maintained surface.
2. `rome`/`ROME` (Java) — mature but JVM-oriented, dated API, large
   dependency, and weaker Kotlin interop.
3. `kxml2` — a parser only; the mapping would still be ours.
4. rssparser (chosen) — Apache-2.0, Android-tested, covers RSS and Atom
   with typed models.

**Version pin:** 6.2.0 is compiled with Kotlin 2.4 metadata, which the
Kotlin 2.2 compiler bundled with AGP 9.5.1 cannot read (it reads up to
2.3). 6.1.2 is the newest release that compiles cleanly here; it targets
the OkHttp 4.12 API, whose `okhttp3.Call.Factory` supertype is still
present in OkHttp 5.5.1, so no older HTTP stack is pulled in.

**Consequences:** Parsing errors are the dependency's own exception types
(`RssParsingException`, `IllegalArgumentException`), mapped to
`FeedFetchResult` so UI code never sees them. `rssparser` uses
`XmlPullParserFactory`, which on the JVM resolves to the Android
implementation only under Robolectric, so parser tests run with
Robolectric. A feed the parser rejects is reported as "not a feed"
without being stored.

## 2026-11-19 — One OkHttp client for all feed requests

**Decision:** A single `OkHttpClient` (`FeedHttp.newClient()`) is created
by `AppContainer` and shared by rssparser. It sets a descriptive
`User-Agent`, a 10 s connect timeout, a 15 s read timeout, and a 20 s
whole-call timeout.

**Alternatives considered:** Default OkHttp timeouts (5 calls × 10 s can
leave the form spinning for minutes), per-request clients (reopens
connection pools), `HttpURLConnection` (no timeouts sharing, no
interceptors), Ktor (a second HTTP abstraction).

**Rationale:** One pool, one set of timeouts, and one place to add
headers. rssparser's own downloader does not set a `User-Agent`; the
interceptor covers both the parser and any future request.

**Consequences:** Raising the timeout is a single constant change. All
failures are mapped to `FeedFetchResult` — `HttpError(code)`,
`NotAFeed`, `Timeout`, `Network`, `InvalidUrl`, `Unknown` — and
cancellation is rethrown so a closed screen cancels its request.

**Correction (2026-11-19 review):** rssparser runs its download and parse in
internal `SupervisorJob` coroutines that do not inherit the caller's `Job`.
Cancellation is still rethrown, but it is observed only after the parser
finishes, so closing a screen does not actually abort the request. Recorded in
`LIMITATIONS.md`.

## 2026-11-19 — Strict add-feed validation before saving

**Decision:** Adding a feed validates the name and URL locally, rejects
a URL already stored, then fetches and parses the feed. Nothing is
persisted unless the fetch succeeds. A reachable feed that parses with
zero items is accepted (a new blog legitimately has no posts). The name
is required, trimmed, and capped at 80 characters; the URL must be an
absolute `http`/`https` URL with a host.

**Alternatives considered:**

1. Save first and validate in the background — keeps the tap instant,
   but leaves a broken feed imitating a working one until refresh.
2. Save on URL validity only and surface fetch errors in the list — the
   same problem, plus it lets a typo stay forever.
3. Save on failure with a "retry" flag — more state for no MVP benefit.

**Rationale:** A list of feeds that silently fail to download is worse
than a clear "I could not fetch this" at add time, and the user is
already looking at the form when the error occurs.

**Consequences:** Feeds cannot be added offline. Editing an existing feed
does not re-fetch unless its URL changed, so renaming works offline. A
duplicate URL is rejected by a repository check before fetching, and
still caught by the unique index if two writes race
(`DuplicateFeedUrlException`).

**Scope:** Milestone 3 validates reachability only; articles are stored
in Milestone 4. Fetching runs in `Dispatchers.IO` and every failure has
a pt-BR message.

## 2026-11-19 — Navigation Compose with string routes

**Decision:** Screens are hosted by `FeedbilNavHost` using Navigation
Compose 2.10.2 and plain string routes: `feeds` and
`feedForm?feedId={feedId}`. ViewModels are created at the destination
with `viewModel { }` and explicit constructor arguments from
`AppContainer`.

**Alternatives considered:** A single `when` over enum state (fine for
two screens, but back handling, saved state, and future screens are
hand-rolled); type-safe routes with serializable argument classes
(cleaner at scale, but one optional `Long` does not justify the extra
ceremony yet).

**Rationale:** Two destinations, one optional argument, and a form that
serves both add and edit. String routes are the smallest thing that
works and are easy to replace later.

**Consequences:** The feed id travels as `LongType` with
`Feed.UNSAVED_ID` as default, so the same form covers add and edit. The
feed list is the start destination. Migrating to type-safe routes is
contained to `FeedbilNavHost`.

## 2026-11-19 — Cleartext HTTP feeds are allowed

**Decision:** The manifest enables a network security config with
`cleartextTrafficPermitted="true"`, so `http://` feeds work.

**Alternatives considered:** HTTPS-only — safer, but many blogs are
still served over plain HTTP and would simply fail to add; allowing
cleartext app-wide (the manifest default on old target SDKs, but not on
modern ones) — same effect without an explicit, documented file.

**Rationale:** The MVP reads public feeds the user explicitly chooses;
blocking `http` would reject valid feeds with an error the user cannot
fix. The setting is declared in one file
(`res/xml/network_security_config.xml`) instead of relying on a
deprecated manifest default.

**Consequences:** Feed requests may travel unencrypted. No app data
other than the feed request is sent. Recorded in `LIMITATIONS.md`.

## 2026-11-19 — Articles-first home, feeds as a secondary screen

**Decision:** The start destination is the combined article list. Feed
management stays on the existing `feeds` screen, reached through a top-bar
action and with a back arrow. Article rows are display-only in M4; opening
an article arrives with M5.

**Alternatives considered:**

1. Feeds-first (keep the M3 home and add a per-feed article screen) — a small
   change, but two taps to read, no combined inbox, and a redundant "source"
   column.
2. Bottom tabs for Articles and Feeds — discoverable but more navigation UI
   and duplicated top-level state for only two destinations.
3. Articles-first (chosen) — reader-first, matches the requirement to show the
   source/feed on each row, and needs no per-feed article screen.

**Rationale:** M4 is the first milestone where the user comes to read, not to
manage subscriptions. The feed list remains one tap away for management.

**Consequences:** The start destination changed; existing feed-list navigation
gained a back action; string routes made this change local to
`FeedbilNavHost`.

## 2026-11-19 — Refresh triggers: launch plus manual, no background

**Decision:** All saved feeds are refreshed automatically once when the
article list first appears after the app process starts. Every later refresh
is an explicit top-bar action. There is no timer, no refresh on returning to a
still-running app, no pull-to-refresh, and no WorkManager job in M4.

**Alternatives considered:**

1. Manual only — simplest and predictable, but a fresh install with feeds
   added in M3 would show no articles until the user discovers the action.
2. Automatic on every resume when data is older than a time window — fresher,
   but needs a staleness policy and throttling, and causes network use every
   time the user returns to the app.
3. Periodic WorkManager refresh — runs while the app is closed, but Android
   makes intervals inexact and adds dependency/worker/retry complexity.
4. Launch once plus manual (chosen).

**Rationale:** The user asked for the smallest useful rule and explicitly
deferred background work. Keeping the refresh logic in a UI-independent
`FeedRefreshService` means a future worker can call the same method without
changing storage or refresh code.

**Consequences:** This supersedes the M3 "manual refresh only for the MVP"
decision. Returning to a backgrounded app whose process is still alive relies
on manual refresh; a failed launch refresh is not retried automatically.
An app opened with no feeds skips the automatic refresh entirely.

## 2026-11-19 — Persist the last successful fetch time

**Decision:** `feeds.lastFetchedAt` is a nullable `INTEGER` added in schema
v2 with a hand-written `MIGRATION_1_2`. It is set after a successful refresh
or after initial articles are stored on add/URL change. Existing feeds start
as `null`. The feed list displays "Nunca atualizado" or "Atualizado em …".

**Alternatives considered:** In-memory-only state (lost on restart), a
separate metadata table (more schema for one value), and DataStore (a new
dependency and two sources of truth).

**Rationale:** Staleness must survive restarts for users to judge whether a
feed is still working, and a future background trigger needs persisted state.
The migration path is exercised and tested now rather than under time pressure
later.

**Consequences:** Schema 1 and 2 JSON files are committed; destructive
fallback remains forbidden. Per-feed failures are still session-only, so after
a restart a long-failing feed is visible only through an old `lastFetchedAt`.

## 2026-11-19 — Store initial articles on add and URL change

**Decision:** The feed-validation fetch is reused: after a new feed is saved,
or after an existing feed's URL changes, its parsed items are mapped and stored
immediately with that feed's id. Renaming only never fetches. If article
storage fails, the feed remains saved and the next refresh heals the gap.

**Alternatives considered:** Require a manual refresh after adding (wastes the
fetch and shows an empty list), wrap feed and articles in one transaction
(across two repositories, which complicates the established boundaries), and
delete old articles when a URL changes (silent data loss).

**Rationale:** A new subscription should show content immediately without a
second request; retention will remove stale articles from a replaced URL
without deleting user history by hand.

**Consequences:** A feed can exist with `lastFetchedAt = null` if the initial
store failed; that is intentional and self-healing.

## 2026-11-19 — Feed item mapping and missing fields

**Decision:** An entry is stored only when it has a non-blank `link`. Trimmed
blank values become `null`; titles are never derived from the URL or the
description. A missing title renders as the localized "Sem título". Dates are
parsed from RFC 1123/RFC 822 and ISO 8601; unparseable or missing dates use the
observation time as already decided. HTML is stored as supplied and rendered
as plain text in the list.

**Alternatives considered:** Use the link as a title, derive a title from the
description, drop undated entries, or format dates with the device locale.

**Rationale:** "Never invent article data" is a product constraint; the
placeholder is explicitly a UI label, not feed data. Accepted date formats
cover the RSS and Atom variants seen in the fixtures without a date library.

**Consequences:** Some feeds legitimately show "Sem título". HTML sanitization
and full-text rendering are deferred to M5. Dates use pt-BR `dd/MM/yyyy, HH:mm`
in the device time zone, matching the app's fixed UI language.

## 2026-11-19 — Refresh orchestration, concurrency, and failure UX

**Decision:** `FeedRefreshService` fetches at most four feeds concurrently,
serializes triggers with a process-level mutex, and returns a `RefreshSummary`
with typed per-feed failures. Storage failures are isolated per feed; existing
OkHttp timeouts (10 s connect, 15 s read, 20 s call) are reused and there is no
automatic retry or conditional GET/ETag. Stored articles stay visible while a
refresh runs and after it fails; failures appear in a dismissible banner with
a details dialog and a retry action.

**Alternatives considered:** Sequential refresh (slower with many feeds),
unbounded parallel requests (server and battery unfriendly), automatic retry
with backoff (long waits and repeated load), hiding the list behind a full
spinner (hides already available content), and putting orchestration in the
ViewModel (harder to reuse from a future background worker).

**Rationale:** Each feed is independent, and the UI needs clear partial-failure
feedback without losing reading material. The service depends only on
repositories and a fetch function, so tests use fakes and no live feeds.

**Consequences:** A dead feed is retried only when the user refreshes or on the
next app process launch. Refresh always downloads the full feed; bandwidth is
acceptable at MVP scale. A concurrent manual tap during a refresh is ignored
by the ViewModel and serialized by the service.

## 2026-11-19 — Combined article query and ordering

**Decision:** A Room join exposes `ArticleListItem(article, feedName)` as a
`Flow`, ordered by `publishedAt DESC, id DESC`. Read state is shown with an
unread dot and a bolder title; there is no read filter or mark-as-read action
in M4.

**Alternatives considered:** Per-feed queries only (the articles-first home
needs a combined list), a view with a copied feed name (would go stale on
rename), and read/unread filtering (not in the MVP scope).

**Rationale:** Room invalidates the joined query when articles or feeds
change, so refresh, renames, deletes, and later read-state updates all appear
without manual reloads. Retention ordering is intentionally different: the
storage trim keeps unread articles first, while the list orders purely by date.

**Consequences:** Read state cannot be changed from the M4 screen; M5 will
record it when an article is opened and the indicator will update through this
same Flow.

## 2026-11-19 — Article content rendering: Compose + Coil, images online-only

**Decision:** The detail screen renders feed HTML as Compose text blocks and
images. A pure-Kotlin `ArticleContentFormatter` parses the stored
`contentHtml` (falling back to `description`) into paragraphs and image blocks.
It keeps `b`/`strong`, `i`/`em`, `u`, `br`, paragraph/list breaks and `http(s)`
links, decodes common and numeric entities, and drops scripts, styles,
iframes, images and links with any other URL scheme. Coil 3.3.0 with its OkHttp
network fetcher loads `<img src>` URLs from the feed. Images are **not**
downloaded or stored locally: offline reading shows a neutral experience
without images. Only images already present in the stored HTML are used;
`RssItem.image`/enclosure URLs are not stored or rendered.

**Alternatives considered:**

1. Plain text only — safest and smallest, but the user rejected dropping
   images, which are a requirement.
2. Sanitized `WebView` — easiest way to get images, formatting and links in
   one renderer, but heavy, memory-hungry, awkward to unit-test, and needs a
   locked-down configuration (no JavaScript, `http(s)` only).
3. Third-party Compose HTML library — another dependency with no concrete
   benefit over a focused parser for the small tag set we need.
4. Download images at refresh and store them on-device — real offline images,
   but needs bounded downloads, file storage, pruning and a v3 schema
   migration. Deferred as a separate piece of work.
5. Coil 3.6.x — uses Kotlin 2.4 metadata that this project's Kotlin 2.2
   compiler cannot read; 3.3.0 is the newest compatible line.

**Rationale:** The user confirmed that showing images is a must-have, but that
loading them on demand (not offline) is acceptable. Reusing the HTML already
stored avoids a schema change, and a pure-Kotlin parser keeps the security-
relevant logic directly unit-testable.

**Consequences:** Reading works offline for text; images require a network
connection. An image request goes to whatever host the feed names, which may
be a tracker. Images inside a sentence are hoisted to their own block instead
of flowing inline; this keeps layout simple and lets images size naturally.
The "Conteúdo fornecido pelo feed" caption makes clear the app shows RSS
content, not the publisher's full page. A malformed skipped tag without a
closing tag no longer truncates the article: parsing continues after its
opening tag, so its body may appear as plain (non-executable) text.

## 2026-11-19 — On-device AI: LiteRT-LM 0.16.1, public Gemma 4 E2B, in-app download

**Decision:** Summaries run locally through the LiteRT-LM Kotlin API
(`com.google.ai.edge.litertlm:litertlm-android:0.16.1`) on the CPU backend.
The single fixed model is `litert-community/gemma-4-E2B-it-litert-lm` /
`gemma-4-E2B-it.litertlm` (2,588,147,712 bytes, Apache-2.0). The app downloads
it anonymously from the Hugging Face `resolve/main` URL into `filesDir`, with a
progress indicator and an expected-size check; no account, token or file
import is needed. The model picker/settings idea is explicitly deferred.

**Runtime version:** 0.16.1 is the newest release whose Kotlin metadata (2.3.0)
the project's Kotlin 2.2.10 compiler can read; 0.17.0–0.18.1 carry 2.4.0
metadata and need Kotlin 2.3+. This is the same pinning pattern used for
rssparser and Coil. Upgrading the AGP/Kotlin toolchain was rejected for now:
it would invalidate the confirmed baseline and several existing pins.
Gemma 4 is supported at this runtime version (its README at tag v0.16.1 shows
`gemma-4-E2B-it.litertlm`, and support landed in v0.11/v0.13).

**Alternatives considered (model):**

1. Gemma 3 1B IT int4 (~529 MB) — the best small-model quality, but its
   Hugging Face repo is gated: the `resolve` URL returns 401 without a token,
   which would require an HF account and contradict the no-account decision.
2. Qwen3-0.6B (~614 MB, Apache-2.0, public) — good size and license, but its
   `.litertlm` file requires LiteRT-LM 0.18+, i.e. a Kotlin toolchain upgrade.
3. Gemma 4 E2B (chosen) — public, anonymous download, modern model, works on
   the pinned runtime. Cost: ~2.59 GB download and ~1.7 GB peak CPU RAM.

**Alternatives considered (delivery):**

1. Bundle the model in `assets/` — offline from first launch, but a 2.59 GB
   APK is impractical and the model could not be updated independently.
2. Manual file import — no network code, but the most setup friction.
3. Gallery-style HF browser login + WorkManager background download — best
   UX, but OAuth registration, secure token storage and a worker with
   notification are a separate milestone's worth of work.
4. Anonymous in-app streaming download (chosen) — reuses OkHttp with a
   dedicated long-timeout client, verifies the byte count before the engine
   sees the path, and writes through a `.part` file so a cancelled or failed
   download never leaves a corrupt model behind.

**AI contract:** The entire feed-provided text is used (HTML stripped by the
M5 sanitizer, `contentHtml` preferred over `description`); there is no
dedicated AI truncation yet, but the M5 sanitizer's existing 200,000-character
cap still applies. The user asked to tune limits after real use. Output is pt-BR,
bounded to ~200 tokens (roughly 2–3 sentences), uses low-temperature sampling
and a system instruction that forbids invented facts, markdown and preambles,
and explicitly treats article text as data so embedded instructions are
ignored. Output is always labeled "Resumo gerado por IA". The summary lives in
ViewModel memory only; reuse/caching for sharing is Milestone 7's decision.

**Consequences:** An engine is created lazily on the first summary and reused
afterwards, serialized by a mutex; generation uses the runtime's async Flow so
leaving the screen can cancel it via `cancelProcess()`, and native `Error`s
(such as `UnsatisfiedLinkError` on 32-bit ABIs) are caught and mapped to a
typed error rather than crashing. The `file` domain is excluded from Android
Auto Backup in both backup rule files so the ~2.59 GB model is never backed up;
the local database is still backed up. Long articles may exceed the effective
context and surface a specific error rather than a crash. Reading, share and
feed features do not depend on the model being present.

**Deferred:** model selection UI, persistent summaries, background download,
and GPU/NPU backends.

## 2026-11-19 — Milestone 7: sharing

**Decision:** Sharing uses a plain `Intent(Intent.ACTION_SEND)` with
`type = "text/plain"` and `Intent.createChooser(...)`; no new permission or
dependency. The article screen shows one share icon in the top bar that opens
a two-item dropdown: "Compartilhar" (normal) and "Compartilhar com resumo de
IA" (AI). AI sharing reuses a summary already generated in this screen's
ViewModel; if none exists and the model is `Ready`, it generates one through
the same M6 inference path and caches it. Otherwise a dismissible dialog
explains the failure and offers "Compartilhar sem resumo"; it also offers
"Baixar modelo" when the model is absent or failed, but never starts a
download on its own. Normal sharing never touches AI.

Share text (built by a pure, JVM-tested `ShareTextBuilder`, all words from
string resources):

1. Normal: the article title (or "Sem título") on one line, then the
   sanitized source URL.
2. AI: the summary, a blank line, then the sanitized source URL. **No
   "generated by AI" marker is included in the shared text.** This is an
   explicit override of the rule that AI output be presented as generated
   content: the on-screen "Resumo gerado por IA" label remains, but a
   recipient of the shared text sees only the summary and the URL.

Share actions are hidden when the feed link is not a valid `http(s)` URL.

**Alternatives considered (Sharesheet):**

1. `ShareCompat.IntentBuilder` (androidx.core, already on the classpath) —
   fluent API with subject/recipient helpers we do not need; identical
   Sharesheet and behavior. Plain intent is more transparent.
2. A custom Compose share sheet (copy link, etc.) — more control but a
   substantial UI and test surface for no MVP requirement.

**Alternatives considered (share UI):** two separate inline buttons (more
visible, but consumes reading space and the AI icon is ambiguous); one
share button with only normal sharing (fails the two-explicit-actions
requirement).

**Alternatives considered (AI reuse):**

1. Persist summaries in the `articles` table with a v3 migration — instant AI
   sharing across sessions, but schema, migration and test cost for little
   MVP benefit; article content is immutable, so staleness is not a concern.
2. Always regenerate — consistent, but slow and redundant when a summary is
   already on screen. Reuse in memory is the smallest change that avoids it.

**Alternatives considered (fallback):** show an error only (extra manual
step); silently downgrade to normal share (confusing — the user asked for an
AI summary). A dialog with explicit choices keeps the article and gives a
one-tap working path.

**Rationale:** The chooser, reuse, and fallback choices follow the project's
"smallest useful thing" rule and keep the M6 boundary intact: the app never
sends article text anywhere, and sharing works even when the model is absent.
The AI label override was made deliberately by the user after the conflict
with the "present as generated content" constraint was explained; it is
recorded here rather than treated as an accident.

**Consequences:** A summary generated on the article screen is reused even if
the model state later changes. Closing and reopening the article loses it, so
AI sharing then requires generation (and therefore the model). Shared AI text
has no generated marker; the source screen still labels it. Real share
targets and native inference were verified on a physical device on
2026-11-19 (see `ROADMAP.md`).

## 2026-11-19 — Links open in the external browser

**Decision:** Both the inline links in article content and the "Abrir
original" action use Compose's `LocalUriHandler` (the platform `ACTION_VIEW`
intent). `HttpUrlValidator` allows only absolute `http`/`https` URLs with a
host before any URL is opened or rendered as an image.

**Alternatives considered:** Custom Tabs (`androidx.browser`) keeps the user
closer to the app but adds a dependency; an in-app `WebView` is the heaviest
option and blurs the distinction between RSS content and a fetched publisher
page.

**Rationale:** External browser is predictable, needs no new dependency, and
matches the user's choice.

**Consequences:** Following a link leaves the app. Unsafe schemes become
plain, non-clickable text, so embedded instructions in feed content cannot
trigger navigation. A missing browser is swallowed rather than crashing the
screen: a guarded `UriHandler` is provided around the detail content, so
inline links and the "Abrir original" button share the same behavior.

## 2026-11-19 — Milestone 8: local data, offline policy, and accessibility verification

Four decisions were confirmed before M8 implementation.

### App data is never backed up

**Decision:** `android:allowBackup="false"`; the `fullBackupContent` and
`dataExtractionRules` attributes and their XML files are removed. Neither the
feeds/articles database nor any other app data leaves the device through
Android Auto Backup or device-to-device transfer.

**Alternatives considered:**

1. Keep `allowBackup="true"` with rules excluding only `filesDir` (the M6
   state) — gives automatic restore, but uploads the local-only feed and
   article database to the user's cloud account, contradicting the stated
   constraint that feeds and articles stay local.
2. Keep platform backup but add rules excluding the database as well — more
   configuration, same final behavior as disabling backup, with rules that
   must be kept in sync with the schema.

**Rationale:** "Keep feeds and articles local; no account or cloud sync" is a
product constraint. Since the app has no account or server-side restore,
backup offers no recovery path the user controls. This supersedes the M6
choice to exclude only the model from backup.

**Consequences:** App data is lost if the app is uninstalled or its data is
cleared. This is recorded in `LIMITATIONS.md`. The model was already excluded
from backup; that exclusion is now redundant and removed with the rules.

### Offline behavior relies on typed errors, not connectivity state

**Decision:** No `ACCESS_NETWORK_STATE` permission and no offline banner or
connectivity observer are added. Reading from stored data keeps working
offline; any operation that needs the network reports a typed, localized error
(`FeedFetchResult.Network`/`Timeout` mapped to `error_network`/`error_timeout`,
the partial-failure banner, and the feed-form submit error).

**Alternatives considered:**

1. Add a `ConnectivityManager` observer and a global offline state/banner —
   clearer proactive feedback, but a new permission, state plumbing in every
   screen, and more tests for a requirement already met reactively.
2. Auto-retry queued refreshes when connectivity returns — battery and
   complexity cost, and a background trigger is explicitly deferred.

**Rationale:** M8's acceptance criterion is that core flows work offline
except refresh, which the existing storage-backed reading path already
satisfies. The smallest change that verifies and documents this is preferred.

**Consequences:** There is no proactive "you are offline" message; the user
sees an error only when they attempt a network operation. Offline reading and
image behavior are documented in `LIMITATIONS.md`.

### Accessibility verification: fixes plus JVM Compose tests

**Decision:** Fix the concrete semantics gaps found in the M8 audit (named
loading indicators, refreshing state, read/unread state, share-menu expanded
state, feed-row button role) and add Robolectric-based Compose UI tests that
assert accessible names and key UI states. Compose test dependencies move to
`testImplementation` so the suite runs with `:app:testDebugUnitTest`.

**Alternatives considered:**

1. Fixes plus a documented manual/static audit only — smallest change, but no
   automated regression guard for semantics.
2. Instrumented `androidTest` Compose tests — closest to real TalkBack, but no
   device was available to the agent, so the suite could not be verified here.

**Rationale:** The roadmap's testing strategy calls for Compose/UI tests, and
"accessibility basics are checked" needs repeatable evidence. Robolectric
Compose tests provide deterministic coverage without hardware; manual device
checks are supplied by the user and recorded separately.

**Consequences:** New strings are added to `strings.xml` (pt-BR). The test rule
setup is validated with a smoke test before the full suite is written.

### Manual device checks

**Decision:** The user runs the M8 manual checklist (dark theme, font scaling,
TalkBack focus/announcements, airplane mode, network interruption, real feeds,
share targets) because no emulator or device is available to the agent.
Findings are fed back into fixes and `LIMITATIONS.md`.

**Rationale:** Several acceptance items (on-device responsiveness, real share
targets, TalkBack) cannot be proven by JVM tests.

**Consequences:** Any check the user reports as unrun or failing stays listed
as a limitation; nothing is claimed as verified without an actual result.

## 2026-11-19 — Feedbil launcher icon replaces the template placeholder

**Decision:** Replace the default Android Studio launcher icon with the
supplied Feedbil logo as a **mark-only adaptive icon**: a solid `#131313`
background and the white "f + RSS waves" mark scaled into the 66 dp
adaptive safe zone of the 108 dp canvas. The "Feedbil" wordmark is omitted
from the artwork. The same white-on-transparent PNG serves as the
foreground and the Android 13 themed (monochrome) layer. Density-specific
legacy fallback icons are regenerated from the same mark. The original
`logo.png` is not kept in the repository after generation.

**Alternatives considered:**

1. Put the full PNG (mark + wordmark) into the adaptive foreground. Faithful
   to the source artwork, but a launcher's circle/squircle mask clips the
   wordmark, and fitting the whole tile into the safe zone makes the mark
   small. The launcher already draws the app label below the icon.
2. Ship legacy PNG icons only, with no adaptive layers. Simplest, but
   ignores modern launcher shaping and the themed-icon API; with `minSdk`
   26 there is no device reason to drop adaptive icons.
3. Trace the raster mark into a vector drawable. Cleaner at any density,
   but the source is raster and tracing adds effort and fidelity risk for
   a one-off icon with no measurable benefit at the supported densities.

**Rationale:** The safe zone prevents clipping across launcher shapes, the
solid black background matches the supplied logo and the always-dark OLED
theme, and omitting the wordmark avoids duplicating the launcher label.
No new dependency, permission, or app-code change is needed. On Android 12+
the system splash screen reuses the launcher icon automatically, so no
separate splash asset is introduced.

**Consequences:** `mipmap-anydpi-v26` still supplies the adaptive icon for
both `ic_launcher` and `ic_launcher_round`; the template WebP fallbacks and
template vector foreground were removed. Visual rendering by a launcher
and the splash screen must be confirmed on a device by the user; the build,
resource packaging, and unit tests can only prove the resources are valid.

## 2026-11-19 — GitHub Releases distribution with per-commit builds

**Decision:** Publish a signed release APK to GitHub Releases on every push
to `main` using `.github/workflows/release.yml`. Each run creates a new
prerelease tagged `build-<run number>` with an attached
`feedbil-<run number>.apk`. CI injects `versionCode = <run number>` and
`versionName = 1.0.<run number>` through Gradle properties; local builds
keep `1` / `1.0`. Unit tests must pass before publishing. The signing
keystore (base64) and its passwords live in repository Actions secrets and
are decoded into `$RUNNER_TEMP` for the build; no key is committed. Runs
are queued rather than cancelled so every commit produces a release.

**Alternatives considered (cadence):**

1. One rolling "latest" release whose APK asset is replaced on every push
   — less noise, but the version never changes, so Android update checks
   and release-based updaters cannot detect a new build.
2. Tag-triggered releases only — deliberate, named versions, but does not
   satisfy "update on every commit".
3. New prerelease per push with the run number in the tag (chosen).

**Alternatives considered (signing key):**

1. Commit the keystore — simplest and reproducible for sideloading, but
   anyone could sign apps with the project's identity.
2. Generate a fresh key each run — no secrets, but users would have to
   uninstall before every update because the signature changes.
3. Actions secrets (chosen) — key stays out of git; requires an offline
   backup because losing it breaks the update path.

**Alternatives considered (versioning):**

1. Keep `1.0` / versionCode 1 fixed — no build-script change, but every
   build looks identical to Android.
2. Inject from the Actions run number (chosen) — monotonic versionCode
   with no source commit needed, so the workflow cannot create a commit
   loop.

**Rationale:** The app is local-only and has no Play presence, so a signed
APK attached to a GitHub Release is the smallest usable distribution path.
Automated tests gate publication because a failed build must not replace a
working download. Per-run versioning gives users a real update signal
without writing to the repository.

**Consequences:** Releases accumulate over time; pruning is not part of
this decision. The key is self-signed rather than Play-managed. If the
secrets are lost or rotated, users must uninstall before installing a
build with a different signature. Linux CI builds and end-to-end release
creation are unverified until the first workflow run completes.

## 2026-11-20 — Release keystore is PKCS12; CI derives the key password from the store password

**Decision:** Keep the existing release keystore in its generated format. It
starts with the ASN.1 `30 82` bytes, so it is PKCS12 despite the `.jks`
extension. PKCS12 encrypts the private key with the store password and
`keytool` ignores a separate `-keypass`. The workflow therefore passes
`secrets.RELEASE_STORE_PASSWORD` as both `FEEDBIL_RELEASE_STORE_PASSWORD`
and `FEEDBIL_RELEASE_KEY_PASSWORD`; `RELEASE_KEY_PASSWORD` is no longer
read. An inspection step runs `keytool -list` and `keytool -certreq` on the
decoded file before Gradle so a bad password or alias fails with a clear
message.

**Alternatives considered:**

1. Regenerate with `-storetype JKS` and distinct store/key passwords, then
   re-set all four secrets — cleaner separation, but it replaces a key that
   may already be distributed in signed builds and repeats the secret setup.
2. Keep a duplicate `RELEASE_KEY_PASSWORD` secret and align its value with
   the store password — no workflow change, but re-entering a write-only
   secret twice invited whitespace/typo mismatches, which happened twice.
3. Derive the key password from the store-password secret (chosen) — one
   known-good value, no duplicate to keep in sync, and an explicit
   diagnostic step if the assumption stops holding.

**Rationale:** A mismatched key password fails at `:app:packageRelease`
with `Given final block not properly padded`. This was reproduced locally
with a throwaway PKCS12 keystore: the distinct `-keypass` was ignored by
`keytool`, a different `FEEDBIL_RELEASE_KEY_PASSWORD` reproduced the exact
CI error, and setting it to the store password produced a verified signed
APK. Removing the duplicate secret eliminates that failure class.

**Consequences:** CI needs three secrets, not four; an existing
`RELEASE_KEY_PASSWORD` secret is ignored and can be deleted. If the keystore
is ever regenerated as JKS with a distinct key password, the workflow and
this decision must be revisited.

## 2026-11-20 — Keep Kotlin multifile part classes in the release build

**Decision:** Add
`-keep,allowshrinking,allowoptimization class kotlin.**Kt__*` to
`app/src/main/keepRules/rules.keep` and retain the template's
`optimization { enable = true; packageScope = ... }` configuration.
Release R8 optimization stays enabled.

**Context:** The signed GitHub Release APK crashed on launch, while the
Android Studio debug install worked. The retraced stack trace was
`java.lang.IllegalAccessError: Class kotlin.text.m extended by class lj2 is inaccessible`
at `Room.databaseBuilder` in `AppContainer`. The release mapping showed that
R8 had repackaged `kotlin.text.StringsKt__StringNumberConversionsJVMKt` into
the unnamed package (`lj2`) while its package-private superclass
`StringsKt__IndentKt` stayed in `kotlin.text` (`kotlin.text.m`). Kotlin
multifile part classes form inheritance chains, so moving only one of them
breaks verification. Debug has optimization disabled and was unaffected.

**Alternatives considered:**

1. Targeted keep rule (chosen) — pins the names and packages of all Kotlin
   multifile parts while still allowing shrinking and optimization. This is
   a known, effectively free workaround for the same R8 failure seen in
   other apps.
2. Add `-dontrepackage` globally — fixes the whole class of cross-package
   access errors, but slightly increases DEX size because repackaging is
   skipped for every optimized class.
3. Remove `packageScope` — Google recommends not using it once an app
   already enables R8, but doing so brings app and LiteRT code under full
   R8. The LiteRT AAR ships no consumer keep rules, so this risks a new
   native/reflection regression that needs its own device verification.
4. Disable release optimization — guarantees parity with the working debug
   build, but gives up all code and resource shrinking.

**Rationale:** The targeted rule addresses exactly the observed failure
with the smallest change and no new risk to the on-device AI path. The
repackaged class was unused and is removed by shrinking; the part classes
that remain keep their original names and packages, so the inheritance
chain is consistent again.

**Consequences:** Keep rules live in `src/main/keepRules/*.keep` under the
new AGP 9.3+ optimization DSL. A release rebuild succeeds and all 196 JVM
tests pass; the final proof is launching the next signed CI release APK on
a physical device. If a similar `IllegalAccessError` appears for another
package-private chain, switch to `-dontrepackage` (option 2).
