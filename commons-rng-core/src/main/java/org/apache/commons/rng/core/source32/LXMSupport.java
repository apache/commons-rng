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

/**
 * Utility support for the LXM family of generators. The LXM family is described
 * in further detail in:
 *
 * <blockquote>Steele and Vigna (2021) LXM: better splittable pseudorandom number generators
 * (and almost as fast). Proceedings of the ACM on Programming Languages, Volume 5,
 * Article 148, pp 1–31.</blockquote>
 *
 * <p>Constants are provided to advance the state of an LCG by a power of 2 in a single
 * multiply operation to support jump operations.
 *
 * @see <a href="https://doi.org/10.1145/3485525">Steele &amp; Vigna (2021) Proc. ACM Programming
 *      Languages 5, 1-31</a>
 * @since 1.5
 */
final class LXMSupport {
    /** 32-bit LCG multiplier. Note: (M % 8) = 5. */
    static final int M32 = 0xadb4a92d;
    /** Jump constant {@code m'} for an advance of the 32-bit LCG by 2^16.
     * Computed as: {@code m' = m^(2^16) (mod 2^32)}. */
    static final int M32P = 0x65640001;
    /** Jump constant precursor for {@code c'} for an advance of the 32-bit LCG by 2^16.
     * Computed as:
     * <pre>
     * product_{i=0}^{15} { M^(2^i) + 1 } (mod 2^32)
     * </pre>
     * <p>The jump is computed for the LCG with an update step of {@code s = m * s + c} as:
     * <pre>
     * s = m' * s + c' * c
     * </pre>
     */
    static final int C32P = 0x046b0000;
    /**
     * The fractional part of the golden ratio, phi, scaled to 32-bits and rounded to odd.
     * <pre>
     * phi = (sqrt(5) - 1) / 2) * 2^32
     * </pre>
     * @see <a href="https://en.wikipedia.org/wiki/Golden_ratio">Golden ratio</a>
     */
    static final int GOLDEN_RATIO_32 = 0x9e3779b9;

    /** No instances. */
    private LXMSupport() {}

    /**
     * Perform a 32-bit mixing function using Doug Lea's 32-bit mix constants and shifts.
     *
     * <p>This is based on the original 32-bit mix function of Austin Appleby's
     * MurmurHash3 modified to use a single mix constant and 16-bit shifts, which may have
     * a performance advantage on some processors.
     *
     * <p>The code was kindly provided by Guy Steele as a printing constraint led to
     * its omission from Steele and Vigna's paper.
     *
     * <p>Note from Guy Steele:
     * <blockquote>
     * The constant 0xd36d884b was chosen by Doug Lea by taking the (two’s-complement)
     * negation of the decimal constant 747796405, which appears in Table 5 of L’Ecuyer’s
     * classic paper “Tables of Linear Congruential Generators of Different Sizes and Good
     * Lattice Structure” (January 1999); the constant in lea64 was chosen in a similar manner.
     * These choices were based on his engineering intuition and then validated by testing.
     * </blockquote>
     *
     * @param x the input value
     * @return the output value
     */
    static int lea32(int x) {
        x = (x ^ (x >>> 16)) * 0xd36d884b;
        x = (x ^ (x >>> 16)) * 0xd36d884b;
        return x ^ (x >>> 16);
    }
}
