package com.schottentotten.engine;

import com.schottentotten.TestSupport;
import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.FirstPlayerRule;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.model.TurnPhase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSetupTest {

    @Test
    void baseGameDealsSixClanCardsEach() {
        GameState s = TestSupport.newGame(false, false);
        assertEquals(9, s.board().stones().size());
        for (PlayerId p : PlayerId.values()) {
            assertEquals(6, s.handSize(p));
            assertTrue(s.hand(p).stream().allMatch(c -> c instanceof ClanCard));
        }
        assertEquals(54 - 12, s.deckSize(DeckType.CLAN));
        assertTrue(s.deck(DeckType.TACTIC).isEmpty());
        assertTrue(s.deck(DeckType.EXPANSION).isEmpty());
        assertEquals(TurnPhase.PLAY, s.phase());
        assertEquals(PlayerId.P1, s.currentPlayer());
    }

    @Test
    void tacticVariantDealsSevenClanCardsAndKeepsDecksSeparate() {
        GameState s = TestSupport.newGame(true, false);
        for (PlayerId p : PlayerId.values()) {
            assertEquals(7, s.handSize(p));
            assertTrue(s.hand(p).stream().allMatch(c -> c instanceof ClanCard));
        }
        assertEquals(54 - 14, s.deckSize(DeckType.CLAN));
        assertEquals(10, s.deckSize(DeckType.TACTIC));
        assertTrue(s.deck(DeckType.TACTIC).orElseThrow().cards().stream().allMatch(c -> c.deckType() == DeckType.TACTIC));
    }

    @Test
    void expansionDeckOnlyWhenEnabled() {
        assertEquals(6, TestSupport.newGame(false, true).deckSize(DeckType.EXPANSION));
        assertEquals(6, TestSupport.newGame(true, true).deckSize(DeckType.EXPANSION));
        assertTrue(TestSupport.newGame(true, false).deck(DeckType.EXPANSION).isEmpty());
    }

    @Test
    void everyCardIsDealtExactlyOnce() {
        GameState s = TestSupport.newGame(true, true);
        List<Card> all = new ArrayList<>();
        all.addAll(s.hand(PlayerId.P1));
        all.addAll(s.hand(PlayerId.P2));
        s.decks().values().forEach(d -> all.addAll(d.cards()));
        assertEquals(70, all.size());
        assertEquals(70, new HashSet<>(all).size());
    }

    @Test
    void shufflingIsReproducibleWithTheSameSeed() {
        GameState a = TestSupport.newGame(true, true);
        GameState b = TestSupport.newGame(true, true);
        assertEquals(TestSupport.json(a), TestSupport.json(b));
        GameConfiguration other = new GameConfiguration(PlayerProfile.human("Alice"), PlayerProfile.human("Bob"),
                true, true, 1, FirstPlayerRule.PLAYER_ONE, 7L);
        assertNotEquals(a.deck(DeckType.CLAN).orElseThrow().cards(),
                TestSupport.ENGINE.newGame(other).deck(DeckType.CLAN).orElseThrow().cards());
    }

    @Test
    void firstPlayerRuleIsApplied() {
        GameConfiguration p2 = new GameConfiguration(PlayerProfile.human("A"), PlayerProfile.human("B"), false, false,
                1, FirstPlayerRule.PLAYER_TWO, 1L);
        assertEquals(PlayerId.P2, TestSupport.ENGINE.newGame(p2).currentPlayer());
        boolean sawP1 = false;
        boolean sawP2 = false;
        for (long seed = 0; seed < 20; seed++) {
            GameConfiguration r = new GameConfiguration(PlayerProfile.human("A"), PlayerProfile.human("B"), false,
                    false, 1, FirstPlayerRule.RANDOM, seed);
            PlayerId first = TestSupport.ENGINE.newGame(r).currentPlayer();
            sawP1 |= first == PlayerId.P1;
            sawP2 |= first == PlayerId.P2;
        }
        assertTrue(sawP1 && sawP2);
        assertFalse(TestSupport.newGame(false, false).config().isMultiRound());
    }
}
