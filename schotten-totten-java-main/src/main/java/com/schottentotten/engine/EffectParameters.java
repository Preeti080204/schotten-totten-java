package com.schottentotten.engine;

import com.schottentotten.model.DeckType;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Generic, immutable parameters of a card effect or effect resolution. Each effect reads only
 * the fields it needs. Using one value type for all effects lets the views drive target
 * selection generically from the list of legal parameter sets, so new cards need no view code.
 *
 * @param stone        target Stone index (0-8), or {@code null}
 * @param targetCardId card targeted on the board, or {@code null}
 * @param destination  destination Stone index, {@link #DISCARD} to discard, or {@code null}
 * @param secondCardId second card (swap effects), or {@code null}
 * @param decks        deck sources for multi-draw effects (Recruiter), canonical order
 * @param deck         a single chosen deck, or {@code null}
 * @param cardIds      chosen cards (Recruiter return set or Scout order)
 */
public record EffectParameters(Integer stone, String targetCardId, Integer destination, String secondCardId,
                               List<DeckType> decks, DeckType deck, List<String> cardIds) {

    /** Destination value meaning "discard the card". */
    public static final int DISCARD = -1;

    /** Parameters for an effect that needs no choice. */
    public static final EffectParameters NONE = new EffectParameters(null, null, null, null, null, null, null);

    /**
     * Copies the lists and normalises {@code null} lists to empty lists.
     *
     * @param stone        target Stone
     * @param targetCardId target card
     * @param destination  destination
     * @param secondCardId second card
     * @param decks        deck sources
     * @param deck         chosen deck
     * @param cardIds      chosen cards
     */
    public EffectParameters {
        decks = decks == null ? List.of() : List.copyOf(decks);
        cardIds = cardIds == null ? List.of() : List.copyOf(cardIds);
    }

    /**
     * Parameters targeting a Stone.
     *
     * @param stone Stone index
     * @return parameters
     */
    public static EffectParameters onStone(int stone) {
        return new EffectParameters(stone, null, null, null, null, null, null);
    }

    /**
     * Parameters targeting a card on the board.
     *
     * @param cardId card identifier
     * @return parameters
     */
    public static EffectParameters target(String cardId) {
        return new EffectParameters(null, cardId, null, null, null, null, null);
    }

    /**
     * Parameters moving a card to a destination Stone or to the discard.
     *
     * @param cardId      card identifier
     * @param destination Stone index or {@link #DISCARD}
     * @return parameters
     */
    public static EffectParameters move(String cardId, int destination) {
        return new EffectParameters(null, cardId, destination, null, null, null, null);
    }

    /**
     * Parameters swapping two cards.
     *
     * @param first  first card
     * @param second second card
     * @return parameters
     */
    public static EffectParameters swap(String first, String second) {
        return new EffectParameters(null, first, null, second, null, null, null);
    }

    /**
     * Parameters giving deck sources, sorted canonically.
     *
     * @param decks deck sources
     * @return parameters
     */
    public static EffectParameters draws(List<DeckType> decks) {
        List<DeckType> sorted = new ArrayList<>(decks);
        sorted.sort(null);
        return new EffectParameters(null, null, null, null, sorted, null, null);
    }

    /**
     * Parameters choosing one deck.
     *
     * @param deck deck type
     * @return parameters
     */
    public static EffectParameters ofDeck(DeckType deck) {
        return new EffectParameters(null, null, null, null, null, deck, null);
    }

    /**
     * Parameters listing chosen cards (order is meaningful for Highland Scout).
     *
     * @param cardIds card identifiers
     * @return parameters
     */
    public static EffectParameters cards(List<String> cardIds) {
        return new EffectParameters(null, null, null, null, null, null, cardIds);
    }

    /**
     * Returns a compact textual form, for logs and debugging.
     *
     * @return description
     */
    public String describe() {
        StringJoiner j = new StringJoiner(", ", "{", "}");
        if (stone != null) {
            j.add("stone=" + (stone + 1));
        }
        if (targetCardId != null) {
            j.add("card=" + targetCardId);
        }
        if (destination != null) {
            j.add("to=" + (destination == DISCARD ? "discard" : String.valueOf(destination + 1)));
        }
        if (secondCardId != null) {
            j.add("with=" + secondCardId);
        }
        if (!decks.isEmpty()) {
            j.add("decks=" + decks);
        }
        if (deck != null) {
            j.add("deck=" + deck);
        }
        if (!cardIds.isEmpty()) {
            j.add("cards=" + cardIds);
        }
        return j.toString();
    }
}
