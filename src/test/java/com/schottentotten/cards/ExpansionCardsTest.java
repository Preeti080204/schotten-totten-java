package com.schottentotten.cards;

import com.schottentotten.TestSupport;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.IllegalActionException;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.ClaimAction;
import com.schottentotten.engine.action.EndTurnAction;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.engine.action.ResolveEffectAction;
import com.schottentotten.model.Card;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.TurnPhase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.schottentotten.TestSupport.ENGINE;
import static com.schottentotten.TestSupport.card;
import static com.schottentotten.TestSupport.clan;
import static com.schottentotten.TestSupport.hand;
import static com.schottentotten.TestSupport.place;
import static com.schottentotten.model.PlayerId.P1;
import static com.schottentotten.model.PlayerId.P2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpansionCardsTest {

    private static void rejected(GameState s, PlayerId actor, Action a) {
        String before = TestSupport.json(s);
        assertThrows(IllegalActionException.class, () -> ENGINE.apply(s, actor, a));
        assertEquals(before, TestSupport.json(s));
    }

    private static void endTurn(GameState s, PlayerId p) throws IllegalActionException {
        List<DeckType> d = ENGINE.drawableDecks(s, p);
        ENGINE.apply(s, p, new EndTurnAction(d.isEmpty() ? null : DeckType.CLAN));
    }

    private static void playAnywhere(GameState s, PlayerId p) throws IllegalActionException {
        Card c = s.hand(p).stream().filter(x -> x.deckType() == DeckType.CLAN).findFirst().orElseThrow();
        int stone = s.board().stones().stream().filter(st -> st.hasRoom(p)).reduce((a, b) -> b).orElseThrow().index();
        ENGINE.apply(s, p, new PlayCardAction(c.id(), EffectParameters.onStone(stone)));
    }

    @Test
    void highlandScoutReordersTheTopOfADeck() throws Exception {
        GameState s = TestSupport.newGame(false, true);
        hand(s, P1, card("X-SCOUT-1"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"));
        List<Card> top = s.deck(DeckType.CLAN).orElseThrow().peek(3);
        ENGINE.apply(s, P1, new PlayCardAction("X-SCOUT-1", EffectParameters.ofDeck(DeckType.CLAN)));
        assertEquals(TurnPhase.RESOLVE_EFFECT, s.phase());
        assertEquals(top.stream().map(Card::id).toList(), s.pending().orElseThrow().cardIds());
        rejected(s, P1, new ResolveEffectAction(EffectParameters.cards(List.of(top.get(0).id()))));
        List<String> reversed = new ArrayList<>(top.stream().map(Card::id).toList());
        java.util.Collections.reverse(reversed);
        ENGINE.apply(s, P1, new ResolveEffectAction(EffectParameters.cards(reversed)));
        assertEquals(reversed, s.deck(DeckType.CLAN).orElseThrow().peek(3).stream().map(Card::id).toList());
        assertTrue(s.discard().contains(card("X-SCOUT-1")));
        ENGINE.apply(s, P1, new EndTurnAction(DeckType.CLAN));
        assertTrue(s.findInHand(P1, reversed.get(0)).isPresent());
    }

    @Test
    void stoneWardBlocksBansheeAndTraitorThenExpires() throws Exception {
        GameState s = TestSupport.newGame(true, true);
        place(s, P1, 2, clan("R9"), clan("R8"));
        hand(s, P1, card("X-WARD-1"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"), clan("Y6"));
        hand(s, P2, card("T-BANSHEE"), card("T-TRAITOR"), clan("B1"), clan("B2"), clan("B3"), clan("B4"), clan("B5"));
        rejected(s, P1, new PlayCardAction("X-WARD-1", EffectParameters.onStone(3)));
        ENGINE.apply(s, P1, new PlayCardAction("X-WARD-1", EffectParameters.onStone(2)));
        assertTrue(s.board().stone(2).side(P1).ward().isPresent());
        assertEquals(1, s.specialPlayed(P1), "expansion cards share the special-card counter");
        endTurn(s, P1);
        rejected(s, P2, new PlayCardAction("T-BANSHEE", EffectParameters.target("RED-9")));
        rejected(s, P2, new PlayCardAction("T-TRAITOR", EffectParameters.move("RED-9", 0)));
        playAnywhere(s, P2);
        endTurn(s, P2);
        assertEquals(1, s.board().stone(2).side(P1).ward().orElseThrow().remainingOpponentTurns());
        playAnywhere(s, P1);
        endTurn(s, P1);
        rejected(s, P2, new PlayCardAction("T-BANSHEE", EffectParameters.target("RED-9")));
        playAnywhere(s, P2);
        endTurn(s, P2);
        assertTrue(s.board().stone(2).side(P1).ward().isEmpty(), "ward lasts two opponent turns");
        assertTrue(s.discard().contains(card("X-WARD-1")));
        playAnywhere(s, P1);
        endTurn(s, P1);
        ENGINE.apply(s, P2, new PlayCardAction("T-BANSHEE", EffectParameters.target("RED-9")));
        assertFalse(s.board().stone(2).side(P1).contains("RED-9"));
    }

    @Test
    void stoneWardIsDiscardedWhenStoneIsClaimed() throws Exception {
        GameState s = TestSupport.newGame(false, true);
        place(s, P1, 0, clan("R7"), clan("R8"), clan("R9"));
        hand(s, P1, card("X-WARD-1"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"));
        ENGINE.apply(s, P1, new PlayCardAction("X-WARD-1", EffectParameters.onStone(0)));
        ENGINE.apply(s, P1, new ClaimAction(0));
        assertTrue(s.board().stone(0).side(P1).ward().isEmpty());
        assertTrue(s.discard().contains(card("X-WARD-1")));
    }

    @Test
    void wardNeedsOwnCards() {
        GameState s = TestSupport.newGame(false, true);
        hand(s, P1, card("X-WARD-1"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"));
        rejected(s, P1, new PlayCardAction("X-WARD-1", EffectParameters.onStone(0)));
    }

    @Test
    void clanExchangeSwapsOwnCardsKeepingCompletion() throws Exception {
        GameState s = TestSupport.newGame(false, true);
        place(s, P1, 0, clan("R1"), clan("G8"), clan("R3"));
        place(s, P1, 4, clan("G7"), clan("R2"), clan("G9"));
        long done0 = s.board().stone(0).side(P1).completedAt();
        hand(s, P1, card("X-EXCHANGE-1"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"));
        rejected(s, P1, new PlayCardAction("X-EXCHANGE-1", EffectParameters.swap("RED-1", "RED-3")));
        ENGINE.apply(s, P1, new PlayCardAction("X-EXCHANGE-1", EffectParameters.swap("GREEN-8", "RED-2")));
        assertTrue(s.board().stone(0).side(P1).contains("RED-2"));
        assertTrue(s.board().stone(4).side(P1).contains("GREEN-8"));
        assertEquals(done0, s.board().stone(0).side(P1).completedAt());
        assertTrue(ENGINE.checkClaim(s, P1, 4).allowed(), "Green 7-8-9 can be claimed early");
        assertTrue(ENGINE.checkClaim(s, P1, 0).reason().contains("Color-Run"));
    }

    @Test
    void clanExchangeRejectsOpponentCardsAndClaimedStones() {
        GameState s = TestSupport.newGame(false, true);
        place(s, P1, 0, clan("R1"));
        place(s, P2, 4, clan("G7"));
        place(s, P1, 6, clan("B7"));
        s.board().stone(6).claim(P1);
        hand(s, P1, card("X-EXCHANGE-1"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"));
        rejected(s, P1, new PlayCardAction("X-EXCHANGE-1", EffectParameters.swap("RED-1", "GREEN-7")));
        rejected(s, P1, new PlayCardAction("X-EXCHANGE-1", EffectParameters.swap("RED-1", "BLUE-7")));
    }

    @Test
    void gameWithoutExpansionsHasNoExpansionCard() {
        GameState s = TestSupport.newGame(true, false);
        assertTrue(s.deck(DeckType.EXPANSION).isEmpty());
        assertTrue(ENGINE.drawableDecks(s, P1).stream().noneMatch(d -> d == DeckType.EXPANSION));
    }

    @Test
    void recruiterCanUseExpansionDeck() throws Exception {
        GameState s = TestSupport.newGame(true, true);
        hand(s, P1, card("T-RECRUITER"), clan("R1"), clan("R2"), clan("R3"), clan("R4"), clan("R5"), clan("R6"));
        ENGINE.apply(s, P1, new PlayCardAction("T-RECRUITER", EffectParameters.draws(
                List.of(DeckType.EXPANSION, DeckType.EXPANSION, DeckType.TACTIC))));
        Card x = s.hand(P1).stream().filter(c -> c.deckType() == DeckType.EXPANSION).findFirst().orElseThrow();
        ENGINE.apply(s, P1, new ResolveEffectAction(EffectParameters.cards(List.of(x.id(), "RED-1"))));
        List<Card> exp = s.deck(DeckType.EXPANSION).orElseThrow().cards();
        assertEquals(x, exp.get(exp.size() - 1));
    }
}
