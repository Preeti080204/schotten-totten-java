package com.schottentotten.model;

import java.util.Objects;

/**
 * Describes one custom expansion card design.
 *
 * @param key         stable key used to build card identifiers, for example {@code "SCOUT"}
 * @param displayName name shown to players
 * @param category    short family label such as {@code "Ruse"} or {@code "Ward"}
 * @param copies      number of copies in the expansion deck
 * @param rulesText   full rules text
 * @param effectId    identifier of the effect registered for this card
 */
public record ExpansionDefinition(String key, String displayName, String category, int copies,
                                  String rulesText, String effectId) {

    /**
     * Validates the definition.
     *
     * @param key         stable key
     * @param displayName display name
     * @param category    category label
     * @param copies      number of copies
     * @param rulesText   rules text
     * @param effectId    effect identifier
     */
    public ExpansionDefinition {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(effectId, "effectId");
        if (copies < 1) {
            throw new IllegalArgumentException("copies must be positive");
        }
    }
}
