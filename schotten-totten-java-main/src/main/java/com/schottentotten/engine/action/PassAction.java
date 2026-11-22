package com.schottentotten.engine.action;

/**
 * Plays no card this turn (only allowed when no Clan card can be placed).
 */
public record PassAction() implements Action {

    @Override
    public String describe() {
        return "pass";
    }
}
