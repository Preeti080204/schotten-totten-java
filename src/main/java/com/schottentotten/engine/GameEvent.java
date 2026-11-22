package com.schottentotten.engine;

import com.schottentotten.model.PlayerId;

import java.util.Objects;

/**
 * Public notification of something that happened in the game. Events never reveal private
 * information (for example the identity of a drawn card).
 *
 * @param type    event kind
 * @param player  acting player, or {@code null}
 * @param message public, human readable description
 * @param stone   Stone concerned (0-8), or {@code -1}
 * @param cardId  card concerned, or {@code null}
 */
public record GameEvent(GameEventType type, PlayerId player, String message, int stone, String cardId) {

    /**
     * Validates the components.
     *
     * @param type    event kind
     * @param player  acting player
     * @param message description
     * @param stone   Stone index
     * @param cardId  card identifier
     */
    public GameEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(message, "message");
    }

    /**
     * Creates an event without Stone or card.
     *
     * @param type    event kind
     * @param player  acting player
     * @param message description
     * @return event
     */
    public static GameEvent of(GameEventType type, PlayerId player, String message) {
        return new GameEvent(type, player, message, -1, null);
    }
}
