# Expansion Cards — "Highland Muster" (custom project requirement)

[SPEC p.1] requires new cards that we design and implement, and asks at the start of each game whether
to include them. Everything in this document is **CUSTOM**: none of it comes from the official rulebook.

## 1. How the expansion enters the game

| Topic | Decision |
|---|---|
| Opt-in | The new-game screen (GUI) and `new` command (CLI) ask **"Include expansion cards? Yes / No"**. |
| Disabled | No expansion card exists in the game. The base game and Tactic variant behave exactly as specified in `rules-specification.md`. |
| Enabled | A separate, shuffled **Expansion deck** of 6 cards is placed next to the Clan deck (and the Tactic deck when that variant is active). |
| Compatible modes | Works with the base game **and** with the Tactic variant. |
| Set-up | Starting hands are unchanged: 6 Clan cards (base) or 7 Clan cards (Tactic). |
| Drawing | At the end of the turn the player chooses which non-empty deck to draw from (Clan / Tactic / Expansion). An empty deck cannot be chosen. One card per turn. |
| Hand limit | Unchanged (6 base, 7 Tactic). |
| Special-card restriction | Expansion cards and Tactic cards share **one** counter: you may play a special card only if you have not played more special cards than your opponent. In a base game with expansions, this restriction applies to expansion cards alone. |
| Passing | A hand that contains only special cards (Tactic or expansion) may pass, exactly like the official Tactic rule. |
| Discard | Every expansion card goes to the public discard pile once it is resolved or expires. |
| Recruiter | Can draw from and return cards to the Expansion deck (each card goes back to its own deck). |

## 2. The cards

### 2.1 Highland Scout ×2 — *Ruse (two-step)*

> Choose a non-empty deck. Look at its top 3 cards (fewer if the deck is smaller), then put them back on top in any order you choose.

- **Step 1:** pick the deck. **Step 2:** a private dialog shows the cards; the player orders them (top first). The opponent only sees that a scout happened and on which deck.
- **Duration:** instant. The order remains until the cards are drawn.
- **Information:** the seen cards are private to the scouting player. They are **never** used by the early-claim proof, which only uses public cards.
- **Limits:** cannot be played if all decks are empty.
- **Interactions:** affects the next draws of **both** players, so ordering cards to your advantage also decides what your opponent may draw.
- **Balance:** gives no card advantage and costs both a turn and a special-card slot. It is a small, information-only edge. Two copies exist so it appears regularly.

### 2.2 Stone Ward ×2 — *Ward (placed beside a Stone, on your side)*

> Place on your side of an unclaimed Stone where you already have at least one card. While the Ward is active, your cards on that Stone cannot be targeted by your opponent's Banshee, Traitor or any other opponent removal or theft effect.

- **Slot:** the Ward does **not** occupy a formation slot and does not count towards capacity or formation strength.
- **Duration:** active for your opponent's next **two** turns. It expires at the end of the opponent's second turn after it was played, and goes to the discard. If the Stone is claimed first, the Ward is discarded at that moment.
- **Limits:** one Ward per side of a Stone. Your own effects (Strategist, Clan Exchange) can still move your warded cards.
- **Interactions:** blocks Banshee and Traitor. It does not block Combat Modes (Mud Fight / Blind-Man's Bluff change the stone, not your cards).
- **Balance:** purely defensive and temporary. Using it spends your turn, so you place no troop that turn. It only matters when the opponent still holds Banshee or Traitor, so it is a bluff and tempo card rather than a guaranteed gain.

### 2.3 Clan Exchange ×2 — *Ruse*

> Choose two of your formation cards (Clan or Elite Troop) on two **different** unclaimed Stones and swap them.

- **Capacity:** both sides keep the same card count, so capacity can never be exceeded.
- **Completion order:** each side keeps its completion time (a complete side stays complete). Only the composition changes.
- **Joker limit:** unaffected (cards stay on your side).
- **Limits:** you need at least two cards on two different unclaimed stones.
- **Interactions:** your own Stone Ward does not prevent it. The opponent's Stone Ward is irrelevant, because only your own cards are swapped.
- **Balance:** it reorganises troops you have already committed, without adding cards or removing enemy cards. It is weaker than Strategist, which can also discard or move to an empty slot.

## 3. Engine design (extensibility)

- Each card is an `ExpansionCard` whose `ExpansionDefinition` (key, name, copies, category, rules text) is registered in
  `ExpansionCatalog` **together with** its `CardEffect` implementation.
- The `GameEngine` never names a specific card. It asks the `EffectRegistry` for the card's effect and calls
  `validate` / `apply` / `enumerate`, plus `resolve` for multi-step effects (`MultiStepEffect`).
- Adding a new expansion card = one new `CardEffect` class + one `ExpansionDefinition` registration line.
  No change to `GameEngine`, the CLI or the GUI is needed: both views build their target-selection steps from the effect's legal
  parameter sets.
- Duration effects (Stone Ward) are stored in `StoneState` as `ActiveWard` records and are serialised in save files.
