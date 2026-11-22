package com.schottentotten.ai;

import com.schottentotten.engine.GameEngine;
import com.schottentotten.model.Difficulty;

/**
 * Creates the strategy matching a difficulty level.
 */
public final class StrategyFactory {

    private StrategyFactory() {
    }

    /**
     * Creates a strategy.
     *
     * @param difficulty difficulty level
     * @param engine     rules engine
     * @param seed       random seed for reproducible decisions
     * @return the strategy
     */
    public static PlayerStrategy create(Difficulty difficulty, GameEngine engine, long seed) {
        return switch (difficulty) {
            case EASY -> new SimpleStrategy(engine, seed);
            case HARD -> new AdvancedStrategy(engine, seed);
        };
    }
}
