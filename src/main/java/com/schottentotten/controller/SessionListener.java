package com.schottentotten.controller;

/**
 * Observer of a {@link GameSession} (Observer pattern). Both the GUI and the CLI register a
 * listener, which is how a move made in one view appears in the other.
 *
 * <p>Listeners are called on the thread that submitted the action, while the session lock is
 * held, so they must return quickly (the GUI re-dispatches to the JavaFX thread).</p>
 */
@FunctionalInterface
public interface SessionListener {

    /**
     * Called after the session state changed.
     *
     * @param update the change
     */
    void onUpdate(SessionUpdate update);
}
