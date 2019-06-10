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
 * generators with 256-bits of state.
 *
 * @see <a href="http://xoshiro.di.unimi.it/">xorshiro / xoroshiro generators</a>
 *
 * @since 1.3
 */
abstract class AbstractXoShiRo256 extends LongProvider implements LongJumpableUniformRandomProvider {
    /** Size of the state vector. */
    private static final int SEED_SIZE = 4;
    /** The coefficients for the jump function. */
    private static final long[] JUMP_COEFFICIENTS = {
        0x180ec6d33cfd0abaL, 0xd5a61266f0c9392cL, 0xa9582618e03fc9aaL, 0x39abdc4529b1661cL
    };
    /** The coefficients for the long jump function. */
    private static final long[] LONG_JUMP_COEFFICIENTS = {
        0x76e15d3efefdcbbfL, 0xc5004e441c522fb3L, 0x77710069854ee241L, 0x39109bb02acbe635L
    };

    // State is maintained using variables rather than an array for performance

    /** State 0 of the generator. */
    protected long state0;
    /** State 1 of the generator. */
    protected long state1;
    /** State 2 of the generator. */
    protected long state2;
    /** State 3 of the generator. */
    protected long state3;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 4, only the first 4 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros will create a non-functional generator.
     */
    AbstractXoShiRo256(long[] seed) {
        if (seed.length < SEED_SIZE) {
            final long[] state = new long[SEED_SIZE];
            fillState(state, seed);
            setState(state);
        } else {
            setState(seed);
        }
    }

    /**
     * Creates a new instance using a 4 element seed.
     * A seed containing all zeros will create a non-functional generator.
     *
     * @param seed0 Initial seed element 0.
     * @param seed1 Initial seed element 1.
     * @param seed2 Initial seed element 2.
     * @param seed3 Initial seed element 3.
     */
    AbstractXoShiRo256(long seed0, long seed1, long seed2, long seed3) {
        state0 = seed0;
        state1 = seed1;
        state2 = seed2;
        state3 = seed3;
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected AbstractXoShiRo256(AbstractXoShiRo256 source) {
        super(source);
        state0 = source.state0;
        state1 = source.state1;
        state2 = source.state2;
        state3 = source.state3;
    }

    /**
     * Copies the state from the array into the generator state.
     *
     * @param state the new state
     */
    private void setState(long[] state) {
        state0 = state[0];
        state1 = state[1];
        state2 = state[2];
        state3 = state[3];
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(
                                        new long[] {state0, state1, state2, state3}),
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
     * <p>The jump size is the equivalent of 2<sup>128</sup>
     * calls to {@link UniformRandomProvider#nextLong() nextLong()}. It can provide
     * up to 2<sup>128</sup> non-overlapping subsequences.</p>
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
     * <p>The jump size is the equivalent of 2<sup>192</sup> calls to
     * {@link UniformRandomProvider#nextLong() nextLong()}. It can provide up to
     * 2<sup>64</sup> non-overlapping subsequences of length 2<sup>192</sup>; each
     * subsequence can provide up to 2<sup>64</sup> non-overlapping subsequences of
     * length 2<sup>128</sup>using the {@link #jump()} method.</p>
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
    protected abstract AbstractXoShiRo256 copy();

    /**
     * Perform the jump to advance the generator state. Resets the cached state of the generator.
     *
     * @param jumpCoefficients Jump coefficients.
     */
    private void performJump(long[] jumpCoefficients) {
        long s0 = 0;
        long s1 = 0;
        long s2 = 0;
        long s3 = 0;
        for (final long jc : jumpCoefficients) {
            for (int b = 0; b < 64; b++) {
                if ((jc & (1L << b)) != 0) {
                    s0 ^= state0;
                    s1 ^= state1;
                    s2 ^= state2;
                    s3 ^= state3;
                }
                next();
            }
        }
        state0 = s0;
        state1 = s1;
        state2 = s2;
        state3 = s3;
        resetCachedState();
    }
}
