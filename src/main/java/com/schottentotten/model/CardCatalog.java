package com.schottentotten.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The complete set of cards that can appear in a game, indexed by stable identifier.
 *
 * <p>The catalogue is immutable. It is used to rebuild cards from identifiers (save files,
 * command-line input) and to create the decks at set-up.</p>
 */
public final class CardCatalog {

    private final Map<String, Card> byId;

    /**
     * Builds a catalogue with every Clan card, every Tactic card and the given expansion designs.
     *
     * @param expansionDefinitions expansion card designs to include
     */
    public CardCatalog(Collection<ExpansionDefinition> expansionDefinitions) {
        Map<String, Card> map = new LinkedHashMap<>();
        clanCards().forEach(c -> map.put(c.id(), c));
        tacticCards().forEach(c -> map.put(c.id(), c));
        expansionCards(expansionDefinitions).forEach(c -> map.put(c.id(), c));
        this.byId = Map.copyOf(map);
    }

    /**
     * Creates the 54 Clan cards, one per colour and strength.
     *
     * @return new list of Clan cards ordered by colour then strength
     */
    public static List<ClanCard> clanCards() {
        List<ClanCard> cards = new ArrayList<>();
        for (ClanColor color : ClanColor.values()) {
            for (int s = ClanCard.MIN_STRENGTH; s <= ClanCard.MAX_STRENGTH; s++) {
                cards.add(new ClanCard(color, s));
            }
        }
        return cards;
    }

    /**
     * Creates the 10 official Tactic cards.
     *
     * @return new list of Tactic cards
     */
    public static List<TacticCard> tacticCards() {
        List<TacticCard> cards = new ArrayList<>();
        for (TacticType type : TacticType.values()) {
            for (int i = 1; i <= type.copies(); i++) {
                String suffix = type.copies() > 1 ? "-" + i : "";
                cards.add(new TacticCard("T-" + type.name() + suffix, type));
            }
        }
        return cards;
    }

    /**
     * Creates the expansion cards for the given designs.
     *
     * @param definitions expansion designs
     * @return new list of expansion cards
     */
    public static List<ExpansionCard> expansionCards(Collection<ExpansionDefinition> definitions) {
        List<ExpansionCard> cards = new ArrayList<>();
        for (ExpansionDefinition def : definitions) {
            for (int i = 1; i <= def.copies(); i++) {
                cards.add(new ExpansionCard("X-" + def.key() + "-" + i, def));
            }
        }
        return cards;
    }

    /**
     * Finds a card by identifier.
     *
     * @param id card identifier
     * @return the card, if it exists
     */
    public Optional<Card> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Returns the card with the given identifier.
     *
     * @param id card identifier
     * @return the card
     * @throws IllegalArgumentException if the identifier is unknown
     */
    public Card get(String id) {
        Card c = byId.get(id);
        if (c == null) {
            throw new IllegalArgumentException("Unknown card id: " + id);
        }
        return c;
    }

    /**
     * Returns every card of the catalogue.
     *
     * @return unmodifiable collection of cards
     */
    public Collection<Card> all() {
        return byId.values();
    }
}
