# Schotten Totten — Implemented Rules Specification

Sources:

- **[RB]** `docs/reference/Schottentotten Rulebook (1).pdf` (IELLO 2016 English rulebook, pages 2–9 as numbered by the PDF reader; printed page numbers 2–15).
- **[SPEC]** `docs/reference/Schotten Totten Project LO02 A26 (1).pdf` (university specification).

Every rule below is tagged:

- `OFFICIAL` — stated by the rulebook.
- `INTERPRETATION` — the rulebook is silent or ambiguous; the decision is ours and is applied consistently.
- `CUSTOM` — not in the rulebook at all (project additions such as expansion cards).

The engine implementation is in `com.schottentotten.engine` and `com.schottentotten.rules`; each rule names the class that enforces it.

---

## 1. Components

| Rule | Tag | Enforced by |
|---|---|---|
| 2 players. | OFFICIAL [RB p.2] | `GameState` |
| 9 Stone tiles in a line. | OFFICIAL [RB p.3] | `Board.STONE_COUNT` |
| 54 Clan cards: 6 colours × strengths 1–9, one of each. | OFFICIAL [RB p.2] | `CardCatalog` |
| 10 Tactic cards: Joker ×2, Spy, Shield-Bearer, Blind-Man's Bluff, Mud Fight, Traitor, Recruiter, Strategist, Banshee. | OFFICIAL [RB p.7–8] | `TacticType` |
| Colours used: Red, Orange, Yellow, Green, Blue, Purple. The rulebook names green, purple, blue and red in its example; the other two are not named. | INTERPRETATION | `ClanColor` |

## 2. Set-up

| Rule | Tag | Enforced by |
|---|---|---|
| Clan cards shuffled into one face-down deck. | OFFICIAL [RB p.3] | `GameSetup` |
| Base game: each player draws 6 Clan cards. | OFFICIAL [RB p.3] | `RuleSet.handSize()` |
| Tactic variant: Tactic cards form a second, separate deck; each player draws 7 Clan cards. | OFFICIAL [RB p.6] | `RuleSet` |
| First player: the rulebook uses "who most recently travelled near Scotland". The set-up screen lets the players pick Player 1, Player 2 or random. | INTERPRETATION | `GameConfiguration.firstPlayerRule` |
| Expansion cards (when enabled) form a third separate deck of 6 cards. See `expansion-rules.md`. | CUSTOM | `GameSetup` |

## 3. Turn structure

A turn is a strict state machine (`TurnPhase`):

```
PLAY ──play card──► (RESOLVE_EFFECT)* ──► CLAIM ──end turn / draw──► next player's PLAY
 │                                          ▲
 └──pass (only when allowed)────────────────┘
CLAIM ──claim gives victory──► ROUND_OVER ──next round──► PLAY (new round) | MATCH_OVER
```

| Rule | Tag | Enforced by |
|---|---|---|
| On your turn you must play exactly one card from your hand. | OFFICIAL [RB p.4] | `GameEngine` |
| Clan cards / Elite Troops are placed on your own side of an unclaimed Stone. | OFFICIAL [RB p.4] | `FormationPlacementEffect` |
| A side holds at most 3 cards (4 under Mud Fight). | OFFICIAL [RB p.4, p.8] | `StoneState.requiredSize()`, `StoneState.hasRoom()` |
| A placed card cannot move (except through Strategist / Traitor / Clan Exchange). | OFFICIAL [RB p.4] | effects only |
| After playing and before drawing you may claim one or several Stones. | OFFICIAL [RB p.4] | `TurnPhase.CLAIM` |
| Claims are allowed in the claim window that follows **any** play (including Ruses and Combat Modes) and also after a pass. The rulebook only mentions "after playing a card on a Stone". | INTERPRETATION | `GameEngine` |
| Draw one card at the end of the turn, if the hand is below the hand limit and the chosen deck is not empty. Base game: always the Clan deck. Tactic / expansion: the player picks any non-empty deck. | OFFICIAL [RB p.4, p.6] | `GameEngine` |
| If every deck is empty (or the hand is full), the turn ends without drawing. | OFFICIAL [RB p.4] | `GameEngine` |
| Only one card is drawn per turn even if the hand is more than one card short (it can be short after Recruiter when decks run low). | INTERPRETATION | `GameEngine` |
| Invalid actions are rejected with a message and change nothing. | Design requirement | `GameEngine` validates before mutating |

## 4. Passing

| Rule | Tag | Enforced by |
|---|---|---|
| A player may pass only when no Clan card from their hand can legally be placed (hand has only special cards, or all own unclaimed sides are full). | OFFICIAL [RB p.6] | `RuleSet.mayPass` |
| The same rule applies in the base game (a hand whose cards cannot be placed anywhere) and to an empty hand. | INTERPRETATION | `RuleSet.mayPass` |
| Deadlock: if both players pass consecutively, nothing can change any more, so the round ends. The player controlling more stones wins the round; equal counts give a drawn round. | CUSTOM (prevents infinite games) | `GameEngine` |

