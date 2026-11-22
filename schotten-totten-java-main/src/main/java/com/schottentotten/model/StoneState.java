package com.schottentotten.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One of the nine border Stones together with both formations, any Combat Mode cards played on
 * the tile and its owner once claimed.
 *
 * <p>All mutations go through methods that keep the completion invariant of {@link StoneSide}
 * consistent with the current required formation size.</p>
 */
public final class StoneState {

    /** Formation size of a normal Stone. */
    public static final int NORMAL_SIZE = 3;
    /** Formation size on a Mud Fight Stone. */
    public static final int MUD_SIZE = 4;

    private final int index;
    private final Map<PlayerId, StoneSide> sides = new EnumMap<>(PlayerId.class);
    private final List<TacticCard> combatModes = new ArrayList<>();
    private PlayerId owner;

    /**
     * Creates an empty unclaimed Stone.
     *
     * @param index position on the border, 0 to 8
     */
    public StoneState(int index) {
        this.index = index;
        for (PlayerId p : PlayerId.values()) {
            sides.put(p, new StoneSide());
        }
    }

    /**
     * Restores a Stone from saved components.
     *
     * @param index       position on the border
     * @param p1          Player 1 side
     * @param p2          Player 2 side
     * @param combatModes combat modes on the tile
     * @param owner       owner or {@code null}
     */
    public StoneState(int index, StoneSide p1, StoneSide p2, List<TacticCard> combatModes, PlayerId owner) {
        this.index = index;
        sides.put(PlayerId.P1, p1);
        sides.put(PlayerId.P2, p2);
        this.combatModes.addAll(combatModes);
        this.owner = owner;
    }

    /**
     * Returns the position of this Stone (0-based).
     *
     * @return index from 0 to 8
     */
    public int index() {
        return index;
    }

    /**
     * Returns a player's side.
     *
     * @param player the player
     * @return that player's side (read-only view of cards)
     */
    public StoneSide side(PlayerId player) {
        return sides.get(player);
    }

    /**
     * Returns the combat modes played on the tile.
     *
     * @return unmodifiable copy
     */
    public List<TacticCard> combatModes() {
        return List.copyOf(combatModes);
    }

    /**
     * Tells whether a given combat mode is active on this Stone.
     *
     * @param type the combat mode type
     * @return {@code true} if present
     */
    public boolean hasCombatMode(TacticType type) {
        return combatModes.stream().anyMatch(c -> c.type() == type);
    }

    /**
     * Tells whether this Stone is decided by sum only (Blind-Man's Bluff).
     *
     * @return {@code true} if Blind-Man's Bluff is on the tile
     */
    public boolean isSumOnly() {
        return hasCombatMode(TacticType.BLIND_MANS_BLUFF);
    }

    /**
     * Returns the number of cards each formation needs (and the side capacity).
     *
     * @return 3, or 4 under Mud Fight
     */
    public int requiredSize() {
        return hasCombatMode(TacticType.MUD_FIGHT) ? MUD_SIZE : NORMAL_SIZE;
    }

    /**
     * Returns the owner of the Stone.
     *
     * @return the owner, or empty if unclaimed
     */
    public Optional<PlayerId> owner() {
        return Optional.ofNullable(owner);
    }

    /**
     * Tells whether the Stone has been claimed.
     *
     * @return {@code true} once claimed
     */
    public boolean isClaimed() {
        return owner != null;
    }

    /**
     * Tells whether a side has room for one more formation card.
     *
     * @param player side owner
     * @return {@code true} if the Stone is unclaimed and the side is not full
     */
    public boolean hasRoom(PlayerId player) {
        return !isClaimed() && sides.get(player).size() < requiredSize();
    }

    /**
     * Tells whether a player's formation is complete.
     *
     * @param player side owner
     * @return {@code true} if the side holds the required number of cards
     */
    public boolean isComplete(PlayerId player) {
        return sides.get(player).size() >= requiredSize();
    }

    /**
     * Places a formation card and records completion when the side becomes complete.
     *
     * @param player   side owner
     * @param card     the card
     * @param sequence current action sequence number
     */
    public void addCard(PlayerId player, Card card, long sequence) {
        StoneSide side = sides.get(player);
        side.add(card);
        if (side.size() == requiredSize() && side.completedAt() == StoneSide.NOT_COMPLETE) {
            side.setCompletedAt(sequence);
        }
    }

    /**
     * Removes a formation card, clearing the completion time if the side is no longer complete.
     *
     * @param player side owner
     * @param cardId card identifier
     * @return the removed card
     */
    public Card removeCard(PlayerId player, String cardId) {
        StoneSide side = sides.get(player);
        Card removed = side.remove(cardId);
        if (side.size() < requiredSize()) {
            side.setCompletedAt(StoneSide.NOT_COMPLETE);
        }
        return removed;
    }

    /**
     * Replaces a card in place without changing completion (used by Clan Exchange).
     *
     * @param player      side owner
     * @param cardId      card to replace
     * @param replacement new card
     */
    public void replaceCard(PlayerId player, String cardId, Card replacement) {
        sides.get(player).replace(cardId, replacement);
    }

    /**
     * Adds a combat mode and re-evaluates completion of both sides.
     *
     * @param card combat mode card
     */
    public void addCombatMode(TacticCard card) {
        combatModes.add(card);
        for (StoneSide side : sides.values()) {
            if (side.size() < requiredSize()) {
                side.setCompletedAt(StoneSide.NOT_COMPLETE);
            }
        }
    }

    /**
     * Sets or clears the ward of one side.
     *
     * @param player side owner
     * @param ward   ward or {@code null}
     */
    public void setWard(PlayerId player, ActiveWard ward) {
        sides.get(player).setWard(ward);
    }

    /**
     * Marks the Stone as claimed.
     *
     * @param player new owner
     */
    public void claim(PlayerId player) {
        if (owner != null) {
            throw new IllegalStateException("Stone already claimed");
        }
        owner = player;
    }

    /**
     * Creates an independent copy.
     *
     * @return deep copy of this Stone
     */
    public StoneState copy() {
        return new StoneState(index, sides.get(PlayerId.P1).copy(), sides.get(PlayerId.P2).copy(),
                combatModes, owner);
    }
}
