package com.schottentotten.cards;

import com.schottentotten.engine.CardEffect;
import com.schottentotten.engine.EffectContext;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.GameEventType;
import com.schottentotten.engine.IllegalActionException;
import com.schottentotten.engine.ParameterField;
import com.schottentotten.model.Card;
import com.schottentotten.model.StoneState;
import com.schottentotten.model.TacticCard;
import com.schottentotten.model.TacticCategory;
import com.schottentotten.model.TacticType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Plays a Combat Mode (Blind-Man's Bluff or Mud Fight) on the tile of an unclaimed Stone. The
 * mode's consequences (sum-only comparison, four-card formations) are read by
 * {@link StoneState#isSumOnly()} and {@link StoneState#requiredSize()}.
 */
public final class CombatModeEffect implements CardEffect {

    private final TacticType mode;

    /**
     * Creates the effect for one combat mode.
     *
     * @param mode a {@link TacticCategory#COMBAT_MODE} tactic type
     */
    public CombatModeEffect(TacticType mode) {
        this.mode = Objects.requireNonNull(mode);
        if (mode.category() != TacticCategory.COMBAT_MODE) {
            throw new IllegalArgumentException(mode + " is not a combat mode");
        }
    }

    @Override
    public String id() {
        return mode.effectId();
    }

    @Override
    public void validate(EffectContext ctx, Card card, EffectParameters params) throws IllegalActionException {
        StoneState stone = ctx.requireStone(params.stone());
        if (stone.isClaimed()) {
            throw new IllegalActionException("Stone " + (stone.index() + 1) + " is already claimed.");
        }
        if (stone.hasCombatMode(mode)) {
            throw new IllegalActionException(mode.displayName() + " is already on Stone " + (stone.index() + 1) + ".");
        }
    }

    @Override
    public void apply(EffectContext ctx, Card card, EffectParameters params) {
        StoneState stone = ctx.state().board().stone(params.stone());
        stone.addCombatMode((TacticCard) card);
        ctx.emit(GameEventType.COMBAT_MODE_PLAYED, ctx.name(ctx.actor()) + " plays " + mode.displayName()
                + " on Stone " + (stone.index() + 1) + ".", stone.index(), card.id());
    }

    @Override
    public List<EffectParameters> enumerate(EffectContext ctx, Card card) {
        List<EffectParameters> result = new ArrayList<>();
        for (StoneState stone : ctx.state().board().stones()) {
            if (!stone.isClaimed() && !stone.hasCombatMode(mode)) {
                result.add(EffectParameters.onStone(stone.index()));
            }
        }
        return result;
    }

    @Override
    public String prompt(ParameterField field) {
        return "Choose an unclaimed Stone for " + mode.displayName();
    }
}
