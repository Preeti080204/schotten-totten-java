# Requirements Traceability Matrix

This matrix lists every requirement of the project, where it comes from, how it is implemented, how it was verified and its current status.

## Sources

- **[SPEC]**: `docs/reference/Schotten Totten Project LO02 A26 (1).pdf`, the university specification (3 pages).
- **[RB]**: `docs/reference/Schottentotten Rulebook (1).pdf`, the IELLO English rulebook. Page numbers are PDF pages (1 to 9), the same convention as `rules-specification.md`.
- **[BRIEF]**: the project brief given to the development team (the "master build prompt"), referenced by its section number (§).

## Status values

- **Done**: implemented and verified by the listed automated test(s) or by the listed recorded check.
- **Done (manual)**: implemented and verified by inspection, a screenshot or an interactive run, without an automated assertion.
- **Excluded**: deliberately not implemented because the specification forbids it.
- **Open**: needs a decision outside the code.

Test names refer to classes under `src/test/java/com/schottentotten/` (for example `ClaimResolverTest#earlyClaimUsesOnlyPublicCards`). "GUI tests" means `view/GuiPlaythroughTest`, which plays complete matches through the real JavaFX window (opt-in, see `test-report.md`). "Tour" means the automated screenshot tour (`view/ScreenshotTour`); its images are in `docs/screenshots/`.

---

## 1. University specification [SPEC]

