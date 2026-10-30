# Cloner APK builder

Builds an Android app-cloner APK on GitHub's servers, so you don't need Android Studio.
It uses the open-source BlackBox virtual engine (Apache-2.0), which is the same kind of
engine that "parallel space" / "multi app" cloners are built on. The engine's own demo
app already has the UI: pick an installed app, clone it, launch it.

## Steps
1. Create a new GitHub repo and upload this folder's contents (keep `.github/workflows/build.yml`).
2. Open the **Actions** tab, choose **Build cloner APK**, click **Run workflow**.
3. When it finishes (about 5-10 minutes), download **cloner-apks** from the run page and unzip it.
4. Install the APK. If your phone is 64-bit and a target app doesn't show in the list,
   try the other APK (32-bit vs 64-bit). The engine ships them as separate apps.

## If the build fails
- Change the **JDK version** input (11, then 17, then 8). Old Android projects are picky.
- Change **engine_repo** to another fork, for example `runforprogram/BlackBox-new`.
- Paste the red error lines from the Actions log to Claude and ask for a fix.

## Honest expectations
- Works well for ordinary apps (social, messaging, utilities, many games).
- Banking, streaming-DRM and anti-cheat games often detect virtualization and refuse to run.
- Some services forbid multiple instances; accounts can be banned. Your risk.
- Android 13+ support depends on the fork. Newer Android versions break these engines often.
- The upstream project's author deleted the original repo, so forks are not equally maintained.
