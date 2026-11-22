package com.schottentotten.engine;

/**
 * The fields of {@link EffectParameters}, in the order in which views ask the player for them.
 * Views read the legal parameter sets of an effect and ask for one field at a time, filtering
 * the remaining candidates after each choice.
 */
public enum ParameterField {
    /** Deck sources of a multi-draw. */
    DECKS,
    /** A single deck. */
    DECK,
    /** A target Stone. */
    STONE,
    /** A target card on the board. */
    TARGET_CARD,
    /** Destination Stone or discard. */
    DESTINATION,
    /** A second card on the board. */
    SECOND_CARD,
    /** A list of chosen cards. */
    CARDS;

    /**
     * Reads this field from parameters.
     *
     * @param p parameters
     * @return the field value ({@code null} or an empty list when unused)
     */
    public Object read(EffectParameters p) {
        return switch (this) {
            case DECKS -> p.decks().isEmpty() ? null : p.decks();
            case DECK -> p.deck();
            case STONE -> p.stone();
            case TARGET_CARD -> p.targetCardId();
            case DESTINATION -> p.destination();
            case SECOND_CARD -> p.secondCardId();
            case CARDS -> p.cardIds().isEmpty() ? null : p.cardIds();
        };
    }
}
