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
 * <h2>Note: PCG generators may exhibit massive stream correlation</h2>
 *
 * <p>Although the seed size is 128 bits, only the first 64 are effective: in effect,
 * two seeds that only differ by the last 64 bits may produce highly correlated sequences.
 *
 * <p>Due to the use of an underlying linear congruential generator (LCG) alterations
 * to the 128 bit seed have the following effect: the first 64-bits alter the
 * generator state; the second 64 bits, with the exception of the most significant bit,
 * which is discarded, choose between one of two alternative LCGs
 * where the output of the chosen LCG is the same sequence except for an additive
 * constant determined by the seed bits. The result is that seeds that differ
 * only in the last 64-bits will have a 50% chance of producing highly correlated
 * output sequences.

 * <p>Consider using the fixed increment variant where the 64-bit seed sets the
 * generator state.
 *
 * <p>For further information see:
 * <ul>
 *  <li>
 *   <blockquote>
 *    Durst, M.J. (1989) <i>Using Linear Congruential Generators For Parallel Random Number Generation.
 *    Section 3.1: Different additive constants in a maximum potency congruential generator</i>.
 *    1989 Winter Simulation Conference Proceedings, Washington, DC, USA, 1989, pp. 462-466.
 *   </blockquote>
 *  </li>
 * </ul>
 *
 * @see <a href="http://www.pcg-random.org/">
 *  PCG, A Family of Better Random Number Generators</a>
 * @see <a href="https://ieeexplore.ieee.org/document/718715">Durst, M.J. (1989)
 *  Using Linear Congruential Generators For Parallel Random Number Generation</a>
 * @see <a href="https://issues.apache.org/jira/browse/RNG-123">
 *  PCG generators may exhibit massive stream correlation</a>
 * @since 1.3
 */
abstract class AbstractPcg6432 extends IntProvider {
    /** Size of the seed array. */
    private static final int SEED_SIZE = 2;
    /** The default increment. */
    private static final long DEFAULT_INCREMENT = 1442695040888963407L;

    /** The state of the LCG. */
    private long state;

    /** The increment of the LCG. */
    private long increment;

    /**
     * Creates a new instance using a default increment.
     *
     * @param seed Initial state.
     * @since 1.4
     */
    AbstractPcg6432(Long seed) {
        increment = DEFAULT_INCREMENT;
        state = bump(seed + this.increment);
    }

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 2, only the first 2 elements will
     * be used; if smaller, the remaining elements will be automatically set.
     *
     * <p>The 1st element is used to set the LCG state. The 2nd element is used
     * to set the LCG increment; the most significant bit
     * is discarded by left shift and the increment is set to odd.</p>
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
