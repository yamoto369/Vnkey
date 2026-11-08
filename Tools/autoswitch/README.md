# Auto switch by typing: word lists and tests

The engine (`Engine/Engine.cpp`, "Auto switch language from typing behavior")
decides when a word typed in one mode belongs to the other language. It uses
`engine/AutoSwitchData.h`, generated here.

## Rebuild the word lists

```sh
cd Tools/autoswitch
swiftc -O -o /tmp/spellfilter spellfilter.swift
cut -d' ' -f1 en_50k.txt | grep -E '^[a-z]{2,}$' | /tmp/spellfilter > en_spell_ok.txt
python3 build_dict.py
```

- `vi_50k.txt`, `en_50k.txt`: word frequencies from FrequencyWords by Hermit Dave (OpenSubtitles 2018), CC BY-SA 4.0.
- `en_spell_ok.txt`: the English words the macOS spell checker accepts, lowercase or capitalized. This drops subtitle junk.
- `en_extra.txt`: chat shorthand without apostrophes, plus developer words. These are always treated as English.

## Test

```sh
cd Tools/autoswitch/test
E=../../../Engine
clang++ -std=c++17 -O2 -w -I$E -I$E/platforms -o harness harness.cpp $E/Engine.cpp $E/Vietnamese.cpp $E/Macro.cpp $E/SmartSwitchKey.cpp $E/ConvertTool.cpp
python3 run_cases.py   # regression cases in cases.tsv
python3 bulk.py        # accuracy over the 20k English / 3.4k Vietnamese word lists
./harness bench        # cost of one English-mode check
```

`harness.cpp` drives the real engine and mirrors what `App/Core/VikeyCore.mm` does with its output.

## Lock down behavior (run before and after any rule change)

```sh
Tools/autoswitch/check.sh            # rule cases + golden snapshots + Telex lock + metrics
Tools/autoswitch/check.sh update     # accept the current behavior after reviewing the diffs
EXTRA="-DAS_VN_FLOOR=30" Tools/autoswitch/check.sh   # try a tweak without editing code
```

- `rules/rules.tsv`: every decided rule with tagged cases (see DECISIONS.md).
- `rules/golden/`: snapshots of typing the chat corpus (with and without typos), every syllable in Vietnamese mode, and the metrics.
