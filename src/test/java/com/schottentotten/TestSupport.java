package com.schottentotten;

import com.schottentotten.cards.StandardRules;
import com.schottentotten.engine.GameEngine;
import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.ClanColor;
import com.schottentotten.model.Deck;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.Difficulty;
import com.schottentotten.model.FirstPlayerRule;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.persistence.SaveManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helpers to build precise game positions in tests while keeping every card exactly once.
 */
public final class TestSupport {

    /** Shared engine. */
    public static final GameEngine ENGINE = StandardRules.createEngine();
    /** Shared save manager (used to compare states). */
    public static final SaveManager SAVES = new SaveManager(ENGINE.catalog());

    private TestSupport() {
    }

    /**
     * Human vs human configuration.
     *
     * @param tactic     Tactic variant
     * @param expansions expansions
     * @return configuration
     */
    public static GameConfiguration config(boolean tactic, boolean expansions) {
        return new GameConfiguration(PlayerProfile.human("Alice"), PlayerProfile.human("Bob"), tactic, expansions, 1,
                FirstPlayerRule.PLAYER_ONE, 42L);
    }

    /**
     * Virtual vs virtual configuration.
     *
     * @param tactic     Tactic variant
     * @param expansions expansions
     * @param d1         difficulty of Player 1
     * @param d2         difficulty of Player 2
     * @param rounds     rounds
     * @param seed       seed
     * @return configuration
     */
    public static GameConfiguration aiConfig(boolean tactic, boolean expansions, Difficulty d1, Difficulty d2,
                                             int rounds, long seed) {
        return new GameConfiguration(PlayerProfile.virtual("Bot A", d1), PlayerProfile.virtual("Bot B", d2), tactic,
                expansions, rounds, FirstPlayerRule.PLAYER_ONE, seed);
    }

    /**
     * Creates a new game with Alice (P1) to play.
     *
     * @param tactic     Tactic variant
     * @param expansions expansions
     * @return state
     */
    public static GameState newGame(boolean tactic, boolean expansions) {
        return ENGINE.newGame(config(tactic, expansions));
    }

    /**
     * Parses a Clan card such as {@code "R5"}.
     *
     * @param code colour letter and strength
     * @return the card
     */
    public static ClanCard clan(String code) {
        return new ClanCard(ClanColor.parse(code.substring(0, 1)), Integer.parseInt(code.substring(1)));
    }

    /**
     * Returns a card by identifier.
     *
     * @param id card id
     * @return card
     */
    public static Card card(String id) {
        return ENGINE.catalog().get(id);
    }

    /**
     * Removes a card from wherever it is in hands or decks.
     *
     * @param state the state
     * @param card  the card
     */
    public static void detach(GameState state, Card card) {
        for (PlayerId p : PlayerId.values()) {
            if (state.findInHand(p, card.id()).isPresent()) {
                state.removeFromHand(p, card.id());
                return;
            }
        }
        for (DeckType t : DeckType.values()) {
            state.deck(t).ifPresent(d -> {
                if (d.cards().contains(card)) {
                    List<Card> rest = new ArrayList<>(d.cards());
                    rest.remove(card);
                    state.replaceDeck(new Deck(t, rest));
                }
            });
        }
    }

    /**
     * Places cards on a side of a Stone (taken from hands or decks).
     *
     * @param state  state
     * @param player side owner
     * @param stone  Stone index (0-based)
     * @param cards  cards to place in order
     */
    public static void place(GameState state, PlayerId player, int stone, Card... cards) {
        for (Card c : cards) {
            detach(state, c);
            state.board().stone(stone).addCard(player, c, state.nextSequence());
        }
    }

    /**
     * Replaces a player's hand; the previous hand goes to the bottom of its decks.
     *
     * @param state  state
     * @param player player
     * @param cards  new hand
     */
    public static void hand(GameState state, PlayerId player, Card... cards) {
        List<Card> old = state.hand(player);
        state.replaceHand(player, List.of());
        for (Card c : cards) {
            detach(state, c);
        }
        for (Card c : old) {
            if (!Arrays.asList(cards).contains(c)) {
                state.deck(c.deckType()).orElseThrow().putBottom(c);
            }
        }
        state.replaceHand(player, Arrays.asList(cards));
    }

    /**
     * Returns the canonical JSON of a state (to compare states).
     *
     * @param state state
     * @return JSON without the timestamp
     */
    public static String json(GameState state) {
        return SAVES.toJson(state).replaceAll("\"savedAt\" : \"[^\"]*\"", "");
    }
}
