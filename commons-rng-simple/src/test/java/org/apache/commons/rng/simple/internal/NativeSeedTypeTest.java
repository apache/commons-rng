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
package org.apache.commons.rng.simple.internal;

import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link NativeSeedType} factory seed conversions.
 *
 * <p>Note: All supported types are tested in the {@link NativeSeedTypeParametricTest}.
 */
class NativeSeedTypeTest {
    /**
     * Test the conversion throws for an unsupported seed type.
     * The error message should contain the type, not the string representation of the seed.
     */
    @ParameterizedTest
    @MethodSource
    void testConvertSeedToBytesUsingUnsupportedSeedThrows(Object seed) {
        final UnsupportedOperationException ex = Assertions.assertThrows(
            UnsupportedOperationException.class, () -> NativeSeedType.convertSeedToBytes(seed));
        if (seed == null) {
            Assertions.assertTrue(ex.getMessage().contains("null"));
        } else {
            Assertions.assertTrue(ex.getMessage().contains(seed.getClass().getName()));
        }
    }

    /**
     * Return an array of unsupported seed objects.
     *
     * @return the seeds
     */
    static Object[] testConvertSeedToBytesUsingUnsupportedSeedThrows() {
        return new Object[] {
            null,
            BigDecimal.ONE,
            "The quick brown fox jumped over the lazy dog",
            new Object() {
                @Override
                public String toString() {
                    throw new IllegalStateException("error message should not call toString()");
                }
            }
        };
    }

    /**
     * Test the conversion passes through a byte[]. This hits the edge case of a seed
     * that can be converted that is not a native type.
     */
    @Test
    void testConvertSeedToBytesUsingByteArray() {
        final byte[] seed = {42, 78, 99};
        Assertions.assertSame(seed, NativeSeedType.convertSeedToBytes(seed));
    }

    @ParameterizedTest
    @CsvSource({
        "3, 1",
        "4, 1",
        "5, 1",
        "7, 2",
        "8, 2",
        "9, 2",
        "13, 2",
        "0, 0",
    })
    void testConvertByteArrayToIntArray(int byteSize, int intSize) {
        final byte[] bytes = new byte[byteSize];
        // Get the maximum number of ints to use all the bytes
        final int size = SeedUtils.intSizeFromByteSize(byteSize);
        // If the size is too big, fill the remaining bytes with non-zero values.
        // These should not be used during conversion.
        if (size > intSize) {
            Arrays.fill(bytes, intSize * Integer.BYTES, bytes.length, (byte) -1);
        }
        final int expected = Math.min(size, intSize);
        final int[] ints = (int[]) NativeSeedType.INT_ARRAY.convert(bytes, intSize);
        Assertions.assertEquals(expected, ints.length);
        // The seed should be zero, i.e. extra bytes have not been used
        for (final int i : ints) {
            Assertions.assertEquals(0, i);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "7, 1",
        "8, 1",
        "9, 1",
        "15, 2",
        "16, 2",
        "17, 2",
        "25, 2",
        "0, 0",
    })
    void testConvertByteArrayToLongArray(int byteSize, int longSize) {
        final byte[] bytes = new byte[byteSize];
        // Get the maximum number of longs to use all the bytes
        final long size = SeedUtils.longSizeFromByteSize(byteSize);
        // If the size is too big, fill the remaining bytes with non-zero values.
        // These should not be used during conversion.
        if (size > longSize) {
            Arrays.fill(bytes, longSize * Long.BYTES, bytes.length, (byte) -1);
        }
        final long expected = Math.min(size, longSize);
        final long[] longs = (long[]) NativeSeedType.LONG_ARRAY.convert(bytes, longSize);
        Assertions.assertEquals(expected, longs.length);
        // The seed should be zero, i.e. extra bytes have not been used
        for (final long i : longs) {
            Assertions.assertEquals(0, i);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1",
        "2, 1",
        "3, 1",
        "3, 2",
        "4, 2",
        "5, 2",
        "7, 2",
        "0, 0",
    })
    void testConvertIntArrayToLongArray(int intSize, int longSize) {
        final int[] ints = new int[intSize];
        // Get the maximum number of longs to use all the ints
        final long size = SeedUtils.longSizeFromIntSize(intSize);
        // If the size is too big, fill the remaining ints with non-zero values.
        // These should not be used during conversion.
        if (size > longSize) {
            Arrays.fill(ints, longSize * 2, ints.length, -1);
        }
        final long expected = Math.min(size, longSize);
        final long[] longs = (long[]) NativeSeedType.LONG_ARRAY.convert(ints, longSize);
        Assertions.assertEquals(expected, longs.length);
        // The seed should be zero, i.e. extra ints have not been used
        for (final long i : longs) {
            Assertions.assertEquals(0, i);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "0, 1",
        "1, 1",
        "2, 1",
        "0, 2",
        "1, 2",
        "2, 2",
        "3, 2",
        "0, 0",
    })
    void testConvertLongArrayToIntArray(int longSize, int intSize) {
        final long[] longs = new long[longSize];
        // Get the maximum number of ints to use all the longs
        final int size = longSize * 2;
        // If the size is too big, fill the remaining longs with non-zero values.
        // These should not be used during conversion.
        if (size > intSize) {
            Arrays.fill(longs, SeedUtils.longSizeFromIntSize(intSize), longs.length, -1);
        }
        final int expected = Math.min(size, intSize);
        final int[] ints = (int[]) NativeSeedType.INT_ARRAY.convert(longs, intSize);
        Assertions.assertEquals(expected, ints.length);
        // The seed should be zero, i.e. extra longs have not been used
        for (final int i : ints) {
            Assertions.assertEquals(0, i);
        }
    }
}
