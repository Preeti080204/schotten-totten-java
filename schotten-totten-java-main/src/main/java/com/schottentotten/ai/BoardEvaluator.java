package com.schottentotten.ai;

import com.schottentotten.engine.GameEngine;
import com.schottentotten.model.Board;
import com.schottentotten.model.Card;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.RoundResult;
import com.schottentotten.model.StoneSide;
import com.schottentotten.model.StoneState;
import com.schottentotten.model.TacticCard;
import com.schottentotten.model.TacticType;
import com.schottentotten.model.TurnPhase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scores a position from one player's point of view (positive is good for that player).
 *
 * <p>Each unclaimed Stone contributes {@code tanh(difference of estimated formation values)},
 * so its contribution lies between -1 and 1. Claimed Stones contribute a fixed amount. Stones get
 * extra weight when they belong to a window of three adjacent Stones that one player is close
 * to completing, or when a player is one Stone away from five. This makes the AI pursue
 * adjacent triples and block the opponent's. A finished round scores plus or minus
 * {@value #WIN_SCORE}.</p>
 */
final class BoardEvaluator {

    /** Score of a won position. */
    static final double WIN_SCORE = 1000;

    private final GameEngine engine;
    private final PlayerId me;
    private final FormationHeuristics mine;
    private final FormationHeuristics theirs;
    private final boolean opponentMayHaveRemoval;
    private final Map<String, Boolean> claimCache = new HashMap<>();

    /**
     * Creates an evaluator for one decision.
     *
     * @param engine engine (claim checks)
     * @param view   the player's view
     */
    BoardEvaluator(GameEngine engine, PlayerView view) {
        this.engine = engine;
        this.me = view.me();
        List<Card> mineObtainable = new ArrayList<>(view.unseenCards());
        mineObtainable.addAll(view.myHand());
        this.mine = new FormationHeuristics(mineObtainable);
        this.theirs = new FormationHeuristics(view.unseenCards());
        this.opponentMayHaveRemoval = view.unseenCards().stream().anyMatch(c -> c instanceof TacticCard t
                && (t.type() == TacticType.BANSHEE || t.type() == TacticType.TRAITOR));
    }

    /**
     * Scores a state.
     *
     * @param state the state (typically after a simulated action)
     * @return score from the evaluator's player point of view
     */
    double evaluate(GameState state) {
        if (state.phase() == TurnPhase.ROUND_OVER || state.phase() == TurnPhase.MATCH_OVER) {
            RoundResult r = state.lastRoundResult().orElse(null);
            if (r != null && r.round() == state.round()) {
                return r.winner() == null ? 0 : (r.winner() == me ? WIN_SCORE : -WIN_SCORE);
            }
        }
        Board board = state.board();
        double[] weights = weights(board);
        List<Card> handFormation = state.hand(me).stream().filter(Card::isFormationCard).toList();
        double total = 0;
        for (StoneState stone : board.stones()) {
            int i = stone.index();
            if (stone.isClaimed()) {
                total += (stone.owner().get() == me ? 1.6 : -1.6) * weights[i];
                continue;
            }
            total += weights[i] * stoneValue(state, stone, handFormation);
        }
        return total;
    }

    private double stoneValue(GameState state, StoneState stone, List<Card> handFormation) {
        PlayerId opp = me.opponent();
        int size = stone.requiredSize();
        boolean sumOnly = stone.isSumOnly();
        if (stone.isComplete(me) && isClaimable(state, stone)) {
            return 1.5;
        }
        double a = mine.estimate(stone.side(me).cards(), size, sumOnly, handFormation);
        double b = theirs.estimate(stone.side(opp).cards(), size, sumOnly, List.of());
        double placed = stone.side(me).size() + stone.side(opp).size();
        double confidence = 0.6 + 0.4 * Math.min(1.0, placed / (2.0 * size));
        double v = Math.tanh((a - b) / 25.0) * confidence;
        if (stone.isComplete(me) && stone.isComplete(opp)) {
            v = Math.signum(v) * Math.max(0.6, Math.abs(v));
        }
        StoneSide side = stone.side(me);
        if (side.ward().isPresent() && opponentMayHaveRemoval && v > 0) {
            v += 0.15;
        }
        return v;
    }

    private boolean isClaimable(GameState state, StoneState stone) {
        StringBuilder key = new StringBuilder().append(stone.index()).append('|');
        stone.side(me).cards().forEach(c -> key.append(c.id()).append(','));
        key.append('|');
        stone.side(me.opponent()).cards().forEach(c -> key.append(c.id()).append(','));
        key.append('|').append(stone.requiredSize()).append(stone.isSumOnly()).append(state.discard().size());
        return claimCache.computeIfAbsent(key.toString(),
                k -> engine.checkClaim(state, me, stone.index()).allowed());
    }

    private double[] weights(Board board) {
        int n = board.stones().size();
        double[] w = new double[n];
        Arrays.fill(w, 1.0);
        PlayerId opp = me.opponent();
        for (int start = 0; start + 2 < n; start++) {
            int my = 0;
            int their = 0;
            for (int j = start; j < start + 3; j++) {
                PlayerId owner = board.stone(j).owner().orElse(null);
                if (owner == me) {
                    my++;
                } else if (owner == opp) {
                    their++;
                }
            }
            for (int j = start; j < start + 3; j++) {
                if (board.stone(j).isClaimed()) {
                    continue;
                }
                if (their == 0) {
                    w[j] += 0.35 * my;
                }
                if (my == 0) {
                    w[j] += 0.45 * their;
                }
            }
        }
        int myStones = board.stonesOwnedBy(me);
        int theirStones = board.stonesOwnedBy(opp);
        for (int j = 0; j < n; j++) {
            if (!board.stone(j).isClaimed()) {
                if (theirStones >= 4) {
                    w[j] += 1.0;
                }
                if (myStones >= 4) {
                    w[j] += 0.8;
                }
            }
        }
        return w;
    }
}
