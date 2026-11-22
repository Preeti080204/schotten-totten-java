package com.schottentotten.util;

import java.util.List;

/**
 * A small deterministic pseudo-random generator (SplitMix64) whose whole state is one
 * {@code long}. The state can be saved and restored, so a game resumed from a save file
 * continues with exactly the same random sequence.
 *
 * <p>{@link java.util.Random} is not used because its internal state cannot be read back.</p>
 */
public final class SeededRandom {

    private long state;

    /**
     * Creates a generator from a seed.
     *
     * @param seed initial state
     */
    public SeededRandom(long seed) {
        this.state = seed;
    }

    /**
     * Returns the current internal state (to persist it).
     *
     * @return state value
     */
    public long state() {
        return state;
    }

    /**
     * Returns the next 64-bit value.
     *
     * @return pseudo-random long
     */
    public long nextLong() {
        state += 0x9E3779B97F4A7C15L;
        long z = state;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * Returns a uniformly distributed value in {@code [0, bound)}.
     *
     * @param bound exclusive upper bound, must be positive
     * @return pseudo-random int
     */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        return (int) Long.remainderUnsigned(nextLong(), bound);
    }

    /**
     * Returns a value in {@code [0, 1)}.
     *
     * @return pseudo-random double
     */
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    /**
     * Shuffles a list in place with the Fisher-Yates algorithm.
     *
     * @param list list to shuffle
     * @param <T>  element type
     */
    public <T> void shuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }

    /**
     * Returns an independent copy with the same state.
     *
     * @return copy
     */
    public SeededRandom copy() {
        return new SeededRandom(state);
    }
}
