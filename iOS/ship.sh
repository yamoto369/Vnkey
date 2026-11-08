#!/bin/bash
# Vikey iOS → TestFlight. Unsigned archive → sign the keyboard extension, then the app, each with
# its App Store profile's entitlements → IPA → altool upload. Bump CURRENT_PROJECT_VERSION first.
# NO_UPLOAD=1 stops after signing (QA the IPA).
set -euo pipefail
cd "$(dirname "$0")"
CERT="D65079A872D9780C59A00352DA1334085760F4C3"   # Apple Distribution (skyline-ci keychain)
KC=~/Library/Keychains/skyline-ci.keychain-db
S=build/signing

echo "==> engine rule lock"
../Tools/autoswitch/check.sh >/dev/null || { echo "check.sh failed: engine behavior changed"; exit 1; }

echo "==> regenerate + clean"
xcodegen generate >/dev/null
rm -rf build/Vikey.xcarchive build/ship

echo "==> unsigned archive"
xcodebuild archive -project VikeyiOS.xcodeproj -scheme Vikey -configuration Release \
  -destination 'generic/platform=iOS' -archivePath build/Vikey.xcarchive \
  CODE_SIGN_STYLE=Manual CODE_SIGNING_ALLOWED=NO >/dev/null

APP="build/Vikey.xcarchive/Products/Applications/Vikey.app"
EXT="$APP/PlugIns/VikeyKeyboard.appex"
VER=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleVersion' "$APP/Info.plist")
echo "==> archived build $VER"
ls "$APP" | grep -q Assets.car || { echo "FAIL: no Assets.car (app icon)"; exit 1; }
[ -d "$EXT" ] || { echo "FAIL: keyboard extension missing"; exit 1; }

security unlock-keychain -p "$(grep '^KC_PASS=' ~/.secrets/s10w_signing | cut -d= -f2-)" "$KC" 2>/dev/null || true
sign() {   # sign <bundle> <profile>
  cp "$2" "$1/embedded.mobileprovision"
  security cms -D -i "$2" > build/pp.plist 2>/dev/null
  /usr/libexec/PlistBuddy -x -c 'Print :Entitlements' build/pp.plist > build/ent.plist
  find "$1" -maxdepth 2 \( -name '*.framework' -o -name '*.dylib' \) | while read f; do codesign --force --sign "$CERT" --keychain "$KC" "$f"; done
  codesign --force --sign "$CERT" --keychain "$KC" --entitlements build/ent.plist --timestamp=none "$1"
}
echo "==> sign"
sign "$EXT" "$S/AppStore_com.devuap.vikey.ios.keyboard.mobileprovision"
sign "$APP" "$S/AppStore_com.devuap.vikey.ios.mobileprovision"
codesign --verify --deep --strict "$APP"
for b in "$APP" "$EXT"; do
  codesign -d --entitlements :- "$b" 2>/dev/null | grep -q group.com.devuap.vikey || { echo "FAIL: app group missing in $b"; exit 1; }
done
echo "    entitlements ok (app group in app + keyboard)"

echo "==> package"
mkdir -p build/ship/Payload && cp -R "$APP" build/ship/Payload/
( cd build/ship && zip -qry Vikey.ipa Payload )
[ -n "${NO_UPLOAD:-}" ] && { echo "==> NO_UPLOAD: build/ship/Vikey.ipa"; exit 0; }

echo "==> upload"
xcrun altool --upload-app -f build/ship/Vikey.ipa -t ios --apiKey 3QNAF6H8AT --apiIssuer 6587ae6d-4945-468e-8879-d58b7a6d33ad 2>&1 | tail -4
echo "==> uploaded build $VER"
