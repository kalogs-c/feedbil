# RSS Reader MVP --- Development Roadmap

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

Do not implement unless later approved: - User accounts, cloud sync, or
remote backend. - Full-text extraction from publisher websites. -
Recommendations, ranking, social features, or discovery. - Multiple AI
models or model downloads/management UI beyond what is necessary for the
chosen MVP model. - OPML import/export, folders, tags, search, widgets,
notifications, or tablet-specific layouts. - Complex offline media
handling.

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

## 2. Decision gates --- resolve before implementation

The following are open decisions, not fixed technical requirements.
Discuss them with the user at the relevant milestone and record the
approved choice in `DECISIONS.md`.

-------------------------------------------------------------------------------

  Decision                Options to discuss Trade-offs to explain

  ----------------------- ----------------------- -------------------------------

  Minimum Android version Choose an appropriate Device reach vs API
                          `minSdk`                availability and maintenance

  Project structure Minimal single-module Simplicity now vs
                          app vs multiple modules isolation/build complexity

  App architecture        Simple Compose state + Learning curve, testability,
                          ViewModel; layered coupling, future growth
                          architecture; other
                          options

  Dependency injection    Manual construction;    Boilerplate vs complexity and
                          lightweight DI; test setup
                          framework

  Local persistence Room; another local Querying/relations/migrations
                          database; simpler vs setup and maintenance
                          storage if justified

  RSS parsing Existing maintained Format compatibility and
                          parser; focused custom maintenance vs
                          parser dependencies/control

  Feed refresh            Manual refresh only;    MVP simplicity vs freshness,
                          background refresh; battery, and Android background
                          both                    limits

  Network stack Choose an HTTP client Dependency size, API
                          approach                ergonomics, cancellation,
                                                  testability

  Article reading Render feed-provided    Fidelity vs security,
                          content; sanitized accessibility, and
                          HTML; other supported implementation effort
                          format

  AI model/runtime        LiteRT model and        Model quality, APK/model size,
                          supported task/runtime latency, memory, device
                          path                    compatibility, licensing

  Model delivery Bundled model; Offline-first behavior vs app
                          app-managed download; size, storage, and update
                          other option            mechanism

  AI output contract Summary length, Predictability vs flexibility
                          language, structure,    and inference cost
                          and fallback behavior

  Sharing Android Sharesheet with Compatibility and
                          plain text; other implementation complexity
                          formats

  UI/navigation Minimal Compose Simplicity vs future navigation
                          navigation approach growth

  Theme System-following with User preference, OLED goals,
                          dark option;            and accessibility
                          always-dark; other

  Testing Unit, instrumented, UI, Confidence vs development time
                          and integration test    and setup
                          scope

## Background work No automatic work in    Predictability and battery vs MVP; scheduled work if freshness and platform required                constraints

Do not decide all items up front. Resolve only what is needed for the
next milestone.

## 3. Roadmap

### Milestone 1 --- Project foundation

**Work** - Create the Android project and verify it builds and runs on
an emulator/device. - Establish package naming, code style, and basic
project documentation. - Add a minimal Compose app shell. - Add app
name, launcher icon placeholder, and string resources. - Establish a
basic theme with readable typography, spacing, and dark/OLED-friendly
colors. - Add a simple navigation structure only if required by the
approved screen flow.

**Acceptance criteria** - App launches without errors. - A basic screen
renders in light/dark modes as agreed. - UI text is sourced from string
resources. - Build and test commands are documented.

**Decision gate** - Resolve minimum Android version, project structure,
architecture approach, theme behavior, and initial dependencies.

### Milestone 2 --- Domain model and local storage

**Work** - Define the minimum concepts: feed and article. - Define
stable identifiers and relationships. - Store feed name, feed URL,
article metadata, article content supplied by RSS, publication date, and
feed association as appropriate. - Implement retention rules: remove
articles older than 7 days and enforce a maximum of 100 retained
articles per feed. - Ensure retention runs after feed updates and when
needed on app startup. - Handle missing or invalid publication dates
explicitly; agree on a deterministic policy rather than guessing. - Add
database migrations strategy appropriate to the chosen storage.

**Acceptance criteria** - Feeds and articles survive app restarts. -
Articles are associated with the correct feed. - Retention is enforced
per feed, not globally. - Duplicate feed entries do not create duplicate
articles. - Storage behavior is covered by tests.

**Decision gate** - Resolve persistence technology, entity shape,
duplicate detection strategy, and exact policy for missing dates and the
100-article ordering rule.

### Milestone 3 --- Feed management

**Work** - Build a feed list screen. - Add a feed flow with name and URL
fields. - Validate required fields and URL format. - Fetch the feed to
validate that it is reachable and parseable before saving, or agree on
another clear failure flow. - Support editing and deleting feeds. - Show
loading, empty, and error states. - Avoid blocking the UI during network
requests.

**Acceptance criteria** - User can add, edit, and remove feeds. -
Invalid URLs and unsupported/invalid feeds produce understandable
errors. - Network failures do not crash the app. - Feed list updates
after changes.

