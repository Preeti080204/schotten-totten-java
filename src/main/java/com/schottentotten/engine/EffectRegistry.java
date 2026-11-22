package com.schottentotten.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maps effect identifiers to {@link CardEffect} implementations. New cards are added by
 * registering a new effect; the engine does not change (Open/Closed principle).
 */
public final class EffectRegistry {

    private final Map<String, CardEffect> effects = new LinkedHashMap<>();

    /** Creates an empty registry. */
    public EffectRegistry() {
    }

    /**
     * Registers an effect.
     *
     * @param effect the effect
     * @return this registry, for chaining
     * @throws IllegalArgumentException if an effect with the same id is already registered
     */
    public EffectRegistry register(CardEffect effect) {
        if (effects.putIfAbsent(effect.id(), effect) != null) {
            throw new IllegalArgumentException("Duplicate effect id: " + effect.id());
        }
        return this;
    }

    /**
     * Finds an effect.
     *
     * @param id effect identifier
     * @return the effect, if registered
     */
    public Optional<CardEffect> find(String id) {
        return Optional.ofNullable(effects.get(id));
    }

    /**
     * Returns the effect registered under an id.
     *
     * @param id effect identifier
     * @return the effect
     * @throws IllegalStateException if none is registered
     */
    public CardEffect get(String id) {
        CardEffect e = effects.get(id);
        if (e == null) {
            throw new IllegalStateException("No effect registered for " + id);
        }
        return e;
    }

    /**
     * Returns the registered effects that observe the game lifecycle.
     *
     * @return lifecycle hooks
     */
    public List<LifecycleHook> hooks() {
        List<LifecycleHook> hooks = new ArrayList<>();
        for (CardEffect e : effects.values()) {
            if (e instanceof LifecycleHook h) {
                hooks.add(h);
            }
        }
        return hooks;
    }
}