| ID | Requirement | Source | Implementation | Verification | Status |
|---|---|---|---|---|---|
| S-01 | Two players, each either physical (human) or virtual, in any combination | SPEC p.1 §1 | `PlayerProfile`, `PlayerType`; set-up screen and CLI `new` | `GameSessionTest#virtualPlayerDriverPlaysAWholeGame`, `CommandLineViewTest#humanVsHumanGameIsPlayableToTheEndThroughCommands`, `#computerVsComputerRunsUnattended`; GUI tests (human vs computer, hot-seat, computer vs computer) | Done |
| S-02 | Virtual players have a simple but coherent strategy | SPEC p.1 §1 | `ai` package: `SimpleStrategy` (Easy), `AdvancedStrategy` (Hard) | `StrategyTest` (6 tests, 13 runs including parameterised ones) | Done |
| S-03 | The whole application is integrated in a graphical interface | SPEC p.1 §1 | `view` package (JavaFX) | GUI tests; Tour | Done |
| S-04 | Command-line interface used to test the engine, kept for evaluation and submission | SPEC p.1 §1, p.2 §2.2 | `cli.CommandLineView`, `app.CliLauncher` | `CommandLineViewTest` (12 tests) | Done |
| S-05 | Expansion concept: new cards designed by us; the player is asked at the start of a game whether to include them | SPEC p.1 §1 | `ExpansionCatalog`, 3 effects in `cards`; "Include expansion cards?" in `SetupScreen` and in the CLI `new` wizard | `ExpansionCardsTest` (8 tests); `GameSetupTest#expansionDeckOnlyWhenEnabled`; GUI tests (expansion matches) | Done |
| S-06 | All variants of the rules are included, except the Experts variant | SPEC p.1 §1 | Tactic variant (RB p.6–8) and multi-round play (RB p.6) | See R-20 to R-37 and R-16 to R-19 | Done |
| S-07 | Experts variant not implemented | SPEC p.1 §1, RB p.9 | Not present anywhere in the code; claims always happen after playing | `rules-specification.md` §10 | Excluded |
| S-08 | Object-oriented design that is modular (independent components with relations) | SPEC p.1 §1 | Packages model, rules, engine, cards, ai, controller, persistence, cli, view; the engine has no UI dependency | `docs/uml/class-diagram.png`; `design-evolution.md` | Done (manual) |
| S-09 | Extensible: rule changes and new cards with new functions can be added | SPEC p.1 §1 | `CardEffect` / `MultiStepEffect` / `LifecycleHook` in an `EffectRegistry`; `RuleSet` from `GameConfiguration`; the engine never names a specific card | The 3 expansion cards were added without changing `GameEngine` (`ExpansionCatalog`, `expansion-rules.md`) | Done |
| S-10 | Save a game in progress and resume it later | SPEC p.1 §1 | `persistence.SaveManager` (versioned JSON), GUI save/load/autosave, CLI `save`/`load` | `SaveManagerTest` (6 tests), `ControllerServicesTest#controllerSavesLoadsAndResumes`, `CommandLineViewTest#saveAndLoadRoundTrip`; Tour step 13 | Done |
| S-11 | Phase 1: UML class diagram with the Singleton and Strategy study; sequence diagram of a round | SPEC p.2 §2.1 | `docs/uml/*.puml` and rendered PNG/SVG | Rendered with PlantUML 1.2024.7; checked against `javap` output | Done (final diagrams); no Phase 1 original is available, see `design-evolution.md` |
| S-12 | Phase 2: functional simulator usable from the command line | SPEC p.2 §2.2 | `CliLauncher` (`mvn exec:java`) | `CommandLineViewTest` | Done |
| S-13 | Phase 3: GUI implementing MVC | SPEC p.2 §2.3 | `GameSession` (model holder plus observer), `GameController`/`SessionHub` (controller), `view` and `cli` (views) | `docs/uml/mvc.png`; `GameSessionTest#twoViewsSeeTheSameChanges` | Done |
| S-14 | The game stays playable from the command line and the GUI **within the same game** (two competing views) | SPEC p.2 §2.3 | `Launcher --with-cli` starts both views on one `SessionHub`; every action goes through `GameSession.submitAsHuman` (synchronised, version-checked) | `GuiPlaythroughTest#commandLineAndWindowPlayTheSameGame`, `CommandLineViewTest#twoViewsShareTheSameLiveGame`, `GameSessionTest#concurrentSubmissionsCannotDoublePlay`; manual run with `-Djavafx.args=--with-cli` | Done |
| S-15 | ZIP named `XXX_YYY_Project_LO02_A25.zip` | SPEC p.2 §3 | `scripts/package-submission.sh` uses the name exactly as printed and prints a warning | The script stops unless the team names are given | Open: the A26 document asks for an **A25** file name. We kept A25 as written and flag it for confirmation with the teacher (see `project-status.md`). |
| S-16 | ZIP contains `src` (all sources) | SPEC p.2 §3 | Packaging script copies `src/` | Listing of the produced archive (`test-report.md` §6) | Done |
| S-17 | ZIP contains `classes` (all compiled classes) | SPEC p.2 §3 | Packaging script copies `target/classes` | Archive listing | Done |
| S-18 | ZIP contains `doc` with the Javadoc, the final class diagram and a status summary | SPEC p.2 §3 | Packaging script copies the generated Javadoc, `docs/uml`, `project-status.md`, `design-evolution.md` and the other documents | Archive listing | Done |
| S-19 | Javadoc on every code element, at application, package and class level | SPEC p.3 | `src/main/javadoc/overview.html` (application), `package-info.java` in all 15 packages, class and member comments | `javadoc -Xdoclint:all` reports **0 warnings** | Done |
| S-20 | Clean code following Oracle conventions; no obsolete code left in comments | SPEC p.3 | Conventions applied throughout; `-Xlint:all` compiler flag | Search for TODO/FIXME and commented-out statements returns nothing | Done (manual) |
| S-21 | Final class diagram, with the changes from the initial diagram justified and both diagrams shown | SPEC p.3 | `docs/uml/class-diagram.*`, `docs/design-evolution.md` | Document review | Partial: the final diagram and its justification exist. No initial Phase 1 diagram was provided to us, so it cannot be shown (stated in `design-evolution.md`). |
| S-22 | Current state of the application: implemented / not implemented / working / buggy | SPEC p.3 | `docs/project-status.md` | Document review | Done |

## 2. Game rules [RB]

Details and interpretations of every rule are in `rules-specification.md`.

