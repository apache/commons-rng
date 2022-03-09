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

import java.math.BigInteger;
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
    /** 2^63. */
    private static final BigInteger TWO_POW_63 = BigInteger.ONE.shiftLeft(63);
    /** 2^128. The modulus of a 128-bit LCG. */
    private static final BigInteger MOD = BigInteger.ONE.shiftLeft(128);
    /** A mask to clear the lower 6 bits of an integer. */
    private static final int CLEAR_LOWER_6 = -1 << 6;
    /** A mask to clear the lower 7 bits of an integer. */
    private static final int CLEAR_LOWER_7 = -1 << 7;

    @Test
    void testLea64() {
        // Code generated using the reference java code provided by Steele and Vigna:
        // https://doi.org/10.1145/3485525
        final long[] expected = {
            0x45b8512f9ff46f10L, 0xd6ce3db0dd63efc3L, 0x47bf6058710f2a88L, 0x85b8c74e40981596L,
            0xd77442e45944235eL, 0x3ea4255636bfb1c3L, 0x296ec3c9d3e0addcL, 0x6c285eb9694f6eb2L,
            0x8121aeca2ba15b66L, 0x2b6d5c2848c4fdc4L, 0xcc99bc57f5e3e024L, 0xc00f59a3ad3666cbL,
            0x74e5285467c20ae7L, 0xf4d51701e3ea9555L, 0x3aeb92e31a9b1a0eL, 0x5a1a0ce875c7dcaL,
            0xb9a561fb7d82d0f3L, 0x97095f0ab633bf2fL, 0xfe74b5290c07c1d1L, 0x9dfd354727d45838L,
            0xf6279a8801201eddL, 0x2db471b1d42860eeL, 0x4ee66ceb27bd34ecL, 0x2005875ad25bd11aL,
            0x92eac4d1446a0204L, 0xa46087d5dd5fa38eL, 0x7967530c43faabe1L, 0xc53e1dd74fd9bd15L,
            0x259001ab97cca8bcL, 0x5edf024ee6cb1d8bL, 0x3fc021bba7d0d7e6L, 0xf82cae56e00245dbL,
            0xf1dc30974b524d02L, 0xe1f2f1db0af7ace9L, 0x853d5892ebccb9f6L, 0xe266f36a3121da55L,
            0x3b034a81bad01622L, 0x852b53c14569ada2L, 0xee902ddc658c86c9L, 0xd9e926b766013254L,
        };
        long state = 0x012de1babb3c4104L;
        final long increment = 0xc8161b4202294965L;

        for (final long e : expected) {
            Assertions.assertEquals(e, LXMSupport.lea64(state += increment));
        }
    }

    @Test
    void testUnsignedMultiplyHighEdgeCases() {
        final long[] values = {
            -1, 0, 1, Long.MAX_VALUE, Long.MIN_VALUE, LXMSupport.M128L,
            0xffL, 0xff00L, 0xff0000L, 0xff000000L,
            0xff00000000L, 0xff0000000000L, 0xff000000000000L, 0xff000000000000L,
            0xffffL, 0xffff0000L, 0xffff00000000L, 0xffff000000000000L,
            0xffffffffL, 0xffffffff00000000L
        };

        for (final long v1 : values) {
            for (final long v2 : values) {
                assertMultiplyHigh(v1, v2, LXMSupport.unsignedMultiplyHigh(v1, v2));
            }
        }
    }

    @Test
    void testUnsignedMultiplyHigh() {
        final long[] values = new SplittableRandom().longs(100).toArray();
        for (final long v1 : values) {
            for (final long v2 : values) {
                assertMultiplyHigh(v1, v2, LXMSupport.unsignedMultiplyHigh(v1, v2));
            }
        }
    }

    private static void assertMultiplyHigh(long v1, long v2, long hi) {
        final BigInteger bi1 = toUnsignedBigInteger(v1);
        final BigInteger bi2 = toUnsignedBigInteger(v2);
        final BigInteger expected = bi1.multiply(bi2);
        Assertions.assertTrue(expected.bitLength() <= 128);
        Assertions.assertEquals(expected.shiftRight(64).longValue(), hi,
            () -> String.format("%s * %s", bi1, bi2));
    }

    /**
     * Create a big integer treating the value as unsigned.
     *
     * @param v Value
     * @return the big integer
     */
    private static BigInteger toUnsignedBigInteger(long v) {
        return v < 0 ?
            TWO_POW_63.add(BigInteger.valueOf(v & Long.MAX_VALUE)) :
            BigInteger.valueOf(v);
    }

    /**
     * Create a 128-bit big integer treating the value as unsigned.
     *
     * @param hi High part of value
     * @param lo High part of value
     * @return the big integer
     */
    private static BigInteger toUnsignedBigInteger(long hi, long lo) {
        return toUnsignedBigInteger(hi).shiftLeft(64).add(toUnsignedBigInteger(lo));
    }


    @Test
    void testUnsignedAddHigh() {
        // This will trigger a carry as the sum is 2^64.
        // Note: b must be odd.
        long a = 1;
        long b = -1;
        // The values are adjusted randomly to make 'b' smaller
        // and 'a' larger but always maintaining the sum to 2^64.
        final SplittableRandom sr = new SplittableRandom();
        // Number of steps = 2^5
        final int pow = 5;
        // Divide 2^64 by the number of steps. This ensures 'a'
        // will not wrap if all steps are the maximum step.
        final long range = 1L << (64 - pow);
        for (int i = 1 << pow; i-- != 0;) {
            // Check invariants
            Assertions.assertEquals(0L, a + b);
            Assertions.assertEquals(1L, b & 0x1);
            // Should carry
            assertAddHigh(a, b);
            // Should not carry
            assertAddHigh(a - 1, b);
            // Update. Must be even to maintain an odd b
            final long step = sr.nextLong(range) & ~0x1;
            a += step;
            b -= step;
        }

        // Random. This should carry 50% of the time.
        for (int i = 0; i < 1000; i++) {
            assertAddHigh(sr.nextLong(), sr.nextLong() | 1);
        }
    }

    private static void assertAddHigh(long a, long b) {
        // Reference using comparedUnsigned. If the sum is smaller
        // than the argument 'a' then adding 'b' has triggered a carry
        final long sum = a + b;
        final long carry = Long.compareUnsigned(sum, a) < 0 ? 1 : 0;
        Assertions.assertEquals(carry, LXMSupport.unsignedAddHigh(a, b),
            () -> String.format("%d + %d", a, b));
    }

    @ParameterizedTest
    @CsvSource({
        "6364136223846793005, 1442695040888963407, 2738942865345",
        // LXM 64-bit multiplier:
        // -3372029247567499371 == 0xd1342543de82ef95L
        "-3372029247567499371, 9832718632891239, 236823998",
        "-3372029247567499371, -6152834681292394, -6378917984523",
        "-3372029247567499371, 12638123, 21313",
        "-3372029247567499371, -67123, 42",
    })
    void testLcgAdvancePow2(long m, long c, long state) {
        // Bootstrap the first powers
        long s = state;
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
        for (int n = 3; n < 63; n++) {
            final int n1 = n + 1;
            Assertions.assertEquals(
                lcgAdvancePow2(lcgAdvancePow2(state, m, c, n), m, c, n),
                lcgAdvancePow2(state, m, c, n1), () -> "2^" + n1 + " cycles");
        }

        // Larger/negative powers are ignored
        for (final int i : new int[] {64, 67868, Integer.MAX_VALUE, Integer.MIN_VALUE, -26762, -2, -1}) {
            final int n = i;
            Assertions.assertEquals(state, lcgAdvancePow2(state, m, c, n),
                () -> "2^" + n + " cycles");
        }
    }

    @ParameterizedTest
    @CsvSource({
        "126868183112323, 6364136223846793005, 1442695040888963407, 2738942865345, 3467819237274724, 12367842684328",
        // Force carry when computing (m + 1) with ml = -1
        "-126836182831123, -1, 12678162381123, -12673162838122, 12313212312354235, 127384628323784",
        "92349876232, -1, 92374923739482, 2394782347892, 1239748923479, 627348278239",
        // LXM 128-bit multiplier:
        // -3024805186288043011 == 0xd605bbb58c8abbfdL
        "1, -3024805186288043011, 9832718632891239, 236823998, -23564628723714323, -12361783268182",
        "1, -3024805186288043011, -6152834681292394, -6378917984523, 127317381313, -12637618368172",
        "1, -3024805186288043011, 1, 2, 3, 4",
        "1, -3024805186288043011, -1, -78, -56775, 121",
    })
    void testLcg128AdvancePow2(long mh, long ml, long ch, long cl, long stateh, long statel) {
        // Bootstrap the first powers
        BigInteger s = toUnsignedBigInteger(stateh, statel);
        final BigInteger m = toUnsignedBigInteger(mh, ml);
        final BigInteger c = toUnsignedBigInteger(ch, cl);
        for (int i = 0; i < 1; i++) {
            s = m.multiply(s).add(c).mod(MOD);
        }
        Assertions.assertEquals(s.shiftRight(64).longValue(),
            lcgAdvancePow2High(stateh, statel, mh, ml, ch, cl, 0), "2^0 cycles");
        for (int i = 0; i < 1; i++) {
            s = m.multiply(s).add(c).mod(MOD);
        }
        Assertions.assertEquals(s.shiftRight(64).longValue(),
            lcgAdvancePow2High(stateh, statel, mh, ml, ch, cl, 1), "2^1 cycles");
        for (int i = 0; i < 2; i++) {
            s = m.multiply(s).add(c).mod(MOD);
        }
        Assertions.assertEquals(s.shiftRight(64).longValue(),
            lcgAdvancePow2High(stateh, statel, mh, ml, ch, cl, 2), "2^2 cycles");
        for (int i = 0; i < 4; i++) {
            s = m.multiply(s).add(c).mod(MOD);
        }
        Assertions.assertEquals(s.shiftRight(64).longValue(),
            lcgAdvancePow2High(stateh, statel, mh, ml, ch, cl, 3), "2^3 cycles");

        // Larger powers should align
        for (int n = 3; n < 127; n++) {
            final int n1 = n + 1;
            // The method under test does not return the lower half (by design) so
            // we compute it using the same algorithm
            final long lo = lcgAdvancePow2(statel, ml, cl, n);
            final long hi = lcgAdvancePow2High(stateh, statel, mh, ml, ch, cl, n);
            Assertions.assertEquals(
                lcgAdvancePow2High(hi, lo, mh, ml, ch, cl, n),
                lcgAdvancePow2High(stateh, statel, mh, ml, ch, cl, n1), () -> "2^" + n1 + " cycles");
        }

        // Larger/negative powers are ignored
        for (final int i : new int[] {128, 67868, Integer.MAX_VALUE, Integer.MIN_VALUE, -26762, -2, -1}) {
            final int n = i;
            Assertions.assertEquals(stateh, lcgAdvancePow2High(stateh, statel, mh, ml, ch, cl, n),
                () -> "2^" + n + " cycles");
        }
    }

    @Test
    void testLcg64Advance2Pow32Constants() {
        // Computing with a addition of 1 will compute:
        // m^(2^32)
        // product {m^(2^i) + 1}  for i in [0, 31]
        final long[] out = new long[2];
        lcgAdvancePow2(LXMSupport.M64, 1, 32, out);
        Assertions.assertEquals(LXMSupport.M64P, out[0], "m'");
        Assertions.assertEquals(LXMSupport.C64P, out[1], "c'");
        // Check the special values of the low half
        Assertions.assertEquals(1, (int) out[0], "low m'");
        Assertions.assertEquals(0, (int) out[1], "low c'");
    }

    @Test
    void testLcg128Advance2Pow64Constants() {
        // Computing with an addition of 1 will compute:
        // m^(2^64)
        // product {m^(2^i) + 1}  for i in [0, 63]
        final long[] out = new long[4];
        lcgAdvancePow2(1, LXMSupport.M128L, 0, 1, 64, out);
        Assertions.assertEquals(LXMSupport.M128PH, out[0], "high m'");
        Assertions.assertEquals(LXMSupport.C128PH, out[2], "high c'");
        // These values are not stored.
        // Their special values simplify the 128-bit multiplication.
        Assertions.assertEquals(1, out[1], "low m'");
        Assertions.assertEquals(0, out[3], "low c'");
    }

    /**
     * Test the precomputed 64-bit LCG advance constant in {@link LXMSupport} can be used
     * to compute the same jump as the bootstrap tested generic version in this class.
     */
    @Test
    void testLcgAdvance2Pow32() {
        final SplittableRandom r = new SplittableRandom();
        final long[] out = new long[2];

        for (int i = 0; i < 2000; i++) {
            // Must be odd
            final long c = r.nextLong() | 1;
            lcgAdvancePow2(LXMSupport.M64, c, 32, out);
            final long a = out[1];
            // Test assumptions
            Assertions.assertEquals(1, (a >>> 32) & 0x1, "High half c' should be odd");
            Assertions.assertEquals(0, (int) a, "Low half c' should be 0");
            // This value can be computed from the constant
            Assertions.assertEquals(a, LXMSupport.C64P * c);
        }
    }

    /**
     * Test the precomputed 128-bit LCG advance constant in {@link LXMSupport} can be used
     * to compute the same jump as the bootstrap tested generic version in this class.
     */
    @Test
    void testLcgAdvance2Pow64() {
        final SplittableRandom r = new SplittableRandom();
        final long[] out = new long[4];

        for (int i = 0; i < 2000; i++) {
            // Must be odd for the assumptions
            final long ch = r.nextLong();
            final long cl = r.nextLong() | 1;
            lcgAdvancePow2(1, LXMSupport.M128L, ch, cl, 64, out);
            final long ah = out[2];
            // Test assumptions
            Assertions.assertEquals(1, ah & 0x1, "High half c' should be odd");
            Assertions.assertEquals(0, out[3], "Low half c' should be 0");
            // This value can be computed from the constant
            Assertions.assertEquals(ah, LXMSupport.C128PH * cl);
        }
    }

    /**
     * Compute the multiplier {@code m'} and addition {@code c'} to advance the state of a
     * 64-bit Linear Congruential Generator (LCG) a number of consecutive steps:
     *
     * <pre>
     * s = m' * s + c'
     * </pre>
     *
     * <p>A number of consecutive steps can be computed in a single multiply and add
     * operation. This method computes the accumulated multiplier and addition for the
     * given number of steps expressed as a power of 2. Provides support to advance for
     * 2<sup>k</sup> for {@code k in [0, 63)}. Any power {@code >= 64} is ignored as this
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
     * @param k Number of advance steps as a power of 2 (range [0, 63])
     * @param out Output result [m', c']
     * @see <A
     * href="https://www.osti.gov/biblio/89100-random-number-generation-arbitrary-strides">
     * Brown, F.B. (1994) Random number generation with arbitrary strides, Transactions of
     * the American Nuclear Society 71</a>
     */
    private static void lcgAdvancePow2(long m, long c, int k, long[] out) {
        // If any bits above the first 6 are set then this would wrap the generator to
        // the same point as multiples of period (2^64).
        // It also identifies negative powers to ignore.
        if ((k & CLEAR_LOWER_6) != 0) {
            // m'=1, c'=0
            out[0] = 1;
            out[1] = 0;
            return;
        }

        long mp = m;
        long a = c;

        for (int i = k; i != 0; i--) {
            // Update the multiplier and constant for the next power of 2
            a = (mp + 1) * a;
            mp *= mp;
        }
        out[0] = mp;
        out[1] = a;
    }

    /**
     * Compute the advanced state of a 64-bit Linear Congruential Generator (LCG). The
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
     * @param k Number of advance steps as a power of 2 (range [0, 63])
     * @return the new state
     * @see <A
     * href="https://www.osti.gov/biblio/89100-random-number-generation-arbitrary-strides">
     * Brown, F.B. (1994) Random number generation with arbitrary strides, Transactions of
     * the American Nuclear Society 71</a>
     */
    static long lcgAdvancePow2(long s, long m, long c, int k) {
        final long[] out = new long[2];
        lcgAdvancePow2(m, c, k, out);
        final long mp = out[0];
        final long ap = out[1];
        return mp * s + ap;
    }

    /**
     * Compute the multiplier {@code m'} and addition {@code c'} to advance the state of a
     * 128-bit Linear Congruential Generator (LCG) a number of consecutive steps:
     *
     * <pre>
     * s = m' * s + c'
     * </pre>
     *
     * <p>A number of consecutive steps can be computed in a single multiply and add
     * operation. This method computes the accumulated multiplier and addition for the
     * given number of steps expressed as a power of 2. Provides support to advance for
     * 2<sup>k</sup> for {@code k in [0, 127)}. Any power {@code >= 128} is ignored as
     * this would wrap the generator to the same point. Negative powers are ignored but do
     * not throw an exception.
     *
     * <p>Based on the algorithm from:
     *
     * <blockquote>Brown, F.B. (1994) Random number generation with arbitrary strides,
     * Transactions of the American Nuclear Society 71.</blockquote>
     *
     * @param mh High half of multiplier
     * @param ml Low half of multiplier
     * @param ch High half of constant
     * @param cl Low half of constant
     * @param k Number of advance steps as a power of 2 (range [0, 127])
     * @param out Output result [m', c'] packed as high-half, low-half of each constant
     * @see <A
     * href="https://www.osti.gov/biblio/89100-random-number-generation-arbitrary-strides">
     * Brown, F.B. (1994) Random number generation with arbitrary strides, Transactions of
     * the American Nuclear Society 71</a>
     */
    private static void lcgAdvancePow2(final long mh, final long ml,
                                       final long ch, final long cl,
                                       int k, long[] out) {
        // If any bits above the first 7 are set then this would wrap the generator to
        // the same point as multiples of period (2^128).
        // It also identifies negative powers to ignore.
        if ((k & CLEAR_LOWER_7) != 0) {
            // m'=1, c'=0
            out[0] = out[2] = out[3] = 0;
            out[1] = 1;
            return;
        }

        long mph = mh;
        long mpl = ml;
        long ah = ch;
        long al = cl;

        for (int i = k; i != 0; i--) {
            // Update the multiplier and constant for the next power of 2
            // c = (m + 1) * c
            // Create (m + 1), carrying any overflow
            final long mp1l = mpl + 1;
            final long mp1h = mp1l == 0 ? mph + 1 : mph;
            ah = LXMSupport.unsignedMultiplyHigh(mp1l, al) + mp1h * al + mp1l * ah;
            al = mp1l * al;

            // m = m * m
            // Note: A dedicated unsignedSquareHigh is benchmarked in the JMH module
            mph = LXMSupport.unsignedMultiplyHigh(mpl, mpl) + 2 * mph * mpl;
            mpl = mpl * mpl;
        }

        out[0] = mph;
        out[1] = mpl;
        out[2] = ah;
        out[3] = al;
    }

    /**
     * Compute the advanced state of a 128-bit Linear Congruential Generator (LCG). The
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
     * <p>Note: Returns only the high-half of the state. For powers of 2 less than 64 the low
     * half can be computed using {@link #lcgAdvancePow2(long, long, long, int)}.
     *
     * <p>This is package-private for use in {@link AbstractLXMTest} to provide jump functionality
     * to a composite LXM generator.
     *
     * @param sh High half of state
     * @param sl Low half of state
     * @param mh High half of multiplier
     * @param ml Low half of multiplier
     * @param ch High half of constant
     * @param cl Low half of constant
     * @param k Number of advance steps as a power of 2 (range [0, 127])
     * @return high half of the new state
     * @see <A
     * href="https://www.osti.gov/biblio/89100-random-number-generation-arbitrary-strides">
     * Brown, F.B. (1994) Random number generation with arbitrary strides, Transactions of
     * the American Nuclear Society 71</a>
     */
    static long lcgAdvancePow2High(long sh, long sl,
                                   long mh, long ml,
                                   long ch, long cl,
                                   int k) {
        final long[] out = new long[4];
        lcgAdvancePow2(mh, ml, ch, cl, k, out);
        final long mph = out[0];
        final long mpl = out[1];
        final long ah = out[2];
        final long al = out[3];

        // Perform the single update to the state
        // m * s + c
        long hi = LXMSupport.unsignedMultiplyHigh(mpl, sl) + mpl * sh + mph * sl + ah;
        // Carry propagation of
        // lo = sl * mpl + al
        final long lo = sl * mpl;
        if (Long.compareUnsigned(lo + al, lo) < 0) {
            ++hi;
        }
        return hi;
    }
}
