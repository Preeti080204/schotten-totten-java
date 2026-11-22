package com.schottentotten.controller;

import com.schottentotten.engine.GameEngine;
import com.schottentotten.engine.GameEvent;
import com.schottentotten.engine.GameEventType;
import com.schottentotten.engine.IllegalActionException;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.NextRoundAction;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.TurnPhase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One live game: the single shared model behind every view (MVC controller facade).
 *
 * <p><b>Thread safety.</b> The GUI thread, the CLI thread and the virtual-player thread may all
 * submit actions. Every public method is synchronised, and an action is applied to a copy of the
 * state that replaces the current state only if the engine accepts it. So a rejected or failing
 * action can never leave a half-modified state, and two views can never interleave
 * inside one action.</p>
 *
 * <p>The session also records the actions applied since its creation, which allows a replay of
 * the game from the initial state.</p>
 */
public final class GameSession {

    /**
     * An applied action with its actor (for replays).
     *
     * @param actor  the acting player
     * @param action the action
     */
    public record HistoryEntry(PlayerId actor, Action action) {
    }

    private final GameEngine engine;
    private final GameState initialState;
    private final List<SessionListener> listeners = new CopyOnWriteArrayList<>();
    private final List<HistoryEntry> history = new ArrayList<>();
    private GameState state;

    /**
     * Creates a session around an existing state (new or loaded game).
     *
     * @param engine the rules engine
     * @param state  the initial state (copied)
     */
    public GameSession(GameEngine engine, GameState state) {
        this.engine = Objects.requireNonNull(engine);
        this.state = state.copy();
        this.initialState = state.copy();
    }

    /**
     * Returns the engine of this session.
     *
     * @return engine
     */
    public GameEngine engine() {
        return engine;
    }

    /**
     * Returns a deep copy of the current state. Callers may read it freely on any thread.
     *
     * @return state snapshot
     */
    public synchronized GameState snapshot() {
        return state.copy();
    }

    /**
     * Returns the current state version.
     *
     * @return version number
     */
    public synchronized long version() {
        return state.version();
    }

    /**
     * Returns the player who must act now, if any.
     *
     * @return player to act
     */
    public synchronized Optional<PlayerId> playerToAct() {
        return engine.playerToAct(state);
    }

    /**
     * Submits an action for a player.
     *
     * @param actor  acting player
     * @param action the action
     * @return outcome (rejections carry a player-facing message)
     */
    public synchronized ActionOutcome submit(PlayerId actor, Action action) {
        return submit(actor, action, -1);
    }

    /**
     * Submits an action only if the state has not changed since {@code expectedVersion} (used by
     * virtual players, whose decision may be outdated).
     *
     * @param actor           acting player
     * @param action          the action
     * @param expectedVersion required version, or {@code -1} to skip the check
     * @return outcome
     */
    public synchronized ActionOutcome submit(PlayerId actor, Action action, long expectedVersion) {
        if (expectedVersion >= 0 && expectedVersion != state.version()) {
            return ActionOutcome.rejected("The game changed before this action was submitted.");
        }
        GameState next = state.copy();
        List<GameEvent> events;
        try {
            events = engine.apply(next, actor, action);
        } catch (IllegalActionException e) {
            return ActionOutcome.rejected(e.getMessage());
        }
        state = next;
        history.add(new HistoryEntry(actor, action));
        publish(events);
        return new ActionOutcome(true, action.describe(), events);
    }

    /**
     * Submits an action on behalf of whichever <em>human</em> must act now. Used by the views,
     * which control human seats only; a view can never move for a virtual player.
     *
     * @param action the action
     * @return outcome
     */
    public synchronized ActionOutcome submitAsHuman(Action action) {
        Optional<PlayerId> toAct = engine.playerToAct(state);
        if (toAct.isEmpty()) {
            if (action instanceof NextRoundAction) {
                return submit(state.currentPlayer(), action);
            }
            return ActionOutcome.rejected(state.phase() == TurnPhase.MATCH_OVER
                    ? "The match is over." : "The round is over. Start the next round.");
        }
        PlayerId actor = toAct.get();
        if (state.config().profile(actor).isVirtual()) {
            return ActionOutcome.rejected("It is " + state.config().profile(actor).name()
                    + "'s turn (virtual player). Please wait.");
        }
        return submit(actor, action);
    }

    /**
     * Registers a listener and immediately sends it the current state.
     *
     * @param listener the listener
     */
    public synchronized void addListener(SessionListener listener) {
        listeners.add(listener);
        listener.onUpdate(new SessionUpdate(this, List.of(GameEvent.of(GameEventType.GAME_STARTED, null,
                "Session attached.")), state.copy()));
    }

    /**
     * Unregisters a listener.
     *
     * @param listener the listener
     */
    public void removeListener(SessionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns the state the session started from.
     *
     * @return copy of the initial state
     */
    public GameState initialState() {
        return initialState.copy();
    }

    /**
     * Returns the actions applied so far.
     *
     * @return unmodifiable copy of the history
     */
    public synchronized List<HistoryEntry> history() {
        return List.copyOf(history);
    }

    private void publish(List<GameEvent> events) {
        SessionUpdate update = new SessionUpdate(this, events, state.copy());
        for (SessionListener l : listeners) {
            try {
                l.onUpdate(update);
            } catch (RuntimeException e) {
                System.err.println("Session listener failed: " + e);
            }
        }
    }
}
