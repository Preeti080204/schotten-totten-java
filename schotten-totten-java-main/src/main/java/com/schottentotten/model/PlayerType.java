package com.schottentotten.model;

/**
 * Who controls a seat.
 */
public enum PlayerType {
    /** A physical person using the GUI or the CLI. */
    HUMAN,
    /** A virtual player driven by a {@code PlayerStrategy}. */
    VIRTUAL
}
