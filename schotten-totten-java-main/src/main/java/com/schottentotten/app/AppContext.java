package com.schottentotten.app;

import com.schottentotten.cards.StandardRules;
import com.schottentotten.controller.GameController;
import com.schottentotten.controller.RulesDigest;
import com.schottentotten.controller.SessionHub;
import com.schottentotten.engine.GameEngine;
import com.schottentotten.persistence.SaveManager;

/**
 * Composition root: builds the engine, the shared session hub and the services used by both
 * views. Exactly one context exists per running application, so the GUI and the CLI started
 * together share the same live game.
 */
public final class AppContext {

    private final GameEngine engine;
    private final SessionHub hub;
    private final GameController controller;
    private final RulesDigest rules;

    private AppContext() {
        this.engine = StandardRules.createEngine();
        this.hub = new SessionHub();
        SaveManager saves = new SaveManager(engine.catalog());
        this.controller = new GameController(engine, hub, saves,
                () -> GameSettings.getInstance().speed().aiDelayMillis());
        this.rules = new RulesDigest(engine.catalog());
    }

    /**
     * Creates the application context.
     *
     * @return new context
     */
    public static AppContext create() {
        return new AppContext();
    }

    /**
     * Returns the rules engine.
     *
     * @return engine
     */
    public GameEngine engine() {
        return engine;
    }

    /**
     * Returns the shared session hub.
     *
     * @return hub
     */
    public SessionHub hub() {
        return hub;
    }

    /**
     * Returns the application controller.
     *
     * @return controller
     */
    public GameController controller() {
        return controller;
    }

    /**
     * Returns the rules digest.
     *
     * @return rules
     */
    public RulesDigest rules() {
        return rules;
    }

    /**
     * Stops background activity before the application exits.
     */
    public void shutdown() {
        hub.shutdown();
    }
}
