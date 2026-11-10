# Vikey

Vietnamese typing for **Windows 10 and Windows 11** (x64 and ARM64).

Vikey uses Telex and automatically switches between Vietnamese and English as you type. It preserves programming terms and variable names, and lets you undo an automatic correction by pressing Delete immediately after it.

## Features
- Telex with modern or classic tone placement (hoà / hòa), spell checking, and Unicode output.
- Automatic English/Vietnamese switching based on the word and its context.
- Programming-aware handling of code, commands, paths and variable names.
- Undo an automatic correction by pressing Delete immediately after it.
- Per-app language memory and optional per-website memory in supported browsers.
- Custom switch shortcut, including modifier-only shortcuts, with an optional sound.
- Dvorak, Colemak and other keyboard layouts.

## Build
Requirements: Windows 10/11, Visual Studio 2022 with Desktop development with C++, CMake 3.21+, Ninja, and Python 3.12+.

```powershell
cmake -S Windows -B Windows/build -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build Windows/build --config Release --target vikey
```

Executable: `Windows/build/App/Vikey.exe`.

## Tests
```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File Windows/check.ps1
```

The suite validates Telex rules, regressions, golden snapshots, driver/simulator parity, undo, realistic typing, touch correction and performance. See [DECISIONS.md](DECISIONS.md).

## Contributing
Pull requests are welcome. Include a screenshot or demo for behaviour changes, describe before/after, and add tests. See [CONTRIBUTING.md](CONTRIBUTING.md).

## License
GPL-3.0. Based on [OpenKey](https://github.com/tuyenvm/OpenKey) by Tuyen Mai. See [LICENSE](LICENSE).
