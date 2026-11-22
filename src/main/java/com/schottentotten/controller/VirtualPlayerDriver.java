package com.schottentotten.controller;

import com.schottentotten.ai.PlayerStrategy;
import com.schottentotten.ai.PlayerView;
import com.schottentotten.ai.StrategyFactory;
import com.schottentotten.engine.GameEngine;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.NextRoundAction;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.util.SeededRandom;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Plays the virtual seats of a session. It observes the session and, whenever a virtual player
 * must act, asks that player's {@link PlayerStrategy} for a decision on a background thread and
 * submits it through the normal session API, so it gets exactly the same validation as a human.
 *
 * <p>Decisions carry the state version they were computed for. If a view changed the game in
 * the meantime, the session rejects the outdated action and the driver decides again from the
 * new state.</p>
 */
public final class VirtualPlayerDriver implements SessionListener {

    private final GameSession session;
    private final GameEngine engine;
    private final Map<PlayerId, PlayerStrategy> strategies = new EnumMap<>(PlayerId.class);
    private final ScheduledExecutorService executor;
    private final SeededRandom viewRandom;
    private volatile long delayMillis;
    private volatile boolean paused;

    /**
     * Creates a driver for a session.
     *
     * @param session     the session
     * @param delayMillis delay before each virtual-player action, in milliseconds
     */
    public VirtualPlayerDriver(GameSession session, long delayMillis) {
        this.session = session;
        this.engine = session.engine();
        this.delayMillis = delayMillis;
        GameState snapshot = session.snapshot();
        long seed = snapshot.config().seed();
        for (PlayerId p : PlayerId.values()) {
            PlayerProfile profile = snapshot.config().profile(p);
            if (profile.isVirtual()) {
                strategies.put(p, StrategyFactory.create(profile.difficulty(), engine, seed * 31 + p.ordinal()));
            }
        }
        this.viewRandom = new SeededRandom(seed ^ 0x5DEECE66DL);
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "virtual-players");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts observing the session (no effect if there is no virtual player).
     */
    public void start() {
        if (!strategies.isEmpty()) {
            session.addListener(this);
        }
    }

    /**
     * Changes the pace of the virtual players.
     *
     * @param delayMillis delay before each action, in milliseconds
     */
    public void setDelay(long delayMillis) {
        this.delayMillis = Math.max(0, delayMillis);
    }

    /**
     * Pauses or resumes the virtual players.
     *
     * @param paused {@code true} to pause
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
        if (!paused) {
            schedule(session.snapshot());
        }
    }

    /**
     * Tells whether the driver is paused.
     *
     * @return paused flag
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Stops the background thread and detaches from the session.
     */
    public void shutdown() {
        session.removeListener(this);
        executor.shutdownNow();
    }

    @Override
    public void onUpdate(SessionUpdate update) {
        schedule(update.snapshot());
    }

    private void schedule(GameState snapshot) {
        if (paused || executor.isShutdown()) {
            return;
        }
        Optional<PlayerId> actor = engine.playerToAct(snapshot);
        long version = snapshot.version();
        if (actor.isPresent() && strategies.containsKey(actor.get())) {
            long delay = snapshot.phase() == TurnPhase.PLAY ? delayMillis : delayMillis / 2;
            executor.schedule(() -> act(actor.get(), version), delay, TimeUnit.MILLISECONDS);
        } else if (snapshot.phase() == TurnPhase.ROUND_OVER && strategies.size() == PlayerId.values().length) {
            executor.schedule(() -> session.submit(snapshot.currentPlayer(), new NextRoundAction(), version),
                    delayMillis * 4, TimeUnit.MILLISECONDS);
        }
    }

    private void act(PlayerId actor, long version) {
        if (paused) {
            return;
        }
        GameState snapshot = session.snapshot();
        if (snapshot.version() != version) {
            return;
        }
        try {
            Action action = strategies.get(actor).chooseAction(PlayerView.of(snapshot, actor, viewRandom));
            ActionOutcome outcome = session.submit(actor, action, version);
            if (!outcome.accepted() && session.version() == version) {
                List<Action> legal = engine.legalActions(snapshot);
                if (!legal.isEmpty()) {
                    session.submit(actor, legal.get(0), version);
                }
            }
        } catch (RuntimeException e) {
            System.err.println("Virtual player error: " + e);
        }
    }
}
