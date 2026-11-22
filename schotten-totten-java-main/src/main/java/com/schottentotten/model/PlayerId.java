package com.schottentotten.model;

/**
 * Identifies one of the two seats at the table.
 *
 * <p>Player 1 and Player 2 are fixed seats for the whole match; names and controller types are
 * stored separately in {@link PlayerProfile}.</p>
 */
public enum PlayerId {
    /** The first seat. */
    P1,
    /** The second seat. */
    P2;

    /**
     * Returns the other seat.
     *
     * @return the opponent of this player
     */
    public PlayerId opponent() {
        return this == P1 ? P2 : P1;
    }

    /**
     * Returns the human-friendly seat number.
     *
     * @return {@code 1} or {@code 2}
     */
    public int number() {
        return ordinal() + 1;
    }
}