## 5. Formations

Formation size *N* is 3 (4 on a Mud Fight stone). From strongest to weakest [RB p.5]:

| Rank | Name | Definition (N cards) |
|---|---|---|
| 5 | Color-Run | same colour, N consecutive strengths |
| 4 | Three of a Kind (Four of a Kind under Mud Fight) | same strength |
| 3 | Color | same colour |
| 2 | Run | N consecutive strengths, any colours |
| 1 | Sum | anything else |

| Rule | Tag | Enforced by |
|---|---|---|
| Higher rank wins. | OFFICIAL | `FormationValue.compareTo` |
| Same rank: higher total strength wins. | OFFICIAL | `FormationValue.compareTo` |
| Still tied: whoever **completed** the formation first (played the last required card first) wins. | OFFICIAL [RB p.5] | `StoneSide.completedAt` |
| Card order does not matter for runs. | OFFICIAL | `FormationEvaluator` sorts strengths |
| Runs do not wrap around (9-1-2 is not a run). | INTERPRETATION (standard reading) | `FormationEvaluator` |
| Mud Fight: the five categories keep the same order with 4 cards ("Four of a Kind" replaces "Three of a Kind"). | INTERPRETATION (rulebook only says "combinations with four cards") | `FormationEvaluator` |
| Blind-Man's Bluff: only the total strength counts; equal totals are resolved by completion order. | OFFICIAL [RB p.8] + tie-break INTERPRETATION | `FormationEvaluator` |
| A stone can hold Mud Fight **and** Blind-Man's Bluff together: four cards compared by sum. | INTERPRETATION | `StoneState` |
| Completion time is reset if a complete side loses a card (Banshee, Strategist, Traitor) or becomes incomplete because Mud Fight raises the size. It is set again when the side next reaches the required size. | INTERPRETATION | `StoneState` |

## 6. Claiming stones

| Rule | Tag | Enforced by |
|---|---|---|
| The claimant's side must be complete. | OFFICIAL [RB p.4] | `ClaimResolver` |
| Normal claim: both sides complete and the claimant's formation beats the opponent's (rank, then sum, then completion order). | OFFICIAL | `ClaimResolver` |
| Early claim: the opponent's side is incomplete, and **no** possible completion can beat the claimant. | OFFICIAL [RB p.4] | `ClaimResolver.check` |
| The proof only "knows" cards played publicly (on the board or in the discard). Every other card — deck, either hand, including the claimant's own hand — counts as possibly available to the opponent. | OFFICIAL ("cards in hand don't count") | `ClaimResolver` |
| Elite Troops that have not been played publicly are also treated as possibly available to the opponent (a Joker only if the opponent has not already played one). | INTERPRETATION (conservative) | `ClaimResolver` |
| The proof searches every combination of the missing cards exhaustively (at most 4 cards from at most 58 candidates) and stops as soon as one completion beats the claimant. It is exact for the formation, so it can never award an unprovable claim. | Design | `ClaimResolver` |
| The proof assumes the stone's current Combat Modes. A Combat Mode or Ruse that the opponent might play later is not considered: claiming resolves the stone immediately, so no later card can affect it. | INTERPRETATION (documented limitation) | `ClaimResolver` |
| Once claimed, a stone is locked: no more cards can be placed on it, and no card on it can be targeted. | OFFICIAL [RB p.5] | `StoneState.isClaimed` |
| Several stones can be claimed in the same window, until the game ends. | OFFICIAL [RB p.4] | `GameEngine` |
| Elite Troop identities (colour / strength) are chosen when claiming. The engine proposes the strongest identity, and the claimant can override it in the claim dialog or with CLI `claim <n> as ...`. The opponent's Elite Troops are always evaluated at their strongest, which is the most favourable choice for their owner. | OFFICIAL + INTERPRETATION | `ClaimResolver`, `EliteIdentity`, `FormationEvaluator` |

## 7. End of the game

| Rule | Tag | Enforced by |
|---|---|---|
| A player controlling 3 adjacent stones wins immediately. | OFFICIAL [RB p.6] | `VictoryChecker` |
| A player controlling 5 stones anywhere wins immediately. | OFFICIAL [RB p.6] | `VictoryChecker` |
| After victory, every further game action is rejected. | Design | `GameEngine` checks `TurnPhase` |

## 8. Multi-round play

