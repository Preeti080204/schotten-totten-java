package com.schottentotten.cards;

import com.schottentotten.engine.CardEffect;
import com.schottentotten.engine.EffectContext;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.GameEventType;
import com.schottentotten.engine.IllegalActionException;
import com.schottentotten.engine.LifecycleHook;
import com.schottentotten.engine.ParameterField;
import com.schottentotten.model.ActiveWard;
import com.schottentotten.model.Card;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.StoneState;

import java.util.ArrayList;
import java.util.List;

/**
 * Stone Ward (custom expansion): protects the player's cards on one unclaimed Stone from the
 * opponent's Banshee and Traitor for the opponent's next {@value #DURATION} turns.
 *
 * <p>The ward does not occupy a formation slot. Its duration is tracked in {@link ActiveWard}
 * and counted down through the {@link LifecycleHook} callbacks; it is discarded when it expires or
 * when the Stone is claimed.</p>
 */
public final class StoneWardEffect implements CardEffect, LifecycleHook {

    /** Effect identifier. */
    public static final String ID = "expansion.stone_ward";
    /** Number of opponent turns the ward lasts. */
    public static final int DURATION = 2;

    /** Creates the effect; it holds no state and can be shared. */
    public StoneWardEffect() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void validate(EffectContext ctx, Card card, EffectParameters params) throws IllegalActionException {
        StoneState stone = ctx.requireStone(params.stone());
        if (stone.isClaimed()) {
            throw new IllegalActionException("Stone " + (stone.index() + 1) + " is already claimed.");
        }
        if (stone.side(ctx.actor()).size() == 0) {
            throw new IllegalActionException("You need at least one card on Stone " + (stone.index() + 1)
                    + " to ward it.");
        }
        if (stone.side(ctx.actor()).ward().isPresent()) {
            throw new IllegalActionException("Your side of Stone " + (stone.index() + 1) + " is already warded.");
        }
    }

    @Override
    public void apply(EffectContext ctx, Card card, EffectParameters params) {
        StoneState stone = ctx.state().board().stone(params.stone());
        stone.setWard(ctx.actor(), new ActiveWard(card, DURATION));
        ctx.emit(GameEventType.WARD_CHANGED, ctx.name(ctx.actor()) + " places a Stone Ward on Stone "
                + (stone.index() + 1) + " (protected for " + DURATION + " opponent turns).", stone.index(), card.id());
    }

    @Override
    public List<EffectParameters> enumerate(EffectContext ctx, Card card) {
        List<EffectParameters> result = new ArrayList<>();
        for (StoneState stone : ctx.state().board().stones()) {
            if (!stone.isClaimed() && stone.side(ctx.actor()).size() > 0 && stone.side(ctx.actor()).ward().isEmpty()) {
                result.add(EffectParameters.onStone(stone.index()));
            }
        }
        return result;
    }

    @Override
    public String prompt(ParameterField field) {
        return "Stone Ward: choose an unclaimed Stone where you have cards";
    }

    @Override
    public void onTurnEnd(EffectContext ctx, PlayerId endingPlayer) {
        PlayerId wardOwner = endingPlayer.opponent();
        for (StoneState stone : ctx.state().board().stones()) {
            stone.side(wardOwner).ward().ifPresent(ward -> {
                int left = ward.remainingOpponentTurns() - 1;
                if (left <= 0) {
                    stone.setWard(wardOwner, null);
                    ctx.discard(ward.card());
                    ctx.emit(GameEventType.WARD_CHANGED, "The Stone Ward of " + ctx.name(wardOwner) + " on Stone "
                            + (stone.index() + 1) + " fades.", stone.index(), ward.card().id());
                } else {
                    stone.setWard(wardOwner, new ActiveWard(ward.card(), left));
                }
            });
        }
    }

    @Override
    public void onStoneClaimed(EffectContext ctx, StoneState stone) {
        for (PlayerId p : PlayerId.values()) {
            stone.side(p).ward().ifPresent(ward -> {
                stone.setWard(p, null);
                ctx.discard(ward.card());
            });
        }
    }
}
