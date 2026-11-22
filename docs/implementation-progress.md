# Implementation Progress

Living record of the development phases from the project brief (§19). Status as of the final packaging work.

| Phase | Scope | Status | Evidence |
|---|---|---|---|
| **A** — Requirements and architecture | Read both PDFs; requirements matrix; rules specification; packages; Maven project | Done | `docs/requirements-traceability.md`, `docs/rules-specification.md`, `pom.xml` |
| **B** — Core engine | Cards, decks, board, turns, formations, claims, victory; unit tests | Done | `engine`, `rules`, `model`; `GameEngineTest`, `ClaimResolverTest`, `FormationEvaluatorTest` |
| **C** — Variants and extensions | Full Tactic variant (10 cards), expansion cards, multi-round | Done | `cards/*`, `ExpansionCatalog`, `MatchScore`; `TacticCardsTest`, `ExpansionCardsTest` |
| **D** — AI and persistence | Strategy pattern (Easy / Hard), versioned JSON saves | Done | `ai`, `persistence`; `StrategyTest`, `SaveManagerTest` |
| **E** — CLI | Complete command-line gameplay | Done | `cli.CommandLineView`; `CommandLineViewTest` (12 tests) |
| **F** — Premium GUI | Design system, assets, every screen, interactions, AI / hot-seat presentation, polish | Done | `view` package; screenshot tour; GUI playthroughs |
| **G** — Integration and verification | Shared GUI+CLI session; full suite; playthroughs; save/resume; every mode; visual QA | Done | `GuiPlaythroughTest` (including shared session); `test-report.md` |
| **H** — Documentation and packaging | Javadoc, UML, design evolution, status, submission ZIP | Done (ZIP needs team names) | `docs/*`, `docs/uml/*`, `scripts/package-submission.sh` |

## Commits (main line of work)

| Commit | Contents |
|---|---|
| Engine / model / rules / cards / AI / persistence | Core game |
| CLI | Command-line view |
| GUI | JavaFX screens, board, assets, sounds, screenshot tour |
| `5dbd9bf` | GUI playthrough tests and visual QA fixes |
| `2e681f8` | Javadoc overview and package docs, UML diagrams, rules-spec name fixes |
| Later | Mute control, deal animation, shared-session GUI test, documentation set |

## Remaining before Moodle upload

1. Confirm the A25 / A26 archive suffix with the teacher.
2. Run `scripts/package-submission.sh LASTNAME1 LASTNAME2` (optionally `--term A26`).
3. Spot-check the ZIP contents (`src`, `classes`, `doc`).
