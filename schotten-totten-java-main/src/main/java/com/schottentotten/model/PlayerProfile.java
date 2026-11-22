package com.schottentotten.model;

import java.util.Objects;

/**
 * Immutable description of a seat: name, controller type and (for virtual players) difficulty.
 *
 * @param name       player name shown in the interfaces
 * @param type       human or virtual
 * @param difficulty AI difficulty (ignored for humans)
 */
public record PlayerProfile(String name, PlayerType type, Difficulty difficulty) {

    /**
     * Validates the profile and normalises a blank name.
     *
     * @param name       player name
     * @param type       controller type
     * @param difficulty AI difficulty
     */
    public PlayerProfile {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(difficulty, "difficulty");
        name = (name == null || name.isBlank()) ? "Player" : name.strip();
        if (name.length() > 24) {
            name = name.substring(0, 24);
        }
    }

    /**
     * Creates a human profile.
     *
     * @param name player name
     * @return profile
     */
    public static PlayerProfile human(String name) {
        return new PlayerProfile(name, PlayerType.HUMAN, Difficulty.EASY);
    }

    /**
     * Creates a virtual-player profile.
     *
     * @param name       player name
     * @param difficulty AI difficulty
     * @return profile
     */
    public static PlayerProfile virtual(String name, Difficulty difficulty) {
        return new PlayerProfile(name, PlayerType.VIRTUAL, difficulty);
    }

    /**
     * Tells whether this seat is controlled by a strategy.
     *
     * @return {@code true} for virtual players
     */
    public boolean isVirtual() {
        return type == PlayerType.VIRTUAL;
    }
}
