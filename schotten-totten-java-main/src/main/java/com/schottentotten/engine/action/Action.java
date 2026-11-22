package com.schottentotten.engine.action;

/**
 * A command submitted to the engine by a view or by a virtual player. Actions are immutable
 * values; they carry no behaviour and are validated by the engine before any state change.
 * Humans and virtual players use exactly the same actions.
 */
public sealed interface Action
        permits PlayCardAction, PassAction, ClaimAction, EndTurnAction, ResolveEffectAction, NextRoundAction {

    /**
     * Returns a short description for logs and the CLI.
     *
     * @return description
     */
    String describe();
}
