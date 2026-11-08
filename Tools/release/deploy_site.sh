#!/bin/zsh
# Builds site/dist and deploys it to Cloudflare Pages (project "vikey", domain vikey.app), with site/functions
# (the /api/notify form -> D1 "vikey-notify", see site/wrangler.toml). BRANCH=preview deploys a preview instead.
set -e
cd "$(dirname "$0")/../.."
# site/downloads is not in git: fetch any file release.json points to that this checkout lacks, so a deploy
# from any worktree never drops another platform's download
python3 - <<'PY' | while read -r tag f; do gh release download "$tag" -R tonyminibot/Vikey -p "$f" -D site/downloads --clobber; done
import json, os, re
def urls(o):
    if isinstance(o, dict):
        for v in o.values(): yield from urls(v)
    elif isinstance(o, str) and o.startswith("/downloads/"): yield o
for u in urls(json.load(open("site/release.json"))):
    f = u.split("/")[-1]; m = re.match(r"vikey[-_](\d+(?:\.\d+)+)", f, re.I)
    if m and not os.path.exists(f"site/downloads/{f}"): print(f"v{m.group(1)} {f}")
PY
python3 site/build.py
set -a; . ~/.secrets/cloudflare_dedicated; set +a
cd site && npx --yes wrangler@latest pages deploy --branch "${BRANCH:-main}" --commit-dirty=true | tail -2
