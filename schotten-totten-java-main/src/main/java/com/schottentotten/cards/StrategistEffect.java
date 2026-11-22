package com.schottentotten.cards;

import com.schottentotten.engine.CardEffect;
import com.schottentotten.engine.EffectContext;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.GameEventType;
import com.schottentotten.engine.IllegalActionException;
import com.schottentotten.engine.ParameterField;
import com.schottentotten.model.Card;
import com.schottentotten.model.StoneState;
import com.schottentotten.model.TacticType;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategist: take one of your own formation cards from an unclaimed Stone and either place it
 * on your side of a different unclaimed Stone with room, or discard it face-up.
 */
public final class StrategistEffect implements CardEffect {

    /** Creates the effect; it holds no state and can be shared. */
    public StrategistEffect() {
    }

    @Override
    public String id() {
        return TacticType.STRATEGIST.effectId();
    }

    @Override
    public void validate(EffectContext ctx, Card card, EffectParameters params) throws IllegalActionException {
        StoneState source = ctx.requireCardOnUnclaimedStone(params.targetCardId(), ctx.actor());
        if (params.destination() == null) {
            throw new IllegalActionException("Choose a destination Stone or the discard.");
        }
        if (params.destination() == EffectParameters.DISCARD) {
            return;
        }
        StoneState dest = ctx.requireStone(params.destination());
        if (dest.index() == source.index()) {
            throw new IllegalActionException("The Strategist must move the card to a different Stone.");
        }
        if (dest.isClaimed()) {
            throw new IllegalActionException("Stone " + (dest.index() + 1) + " is already claimed.");
        }
        if (!dest.hasRoom(ctx.actor())) {
            throw new IllegalActionException("Your side of Stone " + (dest.index() + 1) + " is full.");
        }
    }

    @Override
    public void apply(EffectContext ctx, Card card, EffectParameters params) {
        StoneState source = ctx.state().board().stoneHolding(params.targetCardId()).orElseThrow();
        Card moved = source.removeCard(ctx.actor(), params.targetCardId());
        ctx.discard(card);
        if (params.destination() == EffectParameters.DISCARD) {
            ctx.discard(moved);
            ctx.emit(GameEventType.CARD_DISCARDED, ctx.name(ctx.actor()) + " plays Strategist and discards "
                    + moved.displayName() + " from Stone " + (source.index() + 1) + ".", source.index(), moved.id());
        } else {
            StoneState dest = ctx.state().board().stone(params.destination());
            dest.addCard(ctx.actor(), moved, ctx.state().sequence());
            ctx.emit(GameEventType.CARD_MOVED, ctx.name(ctx.actor()) + " plays Strategist and moves "
                    + moved.displayName() + " from Stone " + (source.index() + 1) + " to Stone "
                    + (dest.index() + 1) + ".", dest.index(), moved.id());
        }
    }

    @Override
    public List<EffectParameters> enumerate(EffectContext ctx, Card card) {
        List<EffectParameters> result = new ArrayList<>();
        List<StoneState> stones = ctx.state().board().stones();
        for (StoneState source : stones) {
            if (source.isClaimed()) {
                continue;
            }
            for (Card c : source.side(ctx.actor()).cards()) {
                for (StoneState dest : stones) {
                    if (dest.index() != source.index() && dest.hasRoom(ctx.actor())) {
                        result.add(EffectParameters.move(c.id(), dest.index()));
                    }
                }
                result.add(EffectParameters.move(c.id(), EffectParameters.DISCARD));
            }
        }
        return result;
    }

    @Override
    public String prompt(ParameterField field) {
        return field == ParameterField.TARGET_CARD
                ? "Strategist: choose one of your cards on an unclaimed Stone"
                : "Strategist: choose another unclaimed Stone, or discard the card";
    }
}
