package com.schottentotten.controller;

import com.schottentotten.engine.GameEvent;
import com.schottentotten.model.GameState;

import java.util.List;

/**
 * Notification sent to the views after every successful action.
 *
 * @param session  the session that changed
 * @param events   public events produced by the action
 * @param snapshot deep copy of the state after the action (safe to read from any thread)
 */
public record SessionUpdate(GameSession session, List<GameEvent> events, GameState snapshot) {

    /**
     * Copies the event list.
     *
     * @param session  session
     * @param events   events
     * @param snapshot state copy
     */
    public SessionUpdate {
        events = List.copyOf(events);
    }
}
