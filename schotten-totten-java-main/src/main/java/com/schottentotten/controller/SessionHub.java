package com.schottentotten.controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Holds the active {@link GameSession} of the application and notifies the views when it is
 * replaced (new game, loaded game). The GUI and the CLI share one hub, so they always act on
 * the same live game.
 */
public final class SessionHub {

    private final List<Consumer<GameSession>> listeners = new CopyOnWriteArrayList<>();
    private volatile GameSession current;
    private volatile VirtualPlayerDriver driver;

    /** Creates a hub with no active session. */
    public SessionHub() {
    }

    /**
     * Returns the active session.
     *
     * @return the session, if a game is active
     */
    public Optional<GameSession> current() {
        return Optional.ofNullable(current);
    }

    /**
     * Replaces the active session, starts a virtual-player driver for it and notifies the views.
     *
     * @param session    new session
     * @param thinkDelay delay before each virtual-player action, in milliseconds
     */
    public synchronized void activate(GameSession session, long thinkDelay) {
        if (driver != null) {
            driver.shutdown();
        }
        current = session;
        driver = new VirtualPlayerDriver(session, thinkDelay);
        listeners.forEach(l -> l.accept(session));
        driver.start();
    }

    /**
     * Returns the virtual-player driver of the active session.
     *
     * @return driver, if a session is active
     */
    public Optional<VirtualPlayerDriver> driver() {
        return Optional.ofNullable(driver);
    }

    /**
     * Closes the active session (back to the main menu): stops its virtual players and tells the
     * views, which receive {@code null}.
     */
    public synchronized void clear() {
        if (driver != null) {
            driver.shutdown();
        }
        driver = null;
        current = null;
        listeners.forEach(l -> l.accept(null));
    }

    /**
     * Registers a listener notified whenever the active session changes.
     *
     * @param listener callback receiving the new session, or {@code null} when it is closed
     */
    public void addSessionChangeListener(Consumer<GameSession> listener) {
        listeners.add(listener);
    }

    /**
     * Stops background activity (called when the application exits).
     */
    public synchronized void shutdown() {
        if (driver != null) {
            driver.shutdown();
        }
    }
}
