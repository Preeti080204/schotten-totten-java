package com.schottentotten.rules;

/**
 * Formation categories from the weakest to the strongest ({@link #ordinal()} order is the
 * strength order used for comparisons).
 */
public enum FormationRank {
    /** Any combination. */
    SUM("Sum"),
    /** Consecutive strengths, any colours. */
    RUN("Run"),
    /** Same colour. */
    COLOR("Color"),
    /** Same strength (Three of a Kind, or Four of a Kind under Mud Fight). */
    SAME_STRENGTH("Three of a Kind"),
    /** Same colour and consecutive strengths. */
    COLOR_RUN("Color-Run");

    private final String displayName;

    FormationRank(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the official name of the category.
     *
     * @return display name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the name adapted to the formation size ("Four of a Kind" under Mud Fight).
     *
     * @param size formation size
     * @return display name
     */
    public String displayName(int size) {
        if (this == SAME_STRENGTH && size == 4) {
            return "Four of a Kind";
        }
        return displayName;
    }
}
