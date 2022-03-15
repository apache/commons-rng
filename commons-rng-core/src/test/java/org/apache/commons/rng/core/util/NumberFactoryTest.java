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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.apache.commons.math3.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link NumberFactory}.
 */
class NumberFactoryTest {
    /** sizeof(int) in bytes. */
    private static final int INT_SIZE = Integer.BYTES;
    /** sizeof(long) in bytes. */
    private static final int LONG_SIZE = Long.BYTES;

    /** Test values. */
    private static final long[] LONG_TEST_VALUES = new long[] {0L, 1L, -1L, 19337L, 1234567891011213L,
        -11109876543211L, Integer.MAX_VALUE, Integer.MIN_VALUE, Long.MAX_VALUE,
        Long.MIN_VALUE, 0x9e3779b97f4a7c13L};
    /** Test values. */
    private static final int[] INT_TEST_VALUES = new int[] {0, 1, -1, 19337, 1234567891, -1110987656,
        Integer.MAX_VALUE, Integer.MIN_VALUE, 0x9e3779b9};

    /**
     * Provide a stream of the test values for long conversion.
     *
     * @return the stream
     */
    static LongStream longTestValues() {
        return Arrays.stream(LONG_TEST_VALUES);
    }

    /**
     * Provide a stream of the test values for long conversion.
     *
     * @return the stream
     */
    static IntStream intTestValues() {
        return Arrays.stream(INT_TEST_VALUES);
    }

    @ParameterizedTest
    @MethodSource(value = {"intTestValues"})
    void testMakeBooleanFromInt(int v) {
        // Test if the bit is set differently then the booleans are opposite
        final boolean b1 = NumberFactory.makeBoolean(v);
        final boolean b2 = NumberFactory.makeBoolean(~v);
        Assertions.assertNotEquals(b1, b2);
    }

    @ParameterizedTest
    @MethodSource(value = {"longTestValues"})
    void testMakeBooleanFromLong(long v) {
        // Test if the bit is set differently then the booleans are opposite
        final boolean b1 = NumberFactory.makeBoolean(v);
        final boolean b2 = NumberFactory.makeBoolean(~v);
        Assertions.assertNotEquals(b1, b2);
    }

    @Test
    void testMakeIntFromLong() {
        // Test the high order bits and low order bits are xor'd together
        Assertions.assertEquals(0xffffffff, NumberFactory.makeInt(0xffffffff00000000L));
        Assertions.assertEquals(0x00000000, NumberFactory.makeInt(0xffffffffffffffffL));
        Assertions.assertEquals(0xffffffff, NumberFactory.makeInt(0x00000000ffffffffL));
        Assertions.assertEquals(0x00000000, NumberFactory.makeInt(0x0000000000000000L));
        Assertions.assertEquals(0x0f0f0f0f, NumberFactory.makeInt(0x0f0f0f0f00000000L));
        Assertions.assertEquals(0xf0f0f0f0, NumberFactory.makeInt(0x00000000f0f0f0f0L));
        Assertions.assertEquals(0x00000000, NumberFactory.makeInt(0x0f0f0f0f0f0f0f0fL));
        Assertions.assertEquals(0xffffffff, NumberFactory.makeInt(0x0f0f0f0ff0f0f0f0L));
    }

    @ParameterizedTest
    @MethodSource(value = {"longTestValues"})
    void testExtractLoExtractHi(long v) {
        final int vL = NumberFactory.extractLo(v);
        final int vH = NumberFactory.extractHi(v);

        final long actual = (((long) vH) << 32) | (vL & 0xffffffffL);
        Assertions.assertEquals(v, actual);
    }

    @ParameterizedTest
    @MethodSource(value = {"longTestValues"})
    void testLong2Long(long v) {
        final int vL = NumberFactory.extractLo(v);
        final int vH = NumberFactory.extractHi(v);

        Assertions.assertEquals(v, NumberFactory.makeLong(vH, vL));
    }