| ID | Requirement | Source | Implementation | Verification | Status |
|---|---|---|---|---|---|
| R-01 | 54 Clan cards, 6 colours, strengths 1–9, one of each | RB p.2 | `CardCatalog`, `ClanCard`, `ClanColor` | `CardCatalogTest#thereAreExactly54UniqueClanCards`, `#everyColourStrengthCombinationExistsOnce`, `#clanCardRejectsInvalidStrength` | Done |
| R-02 | 9 Stones in a line | RB p.2–3 | `Board.STONE_COUNT` | `GameSetupTest`, `GameEngineTest#invalidStoneIsRejected` | Done |
| R-03 | Shuffle the Clan deck; each player draws 6 cards | RB p.3 | `GameSetup`, `RuleSet.handSize()` | `GameSetupTest#baseGameDealsSixClanCardsEach`, `#everyCardIsDealtExactlyOnce`, `#shufflingIsReproducibleWithTheSameSeed` | Done |
| R-04 | First player | RB p.3 | `FirstPlayerRule` (Player 1, Player 2, random); interpretation documented | `GameSetupTest#firstPlayerRuleIsApplied` | Done |
| R-05 | A turn: play one card next to a Stone on your side, then draw | RB p.4 | `GameEngine` state machine (`TurnPhase`) | `GameEngineTest#legalPlacementMovesCardToStone`, `#endTurnDrawsOneCardAndPassesTurn` | Done |
| R-06 | At most 3 cards per side of a Stone | RB p.4 | `StoneState.hasRoom`, `requiredSize` | `GameEngineTest#fullSideIsRejected` | Done |
| R-07 | Placed cards cannot be moved | RB p.4 | No action moves a card except the Strategist, Traitor and Clan Exchange effects | `GameEngineTest#turnStateMachinePreventsDoublePlayAndEarlyClaims` | Done |
| R-08 | Empty deck: continue without drawing | RB p.4 | `GameEngine.drawableDecks`, `EndTurnAction(null)` | `GameEngineTest#emptyDeckMeansNoDraw` | Done |
| R-09 | Claim one or several Stones after playing and before drawing | RB p.4 | `TurnPhase.CLAIM`, `ClaimAction` | `GameEngineTest#multipleClaimsInOneWindow` | Done |
| R-10 | Normal claim: both formations complete, the stronger wins | RB p.4 | `ClaimResolver.check` | `ClaimResolverTest#rulebookExampleNormalClaim`, `#incompleteClaimantCannotClaim` | Done |
| R-11 | Early claim proven from played cards only (cards in hand do not count) | RB p.4 | `ClaimResolver` exhaustive search over every card not publicly played | `ClaimResolverTest#earlyClaimAllowedWhenNothingCanBeatIt`, `#earlyClaimRejectedWhenOpponentCouldStillWin`, `#earlyClaimUsesOnlyPublicCards`, `#earlyClaimTieGoesToClaimantWhoCompletedFirst`, `#earlyClaimConsidersUnplayedEliteTroops` | Done |
| R-12 | Formation ranking: Color-Run > Three of a Kind > Color > Run > Sum | RB p.5 | `FormationEvaluator`, `FormationRank` | `FormationEvaluatorTest#rankingOrderIsOfficial`, `#rulebookExampleThreeOfAKindBeatsSum`, `#runsDoNotWrapAround` | Done |
| R-13 | Card order does not matter for runs | RB p.5 | Strengths are sorted before evaluation | `FormationEvaluatorTest#rankingOrderIsOfficial` | Done |
| R-14 | Tie: higher total; still tied: the player who completed first | RB p.5 | `FormationValue.compareTo`, `StoneSide.completedAt` | `FormationEvaluatorTest#sameCategoryComparesSumAndEqualSumsTie`, `ClaimResolverTest#equalFormationsGoToWhoeverCompletedFirst` | Done |
| R-15 | A claimed Stone accepts no more cards, even if a formation is incomplete | RB p.5 | `StoneState.isClaimed` checked by every effect | `GameEngineTest#claimedStoneIsRejected`, `ClaimResolverTest#claimedStoneCannotBeClaimedAgain`, `TacticCardsTest#combatModeCannotBePlayedTwiceOrOnClaimedStone` | Done |
| R-16 | Game ends immediately with 3 adjacent Stones | RB p.6 | `VictoryChecker` | `GameEngineTest#threeAdjacentStonesWinImmediately` | Done |
| R-17 | Game ends immediately with 5 Stones anywhere | RB p.6 | `VictoryChecker` | `GameEngineTest#fiveScatteredStonesWin`, `#fourScatteredStonesDoNotWin` | Done |
| R-18 | No action after the end of the game | RB p.6 | `TurnPhase.ROUND_OVER` / `MATCH_OVER` reject every game action | `GameEngineTest#multiRoundScoringAndNextRound`, `CommandLineViewTest#informationCommandsWorkBeforeAndDuringAGame` | Done |
| R-19 | Several rounds: winner 5 points, opponent 1 point per Stone; most points wins; number of rounds agreed first | RB p.6 | `MatchScore`, `GameConfiguration.rounds`, set-up screen and CLI | `GameEngineTest#multiRoundScoringAndNextRound`, `SaveManagerTest#matchScoresArePreserved`; GUI tests (2-round matches) | Done |
| R-20 | Tactic variant: separate Tactic deck, 7 cards in hand | RB p.6 | `GameSetup`, `RuleSet` | `GameSetupTest#tacticVariantDealsSevenClanCardsAndKeepsDecksSeparate`, `CardCatalogTest#tacticDeckHasTheTenOfficialCards` | Done |
| R-21 | Play a Clan or a Tactic card; choose the deck to draw from; empty deck cannot be chosen | RB p.6 | `EndTurnAction(deck)`, `drawableDecks` | `GameEngineTest#tacticVariantRequiresDeckChoice`, `CommandLineViewTest#tacticGameAsksForTheDeckAndPlaysByLabel` | Done |
| R-22 | Pass when no Clan card can be played | RB p.6 | `RuleSet.mayPass`, `PassAction` | `GameEngineTest#passOnlyWhenNoClanCardCanBePlaced`, `TacticCardsTest#passIsAllowedWithOnlyTacticCards` | Done |
| R-23 | Never more than one Tactic card ahead of the opponent | RB p.7 | `RuleSet.maySpecialBePlayed` | `TacticCardsTest#tacticCountRestrictionFollowsTheRulebookExample` | Done |
| R-24 | Joker (×2): any colour and strength; only one Joker per player; the second stays in hand | RB p.7 | `TacticType.JOKER`, `GameState.hasPlayedJoker` | `TacticCardsTest#jokerIsPlayedLikeAClanCardButOnlyOnce`, `FormationEvaluatorTest#jokerTakesTheBestIdentity`, `ClaimResolverTest#opponentWhoPlayedAJokerCannotUseTheSecond` | Done |
| R-25 | Spy: strength 7, any colour | RB p.7 | `TacticType.SPY` | `FormationEvaluatorTest#spyIsAlwaysSeven`, `TacticCardsTest#spyAndShieldBearerOccupyFormationSlots` | Done |
| R-26 | Shield-Bearer: strength 1, 2 or 3, any colour | RB p.7 | `TacticType.SHIELD_BEARER` | `FormationEvaluatorTest#shieldBearerIsOneToThree` | Done |
| R-27 | Elite Troop identities chosen when claiming | RB p.7 | `ClaimAction.identities`, `EliteIdentity`; GUI identities dialog; CLI `claim n as ...` | `ClaimResolverTest#chosenEliteIdentityIsApplied`, `FormationEvaluatorTest#overridesAreRespectedAndValidated`, `CommandLineViewTest#claimWithEliteIdentityAndRefusals` | Done |
| R-28 | Blind-Man's Bluff: only the sum counts | RB p.8 | `CombatModeEffect`, `FormationEvaluator` (sum only) | `TacticCardsTest#blindMansBluffMakesSumDecide`, `FormationEvaluatorTest#sumOnlyIgnoresCategories`, `ClaimResolverTest#blindMansBluffComparesSumsOnly` | Done |
| R-29 | Mud Fight: formations of 4 cards | RB p.8 | `CombatModeEffect`, `StoneState.requiredSize` | `TacticCardsTest#mudFightRaisesCapacityToFour`, `FormationEvaluatorTest#mudFightUsesFourCards`, `ClaimResolverTest#mudFightNeedsFourCards` | Done |
| R-30 | Ruses are discarded face up; anyone may look at the discard at any time | RB p.8 | `GameState.discard`; GUI discard pile and viewer; CLI `discard` | `TacticCardsTest` (Ruse tests check the discard); Tour step 05 | Done |
| R-31 | Traitor ("Traiter"): move an opponent's Clan card from an unclaimed Stone to your side of an unclaimed Stone | RB p.8 | `TraitorEffect`, identifier `TRAITOR` | `TacticCardsTest#traitorStealsOnlyClanCards`, `CommandLineViewTest#wizardAsksForTraitorDestination`; Tour steps 04–05 | Done |
| R-32 | Recruiter: draw 3 from one or both decks, return 2 from the whole hand to the bottom of their decks | RB p.8 | `RecruiterEffect` (`MultiStepEffect`) | `TacticCardsTest#recruiterDrawsThreeAndReturnsTwo`, `ExpansionCardsTest#recruiterCanUseExpansionDeck`, `SaveManagerTest#pendingEffectIsPreserved` | Done |
| R-33 | Strategist: move one of your cards from an unclaimed Stone to another unclaimed Stone, or discard it | RB p.8 | `StrategistEffect` | `TacticCardsTest#strategistMovesOrDiscardsOwnCard`, `#strategistCanDiscard` | Done |
| R-34 | Banshee: discard one opponent card from an unclaimed Stone | RB p.8 | `BansheeEffect` | `TacticCardsTest#bansheeDiscardsOpponentCardOnUnclaimedStoneOnly`, `#bansheeCanRemoveEliteTroop` | Done |
| R-35 | Combat Modes are played on the Stone tile | RB p.8 | `StoneState.combatModes` | `TacticCardsTest#combatModeCannotBePlayedTwiceOrOnClaimedStone` | Done |
| R-36 | Hand limit of 7 cards in the Tactic variant | RB p.7 | `RuleSet.handSize`, `drawableDecks` | `GameEngineTest#endTurnDrawsOneCardAndPassesTurn`, `TacticCardsTest#recruiterDrawsThreeAndReturnsTwo` | Done |
| R-37 | A Ruse with no possible effect cannot be played (interpretation) | rules-spec §9.4 | `CardEffect.enumerate` / `validate` | `TacticCardsTest#ruseWithoutTargetCannotBePlayed` | Done |

