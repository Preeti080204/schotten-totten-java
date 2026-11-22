package com.schottentotten.rules;

import com.schottentotten.model.Card;
import com.schottentotten.model.CardCatalog;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.StoneSide;
import com.schottentotten.model.StoneState;
import com.schottentotten.model.TacticCard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Decides whether a player may claim a Stone, for both normal and early claims.
 *
 * <p><b>Early-claim proof.</b> When the opponent's formation is incomplete, the claim is allowed
 * only if <em>every</em> possible completion of the opponent's formation loses to the claimant.
 * The candidate cards are all formation cards of the game that are not public (not on the board
 * and not in the discard). This includes the claimant's own hand, as required by the rulebook
 * ("cards you have in hand don't count"). Unplayed Elite Troops are included, and a Joker is
 * included only if the opponent may still play one. The search is exhaustive with early exit,
 * so a false-positive claim is impossible. On a tie the claimant wins, because the claimant
 * completed the formation first.</p>
 */
public final class ClaimResolver {

    private final FormationEvaluator evaluator;
    private final CardCatalog catalog;

    /**
     * Creates a resolver.
     *
     * @param evaluator formation evaluator
     * @param catalog   card catalogue of the session
     */
    public ClaimResolver(FormationEvaluator evaluator, CardCatalog catalog) {
        this.evaluator = Objects.requireNonNull(evaluator);
        this.catalog = Objects.requireNonNull(catalog);
    }

    /**
     * Checks a claim with the strongest identities for the claimant's Elite Troops.
     *
     * @param state      game state
     * @param claimant   claiming player
     * @param stoneIndex Stone index (0-8)
     * @return the check result
     */
    public ClaimCheck check(GameState state, PlayerId claimant, int stoneIndex) {
        return check(state, claimant, stoneIndex, Map.of());
    }

    /**
     * Checks a claim with imposed identities for some of the claimant's Elite Troops.
     *
     * @param state      game state
     * @param claimant   claiming player
     * @param stoneIndex Stone index (0-8)
     * @param identities chosen Elite Troop identities keyed by card id
     * @return the check result
     */
    public ClaimCheck check(GameState state, PlayerId claimant, int stoneIndex, Map<String, EliteIdentity> identities) {
        if (stoneIndex < 0 || stoneIndex >= state.board().stones().size()) {
            return ClaimCheck.refused("There is no Stone " + (stoneIndex + 1) + ".");
        }
        StoneState stone = state.board().stone(stoneIndex);
        if (stone.isClaimed()) {
            return ClaimCheck.refused("Stone " + (stoneIndex + 1) + " is already claimed.");
        }
        PlayerId opponent = claimant.opponent();
        StoneSide mySide = stone.side(claimant);
        if (!stone.isComplete(claimant)) {
            return ClaimCheck.refused("Your formation on Stone " + (stoneIndex + 1) + " is not complete ("
                    + mySide.size() + "/" + stone.requiredSize() + ").");
        }
        for (String cardId : identities.keySet()) {
            boolean elite = mySide.cards().stream().anyMatch(c -> c.id().equals(cardId) && c.hasWildColor());
            if (!elite) {
                return ClaimCheck.refused("Card " + cardId + " is not one of your Elite Troops on this Stone.");
            }
        }
        FormationValue mine;
        try {
            mine = evaluator.evaluate(mySide.cards(), stone.isSumOnly(), identities);
        } catch (IllegalArgumentException e) {
            return ClaimCheck.refused(e.getMessage());
        }
        StoneSide theirSide = stone.side(opponent);
        if (stone.isComplete(opponent)) {
            FormationValue theirs = evaluator.evaluate(theirSide.cards(), stone.isSumOnly());
            int cmp = mine.compareTo(theirs);
            boolean wins = cmp > 0 || (cmp == 0 && mySide.completedAt() < theirSide.completedAt());
            String reason = wins
                    ? describe(mine, stone) + " beats " + describe(theirs, stone)
                    + (cmp == 0 ? " (completed first)" : "") + "."
                    : describe(mine, stone) + " does not beat " + describe(theirs, stone)
                    + (cmp == 0 ? " (your opponent completed first)" : "") + ".";
            return new ClaimCheck(wins, reason, mine, theirs, false);
        }
        int missing = stone.requiredSize() - theirSide.size();
        List<Card> pool = candidatePool(state, opponent);
        List<Card> base = new ArrayList<>(theirSide.cards());
        boolean beatable = existsBeatingCompletion(base, pool, missing, 0, mine, stone.isSumOnly());
        if (beatable) {
            return new ClaimCheck(false, "Your opponent could still beat " + describe(mine, stone)
                    + " on Stone " + (stoneIndex + 1) + ".", mine, null, false);
        }
        return new ClaimCheck(true, describe(mine, stone)
                + " cannot be beaten by any possible completion (early claim).", mine, null, true);
    }

    /**
     * Returns the formation cards that may still reach the opponent: every formation card of the
     * game that is neither on the board nor in the discard.
     *
     * @param state    game state
     * @param opponent the player whose formation would be completed
     * @return candidate cards, strongest first
     */
    public List<Card> candidatePool(GameState state, PlayerId opponent) {
        Set<String> publicIds = new HashSet<>();
        state.board().allPublicCards().forEach(c -> publicIds.add(c.id()));
        state.discard().forEach(c -> publicIds.add(c.id()));
        boolean tactics = state.config().tacticVariant();
        boolean jokerAdded = false;
        List<Card> pool = new ArrayList<>();
        for (Card card : catalog.all()) {
            if (!card.isFormationCard() || publicIds.contains(card.id())) {
                continue;
            }
            if (card instanceof TacticCard tactic) {
                if (!tactics) {
                    continue;
                }
                if (tactic.isJoker()) {
                    if (state.hasPlayedJoker(opponent) || jokerAdded) {
                        continue;
                    }
                    jokerAdded = true;
                }
            }
            pool.add(card);
        }
        pool.sort(Comparator.comparingInt(ClaimResolver::maxStrength).reversed()
                .thenComparing(c -> c instanceof ClanCard ? 1 : 0)
                .thenComparing(Card::id));
        return pool;
    }

    private boolean existsBeatingCompletion(List<Card> current, List<Card> pool, int missing, int from,
                                            FormationValue target, boolean sumOnly) {
        if (missing == 0) {
            return evaluator.evaluate(current, sumOnly).beats(target);
        }
        for (int i = from; i <= pool.size() - missing; i++) {
            current.add(pool.get(i));
            boolean found = existsBeatingCompletion(current, pool, missing - 1, i + 1, target, sumOnly);
            current.remove(current.size() - 1);
            if (found) {
                return true;
            }
        }
        return false;
    }

    private static int maxStrength(Card card) {
        return card.allowedStrengths().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    private static String describe(FormationValue value, StoneState stone) {
        return value.describe(stone.requiredSize(), stone.isSumOnly());
    }
}
