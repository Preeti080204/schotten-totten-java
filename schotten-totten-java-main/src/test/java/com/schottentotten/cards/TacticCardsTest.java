package com.schottentotten.cards;

import com.schottentotten.TestSupport;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.IllegalActionException;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.ClaimAction;
import com.schottentotten.engine.action.EndTurnAction;
import com.schottentotten.engine.action.PassAction;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.engine.action.ResolveEffectAction;
import com.schottentotten.model.Card;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.TurnPhase;
import org.junit.jupiter.api.Test;

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

class TacticCardsTest {

    private static GameState tactic() {
        return TestSupport.newGame(true, false);
    }

    private static void rejected(GameState s, PlayerId actor, Action a) {
        String before = TestSupport.json(s);
        assertThrows(IllegalActionException.class, () -> ENGINE.apply(s, actor, a));
        assertEquals(before, TestSupport.json(s));
    }

    private static PlayCardAction play(String id, EffectParameters p) {
        return new PlayCardAction(id, p);
    }

    @Test
    void jokerIsPlayedLikeAClanCardButOnlyOnce() throws Exception {
        GameState s = tactic();
        hand(s, P1, card("T-JOKER-1"), card("T-JOKER-2"), clan("R1"), clan("R2"), clan("R3"), clan("R4"), clan("R5"));
        ENGINE.apply(s, P1, play("T-JOKER-1", EffectParameters.onStone(0)));
        assertTrue(s.hasPlayedJoker(P1));
        assertEquals(1, s.specialPlayed(P1));
        ENGINE.apply(s, P1, new EndTurnAction(DeckType.CLAN));
        ENGINE.apply(s, P2, play(s.hand(P2).get(0).id(), EffectParameters.onStone(0)));
        ENGINE.apply(s, P2, new EndTurnAction(DeckType.CLAN));
        s.incrementSpecialPlayed(P2);
        rejected(s, P1, play("T-JOKER-2", EffectParameters.onStone(1)));
        assertTrue(ENGINE.legalActions(s).stream().noneMatch(a -> a instanceof PlayCardAction p
                && p.cardId().equals("T-JOKER-2")));
    }

    @Test
    void tacticCountRestrictionFollowsTheRulebookExample() throws Exception {
        GameState s = tactic();
        hand(s, P1, card("T-SPY"), card("T-SHIELD_BEARER"), clan("R1"), clan("R2"), clan("R3"), clan("R4"),
                clan("R5"));
        s.incrementSpecialPlayed(P1);
        s.incrementSpecialPlayed(P1);
        s.incrementSpecialPlayed(P2);
        rejected(s, P1, play("T-SPY", EffectParameters.onStone(0)));
        s.incrementSpecialPlayed(P2);
        ENGINE.apply(s, P1, play("T-SPY", EffectParameters.onStone(0)));
        assertEquals(3, s.specialPlayed(P1));
    }

    @Test
    void spyAndShieldBearerOccupyFormationSlots() throws Exception {
        GameState s = tactic();
        place(s, P1, 0, clan("R1"), clan("R2"));
        place(s, P2, 0, clan("B9"), clan("Y4"), clan("P1"));
        hand(s, P1, card("T-SHIELD_BEARER"), clan("G1"), clan("G2"), clan("G3"), clan("G4"), clan("G5"), clan("G6"));
        ENGINE.apply(s, P1, play("T-SHIELD_BEARER", EffectParameters.onStone(0)));
        assertTrue(s.board().stone(0).isComplete(P1));
        assertTrue(ENGINE.checkClaim(s, P1, 0).allowed(), "Red 1-2-3 colour run beats a sum");
        ENGINE.apply(s, P1, new ClaimAction(0));
    }

    @Test
    void mudFightRaisesCapacityToFour() throws Exception {
        GameState s = tactic();
        place(s, P1, 3, clan("R1"), clan("R2"), clan("R3"));
        place(s, P2, 3, clan("G1"), clan("G2"), clan("G3"));
        hand(s, P1, card("T-MUD_FIGHT"), clan("R4"), clan("B2"), clan("B3"), clan("B4"), clan("B5"), clan("B6"));
        ENGINE.apply(s, P1, play("T-MUD_FIGHT", EffectParameters.onStone(3)));
        assertEquals(4, s.board().stone(3).requiredSize());
        assertFalse(s.board().stone(3).isComplete(P1));
        assertFalse(ENGINE.checkClaim(s, P1, 3).allowed());
        ENGINE.apply(s, P1, new EndTurnAction(DeckType.CLAN));
        ENGINE.apply(s, P2, play(s.hand(P2).get(0).id(), EffectParameters.onStone(3)));
        assertEquals(4, s.board().stone(3).side(P2).size());
    }

