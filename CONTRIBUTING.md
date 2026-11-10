# Contributing to Vikey

Thanks for helping improve Vikey for Windows.

## AI-written code is welcome
Use Claude, Copilot, Cursor or anything else. You do not need to say which parts an AI wrote. You do need to understand the change and test it yourself.

## What a pull request needs
1. **Show it.** Include a screenshot or short demo video for new features and behaviour changes. For bug fixes, show the bug.
2. **Before and after.** Describe the previous and new behaviour.
3. **Tests.** Add test cases for the behaviour you changed.
4. **One change per pull request.** Smaller changes are easier to review.

## Where tests go
| Area | Add cases to | Run |
|---|---|---|
| Typing rules | `Tools/autoswitch/rules/rules.tsv`; add a decision to `DECISIONS.md` for new rules | `pwsh Windows/check.ps1` |
| Windows app and keyboard hook | `Windows/Tests/` | `pwsh Windows/check.ps1` |

The Windows validation script checks rule cases, golden snapshots, driver parity, realistic typing and performance. If typing results intentionally change, review and update the golden data only with an explanation in the PR.

## Before opening a PR
- Tests pass on Windows.
- No secrets, keys or personal data in code or screenshots.
- Contributions are released under GPL-3.0.
