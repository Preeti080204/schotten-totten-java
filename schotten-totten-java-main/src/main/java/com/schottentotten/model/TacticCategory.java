package com.schottentotten.model;

/**
 * The three official families of Tactic cards.
 */
public enum TacticCategory {
    /** Played in a formation slot like a Clan card. */
    ELITE_TROOP("Elite Troop"),
    /** Played on the Stone tile, changing how that Stone is decided. */
    COMBAT_MODE("Combat Mode"),
    /** Resolved immediately, then discarded face-up. */
    RUSE("Ruse");

    private final String displayName;

    TacticCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the category name.
     *
     * @return display name
     */
    public String displayName() {
        return displayName;
    }
}
