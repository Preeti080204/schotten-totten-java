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
import com.schottentotten.model.TacticCard;

import java.util.ArrayList;
import java.util.List;

/**
 * Places a formation card (Clan card or Elite Troop) on the player's side of an unclaimed Stone.
 *
 * <p>Rules enforced: the Stone must be unclaimed, the side must have room (3 cards, 4 under Mud
 * Fight), and a player who has already played a Joker cannot play the second Joker.</p>
 */
public final class FormationPlacementEffect implements CardEffect {

    /** Creates the effect; it holds no state and can be shared. */
    public FormationPlacementEffect() {
    }

    @Override
    public String id() {
        return ClanCard.EFFECT_ID;
    }

    @Override
    public void validate(EffectContext ctx, Card card, EffectParameters params) throws IllegalActionException {
        if (!card.isFormationCard()) {
            throw new IllegalActionException(card.displayName() + " is not a formation card.");
        }
        StoneState stone = ctx.requireStone(params.stone());
        if (stone.isClaimed()) {
            throw new IllegalActionException("Stone " + (stone.index() + 1) + " is already claimed.");
        }
        if (!stone.hasRoom(ctx.actor())) {
            throw new IllegalActionException("Your side of Stone " + (stone.index() + 1) + " is full ("
                    + stone.requiredSize() + " cards).");
        }
        if (isJoker(card) && ctx.state().hasPlayedJoker(ctx.actor())) {
            throw new IllegalActionException("You have already played a Joker; the second one must stay in your hand.");
        }
    }

    @Override
    public void apply(EffectContext ctx, Card card, EffectParameters params) {
        StoneState stone = ctx.state().board().stone(params.stone());
        if (isJoker(card)) {
            ctx.state().markJokerPlayed(ctx.actor());
        }
        stone.addCard(ctx.actor(), card, ctx.state().sequence());
        ctx.emit(GameEventType.CARD_PLACED, ctx.name(ctx.actor()) + " plays " + card.displayName()
                + " on Stone " + (stone.index() + 1) + ".", stone.index(), card.id());
    }

    @Override
    public List<EffectParameters> enumerate(EffectContext ctx, Card card) {
        List<EffectParameters> result = new ArrayList<>();
        if (!card.isFormationCard() || (isJoker(card) && ctx.state().hasPlayedJoker(ctx.actor()))) {
            return result;
        }
        for (StoneState stone : ctx.state().board().stones()) {
            if (stone.hasRoom(ctx.actor())) {
                result.add(EffectParameters.onStone(stone.index()));
            }
        }
        return result;
    }

    @Override
    public String prompt(ParameterField field) {
        return "Choose a Stone with room on your side";
    }

    private static boolean isJoker(Card card) {
        return card instanceof TacticCard t && t.isJoker();
    }
}
