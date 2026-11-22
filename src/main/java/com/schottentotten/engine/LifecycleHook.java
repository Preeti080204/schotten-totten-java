package com.schottentotten.engine;

import com.schottentotten.model.PlayerId;
import com.schottentotten.model.StoneState;

/**
 * Optional callbacks for effects with a duration (for example Stone Ward). Effects registered in
 * the {@link EffectRegistry} that implement this interface are notified by the engine, so
 * duration rules never need to be hard-coded in the engine.
 */
public interface LifecycleHook {

    /**
     * Called at the end of each turn, after drawing.
     *
     * @param ctx           context whose actor is the player ending the turn
     * @param endingPlayer  the player who just ended the turn
     */
    default void onTurnEnd(EffectContext ctx, PlayerId endingPlayer) {
    }

    /**
     * Called right after a Stone has been claimed.
     *
     * @param ctx   context whose actor is the claimant
     * @param stone the claimed Stone
     */
    default void onStoneClaimed(EffectContext ctx, StoneState stone) {
    }
}
