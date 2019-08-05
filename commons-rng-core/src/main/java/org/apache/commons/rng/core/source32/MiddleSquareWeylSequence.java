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
 * Middle Square Weyl Sequence Random Number Generator.
 *
 * <p>A fast all-purpose 32-bit generator. Memory footprint is 192 bits and the period is at least
 * {@code 2^64}.</p>
 *
 * <p>Implementation is based on the paper
 * <a href="https://arxiv.org/abs/1704.00358v3">Middle Square Weyl Sequence RNG</a>.</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Middle-square_method">Middle Square Method</a>
 *
 * @since 1.3
 */
public class MiddleSquareWeylSequence extends IntProvider {
    /** Size of the seed array. */
    private static final int SEED_SIZE = 3;

    /** State of the generator. */
    private long x;
    /** State of the Weyl sequence. */
    private long w;
    /**
     * Increment for the Weyl sequence. This must be odd to ensure a full period.
     *
     * <p>This is not final to support the restore functionality.</p>
     */
    private long s;

    /**
     * Creates a new instance.
     *
     * <p>Note: The generator output quality is highly dependent on the initial seed.
     * If the generator is seeded poorly (for example with all zeros) it is possible the
     * generator may output zero for many cycles before the internal state recovers randomness.
     * The seed elements are used to set:</p>
     *
     * <ol>
     *   <li>The state of the generator
     *   <li>The state of the Weyl sequence
     *   <li>The increment of the Weyl sequence
     * </ol>
     *
     * <p>The third element is set to odd to ensure a period of at least 2<sup>64</sup>. If the
     * increment is of low complexity then the Weyl sequence does not contribute high quality
     * randomness. It is recommended to use a permutation of 8 hex characters for the upper
     * and lower 32-bits of the increment.</p>
     *
     * <p>The state of the generator is squared during each cycle. There is a possibility that
     * different seeds can produce the same output, for example 0 and 2<sup>32</sup> produce
     * the same square. This can be avoided by using the high complexity Weyl increment for the
     * state seed element.</p>
     *
     * @param seed Initial seed.
     * If the length is larger than 3, only the first 3 elements will
     * be used; if smaller, the remaining elements will be automatically set.
     */
    public MiddleSquareWeylSequence(long[] seed) {
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
        x = seed[0];
        w = seed[1];
        // Ensure the increment is odd to provide a maximal period Weyl sequence.
        this.s = seed[2] | 1L;
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(new long[] {x, w, s}),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] state) {
        final byte[][] c = splitStateInternal(state, SEED_SIZE * 8);
        setSeedInternal(NumberFactory.makeLongArray(c[0]));
        super.setStateInternal(c[1]);
    }

    /** {@inheritDoc} */
    @Override
    public int next() {
        x *= x;
        x += w += s;
        return (int) (x = (x >>> 32) | (x << 32));
    }

    /** {@inheritDoc} */
    @Override
    public long nextLong() {
        // Avoid round trip from long to int to long by performing two iterations inline
        x *= x;
        x += w += s;
        final long i1 = x & 0xffffffff00000000L;
        x = (x >>> 32) | (x << 32);
        x *= x;
        x += w += s;
        final long i2 = x >>> 32;
        x = i2 | x << 32;
        return i1 | i2;
    }
}
