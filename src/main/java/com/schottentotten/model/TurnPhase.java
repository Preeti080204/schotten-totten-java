package com.schottentotten.model;

/**
 * Phases of the turn state machine. The engine only accepts the actions listed for the current
 * phase, which makes it impossible to play twice, draw twice or claim at the wrong time.
 */
public enum TurnPhase {
    /** The current player must play one card (or pass when allowed). */
    PLAY,
    /** A multi-step card effect waits for its owner's decision (Recruiter, Highland Scout). */
    RESOLVE_EFFECT,
    /** After playing: claim any number of Stones, then end the turn (drawing a card). */
    CLAIM,
    /** The round is finished; waiting for the next round to start. */
    ROUND_OVER,
    /** The whole match is finished. No further action is possible. */
    MATCH_OVER
}
