package com.schottentotten.ai;

import com.schottentotten.TestSupport;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.ClaimAction;
import com.schottentotten.model.Card;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.Difficulty;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.util.SeededRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.schottentotten.TestSupport.ENGINE;
import static com.schottentotten.TestSupport.clan;
import static com.schottentotten.TestSupport.place;
import static com.schottentotten.model.PlayerId.P1;
import static com.schottentotten.model.PlayerId.P2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrategyTest {

    /** Plays a full match between strategies, asserting every chosen action is legal. */
    static GameState playOut(GameConfiguration config) throws Exception {
        GameState s = ENGINE.newGame(config);
        Map<PlayerId, PlayerStrategy> strategies = new EnumMap<>(PlayerId.class);
        for (PlayerId p : PlayerId.values()) {
            strategies.put(p, StrategyFactory.create(config.profile(p).difficulty(), ENGINE, config.seed() + p.ordinal()));
        }
        SeededRandom viewRng = new SeededRandom(config.seed());
        int steps = 0;
        while (s.phase() != TurnPhase.MATCH_OVER) {
            if (s.phase() == TurnPhase.ROUND_OVER) {
                ENGINE.apply(s, s.currentPlayer(), ENGINE.legalActions(s).get(0));
                continue;
            }
            PlayerId actor = ENGINE.playerToAct(s).orElseThrow();
            Action a = strategies.get(actor).chooseAction(PlayerView.of(s, actor, viewRng));
            assertTrue(ENGINE.legalActions(s).contains(a), "illegal AI action " + a.describe());
            ENGINE.apply(s, actor, a);
            steps++;
            if (steps > 3000) {
                throw new AssertionError("game did not finish");
            }
        }
        return s;
    }

    @ParameterizedTest
    @CsvSource({
            "false, false, EASY, EASY, 1",
            "false, false, HARD, EASY, 1",
            "true, false, EASY, HARD, 1",
            "true, false, HARD, HARD, 1",
            "false, true, HARD, EASY, 1",
            "true, true, EASY, EASY, 2",
            "true, true, HARD, HARD, 2"
    })
    void aiGamesFinishWithLegalMovesInEveryMode(boolean tactic, boolean expansions, Difficulty d1, Difficulty d2,
                                               int rounds) {
        for (long seed = 1; seed <= 3; seed++) {
            long sd = seed;
            GameState end = assertTimeoutPreemptively(Duration.ofSeconds(60),
                    () -> playOut(TestSupport.aiConfig(tactic, expansions, d1, d2, rounds, sd)));
            assertEquals(rounds, end.match().results().size());
        }
    }

    @Test
    void hardBeatsEasyMoreOftenThanNot() throws Exception {
        int hardWins = 0;
        int easyWins = 0;
        for (long seed = 1; seed <= 16; seed++) {
            boolean hardFirst = seed % 2 == 0;
            GameConfiguration c = TestSupport.aiConfig(false, false, hardFirst ? Difficulty.HARD : Difficulty.EASY,
                    hardFirst ? Difficulty.EASY : Difficulty.HARD, 1, seed);
            PlayerId winner = playOut(c).match().results().get(0).winner();
            if (winner == null) {
                continue;
            }
            boolean hardWon = (winner == P1) == hardFirst;
            if (hardWon) {
                hardWins++;
            } else {
                easyWins++;
            }
        }
        assertTrue(hardWins > easyWins, "hard " + hardWins + " vs easy " + easyWins);
    }

    @Test
    void strategiesAreReproducible() throws Exception {
        GameConfiguration c = TestSupport.aiConfig(true, true, Difficulty.HARD, Difficulty.EASY, 1, 11L);
        assertEquals(TestSupport.json(playOut(c)), TestSupport.json(playOut(c)));
    }

    @Test
    void aiAlwaysClaimsAvailableStones() {
        GameState s = TestSupport.newGame(false, false);
        place(s, P1, 0, clan("R7"), clan("R8"), clan("R9"));
        s.setPhase(TurnPhase.CLAIM);
        for (Difficulty d : Difficulty.values()) {
            Action a = StrategyFactory.create(d, ENGINE, 1).chooseAction(PlayerView.of(s, P1, new SeededRandom(1)));
            assertEquals(new ClaimAction(0), a);
        }
    }

    @Test
    void hardAiCompletesAWinningFormation() {
        GameState s = TestSupport.newGame(false, false);
        place(s, P1, 4, clan("R7"), clan("R8"));
        TestSupport.hand(s, P1, clan("R9"), clan("B1"), clan("G2"), clan("Y3"), clan("P1"), clan("O2"));
        Action a = StrategyFactory.create(Difficulty.HARD, ENGINE, 3).chooseAction(PlayerView.of(s, P1,
                new SeededRandom(3)));
        assertEquals("RED-9", ((com.schottentotten.engine.action.PlayCardAction) a).cardId());
    }

    @Test
    void hardAiBlocksThreeAdjacentThreat() {
        GameState s = TestSupport.newGame(false, false);
        s.board().stone(3).claim(P2);
        s.board().stone(4).claim(P2);
        place(s, P2, 5, clan("G7"), clan("G8"));
        place(s, P1, 2, clan("B1"));
        TestSupport.hand(s, P1, clan("R9"), clan("O9"), clan("P9"), clan("Y2"), clan("B5"), clan("Y6"));
        Action a = StrategyFactory.create(Difficulty.HARD, ENGINE, 3).chooseAction(PlayerView.of(s, P1,
                new SeededRandom(3)));
        int stone = ((com.schottentotten.engine.action.PlayCardAction) a).parameters().stone();
        assertTrue(stone == 5 || stone == 2, "should defend next to the opponent's pair, played " + (stone + 1));
    }

    @Test
    void viewHidesOpponentHandAndDeckOrder() {
        GameState s = TestSupport.newGame(true, true);
        List<Card> realOpp = s.hand(P2);
        boolean differs = false;
        for (int i = 0; i < 5; i++) {
            PlayerView v = PlayerView.of(s, P1, new SeededRandom(i));
            assertEquals(s.hand(P1), v.myHand());
            assertEquals(realOpp.size(), v.state().handSize(P2));
            assertEquals(s.board().stones().size(), v.state().board().stones().size());
            for (DeckType t : DeckType.values()) {
                assertEquals(s.deckSize(t), v.state().deckSize(t));
            }
            differs |= !v.state().hand(P2).equals(realOpp);
            List<Card> unseen = new ArrayList<>(realOpp);
            s.decks().values().forEach(d -> unseen.addAll(d.cards()));
            assertEquals(new HashSet<>(unseen), new HashSet<>(v.unseenCards()));
        }
        assertTrue(differs, "the opponent's real hand must not be passed through");
        assertNotEquals(s.deck(DeckType.CLAN).orElseThrow().cards(),
                PlayerView.of(s, P1, new SeededRandom(77)).state().deck(DeckType.CLAN).orElseThrow().cards());
    }
}
