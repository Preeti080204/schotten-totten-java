package com.schottentotten.engine.action;

import com.schottentotten.rules.EliteIdentity;

import java.util.Map;

/**
 * Claims a Stone during the claim window.
 *
 * @param stone      Stone index (0-8)
 * @param identities optional identities chosen for the claimant's Elite Troops on that Stone;
 *                   missing ones take their strongest identity
 */
public record ClaimAction(int stone, Map<String, EliteIdentity> identities) implements Action {

    /**
     * Copies the identities.
     *
     * @param stone      Stone index
     * @param identities chosen identities
     */
    public ClaimAction {
        identities = identities == null ? Map.of() : Map.copyOf(identities);
    }

    /**
     * Claims a Stone with the strongest Elite Troop identities.
     *
     * @param stone Stone index
     */
    public ClaimAction(int stone) {
        this(stone, Map.of());
    }

    @Override
    public String describe() {
        return "claim stone " + (stone + 1) + (identities.isEmpty() ? "" : " as " + identities);
    }
}
