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
package org.apache.commons.rng.examples.jmh.core;

import java.math.BigInteger;
import java.util.function.LongSupplier;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.examples.jmh.core.LXMBenchmark.LCG128Source;
import org.apache.commons.rng.examples.jmh.core.LXMBenchmark.LXM128Source;
import org.apache.commons.rng.examples.jmh.core.LXMBenchmark.UnsignedMultiply128Source;
import org.apache.commons.rng.examples.jmh.core.LXMBenchmark.UnsignedMultiplyHighSource;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.opentest4j.TestAbortedException;

/**
 * Test for methods used in {@link LXMBenchmark}. This checks the different versions of
 * 128-bit multiplication compute the correct result (verified using BigInteger).
 */
class LXMBenchmarkTest {
    /** 2^63. */
    private static final BigInteger TWO_POW_63 = BigInteger.ONE.shiftLeft(63);

    /**
     * A function operating on 2 long values.
     */
    interface LongLongFunction {
        /**
         * Apply the function.
         *
         * @param a Argument a
         * @param b Argument b
         * @return the result
         */
        long apply(long a, long b);
    }

    /**
     * A function operating on 4 long values.
     */
    interface LongLongLongLongFunction {
        /**
         * Apply the function.
         *
         * @param a Argument a
         * @param b Argument b
         * @param c Argument c
         * @param d Argument d
         * @return the result
         */
        long apply(long a, long b, long c, long d);
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_9)
    void testMathMultiplyHigh() {
        try {
            assertUnsignedMultiply(UnsignedMultiplyHighSource::mathMultiplyHigh, null);
        } catch (NoSuchMethodError e) {
            throw new TestAbortedException("Update mathMultiplyHigh to call Math.multiplyHigh");
        }
    }

    @Test
    // Note: JAVA_18 is not in the enum
    @EnabledForJreRange(min = JRE.OTHER)
    void testMathUnsignedMultiplyHigh() {
        try {
            assertUnsignedMultiply(UnsignedMultiplyHighSource::mathUnsignedMultiplyHigh, null);
        } catch (NoSuchMethodError e) {
            throw new TestAbortedException("Update mathUnsignedMultiplyHigh to call Math.unsignedMultiplyHigh");
        }
    }

    @Test
    void testUnsignedMultiplyHigh() {
        assertUnsignedMultiply(UnsignedMultiplyHighSource::unsignedMultiplyHigh, null);
    }

    @Test
    void testUnsignedMultiplyHighWithLow() {
        final long[] lo = {0};
        assertUnsignedMultiply((a, b) -> UnsignedMultiplyHighSource.unsignedMultiplyHigh(a, b, lo), lo);
    }

    private static void assertUnsignedMultiply(LongLongFunction fun, long[] lo) {
        final UniformRandomProvider rng = RandomSource.JSF_64.create();
        for (int i = 0; i < 100; i++) {
            final long a = rng.nextLong();
            final long b = rng.nextLong();
            Assertions.assertEquals(unsignedMultiplyHigh(a, b),
                                    fun.apply(a, b));
            if (lo != null) {
                Assertions.assertEquals(a * b, lo[0]);
            }
        }
    }

    @Test
    void testUnsignedMultiplyHighML() {
        final UniformRandomProvider rng = RandomSource.JSF_64.create();
        final long b = LXMBenchmark.ML;
        for (int i = 0; i < 100; i++) {
            final long a = rng.nextLong();
            Assertions.assertEquals(unsignedMultiplyHigh(a, b),
                UnsignedMultiplyHighSource.unsignedMultiplyHighML(a));
        }
    }

    @Test
    void testUnsignedMultiplyHighWithProducts() {
        assertUnsignedMultiply128((a, b, c, d) ->
            UnsignedMultiplyHighSource.unsignedMultiplyHigh(b, d) +
            a * d + b * c, null);
    }

    @Test
    void testUnsignedMultiply128() {
        final long[] lo = {0};
        assertUnsignedMultiply128((a, b, c, d) -> UnsignedMultiply128Source.unsignedMultiply128(a, b, c, d, lo), lo);
    }

    private static void assertUnsignedMultiply128(LongLongLongLongFunction fun, long[] lo) {
        final UniformRandomProvider rng = RandomSource.JSF_64.create();
        for (int i = 0; i < 100; i++) {
            final long a = rng.nextLong();
            final long b = rng.nextLong();
            final long c = rng.nextLong();
            final long d = rng.nextLong();
            Assertions.assertEquals(unsignedMultiplyHigh(a, b, c, d),
                                    fun.apply(a, b, c, d));
            if (lo != null) {
                Assertions.assertEquals(b * d, lo[0]);
            }
        }
    }

