# Vikey for Android — plan and status

Tony, 2026-10-29: "lets build android version" (after Windows and Linux shipped). Parity target: the iOS keyboard
and app, feature for feature.

## How it works on Android

Vikey is an **input method service** (`app.vikey/.ime.VikeyIME`), the way every keyboard on Android works. The
system hands it the focused text field (an `InputConnection`), it draws the keyboard, and it commits text and
deletions back. Same `Engine/` C++ as macOS/Windows/Linux/iOS, built with the NDK into `libvikey.so`; the JNI
bridge `Android/app/src/main/cpp/vikey_jni.cpp` mirrors `iOS/Shared/VKDriver.mm`.

| iOS feature | Android |
|---|---|
| Telex, tone placement, spelling, restore-if-invalid, auto switch, Backspace undo, 5 s pause | same `Engine/Driver` through JNI (`ime/VikeyDriver.kt`) |
| Keyboard layout (iOS 26 geometry: 402 pt base, 43 pt keys, 54 pt pitch, 6 pt gaps, 44 pt strip) | `ime/KeyboardView.kt`, one Canvas-drawn view, same numbers in dp, scaled by width |
| Fat-finger model, auto-correct, suggestions strip, contractions | `TouchModel` through JNI; `VikeyIME.updateSuggestions` is a line-by-line port of the iOS controller |
| Key popup, long-press alternates (ă â á … – ’ ₫), slide from 123 to a symbol, cursor mode on the space bar, delete repeat → words | `KeyboardView` touches (Handler timers instead of `Timer`) |
| Emoji keyboard: categories, recents, skin tones, search in VI/EN/Telex, search mode above the letters | `ime/EmojiKeyboardView.kt` (custom view, fling scroll), `ime/EmojiIndex.kt`; only emoji the device font can draw (`Paint.hasGlyph`) |
| Auto-capitalize, double-space period, English for web/email fields, return key titles (Đi/Tìm/Gửi/Xong/Tiếp) | `EditorInfo` input type + IME action |
| Haptics / key click | `VibrationEffect.EFFECT_TICK` (no Full Access needed on Android) / `AudioManager` key click |
| Settings app, onboarding, demo card, acknowledgements, typing diagnostics + log | Jetpack Compose (`ui/`), same sections and tips as `iOS/App/MainView.swift`; log at `filesDir/keyboard-log.txt`, shared with the share sheet |
| Globe key | shown when another keyboard is enabled; tap = next keyboard, hold = picker |
| Dark mode | follows the system (keyboard + app) |

Not on Android: Full Access (not a concept), app-group defaults (one process: `SharedPreferences` read on every
keyboard open).

## Parity audit (2026-10-30, against iOS keyboard build 23 and the iOS app)

Line-by-line against `iOS/Keyboard/*.swift`, `iOS/Shared/*`, `iOS/App/*.swift`, `App/Model/EmojiSearch.swift`.
Everything not listed here is the same (same rules, same numbers in dp).

