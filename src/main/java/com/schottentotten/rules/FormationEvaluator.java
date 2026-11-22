package com.schottentotten.rules;

import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.ClanColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates complete formations, including Elite Troops whose colour and strength are chosen by
 * their owner.
 *
 * <p>The evaluator is stateless and deterministic. For wild cards it searches every allowed
 * strength combination. Colours only matter for the "same colour" categories, so all wild cards
 * take the common colour of the fixed cards (or an arbitrary colour if every card is wild). This
 * makes the search exact while trying at most 9 x 9 x 3 combinations.</p>
 */
public final class FormationEvaluator {

    /** Creates an evaluator; it holds no state and can be shared. */
    public FormationEvaluator() {
    }

    /**
     * Evaluates a complete formation choosing the best identity for every Elite Troop.
     *
     * @param cards   formation cards (exactly the required number)
     * @param sumOnly {@code true} under Blind-Man's Bluff
     * @return best value
     */
    public FormationValue evaluate(List<Card> cards, boolean sumOnly) {
        return evaluate(cards, sumOnly, Map.of());
    }

    /**
     * Evaluates a complete formation where some Elite Troops have an imposed identity.
     *
     * @param cards     formation cards
     * @param sumOnly   {@code true} under Blind-Man's Bluff
     * @param overrides imposed identities keyed by card id (other Elite Troops are optimised)
     * @return best value given the overrides
     * @throws IllegalArgumentException if an override is not allowed for its card
     */
    public FormationValue evaluate(List<Card> cards, boolean sumOnly, Map<String, EliteIdentity> overrides) {
        int n = cards.size();
        ClanColor[] colors = new ClanColor[n];
        int[] strengths = new int[n];
        List<Integer> wild = new ArrayList<>();
        Map<String, EliteIdentity> fixedIdentities = new HashMap<>();
        for (int i = 0; i < n; i++) {
            Card card = cards.get(i);
            if (card instanceof ClanCard clan) {
                colors[i] = clan.color();
                strengths[i] = clan.strength();
            } else if (overrides.containsKey(card.id())) {
                EliteIdentity id = overrides.get(card.id());
                if (!card.allowedStrengths().contains(id.strength())) {
                    throw new IllegalArgumentException(card.displayName() + " cannot have strength " + id.strength());
                }
                colors[i] = id.color();
                strengths[i] = id.strength();
                fixedIdentities.put(card.id(), id);
            } else if (card.isFormationCard()) {
                wild.add(i);
            } else {
                throw new IllegalArgumentException("Not a formation card: " + card.id());
            }
        }
        ClanColor wildColor = commonColor(colors);
        for (int i : wild) {
            colors[i] = wildColor;
        }
        Search search = new Search(cards, colors, strengths, wild, sumOnly);
        search.run(0);
        Map<String, EliteIdentity> identities = new HashMap<>(fixedIdentities);
        for (int k = 0; k < wild.size(); k++) {
            int i = wild.get(k);
            identities.put(cards.get(i).id(), new EliteIdentity(wildColor, search.bestStrengths[k]));
        }
        return new FormationValue(search.bestRank, search.bestSum, identities);
    }

    /**
     * Classifies concrete cards (colour and strength known).
     *
     * @param colors    card colours
     * @param strengths card strengths
     * @param sumOnly   whether only the sum counts
     * @return the formation rank
     */
    public static FormationRank rankOf(ClanColor[] colors, int[] strengths, boolean sumOnly) {
        if (sumOnly) {
            return FormationRank.SUM;
        }
        boolean sameColor = Arrays.stream(colors).allMatch(c -> c == colors[0]);
        int[] sorted = strengths.clone();
        Arrays.sort(sorted);
        boolean sameStrength = sorted[0] == sorted[sorted.length - 1];
        boolean run = true;
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] != sorted[i - 1] + 1) {
                run = false;
                break;
            }
        }
        if (sameColor && run) {
            return FormationRank.COLOR_RUN;
        }
        if (sameStrength) {
            return FormationRank.SAME_STRENGTH;
        }
        if (sameColor) {
            return FormationRank.COLOR;
        }
        return run ? FormationRank.RUN : FormationRank.SUM;
    }

    private static ClanColor commonColor(ClanColor[] colors) {
        ClanColor found = null;
        for (ClanColor c : colors) {
            if (c != null) {
                if (found == null) {
                    found = c;
                } else if (found != c) {
                    return found;
                }
            }
        }
        return found == null ? ClanColor.RED : found;
    }

    /** Exhaustive search over wild-card strengths. */
    private static final class Search {
        private final List<Card> cards;
        private final ClanColor[] colors;
        private final int[] strengths;
        private final List<Integer> wild;
        private final boolean sumOnly;
        private FormationRank bestRank;
        private int bestSum = -1;
        private int[] bestStrengths;

        Search(List<Card> cards, ClanColor[] colors, int[] strengths, List<Integer> wild, boolean sumOnly) {
            this.cards = cards;
            this.colors = colors;
            this.strengths = strengths;
            this.wild = wild;
            this.sumOnly = sumOnly;
            this.bestStrengths = new int[wild.size()];
        }

        void run(int k) {
            if (k == wild.size()) {
                FormationRank rank = rankOf(colors, strengths, sumOnly);
                int sum = Arrays.stream(strengths).sum();
                if (bestRank == null || rank.compareTo(bestRank) > 0 || (rank == bestRank && sum > bestSum)) {
                    bestRank = rank;
                    bestSum = sum;
                    for (int j = 0; j < wild.size(); j++) {
                        bestStrengths[j] = strengths[wild.get(j)];
                    }
                }
                return;
            }
            int index = wild.get(k);
            int[] allowed = cards.get(index).allowedStrengths().stream().mapToInt(Integer::intValue).sorted().toArray();
            for (int s : allowed) {
                strengths[index] = s;
                run(k + 1);
            }
        }
    }
}
