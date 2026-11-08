#!/usr/bin/env python3
"""Packs vikey-data's next-word table (predict/next_words.tsv: context<TAB>word count<TAB>...) into the keyboard's
iOS/Keyboard/NextWords.tsv: context<TAB>w1<TAB>w2<TAB>w3, byte-sorted (NextWords.swift binary-searches it).
Built from FineWeb-2, HPLT, MADLAD-400, FineWeb and Reddit word counts (see ACKNOWLEDGEMENTS.md)."""
import os, unicodedata
SRC = os.path.expanduser("~/Work/vikey-data/predict/next_words.tsv")
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "iOS", "Keyboard", "NextWords.tsv")
rows = []
for line in open(SRC, encoding="utf-8"):
    parts = line.rstrip("\n").split("\t")
    ctx = unicodedata.normalize("NFC", parts[0])
    words = [p.rsplit(" ", 1)[0] for p in parts[1:4] if p]
    if words: rows.append((ctx.encode(), "\t".join([ctx] + words)))
rows.sort(key=lambda r: r[0])
with open(OUT, "w", encoding="utf-8") as f:
    for _, r in rows: f.write(r + "\n")
print(len(rows), "contexts ->", os.path.relpath(OUT), os.path.getsize(OUT) // 1024, "KB")