## 3. Project brief [BRIEF]

### 3.1 Documents and technology (§1, §2, §18)

| ID | Requirement | Source | Implementation | Verification | Status |
|---|---|---|---|---|---|
| B-01 | `requirements-traceability.md` and `rules-specification.md`; ambiguities and custom rules labelled | §1 | This file; `rules-specification.md` (OFFICIAL / INTERPRETATION / CUSTOM tags) | Document review | Done |
| B-02 | Java 21, JavaFX, Maven, JavaFX CSS, JUnit 5, Jackson, Javadoc | §2 | `pom.xml` (release 21, JavaFX 21.0.5, Jackson 2.17.2, JUnit 5.10.3), `view/style.css` | `mvn verify` | Done |
| B-03 | Packages by responsibility; engine never depends on JavaFX | §2 | Packages listed in S-08 | `grep javafx` finds imports only in `view` and `app.Launcher` (see `test-report.md` §5) | Done |
| B-04 | Documented commands: install, compile, test, GUI, CLI, shared session, Javadoc, distribution, submission | §18 | `README.md` | Each command run during verification (`test-report.md`) | Done |
| B-05 | macOS Apple Silicon support without manual JavaFX configuration | §18 | JavaFX from Maven with automatic platform classifier; `javafx-maven-plugin` | Built and run on macOS (Apple Silicon, arm64) | Done |
| B-06 | No mandatory online service, account or runtime download | §18 | All assets are bundled resources | Game runs offline | Done |

