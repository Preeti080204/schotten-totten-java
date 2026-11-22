package com.schottentotten.model;

import java.util.Objects;

/**
 * A custom expansion card (project requirement). Its behaviour is defined by the effect
 * registered under {@link ExpansionDefinition#effectId()}.
 *
 * @param id         stable identifier, for example {@code "X-SCOUT-1"}
 * @param definition the card design
 */
public record ExpansionCard(String id, ExpansionDefinition definition) implements Card {

    /**
     * Validates the components.
     *
     * @param id         identifier
     * @param definition design
     */
    public ExpansionCard {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(definition, "definition");
    }

    @Override
    public String displayName() {
        return definition.displayName();
    }

    @Override
    public DeckType deckType() {
        return DeckType.EXPANSION;
    }

    @Override
    public String effectId() {
        return definition.effectId();
    }

    @Override
    public String toString() {
        return id;
    }
}
