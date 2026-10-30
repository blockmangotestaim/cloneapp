# CloneSpace

Parallel-space style app cloner built on an open-source virtualization engine
([BlackBox](https://github.com/FBlackBox/BlackBox), Apache-2.0 — verify the license of whichever fork you choose).
No Binder / PackageManager hooking is written here; the engine does that.

## Build in the cloud
1. Push this folder to a GitHub repo.
2. **Actions → Build debug APK → Run workflow.** Inputs: JDK version, engine repo/ref/module folder, edition (both / 64-bit / 32-bit), NDK version.
3. Download the APK(s) from the run's **Artifacts**. Debug-signed, no keys needed.

## Two editions (32-bit vs 64-bit)
A virtualized app runs inside the host's process, so it must match the host's bitness.
The workflow builds **arm64-v8a** and **armeabi-v7a (arm32)** APKs (different package ids, installable side by side).
The app inspects each target's `lib/<abi>/` folders (including splits) and shows
"Unsupported: …use the 32-bit edition" in the picker and a dialog on tap. Pure-Java apps work in both.

## Permissions
The host manifest declares the permissions clones typically request, and the app asks for the runtime ones
(camera, mic, storage, contacts, location, phone) before the first clone launch. `targetSdk` is 28 on purpose so
storage and permission behavior match what virtualized apps expect.

## Layout
- `app/.../EngineBridge.kt` — **every** engine call + support checks. If your engine fork renames an API, only this file changes.
- `MainActivity` (clones, launch, shortcut, delete) · `PickAppActivity` (clone an app) · `LaunchProxyActivity` (shortcut target)
- `settings.gradle` includes `engine/<module>` as `:Bcore`.

## Known limits (inherent to this technique)
- Split-APK/AAB-only apps, apps with strong anti-tamper (many games/banking), and apps that need Google Play Services sign-in inside the clone may fail or misbehave; the app reports the engine's error.
- Not tested here: this project was written without network access, so the first CI run may need a small tweak
  (engine module name, a Gradle/AGP version bump if the engine requires it, or an API rename in `EngineBridge.kt`).
- Replace the launcher icon (`sym_def_app_icon`) before sharing.
