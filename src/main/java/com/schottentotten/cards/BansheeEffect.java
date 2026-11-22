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
 * Banshee: discard one formation card (Clan or Elite Troop) from the opponent's side of an
 * unclaimed Stone. Sides protected by a Stone Ward cannot be targeted.
 */
public final class BansheeEffect implements CardEffect {

    /** Creates the effect; it holds no state and can be shared. */
    public BansheeEffect() {
    }

    @Override
    public String id() {
        return TacticType.BANSHEE.effectId();
    }

    @Override
    public void validate(EffectContext ctx, Card card, EffectParameters params) throws IllegalActionException {
        StoneState stone = ctx.requireCardOnUnclaimedStone(params.targetCardId(), ctx.opponent());
        if (ctx.isProtected(stone, ctx.opponent())) {
            throw new IllegalActionException("That card is protected by a Stone Ward.");
        }
    }

    @Override
    public void apply(EffectContext ctx, Card card, EffectParameters params) {
        StoneState stone = ctx.state().board().stoneHolding(params.targetCardId()).orElseThrow();
        Card removed = stone.removeCard(ctx.opponent(), params.targetCardId());
        ctx.discard(card);
        ctx.discard(removed);
        ctx.emit(GameEventType.CARD_DISCARDED, ctx.name(ctx.actor()) + " plays Banshee and discards "
                + ctx.name(ctx.opponent()) + "'s " + removed.displayName() + " from Stone " + (stone.index() + 1)
                + ".", stone.index(), removed.id());
    }

    @Override
    public List<EffectParameters> enumerate(EffectContext ctx, Card card) {
        List<EffectParameters> result = new ArrayList<>();
        for (StoneState stone : ctx.state().board().stones()) {
            if (stone.isClaimed() || ctx.isProtected(stone, ctx.opponent())) {
                continue;
            }
            for (Card c : stone.side(ctx.opponent()).cards()) {
                result.add(EffectParameters.target(c.id()));
            }
        }
        return result;
    }

    @Override
    public String prompt(ParameterField field) {
        return "Banshee: choose an opponent's card on an unclaimed Stone to discard";
    }
}
