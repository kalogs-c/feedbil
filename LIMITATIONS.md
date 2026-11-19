# Limitations

Known MVP limitations and constraints. This file lists current known
issues only; it is not a roadmap. Update it as milestones complete.

- The MVP is in development; no feeds can be added, refreshed, or read
  yet. The home screen is a placeholder.
- The project builds with the Windows-installed Android SDK from the
  Windows side. Linux/WSL builds are not supported: the project resides
  on the Windows filesystem and the Windows SDK build-tools are not
  Linux-compatible. Launch verification (emulator/physical device) is
  pending on the Windows side.
- Model availability, supported devices, memory requirements, and APK
  size impact for the on-device AI summary (milestone 6) are not yet
  determined.
- Feed refresh runs on demand only for now; background refresh is an
  open decision (ROADMAP.md §2), not an MVP guarantee.
