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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.DoubleStream.Builder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the {@link IntJumpDistances} class.
 */
class IntJumpDistancesTest {
    @ParameterizedTest
    @ValueSource(doubles = {-1, -123e45,
        Double.POSITIVE_INFINITY, Double.NaN, Double.NEGATIVE_INFINITY})
    void testWriteUnsignedIntegerThrows(double x) {
        final int[] result = new int[2];
        Assertions.assertThrows(IllegalArgumentException.class, () -> IntJumpDistances.writeUnsignedInteger(x, result));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 0.5, 1, 2, 1.23, 14, 1234L << 30,
        0x1p16, 0x1p32, 0x1.0p52, 0x1.0p56, 0x1.p63, 0x1p66, 0x1.0p128, 0x1p512, 0x1.0p1023,
        1.25687e6, 4.5828e34, 5.678923e190, 9.23678268423647e74,
        Double.MAX_VALUE,
        // Jump values that align with 32-bits
        (1L << 52) + 1,
        ((1L << 52) + 1) * 0x1.0p32,
        ((1L << 52) + 1) * 0x1.0p64,
        // No low 32-bits
        8.307674973655724e34,
        // Random jump values for Philox4x32 that align with 32-bits
        3.839035871160997e35 * 0.25, 4.7836764219450716e35 * 0.25
    })
    @MethodSource
    void testWriteUnsignedInteger(double x) {
        Assertions.assertTrue(x >= 0, "Value must be positive");

        final int[] expected = toIntArray(x);
        // Result array is assumed to be correct length but at least 2
        int[] actual = new int[Math.max(2, 1 + Math.getExponent(x) / 32)];
        //int[] actual = new int[Math.max(2, expected.length)];
        IntJumpDistances.writeUnsignedInteger(x, actual);
        // Trailing zeros should be unwritten
        if (actual.length > expected.length) {
            Assertions.assertEquals(2, actual.length, "Minimum result array size");
            for (int i = expected.length; i < actual.length; i++) {
                Assertions.assertEquals(0, actual[i], "Trailing values should be zero");
            }
            actual = Arrays.copyOf(actual, expected.length);
        }
        Assertions.assertArrayEquals(expected, actual);
    }

    /**
     * Provide a stream of the test values for double conversion to an integer.
     *
     * @return the stream
     */
    static DoubleStream testWriteUnsignedInteger() {
        final SplittableRandom rng = new SplittableRandom();
        final Builder builder = DoubleStream.builder();
        builder.add(Math.nextUp(0x1p63));
        builder.add(Math.nextDown(0x1p64));
        builder.add(Math.nextUp(0x1p64));
        // Create doubles with significand of different lengths.
        // When aligned across boundaries that are a multiple of 32-bits
        // the different length significands test all possible
        // outputs of 1, 2, or 3 int values to represent the significand.
        final long expBits = Double.doubleToRawLongBits(1.0);
        for (final int remove : new int[] {0, 10, 20, 30, 51}) {
            final long mask = ~((1L << remove) - 1);
            for (int i = 0; i < 5; i++) {
                // x in [1, 2)
                final long bits = expBits | (rng.nextLong() >>> 12);
                // Mask out insignificant bits
                double x = Double.longBitsToDouble(bits & mask);
                Assertions.assertTrue(x >= 1.0 && x < 2.0);
                // Representable as a long
                builder.add(x * 0x1.0p53);
                builder.add(x * 0x1.0p51);
                builder.add(x * 0x1.0p25);
                // Scale so the 53-bit significand is aligned across
                // the 2^96 boundary, or just before it.
                x *= 0x1.0p90;
                for (int j = Integer.SIZE; --j >= 0;) {
                    builder.add(x);
                    x *= 2;
                }
            }
        }
        return builder.build();
    }

    /**
     * Convert the {@code value} to an integer representation.
     *
     * <p>The returned value contains the 53-bit significand of the
     * {@code value} converted to an integer. The full representation
     * is a {@code int[]} with the least significant bits first.
     * This array may be padded with leading zeros for large input values.
     * <pre>
     * |----|----|----|---x|xxxx|xxx-|
     * |----|----|xxxx|xxx-|
     * |----|----|xxxx|
     * </pre>
     *
     * @param value Value
     * @return the representation
     */
    private static int[] toIntArray(double value) {
        return toIntArray(new BigDecimal(value).toBigInteger());
    }

    /**
     * Convert the {@code value} to an integer representation.
     *
     * <p>The returned value contains magnitude of the
     * {@code value} converted to an integer. The full representation
     * is a {@code int[]} with the least significant bits first.
     *
     * <p>The special case of zero is returned as [].
     *
     * @param value Value
     * @return the representation
     * @throws AssertionError if the value is negative
     */
    static int[] toIntArray(BigInteger value) {
        // Currently this assumes the value is not negative
        Assertions.assertTrue(value.signum() >= 0, "Value must be positive");

        final int bits = value.bitLength();
        final int n = (int) Math.ceil(bits / 32.0);
        final int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            // The value is positive so the intValue will not
            // add leading 1-bits due to sign extension
            result[i] = value.shiftRight(i * Integer.SIZE).intValue();
        }
        return result;
    }

    /**
     * Convert the {@code value} to a BigInteger.
     *
     * <p>The value contains an unsigned integer magnitude with the least significant bits first.
     *
     * @param value Value
     * @return the BigInteger
     */
    static BigInteger toBigInteger(int[] value) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < value.length; i++) {
            result = result.add(
                BigInteger.valueOf(Integer.toUnsignedLong(value[i])).shiftLeft(i * Integer.SIZE));
        }
        return result;
    }
}
