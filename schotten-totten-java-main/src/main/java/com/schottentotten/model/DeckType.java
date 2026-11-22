package com.schottentotten.model;

/**
 * The draw piles that can exist in a game.
 */
public enum DeckType {
    /** The 54 Clan cards (always present). */
    CLAN("Clan"),
    /** The 10 official Tactic cards (Tactic variant only). */
    TACTIC("Tactic"),
    /** The custom expansion cards (only when expansions are enabled). */
    EXPANSION("Expansion");

    private final String displayName;

    DeckType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns a human readable name.
     *
     * @return display name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Parses a deck name or its first letter, ignoring case.
     *
     * @param text text such as {@code "clan"}, {@code "t"} or {@code "exp"}
     * @return the deck type
     * @throws IllegalArgumentException if the text does not match a deck
     */
    public static DeckType parse(String text) {
        String t = text.trim().toUpperCase();
        for (DeckType d : values()) {
            if (!t.isEmpty() && d.name().startsWith(t)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown deck: " + text);
    }
}
