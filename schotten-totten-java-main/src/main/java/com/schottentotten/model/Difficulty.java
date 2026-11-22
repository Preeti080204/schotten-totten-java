package com.schottentotten.model;

/**
 * Strength of a virtual player.
 */
public enum Difficulty {
    /** Simple but coherent strategy. */
    EASY("Easy"),
    /** Heuristic look-ahead strategy. */
    HARD("Hard");

    private final String displayName;

    Difficulty(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display name.
     *
     * @return display name
     */
    public String displayName() {
        return displayName;
    }
}
