package com.schottentotten.ai;

import com.schottentotten.engine.GameEngine;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.ClaimAction;
import com.schottentotten.engine.action.EndTurnAction;
import com.schottentotten.engine.action.ResolveEffectAction;
import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.GameState;
import com.schottentotten.model.StoneState;
import com.schottentotten.util.SeededRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Common skeleton of the virtual players (Template Method): legal actions are always computed by
 * the engine from the player's {@link PlayerView}; subclasses only rank them. Claims are always
 * taken when legal, because claiming can never hurt.
 */
abstract class AbstractStrategy implements PlayerStrategy {

    /** Rules engine used to list and simulate legal actions. */
    protected final GameEngine engine;
    /** Seeded generator for reproducible tie-breaking. */
    protected final SeededRandom rng;

    /**
     * Creates the strategy.
     *
     * @param engine rules engine
     * @param seed   random seed
     */
    protected AbstractStrategy(GameEngine engine, long seed) {
        this.engine = Objects.requireNonNull(engine);
        this.rng = new SeededRandom(seed);
    }

    @Override
    public final Action chooseAction(PlayerView view) {
        GameState state = view.state();
        List<Action> legal = engine.legalActions(state);
        if (legal.isEmpty()) {
            throw new IllegalStateException("No legal action available in phase " + state.phase());
        }
        if (legal.size() == 1) {
            return legal.get(0);
        }
        return switch (state.phase()) {
            case PLAY -> choosePlay(view, legal);
            case RESOLVE_EFFECT -> chooseResolution(view, legal.stream()
                    .map(a -> (ResolveEffectAction) a).toList());
            case CLAIM -> legal.stream().filter(a -> a instanceof ClaimAction).findFirst()
                    .orElseGet(() -> chooseDraw(view, legal.stream().map(a -> (EndTurnAction) a).toList()));
            default -> legal.get(0);
        };
    }

    /**
     * Chooses the card to play (or pass) among legal actions.
     *
     * @param view  player view
     * @param legal legal actions of the PLAY phase
     * @return chosen action
     */
    protected abstract Action choosePlay(PlayerView view, List<Action> legal);

    /**
     * Chooses how to resolve a pending effect.
     *
     * @param view    player view
     * @param options legal resolutions
     * @return chosen resolution
     */
    protected abstract Action chooseResolution(PlayerView view, List<ResolveEffectAction> options);

    /**
     * Chooses the deck to draw from.
     *
     * @param view    player view
     * @param options legal end-of-turn actions
     * @return chosen action
     */
    protected abstract Action chooseDraw(PlayerView view, List<EndTurnAction> options);

    /**
     * Estimates how much a card in hand helps the player: for formation cards, the best
     * improvement it brings to one of the player's Stones; for special cards, a fixed value.
     *
     * @param view       player view
     * @param heuristics formation heuristics
     * @param card       the card
     * @return usefulness score
     */
    protected double usefulness(PlayerView view, FormationHeuristics heuristics, Card card) {
        if (!card.isFormationCard()) {
            return 6.0;
        }
        double best = card instanceof ClanCard clan ? clan.strength() * 0.3 : 9.0;
        for (StoneState stone : view.state().board().stones()) {
            if (!stone.hasRoom(view.me())) {
                continue;
            }
            List<Card> before = stone.side(view.me()).cards();
            List<Card> after = new ArrayList<>(before);
            after.add(card);
            double gain = heuristics.estimate(after, stone.requiredSize(), stone.isSumOnly(), List.of())
                    - heuristics.estimate(before, stone.requiredSize(), stone.isSumOnly(), List.of());
            best = Math.max(best, gain);
        }
        return best;
    }
}
