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
 * This class aids in implementation of the Multiplicative congruential generator (MCG)
 * versions of the PCG suite of generators, a family of simple fast space-efficient statistically
 * good algorithms for random number generation.
 *
 * @see <a href="http://www.pcg-random.org/">
 *  PCG, A Family of Better Random Number Generators</a>
 * @since 1.3
 */
abstract class AbstractPcgMcg6432 extends IntProvider {

    /** Displays the current state. */
    private long state;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     */
    AbstractPcgMcg6432(Long seed) {
        state = seed | 3;
    }

    /**
     * Provides the next state of the MCG.
     * @param input - The previous state of the generator.
     * @return Next state of the MCG.
     */
    private long bump(long input) {
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
     * The transformation function shall vary with respect to different generators.
     * @param x The input.
     * @return The output of the generator.
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
        state = NumberFactory.makeLong(d[0]);
        super.setStateInternal(d[1]);
    }
}
