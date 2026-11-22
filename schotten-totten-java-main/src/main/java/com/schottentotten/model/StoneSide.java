package com.schottentotten.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One player's side of a Stone: the formation cards in play order, the moment the formation was
 * completed (for the tie-break rule) and an optional protecting ward.
 *
 * <p>Invariant: {@code completedAt >= 0} only while the side holds the number of cards required
 * by its Stone. The owning {@link StoneState} maintains this invariant.</p>
 */
public final class StoneSide {

    /** Marker for a side whose formation is not complete. */
    public static final long NOT_COMPLETE = -1L;

    private final List<Card> cards;
    private long completedAt;
    private ActiveWard ward;

    /**
     * Creates an empty side.
     */
    public StoneSide() {
        this(List.of(), NOT_COMPLETE, null);
    }

    /**
     * Creates a side with the given content (used by copies and save-file loading).
     *
     * @param cards       cards in play order
     * @param completedAt completion sequence number or {@link #NOT_COMPLETE}
     * @param ward        active ward, may be {@code null}
     */
    public StoneSide(List<Card> cards, long completedAt, ActiveWard ward) {
        this.cards = new ArrayList<>(cards);
        this.completedAt = completedAt;
        this.ward = ward;
    }

    /**
     * Returns the cards in play order.
     *
     * @return unmodifiable copy
     */
    public List<Card> cards() {
        return List.copyOf(cards);
    }

    /**
     * Returns the number of cards on this side.
     *
     * @return card count
     */
    public int size() {
        return cards.size();
    }

    /**
     * Tells whether the given card is on this side.
     *
     * @param cardId card identifier
     * @return {@code true} if present
     */
    public boolean contains(String cardId) {
        return cards.stream().anyMatch(c -> c.id().equals(cardId));
    }

    /**
     * Returns the sequence number at which this formation was completed.
     *
     * @return completion sequence or {@link #NOT_COMPLETE}
     */
    public long completedAt() {
        return completedAt;
    }

    /**
     * Returns the active ward, if any.
     *
     * @return ward
     */
    public Optional<ActiveWard> ward() {
        return Optional.ofNullable(ward);
    }

    void add(Card card) {
        cards.add(card);
    }

    Card remove(String cardId) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).id().equals(cardId)) {
                return cards.remove(i);
            }
        }
        throw new IllegalArgumentException("Card not on this side: " + cardId);
    }

    void replace(String cardId, Card replacement) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).id().equals(cardId)) {
                cards.set(i, replacement);
                return;
            }
        }
        throw new IllegalArgumentException("Card not on this side: " + cardId);
    }

    void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    void setWard(ActiveWard ward) {
        this.ward = ward;
    }

    StoneSide copy() {
        return new StoneSide(cards, completedAt, ward);
    }
}