**Decision gate** - Resolve RSS parser, HTTP client, validation
behavior, and whether refresh is manual-only for the MVP.

### Milestone 4 --- Feed refresh and article list

**Work** - Fetch and parse each saved feed. - Map available RSS fields
into the article model without inventing missing data. - Handle common
RSS variations and malformed entries gracefully. - Merge new articles
with stored articles using the approved duplicate strategy. - Apply
7-day and 100-article retention rules. - Display articles in a readable
list, including available title, source/feed, and date. - Add
pull-to-refresh or an explicit refresh action. - Define behavior for
feeds that fail while other feeds succeed.

**Acceptance criteria** - Refresh retrieves new entries and preserves
existing retained entries. - A failed feed does not prevent successful
feeds from updating. - Duplicate entries are not repeatedly inserted. -
Loading, empty, partial-failure, and error states are clear. - Retention
limits remain true after refresh.

**Decision gate** - Resolve refresh orchestration, concurrency,
timeout/retry policy, article sorting, and missing-field presentation.

### Milestone 5 --- Article reading experience

**Work** - Build an article detail screen. - Display only content
available from the RSS feed for the MVP. - Support the content formats
actually encountered in chosen test feeds. - Make long text comfortable
to read: line length, typography, spacing, scrolling, and contrast. -
Handle absent descriptions/content and invalid markup. - Provide a clear
way to return to the article list.

**Acceptance criteria** - Article content is readable on small screens
and with larger system font settings. - Malformed or absent content does
not crash the screen. - External links, if supported, behave safely and
predictably. - The app does not imply it has fetched the publisher's
full article when it only has RSS content.

**Decision gate** - Resolve HTML handling, sanitization, link behavior,
image handling, and whether the article opens in an in-app or external
browser.

### Milestone 6 --- On-device AI summary

**Work** - Select and validate a LiteRT-compatible model suitable for
short article summaries. - Confirm model license, redistribution terms,
supported Android devices, memory requirements, and model size. -
Implement a small AI interface so UI code does not depend directly on
runtime details. - Add the top-of-article AI button. - Run inference
locally using the article content available in the app. - Show progress,
generated summary, and understandable error states. - Handle empty/short
content, overly long input, inference failure, and unavailable model. -
Keep inference off the main thread. - Do not transmit article text to a
remote service.

**Acceptance criteria** - Summary generation works offline on a
supported device after the model is available. - UI remains responsive
during inference. - Failure does not block normal reading. - Output is
presented as generated content, not as verified fact. - Model/runtime
behavior and device requirements are documented.

**Decision gate** - Resolve model, model delivery, LiteRT API/runtime
integration, input limits/truncation strategy, prompt or task format,
output language, and summary length.

### Milestone 7 --- Sharing

**Work** - Add a share action at the top of the article screen. -
Present two explicit actions: 1. **Share with AI:** generate a concise
summary focused on the article's main point and append the original
source URL. 2. **Normal share:** share the article title and original
source URL. - Use the Android sharing mechanism agreed for the MVP. -
Define behavior when AI is unavailable or generation fails. - Ensure the
source URL is included and distinguish generated summary text from
source material.

**Acceptance criteria** - Both share options are understandable and work
with common Android share targets. - Normal share does not invoke AI. -
AI share includes a short summary and source URL. - AI failure provides
a useful fallback or error path without losing the article. - No share
action silently sends content to a remote AI service.

**Decision gate** - Resolve exact share text format, AI reuse/caching
behavior, fallback behavior, and Android Sharesheet integration.

### Milestone 8 --- Quality, accessibility, and MVP hardening

**Work** - Review navigation, loading, empty, error, and offline
states. - Verify dark theme and OLED-friendly surfaces. - Check
contrast, touch target sizes, screen reader labels, focus order, and
font scaling. - Test on at least one emulator and one physical device if
available. - Test feeds with different RSS formats, missing fields,
malformed content, duplicate entries, and stale articles. - Test
airplane mode and network interruptions. - Check database migrations and
retention edge cases. - Review app permissions and remove unnecessary
permissions. - Check startup, refresh, scrolling, and AI inference
responsiveness. - Document known limitations.

**Acceptance criteria** - Core flows work without network access after
data/model availability, except feed refresh. - No known crash in core
flows. - Accessibility basics are checked. - Retention behavior is
verified with automated tests. - MVP limitations and device requirements
are documented.

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
- No assumption that every feed provides every field.

### Retention

- Maximum age: 7 days.
- Maximum count: 100 articles per feed.
- Enforce both rules consistently.
- Define what happens when publication dates are absent or unreliable.
- Define whether the 100 newest articles are selected by publication
  date or feed order.
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

- `README.md` --- project purpose, setup, build/test commands, MVP
  scope.
- `ROADMAP.md` --- this roadmap; update status as milestones complete.
- `DECISIONS.md` --- approved decisions, date, alternatives
  considered, rationale.
- `LIMITATIONS.md` --- known MVP limitations and device/model
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
