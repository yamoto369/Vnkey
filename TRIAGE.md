# Triage

How pull requests and issues on Vikey are labelled, checked and closed. Write every comment for the person who opened the PR or issue.

## Labels

**Area**
| Label | Covers |
|---|---|
| `area:windows` | `Windows/`, keyboard hook, tray app and installer |
| `area:shared` | `Engine/`, shared typing engine and test fixtures |
| `area:tooling` | `Tools/`, `.github/`, website and docs |

**Kind**
| Label | Meaning |
|---|---|
| `correctness` | Crash, wrong output, lost text, security or privacy issue |
| `taste` | Design or behaviour choice; maintainer decides |

**Reproduction**
| Label | Meaning |
|---|---|
| `reproducible` | Reproduced on the released Windows build or confirmed from current code |
| `not-reproducible` | Attempted on the released Windows build but not reproduced |
| `outdated` | Already fixed or no longer applies |

Do not add a reproduction label without an actual attempt.

## Issues
1. Read the issue and comments; check merged PRs and releases.
2. Close duplicates, spam, unrelated reports, or already-fixed issues with a short explanation.
3. Add area and kind labels.
4. Reproduce on the released Windows build, preferably Windows 10 and 11.
5. Ask for missing version, steps or app details in the reporter's language.
6. Leave taste decisions to the maintainer.

## Pull requests
1. Add area and kind labels.
2. Review correctness, edge cases, [DECISIONS.md](DECISIONS.md), CI, signing, updater and downloaded code.
3. Require a screenshot/demo for feature or behaviour changes.
4. Review contributor code; do not run it on maintainer machines.
5. Only the maintainer merges.
