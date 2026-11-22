package com.schottentotten.ai;

import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.ClanColor;
import com.schottentotten.rules.FormationEvaluator;
import com.schottentotten.rules.FormationRank;
import com.schottentotten.rules.FormationValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Estimates the eventual strength of (possibly incomplete) formations for the virtual players.
 *
 * <p>A complete formation gets its exact value. An incomplete formation gets an expected value:
 * every category still reachable with the cards the player believes available is weighted by
 * an approximate probability that decreases with the number of missing cards. When the player's
 * own hand can complete the formation, that exact completion is also considered. Values are on
 * the scale {@code rank * 30 + sum}, so a better category always dominates the sum.</p>
 */
final class FormationHeuristics {

    private static final double[] P_COLOR_RUN = {1, 0.40, 0.15, 0.05, 0.02};
    private static final double[] P_KIND = {1, 0.35, 0.10, 0.03, 0.01};
    private static final double[] P_COLOR = {1, 0.55, 0.30, 0.15, 0.08};
    private static final double[] P_RUN = {1, 0.50, 0.28, 0.15, 0.08};
    private static final double AVERAGE_STRENGTH = 5.0;
    private static final int RANK_STEP = 30;

    private final FormationEvaluator evaluator = new FormationEvaluator();
    private final boolean[][] available = new boolean[ClanColor.values().length][10];

    /**
     * Creates heuristics for a set of cards believed obtainable.
     *
     * @param obtainable cards that may still be drawn or are in the player's hand
     */
    FormationHeuristics(Collection<Card> obtainable) {
        for (Card c : obtainable) {
            if (c instanceof ClanCard clan) {
                available[clan.color().ordinal()][clan.strength()] = true;
            }
        }
    }

    /**
     * Converts an exact formation value to the heuristic scale.
     *
     * @param v       formation value
     * @param sumOnly whether only the sum counts
     * @return score
     */
    static double value(FormationValue v, boolean sumOnly) {
        return sumOnly ? v.sum() * 3.0 : v.rank().ordinal() * RANK_STEP + v.sum();
    }

    /**
     * Estimates the final value of a formation.
     *
     * @param present   cards already on the side
     * @param size      required formation size
     * @param sumOnly   whether only the sum counts
     * @param handCards formation cards the owner holds (empty for the opponent)
     * @return expected score
     */
    double estimate(List<Card> present, int size, boolean sumOnly, List<Card> handCards) {
        if (present.size() >= size) {
            return value(evaluator.evaluate(present.subList(0, size), sumOnly), sumOnly);
        }
        int missing = size - present.size();
        double pattern = patternEstimate(present, missing, size, sumOnly);
        if (!present.isEmpty() && missing <= 2 && handCards.size() >= missing) {
            double best = bestFromHand(present, handCards, missing, sumOnly);
            pattern = Math.max(pattern, best - (missing == 1 ? 2.0 : 10.0));
        }
        return pattern;
    }

    private double bestFromHand(List<Card> present, List<Card> hand, int missing, boolean sumOnly) {
        double[] best = {Double.NEGATIVE_INFINITY};
        List<Card> current = new ArrayList<>(present);
        search(current, hand, missing, 0, sumOnly, best);
        return best[0];
    }

    private void search(List<Card> current, List<Card> hand, int missing, int from, boolean sumOnly, double[] best) {
        if (missing == 0) {
            best[0] = Math.max(best[0], value(evaluator.evaluate(current, sumOnly), sumOnly));
            return;
        }
        for (int i = from; i <= hand.size() - missing; i++) {
            current.add(hand.get(i));
            search(current, hand, missing - 1, i + 1, sumOnly, best);
            current.remove(current.size() - 1);
        }
    }