### 3.2 Game modes and turn engine (§3)

| ID | Requirement | Source | Implementation | Verification | Status |
|---|---|---|---|---|---|
| B-10 | Human vs human with a hot-seat screen that hides the previous player's hand | §3.1 | `GameOverlays.curtain`; hands face down until the next player confirms | GUI test `hotSeatTacticExpansionMatchFinishes` (asserts that no hand card is visible behind the curtain, more than 10 hand-overs); Tour step 06 | Done |
| B-11 | Human vs computer | §3.1 | `VirtualPlayerDriver` plays the virtual seat | GUI tests `humanBeatsOrLosesAgainstComputerInTacticExpansionMatch`, `baseGameAgainstEasyComputer` | Done |
| B-12 | Computer vs computer, watchable, adjustable speed | §3.1 | Side-rail "Computer pace" slider (Slow / Normal / Fast / Instant), Pause, Show hands; CLI `speed`, `pause`, `resume` | GUI test `computerVsComputerCanBeWatchedPausedRevealedAndMuted`; `ControllerServicesTest#driverPausesAndResumesComputerPlayers` | Done |
| B-13 | Strict turn state machine (no double play or draw, no claim at the wrong time) | §3.2 | `TurnPhase`, `GameEngine.requireTurn` | `GameEngineTest#turnStateMachinePreventsDoublePlayAndEarlyClaims`, `#wrongPlayerIsRejectedWithoutChange`, `#cardNotInHandIsRejected` | Done |
| B-14 | Invalid moves never change the state; everything is validated before committing | §3.2 | The engine validates first; `GameSession` applies to a copy and commits only on success | `GameEngineTest#wrongPlayerIsRejectedWithoutChange`, `GameSessionTest#rejectedActionsDoNotNotify`, `#concurrentSubmissionsCannotDoublePlay` | Done |
| B-15 | Deterministic `FormationEvaluator`, unit-tested on its own | §3.3 | `rules.FormationEvaluator` | `FormationEvaluatorTest` (11 tests, 20 runs) | Done |
| B-16 | Early claims never false-positive; conservative with tactics | §3.4 | Exhaustive search; unplayed Elite Troops counted as available to the opponent | `ClaimResolverTest` (13 tests) | Done |
| B-17 | Victory screen with winner and winning condition | §3.5, §9.6 | `GameOverlays.victory` | GUI tests reach "Play Again"; Tour step 07 | Done |

