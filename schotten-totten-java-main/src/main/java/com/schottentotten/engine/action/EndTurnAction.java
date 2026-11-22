package com.schottentotten.engine.action;

import com.schottentotten.model.DeckType;

/**
 * Closes the claim window, draws one card and passes the turn.
 *
 * @param drawFrom deck to draw from, or {@code null} when no draw is possible or only one deck
 *                 is available
 */
public record EndTurnAction(DeckType drawFrom) implements Action {

    @Override
    public String describe() {
        return drawFrom == null ? "end turn" : "end turn drawing from " + drawFrom.displayName();
    }
}
