package com.schottentotten.model;

/**
 * Why a round ended.
 */
public enum VictoryReason {
    /** Three adjacent Stones controlled. */
    THREE_ADJACENT("controls three adjacent Stones"),
    /** Five Stones controlled anywhere. */
    FIVE_STONES("controls five Stones"),
    /** Both players passed consecutively; the player with more Stones wins (custom rule). */
    DEADLOCK_MORE_STONES("controls more Stones when no further play was possible"),
    /** Both players passed consecutively with equal Stones (custom rule). */
    DEADLOCK_DRAW("the border is deadlocked with equal Stones");

    private final String description;

    VictoryReason(String description) {
        this.description = description;
    }

    /**
     * Returns an explanatory phrase.
     *
     * @return description
     */
    public String description() {
        return description;
    }
}
