package com.schottentotten.cards;

import com.schottentotten.engine.EffectContext;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.GameEventType;
import com.schottentotten.engine.IllegalActionException;
import com.schottentotten.engine.MultiStepEffect;
import com.schottentotten.engine.ParameterField;
import com.schottentotten.model.Card;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PendingEffect;
import com.schottentotten.model.TacticType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recruiter: draw three cards from one or both (or, with expansions, any) decks, then return
 * two cards of the whole hand to the bottom of their own decks.
 *
 * <p>Step 1 ({@link #apply}): the player chooses how many cards come from each deck. Step 2
 * ({@link #resolve}): the player chooses the cards to return. If fewer than three cards remain
 * in the decks, all of them are drawn; the card needs at least one drawable card.</p>
 */
public final class RecruiterEffect implements MultiStepEffect {

    /** Cards drawn by the Recruiter. */
    public static final int DRAW_COUNT = 3;
    /** Cards returned by the Recruiter. */
    public static final int RETURN_COUNT = 2;

    /** Creates the effect; it holds no state and can be shared. */
    public RecruiterEffect() {
    }

    @Override
    public String id() {
        return TacticType.RECRUITER.effectId();
    }

    @Override
    public void validate(EffectContext ctx, Card card, EffectParameters params) throws IllegalActionException {
        int expected = drawCount(ctx.state());
        if (expected == 0) {
            throw new IllegalActionException("Every deck is empty: the Recruiter has nothing to draw.");
        }
        if (params.decks().size() != expected) {
            throw new IllegalActionException("Choose exactly " + expected + " deck draws for the Recruiter.");
        }
        Map<DeckType, Integer> counts = count(params.decks());
        for (Map.Entry<DeckType, Integer> e : counts.entrySet()) {
            if (ctx.state().deckSize(e.getKey()) < e.getValue()) {
                throw new IllegalActionException("The " + e.getKey().displayName() + " deck has only "
                        + ctx.state().deckSize(e.getKey()) + " card(s).");
            }
        }
    }

    @Override
    public void apply(EffectContext ctx, Card card, EffectParameters params) {
        GameState state = ctx.state();
        ctx.discard(card);
        List<String> drawn = new ArrayList<>();
        for (DeckType type : params.decks()) {
            Card c = state.deck(type).orElseThrow().draw().orElseThrow();
            state.addToHand(ctx.actor(), c);
            drawn.add(c.id());
        }
        ctx.emit(GameEventType.EFFECT_PLAYED, ctx.name(ctx.actor()) + " plays Recruiter and draws "
                + describeCounts(count(params.decks())) + ".", -1, card.id());
        state.setPending(new PendingEffect(id(), card.id(), ctx.actor(), null, drawn));
        ctx.emit(GameEventType.EFFECT_PENDING, ctx.name(ctx.actor()) + " chooses " + returnCount(state, ctx)
                + " cards to return.", -1, card.id());
    }

    @Override
    public List<EffectParameters> enumerate(EffectContext ctx, Card card) {
        int k = drawCount(ctx.state());
        List<EffectParameters> result = new ArrayList<>();
        if (k == 0) {
            return result;
        }
        List<DeckType> available = ctx.state().nonEmptyDecks();
        enumerateMultisets(ctx.state(), available, 0, k, new ArrayList<>(), result);
        return result;
    }

    @Override
    public void validateResolution(EffectContext ctx, PendingEffect pending, EffectParameters params)
            throws IllegalActionException {
        int expected = returnCount(ctx.state(), ctx);
        Set<String> unique = new HashSet<>(params.cardIds());
        if (unique.size() != params.cardIds().size() || unique.size() != expected) {
            throw new IllegalActionException("Choose exactly " + expected + " different cards to return.");
        }
        for (String id : unique) {
            if (ctx.state().findInHand(ctx.actor(), id).isEmpty()) {
                throw new IllegalActionException("You can only return cards from your hand.");
            }
        }
    }

    @Override
    public void resolve(EffectContext ctx, PendingEffect pending, EffectParameters params) {
        GameState state = ctx.state();
        Map<DeckType, Integer> returned = new EnumMap<>(DeckType.class);
        for (String id : params.cardIds()) {
            Card c = state.removeFromHand(ctx.actor(), id);
            state.deck(c.deckType()).orElseThrow().putBottom(c);
            returned.merge(c.deckType(), 1, Integer::sum);
        }
        ctx.emit(GameEventType.EFFECT_RESOLVED, ctx.name(ctx.actor()) + " returns " + describeCounts(returned)
                + " to the bottom of the decks.", -1, pending.sourceCardId());
    }

    @Override
    public List<EffectParameters> enumerateResolutions(EffectContext ctx, PendingEffect pending) {
        List<String> ids = ctx.state().hand(ctx.actor()).stream().map(Card::id).sorted().toList();
        int k = returnCount(ctx.state(), ctx);
        List<EffectParameters> result = new ArrayList<>();
        combinations(ids, k, 0, new ArrayList<>(), result);
        return result;
    }

    @Override
    public String resolutionPrompt(PendingEffect pending) {
        return "Recruiter: choose 2 cards from your hand to put at the bottom of their decks";
    }

    @Override
    public String prompt(ParameterField field) {
        return "Recruiter: choose which decks to draw 3 cards from";
    }

    private static int drawCount(GameState state) {
        int total = state.decks().values().stream().mapToInt(d -> d.size()).sum();
        return Math.min(DRAW_COUNT, total);
    }

    private static int returnCount(GameState state, EffectContext ctx) {
        return Math.min(RETURN_COUNT, state.handSize(ctx.actor()));
    }

    private static Map<DeckType, Integer> count(List<DeckType> decks) {
        Map<DeckType, Integer> counts = new EnumMap<>(DeckType.class);
        decks.forEach(d -> counts.merge(d, 1, Integer::sum));
        return counts;
    }

    private static String describeCounts(Map<DeckType, Integer> counts) {
        return counts.entrySet().stream()
                .map(e -> e.getValue() + " " + e.getKey().displayName() + (e.getValue() > 1 ? " cards" : " card"))
                .collect(Collectors.joining(" and "));
    }

    private static void enumerateMultisets(GameState state, List<DeckType> decks, int from, int remaining,
                                           List<DeckType> current, List<EffectParameters> out) {
        if (remaining == 0) {
            out.add(EffectParameters.draws(current));
            return;
        }
        for (int i = from; i < decks.size(); i++) {
            DeckType d = decks.get(i);
            long used = current.stream().filter(x -> x == d).count();
            if (used < state.deckSize(d)) {
                current.add(d);
                enumerateMultisets(state, decks, i, remaining - 1, current, out);
                current.remove(current.size() - 1);
            }
        }
    }

    private static void combinations(List<String> ids, int k, int from, List<String> current,
                                     List<EffectParameters> out) {
        if (current.size() == k) {
            out.add(EffectParameters.cards(current));
            return;
        }
        for (int i = from; i < ids.size(); i++) {
            current.add(ids.get(i));
            combinations(ids, k, i + 1, current, out);
            current.remove(current.size() - 1);
        }
    }
}
