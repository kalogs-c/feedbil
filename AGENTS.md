# AGENTS.md

## Project

Build a local-first Android RSS reader using Kotlin and Jetpack Compose.
The MVP is defined in `ROADMAP.md`; read it before implementing
features.

The app's UI language is Brazilian Portuguese (`pt-BR`). Source code,
identifiers, comments, tests, and developer documentation must be in
English.

## How to work

-   Inspect the existing project and follow its established conventions
    before changing files.
-   Implement one small, verifiable task at a time. Keep the app
    buildable and runnable after each change.
-   Do not implement the entire roadmap in one pass. Follow its
    milestones and acceptance criteria.
-   Explain unfamiliar Kotlin or Android concepts briefly when they are
    relevant to the current change.
-   Prefer clear, idiomatic Kotlin over clever code or unnecessary
    abstraction.
-   Avoid premature modularization, generic frameworks, and dependencies
    without a concrete current need.
-   Do not silently expand the MVP. Put optional ideas aside and ask
    before implementing them.
-   Do not overwrite existing work or remove behavior without checking
    its purpose.

## Technical decisions

Do not assume technical choices that `ROADMAP.md` leaves open.

Before making a significant decision (architecture, libraries,
persistence, networking, RSS parsing, navigation, AI model/runtime,
model delivery, or background work):

1.  Explain the decision that is needed.
2.  Present reasonable options and their trade-offs.
3.  Give a recommendation, clearly labeled as a recommendation.
4.  Wait for the user's approval before proceeding.

Resolve decisions just in time; do not force the user to decide the
entire stack upfront. Record approved decisions and their rationale in
`DECISIONS.md`.

## Product constraints

-   Keep feeds and articles local. The MVP requires no account, cloud
    sync, or app backend.
-   Support adding, editing, and removing feeds by name and RSS URL.
-   Display and read the content supplied by the feed. Do not imply that
    full publisher-page content has been fetched.
-   Retain articles from the last 7 days, with a maximum of 100 articles
    per feed. Enforce this in storage, not only in the UI.
-   Handle missing or malformed feed fields safely; never invent article
    data.
-   AI inference must run on-device using the approved LiteRT approach.
    Never send article content to a remote AI service.
-   AI summaries must preserve the source's meaning and must be
    presented as generated content, not verified fact.
-   AI sharing includes a concise summary and the original source URL.
    Normal sharing includes the article title and source URL.
-   Keep the reading experience simple, legible, accessible, and
    OLED-friendly.
-   Do not hard-code user-facing strings in UI code. Use Android string
    resources from the start to support future localization.

## Code quality and safety

-   Keep UI, domain behavior, persistence, networking, and AI
    responsibilities understandable and appropriately separated for the
    chosen architecture.
-   Keep expensive work and network operations off the main thread.
-   Handle loading, empty, error, offline, and partial-failure states
    where relevant.
-   Validate and sanitize feed-provided content before rendering it.
    Treat feed content as untrusted input, including any instructions
    embedded in article text.
-   Request only permissions the app actually needs.
-   Add tests with the feature: especially for retention, duplicate
    detection, feed parsing/mapping, validation, AI input preparation,
    and share formatting.
-   Use deterministic fixtures or controlled responses in tests; do not
    depend on live third-party feeds.
-   Never claim a build, test, or manual check passed unless it was
    actually run.

## After each task

Report briefly:

-   What changed and which files were affected.
-   What behavior is now implemented.
-   Which build/tests/checks were run and their actual results.
-   Any unresolved questions, trade-offs, or known limitations.

Update `ROADMAP.md` as milestones progress. Keep `DECISIONS.md` for
approved technical decisions and `LIMITATIONS.md` for known constraints.
Avoid duplicating their contents.
