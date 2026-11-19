# Limitations

Known MVP limitations and constraints. This file lists current known
issues only; it is not a roadmap. Update it as milestones complete.

- Feed refresh is manual or happens once when the app process starts; there
  is no background refresh, timer, or automatic retry, and returning to an
  already-running app does not refresh. A future background worker can reuse
  `FeedRefreshService` but is not implemented.
- Feeds are downloaded in full on every refresh; no conditional GET/ETag or
  caching is implemented, so a manual refresh always uses the network.
- Per-feed refresh failures are kept in memory for the current session. After
  a restart, only an old `lastFetchedAt` hints that a feed has been failing.
- Cancelling a fetch (for example by leaving the add-feed screen) does not
  abort the in-flight HTTP request. rssparser runs its download and parse in
  internal coroutines with their own `Job`, so cancellation is observed only
  after parsing finishes. The result is discarded, but the request keeps using
  network resources until it completes.
- Entries without a usable link are not stored. Titles are never invented: a
  missing title is shown as "Sem título".
- Article images are loaded on demand from the URLs in the feed's HTML. Text
  remains readable offline, but images do not appear without a network
  connection and are not cached on disk by the app beyond Coil's in-memory
  and HTTP caches. Loading an image contacts the host named by the feed,
  which may be a third-party tracker.
- Images embedded inside a sentence are rendered as their own block between
  paragraphs instead of flowing inline with the text.
- If feed markup contains a skipped tag such as `<script>` or `<iframe>` with
  no closing tag, parsing continues after its opening tag, so that tag's body
  may be shown as plain text. It is never executed, but it can be noisy.
- The article detail screen has no instrumented UI tests; loading, read-state
  and formatting behavior is covered at the ViewModel and formatter level.
- Adding a feed requires a network fetch that proves the URL is a
  reachable feed, so feeds cannot be added offline. Renaming an existing
  feed works offline; changing its URL does not.
- `http://` feeds are allowed, so feed requests can travel unencrypted
  (decided on purpose; see `DECISIONS.md`).
- The feed and article-list screens have no instrumented UI tests: behavior is
  covered by ViewModel tests and Robolectric Compose tests that assert
  accessible names and rendered states. No emulator or device was available to
  the agent; manual device checks are delegated to the user.
- The project builds with the Windows-installed Android SDK from the
  Windows side. Linux/WSL builds are not supported: the project resides
  on the Windows filesystem and the Windows SDK build-tools are not
  Linux-compatible.
- Articles published without a usable date are stored with the time the
  app saw them, and the app displays that value as their date. The
  stored value cannot be distinguished from a real publication date
  later (decided on purpose; see `DECISIONS.md`).
- Duplicate detection compares the trimmed `guid` and `link` only. The
  same article re-published under a URL with different query parameters
  is stored twice, and two entries that share one link but have
  different guids are merged into a single article.
- Retention runs after a feed update and once when the app starts.
  Nothing prunes while the app is closed, so expired articles can remain
  on disk until the next launch or refresh.
- The on-device summary model is a ~2.59 GB one-time download and uses roughly
  1.7 GB peak RAM on CPU. A supported 64-bit Android device with enough free
  storage and memory is required; smaller/older devices may fail to load it.
  The APK does not include the model.
- Model download runs in the foreground with a progress bar. Cancellation is
  checked between downloaded chunks, so leaving the app or screen discards the
  partial file, but the current network read may still finish first. A
  background download with a notification is deferred.
- A single model is hard-coded. There is no model selection screen or download
  of alternative models in the MVP, even though the app is structurally ready
  for one.
- The whole feed-provided article text is used without a dedicated AI input
  limit. The M5 HTML sanitizer still caps input at 200,000 characters, so
  extremely long articles are shortened at that point, and longer articles may
  exceed the model's effective context window. The app reports a specific error
  instead of a summary; both limits can be tuned after real-device use.
- Generated summaries are stored only in the article screen's ViewModel. They
  are reused for AI sharing while the screen is open, even if the model later
  becomes unavailable, but are lost when the screen is closed. Reopening the
  article therefore requires a new inference (and the model) for AI sharing;
  no summary is persisted.
- Shared AI text contains only the summary and the source URL, with no
  "generated by AI" marker; recipients cannot tell it from source material.
  The article screen still labels generated summaries (an approved override;
  see `DECISIONS.md`).
- Share actions are hidden when the article's feed link is not a valid
  `http(s)` URL, because only such URLs are opened or shared.
- Sharing has not been checked against real Android share targets on a
  device. Share text and ViewModel share events are covered by JVM tests;
  the chooser/intent path itself has no UI or device test.
- If the app is backgrounded while AI-share inference is running, the share
  is not opened when inference finishes. Collection is limited to RESUMED
  to avoid starting an activity from the background, and share events are
  not replayed to a later collector.
- AI inference has not been exercised on a physical device with the real
  model. JVM tests cover input preparation, prompt building, download state
  handling and ViewModel states, but the native engine's behavior and latency
  are unverified on hardware.
- The model is downloaded from Hugging Face, which requires network access the
  first time. Inference itself is offline; the feed text is never uploaded.
  The expected byte count is pinned in `ModelManager`; if the publisher
  replaces the model file, that constant must be updated or downloads will be
  rejected as the wrong size. Integrity checking is size-only, not a checksum:
  a corrupted file of exactly the pinned size would pass.
- The LiteRT-LM AAR ships 64-bit native libraries only (`arm64-v8a`, `x86_64`).
  On a 32-bit device the model can still download, but summary generation fails
  with a typed error instead of crashing the app.
- App data is never backed up (`allowBackup="false"`). Uninstalling the app or
  clearing its data permanently removes all feeds and articles; there is no
  in-app account or server-side restore (decided on purpose; see
  `DECISIONS.md`).
- There is no proactive offline indicator. With no connection, stored articles
  remain readable and the AI summary works once the model is present, but
  images do not load and any refresh or feed addition fails with a localized
  network/timeout error. No `ACCESS_NETWORK_STATE` permission is requested
  (decided on purpose; see `DECISIONS.md`).
- Compose UI tests run on the JVM with Robolectric and assert accessible names
  and states; they are not a substitute for on-device TalkBack traversal.
  Manual checks (dark theme, font scaling, TalkBack focus and announcements,
  airplane mode, network interruption, real feeds, share targets, and AI
  responsiveness) are run by the user; any check not reported as passing stays
  unverified.
