package com.schottentotten.rules;

import com.schottentotten.model.ClanColor;

import java.util.Objects;

/**
 * The colour and strength an Elite Troop takes when a Stone is claimed.
 *
 * @param color    chosen colour
 * @param strength chosen strength
 */
public record EliteIdentity(ClanColor color, int strength) {

    /**
     * Validates the identity.
     *
     * @param color    colour
     * @param strength strength
     */
    public EliteIdentity {
        Objects.requireNonNull(color, "color");
    }

    @Override
    public String toString() {
        return color.displayName() + " " + strength;
    }
}
