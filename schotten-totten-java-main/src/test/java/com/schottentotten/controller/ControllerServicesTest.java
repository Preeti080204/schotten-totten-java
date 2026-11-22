package com.schottentotten.controller;

import com.schottentotten.TestSupport;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.ParameterField;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.ClaimAction;
import com.schottentotten.engine.action.EndTurnAction;
import com.schottentotten.engine.action.NextRoundAction;
import com.schottentotten.engine.action.PassAction;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.engine.action.ResolveEffectAction;
import com.schottentotten.model.Card;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.Difficulty;
import com.schottentotten.model.GameState;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.persistence.SaveFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.schottentotten.TestSupport.ENGINE;
import static com.schottentotten.TestSupport.clan;
import static com.schottentotten.model.PlayerId.P1;
import static com.schottentotten.model.PlayerId.P2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the services shared by the two views: wording, the session hub, the controller
 * facade and the parameter chooser.
 */
class ControllerServicesTest {

    private final TextDescriber describer = new TextDescriber(ENGINE.catalog());

    @Test
    void describesEveryKindOfAction() {
        GameState s = TestSupport.newGame(true, true);
        Card red5 = clan("R5");
        TestSupport.place(s, P2, 2, red5);
        assertEquals("Play Red 5 on Stone 3", describer.action(new PlayCardAction(red5.id(),
                EffectParameters.onStone(2)), s));
        String traitor = describer.action(new PlayCardAction("T-TRAITOR",
                new EffectParameters(null, red5.id(), 4, null, null, null, null)), s);
        assertEquals("Play Traitor targeting Red 5 (Bob's side of Stone 3), to Stone 5", traitor);
        assertEquals("Resolve the effect: choosing Red 5", describer.action(new ResolveEffectAction(
                new EffectParameters(null, null, null, null, null, null, List.of(red5.id()))), s));
        assertEquals("Claim Stone 4", describer.action(new ClaimAction(3), s));
        assertEquals("End your turn", describer.action(new EndTurnAction(null), s));
        assertEquals("End your turn and draw from the Tactic deck", describer.action(new EndTurnAction(DeckType.TACTIC), s));
        assertEquals("Pass", describer.action(new PassAction(), s));
        assertEquals("Start the next round", describer.action(new NextRoundAction(), s));
    }

    @Test
    void describesParameterValuesAndPrompts() {
        GameState s = TestSupport.newGame(true, true);
        for (ParameterField f : ParameterField.values()) {
            assertFalse(describer.fieldPrompt(f).isBlank());
        }
        assertEquals("(none)", describer.value(ParameterField.STONE, null, s));
        assertEquals("Stone 9", describer.value(ParameterField.STONE, 8, s));
        assertEquals("the discard pile", describer.value(ParameterField.DESTINATION, EffectParameters.DISCARD, s));
        assertEquals("Expansion deck", describer.value(ParameterField.DECK, DeckType.EXPANSION, s));
        assertEquals("2 Clan + 1 Tactic", describer.value(ParameterField.DECKS,
                List.of(DeckType.CLAN, DeckType.TACTIC, DeckType.CLAN), s));
        assertEquals("Red 5, Joker", describer.value(ParameterField.CARDS, List.of(clan("R5").id(), "T-JOKER-1"), s));
        assertEquals("Red 5", describer.cardWithLocation(clan("R5").id(), s));
        assertEquals("UNKNOWN", describer.cardName("UNKNOWN"));
        String full = describer.action(new PlayCardAction("X-SCOUT-1",
                new EffectParameters(null, null, null, null, List.of(DeckType.CLAN), DeckType.CLAN, null)), s);
        assertTrue(full.contains("drawing 1 Clan") && full.contains("from the Clan deck"), full);
    }

