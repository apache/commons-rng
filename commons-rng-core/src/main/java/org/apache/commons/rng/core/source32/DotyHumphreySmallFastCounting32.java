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
 * Implement the Small, Fast, Counting (SFC) 32-bit generator of Chris Doty-Humphrey.
 * The original source is the PractRand test suite by the same author.
 *
 * <p>The state size is 128-bits; the period is a minimum of 2<sup>32</sup> and an
 * average of approximately 2<sup>127</sup>.</p>
 *
 * @see <a href="http://pracrand.sourceforge.net/">PractRand</a>
 * @since 1.3
 */
public class DotyHumphreySmallFastCounting32 extends IntProvider {
    /** Size of the seed. */
    private static final int SEED_SIZE = 3;

    /** State a. */
    private int a;
    /** State b. */
    private int b;
    /** State c. */
    private int c;
    /** Counter. */
    private int counter;

    /**
     * Creates an instance with the given seed.
     *
     * @param seed Initial seed.
     * If the length is larger than 3, only the first 3 elements will
     * be used; if smaller, the remaining elements will be automatically set.
     */
    public DotyHumphreySmallFastCounting32(final int[] seed) {
        if (seed.length < SEED_SIZE) {
            final int[] state = new int[SEED_SIZE];
            fillState(state, seed);
            setSeedInternal(state);
        } else {
            setSeedInternal(seed);
        }
    }

    /**
     * Seeds the RNG.
     *
     * @param seed Seed.
     */
    private void setSeedInternal(final int[] seed) {
        a = seed[0];
        b = seed[1];
        c = seed[2];
        counter = 1;
        for (int i = 0; i < 15; i++) {
            next();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final int next() {
        final int tmp = a + b + counter++;
        a = b ^ (b >>> 9);
        b = c + (c << 3);
        c = Integer.rotateLeft(c, 21) + tmp;
        return tmp;
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(new int[] {a, b, c, counter}),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(final byte[] s) {
        final byte[][] parts = splitStateInternal(s, 4 * 4);

        final int[] tmp = NumberFactory.makeIntArray(parts[0]);
        a = tmp[0];
        b = tmp[1];
        c = tmp[2];
        counter = tmp[3];

        super.setStateInternal(parts[1]);
    }
}
