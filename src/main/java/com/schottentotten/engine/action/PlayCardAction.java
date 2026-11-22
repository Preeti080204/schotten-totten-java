package com.schottentotten.engine.action;

import com.schottentotten.engine.EffectParameters;

import java.util.Objects;

/**
 * Plays a card from the hand with the parameters its effect requires.
 *
 * @param cardId     the card to play
 * @param parameters the effect parameters (Stone, targets, ...)
 */
public record PlayCardAction(String cardId, EffectParameters parameters) implements Action {

    /**
     * Validates the components.
     *
     * @param cardId     card identifier
     * @param parameters effect parameters
     */
    public PlayCardAction {
        Objects.requireNonNull(cardId, "cardId");
        parameters = parameters == null ? EffectParameters.NONE : parameters;
    }

    @Override
    public String describe() {
        return "play " + cardId + " " + parameters.describe();
    }
}
