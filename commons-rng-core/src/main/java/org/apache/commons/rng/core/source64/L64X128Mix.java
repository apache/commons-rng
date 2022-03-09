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

/**
 * A 64-bit all purpose generator.
 *
 * <p>This is a member of the LXM family of generators: L=Linear congruential generator;
 * X=Xor based generator; and M=Mix. This member uses a 64-bit LCG and 128-bit Xor-based
 * generator. It is named as {@code "L64X128MixRandom"} in the {@code java.util.random}
 * package introduced in JDK 17; the LXM family is described in further detail in:
 *
 * <blockquote>Steele and Vigna (2021) LXM: better splittable pseudorandom number generators
 * (and almost as fast). Proceedings of the ACM on Programming Languages, Volume 5,
 * Article 148, pp 1–31.</blockquote>
 *
 * <p>Memory footprint is 256 bits and the period is 2<sup>64</sup> (2<sup>128</sup> - 1).
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
public class L64X128Mix extends AbstractL64X128 {
    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 4, only the first 4 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros in the last two elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>64</sup>.
     *
     * <p>The 1st element is used to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG. The 2nd element is used
     * to set the LCG state.</p>
     */
    public L64X128Mix(long[] seed) {
        super(seed);
    }

    /**
     * Creates a new instance using a 4 element seed.
     * A seed containing all zeros in the last two elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>64</sup>.
     *
     * <p>The 1st element is used to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG. The 2nd element is used
     * to set the LCG state.</p>
     *
     * @param seed0 Initial seed element 0.
     * @param seed1 Initial seed element 1.
     * @param seed2 Initial seed element 2.
     * @param seed3 Initial seed element 3.
     */
    public L64X128Mix(long seed0, long seed1, long seed2, long seed3) {
        super(seed0, seed1, seed2, seed3);
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected L64X128Mix(L64X128Mix source) {
        super(source);
    }

    /** {@inheritDoc} */
    @Override
    public long next() {
        // LXM generate.
        // Old state is used for the output allowing parallel pipelining
        // on processors that support multiple concurrent instructions.

        final long s0 = x0;
        final long s = ls;

        // Mix
        final long z = LXMSupport.lea64(s + s0);

        // LCG update
        ls = M * s + la;

        // XBG update
        long s1 = x1;

        s1 ^= s0;
        x0 = Long.rotateLeft(s0, 24) ^ s1 ^ (s1 << 16); // a, b
        x1 = Long.rotateLeft(s1, 37); // c

        return z;
    }

    /** {@inheritDoc} */
    @Override
    protected L64X128Mix copy() {
        // This exists to ensure the jump function performed in the super class returns
        // the correct class type. It should not be public.
        return new L64X128Mix(this);
    }
}