| Rule | Tag | Enforced by |
|---|---|---|
| The number of rounds is agreed before starting. | OFFICIAL [RB p.6] | `GameConfiguration.rounds` |
| Round winner gains 5 victory points; the opponent gains 1 point per stone they control. | OFFICIAL [RB p.6] | `MatchScore` |
| Highest total after the last round wins the match. | OFFICIAL | `MatchScore` |
| Drawn (deadlocked) round: no 5-point bonus; each player gains points equal to the stones they control. | CUSTOM | `MatchScore` |
| Equal match totals: the player who won more rounds wins; otherwise the match is drawn. | CUSTOM | `MatchScore` |
| The loser of a round starts the next round (alternate after a drawn round). | CUSTOM | `GameEngine` (next round) |

## 9. Tactic variant [RB p.6–8]

### 9.1 General

| Rule | Tag | Enforced by |
|---|---|---|
| A turn plays either a Clan card or a Tactic card. | OFFICIAL | `GameEngine` |
| Hand limit: 7 cards. Draw from either deck; an empty deck cannot be chosen. | OFFICIAL | `GameEngine` |
| Tactic restriction: you may play a Tactic card only if you have played no more Tactic cards than your opponent (so you are never more than one ahead). | OFFICIAL | `RuleSet.maySpecialBePlayed` |
| Expansion cards count as special cards for the same restriction (one shared counter). | CUSTOM | `RuleSet.maySpecialBePlayed` |
| Played Ruses go face-up to a public discard pile; cards removed by Banshee / Strategist also go there. Anyone can inspect the discard at any time. | OFFICIAL [RB p.8] | `GameState.discard` |

### 9.2 Elite Troops (placed in a formation slot like a Clan card)

| Card | Rule | Tag |
|---|---|---|
| Joker ×2 | Any colour and any strength 1–9, chosen at claim time. | OFFICIAL |
| Joker | Each player may play at most one Joker in the whole round; a player who has played a Joker must keep the second one in hand (even if the first was later discarded). | OFFICIAL ("If you have already played a Joker … keep it until the end of the game") |
| Joker | A Joker may take the identity of a Clan card that is already elsewhere on the board (duplicates are allowed). | INTERPRETATION |
| Spy | Strength 7, any colour. | OFFICIAL |
| Shield-Bearer | Strength 1, 2 or 3, any colour. | OFFICIAL |
| Placement | Same capacity / claimed-stone rules as a Clan card. | OFFICIAL |

### 9.3 Combat Modes (placed on the Stone tile itself, not on a side)

| Card | Rule | Tag |
|---|---|---|
| Blind-Man's Bluff | The stone is decided by total strength only. | OFFICIAL |
| Mud Fight | The stone needs 4 cards per side; side capacity becomes 4. | OFFICIAL |
| Both | Can only be played on an unclaimed stone; each stone holds at most one copy of each mode (only one copy exists anyway). | INTERPRETATION |
| Both | Combat Mode cards sit on the Stone, not on a player's side, so Banshee / Strategist / Traitor cannot target them. | INTERPRETATION |

### 9.4 Ruses (resolved, then discarded face-up)

| Card | Rule | Tag |
|---|---|---|
| Recruiter | Draw 3 cards, choosing how many come from each non-empty deck. Then choose 2 cards from your entire hand and put each at the bottom of its own deck (Clan → Clan deck, Tactic → Tactic deck, expansion → expansion deck). | OFFICIAL |
| Recruiter | If fewer than 3 cards are available, draw what exists. If the hand has fewer than 2 cards, return them all. The usual end-of-turn draw is still offered if the hand is below the limit. | INTERPRETATION |
| Recruiter | Cannot be played when every deck is empty (it would have no effect, see "Ruse with no legal target"). | INTERPRETATION |
| Strategist | Choose one of your own formation cards (Clan or Elite Troop) on an unclaimed stone, then move it to your side of a **different** unclaimed stone that has room, or discard it face-up. | OFFICIAL |
| Banshee | Choose one of the opponent's formation cards (Clan or Elite Troop) on an unclaimed stone and discard it face-up. | OFFICIAL |
| Traitor (printed "Traiter") | Choose one of the opponent's **Clan** cards on an unclaimed stone and place it on your side of an unclaimed stone that has room. It may go to the same stone it came from. Internal identifier: `TRAITOR`. | OFFICIAL + INTERPRETATION (same stone allowed) |
| Ruse with no legal target | A Ruse that would have no effect (for example Banshee with no target) cannot be played. | INTERPRETATION |

## 10. Variants not implemented

- **Expert variant** [RB p.9] — explicitly excluded by [SPEC p.1]. Not implemented.

## 11. Information rules

- The board, combat modes, discard pile, deck sizes, hand sizes and counts of played special cards are public.
- A hand is private to its owner. The GUI hides it behind a hand-over screen in hot-seat games; AI strategies only receive a `PlayerView` in which the opponent's hand and the deck order are hidden.
- Cards seen through Highland Scout (expansion) are known only to the player who played it.
