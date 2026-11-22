package com.schottentotten.ai;

import com.schottentotten.model.Card;
import com.schottentotten.model.Deck;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PendingEffect;
import com.schottentotten.model.PlayerId;
import com.schottentotten.util.SeededRandom;

import java.util.ArrayList;
import java.util.List;

/**
 * What a virtual player is allowed to know.
 *
 * <p>The view holds a <em>determinised</em> copy of the game: every card the player cannot see
 * (the opponent's hand and the content of every deck) is reshuffled at random among those hidden
 * positions. The number of cards of each type in the opponent's hand is kept, because it
 * follows from public information (every draw announces its deck). The real hidden order
 * therefore never reaches the strategy, so a strategy cannot cheat even by accident. Cards the
 * player legitimately saw with Highland Scout stay in place while the scout effect is
 * pending.</p>
 */
public final class PlayerView {

    private final GameState state;
    private final PlayerId me;
    private final List<Card> unseen;

    private PlayerView(GameState state, PlayerId me, List<Card> unseen) {
        this.state = state;
        this.me = me;
        this.unseen = List.copyOf(unseen);
    }

    /**
     * Builds the view of a player.
     *
     * @param real the real state (not modified)
     * @param me   the viewing player
     * @param rng  random generator used to redistribute hidden cards
     * @return the view
     */
    public static PlayerView of(GameState real, PlayerId me, SeededRandom rng) {
        GameState copy = real.copy();
        PlayerId opp = me.opponent();
        List<Card> allUnseen = new ArrayList<>();
        List<Card> newOppHand = new ArrayList<>();
        for (DeckType type : DeckType.values()) {
            Deck deck = copy.deck(type).orElse(null);
            List<Card> fixedTop = knownTop(copy, me, type, deck);
            List<Card> pool = new ArrayList<>();
            List<Card> oppOfType = copy.hand(opp).stream().filter(c -> c.deckType() == type).toList();
            pool.addAll(oppOfType);
            if (deck != null) {
                pool.addAll(deck.cards().subList(fixedTop.size(), deck.size()));
            }
            allUnseen.addAll(pool);
            rng.shuffle(pool);
            newOppHand.addAll(pool.subList(0, oppOfType.size()));
            if (deck != null) {
                List<Card> content = new ArrayList<>(fixedTop);
                content.addAll(pool.subList(oppOfType.size(), pool.size()));
                copy.replaceDeck(new Deck(type, content));
            }
        }
        copy.replaceHand(opp, newOppHand);
        return new PlayerView(copy, me, allUnseen);
    }

    private static List<Card> knownTop(GameState state, PlayerId me, DeckType type, Deck deck) {
        PendingEffect pending = state.pending().orElse(null);
        if (deck == null || pending == null || pending.player() != me || pending.deck() != type) {
            return List.of();
        }
        return deck.peek(pending.cardIds().size());
    }

    /**
     * Returns the determinised state. It can be copied and simulated freely.
     *
     * @return state containing no hidden information
     */
    public GameState state() {
        return state;
    }

    /**
     * Returns the viewing player.
     *
     * @return player
     */
    public PlayerId me() {
        return me;
    }

    /**
     * Returns the viewing player's hand.
     *
     * @return hand
     */
    public List<Card> myHand() {
        return state.hand(me);
    }

    /**
     * Returns every card hidden from the player (opponent's hand and all deck contents).
     *
     * @return unseen cards
     */
    public List<Card> unseenCards() {
        return unseen;
    }
}