### 3.3 Tactic variant, expansion and match (§4, §5, §6)

| ID | Requirement | Source | Implementation | Verification | Status |
|---|---|---|---|---|---|
| B-20 | Tactic variant selectable at the start of a game | §4 | Set-up screen "Rules" tiles; CLI `new` | GUI tests; `CommandLineViewTest#tacticGameAsksForTheDeckAndPlaysByLabel` | Done |
| B-21 | Dedicated effect objects instead of large conditional blocks | §4.1 | One `CardEffect` class per behaviour, looked up by id | Code review; class diagram | Done |
| B-22 | Interpretation for Mud Fight four-card rankings recorded | §4.2 | `rules-specification.md` §5 | Document review | Done |
| B-23 | Traitor naming difference documented; identifier `TRAITOR` | §4.3 | `TacticType.TRAITOR`; `rules-specification.md` §9.4 | Document review | Done |
| B-24 | Visible and inspectable discard pile | §4.3 | Side-rail discard pile, discard viewer overlay (D key), CLI `discard` | Tour step 05; `CommandLineViewTest#informationCommandsWorkBeforeAndDuringAGame` | Done |
| B-25 | "Include expansion cards?" Yes / No at the start of a game | §5 | `SetupScreen` row "Include expansion cards?"; CLI wizard question | Tour step 02; `CommandLineViewTest` | Done |
| B-26 | Without expansions, the cards are completely excluded | §5 | No expansion deck is created | `ExpansionCardsTest#gameWithoutExpansionsHasNoExpansionCard`, `GameSetupTest#expansionDeckOnlyWhenEnabled` | Done |
| B-27 | At least three original, balanced expansion cards, fully implemented, with exact rules, limits, duration, interactions and balance documented | §5 | Highland Scout, Stone Ward, Clan Exchange (two copies each); `expansion-rules.md` | `ExpansionCardsTest` (8 tests); GUI tests play them through the interface | Done |
| B-28 | Extensible `ExpansionCard` abstraction; no giant switch over cards | §5 | `ExpansionCard` + `ExpansionDefinition` + registered `CardEffect` | Class diagram; `ExpansionCatalog` | Done |
| B-29 | Expansion rules explained in the UI | §5 | Rules screen section "Expansion cards" with card faces; card tooltips | Tour step 09 (Tactic cards page; the Expansion page uses the same layout) | Done (manual) |
| B-30 | Optional multi-round match: choose the number of rounds, standings between rounds, overall winner; one round by default | §6 | Set-up "Rounds" stepper (single round by default, up to 9 rounds); victory overlay shows the score table and "Next Round" | GUI tests (2-round matches); `GameEngineTest#multiRoundScoringAndNextRound` | Done |

### 3.4 AI and Singleton (§7, §8)

