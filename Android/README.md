# Vikey for Android

Gradle project for the Android keyboard (`app.vikey`). See `PLAN.md` for how it maps to the iOS keyboard and how
to build, test and release. Quick start:

```sh
source ~/.tools/android_env.sh          # JDK 21 + Android SDK/NDK paths (on the Mac mini)
cd Android && ./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell ime enable app.vikey/.ime.VikeyIME && adb shell ime set app.vikey/.ime.VikeyIME
python3 Tests/livetest.py               # 14 typing cases through the real touch path
```

Release builds are signed when `Android/keystore.properties` exists (storeFile, storePassword, keyAlias,
keyPassword; never committed). Release certificate SHA-256:
`C2:87:4A:2B:75:8F:D7:69:E7:B8:00:CD:C8:E1:32:9A:D0:86:56:8C:A7:9F:40:E2:E3:7E:7D:68:3F:E6:5E:35`.
