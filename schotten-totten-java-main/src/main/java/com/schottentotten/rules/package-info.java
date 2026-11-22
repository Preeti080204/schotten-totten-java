/**
 * Pure rule logic with no side effects: formation strength
 * ({@link com.schottentotten.rules.FormationEvaluator}), claim validation including the proof
 * of early claims from public cards ({@link com.schottentotten.rules.ClaimResolver}), victory
 * detection ({@link com.schottentotten.rules.VictoryChecker}) and the variant-dependent limits
 * ({@link com.schottentotten.rules.RuleSet}).
 *
 * <p>Every class here can be used on a copy of the state, which is how the AI and the views
 * test hypotheses without changing the game.</p>
 */
package com.schottentotten.rules;
