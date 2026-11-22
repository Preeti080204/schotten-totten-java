package com.schottentotten.model;

import java.util.Set;

/**
 * The official Tactic cards of the Tactic variant (10 cards in total).
 *
 * <p>The rulebook prints the Traitor as "Traiter"; the identifier {@link #TRAITOR} is used
 * internally and in the user interface.</p>
 */
public enum TacticType {
    /** Wild colour and strength 1-9; at most one per player. */
    JOKER("Joker", TacticCategory.ELITE_TROOP, 2, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9),
            "Counts as a Clan card of any colour and strength, chosen when the Stone is"
                    + " claimed. Each player may play only one Joker."),
    /** Strength 7 with wild colour. */
    SPY("Spy", TacticCategory.ELITE_TROOP, 1, Set.of(7),
            "Counts as a Clan card of strength 7 in the colour of your choice."),
    /** Strength 1-3 with wild colour. */
    SHIELD_BEARER("Shield-Bearer", TacticCategory.ELITE_TROOP, 1, Set.of(1, 2, 3),
            "Counts as a Clan card of strength 1, 2 or 3 in the colour of your choice."),
    /** Stone decided by sum only. */
    BLIND_MANS_BLUFF("Blind-Man's Bluff", TacticCategory.COMBAT_MODE, 1, Set.of(),
            "Play on a Stone. That Stone is won by the highest total strength only;"
                    + " combinations are ignored."),
    /** Stone requires four cards per side. */
    MUD_FIGHT("Mud Fight", TacticCategory.COMBAT_MODE, 1, Set.of(),
            "Play on a Stone. Formations on that Stone need four cards on each side."),
    /** Draw three, return two. */
    RECRUITER("Recruiter", TacticCategory.RUSE, 1, Set.of(),
            "Draw three cards from one or both decks, then put two cards from your hand at the"
                    + " bottom of their decks."),
    /** Move or discard one of your own formation cards. */
    STRATEGIST("Strategist", TacticCategory.RUSE, 1, Set.of(),
            "Take one of your cards on an unclaimed Stone and move it to another unclaimed"
                    + " Stone, or discard it."),
    /** Discard one opposing formation card. */
    BANSHEE("Banshee", TacticCategory.RUSE, 1, Set.of(),
            "Discard one card from your opponent's side of an unclaimed Stone."),
    /** Steal one opposing Clan card. */
    TRAITOR("Traitor", TacticCategory.RUSE, 1, Set.of(),
            "Take a Clan card from your opponent's side of an unclaimed Stone and place it on"
                    + " your side of an unclaimed Stone.");

    private final String displayName;
    private final TacticCategory category;
    private final int copies;
    private final Set<Integer> eliteStrengths;
    private final String rulesText;

    TacticType(String displayName, TacticCategory category, int copies, Set<Integer> eliteStrengths,
               String rulesText) {
        this.displayName = displayName;
        this.category = category;
        this.copies = copies;
        this.eliteStrengths = eliteStrengths;
        this.rulesText = rulesText;
    }

    /**
     * Returns the card name.
     *
     * @return display name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the family of this card.
     *
     * @return tactic category
     */
    public TacticCategory category() {
        return category;
    }

    /**
     * Returns how many copies exist in the Tactic deck.
     *
     * @return number of copies
     */
    public int copies() {
        return copies;
    }

    /**
     * Returns the strengths an Elite Troop can take (empty for other categories).
     *
     * @return allowed strengths
     */
    public Set<Integer> eliteStrengths() {
        return eliteStrengths;
    }

    /**
     * Returns the rules text shown in the interfaces.
     *
     * @return rules text
     */
    public String rulesText() {
        return rulesText;
    }

    /**
     * Returns the effect identifier executed when the card is played.
     *
     * @return effect key
     */
    public String effectId() {
        return category == TacticCategory.ELITE_TROOP ? ClanCard.EFFECT_ID : "tactic." + name().toLowerCase();
    }
}
