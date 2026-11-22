package com.schottentotten.controller;

import com.schottentotten.ai.PlayerView;
import com.schottentotten.ai.StrategyFactory;
import com.schottentotten.engine.GameEngine;
import com.schottentotten.engine.action.Action;
import com.schottentotten.model.Difficulty;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.persistence.SaveFormatException;
import com.schottentotten.persistence.SaveManager;
import com.schottentotten.util.SeededRandom;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Application service shared by the GUI and the CLI: creates, saves and loads games and
 * publishes them through the common {@link SessionHub}. Views call this controller and the
 * session; they never touch the engine's mutation methods directly.
 */
public final class GameController {

    private final GameEngine engine;
    private final SessionHub hub;
    private final SaveManager saves;
    private final LongSupplier aiDelay;

    /**
     * Creates the controller.
     *
     * @param engine  rules engine
     * @param hub     shared session holder
     * @param saves   save-file manager
     * @param aiDelay supplies the current virtual-player delay in milliseconds
     */
    public GameController(GameEngine engine, SessionHub hub, SaveManager saves, LongSupplier aiDelay) {
        this.engine = engine;
        this.hub = hub;
        this.saves = saves;
        this.aiDelay = aiDelay;
    }

    /**
     * Returns the engine.
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
     * Returns the save-file manager.
     *
     * @return save manager
     */
    public SaveManager saves() {
        return saves;
    }

    /**
     * Starts a new game and makes it the active session of every view.
     *
     * @param config game configuration
     * @return the new session
     */
    public GameSession startNewGame(GameConfiguration config) {
        GameSession session = new GameSession(engine, engine.newGame(config));
        hub.activate(session, aiDelay.getAsLong());
        return session;
    }

    /**
     * Saves the active game.
     *
     * @param file target file
     * @throws IOException           if the file cannot be written
     * @throws IllegalStateException if no game is active
     */
    public void save(Path file) throws IOException {
        GameSession session = hub.current().orElseThrow(() -> new IllegalStateException("No game in progress."));
        saves.save(session.snapshot(), file);
    }

    /**
     * Loads a saved game and makes it the active session.
     *
     * @param file save file
     * @return the resumed session
     * @throws SaveFormatException if the file is missing, corrupted or incompatible
     */
    public GameSession load(Path file) throws SaveFormatException {
        return resume(saves.load(file));
    }

    /**
     * Makes a game state the active session of every view.
     *
     * @param state state to continue from (owned by the new session afterwards)
     * @return the new session
     */
    public GameSession resume(GameState state) {
        GameSession session = new GameSession(engine, state);
        hub.activate(session, aiDelay.getAsLong());
        return session;
    }

    /**
     * Changes the pace of the virtual players of the active game.
     *
     * @param delayMillis delay in milliseconds
     */
    public void setAiDelay(long delayMillis) {
        hub.driver().ifPresent(d -> d.setDelay(delayMillis));
    }

    /**
     * Suggests an action for the human who must act, computed by the advanced strategy from
     * that player's own point of view (no hidden information is used).
     *
     * @return suggested action, if a human must act
     */
    public Optional<Action> hint() {
        Optional<GameSession> session = hub.current();
        if (session.isEmpty()) {
            return Optional.empty();
        }
        GameState snapshot = session.get().snapshot();
        Optional<PlayerId> actor = engine.playerToAct(snapshot);
        if (actor.isEmpty() || snapshot.config().profile(actor.get()).isVirtual()) {
            return Optional.empty();
        }
        long seed = snapshot.config().seed() ^ snapshot.version();
        PlayerView view = PlayerView.of(snapshot, actor.get(), new SeededRandom(seed));
        return Optional.of(StrategyFactory.create(Difficulty.HARD, engine, seed).chooseAction(view));
    }
}
