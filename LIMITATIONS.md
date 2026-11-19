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
- The feed and article-list screens have no instrumented UI tests: their
  behavior is covered at the ViewModel level. No emulator or device was
  available to verify the screens manually (2026-11-19).
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
- Model download runs in the foreground with a progress bar: leaving the app
  or screen cancels it, and the partial file is discarded. A background
  download with a notification is deferred.
- A single model is hard-coded. There is no model selection screen or download
  of alternative models in the MVP, even though the app is structurally ready
  for one.
- The whole feed-provided article text is sent to the model with no truncation.
  Very long articles may exceed the effective context window; the app reports
  a specific error instead of a summary, and the input limit can be tuned
  after real-device use.
- Generated summaries are stored only in the article screen's ViewModel. They
  are lost when the screen is closed; no summary is persisted or reused for AI
  sharing yet (Milestone 7).
- AI inference has not been exercised on a physical device with the real
  model. JVM tests cover input preparation, prompt building, download state
  handling and ViewModel states, but the native engine's behavior and latency
  are unverified on hardware.
- The model is downloaded from Hugging Face, which requires network access the
  first time. Inference itself is offline; the feed text is never uploaded.
  The expected byte count is pinned in `ModelManager`; if the publisher
  replaces the model file, that constant must be updated or downloads will be
  rejected as the wrong size.
