package com.schottentotten.ai;

import com.schottentotten.engine.action.Action;

/**
 * Decision policy of a virtual player (Strategy pattern). A strategy receives a
 * {@link PlayerView} that contains only the information the player is entitled to, and returns
 * one of the legal actions. The action is then submitted to the same engine validation as a
 * human action.
 */
public interface PlayerStrategy {

    /**
     * Chooses the next action.
     *
     * @param view what the player knows
     * @return a legal action for the current phase
     */
    Action chooseAction(PlayerView view);

    /**
     * Returns the display name of the strategy.
     *
     * @return name
     */
    String name();
}
