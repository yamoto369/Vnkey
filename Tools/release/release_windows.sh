#!/bin/zsh
# Publishes the Windows build of a release that the Mac lane (release.sh <version>) already made:
#   Tools/release/release.sh --windows 1.0.14
# The tag v<version> triggers .github/workflows/windows.yml, whose "package" job uploads the installers
# (Vikey-<ver>-x64-Setup.exe, Vikey-<ver>-arm64-Setup.exe) and portable zips to the GitHub release.
# This script waits for them, signs the installers with the Sparkle EdDSA key, puts everything on
# vikey.app/downloads, adds the Windows items to the appcast and release.json, and deploys the site.
set -e
VER=${1:?usage: release.sh --windows <version>}
cd "$(dirname "$0")/../.."
# the version must be the one main is at: the packages on the release were built from it, and the prune below keeps only $VER files
PV=$(grep MARKETING_VERSION project.yml | grep -o '"[^"]*"' | tr -d '"'); [ "$PV" = "$VER" ] || { echo "project.yml is at $PV, not $VER"; exit 1; }
BUILD=$(grep 'CURRENT_PROJECT_VERSION' project.yml | grep -o '[0-9]\+')
REPO=tonyminibot/Vikey
echo "==> waiting for the CI package job on v$VER"
for i in $(seq 1 60); do
  n=$(gh release view "v$VER" -R $REPO --json assets -q '[.assets[].name | select(endswith("-Setup.exe"))] | length' 2>/dev/null || echo 0)
  [ "$n" = "2" ] && break; sleep 30
done
[ "$n" = "2" ] || { echo "installers not on the release after 30 min: gh run list -R $REPO -w windows"; exit 1; }
rm -rf dist/windows && mkdir -p dist/windows site/downloads
gh release download "v$VER" -R $REPO -p "Vikey-$VER-*" -D dist/windows --clobber
# site/downloads is not in git: the deploy must carry the current Mac DMG too, or the Mac download disappears
MACDMG=$(python3 -c "import json;print(json.load(open('site/release.json'))['download_url'].split('/')[-1])")
[ -f "site/downloads/$MACDMG" ] || gh release download "v$(echo "$MACDMG" | sed -E "s/^Vikey-(.*)\.dmg$/\1/")" -R $REPO -p "$MACDMG" -D site/downloads --clobber 2>/dev/null || gh release download "v$VER" -R $REPO -p "$MACDMG" -D site/downloads --clobber
[ -f "site/downloads/$MACDMG" ] || { echo "Mac DMG $MACDMG not found for the site deploy"; exit 1; }
NOTES=$(sed -n "/<!-- release $VER -->/,/<!-- \/release -->/p" site/pages/changelog.html | grep -v '<h2>\|<!--')
for ARCH in x64 arm64; do
  F=dist/windows/Vikey-$VER-$ARCH-Setup.exe
  [ -f "$F" ] || { echo "missing $F"; exit 1; }
  # CI signs with the Certum certificate (secrets CERTUM_EMAIL/CERTUM_OTP); never publish an unsigned installer
  osslsigncode verify "$F" >/dev/null 2>&1 || { echo "$F is not Authenticode-signed (CI secrets missing?)"; [ "$ALLOW_UNSIGNED" = 1 ] || exit 1; }
  SIG=$(~/.tools/sparkle/bin/sign_update --ed-key-file ~/.secrets/vikey_sparkle_ed25519 "$F")
  cp "$F" dist/windows/Vikey-$VER-$ARCH-portable.zip site/downloads/
  python3 Tools/release/appcast.py windows "$VER" "$BUILD" "$ARCH" "https://vikey.app/downloads/Vikey-$VER-$ARCH-Setup.exe" "$SIG" "$NOTES"
done
python3 - "$VER" <<'PY'
import json, hashlib, sys
ver = sys.argv[1]; p = "site/release.json"; rel = json.load(open(p))
sha = lambda f: hashlib.sha256(open(f, "rb").read()).hexdigest()
rel["windows"] = {a: {"download_url": f"/downloads/Vikey-{ver}-{a}-Setup.exe", "portable_url": f"/downloads/Vikey-{ver}-{a}-portable.zip",
                      "sha256": sha(f"site/downloads/Vikey-{ver}-{a}-Setup.exe")} for a in ("x64", "arm64")}
rel["windows"]["version"] = ver
json.dump(rel, open(p, "w"), indent=0); open(p, "a").write("\n")
PY
setopt null_glob; for old in site/downloads/Vikey-*-Setup.exe site/downloads/Vikey-*-portable.zip; do case "$old" in *"-$VER-"*) ;; *) rm "$old";; esac; done
git add site/release.json site/appcast.xml && git commit -q -m "Release $VER (Windows)" && git push -q origin main
Tools/release/deploy_site.sh
echo "==> done: https://vikey.app/download"
