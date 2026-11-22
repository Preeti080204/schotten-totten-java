package com.schottentotten.engine;

/**
 * Thrown when an action violates the rules. The message is meant to be shown to the player.
 * When this exception is thrown the game state has not been modified.
 */
public class IllegalActionException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message explanation for the player
     */
    public IllegalActionException(String message) {
        super(message);
    }
}
