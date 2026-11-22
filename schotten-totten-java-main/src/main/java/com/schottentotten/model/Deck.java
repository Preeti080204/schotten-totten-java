package com.schottentotten.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An ordered draw pile. Index 0 is the top of the deck.
 *
 * <p>The internal list is never exposed; callers get copies or single cards.</p>
 */
public final class Deck {

    private final DeckType type;
    private final List<Card> cards;

    /**
     * Creates a deck with the given content (top first).
     *
     * @param type  deck type
     * @param cards initial cards, top first
     */
    public Deck(DeckType type, List<? extends Card> cards) {
        this.type = type;
        this.cards = new ArrayList<>(cards);
    }

    /**
     * Returns the type of this deck.
     *
     * @return deck type
     */
    public DeckType type() {
        return type;
    }

    /**
     * Returns the number of remaining cards.
     *
     * @return deck size
     */
    public int size() {
        return cards.size();
    }

    /**
     * Tells whether the deck is empty.
     *
     * @return {@code true} if no card remains
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Removes and returns the top card.
     *
     * @return the top card, or empty if the deck is empty
     */
    public Optional<Card> draw() {
        return cards.isEmpty() ? Optional.empty() : Optional.of(cards.remove(0));
    }

    /**
     * Returns (without removing) up to {@code n} cards from the top.
     *
     * @param n maximum number of cards
     * @return copy of the top cards, top first
     */
    public List<Card> peek(int n) {
        return List.copyOf(cards.subList(0, Math.min(n, cards.size())));
    }

    /**
     * Replaces the top cards of the deck with the same cards in a new order.
     *
     * @param newTop the reordered top cards, top first
     * @throws IllegalArgumentException if {@code newTop} is not a permutation of the current top
     */
    public void reorderTop(List<Card> newTop) {
        List<Card> current = new ArrayList<>(cards.subList(0, Math.min(newTop.size(), cards.size())));
        if (current.size() != newTop.size() || !current.containsAll(newTop) || !newTop.containsAll(current)) {
            throw new IllegalArgumentException("New order must be a permutation of the top cards");
        }
        for (int i = 0; i < newTop.size(); i++) {
            cards.set(i, newTop.get(i));
        }
    }

    /**
     * Puts a card at the bottom of the deck.
     *
     * @param card card to insert
     */
    public void putBottom(Card card) {
        cards.add(card);
    }

    /**
     * Returns a snapshot of the content, top first.
     *
     * @return unmodifiable copy of the cards
     */
    public List<Card> cards() {
        return List.copyOf(cards);
    }

    /**
     * Creates an independent copy.
     *
     * @return copy of this deck
     */
    public Deck copy() {
        return new Deck(type, cards);
    }
}