| ID | Requirement | Source | Implementation | Verification | Status |
|---|---|---|---|---|---|
| B-40 | `PlayerStrategy` interface, Strategy pattern | §7 | `ai.PlayerStrategy`, `AbstractStrategy` (template), `StrategyFactory` | `docs/uml/strategy-ai.png` | Done |
| B-41 | Simple strategy: legal, coherent, prefers useful formations, never cheats | §7 | `SimpleStrategy` | `StrategyTest#aiAlwaysClaimsAvailableStones`, `#viewHidesOpponentHandAndDeckOrder` | Done |
| B-42 | Advanced strategy: formation evaluation, adjacency, blocking, tactic use, limited look-ahead, responsive | §7 | `AdvancedStrategy` + `BoardEvaluator` (one-ply simulation, at most 1500 simulations per decision) | `StrategyTest#hardBeatsEasyMoreOftenThanNot`, `#hardAiCompletesAWinningFormation`, `#hardAiBlocksThreeAdjacentThreat` | Done |
| B-43 | Easy and Hard difficulty options | §7 | `Difficulty`; set-up screen; settings default | GUI tests use both | Done |
| B-44 | AI actions go through the same validation | §7 | `VirtualPlayerDriver` calls `GameSession.submit` | `GameSessionTest#virtualPlayerDriverPlaysAWholeGame` | Done |
| B-45 | Seeded randomness, reproducible | §7 | `SeededRandom`, seeds in `GameConfiguration` | `StrategyTest#strategiesAreReproducible`, `GameSetupTest#shufflingIsReproducibleWithTheSameSeed` | Done |
| B-46 | Hidden information hidden from the AI | §7 | `PlayerView` redistributes unseen cards at random | `StrategyTest#viewHidesOpponentHandAndDeckOrder` | Done |
| B-47 | Justified Singleton (`GameSettings`) with UML; per-game rules passed explicitly | §8 | `app.GameSettings` (holder idiom); `GameConfiguration` for rules | `GameSettingsTest#thereIsExactlyOneInstance`; `docs/uml/singleton-gamesettings.png`; `design-evolution.md` (trade-offs) | Done |

### 3.5 Graphical interface (§9, §13, visual brief)

| ID | Requirement | Source | Implementation | Verification | Status |
|---|---|---|---|---|---|
| B-50 | Main menu: title, illustrated background, New Game, Continue, Rules and Tutorial, Settings, Exit; subtle animation and hover effects | §9.1 | `MainMenuScreen` (animated Highlands landscape, drifting clouds, Quit = Exit); Continue appears when an autosave exists | Tour step 01 | Done (manual) |
| B-51 | Set-up screen: names, player types, difficulty, rules, expansion, rounds, explanations | §9.2 | `SetupScreen` with a description under each option | Tour step 02 | Done (manual) |
| B-52 | Board: Stones at the centre; both formation areas; ownership; legal-target highlights; current-player indicator; deck counts; opponent hand count; match score; claim controls; deck choice; menu and save; action log | §9.3 | `BoardView`, `StoneView`, `SideRail` | Tour step 03; GUI tests use every control | Done |
| B-53 | Comfortable on a laptop; responsive; minimum window size; HiDPI | §9.3 | `ScalingPane` / `FitHeightPane`; minimum window 1100×700; card art at 540×720 (about 6 times its displayed size); landscapes at 1280×720 | Tour at 1440×900 and 1280×720 | Done (manual). The landscape backgrounds are slightly soft on large Retina screens (see `project-status.md`). |
| B-54 | Click to select, click to place, drag and drop, highlighted targets, clear invalid-move messages | §9.4 | `BoardView` (drag-and-drop with `Dragboard`), error toasts with the engine's reason | GUI tests (clicks); drag and drop checked interactively | Done (drag and drop manual) |
| B-55 | Hover lift, smooth movement, deal animation, special effects, claim animation; animation never changes game logic; reduce-motion option | §9.4 | Hand-card lift, cards flying to Stones, staggered deal into hands, Stone pulse and confetti; animations only decorate already-committed snapshots; Settings "Reduce motion" | GUI tests run with motion on and off | Done (manual for visual quality) |
| B-56 | Distinct look for Tactic and expansion cards; dialogs or board selection for every choice; never choose silently | §9.5 | Card families have their own frames; `ParameterChooser` asks for every field with more than one possible value | Tour steps 04–05; `ControllerServicesTest#chooserNarrowsOneFieldAtATime` | Done |
| B-57 | Victory screen: winner, reason, claimed Stones, score, match summary, New Game, Main Menu, replay | §9.6 | `GameOverlays.victory` (score table per round), "View the Border", "Play Again", "Main Menu", confetti | Tour step 07; GUI tests | Done |
| B-58 | Custom CSS, typography, panels, hover and pressed states, custom dialogs, tooltips, transitions, helpful errors, music and effects, mute control, keyboard alternatives, focus indicators | §9.7 | `style.css`, bundled OFL fonts, `Dialogs`, tooltips on cards, Stones and buttons, screen fades, `SoundManager` (synthesised audio), Mute button and M key, keyboard shortcuts (1–9, C, E, P, H, D, M, Esc, Cmd/Ctrl+S), `:focused` styles | Tour; GUI test toggles mute | Done (manual for visual quality) |
| B-59 | Rules and tutorial screen: objective, turn, formations, ties, claims, early claims, victory, Tactic variant, every Tactic card, expansion rules, save and load, illustrated examples | §13 | `RulesScreen` built from `text/rules.txt` plus card galleries | Tour steps 08–10; `CommandLineViewTest#rulesAreBrowsable` | Done |
| B-60 | Visual style inspired by the rulebook (medieval Scottish, emerald, turquoise, burgundy, gold, brown); original assets; no copyrighted illustrations | Visual brief | Original generated artwork, parchment and wood textures; `docs/assets.md` lists provenance | Tour; `docs/assets.md` | Done (manual) |
| B-61 | Visual QA screenshots at laptop resolutions | Visual brief | `ScreenshotTour` (13 screens × 2 sizes) | `docs/screenshots/` | Done |

