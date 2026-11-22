/**
 * Virtual players, built with the Strategy design pattern.
 *
 * <p>A {@link com.schottentotten.ai.PlayerStrategy} receives a
 * {@link com.schottentotten.ai.PlayerView}: a copy of the game in which the opponent's hand and
 * the order of the decks are replaced by a random redistribution of the unseen cards, so a
 * strategy cannot cheat. {@link com.schottentotten.ai.SimpleStrategy} (Easy) and
 * {@link com.schottentotten.ai.AdvancedStrategy} (Hard) share the template of
 * {@link com.schottentotten.ai.AbstractStrategy}; {@link com.schottentotten.ai.StrategyFactory}
 * maps a difficulty to a strategy. The chosen action goes through the same engine validation as
 * a human action.</p>
 */
package com.schottentotten.ai;
