# Lodgy APK

`lodgy-debug.apk` is a debug build of the app, rebuilt and pushed here after
every change to `app/` so this is always the latest code on `master`.

To install: download the file to an Android device (or `adb install
lodgy-debug.apk`) and open it. You'll need to allow installs from this source
if prompted - it's an unsigned debug build, not from the Play Store.

This is a debug build, not a release build: unsigned (debug key), unminified,
and does not represent final production output. It's for trying the app
quickly, not for distribution to end users.
