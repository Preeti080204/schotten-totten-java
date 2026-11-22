package com.schottentotten.model;

import java.util.Objects;

/**
 * A Stone Ward expansion card protecting one side of a Stone, with its remaining duration.
 *
 * @param card                   the ward card itself (goes to the discard when it expires)
 * @param remainingOpponentTurns number of opponent turns the ward still lasts
 */
public record ActiveWard(Card card, int remainingOpponentTurns) {

    /**
     * Validates the components.
     *
     * @param card                   ward card
     * @param remainingOpponentTurns remaining duration
     */
    public ActiveWard {
        Objects.requireNonNull(card, "card");
        if (remainingOpponentTurns < 0) {
            throw new IllegalArgumentException("remaining turns cannot be negative");
        }
    }
}
