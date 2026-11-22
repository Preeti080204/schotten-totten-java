package com.schottentotten.rules;

/**
 * Result of checking whether a Stone can be claimed.
 *
 * @param allowed  {@code true} if the claim is legal
 * @param reason   human readable explanation
 * @param claimant value of the claimant's formation (may be {@code null} if incomplete)
 * @param opponent value of the opponent's formation ({@code null} if incomplete)
 * @param early    {@code true} if the claim relies on the early-claim proof
 */
public record ClaimCheck(boolean allowed, String reason, FormationValue claimant, FormationValue opponent,
                         boolean early) {

    /**
     * Creates a refusal.
     *
     * @param reason explanation
     * @return refused check
     */
    public static ClaimCheck refused(String reason) {
        return new ClaimCheck(false, reason, null, null, false);
    }
}
