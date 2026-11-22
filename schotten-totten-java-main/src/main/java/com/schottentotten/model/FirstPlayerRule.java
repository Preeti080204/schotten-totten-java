package com.schottentotten.model;

/**
 * How the first player of the first round is chosen.
 */
public enum FirstPlayerRule {
    /** Player 1 starts. */
    PLAYER_ONE,
    /** Player 2 starts. */
    PLAYER_TWO,
    /** Chosen with the game's seeded random generator. */
    RANDOM
}
