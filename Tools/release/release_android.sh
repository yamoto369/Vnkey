#!/bin/zsh
# Publishes the Android APK of a release the Mac lane already made:
#   Tools/release/release.sh --android 1.0.16
# The tag v<version> (or a manual run of the android workflow with release_tag) puts Vikey-<ver>.apk on the
# GitHub release, signed with the release keystore (secrets ANDROID_KEYSTORE_B64 / ANDROID_KEYSTORE_PROPERTIES).
# This script waits for it, verifies the signature, copies it to vikey.app/downloads, writes release.json "android"
# and deploys the site. (Google Play comes later: the same APK/AAB goes through the Play Console.)
set -e
VER=${1:?usage: release.sh --android <version>}
cd "$(dirname "$0")/../.."
PV=$(grep MARKETING_VERSION project.yml | grep -o '"[^"]*"' | tr -d '"'); [ "$PV" = "$VER" ] || { echo "project.yml is at $PV, not $VER"; exit 1; }
REPO=tonyminibot/Vikey
APK="Vikey-$VER.apk"
echo "==> waiting for $APK on v$VER"
for i in $(seq 1 60); do
  gh release view "v$VER" -R $REPO --json assets -q '.assets[].name' 2>/dev/null | grep -qx "$APK" && break; sleep 30
done
gh release view "v$VER" -R $REPO --json assets -q '.assets[].name' | grep -qx "$APK" || { echo "$APK not on the release after 30 min: gh run list -R $REPO -w android"; exit 1; }
rm -rf dist/android && mkdir -p dist/android site/downloads
gh release download "v$VER" -R $REPO -p "$APK" -D dist/android --clobber
# the APK must carry the release signature (the keystore's SHA-256 is in Android/README.md)
APKSIGNER=$(ls "${ANDROID_HOME:-$HOME/Library/Android/sdk}"/build-tools/*/apksigner | tail -1)
"$APKSIGNER" verify --print-certs "dist/android/$APK" | grep -q "C2:87:4A:2B:75:8F:D7:69:E7:B8:00:CD:C8:E1:32:9A:D0:86:56:8C:A7:9F:40:E2:E3:7E:7D:68:3F:E6:5E:35\|c2874a2b758fd769e7b800cdc8e1329ad086568ca79f40e2e37e7d683fe65e35" || { echo "$APK is not signed with the release key"; exit 1; }
MACDMG=$(python3 -c "import json;print(json.load(open('site/release.json'))['download_url'].split('/')[-1])")
[ -f "site/downloads/$MACDMG" ] || gh release download "v$(echo "$MACDMG" | sed -E 's/^Vikey-(.*)\.dmg$/\1/')" -R $REPO -p "$MACDMG" -D site/downloads --clobber
OTHERS=$(python3 -c "
import json; d=json.load(open('site/release.json')); out=[]
for plat in ('windows','linux'):
    for k,v in (d.get(plat) or {}).items():
        if isinstance(v,dict) and v.get('download_url','').startswith('/downloads/'): out.append(v['download_url'].split('/')[-1])
print(' '.join(out))")
for f in ${=OTHERS}; do [ -f "site/downloads/$f" ] || gh release download "v$VER" -R $REPO -p "$f" -D site/downloads --clobber || true; done
cp "dist/android/$APK" site/downloads/
python3 - "$VER" "$APK" <<'PY'
import json, hashlib, sys
ver, apk = sys.argv[1:3]; p = "site/release.json"; rel = json.load(open(p))
sha = hashlib.sha256(open(f"dist/android/{apk}", "rb").read()).hexdigest()
rel["android"] = {"version": ver, "download_url": f"/downloads/{apk}", "sha256": sha}
json.dump(rel, open(p, "w"), indent=0); open(p, "a").write("\n")
PY
setopt null_glob; for old in site/downloads/Vikey-*.apk; do case "$old" in *"$VER"*) ;; *) rm "$old";; esac; done
git add site/release.json && git commit -q -m "Release $VER (Android)" && git push -q origin main
Tools/release/deploy_site.sh
echo "==> done: https://vikey.app/download#android"
