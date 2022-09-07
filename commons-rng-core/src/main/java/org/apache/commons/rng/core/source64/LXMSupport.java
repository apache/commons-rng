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
 * Utility support for the LXM family of generators. The LXM family is described
 * in further detail in:
 *
 * <blockquote>Steele and Vigna (2021) LXM: better splittable pseudorandom number generators
 * (and almost as fast). Proceedings of the ACM on Programming Languages, Volume 5,
 * Article 148, pp 1–31.</blockquote>
 *
 * <p>Contains methods to compute unsigned multiplication of 64-bit
 * and 128-bit values to create 128-bit results for use in a 128-bit
 * linear congruential generator (LCG). Constants are provided to advance the state
 * of an LCG by a power of 2 in a single multiply operation to support jump
 * operations.
 *
 * @see <a href="https://doi.org/10.1145/3485525">Steele &amp; Vigna (2021) Proc. ACM Programming
 *      Languages 5, 1-31</a>
 * @since 1.5
 */
final class LXMSupport {
    /** 64-bit LCG multiplier. Note: (M % 8) = 5. */
    static final long M64 = 0xd1342543de82ef95L;
    /** Jump constant {@code m'} for an advance of the 64-bit LCG by 2^32.
     * Computed as: {@code m' = m^(2^32) (mod 2^64)}. */
    static final long M64P = 0x8d23804c00000001L;
    /** Jump constant precursor for {@code c'} for an advance of the 64-bit LCG by 2^32.
     * Computed as:
     * <pre>
     * product_{i=0}^{31} { M^(2^i) + 1 } (mod 2^64)
     * </pre>
     * <p>The jump is computed for the LCG with an update step of {@code s = m * s + c} as:
     * <pre>
     * s = m' * s + c' * c
     * </pre>
     */
    static final long C64P = 0x16691c9700000000L;

    /** Low half of 128-bit LCG multiplier. The upper half is {@code 1L}. */
    static final long M128L = 0xd605bbb58c8abbfdL;
    /** High half of the jump constant {@code m'} for an advance of the 128-bit LCG by 2^64.
     * The low half is 1. Computed as: {@code m' = m^(2^64) (mod 2^128)}. */
    static final long M128PH = 0x31f179f5224754f4L;
    /** High half of the jump constant for an advance of the 128-bit LCG by 2^64.
     * The low half is zero. Computed as:
     * <pre>
     * product_{i=0}^{63} { M^(2^i) + 1 } (mod 2^128)
     * </pre>
     * <p>The jump is computed for the LCG with an update step of {@code s = m * s + c} as:
     * <pre>
     * s = m' * s + c' * c
     * </pre>
     */
    static final long C128PH = 0x61139b28883277c3L;
    /**
     * The fractional part of the golden ratio, phi, scaled to 64-bits and rounded to odd.
     * <pre>
     * phi = (sqrt(5) - 1) / 2) * 2^64
     * </pre>
     * @see <a href="https://en.wikipedia.org/wiki/Golden_ratio">Golden ratio</a>
     */
    static final long GOLDEN_RATIO_64 = 0x9e3779b97f4a7c15L;

    /** A mask to convert an {@code int} to an unsigned integer stored as a {@code long}. */
    private static final long INT_TO_UNSIGNED_BYTE_MASK = 0xffff_ffffL;

    /** No instances. */
    private LXMSupport() {}

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

    /**
     * Add the two values as if unsigned 64-bit longs to produce the high 64-bits
     * of the 128-bit unsigned result.
     *
     * <h2>Warning</h2>
     *
     * <p>This method is computing a carry bit for a 128-bit linear congruential
     * generator (LCG). The method is <em>not</em> applicable to all arguments.
     * Some computations can be dropped if the {@code right} argument is assumed to
     * be the LCG addition, which should be odd to ensure a full period LCG.
     *
     * @param left the left argument
     * @param right the right argument (assumed to have the lowest bit set to 1)
     * @return the carry (either 0 or 1)
     */
    static long unsignedAddHigh(long left, long right) {
        // Method compiles to 13 bytes as Java byte code.
        // This is below the default of 35 for code inlining.
        //
        // The unsigned add of left + right may have a 65-bit result.
        // If both values are shifted right by 1 then the sum will be
        // within a 64-bit long. The right is assumed to have a low
        // bit of 1 which has been lost in the shift. The method must
        // compute if a 1 was shifted off the left which would have
        // triggered a carry when adding to the right's assumed 1.
        // The intermediate 64-bit result is shifted
        // 63 bits to obtain the most significant bit of the 65-bit result.
        // Using -1 is the same as a shift of (64 - 1) as only the last 6 bits
        // are used by the shift but requires 1 less byte in java byte code.
        //
        //    01100001      left
        // +  10011111      right always has low bit set to 1
        //
        //    0110000   1   carry last bit of left
        // +  1001111   |
        // +        1 <-+
        // = 10000000       carry bit generated
        return ((left >>> 1) + (right >>> 1) + (left & 1)) >>> -1;
    }
}
