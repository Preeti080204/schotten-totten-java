package com.schottentotten.rules;

import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration-dependent rules: hand size, decks in play, special-card restriction and passing.
 *
 * <p>This is an immutable value derived from a {@link GameConfiguration}. All variant-dependent
 * decisions are made here, so the engine has no scattered {@code if (tacticVariant)} checks.</p>
 */
public final class RuleSet {

    /** Hand size of the base game. */
    public static final int BASE_HAND_SIZE = 6;
    /** Hand size of the Tactic variant. */
    public static final int TACTIC_HAND_SIZE = 7;

    private final GameConfiguration config;

    /**
     * Creates the rules for a configuration.
     *
     * @param config session configuration
     */
    public RuleSet(GameConfiguration config) {
        this.config = config;
    }

    /**
     * Returns the starting and maximum hand size.
     *
     * @return 6 (base) or 7 (Tactic variant)
     */
    public int handSize() {
        return config.tacticVariant() ? TACTIC_HAND_SIZE : BASE_HAND_SIZE;
    }

    /**
     * Returns the decks used by the configuration.
     *
     * @return deck types in play
     */
    public List<DeckType> decks() {
        List<DeckType> decks = new ArrayList<>(List.of(DeckType.CLAN));
        if (config.tacticVariant()) {
            decks.add(DeckType.TACTIC);
        }
        if (config.expansions()) {
            decks.add(DeckType.EXPANSION);
        }
        return decks;
    }

    /**
     * Official restriction: a player may play a special card only if they have not played more
     * special cards than their opponent. Tactic and expansion cards share the counter.
     *
     * @param state  game state
     * @param player the player
     * @return {@code true} if a special card may be played now
     */
    public boolean maySpecialBePlayed(GameState state, PlayerId player) {
        return state.specialPlayed(player) <= state.specialPlayed(player.opponent());
    }

    /**
     * Passing is allowed only when no Clan card of the hand can be placed on any Stone.
     *
     * @param state  game state
     * @param player the player
     * @return {@code true} if the player may pass
     */
    public boolean mayPass(GameState state, PlayerId player) {
        boolean hasClan = state.hand(player).stream().anyMatch(c -> c instanceof ClanCard);
        boolean hasRoom = state.board().stones().stream().anyMatch(s -> s.hasRoom(player));
        return !(hasClan && hasRoom);
    }

    /**
     * Tells whether a card is restricted by the special-card counter.
     *
     * @param card the card
     * @return {@code true} for Tactic and expansion cards
     */
    public boolean isRestricted(Card card) {
        return card.isSpecial();
    }
}