| Area | iOS | Android |
|---|---|---|
| Text context: own copy of the text before the cursor, outside-change reset | after-cursor text compare | `onUpdateSelection` positions we produced vs a real move (no after-cursor read: it is an IPC) |
| Snap-back punctuation (`this .` → `this.`, closing `)` `"` only), smart dashes `--` → `—` | build 23 | same (ported 2026-10-30); smart dashes off where auto-correct is off (Android has no smart-dash field flag) |
| Capitals after `."` `)` `…`, `Lets` → `Let's`, `wont` → `won't` | build 23 | same (ported 2026-10-30) |
| Text Replacements (`omw` → `On my way!`) with backspace undo | `UILexicon` | the personal dictionary's shortcuts (`UserDictionary`), same rule (ASCII-letter shortcuts, user's own win) |
| Auto-correct + undo, contractions, suggestion order, double-space period, auto-shift (sentences/words/all caps), web/email fields English, backspace repeat → words, cursor mode, alternates, slide from 123 | | same |
| Suggestion strip | picks on lift (`touchUpInside`) | same (was touch-down until 2026-10-30) |
| Return key titles | go/search/send/done/next/join/route/continue/SOS | go/search/send/done/next; Android IMEs have no join/route/continue/emergency action, those fields show the return arrow. Return runs the editor action (Android convention) instead of typing `\n` |
| Haptics / click | Core Haptics tick 0.4, needs Full Access | `EFFECT_TICK`, no permission; click via `AudioManager` (follows the Touch sounds setting) |
| Diagnostics line + log | stall watch `CADisplayLink`, memory warning, ~200 KB log | `Choreographer`, `onTrimMemory`, same cap; numbers always `1.00 ms` |
| Globe | `needsInputModeSwitchKey` | `shouldOfferSwitchingToNextInputMethod`; tap = next keyboard, hold = picker |
| Keyboard geometry, colours, fonts | 402 pt base | same numbers in dp; VI/EN key semibold (600), key shadow black 0.22 |
| Emoji keyboard | version table (`maxVersion`) | what the device font draws (`Paint.hasGlyph`); Frequently Used = `EmojiUsage` (fading pick count, 24, starter fill), same as iOS since 2026-10-30 |
| Keyboard language | the phone's language (a per-app language never reaches the extension) | same: the IME resolves its strings with the system locale |
| App: rows, tips, sections, footer, copy (VI + EN) | MainView | same; plus "Choose Vikey as the current keyboard" (Android needs the keyboard selected, not only added) |
| App language row | opens the app's Settings page | Android 13+: opens Settings › Apps › Vikey › Language (`ACTION_APP_LOCALE_SETTINGS`, `locales_config` vi/en); hidden below API 33 (no per-app language) |
| Onboarding | 4 steps (4 = Full Access) | 3 steps (Open Settings lands on the keyboard list; no Full Access); titles shrink so a written line never wraps |
| Demo card | dotted red underline | same, drawn under the text (Compose has no dotted underline) |
| Acknowledgements | NavigationLink | same list; system Back returns |
| Send feedback row (build 25), emoticon search `:D` `=))` `^_^` `hihi` (build 24) | | same (`platform=android`) |

Not possible on Android: Full Access (no such permission; haptics and the log just work), join/route/continue/SOS
return titles (no IME actions for them).

Tests: `Tests/livetest.py` (14 shared cases) and `Tests/paritytest.py` (13 cases for the build-23 behaviours and the
strip; needs a personal-dictionary entry `omw` → `On my way!`, see the file).

## Build, test, release

- Toolchain: `~/.tools/android_env.sh` (openjdk 21, SDK 35, NDK 27.2, CMake 3.22). `cd Android && ./gradlew assembleDebug`.
- Emulator: AVD `Vikey-Dev` (Pixel 7, API 35 arm64, headless: `emulator -avd Vikey-Dev -no-window -no-audio`).
  Enable + select: `adb shell ime enable app.vikey/.ime.VikeyIME && adb shell ime set app.vikey/.ime.VikeyIME`.
- Live test: `python3 Android/Tests/livetest.py` taps the keyboard through the real touch path and reads the
  field back, same cases as `Windows/Tests/cases.tsv` (14/14 on 2026-10-29). Needs a debug build (the
  `app.vikey.DEBUG` broadcast sets the language quietly, like `setLanguageForTests` on iOS).
- Release: tag `v<ver>` → `.github/workflows/android.yml` builds `Vikey-<ver>.apk` signed with the release
  keystore (secrets `ANDROID_KEYSTORE_B64`, `ANDROID_KEYSTORE_PROPERTIES`; keystore + passwords in
  `~/.secrets/vikey_android_keystore.*`, certificate SHA-256 `C2:87:4A:2B:…:5E:35`) and puts it on the GitHub
  release; `Tools/release/release.sh --android <ver>` verifies the signature, copies it to vikey.app/downloads,
  writes `release.json.android` and deploys the site (the download page shows the Android card from that key).
- Version = `project.yml` MARKETING_VERSION (versionCode 1.0.16 → 10016).
- Google Play: the company account is being created (2026-10-29); the same build goes through the Play Console
  as an AAB (`./gradlew bundleRelease`) once it exists.

## Status

- 2026-10-30: parity audit against iOS build 23 (table above); livetest 14/14, paritytest 13/13; every app screen and
  keyboard state reviewed in VI/EN, light/dark.
- 2026-10-29: keyboard, emoji keyboard, app (onboarding, settings, acknowledgements) done; live test 14/14 on the
  emulator; release build signed and tested. Not yet tried on a physical phone.
