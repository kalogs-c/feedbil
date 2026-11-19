# RSS Reader MVP — Development Roadmap

## 0. Product definition

Build a local-first Android RSS reader with Kotlin and Jetpack Compose.
Keep the MVP small, readable, testable, and structured so future
features can be added without premature abstraction.

### MVP scope

- Add, edit, and remove feeds using a user-defined name and RSS URL.
- Fetch and parse RSS feeds.
- Display articles using fields provided by the feed.
- Open and read an article in the app.
- Generate an article summary locally using LiteRT, triggered by an AI
  button at the top of the article screen.
- Share an article in two ways:
  - **Share with AI:** generate a short, factual summary of the
    article and include its source URL.
  - **Normal share:** share the article title and source URL.
- Retain only articles published within the last 7 days, with a
  maximum of 100 articles per feed.
- Remember when the user last opened an article; when the 100-article
  cap is applied, unread articles are kept in preference to read ones.
- Store app data locally. No account or app-owned backend is required
  for the MVP.

### Product and UX constraints

- Simple, clean reading experience; prioritize legibility and
  accessibility.
- OLED-friendly dark theme; avoid unnecessary bright surfaces.
- Main app language: Brazilian Portuguese (`pt-BR`).
- Source code, identifiers, comments, tests, and developer
  documentation: English.
- Prepare localization infrastructure for future languages, without
  building a translation system prematurely.
- AI inference must run on-device. Do not silently send article
  content to a remote service.

### Explicitly out of scope for MVP

Do not implement unless later approved:

- User accounts, cloud sync, or remote backend.
- Full-text extraction from publisher websites.
- Recommendations, ranking, social features, or discovery.
- Multiple AI models or model downloads/management UI beyond what is
  necessary for the chosen MVP model.
- OPML import/export, folders, tags, search, widgets, notifications,
  or tablet-specific layouts.
- Complex offline media handling.

## 1. Working agreement for AI-assisted development

The AI coding agent should act as an implementation partner and Kotlin
tutor.

1. Work in small, reviewable milestones. Do not generate the entire app
   at once.
2. Before making a material technical decision, present:
   - The decision to make.
   - Viable options.
   - Main trade-offs (complexity, maintainability, performance,
     dependencies, testing, and future flexibility).
   - A recommendation, clearly marked as a recommendation rather than
     an assumed decision.
3. Wait for approval before committing to a material decision. Do not
   treat silence as approval.
4. Do not introduce libraries, architecture layers, abstractions, or
   design patterns without explaining why they are useful now.
5. Prefer idiomatic, straightforward Kotlin and Compose. Optimize for
   clarity over cleverness.
6. Explain unfamiliar Kotlin/Android concepts briefly when they appear.
   Avoid turning every change into a tutorial.
7. Keep changes focused. After each milestone, summarize files changed,
   behavior added, tests run, and remaining risks.
8. Do not silently expand MVP scope. Record proposed additions
   separately.
9. Keep all user-facing strings out of hard-coded UI code; use Android
   string resources from the beginning.
10. Never fabricate feed data, article content, model output, or test
    results.

The repository is under version control (git). Keep work in small,
reviewable commits — one milestone or sub-task per commit where
practical — so changes stay easy to review and revert.

## 2. Decision gates — resolve before implementation

The following are open decisions, not fixed technical requirements.
Discuss them with the user at the relevant milestone and record the
approved choice in `DECISIONS.md`.

