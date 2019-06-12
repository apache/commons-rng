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
package org.apache.commons.rng.core.util;

import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link NumberFactory}.
 */
public class NumberFactoryTest {
    /** sizeof(int). */
    private static final int INT_SIZE = 4;
    /** sizeof(long). */
    private static final int LONG_SIZE = 8;

    /** Test values. */
    private static final long[] LONG_TEST_VALUES = new long[] {0L, 1L, -1L, 19337L, 1234567891011213L,
        -11109876543211L, Long.valueOf(Integer.MAX_VALUE), Long.valueOf(Integer.MIN_VALUE), Long.MAX_VALUE,
        Long.MIN_VALUE, 0x9e3779b97f4a7c13L};
    /** Test values. */
    private static final int[] INT_TEST_VALUES = new int[] {0, 1, -1, 19337, 1234567891, -1110987656,
        Integer.MAX_VALUE, Integer.MIN_VALUE, 0x9e3779b9};

    @Test
    public void testMakeBooleanFromInt() {
        // Test if the bit is set differently then the booleans are opposite
        final boolean b1 = NumberFactory.makeBoolean(0);
        final boolean b2 = NumberFactory.makeBoolean(0xffffffff);
        Assert.assertNotEquals(b1, b2);
    }

    @Test
    public void testMakeBooleanFromLong() {
        // Test if the bit is set differently then the booleans are opposite
        final boolean b1 = NumberFactory.makeBoolean(0L);
        final boolean b2 = NumberFactory.makeBoolean(0xffffffffffffffffL);
        Assert.assertNotEquals(b1, b2);
    }

    @Test
    public void testMakeIntFromLong() {
        // Test the high order bits and low order bits are xor'd together
        Assert.assertEquals(0xffffffff, NumberFactory.makeInt(0xffffffff00000000L));
        Assert.assertEquals(0x00000000, NumberFactory.makeInt(0xffffffffffffffffL));
        Assert.assertEquals(0xffffffff, NumberFactory.makeInt(0x00000000ffffffffL));
        Assert.assertEquals(0x00000000, NumberFactory.makeInt(0x0000000000000000L));
        Assert.assertEquals(0x0f0f0f0f, NumberFactory.makeInt(0x0f0f0f0f00000000L));
        Assert.assertEquals(0xf0f0f0f0, NumberFactory.makeInt(0x00000000f0f0f0f0L));
        Assert.assertEquals(0x00000000, NumberFactory.makeInt(0x0f0f0f0f0f0f0f0fL));
        Assert.assertEquals(0xffffffff, NumberFactory.makeInt(0x0f0f0f0ff0f0f0f0L));
    }

    @Test
    public void testExtractLoExtractHi() {
        for (long v : LONG_TEST_VALUES) {
            final int vL = NumberFactory.extractLo(v);
            final int vH = NumberFactory.extractHi(v);

            final long actual = (((long) vH) << 32) | (vL & 0xffffffffL);
            Assert.assertEquals(v, actual);
        }
    }

    @Test
    public void testLong2Long() {
        for (long v : LONG_TEST_VALUES) {
            final int vL = NumberFactory.extractLo(v);
            final int vH = NumberFactory.extractHi(v);

            Assert.assertEquals(v, NumberFactory.makeLong(vH, vL));
        }
    }

    @Test
    public void testLongToByteArraySignificanceOrder() {
        // Start at the least significant bit
        long value = 1;
        for (int i = 0; i < LONG_SIZE; i++) {
            final byte[] b = NumberFactory.makeByteArray(value);
            for (int j = 0; j < LONG_SIZE; j++) {
                // Only one byte should be non zero
                Assert.assertEquals(b[j] != 0, j == i);
            }
            // Shift to the next byte
            value <<= 8;
        }
    }

    @Test
    public void testLongFromByteArray2Long() {
        for (long expected : LONG_TEST_VALUES) {
            final byte[] b = NumberFactory.makeByteArray(expected);
            Assert.assertEquals(expected, NumberFactory.makeLong(b));
        }
    }

