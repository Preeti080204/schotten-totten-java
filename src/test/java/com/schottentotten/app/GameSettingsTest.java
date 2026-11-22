package com.schottentotten.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schottentotten.model.Difficulty;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the settings Singleton. Surefire points {@code schotten.settings} to the build
 * directory, so the user's own preferences are never touched.
 */
class GameSettingsTest {

    @Test
    void thereIsExactlyOneInstance() {
        assertSame(GameSettings.getInstance(), GameSettings.getInstance());
    }

    @Test
    void changesArePersistedAndAnnounced() throws Exception {
        GameSettings settings = GameSettings.getInstance();
        AtomicInteger notified = new AtomicInteger();
        settings.addListener(notified::incrementAndGet);
        settings.setSoundEnabled(false);
        settings.setMusicEnabled(false);
        settings.setVolume(0.25);
        settings.setReduceMotion(true);
        settings.setSpeed(GameSettings.Speed.FAST);
        settings.setDefaultDifficulty(Difficulty.HARD);
        settings.setShowHints(false);
        assertEquals(7, notified.get());
        settings.setLastPlayerNames("Morag", "Angus");

        Path file = Path.of(System.getProperty("schotten.settings"));
        JsonNode json = new ObjectMapper().readTree(file.toFile());
        assertEquals(false, json.get("soundEnabled").asBoolean());
        assertEquals(0.25, json.get("volume").asDouble(), 1e-9);
        assertEquals("FAST", json.get("speed").asText());
        assertEquals("HARD", json.get("defaultDifficulty").asText());
        assertEquals("Morag", json.get("lastPlayer1").asText());
        assertArrayEquals(new String[]{"Morag", "Angus"}, settings.lastPlayerNames());
        assertTrue(settings.isReduceMotion());

        settings.setVolume(4);
        assertEquals(1.0, settings.volume(), 1e-9);
        settings.setVolume(-1);
        assertEquals(0.0, settings.volume(), 1e-9);
    }

    @Test
    void speedsAreOrderedFromSlowToInstant() {
        long previous = Long.MAX_VALUE;
        for (GameSettings.Speed s : GameSettings.Speed.values()) {
            assertTrue(s.aiDelayMillis() < previous);
            previous = s.aiDelayMillis();
        }
        assertEquals("Normal", GameSettings.Speed.NORMAL.displayName());
    }
}
