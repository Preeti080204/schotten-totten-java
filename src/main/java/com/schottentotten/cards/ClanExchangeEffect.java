package com.schottentotten.cards;

import com.schottentotten.engine.CardEffect;
import com.schottentotten.engine.EffectContext;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.GameEventType;
import com.schottentotten.engine.IllegalActionException;
import com.schottentotten.engine.ParameterField;
import com.schottentotten.model.Card;
import com.schottentotten.model.StoneState;

import java.util.ArrayList;
import java.util.List;

/**
 * Clan Exchange (custom expansion): swap two of your own formation cards located on two
 * different unclaimed Stones. Card counts do not change, so capacity and completion times are
 * preserved.
 */
public final class ClanExchangeEffect implements CardEffect {

    /** Effect identifier. */
    public static final String ID = "expansion.clan_exchange";

    /** Creates the effect; it holds no state and can be shared. */
    public ClanExchangeEffect() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void validate(EffectContext ctx, Card card, EffectParameters params) throws IllegalActionException {
        StoneState first = ctx.requireCardOnUnclaimedStone(params.targetCardId(), ctx.actor());
        StoneState second = ctx.requireCardOnUnclaimedStone(params.secondCardId(), ctx.actor());
        if (first.index() == second.index()) {
            throw new IllegalActionException("Clan Exchange needs cards on two different Stones.");
        }
    }

    @Override
    public void apply(EffectContext ctx, Card card, EffectParameters params) {
        StoneState first = ctx.state().board().stoneHolding(params.targetCardId()).orElseThrow();
        StoneState second = ctx.state().board().stoneHolding(params.secondCardId()).orElseThrow();
        Card a = ctx.cardOn(first, ctx.actor(), params.targetCardId());
        Card b = ctx.cardOn(second, ctx.actor(), params.secondCardId());
        first.replaceCard(ctx.actor(), a.id(), b);
        second.replaceCard(ctx.actor(), b.id(), a);
        ctx.discard(card);
        ctx.emit(GameEventType.CARD_MOVED, ctx.name(ctx.actor()) + " plays Clan Exchange: " + a.displayName()
                + " (Stone " + (first.index() + 1) + ") and " + b.displayName() + " (Stone " + (second.index() + 1)
                + ") swap places.", first.index(), a.id());
    }

    @Override
    public List<EffectParameters> enumerate(EffectContext ctx, Card card) {
        List<EffectParameters> result = new ArrayList<>();
        List<StoneState> stones = ctx.state().board().stones();
        for (int i = 0; i < stones.size(); i++) {
            StoneState s1 = stones.get(i);
            if (s1.isClaimed()) {
                continue;
            }
            for (int j = i + 1; j < stones.size(); j++) {
                StoneState s2 = stones.get(j);
                if (s2.isClaimed()) {
                    continue;
                }
                for (Card a : s1.side(ctx.actor()).cards()) {
                    for (Card b : s2.side(ctx.actor()).cards()) {
                        result.add(EffectParameters.swap(a.id(), b.id()));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String prompt(ParameterField field) {
        return field == ParameterField.TARGET_CARD
                ? "Clan Exchange: choose one of your cards on an unclaimed Stone"
                : "Clan Exchange: choose one of your cards on another unclaimed Stone";
    }
}
