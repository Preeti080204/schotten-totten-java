package com.schottentotten.model;

import java.util.List;
import java.util.Objects;

/**
 * State of a multi-step effect waiting for its owner's second decision.
 *
 * @param effectId     effect that will resolve the decision
 * @param sourceCardId the card that started the effect
 * @param player       the player who must decide
 * @param deck         deck concerned (Highland Scout), may be {@code null}
 * @param cardIds      cards concerned (for example the scouted cards, top first)
 */
public record PendingEffect(String effectId, String sourceCardId, PlayerId player, DeckType deck,
                            List<String> cardIds) {

    /**
     * Validates and copies the components.
     *
     * @param effectId     effect identifier
     * @param sourceCardId source card identifier
     * @param player       deciding player
     * @param deck         deck concerned
     * @param cardIds      cards concerned
     */
    public PendingEffect {
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(sourceCardId, "sourceCardId");
        Objects.requireNonNull(player, "player");
        cardIds = cardIds == null ? List.of() : List.copyOf(cardIds);
    }
}
