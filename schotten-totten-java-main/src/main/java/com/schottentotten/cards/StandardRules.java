package com.schottentotten.cards;

import com.schottentotten.engine.EffectRegistry;
import com.schottentotten.engine.GameEngine;
import com.schottentotten.model.CardCatalog;
import com.schottentotten.model.TacticType;

/**
 * Factory wiring the official cards and the expansion pack into an engine. This is the single
 * composition point between card implementations and the generic engine.
 */
public final class StandardRules {

    private StandardRules() {
    }

    /**
     * Creates a registry with every official and expansion effect.
     *
     * @return effect registry
     */
    public static EffectRegistry createRegistry() {
        EffectRegistry registry = new EffectRegistry()
                .register(new FormationPlacementEffect())
                .register(new CombatModeEffect(TacticType.BLIND_MANS_BLUFF))
                .register(new CombatModeEffect(TacticType.MUD_FIGHT))
                .register(new RecruiterEffect())
                .register(new StrategistEffect())
                .register(new BansheeEffect())
                .register(new TraitorEffect());
        ExpansionCatalog.entries().forEach(e -> registry.register(e.effect().get()));
        return registry;
    }

    /**
     * Creates the card catalogue including the expansion cards.
     *
     * @return catalogue
     */
    public static CardCatalog createCatalog() {
        return new CardCatalog(ExpansionCatalog.definitions());
    }

    /**
     * Creates a fully wired engine.
     *
     * @return engine
     */
    public static GameEngine createEngine() {
        return new GameEngine(createRegistry(), createCatalog());
    }
}
