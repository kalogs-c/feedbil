# Limitations

Known MVP limitations and constraints. This file lists current known
issues only; it is not a roadmap. Update it as milestones complete.

- The MVP is in development; no feeds can be added, refreshed, or read
  yet. The home screen is a placeholder.
- A Windows-installed Android SDK exists (reachable via WSL at
  `/mnt/c/Users/Admin/AppData/Local/Android/Sdk`) but it cannot be used
  for Linux builds because its build-tools/AAPT are Windows binaries.
  The Linux build uses a minimal Linux SDK at `~/Android/Sdk`
  (`platform-tools`, `platforms;android-37.0`, `build-tools;36.0.0`).
  The app has not yet been launched on a device or emulator from this
  machine; launch verification happens on the Windows side.
- Model availability, supported devices, memory requirements, and APK
  size impact for the on-device AI summary (milestone 6) are not yet
  determined.
- Feed refresh runs on demand only for now; background refresh is an
  open decision (ROADMAP.md §2), not an MVP guarantee.
