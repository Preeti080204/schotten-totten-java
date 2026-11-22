package com.schottentotten.model;

/**
 * One line of the public action log.
 *
 * @param round  round number
 * @param turn   turn number within the round
 * @param player acting player, or {@code null} for system messages
 * @param text   public description of what happened
 */
public record LogEntry(int round, int turn, PlayerId player, String text) {
}
