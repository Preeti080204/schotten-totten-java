package com.schottentotten.engine;

import com.schottentotten.model.Card;

import java.util.List;

/**
 * Behaviour executed when a card is played. Each card type has its own small implementation
 * (Strategy / Command style). The engine never names specific cards: it looks up the card's
 * {@link Card#effectId()} in the {@link EffectRegistry}.
 *
 * <p>Contract: {@link #validate} must reject every illegal parameter set without modifying the
 * state, and {@link #enumerate} must return exactly the parameter sets that {@link #validate}
 * accepts. The engine relies on this to compute legal moves for the AI and the views.</p>
 */
public interface CardEffect {

    /**
     * Returns the identifier under which this effect is registered.
     *
     * @return effect key
     */
    String id();

    /**
     * Checks that the parameters are legal. Must not modify the state.
     *
     * @param ctx    effect context
     * @param card   the card being played (still in the hand)
     * @param params chosen parameters
     * @throws IllegalActionException if the play is illegal, with a player-facing message
     */
    void validate(EffectContext ctx, Card card, EffectParameters params) throws IllegalActionException;

    /**
     * Applies the effect. Called only after {@link #validate} succeeded and after the card was
     * removed from the hand. The effect is responsible for putting the card where it belongs
     * (formation, Stone tile, discard, ...).
     *
     * @param ctx    effect context
     * @param card   the card being played
     * @param params validated parameters
     */
    void apply(EffectContext ctx, Card card, EffectParameters params);

    /**
     * Lists every legal parameter set for playing the card now.
     *
     * @param ctx  effect context
     * @param card the card in hand
     * @return legal parameter sets (empty if the card cannot be played)
     */
    List<EffectParameters> enumerate(EffectContext ctx, Card card);

    /**
     * Returns the instruction shown to the player when a field must be chosen.
     *
     * @param field the field being chosen
     * @return prompt text
     */
    default String prompt(ParameterField field) {
        return switch (field) {
            case STONE -> "Choose a Stone";
            case TARGET_CARD -> "Choose a card";
            case DESTINATION -> "Choose a destination";
            case SECOND_CARD -> "Choose a second card";
            case DECKS -> "Choose the decks to draw from";
            case DECK -> "Choose a deck";
            case CARDS -> "Choose cards";
        };
    }
}
