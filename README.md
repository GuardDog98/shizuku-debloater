# ShizukuDebloater (minimal example)

What this repo contains
- Minimal Android app that lists installed packages (user scope) and allows disabling or "uninstall for user" via Shizuku.
- NOT intended to automatically remove many packages without user confirmation.
- Requires Shizuku Manager installed and user-granted Shizuku permission.

How to build & test
1. Install Shizuku Manager from Play/F-Droid and start it (follow Shizuku docs).
2. Open project in Android Studio, build & install on a device where Shizuku is running.
3. Launch app, tap "Scan packages", grant Shizuku permission when requested by Shizuku Manager.
4. Use Disable / Uninstall buttons per-package. Confirm dialogs are shown before actions.

Safety & legal
- This is an example. Use at your own risk.
- The app performs operations that can affect system behavior. Prefer "Disable" first.
- Do NOT include proprietary code in this repo.
