# Project Status — Schotten Totten (LO02 A26)

Summary of what is implemented, what works and what remains imperfect, as required by [SPEC p.3].

## 1. Implemented and working

### Game rules

- Base game: 54 Clan cards, 9 Stones, hands of 6, turn state machine, formations, normal and early claims, 3-adjacent and 5-Stone victories.
- Tactic variant: separate Tactic deck, hands of 7, deck choice when drawing, passing, special-card counter, all 10 Tactic cards (Joker ×2, Spy, Shield-Bearer, Blind-Man's Bluff, Mud Fight, Recruiter, Strategist, Banshee, Traitor), public discard pile.
- Early claims proved from public cards only (board and discard), with an exhaustive search that never awards an unprovable claim.
- Multi-round matches: configurable number of rounds (1–9), scoring 5 / stones controlled, standings between rounds.
- Expansion "Highland Muster": Highland Scout, Stone Ward, Clan Exchange (two copies each), opted in with "Include expansion cards?".
- Experts variant: **not** implemented (excluded by the specification).

### Architecture

- MVC with two competing views on one live `GameSession` (GUI and CLI).
- Engine free of JavaFX; card behaviour plugged in through an `EffectRegistry`.
- Strategy pattern for Easy and Hard virtual players (`PlayerView` hides hands and deck order).
- Justified Singleton `GameSettings` for preferences; per-game rules travel in `GameConfiguration`.
- Versioned JSON saves (format version 1) with round-trip tests, including pending effects and match scores.

### Interfaces

- Premium JavaFX GUI: main menu, set-up, board, rules and tutorial, settings, load, hot-seat curtain, victory, mute, pause / reveal / pace for computer matches, keyboard shortcuts, drag-and-drop, reduce motion, sound and music.
- Complete CLI (`help` lists every command).
- Shared session: `mvn javafx:run -Djavafx.args=--with-cli`, verified by an automated test that plays one move on each side.

### Documentation and delivery

- Rules specification, expansion rules, requirements traceability, design evolution, assets provenance, this status report, test report, UML (PlantUML + PNG/SVG), Javadoc (application, packages, classes; `javadoc -Xdoclint:all` = 0 warnings).
- Packaging scripts: `scripts/package-submission.sh`, `scripts/build-distribution.sh`.

## 2. Verified

| Area | Evidence |
|---|---|
| Automated tests | 136 runs, 0 failures with GUI playthroughs; critical packages ≈ 94 % line coverage (see `test-report.md`) |
| Every Tactic and expansion card | Unit tests; GUI playthroughs played every special card except Strategist through the widgets |
| Shared GUI + CLI session | `GuiPlaythroughTest#commandLineAndWindowPlayTheSameGame` |
| Visual QA | Screenshot tour at 1440×900 and 1280×720 (`docs/screenshots/`) |
| Build | `mvn package` and `javadoc:javadoc` succeed on macOS arm64 with OpenJDK 21 |

## 3. Not implemented (by design)

| Item | Reason |
|---|---|
| Experts variant | Explicitly excluded by [SPEC p.1] |
| Phase 1 initial class diagram | Not supplied to us; stated in `design-evolution.md` rather than invented |

## 4. Known limitations and minor issues

1. **ZIP file name A25 vs A26.** The A26 specification prints `XXX_YYY_Project_LO02_A25.zip`. The packaging script keeps **A25** by default and prints a warning. Confirm the expected suffix with the teacher before Moodle upload (`--term A26` produces the other name).
2. **Landscape backgrounds** are 1280×720; on large Retina screens they look slightly soft. Card art (540×720) is sharp.
3. **Overlays** (menus, dialogs, victory) are not scaled when the window is shorter than their design height; the board and the side rail are.
4. **CLI `speed` command** hard-codes the four delays instead of reading `GameSettings.Speed`, to avoid a `cli` → `app` dependency.
5. **Victory ribbon** always shows the word "Victory", coloured by the winning clan; the reason and the score table sit underneath.
6. **Strategist** was not drawn by the hint AI during the recorded GUI playthroughs; its interaction is the same shape as Traitor and is covered by unit tests.
7. **Drag-and-drop** works but is not driven by the automated GUI tests (they click).
8. **Deadlock rule** (two consecutive passes end the round) is a documented custom rule that prevents infinite games.

## 5. Rule assumptions (documented in `rules-specification.md`)

Notable INTERPRETATION / CUSTOM decisions: runs do not wrap around; Mud Fight keeps the five formation ranks with four cards; Blind-Man's Bluff + Mud Fight together means four cards compared by sum; early-claim proof treats unplayed Elite Troops as available to the opponent and ignores Combat Modes the opponent might still play; Traitor may place the stolen card on the same Stone; Recruiter draws what exists when fewer than three cards remain; consecutive passes end a round.

## 6. Open action for the team

Confirm with the teacher whether the Moodle archive must be named `…_A25.zip` (as printed) or `…_A26.zip` (to match the document title), then run:

```bash
scripts/package-submission.sh LASTNAME1 LASTNAME2          # default A25
# or
scripts/package-submission.sh LASTNAME1 LASTNAME2 --term A26
```
