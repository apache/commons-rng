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

import java.util.SplittableRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link LXMSupport}.
 *
 * <p>Note: The LXM generators require the LCG component to be advanced in a single
 * jump operation by half the period 2<sup>k/2</sup>, where {@code k} is the LCG
 * state size. This is performed in a single step using coefficients {@code m'} and
 * {@code c'}.
 *
 * <p>This class contains a generic algorithm to compute an arbitrary
 * LCG update for any power of 2. This can be bootstrap tested using small powers of
 * 2 by manual LCG iteration. Small powers can then be used to verify increasingly
 * large powers. The main {@link LXMSupport} class contains precomputed coefficients
 * {@code m'} and a precursor to {@code c'} for a jump of 2<sup>k/2</sup>
 * assuming the multiplier {@code m} used in the LXM generators. The coefficient
 * {@code c'} is computed by multiplying by the generator additive constant. The output
 * coefficients have the following properties:
 * <ul>
 * <li>The multiplier {@code m'} is constant.
 * <li>The lower half of the multiplier {@code m'} is 1.
 * <li>The lower half of the additive parameter {@code c'} is 0.
 * <li>The upper half of the additive parameter {@code c'} is odd when {@code c} is odd.
 * </ul>
 */
class LXMSupportTest {
    /** A mask to clear the lower 5 bits of an integer. */
    private static final int CLEAR_LOWER_5 = -1 << 5;

    @Test
    void testLea32() {
        // Code generated using the reference c code provided by Guy Steele.
        // Note: A java implementation requires using the unsigned shift '>>>' operator.
        //
        // uint32_t lea32(uint32_t z) {
        //    z = (z ^ (z >> 16));
        //    z *= 0xd36d884b;
        //    z = (z ^ (z >> 16));
        //    z *= 0xd36d884b;
        //    return z ^ (z >> 16);
        // }
        final int[] expected = {
            0x4fe04eac, 0x7bc5cb6c, 0x29af7e05, 0xf80de147,
            0xb90bc13a, 0x6fbce371, 0x3dbbfab0, 0xcf366cd9,
            0x90c9c2a2, 0x988ea20f, 0x75fc2207, 0x58197217,
            0xdbc584a5, 0x5d232d06, 0xd4faec13, 0xfa1fb8fd,
            0xea45a9d4, 0xb2bdb43c, 0x0502b325, 0x2ebca83c,
            0x3337cf53, 0x5531f920, 0x3edf02c5, 0xa9b79cf7,
            0x80ff2c21, 0xaba7498f, 0xa7dd9739, 0xb85c77ea,
            0x4e846134, 0x99461d4e, 0x87c027b1, 0xc8c37ec7,
            0x457301bc, 0xa8d33a18, 0x87420fcc, 0x1c5a5b6d,
            0x44b0e3ff, 0x45459875, 0x99d6ef70, 0x12e06019,
        };
        int state = 0x012de1ba;
        final int increment = 0xc8161b42;

        for (final int e : expected) {
            Assertions.assertEquals(e, LXMSupport.lea32(state += increment));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "235642368, 72987979, 792134597",
        // LXM 32-bit multiplier:
        // -1380669139 == 0xadb4a92d
        "-1380669139, 617328132, 236746283",
        "-1380669139, -789374989, -180293891",
        "-1380669139, 67236828, 236784628",
        "-1380669139, -13421542, 42",
    })
    void testLcgAdvancePow2(int m, int c, int state) {
        // Bootstrap the first powers
        int s = state;
        for (int i = 0; i < 1; i++) {
            s = m * s + c;
        }
        Assertions.assertEquals(s, lcgAdvancePow2(state, m, c, 0), "2^0 cycles");
        for (int i = 0; i < 1; i++) {
            s = m * s + c;
        }
        Assertions.assertEquals(s, lcgAdvancePow2(state, m, c, 1), "2^1 cycles");
        for (int i = 0; i < 2; i++) {
            s = m * s + c;
        }
        Assertions.assertEquals(s, lcgAdvancePow2(state, m, c, 2), "2^2 cycles");
        for (int i = 0; i < 4; i++) {
            s = m * s + c;
        }
        Assertions.assertEquals(s, lcgAdvancePow2(state, m, c, 3), "2^3 cycles");

        // Larger powers should align
        for (int n = 3; n < 31; n++) {
            final int n1 = n + 1;
            Assertions.assertEquals(
                lcgAdvancePow2(lcgAdvancePow2(state, m, c, n), m, c, n),
                lcgAdvancePow2(state, m, c, n1), () -> "2^" + n1 + " cycles");
        }

        // Larger/negative powers are ignored
        for (final int i : new int[] {32, 67868, Integer.MAX_VALUE, Integer.MIN_VALUE, -26762, -2, -1}) {
            final int n = i;
            Assertions.assertEquals(state, lcgAdvancePow2(state, m, c, n),
                () -> "2^" + n + " cycles");
        }
    }

    @Test
    void testLcg32Advance2Pow16Constants() {
        // Computing with a addition of 1 will compute:
        // m^(2^16)
        // product {m^(2^i) + 1}  for i in [0, 15]
        final int[] out = new int[2];
        lcgAdvancePow2(LXMSupport.M32, 1, 16, out);
        Assertions.assertEquals(LXMSupport.M32P, out[0], "m'");
        Assertions.assertEquals(LXMSupport.C32P, out[1], "c'");
        // Check the special values of the low half
        Assertions.assertEquals(1, out[0] & 0xffff, "low m'");
        Assertions.assertEquals(0, out[1] & 0xffff, "low c'");
    }

    /**
     * Test the precomputed 32-bit LCG advance constant in {@link LXMSupport} can be used
     * to compute the same jump as the bootstrap tested generic version in this class.
     */
    @Test
    void testLcgAdvance2Pow16() {
        final SplittableRandom r = new SplittableRandom();
        final int[] out = new int[2];

        for (int i = 0; i < 2000; i++) {
            // Must be odd
            final int c = r.nextInt() | 1;
            lcgAdvancePow2(LXMSupport.M32, c, 16, out);
            final int a = out[1];
            // Test assumptions
            Assertions.assertEquals(1, (a >>> 16) & 0x1, "High half c' should be odd");
            Assertions.assertEquals(0, a & 0xffff, "Low half c' should be 0");
            // This value can be computed from the constant
            Assertions.assertEquals(a, LXMSupport.C32P * c);
        }
    }

    /**
     * Compute the multiplier {@code m'} and addition {@code c'} to advance the state of a
     * 32-bit Linear Congruential Generator (LCG) a number of consecutive steps:
     *
     * <pre>
     * s = m' * s + c'
     * </pre>
     *
     * <p>A number of consecutive steps can be computed in a single multiply and add
     * operation. This method computes the accumulated multiplier and addition for the
     * given number of steps expressed as a power of 2. Provides support to advance for
     * 2<sup>k</sup> for {@code k in [0, 31)}. Any power {@code >= 32} is ignored as this
     * would wrap the generator to the same point. Negative powers are ignored but do not
     * throw an exception.
     *
     * <p>Based on the algorithm from:
     *
     * <blockquote>Brown, F.B. (1994) Random number generation with arbitrary strides,
     * Transactions of the American Nuclear Society 71.</blockquote>
     *
     * @param m Multiplier
     * @param c Constant
     * @param k Number of advance steps as a power of 2 (range [0, 31])
     * @param out Output result [m', c']
     * @see <A
     * href="https://www.osti.gov/biblio/89100-random-number-generation-arbitrary-strides">
     * Brown, F.B. (1994) Random number generation with arbitrary strides, Transactions of
     * the American Nuclear Society 71</a>
     */
    private static void lcgAdvancePow2(int m, int c, int k, int[] out) {
        // If any bits above the first 5 are set then this would wrap the generator to
        // the same point as multiples of period (2^32).
        // It also identifies negative powers to ignore.
        if ((k & CLEAR_LOWER_5) != 0) {
            // m'=1, c'=0
            out[0] = 1;
            out[1] = 0;
            return;
        }

        int mp = m;
        int a = c;

        for (int i = k; i != 0; i--) {
            // Update the multiplier and constant for the next power of 2
            a = (mp + 1) * a;
            mp *= mp;
        }
        out[0] = mp;
        out[1] = a;
    }

    /**
     * Compute the advanced state of a 32-bit Linear Congruential Generator (LCG). The
     * base generator advance step is:
     *
     * <pre>
     * s = m * s + c
     * </pre>
     *
     * <p>A number of consecutive steps can be computed in a single multiply and add
     * operation. This method computes the update coefficients and applies them to the
     * given state.
     *
     * <p>This method is used for testing only. For arbitrary jumps an efficient implementation
     * would inline the computation of the update coefficients; or for repeat jumps of the same
     * size pre-compute the coefficients once.
     *
     * <p>This is package-private for use in {@link AbstractLXMTest} to provide jump functionality
     * to a composite LXM generator.
     *
     * @param s State
     * @param m Multiplier
     * @param c Constant
     * @param k Number of advance steps as a power of 2 (range [0, 31])
     * @return the new state
     * @see <A
     * href="https://www.osti.gov/biblio/89100-random-number-generation-arbitrary-strides">
     * Brown, F.B. (1994) Random number generation with arbitrary strides, Transactions of
     * the American Nuclear Society 71</a>
     */
    static int lcgAdvancePow2(int s, int m, int c, int k) {
        final int[] out = new int[2];
        lcgAdvancePow2(m, c, k, out);
        final int mp = out[0];
        final int ap = out[1];
        return mp * s + ap;
    }
}
