package com.schottentotten.engine;

import com.schottentotten.model.PendingEffect;

import java.util.List;

/**
 * An effect that needs a second decision after it has been applied (for example Recruiter:
 * draw first, then choose which cards to return). {@link CardEffect#apply} stores a
 * {@link PendingEffect} in the state; the engine then waits for a
 * {@link com.schottentotten.engine.action.ResolveEffectAction}.
 */
public interface MultiStepEffect extends CardEffect {

    /**
     * Checks a resolution decision without modifying the state.
     *
     * @param ctx     effect context
     * @param pending the pending effect
     * @param params  the decision
     * @throws IllegalActionException if the decision is illegal
     */
    void validateResolution(EffectContext ctx, PendingEffect pending, EffectParameters params)
            throws IllegalActionException;

    /**
     * Applies a validated resolution decision.
     *
     * @param ctx     effect context
     * @param pending the pending effect
     * @param params  the decision
     */
    void resolve(EffectContext ctx, PendingEffect pending, EffectParameters params);

    /**
     * Lists every legal resolution decision.
     *
     * @param ctx     effect context
     * @param pending the pending effect
     * @return legal decisions
     */
    List<EffectParameters> enumerateResolutions(EffectContext ctx, PendingEffect pending);

    /**
     * Returns the instruction shown while the decision is pending.
     *
     * @param pending the pending effect
     * @return prompt text
     */
    String resolutionPrompt(PendingEffect pending);
}
