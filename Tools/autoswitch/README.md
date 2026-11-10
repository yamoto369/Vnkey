# Auto switch by typing: word lists and tests

The shared C++ engine (`Engine/Engine.cpp`) decides when a word typed in one
language mode belongs to the other language. Its frequency tables are generated
into `Engine/AutoSwitchData.h`.

## Word-list inputs

- `vi_50k.txt`, `en_50k.txt`: word frequencies from FrequencyWords by Hermit Dave (OpenSubtitles 2018), CC BY-SA 4.0.
- `en_spell_ok.txt`: checked-in English allowlist used to filter subtitle noise. It is reviewed data, not tied to an OS spell-check API.
- `en_extra.txt`: chat shorthand and developer vocabulary always treated as English.
- `en_code.txt`: programming words and identifiers.

## Regenerate engine data (Windows)

Run from the repository root in PowerShell:

```powershell
python Tools/autoswitch/build_dict.py
python Tools/autoswitch/build_forms.py C:\path\to\hunspell-en_US
```

The second command is optional when English word forms have not changed. Its input
directory must contain `en_US.dic` and `en_US.aff`; install Hunspell for Windows
and make sure `unmunch.exe` is available on `PATH`.

## Validate the engine (Windows)

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File Windows/check.ps1
```

This builds the shared engine harness, simulator, driver tests and touch simulator
with MSVC, then runs rule cases, reported-bug regressions, golden snapshots,
driver/simulator parity, realistic typing, undo, touch correction and performance
checks.

- `rules/rules.tsv`: decided rule cases (see `DECISIONS.md`).
- `rules/golden/`: locked output snapshots and quality baselines.
- `test/cases.tsv`: additional engine test cases.

After changing a rule, add regression cases and review the golden-data diff before
accepting any new baseline.
