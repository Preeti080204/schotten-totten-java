package com.schottentotten.model;

import java.util.Objects;
import java.util.Set;

/**
 * An official Tactic card.
 *
 * @param id   stable identifier, for example {@code "T-JOKER-2"}
 * @param type the tactic card type
 */
public record TacticCard(String id, TacticType type) implements Card {

    /**
     * Validates the components.
     *
     * @param id   stable identifier
     * @param type tactic type
     */
    public TacticCard {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
    }

    @Override
    public String displayName() {
        return type.displayName();
    }

    @Override
    public DeckType deckType() {
        return DeckType.TACTIC;
    }

    @Override
    public String effectId() {
        return type.effectId();
    }

    @Override
    public boolean isFormationCard() {
        return type.category() == TacticCategory.ELITE_TROOP;
    }

    @Override
    public Set<Integer> allowedStrengths() {
        return type.eliteStrengths();
    }

    @Override
    public boolean hasWildColor() {
        return isFormationCard();
    }

    /**
     * Tells whether this card is a Joker.
     *
     * @return {@code true} for both Joker copies
     */
    public boolean isJoker() {
        return type == TacticType.JOKER;
    }

    @Override
    public String toString() {
        return id;
    }
}
