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

import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.RandomProviderState;

/**
 * Base class with default implementation for common methods.
 */
public abstract class BaseProvider
    implements RestorableUniformRandomProvider {
    /** {@inheritDoc} */
    @Override
    public int nextInt(int n) {
        checkStrictlyPositive(n);

        if ((n & -n) == n) {
            return (int) ((n * (long) (nextInt() >>> 1)) >> 31);
        }
        int bits;
        int val;
        do {
            bits = nextInt() >>> 1;
            val = bits % n;
        } while (bits - val + (n - 1) < 0);

        return val;
    }

    /** {@inheritDoc} */
    @Override
    public long nextLong(long n) {
        checkStrictlyPositive(n);

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
     * Creates a snapshot of the RNG state.
     *
     * @return the internal state.
     * @throws UnsupportedOperationException if not implemented.
     */
    protected byte[] getStateInternal() {
        throw new UnsupportedOperationException();
    }

    /**
     * Resets the RNG to the given {@code state}.
     *
     * @param state State (previously obtained by a call to
     * {@link #getStateInternal()}).
     * @throws UnsupportedOperationException if not implemented.
     *
     * @see #checkStateSize(byte[],int)
     */
    protected void setStateInternal(byte[] state) {
        throw new UnsupportedOperationException();
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
     * @throws IllegalArgumentException if {@code state.length != expected}.
     */
    protected void checkStateSize(byte[] state,
                                  int expected) {
        if (state.length != expected) {
            throw new IllegalArgumentException("State size must be " + expected +
                                               " but was " + state.length);
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
     * Checks that the argument is strictly positive.
     *
     * @param n Number to check.
     * @throws IllegalArgumentException if {@code n <= 0}.
     */
    private void checkStrictlyPositive(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Must be strictly positive: " + n);
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
}