    @Test
    public void testLongArrayFromByteArray2LongArray() {
        final byte[] b = NumberFactory.makeByteArray(LONG_TEST_VALUES);
        Assert.assertArrayEquals(LONG_TEST_VALUES, NumberFactory.makeLongArray(b));
    }

    @Test
    public void testLongArrayToByteArrayMatchesLongToByteArray() {
        // Test individually the bytes are the same as the array conversion
        for (int i = 0; i < LONG_TEST_VALUES.length; i++) {
            final byte[] b1 = NumberFactory.makeByteArray(LONG_TEST_VALUES[i]);
            final byte[] b2 = NumberFactory.makeByteArray(new long[] {LONG_TEST_VALUES[i]});
            Assert.assertArrayEquals(b1, b2);
        }
    }

    @Test
    public void testIntToByteArraySignificanceOrder() {
        // Start at the least significant bit
        int value = 1;
        for (int i = 0; i < INT_SIZE; i++) {
            final byte[] b = NumberFactory.makeByteArray(value);
            for (int j = 0; j < INT_SIZE; j++) {
                // Only one byte should be non zero
                Assert.assertEquals(b[j] != 0, j == i);
            }
            // Shift to the next byte
            value <<= 8;
        }
    }

    @Test
    public void testIntFromByteArray2Int() {
        for (int expected : INT_TEST_VALUES) {
            final byte[] b = NumberFactory.makeByteArray(expected);
            Assert.assertEquals(expected, NumberFactory.makeInt(b));
        }
    }

    @Test
    public void testIntArrayFromByteArray2IntArray() {
        final byte[] b = NumberFactory.makeByteArray(INT_TEST_VALUES);
        Assert.assertArrayEquals(INT_TEST_VALUES, NumberFactory.makeIntArray(b));
    }

    @Test
    public void testIntArrayToByteArrayMatchesIntToByteArray() {
        // Test individually the bytes are the same as the array conversion
        for (int i = 0; i < INT_TEST_VALUES.length; i++) {
            final byte[] b1 = NumberFactory.makeByteArray(INT_TEST_VALUES[i]);
            final byte[] b2 = NumberFactory.makeByteArray(new int[] {INT_TEST_VALUES[i]});
            Assert.assertArrayEquals(b1, b2);
        }
    }

    @Test
    public void testMakeIntPrecondition1() {
        for (int i = 0; i <= 10; i++) {
            try {
                NumberFactory.makeInt(new byte[i]);
                if (i != INT_SIZE) {
                    Assert.fail("Exception expected");
                }
            } catch (IllegalArgumentException e) {
                // Expected.
            }
        }
    }

    @Test
    public void testMakeIntArrayPrecondition1() {
        for (int i = 0; i <= 20; i++) {
            try {
                NumberFactory.makeIntArray(new byte[i]);
                if (i != 0 && (i % INT_SIZE != 0)) {
                    Assert.fail("Exception expected");
                }
            } catch (IllegalArgumentException e) {
                // Expected.
            }
        }
    }

    @Test
    public void testMakeLongPrecondition1() {
        for (int i = 0; i <= 10; i++) {
            try {
                NumberFactory.makeLong(new byte[i]);
                if (i != LONG_SIZE) {
                    Assert.fail("Exception expected");
                }
            } catch (IllegalArgumentException e) {
                // Expected.
            }
        }
    }

    @Test
    public void testMakeLongArrayPrecondition1() {
        for (int i = 0; i <= 20; i++) {
            try {
                NumberFactory.makeLongArray(new byte[i]);
                if (i != 0 && (i % LONG_SIZE != 0)) {
                    Assert.fail("Exception expected");
                }
            } catch (IllegalArgumentException e) {
                // Expected.
            }
        }
    }

