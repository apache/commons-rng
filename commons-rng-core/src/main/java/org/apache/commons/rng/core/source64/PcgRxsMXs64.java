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

/**
 * A Permuted Congruential Generator (PCG) that is composed of a 64-bit Linear Congruential
 * Generator (LCG) combined with the RXS-M-XS (random xorshift; multiply; xorshift) output
 * transformation to create 64-bit output.
 *
 * <p>State size is 128 bits and the period is 2<sup>64</sup>.</p>
 *
 * @see <a href="http://www.pcg-random.org/">
 *  PCG, A Family of Better Random Number Generators</a>
 * @since 1.3
 */
public class PcgRxsMXs64 extends LongProvider {
    /** Size of the seed array. */
    private static final int SEED_SIZE = 2;

    /** The state of the LCG. */
    private long state;

    /** The increment of the LCG. */
    private long increment;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 2, only the first 2 elements will
     * be used; if smaller, the remaining elements will be automatically set.
     */
    public PcgRxsMXs64(long[] seed) {
        if (seed.length < SEED_SIZE) {
            final long[] tmp = new long[SEED_SIZE];
            fillState(tmp, seed);
            setSeedInternal(tmp);
        } else {
            setSeedInternal(seed);
        }
    }

    /**
     * Seeds the RNG.
     *
     * @param seed Seed.
     */
    private void setSeedInternal(long[] seed) {
        // Ensure the increment is odd to provide a maximal period LCG.
        this.increment = (seed[1] << 1) | 1;
        this.state = bump(seed[0] + this.increment);
    }

    /**
     * Provides the next state of the LCG.
     *
     * @param input Current state.
     * @return next state
     */
    private long bump(long input) {
        return input * 6364136223846793005L + increment;
    }

    /** {@inheritDoc} */
    @Override
    public long next() {
        final long x = state;
        state = bump(state);
        final long word = ((x >>> ((x >>> 59) + 5)) ^ x) * -5840758589994634535L;
        return (word >>> 43) ^ word;
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        // The increment is divided by 2 before saving.
        // This transform is used in the reference PCG code; it prevents restoring from
        // a byte state a non-odd increment that results in a sub-maximal period generator.
        return composeStateInternal(NumberFactory.makeByteArray(
                new long[] {state, increment >>> 1}),
                super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, SEED_SIZE * 8);
        final long[] tempseed = NumberFactory.makeLongArray(c[0]);
        state = tempseed[0];
        // Reverse the transform performed during getState to make the increment odd again.
        increment = tempseed[1] << 1 | 1;
        super.setStateInternal(c[1]);
    }
}