    @Test
    void testLongToByteArraySignificanceOrder() {
        // Start at the least significant bit
        long value = 1;
        for (int i = 0; i < LONG_SIZE; i++) {
            final byte[] b = NumberFactory.makeByteArray(value);
            for (int j = 0; j < LONG_SIZE; j++) {
                // Only one byte should be non zero
                Assertions.assertEquals(b[j] != 0, j == i);
            }
            // Shift to the next byte
            value <<= 8;
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"longTestValues"})
    void testLongToBytesIsLittleEndian(long v) {
        final ByteBuffer bb = ByteBuffer.allocate(LONG_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        bb.putLong(v);
        Assertions.assertArrayEquals(bb.array(), NumberFactory.makeByteArray(v));
    }

    @RepeatedTest(value = 5)
    void testByteArrayToLongArrayIsLittleEndian() {
        final int n = 5;
        byte[] bytes = new byte[n * LONG_SIZE];
        ThreadLocalRandom.current().nextBytes(bytes);
        final ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        final long[] data = NumberFactory.makeLongArray(bytes);
        for (int i = 0; i < n; i++) {
            Assertions.assertEquals(bb.getLong(), data[i]);
        }
        Assertions.assertArrayEquals(bytes, NumberFactory.makeByteArray(data));
    }

    @ParameterizedTest
    @MethodSource(value = {"longTestValues"})
    void testLongFromByteArray2Long(long expected) {
        final byte[] b = NumberFactory.makeByteArray(expected);
        Assertions.assertEquals(expected, NumberFactory.makeLong(b));
    }

    @Test
    void testLongArrayFromByteArray2LongArray() {
        final byte[] b = NumberFactory.makeByteArray(LONG_TEST_VALUES);
        Assertions.assertArrayEquals(LONG_TEST_VALUES, NumberFactory.makeLongArray(b));
    }

    @ParameterizedTest
    @MethodSource(value = {"longTestValues"})
    void testLongArrayToByteArrayMatchesLongToByteArray(long v) {
        // Test individually the bytes are the same as the array conversion
        final byte[] b1 = NumberFactory.makeByteArray(v);
        final byte[] b2 = NumberFactory.makeByteArray(new long[] {v});
        Assertions.assertArrayEquals(b1, b2);
    }

    @Test
    void testIntToByteArraySignificanceOrder() {
        // Start at the least significant bit
        int value = 1;
        for (int i = 0; i < INT_SIZE; i++) {
            final byte[] b = NumberFactory.makeByteArray(value);
            for (int j = 0; j < INT_SIZE; j++) {
                // Only one byte should be non zero
                Assertions.assertEquals(b[j] != 0, j == i);
            }
            // Shift to the next byte
            value <<= 8;
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"intTestValues"})
    void testIntToBytesIsLittleEndian(int v) {
        final ByteBuffer bb = ByteBuffer.allocate(INT_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(v);
        Assertions.assertArrayEquals(bb.array(), NumberFactory.makeByteArray(v));
    }

    @RepeatedTest(value = 5)
    void testByteArrayToIntArrayIsLittleEndian() {
        final int n = 5;
        byte[] bytes = new byte[n * INT_SIZE];
        ThreadLocalRandom.current().nextBytes(bytes);
        final ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        final int[] data = NumberFactory.makeIntArray(bytes);
        for (int i = 0; i < n; i++) {
            Assertions.assertEquals(bb.getInt(), data[i]);
        }
        Assertions.assertArrayEquals(bytes, NumberFactory.makeByteArray(data));
    }

    @ParameterizedTest
    @MethodSource(value = {"intTestValues"})
    void testIntFromByteArray2Int(int expected) {
        final byte[] b = NumberFactory.makeByteArray(expected);
        Assertions.assertEquals(expected, NumberFactory.makeInt(b));
    }

    @Test
    void testIntArrayFromByteArray2IntArray() {
        final byte[] b = NumberFactory.makeByteArray(INT_TEST_VALUES);
        Assertions.assertArrayEquals(INT_TEST_VALUES, NumberFactory.makeIntArray(b));
    }

    @ParameterizedTest
    @MethodSource(value = {"intTestValues"})
    void testIntArrayToByteArrayMatchesIntToByteArray(int v) {
        // Test individually the bytes are the same as the array conversion
        final byte[] b1 = NumberFactory.makeByteArray(v);
        final byte[] b2 = NumberFactory.makeByteArray(new int[] {v});
        Assertions.assertArrayEquals(b1, b2);
    }

    @Test
    void testMakeIntPrecondition1() {
        for (int i = 0; i <= 10; i++) {
            final byte[] bytes = new byte[i];
            if (i != INT_SIZE) {
                Assertions.assertThrows(IllegalArgumentException.class,
                    () -> NumberFactory.makeInt(bytes));
            } else {
                Assertions.assertEquals(0, NumberFactory.makeInt(bytes));
            }
        }
    }

    @Test
    void testMakeIntArrayPrecondition1() {
        for (int i = 0; i <= 20; i++) {
            final byte[] bytes = new byte[i];
            if (i != 0 && i % INT_SIZE != 0) {
                Assertions.assertThrows(IllegalArgumentException.class,
                    () -> NumberFactory.makeIntArray(bytes));
            } else {
                Assertions.assertArrayEquals(new int[i / INT_SIZE], NumberFactory.makeIntArray(bytes));
            }
        }
    }

    @Test
    void testMakeLongPrecondition1() {
        for (int i = 0; i <= 10; i++) {
            final byte[] bytes = new byte[i];
            if (i != LONG_SIZE) {
                Assertions.assertThrows(IllegalArgumentException.class,
                    () -> NumberFactory.makeLong(bytes));
            } else {
                Assertions.assertEquals(0L, NumberFactory.makeLong(bytes));
            }
        }
    }

    @Test
    void testMakeLongArrayPrecondition1() {
        for (int i = 0; i <= 20; i++) {
            final byte[] bytes = new byte[i];
            if (i != 0 && i % LONG_SIZE != 0) {
                Assertions.assertThrows(IllegalArgumentException.class,
                    () -> NumberFactory.makeLongArray(bytes));
            } else {
                Assertions.assertArrayEquals(new long[i / LONG_SIZE], NumberFactory.makeLongArray(bytes));
            }
        }
    }

    /**
     * Test different methods for generation of a {@code float} from a {@code int}. The output
     * value should be in the range between 0 and 1.
     */
    @Test
    void testFloatGenerationMethods() {
        final int allBits = 0xffffffff;

        // Not capable of generating 1. Set the delta with 1 or 2 ULP of 1.
        assertCloseToNotAbove1((allBits >>> 9) * 0x1.0p-23f, 2);
        assertCloseToNotAbove1((allBits >>> 8) * 0x1.0p-24f, 1);
        assertCloseToNotAbove1(Float.intBitsToFloat(0x7f << 23 | allBits >>> 9) - 1.0f, 2);

        final int noBits = 0;
        Assertions.assertEquals(0.0f, (noBits >>> 9) * 0x1.0p-23f);
        Assertions.assertEquals(0.0f, (noBits >>> 8) * 0x1.0p-24f);
        Assertions.assertEquals(0.0f, Float.intBitsToFloat(0x7f << 23 | noBits >>> 9) - 1.0f);
    }

    /**
     * Test different methods for generation of a {@code double} from a {@code long}. The output
     * value should be in the range between 0 and 1.
     */
    @Test
    void testDoubleGenerationMethods() {
        final long allBits = 0xffffffffffffffffL;

        // Not capable of generating 1. Set the delta with 1 or 2 ULP of 1.
        assertCloseToNotAbove1((allBits >>> 12) * 0x1.0p-52d, 2);
        assertCloseToNotAbove1((allBits >>> 11) * 0x1.0p-53d, 1);
        assertCloseToNotAbove1(Double.longBitsToDouble(0x3ffL << 52 | allBits >>> 12) - 1.0, 2);

        final long noBits = 0;
        Assertions.assertEquals(0.0, (noBits >>> 12) * 0x1.0p-52d);
        Assertions.assertEquals(0.0, (noBits >>> 11) * 0x1.0p-53d);
        Assertions.assertEquals(0.0, Double.longBitsToDouble(0x3ffL << 52 | noBits >>> 12) - 1.0);
    }

    @Test
    void testMakeDoubleFromLong() {
        final long allBits = 0xffffffffffffffffL;
        final long noBits = 0;
        // Within 1 ULP of 1.0
        assertCloseToNotAbove1(NumberFactory.makeDouble(allBits), 1);
        Assertions.assertEquals(0.0, NumberFactory.makeDouble(noBits));
    }

    @Test
    void testMakeDoubleFromIntInt() {
        final int allBits = 0xffffffff;
        final int noBits = 0;
        // Within 1 ULP of 1.0
        assertCloseToNotAbove1(NumberFactory.makeDouble(allBits, allBits), 1);
        Assertions.assertEquals(0.0, NumberFactory.makeDouble(noBits, noBits));
    }

    @Test
    void testMakeFloatFromInt() {
        final int allBits = 0xffffffff;
        final int noBits = 0;
        // Within 1 ULP of 1.0f
        assertCloseToNotAbove1(NumberFactory.makeFloat(allBits), 1);
        Assertions.assertEquals(0.0f, NumberFactory.makeFloat(noBits));
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
        Assertions.assertTrue(value <= 1.0f, "Not <= 1.0f");
        Assertions.assertTrue(Precision.equals(1.0f, value, maxUlps),
            () -> "Not equal to 1.0f within units of least precision: " + maxUlps);
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
        Assertions.assertTrue(value <= 1.0, "Not <= 1.0");
        Assertions.assertTrue(Precision.equals(1.0, value, maxUlps),
            () -> "Not equal to 1.0 within units of least precision: " + maxUlps);
    }
}
