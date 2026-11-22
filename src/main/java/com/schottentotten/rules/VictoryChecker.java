package com.schottentotten.rules;

import com.schottentotten.model.Board;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.VictoryReason;

import java.util.Optional;

/**
 * Detects the two immediate victory conditions: three adjacent Stones or five Stones anywhere.
 */
public final class VictoryChecker {

    /** Number of adjacent Stones needed for victory. */
    public static final int ADJACENT_TO_WIN = 3;
    /** Number of Stones anywhere needed for victory. */
    public static final int TOTAL_TO_WIN = 5;

    /** Creates a checker; it holds no state and can be shared. */
    public VictoryChecker() {
    }

    /**
     * A detected victory.
     *
     * @param winner the winner
     * @param reason the condition met
     */
    public record Victory(PlayerId winner, VictoryReason reason) {
    }

    /**
     * Checks the board for a winner.
     *
     * @param board the board
     * @return the victory, if a player meets a condition
     */
    public Optional<Victory> check(Board board) {
        for (PlayerId p : PlayerId.values()) {
            if (hasAdjacent(board, p)) {
                return Optional.of(new Victory(p, VictoryReason.THREE_ADJACENT));
            }
        }
        for (PlayerId p : PlayerId.values()) {
            if (board.stonesOwnedBy(p) >= TOTAL_TO_WIN) {
                return Optional.of(new Victory(p, VictoryReason.FIVE_STONES));
            }
        }
        return Optional.empty();
    }

    /**
     * Tells whether a player controls at least three adjacent Stones.
     *
     * @param board  the board
     * @param player the player
     * @return {@code true} if three adjacent Stones are owned
     */
    public boolean hasAdjacent(Board board, PlayerId player) {
        int run = 0;
        for (var stone : board.stones()) {
            run = stone.owner().orElse(null) == player ? run + 1 : 0;
            if (run >= ADJACENT_TO_WIN) {
                return true;
            }
        }
        return false;
    }
}