### 3.6 Command line, MVC, persistence (§10, §11, §12)

| ID | Requirement | Source | Implementation | Verification | Status |
|---|---|---|---|---|---|
| B-70 | Complete CLI: new game, players, variants, expansion, board, hand, play, targets, claims, deck choice, pass, save, load, help, rules, results | §10 | `CommandLineView` (see the `help` text) | `CommandLineViewTest` (12 tests) | Done |
| B-71 | Dedicated launch command for the CLI | §10 | `mvn exec:java`; `java -cp ... com.schottentotten.app.CliLauncher` | README | Done |
| B-72 | GUI and CLI control the same live session, safely (no race, no corruption) | §10, §11 | `SessionHub`, synchronised `GameSession` with version checks | `GuiPlaythroughTest#commandLineAndWindowPlayTheSameGame`, `CommandLineViewTest#twoViewsShareTheSameLiveGame`, `GameSessionTest#concurrentSubmissionsCannotDoublePlay` | Done |
| B-73 | MVC separation and SOLID principles; no GUI code in rule classes | §11 | See `docs/uml/mvc.png` and `design-evolution.md` | Import check (B-03) | Done |
| B-74 | Versioned JSON saves with everything needed to resume (players, AI, board, completion order, hands, deck orders, discard, counters, pending effects, wards, phase, scores, random state) | §12 | `SaveFileDto` (format version 1), `GameStateMapper` | `SaveManagerTest#roundTripRestoresEverything`, `#loadedGameContinuesIdentically`, `#pendingEffectIsPreserved`, `#matchScoresArePreserved` | Done |
| B-75 | Friendly errors for invalid files; no unsafe deserialisation | §12 | `SaveFormatException` messages; plain Jackson records, no Java serialisation | `SaveManagerTest#invalidFilesGiveFriendlyErrors`, `#duplicatedCardIsDetected`; `ControllerServicesTest#controllerSavesLoadsAndResumes` (broken file) | Done |

### 3.7 UML, tests, Javadoc, packaging (§14 to §17, §21)

| ID | Requirement | Source | Implementation | Verification | Status |
|---|---|---|---|---|---|
| B-80 | Class, Singleton, Strategy, normal-turn sequence, Tactic-effect sequence and MVC diagrams matching the code | §14 | `docs/uml/` (PlantUML sources, PNG and SVG) | Checked against `javap` signatures | Done |
| B-81 | `design-evolution.md`; say so if no Phase 1 diagram exists | §14 | `docs/design-evolution.md` | Document review | Done |
| B-82 | Comprehensive JUnit 5 suite; ≥ 80 % coverage of critical logic; real results | §15 | 15 test classes, 139 test runs (4 GUI tests opt-in) | `test-report.md` (JaCoCo figures per package) | Done |
| B-83 | Complete Javadoc (application, package, classes, important methods) | §16 | See S-19 | `javadoc -Xdoclint:all`: 0 warnings | Done |
| B-84 | Submission package with `src`, `classes`, `doc`, README, screenshots, documentation | §17 | `scripts/package-submission.sh` | Archive listing in `test-report.md` | Done |
| B-85 | Flag the A25 / A26 file-name discrepancy | §17 | Script warning; `project-status.md` | — | Open (teacher confirmation) |
| B-86 | `implementation-progress.md` with the real status of each phase | §21 | `docs/implementation-progress.md` | Document review | Done |
