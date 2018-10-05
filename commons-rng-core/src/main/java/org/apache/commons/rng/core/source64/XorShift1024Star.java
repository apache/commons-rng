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
import org.apache.commons.rng.core.util.NumberFactory;

/**
 * A fast RNG.
 *
 * @see <a href="http://xorshift.di.unimi.it/xorshift1024star.c">
 * Original source code</a>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Xorshift">Xorshift (Wikipedia)</a>
 * @since 1.0
 */
public class XorShift1024Star extends LongProvider {
    /** Size of the state vector. */
    private static final int SEED_SIZE = 16;
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
     * set.
     */
    public XorShift1024Star(long[] seed) {
        setSeedInternal(seed);
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        final long[] s = Arrays.copyOf(state, SEED_SIZE + 1);
        s[SEED_SIZE] = index;

        return composeStateInternal(super.getStateInternal(),
                                    NumberFactory.makeByteArray(s));
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
        long s1 = state[index = (index + 1) & 15];
        s1 ^= s1 << 31; // a
        state[index] = s1 ^ s0 ^ (s1 >>> 11) ^ (s0 >>> 30); // b,c
        return state[index] * 1181783497276652981L;
    }
}