    @Test
    void chooserNarrowsOneFieldAtATime() {
        List<EffectParameters> options = List.of(
                new EffectParameters(null, "A", 1, null, null, null, null),
                new EffectParameters(null, "A", 2, null, null, null, null),
                new EffectParameters(null, "B", 1, null, null, null, null));
        ParameterChooser chooser = new ParameterChooser(options);
        assertEquals(Optional.of(ParameterField.TARGET_CARD), chooser.nextField());
        assertEquals(List.of("A", "B"), chooser.values(ParameterField.TARGET_CARD));
        chooser.choose(ParameterField.TARGET_CARD, "A");
        assertEquals(Optional.of(ParameterField.DESTINATION), chooser.nextField());
        assertThrows(IllegalArgumentException.class, () -> chooser.choose(ParameterField.DESTINATION, 7));
        chooser.choose(ParameterField.DESTINATION, 2);
        assertTrue(chooser.isResolved());
        assertEquals(Optional.empty(), chooser.nextField());
        assertEquals(2, chooser.result().destination());
        assertThrows(IllegalArgumentException.class, () -> new ParameterChooser(List.of()));
    }

    @Test
    void hubNotifiesViewsAndClearsTheSession() {
        SessionHub hub = new SessionHub();
        List<GameSession> seen = new ArrayList<>();
        hub.addSessionChangeListener(seen::add);
        assertTrue(hub.current().isEmpty());
        GameSession session = new GameSession(ENGINE, TestSupport.newGame(false, false));
        hub.activate(session, 10);
        assertSame(session, hub.current().orElseThrow());
        assertTrue(hub.driver().isPresent());
        hub.clear();
        assertTrue(hub.current().isEmpty());
        assertTrue(hub.driver().isEmpty());
        assertEquals(2, seen.size());
        assertSame(session, seen.get(0));
        assertNull(seen.get(1));
        hub.shutdown();
    }

    @Test
    void controllerSavesLoadsAndResumes(@TempDir Path dir) throws Exception {
        SessionHub hub = new SessionHub();
        GameController controller = new GameController(ENGINE, hub, TestSupport.SAVES, () -> 5);
        assertThrows(IllegalStateException.class, () -> controller.save(dir.resolve("none.json")));
        assertTrue(controller.hint().isEmpty());
        GameSession first = controller.startNewGame(TestSupport.config(true, false));
        Optional<Action> hint = controller.hint();
        assertTrue(hint.isPresent());
        assertTrue(ENGINE.isLegal(first.snapshot(), P1, hint.get()));
        Path file = dir.resolve("game.json");
        controller.save(file);
        assertTrue(Files.size(file) > 100);
        GameSession loaded = controller.load(file);
        assertSame(loaded, hub.current().orElseThrow());
        assertEquals(TestSupport.json(first.snapshot()), TestSupport.json(loaded.snapshot()));
        Files.writeString(dir.resolve("broken.json"), "{ not json");
        assertThrows(SaveFormatException.class, () -> controller.load(dir.resolve("broken.json")));
        GameSession resumed = controller.resume(TestSupport.newGame(false, false));
        assertSame(resumed, hub.current().orElseThrow());
        controller.setAiDelay(1);
        hub.shutdown();
    }

    @Test
    void driverPausesAndResumesComputerPlayers() throws Exception {
        GameSession session = new GameSession(ENGINE, ENGINE.newGame(TestSupport.aiConfig(false, false,
                Difficulty.EASY, Difficulty.EASY, 1, 5)));
        VirtualPlayerDriver driver = new VirtualPlayerDriver(session, 5);
        driver.setPaused(true);
        assertTrue(driver.isPaused());
        driver.start();
        Thread.sleep(150);
        assertEquals(TurnPhase.PLAY, session.snapshot().phase());
        assertEquals(1, session.snapshot().turn());
        CountDownLatch moved = new CountDownLatch(1);
        session.addListener(u -> {
            if (u.snapshot().turn() > 1) {
                moved.countDown();
            }
        });
        driver.setDelay(1);
        driver.setPaused(false);
        assertTrue(moved.await(10, TimeUnit.SECONDS), "computer players did not resume");
        driver.shutdown();
    }
}
