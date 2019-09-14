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
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;

/**
 * A fast RNG implementing the {@code XorShift1024*} algorithm.
 *
 * <p>Note: This has been superseded by {@link XorShift1024StarPhi}. The sequences emitted
 * by both generators are correlated.</p>
 *
 * @see <a href="http://xorshift.di.unimi.it/xorshift1024star.c">Original source code</a>
 * @see <a href="https://en.wikipedia.org/wiki/Xorshift">Xorshift (Wikipedia)</a>
 * @since 1.0
 */
public class XorShift1024Star extends LongProvider implements JumpableUniformRandomProvider {
    /** Size of the state vector. */
    private static final int SEED_SIZE = 16;
    /** The coefficients for the jump function. */
    private static final long[] JUMP_COEFFICIENTS = {
        0x84242f96eca9c41dL, 0xa3c65b8776f96855L, 0x5b34a39f070b5837L, 0x4489affce4f31a1eL,
        0x2ffeeb0a48316f40L, 0xdc2d9891fe68c022L, 0x3659132bb12fea70L, 0xaac17d8efa43cab8L,
        0xc4cb815590989b13L, 0x5ee975283d71c93bL, 0x691548c86c1bd540L, 0x7910c41d10a1e6a5L,
        0x0b5fc64563b3e2a8L, 0x047f7684e9fc949dL, 0xb99181f2d8f685caL, 0x284600e3f30e38c3L
    };
    /** State. */
    private final long[] state = new long[SEED_SIZE];
    /** The multiplier for the XorShift1024 algorithm. */
    private final long multiplier;
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
    public XorShift1024Star(long[] seed) {
        this(seed, 1181783497276652981L);
    }

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 16, only the first 16 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros will create a non-functional generator.
     * @param multiplier The multiplier for the XorShift1024 algorithm.
     */
    protected XorShift1024Star(long[] seed, long multiplier) {
        setSeedInternal(seed);
        this.multiplier = multiplier;
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected XorShift1024Star(XorShift1024Star source) {
        super(source);
        System.arraycopy(source.state, 0, state, 0, SEED_SIZE);
        multiplier = source.multiplier;
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
        final long s0 = state[index];
        index = (index + 1) & 15;
        long s1 = state[index];
        s1 ^= s1 << 31; // a
        state[index] = s1 ^ s0 ^ (s1 >>> 11) ^ (s0 >>> 30); // b,c
        return state[index] * multiplier;
    }

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
        performJump();
        return copy;
    }

    /**
     * Create a copy.
     *
     * @return the copy
     */
    protected XorShift1024Star copy() {
        // This exists to ensure the jump function returns
        // the correct class type. It should not be public.
        return new XorShift1024Star(this);
    }

    /**
     * Perform the jump to advance the generator state. Resets the cached state of the generator.
     */
    private void performJump() {
        final long[] newState = new long[SEED_SIZE];
        for (final long jc : JUMP_COEFFICIENTS) {
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