    @Test
    void testUnsignedSquareHigh() {
        final UniformRandomProvider rng = RandomSource.JSF_64.create();
        for (int i = 0; i < 100; i++) {
            final long a = rng.nextLong();
            Assertions.assertEquals(unsignedMultiplyHigh(a, a),
                                    UnsignedMultiply128Source.unsignedSquareHigh(a));
        }
    }

    @Test
    void testUnsignedSquare128() {
        final long[] lo = {0};
        final UniformRandomProvider rng = RandomSource.JSF_64.create();
        for (int i = 0; i < 100; i++) {
            final long a = rng.nextLong();
            final long b = rng.nextLong();
            Assertions.assertEquals(unsignedMultiplyHigh(a, b, a, b),
                                    UnsignedMultiply128Source.unsignedSquare128(a, b, lo));
            Assertions.assertEquals(b * b, lo[0]);
        }
    }

    /**
     * Compute the unsigned multiply of two values using BigInteger.
     *
     * @param a First value
     * @param b Second value
     * @return the upper 64-bits of the 128-bit result
     */
    private static long unsignedMultiplyHigh(long a, long b) {
        final BigInteger ba = toUnsignedBigInteger(a);
        final BigInteger bb = toUnsignedBigInteger(b);
        return ba.multiply(bb).shiftRight(64).longValue();
    }

    /**
     * Compute the unsigned multiply of two 128-bit values using BigInteger.
     *
     * <p>This computes the upper 64-bits of a 128-bit result that would
     * be generated by multiplication to a native 128-bit integer type.
     *
     * @param a First value
     * @param b Second value
     * @return the upper 64-bits of the truncated 128-bit result
     */
    private static long unsignedMultiplyHigh(long ah, long al, long bh, long bl) {
        final BigInteger a = toUnsignedBigInteger(ah, al);
        final BigInteger b = toUnsignedBigInteger(bh, bl);
        return a.multiply(b).shiftRight(64).longValue();
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

    /**
     * Test all 128-bit LCG implementations compute the same state update.
     */
    @RepeatedTest(value = 50)
    void testLcg128() {
        final long ah = RandomSource.createLong();
        final long al = RandomSource.createLong() | 1;
        final LongSupplier a = new LCG128Source.ReferenceLcg128(ah, al)::getAsLong;
        final LongSupplier[] b = new LongSupplier[] {
            new LCG128Source.ReferenceLcg128(ah, al)::getAsLong,
            new LCG128Source.ReferenceLcg128Final(ah, al)::getAsLong,
            new LCG128Source.CompareUnsignedLcg128(ah, al)::getAsLong,
            new LCG128Source.ConditionalLcg128(ah, al)::getAsLong,
            new LCG128Source.ConditionalLcg128Final(ah, al)::getAsLong,
            new LCG128Source.BranchlessLcg128(ah, al)::getAsLong,
            new LCG128Source.BranchlessFullLcg128(ah, al)::getAsLong,
            new LCG128Source.BranchlessFullComposedLcg128(ah, al)::getAsLong,
        };
        for (int i = 0; i < 10; i++) {
            final long expected = a.getAsLong();
            for (int j = 0; j < b.length; j++) {
                Assertions.assertEquals(expected, b[j].getAsLong());
            }
        }
    }

    /**
     * Test all 128-bit LCG based implementation of a LXM generator compute the same state update.
     */
    @RepeatedTest(value = 50)
    void testLxm128() {
        final long ah = RandomSource.createLong();
        final long al = RandomSource.createLong();
        // Native seed: LCG add, LCG state, XBG
        // This requires the initial state of the XBG and LCG.
        final long[] seed = {ah, al,
                             LXM128Source.S0, LXM128Source.S1,
                             LXM128Source.X0, LXM128Source.X1};
        final UniformRandomProvider rng = RandomSource.L128_X128_MIX.create(seed);
        final LongSupplier a = rng::nextLong;
        final LongSupplier[] b = new LongSupplier[] {
            new LXM128Source.ReferenceLxm128(ah, al)::getAsLong,
            new LXM128Source.BranchlessLxm128(ah, al)::getAsLong,
        };
        for (int i = 0; i < 10; i++) {
            final long expected = a.getAsLong();
            for (int j = 0; j < b.length; j++) {
                Assertions.assertEquals(expected, b[j].getAsLong());
            }
        }
    }
}
