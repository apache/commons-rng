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

import java.util.Arrays;

import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;

/**
 * This abstract class is a base for algorithms from the Xor-Shift-Rotate family of 64-bit
 * generators with 1024-bits of state.
 *
 * @see <a href="http://xoshiro.di.unimi.it/">xorshiro / xoroshiro generators</a>
 * @since 1.3
 */
abstract class AbstractXoRoShiRo1024 extends LongProvider implements LongJumpableUniformRandomProvider {
    /** Size of the state vector. */
    private static final int SEED_SIZE = 16;
    /** The coefficients for the jump function. */
    private static final long[] JUMP_COEFFICIENTS = {
        0x931197d8e3177f17L, 0xb59422e0b9138c5fL, 0xf06a6afb49d668bbL, 0xacb8a6412c8a1401L,
        0x12304ec85f0b3468L, 0xb7dfe7079209891eL, 0x405b7eec77d9eb14L, 0x34ead68280c44e4aL,
        0xe0e4ba3e0ac9e366L, 0x8f46eda8348905b7L, 0x328bf4dbad90d6ffL, 0xc8fd6fb31c9effc3L,
        0xe899d452d4b67652L, 0x45f387286ade3205L, 0x03864f454a8920bdL, 0xa68fa28725b1b384L,
    };
    /** The coefficients for the long jump function. */
    private static final long[] LONG_JUMP_COEFFICIENTS = {
        0x7374156360bbf00fL, 0x4630c2efa3b3c1f6L, 0x6654183a892786b1L, 0x94f7bfcbfb0f1661L,
        0x27d8243d3d13eb2dL, 0x9701730f3dfb300fL, 0x2f293baae6f604adL, 0xa661831cb60cd8b6L,
        0x68280c77d9fe008cL, 0x50554160f5ba9459L, 0x2fc20b17ec7b2a9aL, 0x49189bbdc8ec9f8fL,
        0x92a65bca41852cc1L, 0xf46820dd0509c12aL, 0x52b00c35fbf92185L, 0x1e5b3b7f589e03c1L,
    };
    /** State. */
    private final long[] state = new long[SEED_SIZE];
    /** Index in "state" array. */
    private int index;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 16, only the first 16 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros will create a non-functional generator.
     */
    AbstractXoRoShiRo1024(long[] seed) {
        setSeedInternal(seed);
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected AbstractXoRoShiRo1024(AbstractXoRoShiRo1024 source) {
        super(source);
        System.arraycopy(source.state, 0, state, 0, SEED_SIZE);
        index = source.index;
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        final long[] s = Arrays.copyOf(state, SEED_SIZE + 1);
        s[SEED_SIZE] = index;

        return composeStateInternal(NumberFactory.makeByteArray(s),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, (SEED_SIZE + 1) * 8);

        final long[] tmp = NumberFactory.makeLongArray(c[0]);
        System.arraycopy(tmp, 0, state, 0, SEED_SIZE);
        index = (int) tmp[SEED_SIZE];

        super.setStateInternal(c[1]);
    }

    /**
     * Seeds the RNG.
     *
     * @param seed Seed.
     */
    private void setSeedInternal(long[] seed) {
        // Reset the whole state of this RNG (i.e. "state" and "index").
        // Filling procedure is not part of the reference code.
        fillState(state, seed);
        index = 0;
    }

    /** {@inheritDoc} */
    @Override
    public long next() {
        final int q = index;
        index = (index + 1) & 15;
        final long s0 = state[index];
        long s15 = state[q];
        final long result = transform(s0, s15);

        s15 ^= s0;
        state[q] = Long.rotateLeft(s0, 25) ^ s15 ^ (s15 << 27);
        state[index] = Long.rotateLeft(s15, 36);

        return result;
    }

    /**
     * Transform the two consecutive 64-bit states of the generator to a 64-bit output.
     * The transformation function shall vary with respect to different generators.
     *
     * @param s0 The current state.
     * @param s15 The previous state.
     * @return the output
     */
    protected abstract long transform(long s0, long s15);

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of 2<sup>512</sup>
     * calls to {@link UniformRandomProvider#nextLong() nextLong()}. It can provide
     * up to 2<sup>512</sup> non-overlapping subsequences.</p>
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
     *
     * <p>The jump size is the equivalent of 2<sup>768</sup> calls to
     * {@link UniformRandomProvider#nextLong() nextLong()}. It can provide up to
     * 2<sup>256</sup> non-overlapping subsequences of length 2<sup>768</sup>; each
     * subsequence can provide up to 2<sup>256</sup> non-overlapping subsequences of
     * length 2<sup>512</sup> using the {@link #jump()} method.</p>
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
    protected abstract AbstractXoRoShiRo1024 copy();

    /**
     * Perform the jump to advance the generator state. Resets the cached state of the generator.
     *
     * @param jumpCoefficients the jump coefficients
     */
    private void performJump(long[] jumpCoefficients) {
        final long[] newState = new long[SEED_SIZE];
        for (final long jc : jumpCoefficients) {
            for (int b = 0; b < 64; b++) {
                if ((jc & (1L << b)) != 0) {
                    for (int i = 0; i < SEED_SIZE; i++) {
                        newState[i] ^= state[(i + index) & 15];
                    }
                }
                next();
            }
        }
        for (int j = 0; j < 16; j++) {
            state[(j + index) & 15] = newState[j];
        }
        resetCachedState();
    }
}
