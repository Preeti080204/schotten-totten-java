package com.schottentotten.model;

import java.util.Objects;
import java.util.Set;

/**
 * One of the 54 Clan cards: a colour and a strength from 1 to 9.
 *
 * @param color    the clan colour
 * @param strength the strength, between {@link #MIN_STRENGTH} and {@link #MAX_STRENGTH}
 */
public record ClanCard(ClanColor color, int strength) implements Card {

    /** Weakest Clan strength. */
    public static final int MIN_STRENGTH = 1;
    /** Strongest Clan strength. */
    public static final int MAX_STRENGTH = 9;
    /** Effect key shared by every Clan card. */
    public static final String EFFECT_ID = "formation.place";

    /**
     * Validates the card values.
     *
     * @param color    the clan colour
     * @param strength the strength
     */
    public ClanCard {
        Objects.requireNonNull(color, "color");
        if (strength < MIN_STRENGTH || strength > MAX_STRENGTH) {
            throw new IllegalArgumentException("Strength must be 1..9: " + strength);
        }
    }

    @Override
    public String id() {
        return color.name() + "-" + strength;
    }

    @Override
    public String displayName() {
        return color.displayName() + " " + strength;
    }

    @Override
    public DeckType deckType() {
        return DeckType.CLAN;
    }

    @Override
    public String effectId() {
        return EFFECT_ID;
    }

    @Override
    public boolean isFormationCard() {
        return true;
    }

    @Override
    public Set<Integer> allowedStrengths() {
        return Set.of(strength);
    }

    @Override
    public String shortLabel() {
        return "" + color.letter() + strength;
    }

    @Override
    public String toString() {
        return id();
    }
}