    /**
     * Test different methods for generation of a {@code float} from a {@code int}. The output
     * value should be in the range between 0 and 1.
     */
    @Test
    public void testFloatGenerationMethods() {
        final int allBits = 0xffffffff;

        // Not capable of generating 1. Set the delta with 1 or 2 ULP of 1.
        assertCloseToNotAbove1((allBits >>> 9) * 0x1.0p-23f, 2);
        assertCloseToNotAbove1((allBits >>> 8) * 0x1.0p-24f, 1);
        assertCloseToNotAbove1(Float.intBitsToFloat(0x7f << 23 | allBits >>> 9) - 1.0f, 2);

        final int noBits = 0;
        Assert.assertEquals(0.0f, (noBits >>> 9) * 0x1.0p-23f, 0);
        Assert.assertEquals(0.0f, (noBits >>> 8) * 0x1.0p-24f, 0);
        Assert.assertEquals(0.0f, Float.intBitsToFloat(0x7f << 23 | noBits >>> 9) - 1.0f, 0);
    }

    /**
     * Test different methods for generation of a {@code double} from a {@code long}. The output
     * value should be in the range between 0 and 1.
     */
    @Test
    public void testDoubleGenerationMethods() {
        final long allBits = 0xffffffffffffffffL;

        // Not capable of generating 1. Set the delta with 1 or 2 ULP of 1.
        assertCloseToNotAbove1((allBits >>> 12) * 0x1.0p-52d, 2);
        assertCloseToNotAbove1((allBits >>> 11) * 0x1.0p-53d, 1);
        assertCloseToNotAbove1(Double.longBitsToDouble(0x3ffL << 52 | allBits >>> 12) - 1.0, 2);

        final long noBits = 0;
        Assert.assertEquals(0.0, (noBits >>> 12) * 0x1.0p-52d, 0);
        Assert.assertEquals(0.0, (noBits >>> 11) * 0x1.0p-53d, 0);
        Assert.assertEquals(0.0, Double.longBitsToDouble(0x3ffL << 52 | noBits >>> 12) - 1.0, 0);
    }

    @Test
    public void testMakeDoubleFromLong() {
        final long allBits = 0xffffffffffffffffL;
        final long noBits = 0;
        // Within 1 ULP of 1.0
        assertCloseToNotAbove1(NumberFactory.makeDouble(allBits), 1);
        Assert.assertEquals(0.0, NumberFactory.makeDouble(noBits), 0);
    }

    @Test
    public void testMakeDoubleFromIntInt() {
        final int allBits = 0xffffffff;
        final int noBits = 0;
        // Within 1 ULP of 1.0
        assertCloseToNotAbove1(NumberFactory.makeDouble(allBits, allBits), 1);
        Assert.assertEquals(0.0, NumberFactory.makeDouble(noBits, noBits), 0);
    }

    @Test
    public void testMakeFloatFromInt() {
        final int allBits = 0xffffffff;
        final int noBits = 0;
        // Within 1 ULP of 1.0f
        assertCloseToNotAbove1(NumberFactory.makeFloat(allBits), 1);
        Assert.assertEquals(0.0f, NumberFactory.makeFloat(noBits), 0);
    }

    /**
     * Assert that the value is close to but <strong>not above</strong> 1. This is used to test
     * the output from methods that produce a {@code float} value that must be in the range
     * between 0 and 1.
     *
     * @param value the value
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point values between x and y.
     * @see Precision#equals(float, float, int)
     */
    private static void assertCloseToNotAbove1(float value, int maxUlps) {
        Assert.assertTrue("Not <= 1.0f", value <= 1.0f);
        Assert.assertTrue("Not equal to 1.0f within units of least precision: " + maxUlps,
                          Precision.equals(1.0f, value, maxUlps));
    }

    /**
     * Assert that the value is close to but <strong>not above</strong> 1. This is used to test
     * the output from methods that produce a {@code double} value that must be in the range
     * between 0 and 1.
     *
     * @param value the value
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point values between x and y.
     * @see Precision#equals(double, double, int)
     */
    private static void assertCloseToNotAbove1(double value, int maxUlps) {
        Assert.assertTrue("Not <= 1.0", value <= 1.0);
        Assert.assertTrue("Not equal to 1.0 within units of least precision: " + maxUlps,
                          Precision.equals(1.0, value, maxUlps));
    }
}
