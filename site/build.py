#!/usr/bin/env python3
"""Builds site/dist from site/pages/*.html by wrapping each page in the shared layout (nav + footer).
No dependencies. Run: python3 site/build.py
Demo frames (assets/demo.json) come from the real engine: Tools/autoswitch/test/harness frames <lang> <keys>."""
import os, re, shutil, datetime, json

ROOT = os.path.dirname(os.path.abspath(__file__))
PAGES = os.path.join(ROOT, "pages")
PAGES_EN = os.path.join(ROOT, "pages_en")   # English (D31), same file names
DIST = os.path.join(ROOT, "dist")
YEAR = datetime.date.today().year
SITE = "https://vikey.app"
GITHUB = "https://github.com/yamoto369/Vnkey"

T = {   # layout text per language (D31: Vietnamese at /, English at /en/)
    "vi": dict(features="Chức năng", faq="Câu hỏi", changelog="Phiên bản", download="Tải về", home="Trang chủ", support="Hỗ trợ",
               faq_long="Câu hỏi thường gặp", bugs="Báo lỗi", feedback="Gửi góp ý", privacy="Quyền riêng tư", ack="Ghi nhận", source="Mã nguồn",
               license="Giấy phép GPL-3.0", switch="EN", switch_title="English", sitename="Vikey - Gõ tiếng Việt"),
    "en": dict(features="Features", faq="FAQ", changelog="Changelog", download="Download", home="Home", support="Support",
               faq_long="FAQ", bugs="Report a bug", feedback="Send feedback", privacy="Privacy", ack="Acknowledgements", source="Source code",
               license="GPL-3.0 license", switch="VI", switch_title="Tiếng Việt", sitename="Vikey - Gõ tiếng Việt"),
}

FLAG_VN = '<svg class="flag" viewBox="0 0 30 20" aria-hidden="true"><rect width="30" height="20" fill="#DA251D"/><polygon fill="#FFFF00" points="15,4 16.18,7.62 19.98,7.62 16.9,9.86 18.08,13.48 15,11.24 11.92,13.48 13.1,9.86 10.02,7.62 13.82,7.62"/></svg>'
def flag_gb(i):   # Union Jack; ids unique per copy (the switch appears twice per page)
    return (f'<svg class="flag" viewBox="0 0 60 30" aria-hidden="true"><clipPath id="gbs{i}"><path d="M0,0v30h60v-30z"/></clipPath>'
            f'<clipPath id="gbt{i}"><path d="M30,15h30v15zv15h-30zh-30v-15zv-15h30z"/></clipPath><g clip-path="url(#gbs{i})">'
            '<path d="M0,0v30h60v-30z" fill="#012169"/><path d="M0,0L60,30M60,0L0,30" stroke="#fff" stroke-width="6"/>'
            f'<path d="M0,0L60,30M60,0L0,30" clip-path="url(#gbt{i})" stroke="#C8102E" stroke-width="4"/>'
            '<path d="M30,0v30M0,15h60" stroke="#fff" stroke-width="10"/><path d="M30,0v30M0,15h60" stroke="#C8102E" stroke-width="6"/></g></svg>')

def nav(lang, pre, other):
    t = T[lang]
    def sw(i):   # the flag of the language it switches TO, so it's easy to spot
        flag = flag_gb(i) if lang == "vi" else FLAG_VN
        return f'<a class="lang" href="{other}" hreflang="{"en" if lang == "vi" else "vi"}" title="{t["switch_title"]}">{flag}{t["switch"]}</a>'
    return f"""<header class="nav"><div class="wrap">
<a class="brand" href="{pre}/"><img src="/assets/icon.png" alt="" width="34" height="34"><span>Vi<b>key</b></span></a>
<nav><a href="{pre}/#features">{t["features"]}</a><a href="{pre}/faq">{t["faq"]}</a><a href="{pre}/changelog">{t["changelog"]}</a><a href="{GITHUB}">GitHub</a>{sw(1)}<a class="btn dl-direct" data-cta="short" href="{pre}/download">{t["download"]}</a></nav>
<div class="nav-m">{sw(2)}<button class="menu-btn" type="button" aria-label="Menu" aria-expanded="false" aria-controls="mnav"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 7h16M4 12h16M4 17h16"/></svg></button></div>
</div>
<nav class="mnav" id="mnav" hidden><a href="{pre}/download">{t["download"]}</a><a href="{pre}/#features">{t["features"]}</a><a href="{pre}/faq">{t["faq"]}</a><a href="{pre}/changelog">{t["changelog"]}</a><a href="{GITHUB}">GitHub</a></nav>
</header>
<script>(()=>{{const b=document.querySelector(".menu-btn"),m=document.getElementById("mnav");if(!b)return;b.onclick=()=>{{const o=m.hidden;m.hidden=!o;b.setAttribute("aria-expanded",o)}};addEventListener("keydown",e=>{{if(e.key==="Escape"){{m.hidden=true;b.setAttribute("aria-expanded",false)}}}})}})()</script>
"""

