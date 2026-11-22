package com.schottentotten.ai;

import com.schottentotten.engine.GameEngine;
import com.schottentotten.engine.IllegalActionException;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.EndTurnAction;
import com.schottentotten.engine.action.PassAction;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.engine.action.ResolveEffectAction;
import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.ExpansionCard;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PendingEffect;
import com.schottentotten.model.TacticCard;
import com.schottentotten.model.TacticType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "Hard" virtual player: one-ply look-ahead with a positional evaluation.
 *
 * <p>Every legal play is simulated with the real engine on the player's determinised view, and
 * the resulting position is scored by {@link BoardEvaluator}. The score combines formation
 * strength comparisons, adjacency windows (to build or block three adjacent Stones), the
 * five-Stone race and immediate claim opportunities. Special cards carry an opportunity cost, so
 * they are only used when they clearly improve the position. Ties are broken with the seeded
 * generator, so games are reproducible.</p>
 */
public final class AdvancedStrategy extends AbstractStrategy {

    private static final int MAX_SIMULATIONS = 1500;

    /**
     * Creates the strategy.
     *
     * @param engine rules engine
     * @param seed   random seed
     */
    public AdvancedStrategy(GameEngine engine, long seed) {
        super(engine, seed);
    }

    @Override
    public String name() {
        return "Hard";
    }

    @Override
    protected Action choosePlay(PlayerView view, List<Action> legal) {
        GameState state = view.state();
        BoardEvaluator evaluator = new BoardEvaluator(engine, view);
        double baseline = evaluator.evaluate(state);
        List<Action> candidates = legal;
        if (candidates.size() > MAX_SIMULATIONS) {
            candidates = new ArrayList<>(legal);
            rng.shuffle(candidates);
            candidates = candidates.subList(0, MAX_SIMULATIONS);
        }
        Action best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Action action : candidates) {
            double score;
            if (action instanceof PassAction) {
                score = baseline - 0.05;
            } else {
                PlayCardAction play = (PlayCardAction) action;
                Card card = state.findInHand(view.me(), play.cardId()).orElseThrow();
                GameState next = state.copy();
                try {
                    engine.apply(next, view.me(), action);
                } catch (IllegalActionException e) {
                    continue;
                }
                score = evaluator.evaluate(next) - cost(card) + bonus(view, card);
            }
            score += rng.nextDouble() * 0.01;
            if (score > bestScore) {
                bestScore = score;
                best = action;
            }
        }
        return best != null ? best : legal.get(0);
    }

    @Override
    protected Action chooseResolution(PlayerView view, List<ResolveEffectAction> options) {
        FormationHeuristics heuristics = heuristics(view);
        PendingEffect pending = view.state().pending().orElseThrow();
        Map<String, Double> useful = new HashMap<>();
        if (pending.deck() != null) {
            for (String id : pending.cardIds()) {
                Card c = view.state().deck(pending.deck()).orElseThrow().peek(pending.cardIds().size()).stream()
                        .filter(x -> x.id().equals(id)).findFirst().orElseThrow();
                useful.put(id, usefulness(view, heuristics, c));
            }
            List<String> sorted = new ArrayList<>(pending.cardIds());
            sorted.sort(Comparator.comparingDouble((String id) -> useful.get(id)).reversed());
            List<String> wanted = new ArrayList<>();
            wanted.add(sorted.get(0));
            if (sorted.size() == 3) {
                wanted.add(sorted.get(2));
                wanted.add(sorted.get(1));
            } else if (sorted.size() == 2) {
                wanted.add(sorted.get(1));
            }
            return options.stream().filter(o -> o.parameters().cardIds().equals(wanted)).findFirst()
                    .orElse(options.get(0));
        }
        for (Card c : view.myHand()) {
            useful.put(c.id(), usefulness(view, heuristics, c));
        }
        return options.stream().min(Comparator.comparingDouble(o -> o.parameters().cardIds().stream()
                .mapToDouble(useful::get).sum())).orElseThrow();
    }

    @Override
    protected Action chooseDraw(PlayerView view, List<EndTurnAction> options) {
        long specials = view.myHand().stream().filter(Card::isSpecial).count();
        long clans = view.myHand().stream().filter(c -> c instanceof ClanCard).count();
        boolean tactic = options.stream().anyMatch(o -> o.drawFrom() == DeckType.TACTIC);
        boolean expansion = options.stream().anyMatch(o -> o.drawFrom() == DeckType.EXPANSION);
        boolean clan = options.stream().anyMatch(o -> o.drawFrom() == DeckType.CLAN);
        DeckType choice;
        if (tactic && (specials == 0 && clans >= 4 || !clan) && rng.nextDouble() < 0.65) {
            choice = DeckType.TACTIC;
        } else if (expansion && (specials == 0 && clans >= 5 || !clan) && rng.nextDouble() < 0.35) {
            choice = DeckType.EXPANSION;
        } else {
            choice = clan ? DeckType.CLAN : options.get(0).drawFrom();
        }
        DeckType wanted = choice;
        return options.stream().filter(o -> o.drawFrom() == wanted).findFirst().orElse(options.get(0));
    }

    private FormationHeuristics heuristics(PlayerView view) {
        List<Card> obtainable = new ArrayList<>(view.unseenCards());
        obtainable.addAll(view.myHand());
        return new FormationHeuristics(obtainable);
    }

    private static double cost(Card card) {
        if (card instanceof TacticCard t) {
            return switch (t.type()) {
                case JOKER -> 0.35;
                case SPY -> 0.2;
                case SHIELD_BEARER -> 0.15;
                case BLIND_MANS_BLUFF, MUD_FIGHT -> 0.3;
                case RECRUITER, STRATEGIST, BANSHEE, TRAITOR -> 0.35;
            };
        }
        return card instanceof ExpansionCard ? 0.25 : 0.0;
    }

    private static double bonus(PlayerView view, Card card) {
        if (card instanceof TacticCard t && t.type() == TacticType.RECRUITER) {
            long strong = view.myHand().stream()
                    .filter(c -> c instanceof ClanCard clan && clan.strength() >= 6).count();
            return strong <= 2 ? 0.5 : 0.05;
        }
        if (card instanceof ExpansionCard e && e.definition().key().equals("SCOUT")) {
            return 0.12;
        }
        return 0.0;
    }
}
