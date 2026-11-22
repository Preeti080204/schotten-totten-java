package com.schottentotten.persistence;

import com.schottentotten.TestSupport;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.Difficulty;
import com.schottentotten.model.GameState;
import com.schottentotten.model.TurnPhase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.schottentotten.TestSupport.ENGINE;
import static com.schottentotten.TestSupport.SAVES;
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

class SaveManagerTest {

    @Test
    void roundTripRestoresEverything(@TempDir Path dir) throws Exception {
        GameState s = ENGINE.newGame(TestSupport.aiConfig(true, true, Difficulty.EASY, Difficulty.HARD, 3, 99L));
        place(s, P1, 0, clan("R1"), card("T-JOKER-1"));
        s.markJokerPlayed(P1);
        s.incrementSpecialPlayed(P1);
        s.incrementSpecialPlayed(P2);
        hand(s, P1, card("X-WARD-1"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"), clan("Y6"));
        ENGINE.apply(s, P1, new PlayCardAction("X-WARD-1", EffectParameters.onStone(0)));
        Path file = dir.resolve("game" + SaveManager.EXTENSION);
        SAVES.save(s, file);
        GameState loaded = SAVES.load(file);
        assertEquals(TestSupport.json(s), TestSupport.json(loaded));
        assertEquals(TurnPhase.CLAIM, loaded.phase());
        assertEquals(s.deck(DeckType.CLAN).orElseThrow().cards(), loaded.deck(DeckType.CLAN).orElseThrow().cards());
        assertEquals(s.deck(DeckType.TACTIC).orElseThrow().cards(), loaded.deck(DeckType.TACTIC).orElseThrow().cards());
        assertEquals(2, loaded.board().stone(0).side(P1).ward().orElseThrow().remainingOpponentTurns());
        assertTrue(loaded.hasPlayedJoker(P1));
        assertEquals(2, loaded.specialPlayed(P1));
        assertEquals(s.rng().state(), loaded.rng().state());
        assertEquals(Difficulty.HARD, loaded.config().player2().difficulty());
        assertEquals(3, loaded.match().totalRounds());
    }

    @Test
    void loadedGameContinuesIdentically() throws Exception {
        GameState s = TestSupport.newGame(true, false);
        GameState loaded = SAVES.fromJson(SAVES.toJson(s));
        for (int i = 0; i < 30; i++) {
            List<Action> a = ENGINE.legalActions(s);
            List<Action> b = ENGINE.legalActions(loaded);
            assertEquals(a, b);
            Action chosen = a.get(i % a.size());
            ENGINE.apply(s, ENGINE.playerToAct(s).orElse(s.currentPlayer()), chosen);
            ENGINE.apply(loaded, ENGINE.playerToAct(loaded).orElse(loaded.currentPlayer()), chosen);
        }
        assertEquals(TestSupport.json(s), TestSupport.json(loaded));
    }

    @Test
    void pendingEffectIsPreserved() throws Exception {
        GameState s = TestSupport.newGame(false, true);
        hand(s, P1, card("X-SCOUT-1"), clan("Y1"), clan("Y2"), clan("Y3"), clan("Y4"), clan("Y5"));
        ENGINE.apply(s, P1, new PlayCardAction("X-SCOUT-1", EffectParameters.ofDeck(DeckType.CLAN)));
        GameState loaded = SAVES.fromJson(SAVES.toJson(s));
        assertEquals(TurnPhase.RESOLVE_EFFECT, loaded.phase());
        assertEquals(s.pending(), loaded.pending());
    }

    @Test
    void matchScoresArePreserved() throws Exception {
        GameState s = ENGINE.newGame(TestSupport.aiConfig(false, false, Difficulty.EASY, Difficulty.EASY, 2, 5L));
        s.board().stone(0).claim(P2);
        s.match().record(com.schottentotten.model.RoundResult.of(1, P1,
                com.schottentotten.model.VictoryReason.FIVE_STONES, s.board()));
        GameState loaded = SAVES.fromJson(SAVES.toJson(s));
        assertEquals(5, loaded.match().points(P1));
        assertEquals(1, loaded.match().points(P2));
    }

    @Test
    void invalidFilesGiveFriendlyErrors(@TempDir Path dir) throws Exception {
        assertThrows(SaveFormatException.class, () -> SAVES.fromJson("not json"));
        assertThrows(SaveFormatException.class, () -> SAVES.fromJson("{\"application\":\"other\"}"));
        String json = SAVES.toJson(TestSupport.newGame(false, false));
        SaveFormatException future = assertThrows(SaveFormatException.class,
                () -> SAVES.fromJson(json.replace("\"formatVersion\" : 1", "\"formatVersion\" : 99")));
        assertTrue(future.getMessage().contains("version"));
        SaveFormatException dup = assertThrows(SaveFormatException.class,
                () -> SAVES.fromJson(json.replaceFirst("\"RED-", "\"RED-X")));
        assertFalse(dup.getMessage().isBlank());
        assertThrows(SaveFormatException.class, () -> SAVES.load(dir.resolve("missing.json")));
        Path broken = dir.resolve("broken.json");
        Files.writeString(broken, json.substring(0, json.length() / 2));
        assertThrows(SaveFormatException.class, () -> SAVES.load(broken));
    }

    @Test
    void duplicatedCardIsDetected() {
        GameState s = TestSupport.newGame(false, false);
        String json = SAVES.toJson(s);
        String first = s.hand(P1).get(0).id();
        String second = s.hand(P1).get(1).id();
        String tampered = json.replace("\"" + second + "\"", "\"" + first + "\"");
        SaveFormatException e = assertThrows(SaveFormatException.class, () -> SAVES.fromJson(tampered));
        assertTrue(e.getMessage().contains("twice") || e.getMessage().contains("missing"), e.getMessage());
    }
}