| Decision | Options to discuss | Trade-offs to explain |
| --- | --- | --- |
| Minimum Android version | Choose an appropriate `minSdk` | Device reach vs API availability and maintenance |
| Project structure | Minimal single-module app vs multiple modules | Simplicity now vs isolation/build complexity |
| App architecture | Simple Compose state + ViewModel; layered architecture; other options | Learning curve, testability, coupling, future growth |
| Dependency injection | Manual construction; lightweight DI; framework | Boilerplate vs complexity and test setup |
| Local persistence | Room; another local database; simpler storage if justified | Querying/relations/migrations vs setup and maintenance |
| RSS parsing | Existing maintained parser; focused custom parser | Format compatibility and maintenance vs dependencies/control |
| Feed refresh | Manual refresh only; background refresh; both | MVP simplicity vs freshness, battery, and Android background limits |
| Network stack | Choose an HTTP client approach | Dependency size, API ergonomics, cancellation, testability |
| Article reading | Render feed-provided content; sanitized HTML; other supported format | Fidelity vs security, accessibility, and implementation effort |
| AI model/runtime | LiteRT model and supported task/runtime path | Model quality, APK/model size, latency, memory, device compatibility, licensing |
| Model delivery | Bundled model; app-managed download; other option | Offline-first behavior vs app size, storage, and update mechanism |
| AI output contract | Summary length, language, structure, and fallback behavior | Predictability vs flexibility and inference cost |
| Sharing | Android Sharesheet with plain text; other formats | Compatibility and implementation complexity |
| UI/navigation | Minimal Compose navigation approach | Simplicity vs future navigation growth |
| Theme | System-following with dark option; always-dark; other | User preference, OLED goals, and accessibility |
| Background work | No automatic work; manual refresh only; WorkManager-based background refresh | Predictability and battery vs freshness and platform constraints |
| Testing | Unit, instrumented, UI, and integration test scope | Confidence vs development time and setup |

