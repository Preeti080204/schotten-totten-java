package com.schottentotten.cards;

import com.schottentotten.engine.CardEffect;
import com.schottentotten.model.ExpansionDefinition;

import java.util.List;
import java.util.function.Supplier;

/**
 * The "Highland Muster" expansion: each entry pairs a card design with its effect. Adding a new
 * expansion card means adding one entry here and one {@link CardEffect} class; neither the engine
 * nor the views need to change.
 */
public final class ExpansionCatalog {

    /**
     * One expansion card design and the factory of its effect.
     *
     * @param definition card design
     * @param effect     effect factory
     */
    public record Entry(ExpansionDefinition definition, Supplier<CardEffect> effect) {
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry(new ExpansionDefinition("SCOUT", "Highland Scout", "Scouting Ruse", 2,
                    "Choose a deck. Look at its top 3 cards and put them back on top in any order.",
                    HighlandScoutEffect.ID), HighlandScoutEffect::new),
            new Entry(new ExpansionDefinition("WARD", "Stone Ward", "Ward", 2,
                    "Place beside your side of an unclaimed Stone where you have cards. For your opponent's next"
                            + " 2 turns, Banshee and Traitor cannot target your cards there.",
                    StoneWardEffect.ID), StoneWardEffect::new),
            new Entry(new ExpansionDefinition("EXCHANGE", "Clan Exchange", "Ruse", 2,
                    "Swap two of your cards located on two different unclaimed Stones.",
                    ClanExchangeEffect.ID), ClanExchangeEffect::new));

    private ExpansionCatalog() {
    }

    /**
     * Returns every expansion entry.
     *
     * @return immutable list of entries
     */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    /**
     * Returns the expansion card designs.
     *
     * @return definitions
     */
    public static List<ExpansionDefinition> definitions() {
        return ENTRIES.stream().map(Entry::definition).toList();
    }
}
