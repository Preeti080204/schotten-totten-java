package com.schottentotten.engine;

/**
 * Kinds of notification published to the views.
 */
public enum GameEventType {
    /** A new game or a loaded game is now active. */
    GAME_STARTED,
    /** A new round has been dealt. */
    ROUND_STARTED,
    /** A card was placed in a formation. */
    CARD_PLACED,
    /** A Combat Mode was played on a Stone. */
    COMBAT_MODE_PLAYED,
    /** A Ruse or expansion effect was played. */
    EFFECT_PLAYED,
    /** A card was moved between Stones. */
    CARD_MOVED,
    /** A card was discarded from the board. */
    CARD_DISCARDED,
    /** A multi-step effect waits for a decision. */
    EFFECT_PENDING,
    /** A multi-step effect was resolved. */
    EFFECT_RESOLVED,
    /** The player passed. */
    PASSED,
    /** A Stone was claimed. */
    STONE_CLAIMED,
    /** A card was drawn. */
    CARD_DRAWN,
    /** The turn passed to the other player. */
    TURN_STARTED,
    /** A ward was placed or expired. */
    WARD_CHANGED,
    /** A round ended. */
    ROUND_OVER,
    /** The match ended. */
    MATCH_OVER
}
