# Test Report

Real results from the machine that builds the project (macOS Apple Silicon, OpenJDK 21.0.13, Maven 3.10.1). Numbers are not invented.

## 1. How to reproduce

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# Engine, CLI, AI and persistence (GUI playthroughs skipped)
mvn test

# Full suite, including the JavaFX playthroughs (needs a display, about 3 minutes)
mvn test -Dschotten.gui.tests=true
```

Surefire writes `schotten.settings` to `target/test-settings.json`, so the tests never touch the player's preferences. The GUI playthroughs also redirect saves to a temporary directory.

## 2. Results (2026-11-17)

### Without GUI playthroughs

```
Tests run: 135, Failures: 0, Errors: 0, Skipped: 4
BUILD SUCCESS
```

The four skipped tests are the methods of `view.GuiPlaythroughTest` (opt-in).

### With GUI playthroughs (`-Dschotten.gui.tests=true`)

```
Tests run: 136, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

(136 = 131 non-GUI methods + 5 GUI methods; the hot-seat test is parameterised over two seeds, so Surefire reports five runs for that class.)

| Class | Runs | Time | Notes |
|---|---:|---:|---|
| `model.CardCatalogTest` | 6 | < 0.1 s | |
| `rules.FormationEvaluatorTest` | 20 | < 0.1 s | |
| `rules.ClaimResolverTest` | 13 | < 0.1 s | |
| `engine.GameSetupTest` | 6 | < 0.1 s | |
| `engine.GameEngineTest` | 19 | < 0.1 s | |
| `cards.TacticCardsTest` | 14 | < 0.1 s | Every Tactic card |
| `cards.ExpansionCardsTest` | 8 | < 0.1 s | Every expansion card |
| `ai.StrategyTest` | 13 | ~ 1.5 s | Easy vs Hard, reproducibility, hidden info |
| `persistence.SaveManagerTest` | 6 | < 0.1 s | |
| `controller.GameSessionTest` | 5 | < 0.1 s | Two views, concurrency, AI driver |
| `controller.ControllerServicesTest` | 6 | < 0.2 s | |
| `app.GameSettingsTest` | 3 | < 0.2 s | |
| `cli.CommandLineViewTest` | 12 | ~ 0.3 s | Full CLI playthroughs |
| `view.GuiPlaythroughTest` | 5 | ~ 169 s | See §3 |

## 3. GUI playthroughs

`GuiPlaythroughTest` opens a real JavaFX window and plays only through the widgets a player uses (hand cards, Stones, claim buttons, draw piles, dialogs, the hot-seat curtain, the victory screen, Pause / Show hands / Mute / pace). Human moves are the Hard AI's hint suggestions, so every kind of card interaction that strategy picks is exercised.

| Test | What it verified | Interface actions | Special cards played through the UI |
|---|---|---:|---|
| `humanBeatsOrLosesAgainstComputerInTacticExpansionMatch` (seed 31, 2 rounds, Hard) | Match ends; 2 round results | 195 | Banshee, Blind-Man's Bluff, Shield-Bearer, Spy |
| `hotSeatTacticExpansionMatchFinishes` (seeds 8 and 19, 2 rounds) | Curtain hides both hands; more than 10 hand-overs | 321 / 333 | Banshee, Clan Exchange, Highland Scout, Joker, Mud Fight, Recruiter, Shield-Bearer, Spy, Stone Ward, Traitor (union across seeds) |
| `baseGameAgainstEasyComputer` (seed 77, 1 round) | Base game against Easy | 85 | (none) |
| `computerVsComputerCanBeWatchedPausedRevealedAndMuted` | Pause freezes the game; Show hands reveals cards; Mute / Unmute flip sound and music; pace slider to Instant; match finishes | — | — |
| `commandLineAndWindowPlayTheSameGame` | A card typed on the CLI appears on Stone 5 in the window; ending the turn in the window is printed on the CLI; a card clicked in the window is printed on the CLI | — | — |

