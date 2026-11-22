package com.schottentotten.persistence;

/**
 * Thrown when a save file cannot be read or is inconsistent. The message is meant for players.
 */
public class SaveFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message explanation
     */
    public SaveFormatException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a cause.
     *
     * @param message explanation
     * @param cause   underlying error
     */
    public SaveFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
