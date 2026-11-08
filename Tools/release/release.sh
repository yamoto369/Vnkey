#!/bin/zsh
# Builds, signs, notarizes and publishes a Vikey release.
#   Tools/release/release.sh 1.0.1
# Steps: bump version → engine rule lock → universal Release build → Developer ID sign → notarize app
#        → DMG (signed, notarized, stapled) → site/downloads + release.json → GitHub Release → deploy site.
set -e
if [ "$1" = "--windows" ]; then exec "$(dirname "$0")/release_windows.sh" "${@:2}"; fi   # Windows lane, after the Mac release
if [ "$1" = "--linux" ]; then exec "$(dirname "$0")/release_linux.sh" "${@:2}"; fi         # Linux lane
if [ "$1" = "--android" ]; then exec "$(dirname "$0")/release_android.sh" "${@:2}"; fi   # Android lane
VER=${1:?usage: release.sh <version> | release.sh --windows <version>}
cd "$(dirname "$0")/../.."
ROOT=$PWD
KC=~/Library/Keychains/s10w-signing.keychain-db
KCP=$(grep "^KC_PASS=" ~/.secrets/s10w_signing | cut -d= -f2-)
IDENT="Developer ID Application: Typing Technology Company Limited (3H6967F79Y)"
NOTARY=(--key ~/.secrets/AuthKey_3QNAF6H8AT.p8 --key-id 3QNAF6H8AT --issuer 6587ae6d-4945-468e-8879-d58b7a6d33ad)

echo "==> version $VER"
# the site is in Vietnamese and English (D31): both changelogs need this release
for f in site/pages/changelog.html site/pages_en/changelog.html; do
  grep -q "<!-- release $VER -->" $f || { echo "missing <!-- release $VER --> in $f"; exit 1; }
done
BUILD=$(( $(grep 'CURRENT_PROJECT_VERSION' project.yml | grep -o '[0-9]\+') + 1 ))
sed -i '' "s/MARKETING_VERSION: \".*\"/MARKETING_VERSION: \"$VER\"/; s/CURRENT_PROJECT_VERSION: \".*\"/CURRENT_PROJECT_VERSION: \"$BUILD\"/" project.yml
xcodegen generate >/dev/null

echo "==> engine rule lock"
Tools/autoswitch/check.sh >/dev/null || { echo "check.sh failed: engine behavior changed"; exit 1; }

echo "==> build (universal)"
rm -rf build/dd dist && mkdir -p dist
xcodebuild -project Vikey.xcodeproj -scheme Vikey -configuration Release -derivedDataPath build/dd \
  ARCHS="arm64 x86_64" ONLY_ACTIVE_ARCH=NO CODE_SIGN_IDENTITY=- CODE_SIGNING_REQUIRED=NO build | grep -E "error:|BUILD" || true
APP=build/dd/Build/Products/Release/Vikey.app
[ "$(defaults read "$ROOT/$APP/Contents/Info.plist" CFBundleShortVersionString)" = "$VER" ] || { echo "built version mismatch"; exit 1; }
lipo -archs "$APP/Contents/MacOS/Vikey"
cp -R "$APP" dist/

echo "==> sign + notarize app"
security unlock-keychain -p "$KCP" "$KC"
# Sparkle's helpers first (inside out, no --deep), then the app with its entitlements
SPK=dist/Vikey.app/Contents/Frameworks/Sparkle.framework/Versions/B
for x in $SPK/XPCServices/Installer.xpc $SPK/XPCServices/Downloader.xpc $SPK/Autoupdate $SPK/Updater.app; do
  [ -e "$x" ] && codesign --force --options runtime --timestamp --preserve-metadata=entitlements -s "$IDENT" "$x"
done
codesign --force --options runtime --timestamp -s "$IDENT" dist/Vikey.app/Contents/Frameworks/Sparkle.framework
codesign --force --options runtime --timestamp --entitlements App/Vikey.entitlements -s "$IDENT" dist/Vikey.app
codesign --verify --deep --strict dist/Vikey.app
ditto -c -k --keepParent dist/Vikey.app dist/Vikey.zip
xcrun notarytool submit dist/Vikey.zip "${NOTARY[@]}" --wait | tail -1
xcrun stapler staple dist/Vikey.app | tail -1
rm dist/Vikey.zip

echo "==> DMG"
DMG=dist/Vikey-$VER.dmg
python3 Tools/release/dmg_background.py dist/dmg_bg.png
create-dmg --volname "Vikey" --volicon design/Vikey.icns \
  --background dist/dmg_bg.png --window-pos 200 120 --window-size 660 420 \
  --icon-size 128 --icon "Vikey.app" 170 190 --app-drop-link 490 190 \
  --hide-extension "Vikey.app" --no-internet-enable \
  --codesign "$IDENT" "$DMG" dist/Vikey.app >/dev/null || true
[ -f "$DMG" ] || { echo "dmg not created"; exit 1; }
xcrun notarytool submit "$DMG" "${NOTARY[@]}" --wait | tail -1
xcrun stapler staple "$DMG" | tail -1
spctl -a -vv -t open --context context:primary-signature "$DMG" 2>&1 | tail -1

echo "==> site + GitHub release"
mkdir -p site/downloads
cp "$DMG" site/downloads/
SHA=$(shasum -a 256 "$DMG" | cut -d' ' -f1)
# merge, don't overwrite: release.json also carries the other platforms (windows, linux, ios)
python3 - "$VER" "$SHA" <<'PY2'
import json, os, sys
ver, sha = sys.argv[1:]; p = "site/release.json"
rel = json.load(open(p)) if os.path.exists(p) else {}
rel.update(version=ver, download_url=f"/downloads/Vikey-{ver}.dmg", sha256=sha)
json.dump(rel, open(p, "w"), indent=0); open(p, "a").write("\n")
PY2
# Sparkle feed: EdDSA-signed DMG, notes from the changelog
SIG=$(~/.tools/sparkle/bin/sign_update --ed-key-file ~/.secrets/vikey_sparkle_ed25519 "$DMG")
NOTES=$(sed -n "/<!-- release $VER -->/,/<!-- \/release -->/p" site/pages/changelog.html | grep -v '<h2>\|<!--')
python3 Tools/release/appcast.py mac "$VER" "$BUILD" "https://vikey.app/downloads/Vikey-$VER.dmg" "$SIG" "$NOTES"
setopt null_glob; for old in site/downloads/Vikey-*.dmg; do [ "$old" = "site/downloads/Vikey-$VER.dmg" ] || rm "$old"; done
git add project.yml site/release.json site/appcast.xml && git commit -q -m "Release $VER" && git push -q origin main
gh release create "v$VER" "$DMG" -R tonyminibot/Vikey --title "Vikey $VER" --notes-file <(sed -n "/<!-- release $VER -->/,/<!-- \/release -->/p" site/pages/changelog.html | sed 's/<li>/- /; s/<[^>]*>//g' | sed '/^\s*$/d') 2>/dev/null \
  || gh release create "v$VER" "$DMG" -R tonyminibot/Vikey --title "Vikey $VER" --notes "Vikey $VER"
Tools/release/deploy_site.sh
echo "==> done: https://vikey.app"