Note: the toolchain baseline is confirmed and recorded in
`DECISIONS.md` (2026-11-18 entries): `minSdk = 26` (raised from the
template's 24 for native `java.time`), single `:app` module, AGP 9.5.1
with built-in Kotlin, `compileSdk`/`targetSdk` 37, Java 11, Gradle 9.6,
configuration cache, `kotlin.code.style=official`. Light layered
architecture (Compose UI + ViewModel + repositories) with manual
dependency construction was also approved.

Milestone 2 resolved local persistence, entity shape, duplicate
detection, the missing-date policy, and read tracking; see the
2026-11-18 entries in `DECISIONS.md`.

Do not decide all items up front. Resolve only what is needed for the
next milestone.

## 3. Roadmap

### Milestone 1 — Project foundation

**Status:** Completed (2026-11-18). The project lived on the WSL
filesystem at inception; on 2026-11-18 it moved to `C:\feedbil` on
Windows, and the Windows-side build, unit tests, and on-device launch
were verified there. The toolchain baseline discussion concluded the
same day (`DECISIONS.md`).

**Work**

- [x] Create the Android project and verify it builds and unit tests
  pass (verified on Linux 2026-11-18, pre-move; re-verified on Windows
  2026-11-18).
- [x] Establish package naming (`com.kalogsc.feedbil`) and code style
  (`kotlin.code.style=official`).
- [x] Add a minimal Compose app shell (`MainActivity` + `FeedbilTheme`).
- [x] Add app name, launcher icon placeholder (default template icons,
  intentionally kept), and string resources (pt-BR).
- [x] Establish an OLED-friendly always-dark theme (decision recorded
  in `DECISIONS.md`).
- [x] Basic project documentation: `README.md` with setup and
  build/test commands, `DECISIONS.md`, `LIMITATIONS.md`.
- [x] Initialize git and keep changes in reviewable commits.
- [x] Verify the app launches on an emulator/device (verified on a
  physical device, SM-S928B running Android 17, on 2026-11-18 on the
  Windows side).
- [x] Discuss and confirm the template-inherited toolchain baseline
  (resolved 2026-11-18; recorded in `DECISIONS.md` — `minSdk` raised
  to 26, everything else confirmed as-is).
- [x] Add a simple navigation structure only if required by the
  approved screen flow (not required; deferred to Milestones 3/4 —
  approved 2026-11-18).

**Acceptance criteria**

- App compiles and unit tests pass (verified 2026-11-18 on Linux,
  pre-move, and re-verified on Windows).
- A basic screen renders in the agreed always-dark OLED theme; launch
  verified on a physical device (SM-S928B, Android 17) on 2026-11-18.
- UI text is sourced from string resources (no hard-coded user-facing
  strings).
- Build and test commands are documented in `README.md`.
- Approved decisions are recorded in `DECISIONS.md`.

**Decision gate** — Resolve minimum Android version, project structure,
architecture approach, theme behavior, and initial dependencies.
**Resolved 2026-11-18** (see `DECISIONS.md`): theme behavior is
always-dark OLED; `minSdk = 26` with the rest of the template toolchain
confirmed as-is; single `:app` module; light layered architecture
(Compose UI + ViewModel + repositories); manual dependency
construction; no new dependencies beyond the template set.

### Milestone 2 — Domain model and local storage

**Status:** Completed (2026-11-18). Storage is implemented and covered by
JVM tests; no screen uses it yet, so the user-visible proof of
persistence arrives with Milestone 3.

**Work**

- [x] Define the minimum concepts: feed and article
  (`domain/model/Feed.kt`, `domain/model/Article.kt`).
- [x] Define stable identifiers and relationships: `Long`
  autoincrement keys; articles reference their feed and are deleted with
  it.
- [x] Store feed name, feed URL, article metadata, article content
  supplied by RSS, publication date, and feed association as
  appropriate (`data/local/entity/Entities.kt`).
- [x] Implement retention rules: remove articles older than 7 days and
  enforce a maximum of 100 retained articles per feed, keeping unread
  articles in preference to read ones.
- [x] Ensure retention runs after feed updates and when needed on app
  startup (`ArticleRepository.store`, `ArticleRepository.pruneAll`, and
  `FeedbilApplication`).
- [x] Handle missing or invalid publication dates explicitly: the stored
  date falls back to the observation time, recorded in `DECISIONS.md`.
- [x] Add a database migration strategy appropriate to the chosen
  storage: exported schemas under `app/schemas`, hand-written migrations,
  no destructive fallback.
- [x] Add read tracking (`lastReadAt`) as an approved scope addition.

**Acceptance criteria**

- Feeds and articles survive app restarts (verified by reopening a
  file-backed database in `DatabasePersistenceTest`).
- Articles are associated with the correct feed (verified in
  `ArticleStorageTest`).
- Retention is enforced per feed, not globally (verified in
  `ArticleStorageTest` and `ArticleRepositoryTest`).
- Duplicate feed entries do not create duplicate articles (verified by
  unique `(feedId, link)` and `(feedId, guid)` indexes).
- Storage behavior is covered by tests: 33 JVM tests pass on Windows
  (2026-11-18).

**Decision gate** — Resolved 2026-11-18 (`DECISIONS.md`): Room 2.8.5 with
KSP; `Long` identities and two unique indexes for duplicate detection;
publication date falling back to observation time; unread-first
retention; separate domain models; exported schemas without destructive
fallback; Robolectric-based storage tests.

### Milestone 3 — Feed management

**Status:** Completed 2026-11-19.

**Work**

- [x] Build a feed list screen.
- [x] Add a feed flow with name and URL fields.
- [x] Validate required fields and URL format.
- [x] Fetch the feed to validate that it is reachable and parseable
      before saving, or agree on another clear failure flow.
- [x] Support editing and deleting feeds.
- [x] Show loading, empty, and error states.
- [x] Avoid blocking the UI during network requests.

**Acceptance criteria**

- [x] User can add, edit, and remove feeds (verified in
      `FeedFormViewModelTest` and `FeedListViewModelTest`).
- [x] Invalid URLs and unsupported/invalid feeds produce understandable
      errors: name/URL rules in `FeedInputValidator`, HTTP status,
      not-a-feed, timeout, and network errors in `FeedFetcher`.
- [x] Network failures do not crash the app (every failure maps to a
      `FeedFetchResult`; verified in `FeedFetcherTest`).
- [x] Feed list updates after changes (the list observes Room; verified
      in `FeedListViewModelTest`).
- [x] Deleting a feed asks for confirmation because its articles are
      removed with it.
- [x] 68 JVM tests pass on Windows (2026-11-19).

**Decision gate** — Resolved 2026-11-19 (`DECISIONS.md`): rssparser for
RSS/Atom parsing, a shared OkHttp client, strict add-feed validation
(fetch and parse before saving), typed fetch errors, Navigation Compose
with string routes, cleartext `http://` feeds allowed, unique feed URLs
enforced by the storage index, and manual refresh only for the MVP.
Refresh is revised in Milestone 4: automatic once at app launch plus a
manual action, still no background work.

### Milestone 4 — Feed refresh and article list

**Status:** Completed 2026-11-19.

**Work**

- [x] Fetch and parse each saved feed through `FeedRefreshService`.
- [x] Map available RSS/Atom fields into `Article` without inventing missing
      data (`FeedArticleMapper`, `FeedDateParser`).
- [x] Handle common RSS variations and malformed entries gracefully: skip
      linkless entries, trim blanks to `null`, parse RFC 1123/822 and ISO dates,
      and map whole-feed failures to typed results.
- [x] Merge new articles with stored articles using the approved duplicate
      strategy (`OnConflictStrategy.IGNORE` in storage).
- [x] Apply 7-day and 100-article retention rules on every refresh.
- [x] Display articles in a readable combined list with title, feed name, date,
      and unread indicator.
- [x] Reflect read/unread state in the article list.
- [x] Add an explicit refresh action; also refresh automatically once on app
      launch (see the decision gate).
- [x] Define partial-failure behavior: failed feeds are listed in a dismissible
      banner with details and retry; successful feeds still update.

**Acceptance criteria**

- Refresh retrieves new entries and preserves existing retained entries
  (verified in `FeedRefreshServiceTest`).
- A failed feed does not prevent successful feeds from updating (verified in
  `FeedRefreshServiceTest`).
- Duplicate entries are not repeatedly inserted (verified in
  `FeedRefreshServiceTest` and storage tests).
- Loading, empty, partial-failure, and error states are represented in
  `ArticleListUiState` and covered by `ArticleListViewModelTest`.
- Retention limits remain true after refresh (verified in
  `FeedRefreshServiceTest`).
- The v1→v2 migration preserves data and adds a nullable fetch time (verified
  in `DatabaseMigrationTest`).
- 102 JVM tests pass on Windows (2026-11-19).

**Decision gate** — Resolved 2026-11-19 (`DECISIONS.md`): articles-first home
with feed management behind a top-bar action; automatic refresh once per app
process at launch plus a manual refresh action (no timer, no background work);
persisted `lastFetchedAt` with a hand-written v1→v2 migration; initial articles
stored when a feed is added or its URL changes; bounded concurrency of four,
no automatic retries; dismissible partial-failure banner; pt-BR absolute
short dates in the list. The M3 "manual refresh only for the MVP" decision is
superseded in part by the launch refresh.

### Milestone 5 — Article reading experience

**Status:** Completed 2026-11-19.

**Work**

- [x] Build an article detail screen with title, metadata, content and a
      "Conteúdo fornecido pelo feed" caption.
- [x] Display only content available from the RSS feed for the MVP,
      preferring `contentHtml` over `description`.
- [x] Render common HTML formatting, safe links and inline `<img>` images
      through a pure-Kotlin sanitizer plus Compose and Coil.
- [x] Make long text comfortable to read: capped line length, `sp` typography,
      spacing, vertical scrolling and OLED dark contrast.
- [x] Handle absent descriptions/content and malformed markup without
      crashing; drop scripts, styles, iframes and unsafe URL schemes.
- [x] Record when an article is opened so the list can reflect read state.
- [x] Provide a back arrow and normal system back to return to the list.

**Acceptance criteria**

- [x] Article content is readable on small screens and with larger system
      font settings (scalable `sp` typography, no fixed text height).
- [x] Malformed or absent content does not crash the screen.
- [x] External links open in the external browser after `http(s)` validation;
      unsafe URLs become plain text.
- [x] The "Conteúdo fornecido pelo feed" caption and "Abrir original" action
      distinguish RSS content from the publisher's full article.
- [x] 124 JVM tests pass on Windows (2026-11-19), including the review
      follow-up regression cases.

**Decision gate** — Resolved 2026-11-19 (`DECISIONS.md`): Compose +
sanitized rich text with Coil images; images loaded on demand from inline
`<img src>` only (no local image storage, no schema change); external browser
for all links; images are a required feature and are no longer stripped.
`RssItem.image`/enclosure images and offline image storage remain deferred.

### Milestone 6 — On-device AI summary

**Status:** Implemented 2026-11-19; review fixes applied the same day. All 157
JVM tests pass and debug/release APKs assemble. On-device inference with the
real 2.59 GB model has **not** been run yet; that check is pending and is not
claimed as passed.

**Work**

- [x] Select and validate a LiteRT-compatible model suitable for short
  article summaries (Gemma 4 E2B IT int4, ~2.59 GB, Apache-2.0, public).
- [x] Confirm model license, redistribution terms, supported Android
  devices, memory requirements, and model size (see `DECISIONS.md` and
  `LIMITATIONS.md`; runtime v0.16.1 documents the model).
- [x] Implement a small AI interface (`ArticleSummarizer`) so UI code does
  not depend directly on runtime details.
- [x] Add the top-of-article AI button.
- [x] Run inference locally using the article content available in the app.
- [x] Show progress, generated summary, and understandable error states.
- [x] Handle empty/short content, overly long input, inference failure, and
  unavailable model.
- [x] Keep inference off the main thread (background dispatcher + mutex).
- [x] Do not transmit article text to a remote service. The only network
  request is the one-time model file download.

**Acceptance criteria**

- [x] Summary generation is fully offline once the model file is available
  (code path verified by unit tests; real-device check pending).
- [x] UI remains responsive during inference (native work off the main
  thread; modeled in ViewModel tests).
- [x] Failure does not block normal reading (covered by tests).
- [x] Output is presented as generated content, not as verified fact
  ("Resumo gerado por IA" label; prompt forbids fabrication).
- [x] Model/runtime behavior and device requirements are documented.
- [x] 157 JVM tests pass on Windows (2026-11-19).

**Decision gate** — Resolved 2026-11-19 (`DECISIONS.md`): LiteRT-LM Kotlin API
pinned to 0.16.1 for Kotlin 2.2 compatibility; fixed public Gemma 4 E2B model
with anonymous in-app download into `filesDir` and size validation; CPU
backend; entire article text as input with no truncation for now; pt-BR output
of ~2–3 sentences; untrusted article text may not issue instructions. A model
picker is deferred, and summary reuse is left for Milestone 7.

### Milestone 7 — Sharing

**Status:** Not started.

**Work**

- Add a share action at the top of the article screen.
- Present two explicit actions:
  1. **Share with AI:** generate a concise summary focused on the
     article's main point and append the original source URL.
  2. **Normal share:** share the article title and original source URL.
- Use the Android sharing mechanism agreed for the MVP.
- Define behavior when AI is unavailable or generation fails.
- Ensure the source URL is included and distinguish generated summary
  text from source material.

**Acceptance criteria**

- Both share options are understandable and work with common Android
  share targets.
- Normal share does not invoke AI.
- AI share includes a short summary and source URL.
- AI failure provides a useful fallback or error path without losing
  the article.
- No share action silently sends content to a remote AI service.

**Decision gate** — Resolve exact share text format, AI reuse/caching
behavior, fallback behavior, and Android Sharesheet integration.

### Milestone 8 — Quality, accessibility, and MVP hardening

**Status:** Not started.

**Work**

- Review navigation, loading, empty, error, and offline states.
- Verify dark theme and OLED-friendly surfaces.
- Check contrast, touch target sizes, screen reader labels, focus
  order, and font scaling.
- Test on at least one emulator and one physical device if available.
- Test feeds with different RSS formats, missing fields, malformed
  content, duplicate entries, and stale articles.
- Test airplane mode and network interruptions.
- Check database migrations and retention edge cases.
- Review app permissions and remove unnecessary permissions.
- Check startup, refresh, scrolling, and AI inference responsiveness.
- Document known limitations.

**Acceptance criteria**

- Core flows work without network access after data/model availability,
  except feed refresh.
- No known crash in core flows.
- Accessibility basics are checked.
- Retention behavior is verified with automated tests.
- MVP limitations and device requirements are documented.

## 4. Suggested screen map

Keep the initial navigation small:

- **Feeds / Articles:** list of feeds and/or a combined article list.
  Decide whether the home screen is feed-first or all-articles-first.
- **Add/Edit Feed:** name and URL form.
- **Article Detail:** feed-provided article content; AI summary
  action; share action.
- **Share choices:** two actions, AI and normal. Prefer a simple
  native or Compose choice surface appropriate to the chosen
  interaction.

Do not add separate screens unless the interaction requires them.

## 5. Core data and behavior requirements

These are product requirements, not prescribed implementation details.

### Feed

- User-provided name.
- RSS URL.
- Stable local identity.
- Optional metadata only when useful and available.

### Article

- Stable local identity.
- Associated feed.
- Original article URL.
- Available title, description/content, publication date, and other
  useful RSS fields.
- Read state: when the user last opened the article.
- No assumption that every feed provides every field.

### Retention

- Maximum age: 7 days.
- Maximum count: 100 articles per feed.
- Enforce both rules consistently.
- When a publication date is absent or unparseable, the observation
  time is used.
- Unread articles are kept in preference to read ones within the
  count limit.
- Treat retention as a storage rule, not just a UI filter.

### AI

- All inference runs on-device.
- Use only content stored from the RSS feed.
- AI summary and AI share are distinct user actions, even if
  implementation can reuse a previously generated summary.
- Preserve the original source URL in AI share output.
- Handle model unavailability and inference errors without breaking
  reading or normal sharing.

## 6. Testing strategy

Add tests alongside each feature rather than postponing all tests until
the end.

- **Unit tests:** retention policy, duplicate detection, RSS-to-domain
  mapping, validation, AI input preparation, share text formatting.
- **Persistence tests:** feed/article writes, relations, deletion
  behavior, migrations, retention queries.
- **Integration tests:** feed fetching and parsing using local fixture
  files or controlled test responses.
- **Compose/UI tests:** add feed, display articles, open article, AI
  loading/error/success states, share choices.
- **Device checks:** offline behavior, font scaling, TalkBack basics,
  dark theme, AI memory/performance.

Avoid tests that depend on live third-party feeds for deterministic CI.

## 7. Repository documentation

Keep documentation short and current:

- `README.md` — project purpose, setup, build/test commands, MVP
  scope.
- `AGENTS.md` — working agreement for AI-assisted development (agent
  instructions); keep separate from `README.md`.
- `ROADMAP.md` — this roadmap; update status as milestones complete.
- `DECISIONS.md` — approved decisions, date, alternatives
  considered, rationale. Includes a clearly marked pending-discussion
  section for decisions inherited from the project template.
- `LIMITATIONS.md` — known MVP limitations and device/model
  requirements.

Do not create documentation files that duplicate one another.

## 8. Definition of MVP complete

The MVP is complete when a user can:

1. Add an RSS feed using a name and URL.
2. Refresh feeds and see available articles.
3. Read the content provided by the RSS feed.
4. Generate an article summary locally with LiteRT.
5. Share either a concise AI summary plus source URL, or title plus
   source URL.
6. Restart the app and retain local feeds and articles.
7. Use the app with readable dark mode and basic accessibility support.
8. Rely on the 7-day / 100-articles-per-feed retention rule.

Everything else should be treated as a post-MVP proposal.
