# ShizukuDebloater (minimal example)

What this repo contains
- Minimal Android app that lists installed packages (user scope) and allows disabling or "uninstall for user" via Shizuku.
- Expert-mode: optionally allows a dangerous "force remove" (attempt to delete APK from system partition) but only when Shizuku is running with root privileges and after strong confirmation.
- NOT intended to automatically remove many packages without user confirmation.
- Requires Shizuku Manager installed and user-granted Shizuku permission.

How to build & test
1. Install Shizuku Manager from Play/F-Droid and start it (follow Shizuku docs). For system-level deletion you must start Shizuku Manager in root mode (or start it via adb with root) so the service runs as root.
2. Open project in Android Studio, build & install on a device where Shizuku is running.
3. Launch app, tap "Scan packages", grant Shizuku permission when requested by Shizuku Manager.
4. Use Disable / Uninstall buttons per-package. Force remove is only visible for system apps and only usable in Expert mode when running as root. Force remove requires typing the exact package name to confirm.

Safety & legal
- This is an example. Use at your own risk.
- Force remove attempts to modify system partitions — this can brick your device. Do NOT use on a primary/critical device. Test only on a spare device or emulator.
- The app performs operations that can affect system behavior. Prefer "Disable" and "Uninstall for user" first.
- Do NOT include proprietary code in this repo.

Recommended safe testing steps
- Test first on an emulator or device that you can restore via fastboot.
- Use "Scan packages" to preview packages and their labels.
- Use "Disable" to test behavior before uninstalling.
- If you use "Force remove", first check the APK path (the app attempts to find it with `pm path <package>` and shows logs).

Legal reminder
- You are responsible for ensuring you have permission to modify the device. Authors are not responsible for damages.
