package com.schottentotten.model;

import java.util.Set;

/**
 * A playing card. Cards are immutable value objects identified by a stable {@link #id()}, which
 * is used in save files, commands and events.
 *
 * <p>Behaviour is not stored on the card itself: the engine looks up the card's
 * {@link #effectId()} in an effect registry. This keeps the card model free of rule logic and lets
 * new cards be added without modifying the engine.</p>
 */
public sealed interface Card permits ClanCard, TacticCard, ExpansionCard {

    /**
     * Returns the stable identifier of this card, unique within the full catalogue.
     *
     * @return identifier such as {@code "RED-5"} or {@code "T-JOKER-1"}
     */
    String id();

    /**
     * Returns the name shown to players.
     *
     * @return display name
     */
    String displayName();

    /**
     * Returns the deck this card belongs to (and returns to, for example with Recruiter).
     *
     * @return owning deck
     */
    DeckType deckType();

    /**
     * Returns the key of the effect that is executed when this card is played.
     *
     * @return effect identifier registered in the effect registry
     */
    String effectId();

    /**
     * Tells whether this card occupies a formation slot next to a Stone (Clan cards and Elite
     * Troops).
     *
     * @return {@code true} for formation cards
     */
    default boolean isFormationCard() {
        return false;
    }

    /**
     * Tells whether this card is a special card (Tactic or expansion) subject to the
     * special-card play restriction.
     *
     * @return {@code true} if playing it counts towards the special-card counter
     */
    default boolean isSpecial() {
        return deckType() != DeckType.CLAN;
    }

    /**
     * Returns the strengths this card may take inside a formation. A Clan card has exactly one;
     * Elite Troops may have several. Non-formation cards return an empty set.
     *
     * @return allowed strengths
     */
    default Set<Integer> allowedStrengths() {
        return Set.of();
    }

    /**
     * Tells whether the colour of this card is chosen freely by its owner (Elite Troops).
     *
     * @return {@code true} if the colour is wild
     */
    default boolean hasWildColor() {
        return false;
    }

    /**
     * Returns a short label for text interfaces.
     *
     * @return short label, for example {@code "R5"} or {@code "Joker"}
     */
    default String shortLabel() {
        return displayName();
    }
}
