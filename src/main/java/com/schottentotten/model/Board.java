package com.schottentotten.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The border: nine Stones in a row.
 */
public final class Board {

    /** Number of Stones on the border. */
    public static final int STONE_COUNT = 9;

    private final List<StoneState> stones;

    /**
     * Creates a board with nine empty Stones.
     */
    public Board() {
        List<StoneState> list = new ArrayList<>();
        for (int i = 0; i < STONE_COUNT; i++) {
            list.add(new StoneState(i));
        }
        this.stones = List.copyOf(list);
    }

    /**
     * Creates a board from existing Stones (used by copies and loading).
     *
     * @param stones exactly nine Stones in order
     */
    public Board(List<StoneState> stones) {
        if (stones.size() != STONE_COUNT) {
            throw new IllegalArgumentException("A board needs exactly 9 stones");
        }
        this.stones = List.copyOf(stones);
    }

    /**
     * Returns the Stone at an index.
     *
     * @param index position from 0 to 8
     * @return the Stone
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public StoneState stone(int index) {
        return stones.get(index);
    }

    /**
     * Returns all Stones in order.
     *
     * @return unmodifiable list of Stones
     */
    public List<StoneState> stones() {
        return stones;
    }

    /**
     * Counts the Stones controlled by a player.
     *
     * @param player the player
     * @return number of claimed Stones
     */
    public int stonesOwnedBy(PlayerId player) {
        return (int) stones.stream().filter(s -> s.owner().orElse(null) == player).count();
    }

    /**
     * Finds the Stone and side holding a card.
     *
     * @param cardId card identifier
     * @return the Stone holding the card in a formation, if any
     */
    public Optional<StoneState> stoneHolding(String cardId) {
        return stones.stream()
                .filter(s -> s.side(PlayerId.P1).contains(cardId) || s.side(PlayerId.P2).contains(cardId))
                .findFirst();
    }

    /**
     * Returns every card visible on the board (formations, combat modes and wards).
     *
     * @return list of public cards on the board
     */
    public List<Card> allPublicCards() {
        List<Card> result = new ArrayList<>();
        for (StoneState s : stones) {
            for (PlayerId p : PlayerId.values()) {
                result.addAll(s.side(p).cards());
                s.side(p).ward().ifPresent(w -> result.add(w.card()));
            }
            result.addAll(s.combatModes());
        }
        return result;
    }

    /**
     * Creates an independent copy.
     *
     * @return deep copy
     */
    public Board copy() {
        return new Board(stones.stream().map(StoneState::copy).toList());
    }
}
