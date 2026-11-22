package com.schottentotten.engine;

import com.schottentotten.model.Card;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.StoneState;
import com.schottentotten.rules.RuleSet;

import java.util.List;
import java.util.Objects;

/**
 * Everything a {@link CardEffect} may use while validating or applying: the state, the acting
 * player, the rules of the session and a sink for public events.
 */
public final class EffectContext {

    private final GameState state;
    private final PlayerId actor;
    private final RuleSet rules;
    private final List<GameEvent> events;

    /**
     * Creates a context.
     *
     * @param state  game state
     * @param actor  acting player
     * @param rules  session rules
     * @param events event sink (may be a throw-away list when only validating)
     */
    public EffectContext(GameState state, PlayerId actor, RuleSet rules, List<GameEvent> events) {
        this.state = Objects.requireNonNull(state);
        this.actor = Objects.requireNonNull(actor);
        this.rules = Objects.requireNonNull(rules);
        this.events = Objects.requireNonNull(events);
    }

    /**
     * Returns the game state.
     *
     * @return state
     */
    public GameState state() {
        return state;
    }

    /**
     * Returns the acting player.
     *
     * @return actor
     */
    public PlayerId actor() {
        return actor;
    }

    /**
     * Returns the opponent of the acting player.
     *
     * @return opponent
     */
    public PlayerId opponent() {
        return actor.opponent();
    }

    /**
     * Returns the session rules.
     *
     * @return rules
     */
    public RuleSet rules() {
        return rules;
    }

    /**
     * Returns the event sink.
     *
     * @return mutable list of events produced by the current action
     */
    public List<GameEvent> events() {
        return events;
    }

    /**
     * Returns the display name of a player.
     *
     * @param player the player
     * @return name
     */
    public String name(PlayerId player) {
        return state.config().profile(player).name();
    }

    /**
     * Publishes an event and records it in the public log.
     *
     * @param type    event kind
     * @param message public description
     * @param stone   Stone index or {@code -1}
     * @param cardId  card identifier or {@code null}
     */
    public void emit(GameEventType type, String message, int stone, String cardId) {
        events.add(new GameEvent(type, actor, message, stone, cardId));
        state.log(actor, message);
    }

    /**
     * Puts a card face-up on the discard pile.
     *
     * @param card card to discard
     */
    public void discard(Card card) {
        state.addToDiscard(card);
    }

    /**
     * Tells whether a side is protected against opponent effects (Stone Ward).
     *
     * @param stone the Stone
     * @param owner side owner
     * @return {@code true} if the side holds an active ward
     */
    public boolean isProtected(StoneState stone, PlayerId owner) {
        return stone.side(owner).ward().isPresent();
    }

    /**
     * Returns the Stone at an index after validating it.
     *
     * @param index Stone index, may be {@code null}
     * @return the Stone
     * @throws IllegalActionException if the index is missing or out of range
     */
    public StoneState requireStone(Integer index) throws IllegalActionException {
        if (index == null || index < 0 || index >= state.board().stones().size()) {
            throw new IllegalActionException("Choose a Stone between 1 and " + state.board().stones().size() + ".");
        }
        return state.board().stone(index);
    }

    /**
     * Finds an unclaimed Stone holding the given card on a given side.
     *
     * @param cardId card identifier, may be {@code null}
     * @param owner  side owner
     * @return the Stone
     * @throws IllegalActionException if the card is not on that side of an unclaimed Stone
     */
    public StoneState requireCardOnUnclaimedStone(String cardId, PlayerId owner) throws IllegalActionException {
        if (cardId == null) {
            throw new IllegalActionException("Choose a card on the board.");
        }
        for (StoneState s : state.board().stones()) {
            if (s.side(owner).contains(cardId)) {
                if (s.isClaimed()) {
                    throw new IllegalActionException("Cards on a claimed Stone cannot be targeted.");
                }
                return s;
            }
        }
        throw new IllegalActionException("That card is not on " + name(owner) + "'s side of the border.");
    }

    /**
     * Returns the card with the given id on a Stone side.
     *
     * @param stone  the Stone
     * @param owner  side owner
     * @param cardId card identifier
     * @return the card
     */
    public Card cardOn(StoneState stone, PlayerId owner, String cardId) {
        return stone.side(owner).cards().stream().filter(c -> c.id().equals(cardId)).findFirst()
                .orElseThrow();
    }
}
