package com.schottentotten.model;

import java.util.Objects;

/**
 * Immutable options of one game session, chosen on the new-game screen or with the CLI
 * {@code new} command. It is passed explicitly to the engine (no global state), so several
 * sessions with different rules can coexist.
 *
 * @param player1         Player 1 profile
 * @param player2         Player 2 profile
 * @param tacticVariant   {@code true} to play the official Tactic variant
 * @param expansions      {@code true} to include the custom expansion cards
 * @param rounds          number of rounds of the match (1 for a single game)
 * @param firstPlayerRule how the first player is chosen
 * @param seed            random seed for shuffling (reproducible games)
 */
public record GameConfiguration(PlayerProfile player1, PlayerProfile player2, boolean tacticVariant,
                                boolean expansions, int rounds, FirstPlayerRule firstPlayerRule, long seed) {

    /** Maximum number of rounds allowed in a match. */
    public static final int MAX_ROUNDS = 9;

    /**
     * Validates the configuration.
     *
     * @param player1         Player 1 profile
     * @param player2         Player 2 profile
     * @param tacticVariant   Tactic variant flag
     * @param expansions      expansion flag
     * @param rounds          number of rounds
     * @param firstPlayerRule first-player rule
     * @param seed            random seed
     */
    public GameConfiguration {
        Objects.requireNonNull(player1, "player1");
        Objects.requireNonNull(player2, "player2");
        Objects.requireNonNull(firstPlayerRule, "firstPlayerRule");
        if (rounds < 1 || rounds > MAX_ROUNDS) {
            throw new IllegalArgumentException("Rounds must be between 1 and " + MAX_ROUNDS);
        }
    }

    /**
     * Returns the profile of a seat.
     *
     * @param player the seat
     * @return the profile
     */
    public PlayerProfile profile(PlayerId player) {
        return player == PlayerId.P1 ? player1 : player2;
    }

    /**
     * Tells whether this is a multi-round match.
     *
     * @return {@code true} if more than one round is played
     */
    public boolean isMultiRound() {
        return rounds > 1;
    }

    /**
     * Returns a short description of the selected rules.
     *
     * @return text such as {@code "Tactic variant + expansions, 3 rounds"}
     */
    public String describeRules() {
        StringBuilder sb = new StringBuilder(tacticVariant ? "Tactic variant" : "Base game");
        if (expansions) {
            sb.append(" + expansions");
        }
        sb.append(rounds > 1 ? ", " + rounds + " rounds" : ", single round");
        return sb.toString();
    }
}
