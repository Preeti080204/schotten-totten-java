package com.schottentotten.model;

import java.util.Objects;

/**
 * Outcome of one round.
 *
 * @param round    round number (1-based)
 * @param winner   winner, or {@code null} for a drawn round
 * @param reason   why the round ended
 * @param stonesP1 Stones controlled by Player 1
 * @param stonesP2 Stones controlled by Player 2
 * @param pointsP1 victory points awarded to Player 1
 * @param pointsP2 victory points awarded to Player 2
 */
public record RoundResult(int round, PlayerId winner, VictoryReason reason, int stonesP1, int stonesP2,
                          int pointsP1, int pointsP2) {

    /** Points awarded to the round winner. */
    public static final int WINNER_POINTS = 5;

    /**
     * Validates the components.
     *
     * @param round    round number
     * @param winner   winner or {@code null}
     * @param reason   reason
     * @param stonesP1 Player 1 Stones
     * @param stonesP2 Player 2 Stones
     * @param pointsP1 Player 1 points
     * @param pointsP2 Player 2 points
     */
    public RoundResult {
        Objects.requireNonNull(reason, "reason");
    }

    /**
     * Builds the result and the official scoring: 5 points to the winner, one point per Stone to
     * the loser (both players score their Stones in a drawn round).
     *
     * @param round  round number
     * @param winner winner or {@code null}
     * @param reason reason
     * @param board  final board
     * @return the result
     */
    public static RoundResult of(int round, PlayerId winner, VictoryReason reason, Board board) {
        int s1 = board.stonesOwnedBy(PlayerId.P1);
        int s2 = board.stonesOwnedBy(PlayerId.P2);
        int p1 = winner == PlayerId.P1 ? WINNER_POINTS : s1;
        int p2 = winner == PlayerId.P2 ? WINNER_POINTS : s2;
        return new RoundResult(round, winner, reason, s1, s2, p1, p2);
    }

    /**
     * Returns the points of one player.
     *
     * @param player the player
     * @return points awarded this round
     */
    public int points(PlayerId player) {
        return player == PlayerId.P1 ? pointsP1 : pointsP2;
    }

    /**
     * Returns the Stones of one player.
     *
     * @param player the player
     * @return Stones controlled at the end of the round
     */
    public int stones(PlayerId player) {
        return player == PlayerId.P1 ? stonesP1 : stonesP2;
    }
}
