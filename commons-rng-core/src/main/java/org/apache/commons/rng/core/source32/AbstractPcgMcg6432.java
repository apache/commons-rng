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
 * family that use an internal 64-bit Multiplicative Congruential Generator (MCG) and output
 * 32-bits per cycle.
 *
 * @see <a href="http://www.pcg-random.org/">
 *  PCG, A Family of Better Random Number Generators</a>
 * @since 1.3
 */
abstract class AbstractPcgMcg6432 extends IntProvider {
    /** The state of the MCG. */
    private long state;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     */
    AbstractPcgMcg6432(Long seed) {
        // A seed of zero will result in a non-functional MCG; it must be odd for a maximal
        // period MCG. The multiplication factor always sets the 2 least-significant bits to 1
        // if they are already 1 so these are explicitly set. Bit k (zero-based) will have
        // period 2^(k-1) starting from bit 2 with a period of 1. Bit 63 has period 2^62.
        state = seed | 3;
    }

    /**
     * Provides the next state of the MCG.
     *
     * @param input Current state.
     * @return next state
     */
    private static long bump(long input) {
        return input * 6364136223846793005L;
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
        return composeStateInternal(NumberFactory.makeByteArray(state),
                super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] d = splitStateInternal(s, 8);
        // As per the constructor, ensure the lower 2 bits of state are set.
        state = NumberFactory.makeLong(d[0]) | 3;
        super.setStateInternal(d[1]);
    }
}
