# Vikey brand guideline

One small set of rules, used the same way on the website, the Mac app, the iOS app and anything we publish.
Code that implements it: `App/Model/Brand.swift` (Mac + iOS), `site/style.css` (`--blue`, `.brand`).

## Name and tagline
- **Vikey**: capital V, the rest lowercase, one word. Never "ViKey", "VIKEY" or "Vi Key".
- Tagline: **Vikey - Gõ tiếng Việt** (store listings, page titles, og:site_name, the app's display name where it fits).
- In running text the name is plain text in the surrounding font and color: "Vikey tự chuyển…".

## Wordmark
- "Vi" in ink, "key" in brand indigo, same font, **bold (700)**, no letter-spacing, never all caps.
- Use it wherever the name stands alone as a logo: website header, demo labels, app window/screen headers, About.
- Usually next to the icon, gap ≈ 0.3 × the text height.

| | Light | Dark |
|---|---|---|
| "Vi" (ink) | `#111318` | white / label color |
| "key" (brand indigo) | `#4F46E5` | `#8C87FF` |

## Colors
- **Brand indigo** `#4F46E5` (dark mode `#8C87FF`), hover/pressed `#4338CA`. The only accent color: buttons, links, the wordmark, highlights.
- Ink `#111318`, muted text `#5B6472`, lines `#E6EAF0`, chip/tint background `#EEF0FF` (or brand at 14% on any background).
- Status only: green for "done/added", red `#D14343` for "wrong" (demo: red dotted/wavy underline on the plain keyboard's wrong words).
- **No pink, no second accent, no gradients on text.** A soft indigo glow behind hero art is fine.

## Typography
- The system font everywhere: SF Pro on Apple platforms, `-apple-system, BlinkMacSystemFont, "Helvetica Neue", Inter, sans-serif` on the web. Monospace: SF Mono / Menlo.
- Headlines 800 (web) / bold (apps); body regular. Short lines, no orphans, no split phrases ([[feedback_no_weird_line_breaks]]).

## Icon
- `design/Vikey.icns` / AppIcon: white "Vi" on an indigo rounded square. Don't recolor, outline or put it on a busy background.
- Small sizes: 16-34 px with a radius of ~22% of the size.

## Voice
- Vietnamese first, English as the second language of every page and app (D31).
- Short, concrete, no hype: say what happens ("gõ "treen" ra "trên""), not adjectives.
