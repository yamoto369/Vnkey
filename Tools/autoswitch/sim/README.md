# Mixed-text typing simulator

The simulator types Vietnamese/English corpora into the real shared engine,
key by key, and measures language-switch quality and text safety.

## Corpora

- `corpus/chat_messages.txt`: 1,869 chat-style messages (21k words), generated with `corpus/gen_chat.py`.
- Viblo tech articles (269k words): not committed (third-party content). Recreate with
  `corpus/fetch_viblo.py` then `corpus/clean_viblo.py`.
- `corpus/synthetic_val.txt`: synthetic validation corpus maintained with the project.

## Build and run (Windows)

From the repository root, configure and build the simulator with MSVC:

```powershell
cmake -S Windows -B Windows/build -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build Windows/build --config Release --target sim rtsim
python Tools/autoswitch/sim/prep.py Tools/autoswitch/sim/corpus/chat_messages.txt > chat.units
Tools/autoswitch/sim/sim.exe chat.units
Tools/autoswitch/sim/sim.exe chat.units --lazy
Tools/autoswitch/sim/sim.exe chat.units --off
```

The complete validation suite is:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File Windows/check.ps1
```

## Realistic typing

`rtsim units.txt [--seed N] [--show N] [--why KIND] [--only MSG] [--trace]`
simulates typos corrected with Backspace, cursor movement, clicks, edits earlier
in the text, and manual language-switch round trips. It checks that the engine
never changes text before a cursor move, click or switch. The validation suite
runs three seeds and locks slip-recovery quality in `rules/golden/rtsim.json`.

## Quality scorecard

`Tools/quality/scorecard.py [--compare REF]` reports accuracy and manual switches,
text safety and slip recovery, auto-correct, speed, locked edge cases and reported
bugs fixed. CI runs the Windows engine validation before building x64 and ARM64.
