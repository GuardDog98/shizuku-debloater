# ShizukuDebloater — safer defaults, improved UX

This repo is an example Android helper app that uses Shizuku to perform package management operations. It is intentionally conservative: safe-by-default UI, clear warnings, and explicit expert flows for dangerous operations.

What's new in this iteration
- Settings: improved UX, Export/Import of allow/ignore lists (clipboard-based copy/paste)
- Restore panel: real restore helper using `cmd package install-existing` and a selectable history list
- Shell helper (ShellUtils) centralizes Shizuku shell calls for reuse
- UI utilities (UIUtils) for consistent dialogs
- Better risk explanations in the UI and README

Key safety notes (read before using)
- Safe mode is the default. Expert mode unlocks force-remove for system apps but requires Shizuku to run as root.
- Force-remove attempts to delete APK files from the system partition. This can break your device and should be used only on test devices/emulators.
- Restore helper uses `cmd package install-existing` which only succeeds if the APK is still present in a system image or cached in a user data location.

How to use
- Install Shizuku Manager and start it (for force-remove start it in root mode if you understand the risks).
- Launch app, grant Shizuku permission.
- Use Settings to manage allow/ignore lists (export/import via clipboard).
- Use Restore to attempt restores where possible.

Legal & warranty
This project is provided under the MIT License. There is no warranty. The authors are not responsible for damages or data loss. Always test on non-critical devices.
