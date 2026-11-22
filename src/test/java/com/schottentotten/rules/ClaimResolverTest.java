package com.schottentotten.rules;

import com.schottentotten.TestSupport;
import com.schottentotten.model.ClanColor;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.TacticCard;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.schottentotten.TestSupport.card;
import static com.schottentotten.TestSupport.clan;
import static com.schottentotten.TestSupport.hand;
import static com.schottentotten.TestSupport.place;
import static com.schottentotten.model.PlayerId.P1;
import static com.schottentotten.model.PlayerId.P2;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimResolverTest {

    private final ClaimResolver resolver = TestSupport.ENGINE.claimResolver();

    @Test
    void rulebookExampleNormalClaim() {
        GameState s = TestSupport.newGame(false, false);
        place(s, P2, 0, clan("G7"), clan("P4"), clan("B3"));
        place(s, P1, 0, clan("G5"), clan("R5"), clan("B5"));
        ClaimCheck c = resolver.check(s, P1, 0);
        assertTrue(c.allowed(), c.reason());
        assertFalse(c.early());
        assertFalse(resolver.check(s, P2, 0).allowed());
    }

    @Test
    void incompleteClaimantCannotClaim() {
        GameState s = TestSupport.newGame(false, false);
        place(s, P1, 0, clan("R9"), clan("R8"));
        assertFalse(resolver.check(s, P1, 0).allowed());
    }

    @Test
    void equalFormationsGoToWhoeverCompletedFirst() {
        GameState s = TestSupport.newGame(false, false);
        place(s, P2, 3, clan("G7"), clan("G8"), clan("G9"));
        place(s, P1, 3, clan("R7"), clan("R8"), clan("R9"));
        assertFalse(resolver.check(s, P1, 3).allowed());
        assertTrue(resolver.check(s, P2, 3).allowed());
    }

    @Test
    void earlyClaimAllowedWhenNothingCanBeatIt() {
        GameState s = TestSupport.newGame(false, false);
        place(s, P1, 2, clan("R7"), clan("R8"), clan("R9"));
        ClaimCheck c = resolver.check(s, P1, 2);
        assertTrue(c.allowed(), c.reason());
        assertTrue(c.early());
    }

    @Test
    void earlyClaimRejectedWhenOpponentCouldStillWin() {
        GameState s = TestSupport.newGame(false, false);
        place(s, P1, 2, clan("R1"), clan("R2"), clan("R3"));
        place(s, P2, 2, clan("Y8"), clan("Y9"));
        assertFalse(resolver.check(s, P1, 2).allowed());
    }

    @Test
    void earlyClaimUsesOnlyPublicCards() {
        GameState s = TestSupport.newGame(false, false);
        place(s, P1, 2, clan("R1"), clan("R2"), clan("R3"));
        place(s, P2, 2, clan("Y8"), clan("Y9"));
        hand(s, P1, clan("Y7"), clan("B1"), clan("B2"), clan("B3"), clan("B4"), clan("B5"));
        assertFalse(resolver.check(s, P1, 2).allowed(), "a card in the claimant's hand does not count");
        place(s, P1, 5, clan("Y7"));
        ClaimCheck c = resolver.check(s, P1, 2);
        assertTrue(c.allowed(), c.reason());
        assertTrue(c.early());
    }

    @Test
    void earlyClaimTieGoesToClaimantWhoCompletedFirst() {
        GameState s = TestSupport.newGame(false, false);
        place(s, P1, 0, clan("R7"), clan("R8"), clan("R9"));
        place(s, P2, 0, clan("G8"), clan("G9"));
        assertTrue(resolver.check(s, P1, 0).allowed());
    }

    @Test
    void earlyClaimConsidersUnplayedEliteTroops() {
        GameState s = TestSupport.newGame(true, false);
        place(s, P1, 0, clan("R6"), clan("R7"), clan("R8"));
        place(s, P2, 0, clan("G8"), clan("G9"));
        place(s, P1, 4, clan("G7"));
        assertFalse(resolver.check(s, P1, 0).allowed(), "an unplayed Joker could become Green 7");
        place(s, P1, 5, card("T-JOKER-1"));
        place(s, P1, 6, card("T-JOKER-2"));
        assertFalse(resolver.check(s, P1, 0).allowed(), "Spy could still complete 7-8-9 green");
        place(s, P1, 7, card("T-SPY"));
        assertTrue(resolver.check(s, P1, 0).allowed());
    }

    @Test
    void opponentWhoPlayedAJokerCannotUseTheSecond() {
        GameState s = TestSupport.newGame(true, false);
        place(s, P1, 0, clan("R6"), clan("R7"), clan("R8"));
        place(s, P2, 0, clan("G8"), clan("G9"));
        place(s, P1, 4, clan("G7"));
        place(s, P1, 7, card("T-SPY"));
        place(s, P2, 5, card("T-JOKER-1"));
        assertFalse(resolver.check(s, P1, 0).allowed());
        s.markJokerPlayed(P2);
        assertTrue(resolver.check(s, P1, 0).allowed());
    }

    @Test
    void mudFightNeedsFourCards() {
        GameState s = TestSupport.newGame(true, false);
        place(s, P1, 1, clan("R7"), clan("R8"), clan("R9"));
        assertTrue(resolver.check(s, P1, 1).allowed());
        s.board().stone(1).addCombatMode((TacticCard) card("T-MUD_FIGHT"));
        assertFalse(resolver.check(s, P1, 1).allowed());
        place(s, P1, 1, clan("R6"));
        assertTrue(resolver.check(s, P1, 1).allowed());
    }

    @Test
    void blindMansBluffComparesSumsOnly() {
        GameState s = TestSupport.newGame(true, false);
        s.board().stone(4).addCombatMode((TacticCard) card("T-BLIND_MANS_BLUFF"));
        place(s, P1, 4, clan("R1"), clan("R2"), clan("R3"));
        place(s, P2, 4, clan("G9"), clan("B9"), clan("P1"));
        assertFalse(resolver.check(s, P1, 4).allowed());
        assertTrue(resolver.check(s, P2, 4).allowed());
    }

    @Test
    void chosenEliteIdentityIsApplied() {
        GameState s = TestSupport.newGame(true, false);
        place(s, P1, 0, clan("R7"), clan("R8"), card("T-JOKER-1"));
        place(s, P2, 0, clan("G9"), clan("B9"), clan("P9"));
        assertTrue(resolver.check(s, P1, 0).allowed());
        assertFalse(resolver.check(s, P1, 0, Map.of("T-JOKER-1", new EliteIdentity(ClanColor.BLUE, 2))).allowed());
        assertFalse(resolver.check(s, P1, 0, Map.of("RED-7", new EliteIdentity(ClanColor.BLUE, 2))).allowed());
    }

    @Test
    void claimedStoneCannotBeClaimedAgain() {
        GameState s = TestSupport.newGame(false, false);
        place(s, P1, 0, clan("R7"), clan("R8"), clan("R9"));
        s.board().stone(0).claim(P1);
        assertFalse(resolver.check(s, P1, 0).allowed());
        assertFalse(resolver.check(s, PlayerId.P2, 0).allowed());
    }
}
