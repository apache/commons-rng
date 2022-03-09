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

import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;

/**
 * A 64-bit all purpose generator.
 *
 * <p>This is a member of the LXM family of generators: L=Linear congruential generator;
 * X=Xor based generator; and M=Mix. This member uses a 64-bit LCG and 1024-bit Xor-based
 * generator. It is named as {@code "L64X1024MixRandom"} in the {@code java.util.random}
 * package introduced in JDK 17; the LXM family is described in further detail in:
 *
 * <blockquote>Steele and Vigna (2021) LXM: better splittable pseudorandom number generators
 * (and almost as fast). Proceedings of the ACM on Programming Languages, Volume 5,
 * Article 148, pp 1–31.</blockquote>
 *
 * <p>Memory footprint is 1184 bits and the period is 2<sup>64</sup> (2<sup>1024</sup> - 1).
 *
 * <p>This generator implements
 * {@link org.apache.commons.rng.LongJumpableUniformRandomProvider LongJumpableUniformRandomProvider}.
 * In addition instances created with a different additive parameter for the LCG are robust
 * against accidental correlation in a multi-threaded setting. The additive parameters must be
 * different in the most significant 63-bits.
 *
 * @see <a href="https://doi.org/10.1145/3485525">Steele &amp; Vigna (2021) Proc. ACM Programming
 *      Languages 5, 1-31</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/random/package-summary.html">
 *      JDK 17 java.util.random javadoc</a>
 * @since 1.5
 */
public class L64X1024Mix extends AbstractL64 {
    /** Size of the seed vector. */
    private static final int SEED_SIZE = 18;
    /** Size of the state vector. */
    private static final int XBG_STATE_SIZE = 16;
    /** LCG multiplier. */
    private static final long M = LXMSupport.M64;

    /** State of the XBG. */
    private final long[] x = new long[XBG_STATE_SIZE];
    /** Index in "state" array. */
    private int index;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 18, only the first 18 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros in the last 16 elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>64</sup>.
     *
     * <p>The 1st element is used to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG. The 2nd element is used
     * to set the LCG state.</p>
     */
    public L64X1024Mix(long[] seed) {
        super(seed = extendSeed(seed, SEED_SIZE));
        System.arraycopy(seed, SEED_SIZE - XBG_STATE_SIZE, x, 0, XBG_STATE_SIZE);
        // Initialising to 15 ensures that (index + 1) % 16 == 0 and the
        // first state picked from the XBG generator is state[0].
        index = XBG_STATE_SIZE - 1;
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected L64X1024Mix(L64X1024Mix source) {
        super(source);
        System.arraycopy(source.x, 0, x, 0, XBG_STATE_SIZE);
        index = source.index;
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        final long[] s = new long[XBG_STATE_SIZE + 1];
        System.arraycopy(x, 0, s, 0, XBG_STATE_SIZE);
        s[XBG_STATE_SIZE] = index;
        return composeStateInternal(NumberFactory.makeByteArray(s),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, (XBG_STATE_SIZE + 1) * Long.BYTES);

        final long[] tmp = NumberFactory.makeLongArray(c[0]);
        System.arraycopy(tmp, 0, x, 0, XBG_STATE_SIZE);
        index = (int) tmp[XBG_STATE_SIZE];

        super.setStateInternal(c[1]);
    }

    /** {@inheritDoc} */
    @Override
    public long next() {
        // LXM generate.
        // Old state is used for the output allowing parallel pipelining
        // on processors that support multiple concurrent instructions.

        final int q = index;
        index = (q + 1) & 15;
        final long s0 = x[index];
        long s15 = x[q];
        final long s = ls;

        // Mix
        final long z = LXMSupport.lea64(s + s0);

        // LCG update
        ls = M * s + la;

        // XBG update
        s15 ^= s0;
        x[q] = Long.rotateLeft(s0, 25) ^ s15 ^ (s15 << 27);
        x[index] = Long.rotateLeft(s15, 36);

        return z;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of moving the state <em>backwards</em> by
     * (2<sup>1024</sup> - 1) positions. It can provide up to 2<sup>64</sup>
     * non-overlapping subsequences.
     */
    @Override
    public UniformRandomProvider jump() {
        return super.jump();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of moving the state <em>backwards</em> by
     * 2<sup>32</sup> (2<sup>1024</sup> - 1) positions. It can provide up to
     * 2<sup>32</sup> non-overlapping subsequences of length 2<sup>32</sup>
     * (2<sup>1024</sup> - 1); each subsequence can provide up to 2<sup>32</sup>
     * non-overlapping subsequences of length (2<sup>1024</sup> - 1) using the
     * {@link #jump()} method.
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        return super.longJump();
    }

    /** {@inheritDoc} */
    @Override
    protected AbstractL64 copy() {
        // This exists to ensure the jump function performed in the super class returns
        // the correct class type. It should not be public.
        return new L64X1024Mix(this);
    }
}
