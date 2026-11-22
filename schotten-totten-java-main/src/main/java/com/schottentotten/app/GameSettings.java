package com.schottentotten.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.schottentotten.model.Difficulty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Application-wide user preferences (Singleton pattern).
 *
 * <p><b>Why a Singleton.</b> Preferences such as sound, motion, AI pace and the default
 * difficulty belong to the user and the installation, not to one game: every screen of the GUI,
 * and the virtual-player pace of games started from the command line, must read the same
 * values, and they are stored in exactly one file. A single,
 * lazily created instance (initialisation-on-demand holder, thread-safe without locks)
 * guarantees one source of truth.</p>
 *
 * <p><b>What is deliberately not here.</b> Game rules and per-game options live in the
 * immutable {@link com.schottentotten.model.GameConfiguration} passed explicitly to the engine.
 * The engine never reads this singleton, so several sessions with different rules can exist and
 * the engine remains testable without global state.</p>
 */
public final class GameSettings {

    /** Pace of virtual players and animations. */
    public enum Speed {
        /** Slow, for demonstrations. */
        SLOW(1400),
        /** Default pace. */
        NORMAL(750),
        /** Fast. */
        FAST(300),
        /** As fast as possible. */
        INSTANT(40);

        private final long aiDelayMillis;

        Speed(long aiDelayMillis) {
            this.aiDelayMillis = aiDelayMillis;
        }

        /**
         * Returns the delay before each virtual-player action.
         *
         * @return delay in milliseconds
         */
        public long aiDelayMillis() {
            return aiDelayMillis;
        }

        /**
         * Returns the name shown in menus.
         *
         * @return capitalised name, e.g. {@code "Normal"}
         */
        public String displayName() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    /** Persisted values. */
    private static final class Values {
        public boolean soundEnabled = true;
        public boolean musicEnabled = true;
        public double volume = 0.7;
        public boolean reduceMotion;
        public Speed speed = Speed.NORMAL;
        public Difficulty defaultDifficulty = Difficulty.EASY;
        public String lastPlayer1 = "Fergus";
        public String lastPlayer2 = "Isla";
        public boolean showHints = true;
    }

    private static final class Holder {
        private static final GameSettings INSTANCE = new GameSettings(defaultFile());
    }

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final Path file;
    private Values values;

    private GameSettings(Path file) {
        this.file = file;
        this.values = load();
    }

    /**
     * Returns the unique instance.
     *
     * @return settings
     */
    public static GameSettings getInstance() {
        return Holder.INSTANCE;
    }

    private static Path defaultFile() {
        String override = System.getProperty("schotten.settings");
        return override != null ? Path.of(override)
                : Path.of(System.getProperty("user.home"), ".schotten-totten", "settings.json");
    }

    private Values load() {
        try {
            if (Files.exists(file)) {
                return mapper.readValue(file.toFile(), Values.class);
            }
        } catch (IOException e) {
            System.err.println("Settings ignored (" + e.getMessage() + ")");
        }
        return new Values();
    }

    /**
     * Writes the preferences to disk. Failures are reported but never interrupt the game.
     */
    public synchronized void save() {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            mapper.writeValue(file.toFile(), values);
        } catch (IOException e) {
            System.err.println("Settings not saved (" + e.getMessage() + ")");
        }
    }

    /**
     * Registers a callback invoked after any change.
     *
     * @param listener callback
     */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void changed() {
        save();
        listeners.forEach(Runnable::run);
    }

    /**
     * Tells whether sound effects are enabled.
     *
     * @return sound flag
     */
    public synchronized boolean isSoundEnabled() {
        return values.soundEnabled;
    }

    /**
     * Enables or disables sound effects.
     *
     * @param enabled flag
     */
    public void setSoundEnabled(boolean enabled) {
        synchronized (this) {
            values.soundEnabled = enabled;
        }
        changed();
    }

    /**
     * Tells whether background music is enabled.
     *
     * @return music flag
     */
    public synchronized boolean isMusicEnabled() {
        return values.musicEnabled;
    }

    /**
     * Enables or disables background music.
     *
     * @param enabled flag
     */
    public void setMusicEnabled(boolean enabled) {
        synchronized (this) {
            values.musicEnabled = enabled;
        }
        changed();
    }

    /**
     * Returns the master volume.
     *
     * @return volume between 0 and 1
     */
    public synchronized double volume() {
        return values.volume;
    }

    /**
     * Sets the master volume.
     *
     * @param volume value between 0 and 1
     */
    public void setVolume(double volume) {
        synchronized (this) {
            values.volume = Math.max(0, Math.min(1, volume));
        }
        changed();
    }

    /**
     * Tells whether animations should be reduced.
     *
     * @return reduced-motion flag
     */
    public synchronized boolean isReduceMotion() {
        return values.reduceMotion;
    }

    /**
     * Enables or disables reduced motion.
     *
     * @param reduce flag
     */
    public void setReduceMotion(boolean reduce) {
        synchronized (this) {
            values.reduceMotion = reduce;
        }
        changed();
    }

    /**
     * Returns the game pace.
     *
     * @return speed
     */
    public synchronized Speed speed() {
        return values.speed;
    }

    /**
     * Sets the game pace.
     *
     * @param speed new speed
     */
    public void setSpeed(Speed speed) {
        synchronized (this) {
            values.speed = speed;
        }
        changed();
    }

    /**
     * Returns the default AI difficulty proposed on the new-game screen.
     *
     * @return difficulty
     */
    public synchronized Difficulty defaultDifficulty() {
        return values.defaultDifficulty;
    }

    /**
     * Sets the default AI difficulty.
     *
     * @param difficulty difficulty
     */
    public void setDefaultDifficulty(Difficulty difficulty) {
        synchronized (this) {
            values.defaultDifficulty = difficulty;
        }
        changed();
    }

    /**
     * Tells whether beginner hints are shown.
     *
     * @return hint flag
     */
    public synchronized boolean isShowHints() {
        return values.showHints;
    }

    /**
     * Enables or disables beginner hints.
     *
     * @param show flag
     */
    public void setShowHints(boolean show) {
        synchronized (this) {
            values.showHints = show;
        }
        changed();
    }

    /**
     * Returns the last names used on the new-game screen.
     *
     * @return two names
     */
    public synchronized String[] lastPlayerNames() {
        return new String[]{values.lastPlayer1, values.lastPlayer2};
    }

    /**
     * Remembers the names used for the last game.
     *
     * @param p1 Player 1 name
     * @param p2 Player 2 name
     */
    public void setLastPlayerNames(String p1, String p2) {
        synchronized (this) {
            values.lastPlayer1 = p1;
            values.lastPlayer2 = p2;
        }
        save();
    }
}
