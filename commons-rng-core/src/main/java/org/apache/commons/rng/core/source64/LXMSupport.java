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

/**
 * Utility support for the LXM family of generators. The LXM family is described
 * in further detail in:
 *
 * <blockquote>Steele and Vigna (2021) LXM: better splittable pseudorandom number generators
 * (and almost as fast). Proceedings of the ACM on Programming Languages, Volume 5,
 * Article 148, pp 1–31.</blockquote>
 *
 * <p>Contains methods to compute unsigned multiplication of 64-bit
 * and 128-bit values to create 128-bit results for use in a 128-bit
 * linear congruential generator (LCG). Methods are provided to advance the state
 * of an LCG by a power of 2 in a single multiply operation to support jump
 * operations.
 *
 * @see <a href="https://doi.org/10.1145/3485525">Steele &amp; Vigna (2021) Proc. ACM Programming
 *      Languages 5, 1-31</a>
 */
final class LXMSupport {
    /** 64-bit LCG multiplier. Note: (M % 8) = 5. */
    static final long M64 = 0xd1342543de82ef95L;
    /** Jump constant {@code m'} for an advance of the 64-bit LCG by 2^32.
     * The value is {@code m^(2^32)}. */
    static final long M64P = 0x8d23804c00000001L;
    /** Jump constant precursor for {@code c'} for an advance of the 64-bit LCG by 2^32.
     * The value is:
     * <pre>
     * product { M^(2^i) + 1 },  i in [0, 31]
     * </pre>
     * <p>The jump is computed for the LCG {@code s = m * s + c} as:
     * <pre>
     * s = m' * s + c' * c
     * </pre>
     */
    static final long C64P = 0x16691c9700000000L;

    /** Low half of 128-bit LCG multiplier. The upper half is {@code 1L}. */
    static final long M128L = 0xd605bbb58c8abbfdL;
    /** High half of the jump constant {@code m'} for an advance of the 128-bit LCG by 2^64.
     * The low half is 1. The value is {@code m^(2^64)}. */
    static final long M128PH = 0x31f179f5224754f4L;
    /** High half of the jump constant for an advance of the 128-bit LCG by 2^64.
     * The low half is zero. The value is:
     * <pre>
     * product { M^(2^i) + 1 },  i in [0, 63]
     * </pre>
     * <p>The jump is computed for the LCG {@code s = m * s + c} as:
     * <pre>
     * s = m' * s + c' * c
     * </pre>
     */
    static final long C128PH = 0x61139b28883277c3L;

    /**
     * The fractional part of the the golden ratio, phi, scaled to 64-bits and rounded to odd.
     * <pre>
     * phi = (sqrt(5) - 1) / 2) * 2^64
     * </pre>
     * @see <a href="https://en.wikipedia.org/wiki/Golden_ratio">Golden ratio</a>
     */
    private static final long GOLDEN_RATIO = 0x9e3779b97f4a7c15L;
    /** A mask to convert an {@code int} to an unsigned integer stored as a {@code long}. */
    private static final long INT_TO_UNSIGNED_BYTE_MASK = 0xffffffffL;

    /** No instances. */
    private LXMSupport() {}

    /**
     * Ensure the seed is at least the specified minimum length.
     *
     * <p>This method can be used in constructors that must pass their seed to the super class
     * to avoid a duplication of seed expansion to the minimum length required by the super class
     * and the class:
     * <pre>
     * public RNG extends AnotherRNG {
     *     public RNG(long[] seed) {
     *         super(seed = LXMSupport.ensureSeedLength(seed, 123));
     *         // Use seed for additional state
     *     }
     * }
     * </pre>
     *
     * <p>Note using the state filling procedure provided in
     * {@link org.apache.commons.rng.core.BaseProvider BaseProvider} is not possible as it
     * is an instance method. Calling a seed expansion routine must use a static method.
     *
     * <p>This method functions as if the seed has been expanded using a {@link SplitMix64}
     * generator seeded with the {@code seed[0]}, or zero if the input seed length is zero.
     * <pre>
     * if (seed.length < length) {
     *     final long[] s = Arrays.copyOf(seed, length);
     *     final SplitMix64 rng = new SplitMix64(s[0]);
     *     for (int i = seed.length; i < length; i++) {
     *         s[i] = rng.nextLong();
     *     }
     *     return s;
     * }
     * </pre>
     *
     * @param seed Input seed
     * @param length The minimum length
     * @return the seed
     */
    static long[] ensureSeedLength(long[] seed, int length) {
        if (seed.length < length) {
            final long[] s = Arrays.copyOf(seed, length);
            // Fill the rest as if using a SplitMix64 RNG
            long x = s[0];
            for (int i = seed.length; i < length; i++) {
                s[i] = stafford13(x += GOLDEN_RATIO);
            }
            return s;
        }
        return seed;
    }