def footer(lang, pre):
    t = T[lang]
    return f"""<footer class="foot"><div class="wrap cols">
<div class="col"><p class="muted">© {YEAR} Tony Dinh</p></div>
<div class="col"><h4>Vikey</h4><a href="{pre}/">{t["home"]}</a><a href="{pre}/#features">{t["features"]}</a><a href="{pre}/download">{t["download"]}</a><a href="{pre}/changelog">{t["changelog"]}</a></div>
<div class="col"><h4>{t["support"]}</h4><a href="{pre}/faq">{t["faq_long"]}</a><a href="{pre}/feedback">{t["feedback"]}</a><a href="{GITHUB}/issues">{t["bugs"]}</a><a href="{pre}/privacy">{t["privacy"]}</a><a href="{pre}/acknowledgements">{t["ack"]}</a></div>
<div class="col"><h4>{t["source"]}</h4><a href="{GITHUB}">GitHub</a><a href="{GITHUB}/blob/main/LICENSE">{t["license"]}</a><a href="https://x.com/tdinh_me">@tdinh_me</a></div>
</div></footer>"""

HEAD = """<!doctype html><html lang="{lang}"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{title}</title>
<meta name="description" content="{description}">
<link rel="icon" href="/assets/icon.png"><link rel="apple-touch-icon" href="/assets/icon.png">
<meta property="og:site_name" content="Vikey - Gõ tiếng Việt"><meta name="application-name" content="Vikey - Gõ tiếng Việt"><meta property="og:title" content="{title}"><meta property="og:description" content="{description}">
<meta property="og:image" content="{site}/assets/og.png"><meta property="og:image:width" content="1200"><meta property="og:image:height" content="630"><meta property="og:type" content="website"><meta property="og:url" content="{site}{path}">
<meta name="twitter:card" content="summary_large_image"><meta name="twitter:image" content="{site}/assets/og.png">
{canonical}
{alternates}
<link rel="stylesheet" href="/style.css?v={ver}">
</head><body{bodyclass}>"""

def release():
    p = os.path.join(ROOT, "release.json")
    return json.load(open(p)) if os.path.exists(p) else {}

PTXT = {"vi": dict(dl="Tải bản Windows", arm="Bản ARM64", soon="Chưa có bản tải"),
        "en": dict(dl="Download for Windows", arm="ARM64 build", soon="Not available")}

def downloads(rel):
    """Windows release assets only; None means no Windows build has been published yet."""
    win = rel.get("windows") or {}
    x64, arm = win.get("x64") or {}, win.get("arm64") or {}
    if not x64.get("download_url"): return {"windows": None}
    return {"windows": {"url": x64["download_url"], "arm": arm.get("download_url"),
                        "portable": x64.get("portable_url"), "portable_arm": arm.get("portable_url"),
                        "sha256": x64.get("sha256"), "sha256_arm": arm.get("sha256"),
                        "version": win.get("version") or rel.get("version")}}

def os_icon(p, cls="ic"):
    return f'<svg class="{cls}" aria-hidden="true"><use href="/assets/os.svg#{p}"/></svg>'

def platform_cards(lang, dl):
    t = PTXT[lang]; d = dl.get("windows")
    shot = '<img class="pshot zoom" src="/assets/shots/dl-windows.webp" width="1200" height="900" alt="Vikey for Windows" loading="lazy">'
    if d:
        btn = f'<a class="btn dl" href="{d["url"]}" download>{t["dl"]} (x64)</a>'
        if d.get("arm"): btn += f'<a class="alt" href="{d["arm"]}" download>{t["arm"]}</a>'
        links = []
        if d.get("portable"): links.append(f'<a href="{d["portable"]}" download>x64 (zip)</a>')
        if d.get("portable_arm"): links.append(f'<a href="{d["portable_arm"]}" download>ARM64 (zip)</a>')
        if links: btn += '<span class="alt">' + " · ".join(links) + "</span>"
        badge = ""
    else:
        btn = f'<a class="btn ghost dl" href="#windows">{t["soon"]}</a>'
        badge = f'<span class="badge">{t["soon"]}</span>'
    return f'<div class="pgrid"><section class="pcard{" pending" if not d else ""}" id="windows" data-os="windows">{shot}{badge}{os_icon("windows", "picon")}<h2>Windows</h2><p>Windows 10 and 11 · x64 and ARM64</p>{btn}</section></div>'

def platform_row(lang, pre, dl):
    return f'<div class="plats"><a href="{pre}/download#windows" title="Windows">{os_icon("windows")}<span>Windows</span></a></div>'

