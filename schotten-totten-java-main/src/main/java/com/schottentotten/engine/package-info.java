/**
 * The game engine: the turn state machine, action validation and the extensible card-effect
 * system.
 *
 * <p>{@link com.schottentotten.engine.GameEngine} is the single entry point that changes a game.
 * It validates an {@link com.schottentotten.engine.action.Action} completely before applying
 * it, and reports what happened as {@link com.schottentotten.engine.GameEvent}s. Card
 * behaviour is looked up in the {@link com.schottentotten.engine.EffectRegistry}: the engine
 * never names a particular card, so new Tactic or expansion cards are added by registering a
 * new {@link com.schottentotten.engine.CardEffect} (or
 * {@link com.schottentotten.engine.MultiStepEffect} for effects that need a second
 * decision).</p>
 *
 * <p>The engine has no dependency on JavaFX, on the command line or on the preferences.</p>
 */
package com.schottentotten.engine;
