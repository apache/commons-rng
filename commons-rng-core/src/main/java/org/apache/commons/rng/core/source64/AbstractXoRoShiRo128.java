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

import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;

/**
 * This abstract class is a base for algorithms from the Xor-Shift-Rotate family of 64-bit
 * generators with 128-bits of state.
 *
 * @see <a href="http://xoshiro.di.unimi.it/">xorshiro / xoroshiro generators</a>
 *
 * @since 1.3
 */
abstract class AbstractXoRoShiRo128 extends LongProvider implements LongJumpableUniformRandomProvider {
    /** Size of the state vector. */
    private static final int SEED_SIZE = 2;
    /** The coefficients for the jump function. */
    private static final long[] JUMP_COEFFICIENTS = {
        0xdf900294d8f554a5L, 0x170865df4b3201fcL
    };
    /** The coefficients for the long jump function. */
    private static final long[] LONG_JUMP_COEFFICIENTS = {
        0xd2a98b26625eee7bL, 0xdddf9b1090aa7ac1L
    };

    // State is maintained using variables rather than an array for performance

    /** State 0 of the generator. */
    protected long state0;
    /** State 1 of the generator. */
    protected long state1;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 2, only the first 2 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros will create a non-functional generator.
     */
    AbstractXoRoShiRo128(long[] seed) {
        if (seed.length < SEED_SIZE) {
            final long[] state = new long[SEED_SIZE];
            fillState(state, seed);
            setState(state);
        } else {
            setState(seed);
        }
    }

    /**
     * Creates a new instance using a 2 element seed.
     * A seed containing all zeros will create a non-functional generator.
     *
     * @param seed0 Initial seed element 0.
     * @param seed1 Initial seed element 1.
     */
    AbstractXoRoShiRo128(long seed0, long seed1) {
        state0 = seed0;
        state1 = seed1;
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected AbstractXoRoShiRo128(AbstractXoRoShiRo128 source) {
        super(source);
        state0 = source.state0;
        state1 = source.state1;
    }

    /**
     * Copies the state from the array into the generator state.
     *
     * @param state the new state
     */
    private void setState(long[] state) {
        state0 = state[0];
        state1 = state[1];
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(new long[] {state0, state1}),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, SEED_SIZE * 8);

        setState(NumberFactory.makeLongArray(c[0]));

        super.setStateInternal(c[1]);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of 2<sup>64</sup>
     * calls to {@link UniformRandomProvider#nextLong() nextLong()}. It can provide
     * up to 2<sup>64</sup> non-overlapping subsequences.</p>
     */
    @Override
    public UniformRandomProvider jump() {
        final UniformRandomProvider copy = copy();
        performJump(JUMP_COEFFICIENTS);
        return copy;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of 2<sup>96</sup> calls to
     * {@link UniformRandomProvider#nextLong() nextLong()}. It can provide up to
     * 2<sup>32</sup> non-overlapping subsequences of length 2<sup>96</sup>; each
     * subsequence can provide up to 2<sup>32</sup> non-overlapping subsequences of
     * length 2<sup>64</sup>using the {@link #jump()} method.</p>
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        final JumpableUniformRandomProvider copy = copy();
        performJump(LONG_JUMP_COEFFICIENTS);
        return copy;
    }

    /**
     * Create a copy.
     *
     * @return the copy
     */
    protected abstract AbstractXoRoShiRo128 copy();

    /**
     * Perform the jump to advance the generator state. Resets the cached state of the generator.
     *
     * @param jumpCoefficients Jump coefficients.
     */
    private void performJump(long[] jumpCoefficients) {
        long s0 = 0;
        long s1 = 0;
        for (final long jc : jumpCoefficients) {
            for (int b = 0; b < 64; b++) {
                if ((jc & (1L << b)) != 0) {
                    s0 ^= state0;
                    s1 ^= state1;
                }
                next();
            }
        }
        state0 = s0;
        state1 = s1;
        resetCachedState();
    }
}