    private double patternEstimate(List<Card> present, int missing, int size, boolean sumOnly) {
        List<ClanCard> clans = new ArrayList<>();
        int wild = 0;
        int presentSum = 0;
        for (Card c : present) {
            if (c instanceof ClanCard clan) {
                clans.add(clan);
                presentSum += clan.strength();
            } else {
                wild++;
                presentSum += c.allowedStrengths().stream().mapToInt(Integer::intValue).max().orElse(0);
            }
        }
        double sumValue = presentSum + missing * AVERAGE_STRENGTH;
        if (sumOnly) {
            return sumValue * 3.0;
        }
        int fromPool = size - clans.size() - wild;
        double remaining = 1.0;
        double expected = 0.0;
        double colorRun = colorRunValue(clans, size, wild);
        if (colorRun > 0) {
            expected += remaining * P_COLOR_RUN[missing] * colorRun;
            remaining *= 1 - P_COLOR_RUN[missing];
        }
        double kind = kindValue(clans, size, fromPool);
        if (kind > 0) {
            expected += remaining * P_KIND[missing] * kind;
            remaining *= 1 - P_KIND[missing];
        }
        double color = colorValue(clans, presentSum, fromPool);
        if (color > 0) {
            expected += remaining * P_COLOR[missing] * color;
            remaining *= 1 - P_COLOR[missing];
        }
        double run = runValue(clans, size, wild);
        if (run > 0) {
            expected += remaining * P_RUN[missing] * run;
            remaining *= 1 - P_RUN[missing];
        }
        return expected + remaining * sumValue;
    }

    private double colorRunValue(List<ClanCard> clans, int size, int wild) {
        if (!sameColor(clans)) {
            return 0;
        }
        double best = 0;
        for (ClanColor color : clans.isEmpty() ? List.of(ClanColor.values()) : List.of(clans.get(0).color())) {
            double v = windowValue(clans, size, wild, color);
            best = Math.max(best, v > 0 ? FormationRank.COLOR_RUN.ordinal() * RANK_STEP + v : 0);
        }
        return best;
    }

    private double runValue(List<ClanCard> clans, int size, int wild) {
        double v = windowValue(clans, size, wild, null);
        return v > 0 ? FormationRank.RUN.ordinal() * RANK_STEP + v : 0;
    }

    private double windowValue(List<ClanCard> clans, int size, int wild, ClanColor color) {
        boolean[] have = new boolean[10];
        for (ClanCard c : clans) {
            if (have[c.strength()]) {
                return 0;
            }
            have[c.strength()] = true;
        }
        for (int start = 10 - size; start >= 1; start--) {
            int low = start;
            int end = start + size - 1;
            boolean fits = clans.stream().allMatch(c -> c.strength() >= low && c.strength() <= end);
            if (!fits) {
                continue;
            }
            int unavailable = 0;
            for (int s = start; s <= end; s++) {
                if (!have[s] && !isAvailable(color, s)) {
                    unavailable++;
                }
            }
            if (unavailable <= wild) {
                return (start + end) * size / 2.0;
            }
        }
        return 0;
    }

    private double kindValue(List<ClanCard> clans, int size, int fromPool) {
        if (clans.isEmpty()) {
            for (int s = 9; s >= 1; s--) {
                if (countStrength(s) >= fromPool) {
                    return FormationRank.SAME_STRENGTH.ordinal() * RANK_STEP + size * s;
                }
            }
            return 0;
        }
        int v = clans.get(0).strength();
        if (clans.stream().anyMatch(c -> c.strength() != v) || countStrength(v) < fromPool) {
            return 0;
        }
        return FormationRank.SAME_STRENGTH.ordinal() * RANK_STEP + size * v;
    }

    private double colorValue(List<ClanCard> clans, int presentSum, int fromPool) {
        if (!sameColor(clans)) {
            return 0;
        }
        double best = 0;
        for (ClanColor color : clans.isEmpty() ? List.of(ClanColor.values()) : List.of(clans.get(0).color())) {
            int needed = fromPool;
            int sum = presentSum;
            for (int s = 9; s >= 1 && needed > 0; s--) {
                int strength = s;
                if (available[color.ordinal()][s] && clans.stream().noneMatch(c -> c.strength() == strength)) {
                    sum += s;
                    needed--;
                }
            }
            if (needed == 0) {
                best = Math.max(best, FormationRank.COLOR.ordinal() * RANK_STEP + sum);
            }
        }
        return best;
    }

    private boolean isAvailable(ClanColor color, int strength) {
        if (color != null) {
            return available[color.ordinal()][strength];
        }
        for (boolean[] row : available) {
            if (row[strength]) {
                return true;
            }
        }
        return false;
    }

    private int countStrength(int strength) {
        int n = 0;
        for (boolean[] row : available) {
            if (row[strength]) {
                n++;
            }
        }
        return n;
    }

    private static boolean sameColor(List<ClanCard> clans) {
        return clans.stream().map(ClanCard::color).distinct().count() <= 1;
    }
}
