package com.schottentotten.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Victory points over a multi-round match. A single game is a match of one round.
 */
public final class MatchScore {

    private final int totalRounds;
    private final List<RoundResult> results;

    /**
     * Creates an empty score sheet.
     *
     * @param totalRounds number of rounds of the match
     */
    public MatchScore(int totalRounds) {
        this(totalRounds, List.of());
    }

    /**
     * Restores a score sheet.
     *
     * @param totalRounds number of rounds
     * @param results     results of the rounds already played
     */
    public MatchScore(int totalRounds, List<RoundResult> results) {
        this.totalRounds = totalRounds;
        this.results = new ArrayList<>(results);
    }

    /**
     * Records a finished round.
     *
     * @param result the round result
     */
    public void record(RoundResult result) {
        if (isComplete()) {
            throw new IllegalStateException("Match already complete");
        }
        results.add(result);
    }

    /**
     * Returns the configured number of rounds.
     *
     * @return number of rounds
     */
    public int totalRounds() {
        return totalRounds;
    }

    /**
     * Returns the results so far.
     *
     * @return unmodifiable copy
     */
    public List<RoundResult> results() {
        return List.copyOf(results);
    }

    /**
     * Returns the total points of a player.
     *
     * @param player the player
     * @return sum of points over the recorded rounds
     */
    public int points(PlayerId player) {
        return results.stream().mapToInt(r -> r.points(player)).sum();
    }

    /**
     * Returns the number of rounds won by a player.
     *
     * @param player the player
     * @return rounds won
     */
    public int roundsWon(PlayerId player) {
        return (int) results.stream().filter(r -> r.winner() == player).count();
    }

    /**
     * Tells whether every round has been played.
     *
     * @return {@code true} when the match is over
     */
    public boolean isComplete() {
        return results.size() >= totalRounds;
    }

    /**
     * Returns the match winner: most points, then most rounds won.
     *
     * @return the winner, or empty for a drawn match or an unfinished match
     */
    public Optional<PlayerId> winner() {
        if (!isComplete()) {
            return Optional.empty();
        }
        int diff = points(PlayerId.P1) - points(PlayerId.P2);
        if (diff == 0) {
            diff = roundsWon(PlayerId.P1) - roundsWon(PlayerId.P2);
        }
        if (diff == 0) {
            return Optional.empty();
        }
        return Optional.of(diff > 0 ? PlayerId.P1 : PlayerId.P2);
    }

    /**
     * Creates an independent copy.
     *
     * @return copy
     */
    public MatchScore copy() {
        return new MatchScore(totalRounds, results);
    }
}
