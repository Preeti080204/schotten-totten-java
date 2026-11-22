package com.schottentotten.controller;

import com.schottentotten.TestSupport;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.model.Difficulty;
import com.schottentotten.model.FirstPlayerRule;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.model.TurnPhase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.schottentotten.TestSupport.ENGINE;
import static com.schottentotten.model.PlayerId.P1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSessionTest {

    @Test
    void twoViewsSeeTheSameChanges() {
        GameSession session = new GameSession(ENGINE, TestSupport.newGame(false, false));
        List<SessionUpdate> gui = new CopyOnWriteArrayList<>();
        List<SessionUpdate> cli = new CopyOnWriteArrayList<>();
        session.addListener(gui::add);
        session.addListener(cli::add);
        GameState s = session.snapshot();
        ActionOutcome out = session.submitAsHuman(new PlayCardAction(s.hand(P1).get(0).id(),
                EffectParameters.onStone(0)));
        assertTrue(out.accepted(), out.message());
        assertEquals(2, gui.size());
        assertEquals(2, cli.size());
        assertEquals(TestSupport.json(gui.get(1).snapshot()), TestSupport.json(cli.get(1).snapshot()));
    }

    @Test
    void rejectedActionsDoNotNotify() {
        GameSession session = new GameSession(ENGINE, TestSupport.newGame(false, false));
        AtomicInteger count = new AtomicInteger();
        session.addListener(u -> count.incrementAndGet());
        String before = TestSupport.json(session.snapshot());
        assertFalse(session.submitAsHuman(new PlayCardAction("NOPE", EffectParameters.onStone(0))).accepted());
        assertEquals(1, count.get());
        assertEquals(before, TestSupport.json(session.snapshot()));
    }

    @Test
    void concurrentSubmissionsCannotDoublePlay() throws Exception {
        GameSession session = new GameSession(ENGINE, TestSupport.newGame(false, false));
        GameState s = session.snapshot();
        Action a = new PlayCardAction(s.hand(P1).get(0).id(), EffectParameters.onStone(0));
        Action b = new PlayCardAction(s.hand(P1).get(1).id(), EffectParameters.onStone(1));
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        Thread t1 = new Thread(() -> {
            await(start);
            if (session.submitAsHuman(a).accepted()) {
                accepted.incrementAndGet();
            }
        });
        Thread t2 = new Thread(() -> {
            await(start);
            if (session.submitAsHuman(b).accepted()) {
                accepted.incrementAndGet();
            }
        });
        t1.start();
        t2.start();
        start.countDown();
        t1.join();
        t2.join();
        assertEquals(1, accepted.get());
        assertEquals(5, session.snapshot().handSize(P1));
    }

    @Test
    void viewsCannotMoveForVirtualPlayers() {
        GameConfiguration c = new GameConfiguration(PlayerProfile.virtual("Bot", Difficulty.EASY),
                PlayerProfile.human("Me"), false, false, 1, FirstPlayerRule.PLAYER_ONE, 1L);
        GameSession session = new GameSession(ENGINE, ENGINE.newGame(c));
        GameState s = session.snapshot();
        assertFalse(session.submitAsHuman(new PlayCardAction(s.hand(P1).get(0).id(),
                EffectParameters.onStone(0))).accepted());
    }

    @Test
    void virtualPlayerDriverPlaysAWholeGame() throws Exception {
        GameConfiguration c = TestSupport.aiConfig(true, true, Difficulty.HARD, Difficulty.EASY, 1, 8L);
        GameSession session = new GameSession(ENGINE, ENGINE.newGame(c));
        CountDownLatch done = new CountDownLatch(1);
        session.addListener(u -> {
            if (u.snapshot().phase() == TurnPhase.MATCH_OVER) {
                done.countDown();
            }
        });
        VirtualPlayerDriver driver = new VirtualPlayerDriver(session, 0);
        driver.start();
        assertTrue(done.await(60, TimeUnit.SECONDS), "AI vs AI session must finish");
        driver.shutdown();
        assertEquals(1, session.snapshot().match().results().size());
        GameState replay = session.initialState();
        for (GameSession.HistoryEntry e : session.history()) {
            ENGINE.apply(replay, e.actor(), e.action());
        }
        assertEquals(TestSupport.json(session.snapshot()), TestSupport.json(replay), "history replays exactly");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
