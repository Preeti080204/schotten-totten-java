package com.schottentotten.controller;

import com.schottentotten.engine.GameEvent;

import java.util.List;

/**
 * Result of submitting an action to a session.
 *
 * @param accepted {@code true} if the action was applied
 * @param message  error message when rejected, otherwise a summary
 * @param events   events produced (empty when rejected)
 */
public record ActionOutcome(boolean accepted, String message, List<GameEvent> events) {

    /**
     * Copies the events.
     *
     * @param accepted acceptance flag
     * @param message  message
     * @param events   events
     */
    public ActionOutcome {
        events = List.copyOf(events);
    }

    /**
     * Creates a rejection.
     *
     * @param message reason
     * @return outcome
     */
    public static ActionOutcome rejected(String message) {
        return new ActionOutcome(false, message, List.of());
    }
}
