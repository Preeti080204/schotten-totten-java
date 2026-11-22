package com.schottentotten.cards;

import com.schottentotten.engine.EffectContext;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.GameEventType;
import com.schottentotten.engine.IllegalActionException;
import com.schottentotten.engine.MultiStepEffect;
import com.schottentotten.engine.ParameterField;
import com.schottentotten.model.Card;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.PendingEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Highland Scout (custom expansion): look at the top three cards of a chosen deck and put them
 * back in any order. Step 1 chooses the deck; step 2 provides the new order (top first). The
 * seen cards are stored in the pending effect, which views show only to the scouting player.
 */
public final class HighlandScoutEffect implements MultiStepEffect {

    /** Effect identifier. */
    public static final String ID = "expansion.highland_scout";
    /** Number of cards looked at. */
    public static final int LOOK_COUNT = 3;

    /** Creates the effect; it holds no state and can be shared. */
    public HighlandScoutEffect() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void validate(EffectContext ctx, Card card, EffectParameters params) throws IllegalActionException {
        DeckType deck = params.deck();
        if (deck == null) {
            throw new IllegalActionException("Choose the deck to scout.");
        }
        if (ctx.state().deckSize(deck) == 0) {
            throw new IllegalActionException("The " + deck.displayName() + " deck is empty or not in play.");
        }
    }

    @Override
    public void apply(EffectContext ctx, Card card, EffectParameters params) {
        ctx.discard(card);
        List<String> seen = ctx.state().deck(params.deck()).orElseThrow().peek(LOOK_COUNT).stream()
                .map(Card::id).toList();
        ctx.state().setPending(new PendingEffect(ID, card.id(), ctx.actor(), params.deck(), seen));
        ctx.emit(GameEventType.EFFECT_PLAYED, ctx.name(ctx.actor()) + " plays Highland Scout and looks at the top "
                + seen.size() + " cards of the " + params.deck().displayName() + " deck.", -1, card.id());
    }

    @Override
    public List<EffectParameters> enumerate(EffectContext ctx, Card card) {
        return ctx.state().nonEmptyDecks().stream().map(EffectParameters::ofDeck).toList();
    }

    @Override
    public void validateResolution(EffectContext ctx, PendingEffect pending, EffectParameters params)
            throws IllegalActionException {
        List<String> order = params.cardIds();
        if (order.size() != pending.cardIds().size() || !new HashSet<>(order).equals(new HashSet<>(pending.cardIds()))) {
            throw new IllegalActionException("Put back exactly the scouted cards, each once, in your chosen order.");
        }
    }

    @Override
    public void resolve(EffectContext ctx, PendingEffect pending, EffectParameters params) {
        List<Card> order = params.cardIds().stream().map(id -> ctx.state().deck(pending.deck()).orElseThrow()
                .peek(LOOK_COUNT).stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow()).toList();
        ctx.state().deck(pending.deck()).orElseThrow().reorderTop(order);
        ctx.emit(GameEventType.EFFECT_RESOLVED, ctx.name(ctx.actor()) + " puts the scouted cards back on the "
                + pending.deck().displayName() + " deck.", -1, pending.sourceCardId());
    }

    @Override
    public List<EffectParameters> enumerateResolutions(EffectContext ctx, PendingEffect pending) {
        List<EffectParameters> result = new ArrayList<>();
        permute(new ArrayList<>(pending.cardIds()), 0, result);
        return result;
    }

    @Override
    public String resolutionPrompt(PendingEffect pending) {
        return "Highland Scout: put the cards back on the " + pending.deck().displayName() + " deck (first = top)";
    }

    @Override
    public String prompt(ParameterField field) {
        return "Highland Scout: choose a deck to scout";
    }

    private static void permute(List<String> ids, int k, List<EffectParameters> out) {
        if (k >= ids.size()) {
            out.add(EffectParameters.cards(ids));
            return;
        }
        for (int i = k; i < ids.size(); i++) {
            Collections.swap(ids, k, i);
            permute(ids, k + 1, out);
            Collections.swap(ids, k, i);
        }
    }
}
