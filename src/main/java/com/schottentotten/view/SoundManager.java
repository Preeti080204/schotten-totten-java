package com.schottentotten.view;

import com.schottentotten.app.GameSettings;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Plays the synthesised sound effects and the ambient music loop, honouring the user's sound,
 * music and volume preferences. Audio failures (for example a machine without an audio device)
 * silently disable sound instead of interrupting the game.
 */
public final class SoundManager {

    /** Sound effects. */
    public enum Sfx {
        /** Button press. */
        CLICK("click"),
        /** Card selected. */
        PICK("card-pick"),
        /** Card placed on the border. */
        PLACE("card-place"),
        /** Card drawn. */
        DRAW("card-draw"),
        /** Stone claimed. */
        CLAIM("claim"),
        /** New turn. */
        TURN("turn"),
        /** Refused action. */
        ERROR("error"),
        /** Special card effect. */
        EFFECT("effect"),
        /** Victory fanfare. */
        VICTORY("victory"),
        /** Defeat motif. */
        DEFEAT("defeat");

        private final String file;

        Sfx(String file) {
            this.file = file;
        }
    }

    private final GameSettings settings = GameSettings.getInstance();
    private final Map<Sfx, AudioClip> clips = new EnumMap<>(Sfx.class);
    private MediaPlayer music;
    private boolean musicWanted;
    private boolean broken;

    /**
     * Creates the sound manager and follows preference changes.
     */
    public SoundManager() {
        settings.addListener(() -> javafx.application.Platform.runLater(this::applySettings));
    }

    /**
     * Plays a sound effect if sound is enabled.
     *
     * @param sfx the effect
     */
    public void play(Sfx sfx) {
        if (broken || !settings.isSoundEnabled()) {
            return;
        }
        try {
            AudioClip clip = clips.computeIfAbsent(sfx, s -> new AudioClip(Assets.url("sounds/" + s.file + ".wav").toExternalForm()));
            clip.play(settings.volume());
        } catch (RuntimeException e) {
            disable(e);
        }
    }

    /**
     * Starts the ambient music loop (if music is enabled).
     */
    public void startMusic() {
        musicWanted = true;
        applySettings();
    }

    /**
     * Stops the ambient music.
     */
    public void stopMusic() {
        musicWanted = false;
        applySettings();
    }

    private void applySettings() {
        if (broken) {
            return;
        }
        try {
            if (musicWanted && settings.isMusicEnabled()) {
                if (music == null) {
                    music = new MediaPlayer(new Media(Assets.url("sounds/music-highlands.m4a").toExternalForm()));
                    music.setCycleCount(MediaPlayer.INDEFINITE);
                    music.setOnError(() -> disable(music.getError()));
                }
                music.setVolume(settings.volume() * 0.35);
                music.play();
            } else if (music != null) {
                music.pause();
            }
        } catch (RuntimeException e) {
            disable(e);
        }
    }

    /**
     * Fades the music out briefly (used on the victory screen).
     *
     * @param seconds duration of the dip
     */
    public void duckMusic(double seconds) {
        if (music != null && !broken) {
            double v = music.getVolume();
            music.setVolume(v * 0.3);
            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(Duration.seconds(seconds));
            p.setOnFinished(e -> music.setVolume(settings.volume() * 0.35));
            p.play();
        }
    }

    private void disable(Throwable e) {
        if (!broken) {
            broken = true;
            System.err.println("Audio disabled: " + (e == null ? "unknown error" : e.getMessage()));
        }
    }
}
