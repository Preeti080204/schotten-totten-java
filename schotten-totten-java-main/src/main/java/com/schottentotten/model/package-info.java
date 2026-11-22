/**
 * Game model: cards, decks, the nine Stones, players and the complete mutable
 * {@link com.schottentotten.model.GameState}.
 *
 * <p>The model holds data only. Rules live in {@code com.schottentotten.rules}, the turn
 * sequence in {@code com.schottentotten.engine} and card behaviour in
 * {@code com.schottentotten.cards}. Cards are immutable value objects identified by a stable
 * identifier (for example {@code "RED-5"} or {@code "T-TRAITOR"}), which is what save files,
 * commands and events refer to.</p>
 */
package com.schottentotten.model;
