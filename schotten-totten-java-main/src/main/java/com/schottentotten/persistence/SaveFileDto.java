package com.schottentotten.persistence;

import java.util.List;
import java.util.Map;

/**
 * JSON structure of a save file. Only plain data (strings, numbers, lists) is stored; cards are
 * referenced by their stable identifiers. No Java serialisation is involved.
 *
 * @param formatVersion save format version, see {@link SaveManager#FORMAT_VERSION}
 * @param application   application identifier
 * @param savedAt       ISO-8601 timestamp
 * @param game          game content
 */
public record SaveFileDto(int formatVersion, String application, String savedAt, GameDto game) {

    /**
     * Session configuration.
     *
     * @param player1         Player 1 profile
     * @param player2         Player 2 profile
     * @param tacticVariant   Tactic variant flag
     * @param expansions      expansions flag
     * @param rounds          number of rounds
     * @param firstPlayerRule first-player rule name
     * @param seed            seed
     */
    public record ConfigDto(PlayerDto player1, PlayerDto player2, boolean tacticVariant, boolean expansions,
                            int rounds, String firstPlayerRule, long seed) {
    }

    /**
     * Player profile.
     *
     * @param name       name
     * @param type       controller type name
     * @param difficulty difficulty name
     */
    public record PlayerDto(String name, String type, String difficulty) {
    }

    /**
     * One side of a Stone.
     *
     * @param cards       card ids in play order
     * @param completedAt completion sequence or -1
     * @param wardCardId  ward card id or {@code null}
     * @param wardTurns   remaining ward turns
     */
    public record SideDto(List<String> cards, long completedAt, String wardCardId, int wardTurns) {
    }

    /**
     * One Stone.
     *
     * @param p1          Player 1 side
     * @param p2          Player 2 side
     * @param combatModes combat-mode card ids
     * @param owner       owner seat name or {@code null}
     */
    public record StoneDto(SideDto p1, SideDto p2, List<String> combatModes, String owner) {
    }

    /**
     * Pending multi-step effect.
     *
     * @param effectId     effect id
     * @param sourceCardId source card id
     * @param player       deciding seat name
     * @param deck         deck name or {@code null}
     * @param cardIds      card ids
     */
    public record PendingDto(String effectId, String sourceCardId, String player, String deck, List<String> cardIds) {
    }

    /**
     * Round result.
     *
     * @param round    round number
     * @param winner   winner seat name or {@code null}
     * @param reason   reason name
     * @param stonesP1 Player 1 Stones
     * @param stonesP2 Player 2 Stones
     * @param pointsP1 Player 1 points
     * @param pointsP2 Player 2 points
     */
    public record RoundDto(int round, String winner, String reason, int stonesP1, int stonesP2, int pointsP1,
                           int pointsP2) {
    }

    /**
     * Log entry.
     *
     * @param round  round
     * @param turn   turn
     * @param player seat name or {@code null}
     * @param text   text
     */
    public record LogDto(int round, int turn, String player, String text) {
    }

    /**
     * Full game content.
     *
     * @param config            configuration
     * @param round             current round
     * @param turn              current turn
     * @param currentPlayer     current seat
     * @param roundStarter      round starter seat
     * @param phase             turn phase
     * @param hands             hands by seat
     * @param decks             decks by type, top first
     * @param discard           discard pile
     * @param stones            the nine Stones
     * @param specialPlayed     special cards played by seat
     * @param jokerPlayed       seats that played a Joker
     * @param pending           pending effect or {@code null}
     * @param sequence          action sequence
     * @param consecutivePasses pass counter
     * @param lastRound         last round result or {@code null}
     * @param results           match results
     * @param rngState          random generator state
     * @param log               public log
     * @param version           state version
     */
    public record GameDto(ConfigDto config, int round, int turn, String currentPlayer, String roundStarter,
                          String phase, Map<String, List<String>> hands, Map<String, List<String>> decks,
                          List<String> discard, List<StoneDto> stones, Map<String, Integer> specialPlayed,
                          List<String> jokerPlayed, PendingDto pending, long sequence, int consecutivePasses,
                          RoundDto lastRound, List<RoundDto> results, long rngState, List<LogDto> log,
                          long version) {
    }
}
