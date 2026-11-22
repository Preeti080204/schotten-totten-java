package com.schottentotten.ai;

import com.schottentotten.engine.GameEngine;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.EndTurnAction;
import com.schottentotten.engine.action.PassAction;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.engine.action.ResolveEffectAction;
import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.GameState;
import com.schottentotten.model.StoneState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "Easy" virtual player: simple but coherent.
 *
 * <ul>
 *   <li>Places the formation card that most improves one of its formations (greedy, one Stone at
 *       a time), preferring Clan cards over Elite Troops.</li>
 *   <li>Plays other special cards only when it cannot place a formation card.</li>
 *   <li>Always claims every Stone it can claim, and draws Clan cards first.</li>
 * </ul>
 */
public final class SimpleStrategy extends AbstractStrategy {

    /**
     * Creates the strategy.
     *
     * @param engine rules engine
     * @param seed   random seed
     */
    public SimpleStrategy(GameEngine engine, long seed) {
        super(engine, seed);
    }

    @Override
    public String name() {
        return "Easy";
    }

    @Override
    protected Action choosePlay(PlayerView view, List<Action> legal) {
        GameState state = view.state();
        List<Card> obtainable = new ArrayList<>(view.unseenCards());
        obtainable.addAll(view.myHand());
        FormationHeuristics heuristics = new FormationHeuristics(obtainable);
        Action best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        List<Action> others = new ArrayList<>();
        for (Action a : legal) {
            if (!(a instanceof PlayCardAction play)) {
                continue;
            }
            Card card = state.findInHand(view.me(), play.cardId()).orElseThrow();
            if (!card.isFormationCard()) {
                others.add(a);
                continue;
            }
            StoneState stone = state.board().stone(play.parameters().stone());
            List<Card> before = stone.side(view.me()).cards();
            List<Card> after = new ArrayList<>(before);
            after.add(card);
            double score = heuristics.estimate(after, stone.requiredSize(), stone.isSumOnly(), List.of())
                    - heuristics.estimate(before, stone.requiredSize(), stone.isSumOnly(), List.of())
                    + rng.nextDouble() * 2.0
                    - (card instanceof ClanCard ? 0 : 6.0);
            if (score > bestScore) {
                bestScore = score;
                best = a;
            }
        }
        if (best != null) {
            return best;
        }
        boolean canPass = legal.stream().anyMatch(a -> a instanceof PassAction);
        if (others.isEmpty() || (canPass && rng.nextDouble() < 0.3)) {
            return legal.stream().filter(a -> a instanceof PassAction).findFirst().orElse(legal.get(0));
        }
        return others.get(rng.nextInt(others.size()));
    }

    @Override
    protected Action chooseResolution(PlayerView view, List<ResolveEffectAction> options) {
        if (view.state().pending().map(p -> p.deck() != null).orElse(false)) {
            return options.get(0);
        }
        return options.stream().min(Comparator.comparingInt(o -> o.parameters().cardIds().stream()
                .mapToInt(id -> strengthOf(view.state().findInHand(view.me(), id).orElseThrow())).sum()))
                .orElseThrow();
    }

    @Override
    protected Action chooseDraw(PlayerView view, List<EndTurnAction> options) {
        return options.stream().filter(o -> o.drawFrom() == DeckType.CLAN).findFirst().orElse(options.get(0));
    }

    private static int strengthOf(Card card) {
        return card instanceof ClanCard clan ? clan.strength() : 10;
    }
}
