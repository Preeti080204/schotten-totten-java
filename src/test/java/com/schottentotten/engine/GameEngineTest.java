package com.schottentotten.engine;

import com.schottentotten.TestSupport;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.ClaimAction;
import com.schottentotten.engine.action.EndTurnAction;
import com.schottentotten.engine.action.NextRoundAction;
import com.schottentotten.engine.action.PassAction;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.model.Card;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.FirstPlayerRule;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.model.VictoryReason;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.schottentotten.TestSupport.ENGINE;
import static com.schottentotten.TestSupport.clan;
import static com.schottentotten.TestSupport.hand;
import static com.schottentotten.TestSupport.place;
import static com.schottentotten.model.PlayerId.P1;
import static com.schottentotten.model.PlayerId.P2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEngineTest {

    private static PlayCardAction playOn(Card card, int stone) {
        return new PlayCardAction(card.id(), EffectParameters.onStone(stone));
    }

    private static void assertRejected(GameState s, Action action, com.schottentotten.model.PlayerId actor) {
        String before = TestSupport.json(s);
        assertThrows(IllegalActionException.class, () -> ENGINE.apply(s, actor, action));
        assertEquals(before, TestSupport.json(s), "a rejected action must not change the state");
    }

    @Test
    void legalPlacementMovesCardToStone() throws Exception {
        GameState s = TestSupport.newGame(false, false);
        Card c = s.hand(P1).get(0);
        ENGINE.apply(s, P1, playOn(c, 4));
        assertTrue(s.board().stone(4).side(P1).contains(c.id()));
        assertEquals(5, s.handSize(P1));
        assertEquals(TurnPhase.CLAIM, s.phase());
    }

    @Test
    void wrongPlayerIsRejectedWithoutChange() {
        GameState s = TestSupport.newGame(false, false);
        assertRejected(s, playOn(s.hand(P2).get(0), 0), P2);
    }

    @Test
    void cardNotInHandIsRejected() {
        GameState s = TestSupport.newGame(false, false);
        assertRejected(s, playOn(s.hand(P2).get(0), 0), P1);
        assertRejected(s, new PlayCardAction("RED-99", EffectParameters.onStone(0)), P1);
    }

    @Test
    void invalidStoneIsRejected() {
        GameState s = TestSupport.newGame(false, false);
        assertRejected(s, playOn(s.hand(P1).get(0), 9), P1);
        assertRejected(s, new PlayCardAction(s.hand(P1).get(0).id(), EffectParameters.NONE), P1);
    }

    @Test
    void fullSideIsRejected() {
        GameState s = TestSupport.newGame(false, false);
        place(s, P1, 0, clan("R1"), clan("R2"), clan("R3"));
        hand(s, P1, clan("R4"), clan("R5"), clan("R6"), clan("R7"), clan("R8"), clan("R9"));
        assertRejected(s, playOn(clan("R4"), 0), P1);
    }

    @Test
    void claimedStoneIsRejected() {
        GameState s = TestSupport.newGame(false, false);
        s.board().stone(2).claim(P2);
        assertRejected(s, playOn(s.hand(P1).get(0), 2), P1);
    }

    @Test
    void turnStateMachinePreventsDoublePlayAndEarlyClaims() throws Exception {
        GameState s = TestSupport.newGame(false, false);
        place(s, P1, 0, clan("R7"), clan("R8"), clan("R9"));
        assertRejected(s, new ClaimAction(0), P1);
        assertRejected(s, new EndTurnAction(null), P1);
        ENGINE.apply(s, P1, playOn(s.hand(P1).get(0), 5));
        assertRejected(s, playOn(s.hand(P1).get(0), 6), P1);
        assertRejected(s, new PassAction(), P1);
        ENGINE.apply(s, P1, new ClaimAction(0));
        assertTrue(s.board().stone(0).isClaimed());
        assertRejected(s, new ClaimAction(0), P1);
    }

    @Test
    void endTurnDrawsOneCardAndPassesTurn() throws Exception {
        GameState s = TestSupport.newGame(false, false);
        int deck = s.deckSize(DeckType.CLAN);
        ENGINE.apply(s, P1, playOn(s.hand(P1).get(0), 0));
        ENGINE.apply(s, P1, new EndTurnAction(null));
        assertEquals(6, s.handSize(P1));
        assertEquals(deck - 1, s.deckSize(DeckType.CLAN));
        assertEquals(P2, s.currentPlayer());
        assertEquals(TurnPhase.PLAY, s.phase());
        assertEquals(2, s.turn());
    }

    @Test
    void tacticVariantRequiresDeckChoice() throws Exception {
        GameState s = TestSupport.newGame(true, false);
        ENGINE.apply(s, P1, playOn(s.hand(P1).get(0), 0));
        assertRejected(s, new EndTurnAction(null), P1);
        assertRejected(s, new EndTurnAction(DeckType.EXPANSION), P1);
        ENGINE.apply(s, P1, new EndTurnAction(DeckType.TACTIC));
        assertEquals(9, s.deckSize(DeckType.TACTIC));
        assertEquals(DeckType.TACTIC, s.hand(P1).get(6).deckType());
    }

    @Test
    void emptyDeckMeansNoDraw() throws Exception {
        GameState s = TestSupport.newGame(false, false);
        s.replaceDeck(new com.schottentotten.model.Deck(DeckType.CLAN, List.of()));
        ENGINE.apply(s, P1, playOn(s.hand(P1).get(0), 0));
        assertRejected(s, new EndTurnAction(DeckType.CLAN), P1);
        ENGINE.apply(s, P1, new EndTurnAction(null));
        assertEquals(5, s.handSize(P1));
    }

    @Test
    void multipleClaimsInOneWindow() throws Exception {
        GameState s = TestSupport.newGame(false, false);
        place(s, P1, 0, clan("R7"), clan("R8"), clan("R9"));
        place(s, P1, 4, clan("G7"), clan("G8"), clan("G9"));
        place(s, P2, 4, clan("B1"), clan("Y3"), clan("P5"));
        ENGINE.apply(s, P1, playOn(s.hand(P1).get(0), 8));
        long claims = ENGINE.legalActions(s).stream().filter(a -> a instanceof ClaimAction).count();
        assertEquals(2, claims);
        ENGINE.apply(s, P1, new ClaimAction(0));
        ENGINE.apply(s, P1, new ClaimAction(4));
        assertEquals(2, s.board().stonesOwnedBy(P1));
    }

    @Test
    void threeAdjacentStonesWinImmediately() throws Exception {
        GameState s = TestSupport.newGame(false, false);
        s.board().stone(2).claim(P1);
        s.board().stone(3).claim(P1);
        place(s, P1, 4, clan("R7"), clan("R8"), clan("R9"));
        ENGINE.apply(s, P1, playOn(s.hand(P1).get(0), 8));
        List<GameEvent> events = ENGINE.apply(s, P1, new ClaimAction(4));
        assertEquals(TurnPhase.MATCH_OVER, s.phase());
        assertEquals(P1, s.lastRoundResult().orElseThrow().winner());
        assertEquals(VictoryReason.THREE_ADJACENT, s.lastRoundResult().orElseThrow().reason());
        assertTrue(events.stream().anyMatch(e -> e.type() == GameEventType.MATCH_OVER));
        assertRejected(s, new EndTurnAction(null), P1);
        assertRejected(s, new ClaimAction(0), P1);
        assertTrue(ENGINE.legalActions(s).isEmpty());
    }

    @Test
    void fiveScatteredStonesWin() throws Exception {
        GameState s = TestSupport.newGame(false, false);
        for (int i : new int[]{0, 2, 4, 6}) {
            s.board().stone(i).claim(P1);
        }
        place(s, P1, 8, clan("R7"), clan("R8"), clan("R9"));
        ENGINE.apply(s, P1, playOn(s.hand(P1).get(0), 1));
        ENGINE.apply(s, P1, new ClaimAction(8));
        assertEquals(VictoryReason.FIVE_STONES, s.lastRoundResult().orElseThrow().reason());
    }

    @Test
    void fourScatteredStonesDoNotWin() throws Exception {
        GameState s = TestSupport.newGame(false, false);
        for (int i : new int[]{0, 2, 4}) {
            s.board().stone(i).claim(P1);
        }
        place(s, P1, 6, clan("R7"), clan("R8"), clan("R9"));
        ENGINE.apply(s, P1, playOn(s.hand(P1).get(0), 1));
        ENGINE.apply(s, P1, new ClaimAction(6));
        assertEquals(TurnPhase.CLAIM, s.phase());
        assertTrue(s.lastRoundResult().isEmpty());
    }

    @Test
    void passOnlyWhenNoClanCardCanBePlaced() throws Exception {
        GameState s = TestSupport.newGame(false, false);
        assertRejected(s, new PassAction(), P1);
        for (int i = 0; i < 9; i++) {
            s.board().stone(i).claim(P2);
        }
        assertTrue(ENGINE.legalActions(s).contains(new PassAction()));
    }

    @Test
    void twoConsecutivePassesEndTheRound() throws Exception {
        GameState s = TestSupport.newGame(false, false);
        hand(s, P1);
        hand(s, P2);
        s.replaceDeck(new com.schottentotten.model.Deck(DeckType.CLAN, List.of()));
        s.board().stone(0).claim(P2);
        ENGINE.apply(s, P1, new PassAction());
        ENGINE.apply(s, P1, new EndTurnAction(null));
        assertEquals(TurnPhase.PLAY, s.phase());
        ENGINE.apply(s, P2, new PassAction());
        ENGINE.apply(s, P2, new EndTurnAction(null));
        assertEquals(TurnPhase.MATCH_OVER, s.phase());
        assertEquals(P2, s.lastRoundResult().orElseThrow().winner());
        assertEquals(VictoryReason.DEADLOCK_MORE_STONES, s.lastRoundResult().orElseThrow().reason());
    }

    @Test
    void multiRoundScoringAndNextRound() throws Exception {
        GameConfiguration cfg = new GameConfiguration(PlayerProfile.human("A"), PlayerProfile.human("B"), false,
                false, 2, FirstPlayerRule.PLAYER_ONE, 3L);
        GameState s = ENGINE.newGame(cfg);
        s.board().stone(0).claim(P1);
        s.board().stone(1).claim(P1);
        s.board().stone(7).claim(P2);
        place(s, P1, 2, clan("R7"), clan("R8"), clan("R9"));
        ENGINE.apply(s, P1, playOn(s.hand(P1).get(0), 5));
        ENGINE.apply(s, P1, new ClaimAction(2));
        assertEquals(TurnPhase.ROUND_OVER, s.phase());
        assertEquals(5, s.match().points(P1));
        assertEquals(1, s.match().points(P2));
        assertRejected(s, playOn(s.hand(P1).get(0), 5), P1);
        ENGINE.apply(s, P2, new NextRoundAction());
        assertEquals(2, s.round());
        assertEquals(P2, s.currentPlayer(), "the loser starts the next round");
        assertEquals(0, s.board().stonesOwnedBy(P1));
        assertEquals(6, s.handSize(P1));
        assertEquals(42, s.deckSize(DeckType.CLAN));
    }

    @Test
    void legalActionsAreAllAccepted() throws Exception {
        GameState s = TestSupport.newGame(true, true);
        for (int step = 0; step < 40 && s.phase() != TurnPhase.MATCH_OVER; step++) {
            List<Action> legal = ENGINE.legalActions(s);
            assertFalse(legal.isEmpty());
            for (Action a : legal) {
                assertTrue(ENGINE.isLegal(s, ENGINE.playerToAct(s).orElse(s.currentPlayer()), a), a.describe());
            }
            Action chosen = legal.get(step % legal.size());
            ENGINE.apply(s, ENGINE.playerToAct(s).orElse(s.currentPlayer()), chosen);
        }
    }

    @Test
    void eventsNeverRevealDrawnCards() throws Exception {
        GameState s = TestSupport.newGame(false, false);
        ENGINE.apply(s, P1, playOn(s.hand(P1).get(0), 0));
        List<GameEvent> events = new ArrayList<>(ENGINE.apply(s, P1, new EndTurnAction(null)));
        Card drawn = s.hand(P1).get(5);
        assertTrue(events.stream().noneMatch(e -> e.message().contains(drawn.displayName())));
    }
}
