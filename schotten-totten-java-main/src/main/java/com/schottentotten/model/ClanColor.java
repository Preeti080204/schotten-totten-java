package com.schottentotten.model;

/**
 * The six Clan colours of the official deck.
 *
 * <p>The rulebook does not name all six colours; this implementation uses red, orange, yellow,
 * green, blue and purple (documented as an interpretation in {@code rules-specification.md}).</p>
 */
public enum ClanColor {
    /** Red clan. */
    RED("Red", 'R'),
    /** Orange clan. */
    ORANGE("Orange", 'O'),
    /** Yellow clan. */
    YELLOW("Yellow", 'Y'),
    /** Green clan. */
    GREEN("Green", 'G'),
    /** Blue clan. */
    BLUE("Blue", 'B'),
    /** Purple clan. */
    PURPLE("Purple", 'P');

    private final String displayName;
    private final char letter;

    ClanColor(String displayName, char letter) {
        this.displayName = displayName;
        this.letter = letter;
    }

    /**
     * Returns the English colour name.
     *
     * @return display name such as {@code "Red"}
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the single-letter abbreviation used by the command-line interface.
     *
     * @return colour letter
     */
    public char letter() {
        return letter;
    }

    /**
     * Parses a colour from its name or letter, ignoring case.
     *
     * @param text colour name ({@code "red"}) or letter ({@code "r"})
     * @return the matching colour
     * @throws IllegalArgumentException if no colour matches
     */
    public static ClanColor parse(String text) {
        String t = text.trim().toUpperCase();
        for (ClanColor c : values()) {
            if (c.name().equals(t) || (t.length() == 1 && c.letter == t.charAt(0))) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown colour: " + text);
    }
}
