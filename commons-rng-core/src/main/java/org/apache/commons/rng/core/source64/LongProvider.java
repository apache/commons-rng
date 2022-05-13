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

package org.apache.commons.rng.core.source64;

import org.apache.commons.rng.core.util.NumberFactory;
import org.apache.commons.rng.core.BaseProvider;

/**
 * Base class for all implementations that provide a {@code long}-based
 * source randomness.
 */
public abstract class LongProvider
    extends BaseProvider
    implements RandomLongSource {

    /** Empty boolean source. This is the location of the sign-bit after 63 right shifts on
     * the boolean source. */
    private static final long EMPTY_BOOL_SOURCE = 1;
    /** Empty int source. This requires a negative value as the sign-bit is used to
     * trigger a refill. */
    private static final long EMPTY_INT_SOURCE = -1;

    /**
     * Provides a bit source for booleans.
     *
     * <p>A cached value from a call to {@link #next()}.
     *
     * <p>Only stores 63-bits when full as 1 bit has already been consumed.
     * The sign bit is a flag that shifts down so the source eventually equals 1
     * when all bits are consumed and will trigger a refill.
     */
    private long booleanSource = EMPTY_BOOL_SOURCE;

    /**
     * Provides a source for ints.
     *
     * <p>A cached half-value value from a call to {@link #next()}.
     * The int is stored in the lower 32 bits with zeros in the upper bits.
     * When empty this is set to negative to trigger a refill.
     */
    private long intSource = EMPTY_INT_SOURCE;

    /**
     * Creates a new instance.
     */
    public LongProvider() {
        super();
    }

    /**
     * Creates a new instance copying the state from the source.
     *
     * <p>This provides base functionality to allow a generator to create a copy, for example
     * for use in the {@link org.apache.commons.rng.JumpableUniformRandomProvider
     * JumpableUniformRandomProvider} interface.
     *
     * @param source Source to copy.
     * @since 1.3
     */
    protected LongProvider(LongProvider source) {
        booleanSource = source.booleanSource;
        intSource = source.intSource;
    }

    /**
     * Reset the cached state used in the default implementation of {@link #nextBoolean()}
     * and {@link #nextInt()}.
     *
     * <p>This should be used when the state is no longer valid, for example after a jump
     * performed for the {@link org.apache.commons.rng.JumpableUniformRandomProvider
     * JumpableUniformRandomProvider} interface.</p>
     *
     * @since 1.3
     */
    protected void resetCachedState() {
        booleanSource = EMPTY_BOOL_SOURCE;
        intSource = EMPTY_INT_SOURCE;
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        final long[] state = {booleanSource, intSource};
        return composeStateInternal(NumberFactory.makeByteArray(state),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, 2 * Long.BYTES);
        final long[] state = NumberFactory.makeLongArray(c[0]);
        booleanSource   = state[0];
        intSource       = state[1];
        super.setStateInternal(c[1]);
    }

    /** {@inheritDoc} */
    @Override
    public long nextLong() {
        return next();
    }

    /** {@inheritDoc} */
    @Override
    public int nextInt() {
        long bits = intSource;
        if (bits < 0) {
            // Refill
            bits = next();
            // Store high 32 bits, return low 32 bits
            intSource = bits >>> 32;
            return (int) bits;
        }
        // Reset and return previous low bits
        intSource = -1;
        return (int) bits;
    }

    /** {@inheritDoc} */
    @Override
    public boolean nextBoolean() {
        long bits = booleanSource;
        if (bits == 1) {
            // Refill
            bits = next();
            // Store a refill flag in the sign bit and the unused 63 bits, return lowest bit
            booleanSource = Long.MIN_VALUE | (bits >>> 1);
            return (bits & 0x1) == 1;
        }
        // Shift down eventually triggering refill, return current lowest bit
        booleanSource = bits >>> 1;
        return (bits & 0x1) == 1;
    }
}
