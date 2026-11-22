package com.schottentotten.engine.action;

/**
 * Starts the next round of a multi-round match.
 */
public record NextRoundAction() implements Action {

    @Override
    public String describe() {
        return "next round";
    }
}
