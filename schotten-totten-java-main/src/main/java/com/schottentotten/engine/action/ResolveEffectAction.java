package com.schottentotten.engine.action;

import com.schottentotten.engine.EffectParameters;

import java.util.Objects;

/**
 * Completes a pending multi-step effect (Recruiter return, Highland Scout ordering).
 *
 * @param parameters the decision
 */
public record ResolveEffectAction(EffectParameters parameters) implements Action {

    /**
     * Validates the components.
     *
     * @param parameters decision parameters
     */
    public ResolveEffectAction {
        Objects.requireNonNull(parameters, "parameters");
    }

    @Override
    public String describe() {
        return "resolve " + parameters.describe();
    }
}
