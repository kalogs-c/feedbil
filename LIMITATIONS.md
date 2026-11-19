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
- Entries without a usable link are not stored. Titles are never invented: a
  missing title is shown as "Sem título".
- Article images are loaded on demand from the URLs in the feed's HTML. Text
  remains readable offline, but images do not appear without a network
  connection and are not cached on disk by the app beyond Coil's in-memory
  and HTTP caches. Loading an image contacts the host named by the feed,
  which may be a third-party tracker.
- Images embedded inside a sentence are rendered as their own block between
  paragraphs instead of flowing inline with the text.
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
- Model availability, supported devices, memory requirements, and APK
  size impact for the on-device AI summary (milestone 6) are not yet
  determined.
