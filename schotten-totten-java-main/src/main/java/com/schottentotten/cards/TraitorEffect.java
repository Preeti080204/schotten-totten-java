package com.schottentotten.cards;

import com.schottentotten.engine.CardEffect;
import com.schottentotten.engine.EffectContext;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.GameEventType;
import com.schottentotten.engine.IllegalActionException;
import com.schottentotten.engine.ParameterField;
import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.StoneState;
import com.schottentotten.model.TacticType;

import java.util.ArrayList;
import java.util.List;

/**
 * Traitor (printed "Traiter" in the rulebook): take a <em>Clan</em> card from the opponent's
 * side of an unclaimed Stone and place it on your side of an unclaimed Stone with room (the same
 * Stone is allowed). Sides protected by a Stone Ward cannot be targeted.
 */
public final class TraitorEffect implements CardEffect {

    /** Creates the effect; it holds no state and can be shared. */
    public TraitorEffect() {
    }

    @Override
    public String id() {
        return TacticType.TRAITOR.effectId();
    }

    @Override
    public void validate(EffectContext ctx, Card card, EffectParameters params) throws IllegalActionException {
        StoneState source = ctx.requireCardOnUnclaimedStone(params.targetCardId(), ctx.opponent());
        if (!(ctx.cardOn(source, ctx.opponent(), params.targetCardId()) instanceof ClanCard)) {
            throw new IllegalActionException("The Traitor can only take a Clan card, not a Tactic card.");
        }
        if (ctx.isProtected(source, ctx.opponent())) {
            throw new IllegalActionException("That card is protected by a Stone Ward.");
        }
        if (params.destination() == null || params.destination() == EffectParameters.DISCARD) {
            throw new IllegalActionException("Choose an unclaimed Stone on your side for the traitor.");
        }
        StoneState dest = ctx.requireStone(params.destination());
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
        Card stolen = source.removeCard(ctx.opponent(), params.targetCardId());
        StoneState dest = ctx.state().board().stone(params.destination());
        dest.addCard(ctx.actor(), stolen, ctx.state().sequence());
        ctx.discard(card);
        ctx.emit(GameEventType.CARD_MOVED, ctx.name(ctx.actor()) + " plays Traitor: " + stolen.displayName()
                + " defects from Stone " + (source.index() + 1) + " to " + ctx.name(ctx.actor()) + "'s side of Stone "
                + (dest.index() + 1) + ".", dest.index(), stolen.id());
    }

    @Override
    public List<EffectParameters> enumerate(EffectContext ctx, Card card) {
        List<EffectParameters> result = new ArrayList<>();
        List<StoneState> stones = ctx.state().board().stones();
        for (StoneState source : stones) {
            if (source.isClaimed() || ctx.isProtected(source, ctx.opponent())) {
                continue;
            }
            for (Card c : source.side(ctx.opponent()).cards()) {
                if (!(c instanceof ClanCard)) {
                    continue;
                }
                for (StoneState dest : stones) {
                    if (dest.hasRoom(ctx.actor())) {
                        result.add(EffectParameters.move(c.id(), dest.index()));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String prompt(ParameterField field) {
        return field == ParameterField.TARGET_CARD
                ? "Traitor: choose an opponent's Clan card on an unclaimed Stone"
                : "Traitor: choose an unclaimed Stone on your side for the traitor";
    }
}
