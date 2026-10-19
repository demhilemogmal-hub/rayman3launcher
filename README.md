# Rayman 3 Launcher

Small Android app that remembers your Rayman 3 disc image and starts the
official Dolphin app with it auto-started.

## Build
1. Open this folder in Android Studio (Hedgehog or newer). Let it sync; accept
   any prompt to upgrade the Gradle/AGP versions or generate the Gradle wrapper.
2. Run on a phone or build with Build > Build APK(s).

## Use
1. Install Dolphin from dolphin-emu.org (package `org.dolphinemu.dolphinemu`) and
   open it once so it can set up its user folder and permissions.
2. Dump your own Rayman 3 disc (.iso/.rvz/.gcm) onto the phone.
3. In this app tap "Choose disc image" (or "Enter file path instead"), then Play.

## Notes
- Touch controls, save states and graphics settings are Dolphin's own; change
  them inside Dolphin.
- Launching works by sending Dolphin's `AutoStartFile` / `AutoStartFiles`
  extras to its MainActivity. Passing a `content://` URI is untested; if Dolphin
  can't open the game, use "Enter file path instead".
- Not compiled or tested yet.

## Build the APK without Android Studio
Push this folder to a GitHub repo. The workflow in `.github/workflows/build.yml`
builds a debug APK on every push (or run it manually from the Actions tab).
Download `rayman3-launcher-debug-apk` from the run's Artifacts, unzip it, and
install `app-debug.apk` on your phone (allow installs from unknown sources).
