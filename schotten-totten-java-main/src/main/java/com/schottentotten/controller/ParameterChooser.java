package com.schottentotten.controller;

import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.ParameterField;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Narrows the legal parameter sets of a card effect one field at a time. Both views use it to
 * ask the player only for the choices that actually matter (for example a Clan card only
 * needs a Stone, while the Traitor needs a card and then a destination). Because it only works
 * on the engine's legal parameter lists, any new card effect is supported automatically.
 */
public final class ParameterChooser {

    private List<EffectParameters> remaining;

    /**
     * Starts a selection.
     *
     * @param options legal parameter sets (must not be empty)
     */
    public ParameterChooser(List<EffectParameters> options) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("No legal option");
        }
        this.remaining = new ArrayList<>(options);
    }

    /**
     * Tells whether exactly one parameter set remains.
     *
     * @return {@code true} when the choice is complete
     */
    public boolean isResolved() {
        return remaining.size() == 1;
    }

    /**
     * Returns the next field the player must choose, i.e. the first field whose value still
     * differs between the remaining options.
     *
     * @return the field, or empty when resolved
     */
    public Optional<ParameterField> nextField() {
        if (isResolved()) {
            return Optional.empty();
        }
        for (ParameterField field : ParameterField.values()) {
            if (values(field).size() > 1) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the distinct values of a field among the remaining options, in order.
     *
     * @param field the field
     * @return distinct values (may contain {@code null})
     */
    public List<Object> values(ParameterField field) {
        LinkedHashSet<Object> set = new LinkedHashSet<>();
        for (EffectParameters p : remaining) {
            set.add(field.read(p));
        }
        return new ArrayList<>(set);
    }

    /**
     * Keeps only the options whose field equals the chosen value.
     *
     * @param field the field
     * @param value the chosen value
     * @throws IllegalArgumentException if no option has that value
     */
    public void choose(ParameterField field, Object value) {
        List<EffectParameters> filtered = remaining.stream()
                .filter(p -> Objects.equals(field.read(p), value)).toList();
        if (filtered.isEmpty()) {
            throw new IllegalArgumentException("That choice is not allowed.");
        }
        remaining = new ArrayList<>(filtered);
    }

    /**
     * Returns the remaining options.
     *
     * @return unmodifiable copy
     */
    public List<EffectParameters> remaining() {
        return List.copyOf(remaining);
    }

    /**
     * Returns the selected parameters once resolved (or the first remaining option when the
     * remaining options are equivalent).
     *
     * @return parameters
     */
    public EffectParameters result() {
        return remaining.get(0);
    }
}