Across the human playthroughs, every Tactic and expansion card except Strategist was played through the UI. Strategist uses the same target-then-destination flow as Traitor (covered by the Traitor tour steps and by `TacticCardsTest`).

## 4. Coverage (JaCoCo)

### Critical logic (engine, rules, model, cards, AI, controller, persistence)

These packages are what the brief asks for (≥ 80 % of critical domain and rule logic). Figures with the GUI playthroughs included:

| Package | Lines | Branches |
|---|---:|---:|
| `rules` | 97.2 % | 98.0 % |
| `model` | 95.7 % | 81.9 % |
| `engine` | 94.8 % | 87.9 % |
| `engine.action` | 100 % | 75.0 % |
| `ai` | 94.8 % | 90.4 % |
| `controller` | 92.9 % | 84.2 % |
| `cards` | 92.2 % | 85.5 % |
| `persistence` | 88.4 % | 73.6 % |
| `util` | 95.0 % | 75.0 % |
| **Critical total** | **≈ 94 %** | **≈ 86 %** |

Without the GUI playthroughs the same packages stay above 88 % lines / 73 % branches; the critical total is essentially unchanged because those packages are covered by the non-GUI suite.

### Whole project

| Suite | Lines | Branches |
|---|---:|---:|
| Without GUI playthroughs | 65.4 % (3957 / 6054) | 58.1 % |
| With GUI playthroughs | 75.7 % (4580 / 6054) | 70.5 % |

The overall figure is pulled down by the screens that the playthroughs do not open for long (`view.screens` at 39.6 % lines with GUI tests; Menu, Setup, Rules, Settings and Load are covered mainly by the screenshot tour). `view` itself (ScreenManager, SoundManager, Dialogs, ScreenshotTour) sits at 50.6 % because the tour is not a JUnit test.

## 5. Other checks

| Check | Result |
|---|---|
| `mvn -q compile` with `-Xlint:all` | Success |
| `javadoc -Xdoclint:all` on every package | **0 warnings** |
| `grep javafx` outside `view` and `app.Launcher` | Empty (the engine never depends on JavaFX) |
| TODO / FIXME / commented-out obsolete statements in `src` | None |
| Screenshot tour (`-Dschotten.screenshots=…`) | 26 PNGs (13 screens × 1440×900 and 1280×720); copies in `docs/screenshots/` |
| Shared GUI + CLI session | Verified by `GuiPlaythroughTest#commandLineAndWindowPlayTheSameGame` and `CommandLineViewTest#twoViewsShareTheSameLiveGame` |

## 6. Screenshots used for visual QA

JPEG copies of the tour, at 1440×900 unless noted:

| File | Screen |
|---|---|
| `01-main-menu.jpg`, `01-main-menu-1280x720.jpg` | Main menu |
| `02-setup.jpg` | New-game set-up (including "Include expansion cards?") |
| `03-board.jpg`, `03-board-1280x720.jpg` | Mid-game board (Tactic + expansion) |
| `04-tactic-traitor-target.jpg` | Traitor: choose the opponent's card |
| `05-tactic-traitor-destination.jpg` | Traitor: choose the destination Stone |
| `06-hot-seat-curtain.jpg` | Hot-seat hand-over |
| `07-victory.jpg` | Victory overlay |
| `08-rules.jpg` … `10-rules-formations.jpg` | Rules and tutorial |
| `11-settings.jpg` | Settings |
| `12-game-menu.jpg` | In-game menu |
| `13-saved-games.jpg` | Saved games |

## 7. Gaps that remain

- Strategist was not played through the GUI playthroughs (same interaction shape as Traitor; covered by unit tests).
- Menu, Setup, Rules, Settings and Load screens have lower JaCoCo coverage; they are exercised by the screenshot tour and by manual use.
- Drag-and-drop placement is implemented and was checked interactively, but the automated GUI tests click rather than drag.
