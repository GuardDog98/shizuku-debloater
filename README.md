# ShizukuDebloater (safe-by-default package manager)

This repo is a minimal Android app example built around Shizuku. It is designed to be safe by default and intentionally conservative.

Main behaviors
- Lists packages and shows app labels
- Shows package filters
- Safe mode: only Disable and Uninstall-for-user operations are active by default
- Expert mode: enables a danger-only force remove flow for system apps after root plus confirmation
- Allow list and Ignore list: user can protect important packages or hide ones they do not want to see
- Restore panel: keeps a minimal uninstall history and acts as a restore entry point
- APK path preview and risk dialog: before a risky operation, the app shows the package path and explains why the action is dangerous

Recommended usage
1. Install Shizuku Manager and start it.
2. Open this app and grant Shizuku permission.
3. Start in Safe mode. Only non-risky actions are shown.
4. If you really need advanced behavior, enable Expert mode and only after verifying the package path and confirming the exact package name.
5. Prefer Disable before Uninstall, and prefer Uninstall-for-user before any system-partition deletion attempt.

Important safety notes
- Force remove is intentionally dangerous and can affect boot stability or render a device unusable.
- It should only be used on a spare device, test handset, or emulator with a known-good recovery path.
- If you are not comfortable with root access, system partition edits, or remount operations, do not use Force remove.
- This app is not a guarantee of safety; the developer, maintainer, and repository owners are not liable for device damage or data loss.

Project structure
- MainActivity.kt — main package list and command execution
- SettingsActivity.kt — allow/ignore list editor
- RestoreActivity.kt — restore panel and package history
- README.md — usage and safety notes
- DISCLAIMER.txt — legal warnings and liability disclaimer
- LICENSE — MIT license

Legal note
This project is provided under the MIT License but that does not eliminate the risk of device damage or liability. The applicable laws in your jurisdiction may impose additional restrictions. Use this project only on devices you own and are permitted to modify.
