/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.rng.core;

import java.util.Arrays;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.RandomProviderState;

/**
 * Base class with default implementation for common methods.
 */
public abstract class BaseProvider
    implements RestorableUniformRandomProvider {
    /** Error message when an integer is not positive. */
    private static final String NOT_POSITIVE = "Must be strictly positive: ";
    /** 2^32. */
    private static final long POW_32 = 1L << 32;
    /**
     * The fractional part of the the golden ratio, phi, scaled to 64-bits and rounded to odd.
     * <pre>
     * phi = (sqrt(5) - 1) / 2) * 2^64
     * </pre>
     * @see <a href="https://en.wikipedia.org/wiki/Golden_ratio">Golden ratio</a>
     */
    private static final long GOLDEN_RATIO_64 = 0x9e3779b97f4a7c15L;
    /** The fractional part of the the golden ratio, phi, scaled to 32-bits and rounded to odd. */
    private static final int GOLDEN_RATIO_32 = 0x9e3779b9;

    /** {@inheritDoc} */
    @Override
    public int nextInt(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException(NOT_POSITIVE + n);
        }

        // Lemire (2019): Fast Random Integer Generation in an Interval
        // https://arxiv.org/abs/1805.10941
        long m = (nextInt() & 0xffffffffL) * n;
        long l = m & 0xffffffffL;
        if (l < n) {
            // 2^32 % n
            final long t = POW_32 % n;
            while (l < t) {
                m = (nextInt() & 0xffffffffL) * n;
                l = m & 0xffffffffL;
            }
        }
        return (int) (m >>> 32);
    }

    /** {@inheritDoc} */
    @Override
    public long nextLong(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException(NOT_POSITIVE + n);
        }

        long bits;
        long val;
        do {
            bits = nextLong() >>> 1;
            val  = bits % n;
        } while (bits - val + (n - 1) < 0);

        return val;
    }

    /** {@inheritDoc} */
    @Override
    public RandomProviderState saveState() {
        return new RandomProviderDefaultState(getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    public void restoreState(RandomProviderState state) {
        if (state instanceof RandomProviderDefaultState) {
            setStateInternal(((RandomProviderDefaultState) state).getState());
        } else {
            throw new IllegalArgumentException("Foreign instance");
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getClass().getName();
    }

    /**
     * Combine parent and subclass states.
     * This method must be called by all subclasses in order to ensure
     * that state can be restored in case some of it is stored higher
     * up in the class hierarchy.
     *
     * I.e. the body of the overridden {@link #getStateInternal()},
     * will end with a statement like the following:
     * <pre>
     *  <code>
     *    return composeStateInternal(state,
     *                                super.getStateInternal());
     *  </code>
     * </pre>
     * where {@code state} is the state needed and defined by the class
     * where the method is overridden.
     *
     * @param state State of the calling class.
     * @param parentState State of the calling class' parent.
     * @return the combined state.
     * Bytes that belong to the local state will be stored at the
     * beginning of the resulting array.
     */
    protected byte[] composeStateInternal(byte[] state,
                                          byte[] parentState) {
        final int len = parentState.length + state.length;
        final byte[] c = new byte[len];
        System.arraycopy(state, 0, c, 0, state.length);
        System.arraycopy(parentState, 0, c, state.length, parentState.length);
        return c;
    }

    /**
     * Splits the given {@code state} into a part to be consumed by the caller
     * in order to restore its local state, while the reminder is passed to
     * the parent class.
     *
     * I.e. the body of the overridden {@link #setStateInternal(byte[])},
     * will contain statements like the following:
     * <pre>
     *  <code>
     *    final byte[][] s = splitState(state, localStateLength);
     *    // Use "s[0]" to recover the local state.
     *    super.setStateInternal(s[1]);
     *  </code>
     * </pre>
     * where {@code state} is the combined state of the calling class and of
     * all its parents.
     *
     * @param state State.
     * The local state must be stored at the beginning of the array.
     * @param localStateLength Number of elements that will be consumed by the
     * locally defined state.
     * @return the local state (in slot 0) and the parent state (in slot 1).
     * @throws IllegalStateException if {@code state.length < localStateLength}.
     */
    protected byte[][] splitStateInternal(byte[] state,
                                          int localStateLength) {
        checkStateSize(state, localStateLength);

        final byte[] local = new byte[localStateLength];
        System.arraycopy(state, 0, local, 0, localStateLength);
        final int parentLength = state.length - localStateLength;
        final byte[] parent = new byte[parentLength];
        System.arraycopy(state, localStateLength, parent, 0, parentLength);

        return new byte[][] {local, parent};
    }

    /**
     * Creates a snapshot of the RNG state.
     *
     * @return the internal state.
     */
    protected byte[] getStateInternal() {
        // This class has no state (and is the top-level class that
        // declares this method).
        return new byte[0];
    }

    /**
     * Resets the RNG to the given {@code state}.
     *
     * @param state State (previously obtained by a call to
     * {@link #getStateInternal()}).
     * @throws IllegalStateException if the size of the given array is
     * not consistent with the state defined by this class.
     *
     * @see #checkStateSize(byte[],int)
     */
    protected void setStateInternal(byte[] state) {
        if (state.length != 0) {
            // This class has no state.
            throw new IllegalStateException("State not fully recovered by subclasses");
        }
    }

    /**
     * Simple filling procedure.
     * It will
     * <ol>
     *  <li>
     *   fill the beginning of {@code state} by copying
     *   {@code min(seed.length, state.length)} elements from
     *   {@code seed},
     *  </li>
     *  <li>
     *   set all remaining elements of {@code state} with non-zero
     *   values (even if {@code seed.length < state.length}).
     *  </li>
     * </ol>
     *
     * @param state State. Must be allocated.
     * @param seed Seed. Cannot be null.
     */
    protected void fillState(int[] state,
                             int[] seed) {
        final int stateSize = state.length;
        final int seedSize = seed.length;
        System.arraycopy(seed, 0, state, 0, Math.min(seedSize, stateSize));

        if (seedSize < stateSize) {
            for (int i = seedSize; i < stateSize; i++) {
                state[i] = (int) (scrambleWell(state[i - seed.length], i) & 0xffffffffL);
            }
        }
    }

    /**
     * Simple filling procedure.
     * It will
     * <ol>
     *  <li>
     *   fill the beginning of {@code state} by copying
     *   {@code min(seed.length, state.length)} elements from
     *   {@code seed},
     *  </li>
     *  <li>
     *   set all remaining elements of {@code state} with non-zero
     *   values (even if {@code seed.length < state.length}).
     *  </li>
     * </ol>
     *
     * @param state State. Must be allocated.
     * @param seed Seed. Cannot be null.
     */
    protected void fillState(long[] state,
                             long[] seed) {
        final int stateSize = state.length;
        final int seedSize = seed.length;
        System.arraycopy(seed, 0, state, 0, Math.min(seedSize, stateSize));

        if (seedSize < stateSize) {
            for (int i = seedSize; i < stateSize; i++) {
                state[i] = scrambleWell(state[i - seed.length], i);
            }
        }
    }

    /**
     * Checks that the {@code state} has the {@code expected} size.
     *
     * @param state State.
     * @param expected Expected length of {@code state} array.
     * @throws IllegalStateException if {@code state.length < expected}.
     * @deprecated Method is used internally and should be made private in
     * some future release.
     */
    @Deprecated
    protected void checkStateSize(byte[] state,
                                  int expected) {
        if (state.length < expected) {
            throw new IllegalStateException("State size must be larger than " +
                                            expected + " but was " + state.length);
        }
    }

    /**
     * Checks whether {@code index} is in the range {@code [min, max]}.
     *
     * @param min Lower bound.
     * @param max Upper bound.
     * @param index Value that must lie within the {@code [min, max]} interval.
     * @throws IndexOutOfBoundsException if {@code index} is not within the
     * {@code [min, max]} interval.
     */
    protected void checkIndex(int min,
                              int max,
                              int index) {
        if (index < min ||
            index > max) {
            throw new IndexOutOfBoundsException(index + " is out of interval [" +
                                                min + ", " +
                                                max + "]");
        }
    }

    /**
     * Transformation used to scramble the initial state of
     * a generator.
     *
     * @param n Seed element.
     * @param mult Multiplier.
     * @param shift Shift.
     * @param add Offset.
     * @return the transformed seed element.
     */
    private static long scramble(long n,
                                 long mult,
                                 int shift,
                                 int add) {
        // Code inspired from "AbstractWell" class.
        return mult * (n ^ (n >> shift)) + add;
    }

    /**
     * Transformation used to scramble the initial state of
     * a generator.
     *
     * @param n Seed element.
     * @param add Offset.
     * @return the transformed seed element.
     * @see #scramble(long,long,int,int)
     */
    private static long scrambleWell(long n,
                                     int add) {
        // Code inspired from "AbstractWell" class.
        return scramble(n, 1812433253L, 30, add);
    }

    /**
     * Extend the seed to the specified minimum length. If the seed is equal or greater than the
     * minimum length, return the same seed unchanged. Otherwise:
     * <ol>
     *  <li>Create a new array of the specified length
     *  <li>Copy all elements of the seed into the array
     *  <li>Fill the remaining values. The additional values will have at most one occurrence
     *   of zero. If the original seed is all zero, the first extended value will be non-zero.
     *  </li>
     * </ol>
     *
     * <p>This method can be used in constructors that must pass their seed to the super class
     * to avoid a duplication of seed expansion to the minimum length required by the super class
     * and the class:
     * <pre>
     * public RNG extends AnotherRNG {
     *     public RNG(long[] seed) {
     *         super(seed = extendSeed(seed, SEED_SIZE));
     *         // Use seed for additional state ...
     *     }
     * }
     * </pre>
     *
     * <p>Note using the state filling procedure provided in {@link #fillState(long[], long[])}
     * is not possible as it is an instance method. Calling a seed extension routine must use a
     * static method.
     *
     * <p>This method functions as if the seed has been extended using a
     * {@link org.apache.commons.rng.core.source64.SplitMix64 SplitMix64}
     * generator seeded with {@code seed[0]}, or zero if the input seed length is zero.
     * <pre>
     * if (seed.length &lt; length) {
     *     final long[] s = Arrays.copyOf(seed, length);
     *     final SplitMix64 rng = new SplitMix64(s[0]);
     *     for (int i = seed.length; i &lt; length; i++) {
     *         s[i] = rng.nextLong();
     *     }
     *     return s;
     * }</pre>
     *
     * @param seed Input seed
     * @param length The minimum length
     * @return the seed
     */
    protected static long[] extendSeed(long[] seed, int length) {
        if (seed.length < length) {
            final long[] s = Arrays.copyOf(seed, length);
            // Fill the rest as if using a SplitMix64 RNG
            long x = s[0];
            for (int i = seed.length; i < length; i++) {
                s[i] = stafford13(x += GOLDEN_RATIO_64);
            }
            return s;
        }
        return seed;
    }

    /**
     * Extend the seed to the specified minimum length. If the seed is equal or greater than the
     * minimum length, return the same seed unchanged. Otherwise:
     * <ol>
     *  <li>Create a new array of the specified length
     *  <li>Copy all elements of the seed into the array
     *  <li>Fill the remaining values. The additional values will have at most one occurrence
     *   of zero. If the original seed is all zero, the first extended value will be non-zero.
     *  </li>
     * </ol>
     *
     * <p>This method can be used in constructors that must pass their seed to the super class
     * to avoid a duplication of seed expansion to the minimum length required by the super class
     * and the class:
     * <pre>
     * public RNG extends AnotherRNG {
     *     public RNG(int[] seed) {
     *         super(seed = extendSeed(seed, SEED_SIZE));
     *         // Use seed for additional state ...
     *     }
     * }
     * </pre>
     *
     * <p>Note using the state filling procedure provided in {@link #fillState(int[], int[])}
     * is not possible as it is an instance method. Calling a seed extension routine must use a
     * static method.
     *
     * <p>This method functions as if the seed has been extended using a
     * {@link org.apache.commons.rng.core.source64.SplitMix64 SplitMix64}-style 32-bit
     * generator seeded with {@code seed[0]}, or zero if the input seed length is zero. The
     * generator uses the 32-bit mixing function from MurmurHash3.
     *
     * @param seed Input seed
     * @param length The minimum length
     * @return the seed
     */
    protected static int[] extendSeed(int[] seed, int length) {
        if (seed.length < length) {
            final int[] s = Arrays.copyOf(seed, length);
            // Fill the rest as if using a SplitMix64-style RNG for 32-bit output
            int x = s[0];
            for (int i = seed.length; i < length; i++) {
                s[i] = murmur3(x += GOLDEN_RATIO_32);
            }
            return s;
        }
        return seed;
    }

    /**
     * Perform variant 13 of David Stafford's 64-bit mix function.
     * This is the mix function used in the {@link SplitMix64} RNG.
     *
     * <p>This is ranked first of the top 14 Stafford mixers.
     *
     * @param x the input value
     * @return the output value
     * @see <a href="http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html">Better
     *      Bit Mixing - Improving on MurmurHash3&#39;s 64-bit Finalizer.</a>
     */
    private static long stafford13(long x) {
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }

    /**
     * Perform the finalising 32-bit mix function of Austin Appleby's MurmurHash3.
     *
     * @param x the input value
     * @return the output value
     * @see <a href="https://github.com/aappleby/smhasher">SMHasher</a>
     */
    private static int murmur3(int x) {
        x = (x ^ (x >>> 16)) * 0x85ebca6b;
        x = (x ^ (x >>> 13)) * 0xc2b2ae35;
        return x ^ (x >>> 16);
    }
}
