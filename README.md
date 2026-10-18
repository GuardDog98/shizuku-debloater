# ShizukuDebloater

A small Android helper app that uses Shizuku to manage packages in a safer, more transparent way.

This project is intentionally conservative:
- Safe mode is the default.
- Expert mode unlocks advanced actions.
- Force removal is visible only when the user explicitly enables it.
- Every risky operation shows a clear explanation before it is allowed.

What it does
- Lists packages available to the current user.
- Shows app labels and package names.
- Allows Disable and Uninstall-for-user actions.
- Supports allow-list and ignore-list protection.
- Provides a restore panel for previously uninstalled packages.
- Shows the APK path and explains the risk before dangerous actions.

Quick start
1. Install Shizuku Manager and start it.
2. Allow your app to use Shizuku.
3. Open the app and tap Scan packages.
4. Start in Safe mode.
5. For advanced/system-package work, enable Expert mode and proceed only on a spare device or emulator.

Safe mode vs Expert mode
Safe mode
- Default and recommended.
- Only Disable and Uninstall for user are active.
- System package warnings are shown before risky actions.

Expert mode
- Enables force-remove functionality.
- Requires Shizuku root access.
- Requires a second confirmation and package-name match.
- Uses the APK path preview and a warning screen.

Allow list and Ignore list
- Allow list: packages in this list are protected and cannot be actioned.
- Ignore list: packages are hidden from the main list to reduce clutter.
- Lists can be exported/imported with clipboard JSON.

Restore panel
- Keeps a basic uninstall history.
- Lets the user select a package and try `cmd package install-existing <pkg>`.
- This is a best-effort helper; restore success depends on the device and package state.

Why force remove is risky
Force removal attempts to delete APK files from the system partition. This can affect:
- boot stability
- OEM or vendor services
- Google Play / system component behavior
- device restore flow

Use only on a test device or spare phone. Never use it on a device where you cannot recover it.

Legal note
This project is provided under the MIT License. It comes with no warranty. The authors are not responsible for damages, data loss, or device instability. Always test on a non-critical device and ensure you are allowed to modify it.

Troubleshooting
- Shizuku not running: start Shizuku Manager and grant permission.
- Permission denied: ensure the app is allowed in Shizuku.
- Package missing: the package may already be disabled or hidden by the device.
- Restore failed: many devices do not keep the system package cache in a restore-friendly state.

Recommended order of operations
1. Disable
2. Uninstall for user
3. Restore only if needed
4. Force remove only in Expert mode on a test device