    /**
     * Perform variant 13 of David Stafford's 64-bit mix function.
     * This is the mix function used in the {@link SplitMix64} RNG.
     *
     * <p>This is ranked first of the top 14 Stafford mixers.
     *
     * @param x the input value
     * @return the output value
     * @see <a href="http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html">Better
     *      Bit Mixing - Improving on MurmurHash3&#39;s 64-bit Finalizer.</a>
     */
    private static long stafford13(long x) {
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }

    /**
     * Perform a 64-bit mixing function using Doug Lea's 64-bit mix constants and shifts.
     *
     * <p>This is based on the original 64-bit mix function of Austin Appleby's
     * MurmurHash3 modified to use a single mix constant and 32-bit shifts, which may have
     * a performance advantage on some processors. The code is provided in Steele and
     * Vigna's paper.
     *
     * @param x the input value
     * @return the output value
     */
    static long lea64(long x) {
        x = (x ^ (x >>> 32)) * 0xdaba0b6eb09322e3L;
        x = (x ^ (x >>> 32)) * 0xdaba0b6eb09322e3L;
        return x ^ (x >>> 32);
    }

    /**
     * Multiply the two values as if unsigned 64-bit longs to produce the high 64-bits
     * of the 128-bit unsigned result.
     *
     * <p>This method computes the equivalent of:
     * <pre>
     * Math.multiplyHigh(a, b) + ((a >> 63) & b) + ((b >> 63) & a)
     * </pre>
     *
     * <p>Note: The method {@code Math.multiplyHigh} was added in JDK 9
     * and should be used as above when the source code targets Java 11
     * to exploit the intrinsic method.
     *
     * <p>Note: The method {@code Math.unsignedMultiplyHigh} was added in JDK 18
     * and should be used when the source code target allows.
     *
     * @param value1 the first value
     * @param value2 the second value
     * @return the high 64-bits of the 128-bit result
     */
    static long unsignedMultiplyHigh(long value1, long value2) {
        // Computation is based on the following observation about the upper (a and x)
        // and lower (b and y) bits of unsigned big-endian integers:
        //   ab * xy
        // =  b *  y
        // +  b * x0
        // + a0 *  y
        // + a0 * x0
        // = b * y
        // + b * x * 2^32
        // + a * y * 2^32
        // + a * x * 2^64
        //
        // Summation using a character for each byte:
        //
        //             byby byby
        // +      bxbx bxbx 0000
        // +      ayay ayay 0000
        // + axax axax 0000 0000
        //
        // The summation can be rearranged to ensure no overflow given
        // that the result of two unsigned 32-bit integers multiplied together
        // plus two full 32-bit integers cannot overflow 64 bits:
        // > long x = (1L << 32) - 1
        // > x * x + x + x == -1 (all bits set, no overflow)
        //
        // The carry is a composed intermediate which will never overflow:
        //
        //             byby byby
        // +           bxbx 0000
        // +      ayay ayay 0000
        //
        // +      bxbx 0000 0000
        // + axax axax 0000 0000

        final long a = value1 >>> 32;
        final long b = value1 & INT_TO_UNSIGNED_BYTE_MASK;
        final long x = value2 >>> 32;
        final long y = value2 & INT_TO_UNSIGNED_BYTE_MASK;


        final long by = b * y;
        final long bx = b * x;
        final long ay = a * y;
        final long ax = a * x;

        // Cannot overflow
        final long carry = (by >>> 32) +
                           (bx & INT_TO_UNSIGNED_BYTE_MASK) +
                            ay;
        // Note:
        // low = (carry << 32) | (by & INT_TO_UNSIGNED_BYTE_MASK)
        // Benchmarking shows outputting low to a long[] output argument
        // has no benefit over computing 'low = value1 * value2' separately.

        return (bx >>> 32) + (carry >>> 32) + ax;
    }
}
