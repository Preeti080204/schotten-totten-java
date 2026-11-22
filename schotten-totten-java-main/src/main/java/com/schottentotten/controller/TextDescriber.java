package com.schottentotten.controller;

import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.ParameterField;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.ClaimAction;
import com.schottentotten.engine.action.EndTurnAction;
import com.schottentotten.engine.action.NextRoundAction;
import com.schottentotten.engine.action.PassAction;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.engine.action.ResolveEffectAction;
import com.schottentotten.model.Card;
import com.schottentotten.model.CardCatalog;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.StoneState;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Turns actions, parameters and cards into player-facing English sentences. Shared by the CLI
 * (prompts, hints) and the GUI (tooltips, hint banner) so both views speak the same language.
 */
public final class TextDescriber {

    private final CardCatalog catalog;

    /**
     * Creates a describer.
     *
     * @param catalog card catalogue used to resolve card identifiers
     */
    public TextDescriber(CardCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Returns the display name of a card.
     *
     * @param cardId card identifier
     * @return name, or the identifier if unknown
     */
    public String cardName(String cardId) {
        return catalog.find(cardId).map(Card::displayName).orElse(cardId);
    }

    /**
     * Describes a card together with its location on the board, if any.
     *
     * @param cardId card identifier
     * @param state  current state
     * @return description such as "Red 7 (Isla's side of Stone 3)"
     */
    public String cardWithLocation(String cardId, GameState state) {
        String name = cardName(cardId);
        Optional<StoneState> stone = state.board().stoneHolding(cardId);
        if (stone.isEmpty()) {
            return name;
        }
        for (PlayerId p : PlayerId.values()) {
            if (stone.get().side(p).contains(cardId)) {
                return name + " (" + state.config().profile(p).name() + "'s side of Stone "
                        + (stone.get().index() + 1) + ")";
            }
        }
        return name + " (Stone " + (stone.get().index() + 1) + ")";
    }

    /**
     * Returns the question asked to the player for a parameter field.
     *
     * @param field the field
     * @return prompt text
     */
    public String fieldPrompt(ParameterField field) {
        return switch (field) {
            case DECKS -> "Choose the decks to draw from";
            case DECK -> "Choose a deck";
            case STONE -> "Choose a Stone";
            case TARGET_CARD -> "Choose the target card";
            case DESTINATION -> "Choose where it goes";
            case SECOND_CARD -> "Choose the second card";
            case CARDS -> "Choose the cards";
        };
    }

    /**
     * Describes one value of a parameter field.
     *
     * @param field the field
     * @param value the value (may be {@code null})
     * @param state current state
     * @return description
     */
    @SuppressWarnings("unchecked")
    public String value(ParameterField field, Object value, GameState state) {
        if (value == null) {
            return "(none)";
        }
        return switch (field) {
            case STONE -> "Stone " + ((Integer) value + 1);
            case DESTINATION -> (Integer) value == EffectParameters.DISCARD
                    ? "the discard pile" : "Stone " + ((Integer) value + 1);
            case TARGET_CARD, SECOND_CARD -> cardWithLocation((String) value, state);
            case DECK -> ((DeckType) value).displayName() + " deck";
            case DECKS -> deckCounts((List<DeckType>) value);
            case CARDS -> cardList((List<String>) value);
        };
    }

    private String deckCounts(List<DeckType> decks) {
        Map<DeckType, Integer> counts = new EnumMap<>(DeckType.class);
        decks.forEach(d -> counts.merge(d, 1, Integer::sum));
        StringJoiner j = new StringJoiner(" + ");
        counts.forEach((d, n) -> j.add(n + " " + d.displayName()));
        return j.toString();
    }

    private String cardList(List<String> ids) {
        StringJoiner j = new StringJoiner(", ");
        ids.forEach(id -> j.add(cardName(id)));
        return j.toString();
    }

    /**
     * Describes an action in plain English.
     *
     * @param action the action
     * @param state  current state (used to resolve card names)
     * @return description
     */
    public String action(Action action, GameState state) {
        return switch (action) {
            case PlayCardAction a -> "Play " + cardName(a.cardId()) + parameters(a.parameters(), state);
            case ResolveEffectAction a -> "Resolve the effect:" + parameters(a.parameters(), state);
            case ClaimAction a -> "Claim Stone " + (a.stone() + 1);
            case EndTurnAction a -> a.drawFrom() == null ? "End your turn"
                    : "End your turn and draw from the " + a.drawFrom().displayName() + " deck";
            case PassAction a -> "Pass";
            case NextRoundAction a -> "Start the next round";
        };
    }

    private String parameters(EffectParameters p, GameState state) {
        StringJoiner j = new StringJoiner(", ", " ", "");
        j.setEmptyValue("");
        if (p.targetCardId() != null) {
            j.add("targeting " + cardWithLocation(p.targetCardId(), state));
        }
        if (p.secondCardId() != null) {
            j.add("with " + cardWithLocation(p.secondCardId(), state));
        }
        if (p.stone() != null) {
            j.add("on Stone " + (p.stone() + 1));
        }
        if (p.destination() != null) {
            j.add("to " + value(ParameterField.DESTINATION, p.destination(), state));
        }
        if (!p.decks().isEmpty()) {
            j.add("drawing " + deckCounts(p.decks()));
        }
        if (p.deck() != null) {
            j.add("from the " + p.deck().displayName() + " deck");
        }
        if (!p.cardIds().isEmpty()) {
            j.add("choosing " + cardList(p.cardIds()));
        }
        return j.toString();
    }
}
