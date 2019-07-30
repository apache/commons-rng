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
package org.apache.commons.rng.core.source32;

import org.apache.commons.rng.core.util.NumberFactory;

/**
 * This abstract class is a base for algorithms from the Permuted Congruential Generator (PCG)
 * family that use an internal 64-bit Linear Congruential Generator (LCG) and output 32-bits
 * per cycle.
 *
 * @see <a href="http://www.pcg-random.org/">
 *  PCG, A Family of Better Random Number Generators</a>
 * @since 1.3
 */
abstract class AbstractPcg6432 extends IntProvider {
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
    AbstractPcg6432(long[] seed) {
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
    public int next() {
        final long x = state;
        state = bump(state);
        return transform(x);
    }

    /**
     * Transform the 64-bit state of the generator to a 32-bit output.
     * The transformation function shall vary with respect to different generators.
     *
     * @param x State.
     * @return the output
     */
    protected abstract int transform(long x);

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
