# Limitations

Known MVP limitations and constraints. This file lists current known
issues only; it is not a roadmap. Update it as milestones complete.

- Feeds and articles can be stored locally, but no screen uses the
  storage yet: the home screen is still a placeholder and feeds cannot
  be added until milestone 3.
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