    @Test
    void combatModeCannotBePlayedTwiceOrOnClaimedStone() {
        GameState s = tactic();
        hand(s, P1, card("T-BLIND_MANS_BLUFF"), clan("R4"), clan("B2"), clan("B3"), clan("B4"), clan("B5"),
                clan("B6"));
        s.board().stone(0).claim(P2);
        rejected(s, P1, play("T-BLIND_MANS_BLUFF", EffectParameters.onStone(0)));
    }

    @Test
    void blindMansBluffMakesSumDecide() throws Exception {
        GameState s = tactic();
        place(s, P1, 1, clan("R1"), clan("R2"), clan("R3"));
        place(s, P2, 1, clan("G9"), clan("B9"), clan("P8"));
        hand(s, P2, card("T-BLIND_MANS_BLUFF"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"), clan("Y6"));
        ENGINE.apply(s, P1, play(s.hand(P1).get(0).id(), EffectParameters.onStone(8)));
        ENGINE.apply(s, P1, new EndTurnAction(DeckType.CLAN));
        ENGINE.apply(s, P2, play("T-BLIND_MANS_BLUFF", EffectParameters.onStone(1)));
        assertTrue(ENGINE.checkClaim(s, P2, 1).allowed());
        ENGINE.apply(s, P2, new ClaimAction(1));
    }

    @Test
    void recruiterDrawsThreeAndReturnsTwo() throws Exception {
        GameState s = tactic();
        hand(s, P1, card("T-RECRUITER"), clan("R1"), clan("R2"), clan("R3"), clan("R4"), clan("R5"), clan("R6"));
        int clanBefore = s.deckSize(DeckType.CLAN);
        int tacticBefore = s.deckSize(DeckType.TACTIC);
        rejected(s, P1, play("T-RECRUITER", EffectParameters.draws(List.of(DeckType.CLAN))));
        ENGINE.apply(s, P1, play("T-RECRUITER", EffectParameters.draws(
                List.of(DeckType.CLAN, DeckType.CLAN, DeckType.TACTIC))));
        assertEquals(TurnPhase.RESOLVE_EFFECT, s.phase());
        assertEquals(9, s.handSize(P1));
        assertTrue(s.discard().contains(card("T-RECRUITER")));
        rejected(s, P1, new ResolveEffectAction(EffectParameters.cards(List.of("RED-1"))));
        rejected(s, P1, new EndTurnAction(DeckType.CLAN));
        Card tacticDrawn = s.hand(P1).stream().filter(c -> c.deckType() == DeckType.TACTIC).findFirst().orElseThrow();
        ENGINE.apply(s, P1, new ResolveEffectAction(EffectParameters.cards(List.of("RED-1", tacticDrawn.id()))));
        assertEquals(7, s.handSize(P1));
        assertEquals(clanBefore - 2 + 1, s.deckSize(DeckType.CLAN));
        assertEquals(tacticBefore, s.deckSize(DeckType.TACTIC));
        List<Card> clanDeck = s.deck(DeckType.CLAN).orElseThrow().cards();
        assertEquals(clan("R1"), clanDeck.get(clanDeck.size() - 1));
        List<Card> tacticDeck = s.deck(DeckType.TACTIC).orElseThrow().cards();
        assertEquals(tacticDrawn, tacticDeck.get(tacticDeck.size() - 1));
        assertEquals(TurnPhase.CLAIM, s.phase());
        assertTrue(ENGINE.drawableDecks(s, P1).isEmpty(), "hand is back to seven cards");
    }

    @Test
    void strategistMovesOrDiscardsOwnCard() throws Exception {
        GameState s = tactic();
        place(s, P1, 0, clan("R1"), clan("R2"), clan("R3"));
        place(s, P1, 5, clan("G1"), clan("G2"), clan("G3"));
        long completedAt = s.board().stone(0).side(P1).completedAt();
        hand(s, P1, card("T-STRATEGIST"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"), clan("Y6"));
        rejected(s, P1, play("T-STRATEGIST", EffectParameters.move("RED-1", 0)));
        rejected(s, P1, play("T-STRATEGIST", EffectParameters.move("RED-1", 5)));
        rejected(s, P1, play("T-STRATEGIST", EffectParameters.move(s.hand(P2).get(0).id(), 2)));
        ENGINE.apply(s, P1, play("T-STRATEGIST", EffectParameters.move("RED-1", 2)));
        assertFalse(s.board().stone(0).side(P1).contains("RED-1"));
        assertTrue(s.board().stone(2).side(P1).contains("RED-1"));
        assertEquals(-1, s.board().stone(0).side(P1).completedAt());
        assertTrue(completedAt > 0);
        assertTrue(s.discard().contains(card("T-STRATEGIST")));
    }

    @Test
    void strategistCanDiscard() throws Exception {
        GameState s = tactic();
        place(s, P1, 0, clan("R1"));
        hand(s, P1, card("T-STRATEGIST"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"), clan("Y6"));
        ENGINE.apply(s, P1, play("T-STRATEGIST", EffectParameters.move("RED-1", EffectParameters.DISCARD)));
        assertTrue(s.discard().contains(clan("R1")));
        assertEquals(0, s.board().stone(0).side(P1).size());
    }

    @Test
    void bansheeDiscardsOpponentCardOnUnclaimedStoneOnly() throws Exception {
        GameState s = tactic();
        place(s, P2, 0, clan("G9"));
        place(s, P2, 1, card("T-SPY"));
        s.board().stone(1).claim(P2);
        hand(s, P1, card("T-BANSHEE"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"), clan("Y6"));
        rejected(s, P1, play("T-BANSHEE", EffectParameters.target("T-SPY")));
        rejected(s, P1, play("T-BANSHEE", EffectParameters.target("YELLOW-1")));
        ENGINE.apply(s, P1, play("T-BANSHEE", EffectParameters.target("GREEN-9")));
        assertEquals(0, s.board().stone(0).side(P2).size());
        assertTrue(s.discard().containsAll(List.of(card("T-BANSHEE"), clan("G9"))));
    }

    @Test
    void bansheeCanRemoveEliteTroop() throws Exception {
        GameState s = tactic();
        place(s, P2, 0, card("T-JOKER-1"));
        hand(s, P1, card("T-BANSHEE"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"), clan("Y6"));
        ENGINE.apply(s, P1, play("T-BANSHEE", EffectParameters.target("T-JOKER-1")));
        assertTrue(s.discard().contains(card("T-JOKER-1")));
    }

    @Test
    void traitorStealsOnlyClanCards() throws Exception {
        GameState s = tactic();
        place(s, P2, 0, clan("G9"), card("T-SPY"));
        place(s, P1, 4, clan("R1"), clan("R2"), clan("R3"));
        hand(s, P1, card("T-TRAITOR"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"), clan("Y6"));
        rejected(s, P1, play("T-TRAITOR", EffectParameters.move("T-SPY", 2)));
        rejected(s, P1, play("T-TRAITOR", EffectParameters.move("GREEN-9", 4)));
        rejected(s, P1, play("T-TRAITOR", EffectParameters.move("GREEN-9", EffectParameters.DISCARD)));
        ENGINE.apply(s, P1, play("T-TRAITOR", EffectParameters.move("GREEN-9", 0)));
        assertTrue(s.board().stone(0).side(P1).contains("GREEN-9"));
        assertFalse(s.board().stone(0).side(P2).contains("GREEN-9"));
        assertTrue(s.discard().contains(card("T-TRAITOR")));
    }

    @Test
    void passIsAllowedWithOnlyTacticCards() throws Exception {
        GameState s = tactic();
        hand(s, P1, card("T-STRATEGIST"), card("T-BANSHEE"));
        assertTrue(ENGINE.legalActions(s).contains(new PassAction()));
        ENGINE.apply(s, P1, new PassAction());
        assertEquals(TurnPhase.CLAIM, s.phase());
    }

    @Test
    void ruseWithoutTargetCannotBePlayed() {
        GameState s = tactic();
        hand(s, P1, card("T-BANSHEE"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"), clan("Y6"));
        assertTrue(ENGINE.legalActions(s).stream().noneMatch(a -> a instanceof PlayCardAction p
                && p.cardId().equals("T-BANSHEE")));
    }
}
