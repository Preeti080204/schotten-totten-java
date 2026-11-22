package com.schottentotten.rules;

import java.util.Map;
import java.util.Objects;

/**
 * The value of a complete formation: its category and total strength, plus the identities
 * chosen for Elite Troops. Values are ordered by rank, then by sum; the completion-order
 * tie-break is applied by {@link ClaimResolver} because it is not a property of the cards.
 *
 * @param rank       formation category ({@link FormationRank#SUM} for sum-only Stones)
 * @param sum        total strength
 * @param identities identity chosen for each Elite Troop, keyed by card id
 */
public record FormationValue(FormationRank rank, int sum, Map<String, EliteIdentity> identities)
        implements Comparable<FormationValue> {

    /**
     * Validates and copies the components.
     *
     * @param rank       rank
     * @param sum        total strength
     * @param identities Elite Troop identities
     */
    public FormationValue {
        Objects.requireNonNull(rank, "rank");
        identities = Map.copyOf(identities);
    }

    @Override
    public int compareTo(FormationValue other) {
        int c = rank.compareTo(other.rank);
        return c != 0 ? c : Integer.compare(sum, other.sum);
    }

    /**
     * Tells whether this formation is strictly stronger than another.
     *
     * @param other the other formation
     * @return {@code true} if this beats {@code other} without needing a tie-break
     */
    public boolean beats(FormationValue other) {
        return compareTo(other) > 0;
    }

    /**
     * Returns a description such as {@code "Color-Run (24)"}.
     *
     * @param size       formation size
     * @param sumOnly    whether the Stone is decided by sum only
     * @return description
     */
    public String describe(int size, boolean sumOnly) {
        return sumOnly ? "Total " + sum : rank.displayName(size) + " (" + sum + ")";
    }
}