def build():
    rel = release()
    if os.path.exists(DIST): shutil.rmtree(DIST)
    shutil.copytree(os.path.join(ROOT, "assets"), os.path.join(DIST, "assets"))
    if os.path.isdir(os.path.join(ROOT, "downloads")):
        shutil.copytree(os.path.join(ROOT, "downloads"), os.path.join(DIST, "downloads"))
    shutil.copy(os.path.join(ROOT, "style.css"), DIST)
    if rel: shutil.copy(os.path.join(ROOT, "release.json"), DIST)
    ver = str(int(os.path.getmtime(os.path.join(ROOT, "style.css"))))
    for lang, srcdir, pre in (("vi", PAGES, ""), ("en", PAGES_EN, "/en")):
      for name in sorted(os.listdir(srcdir)):
        if not name.endswith(".html"): continue
        src = open(os.path.join(srcdir, name), encoding="utf-8").read()
        meta = dict(re.findall(r"<!--\s*(\w+):\s*(.*?)\s*-->", "\n".join(src.split("\n")[:6])))
        body = re.sub(r"^(<!--.*?-->\s*)+", "", src, flags=re.S)
        slug = name[:-5]
        rel_path = "/" if slug == "index" else "/" + slug
        path = pre + rel_path if pre else rel_path
        vi_path, en_path = rel_path, "/en" + rel_path
        other = en_path if lang == "vi" else vi_path
        canonical = '<meta name="robots" content="noindex">' if meta.get("noindex") else f'<link rel="canonical" href="{SITE}{path}">'
        alternates = f'<link rel="alternate" hreflang="vi" href="{SITE}{vi_path}"><link rel="alternate" hreflang="en" href="{SITE}{en_path}"><link rel="alternate" hreflang="x-default" href="{SITE}{vi_path}">'
        one = meta.get("layout") == "one"
        page = HEAD.format(lang=lang, title=meta.get("title", "Vikey"), description=meta.get("description", ""), site=SITE, path=path, ver=ver,
                           canonical=canonical, alternates=alternates, bodyclass=' class="one-page"' if one else "")
        dl = downloads(rel)
        body = body.replace("<!-- PLATFORMS -->", platform_cards(lang, dl)).replace("<!-- PLATFORM_ROW -->", platform_row(lang, pre, dl))
        script = f'<script>window.VK_DL={json.dumps(dl, separators=(",", ":"))}</script><script src="/assets/windows.js" defer></script>'
        page += nav(lang, pre, other) + body + ("" if one else footer(lang, pre)) + script + "</body></html>\n"
        win = rel.get("windows") or {}
        subst = {"VERSION": rel.get("version"), "WIN_VERSION": win.get("version"),
                 "WIN_X64_URL": (win.get("x64") or {}).get("download_url"), "WIN_ARM64_URL": (win.get("arm64") or {}).get("download_url"),
                 "WIN_X64_ZIP": (win.get("x64") or {}).get("portable_url"), "WIN_ARM64_ZIP": (win.get("arm64") or {}).get("portable_url"),
                 "WIN_X64_SHA": (win.get("x64") or {}).get("sha256"), "WIN_ARM64_SHA": (win.get("arm64") or {}).get("sha256")}
        if not win: page = re.sub(r'<!-- windows -->.*?<!-- /windows -->', "", page, flags=re.S)
        for k in sorted(subst, key=len, reverse=True):   # WIN_VERSION before VERSION
            if subst[k]: page = page.replace(k, subst[k])
        # cache-bust assets: /assets/x.png -> /assets/x.png?v=<mtime>, so browsers never show a stale screenshot
        def bust(m):
            f = os.path.join(ROOT, m.group(1).lstrip("/"))
            return m.group(0) if not os.path.exists(f) else f'{m.group(1)}?v={int(os.path.getmtime(f))}"'
        page = re.sub(r'(/assets/[\w./-]+\.(?:png|json|svg|jpg|js))"', bust, page)
        outdir = os.path.join(DIST, "en") if pre else DIST
        os.makedirs(outdir, exist_ok=True)
        out = os.path.join(outdir, "index.html" if slug == "index" else f"{slug}.html")
        open(out, "w", encoding="utf-8").write(page)
    open(os.path.join(DIST, "_redirects"), "w").write("/features /#features 301\n/en/features /en/#features 301\n")   # the features page moved onto the home page
    open(os.path.join(DIST, "_headers"), "w").write("/*\n  X-Content-Type-Options: nosniff\n  Referrer-Policy: strict-origin-when-cross-origin\n/downloads/*\n  Content-Disposition: attachment\n")
    open(os.path.join(DIST, "robots.txt"), "w").write(f"User-agent: *\nAllow: /\nSitemap: {SITE}/sitemap.xml\n")
    pages = [n[:-5] for n in os.listdir(PAGES) if n.endswith(".html") and n != "404.html"]
    urls = "".join(f"<url><loc>{SITE}{pre}{'/' if p == 'index' else '/' + p}</loc></url>" for pre in ("", "/en") for p in sorted(pages))
    open(os.path.join(DIST, "sitemap.xml"), "w").write(f'<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">{urls}</urlset>\n')
    print("built", len(os.listdir(DIST)), "entries into site/dist")

if __name__ == "__main__":
    build()
