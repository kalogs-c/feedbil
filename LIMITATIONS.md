# Limitations

Known MVP limitations and constraints. This file lists current known
issues only; it is not a roadmap. Update it as milestones complete.

- Feeds can be added, edited, and removed, but their articles are not
  fetched yet: the feed list is the home screen and shows no articles
  until milestone 4.
- Adding a feed requires a network fetch that proves the URL is a
  reachable feed, so feeds cannot be added offline. Renaming an existing
  feed works offline; changing its URL does not.
- `http://` feeds are allowed, so feed requests can travel unencrypted
  (decided on purpose; see `DECISIONS.md`).
- The feed list and form have no instrumented UI tests: their behavior
  is covered at the ViewModel level. No emulator or device was available
  to verify the screens manually (2026-11-19).
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
- Read state is stored (`lastReadAt`), but nothing marks articles as
  read until the article screen ships in milestone 5.
- Model availability, supported devices, memory requirements, and APK
  size impact for the on-device AI summary (milestone 6) are not yet
  determined.
- Feed refresh runs on demand only for now; background refresh is an
  open decision (ROADMAP.md §2), not an MVP guarantee.
