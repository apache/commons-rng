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

package org.apache.commons.rng.internal;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Base class with default implementation for common methods.
 */
public abstract class BaseProvider
    implements UniformRandomProvider {
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
    public String toString() {
        return getClass().getName();
    }

    /**
     * Gets the instance's state.
     *
     * @return the current state. The given argument can then be passed
     * to {@link #setState(byte[])} in order to recover the
     * current state.
     */
    public byte[] getState() {
        return getStateInternal();
    }

    /**
     * Sets the instance's state.
     *
     * @param state State. The given argument must have been retrieved
     * by a call to {@link #getState()}.
     */
    public void setState(byte[] state) {
        setStateInternal(state);
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
}
