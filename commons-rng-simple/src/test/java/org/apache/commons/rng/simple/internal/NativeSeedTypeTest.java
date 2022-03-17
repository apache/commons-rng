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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.apache.commons.rng.core.source64.SplitMix64;
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
     * Perform the reference int to long conversion.
     * This may change between release versions.
     * The reference implementation is to create a long using a SplitMix64 generator.
     *
     * @param v Value
     * @return the result
     */
    private static long int2long(int v) {
        return new SplitMix64(v).nextLong();
    }

    /**
     * Perform the reference long to int[] conversion.
     * This may change between release versions.
     * The reference implementation is to create a long[] using a SplitMix64 generator
     * and split each into an int, least significant bytes first.
     *
     * @param v Value
     * @param length Array length
     * @return the result
     */
    private static int[] long2intArray(long v, int length) {
        class LoHiSplitMix64 extends SplitMix64 {
            /** Cache part of the most recently generated long value.
             * Store the upper 32-bits from nextLong() in the lower half
             * and all zero bits in the upper half when cached.
             * Set to -1 when empty and requires a refill. */
            private long next = -1;

            LoHiSplitMix64(long seed) {
                super(seed);
            }

            @Override
            public int nextInt() {
                long l = next;
                if (l < 0) {
                    l = nextLong();
                    // Reserve the upper 32-bits
                    next = l >>> 32;
                    // Return the lower 32-bits
                    return (int) l;
                }
                // Clear cache and return the previous upper 32-bits
                next = -1;
                return (int) l;
            }
        }
        return IntStream.generate(new LoHiSplitMix64(v)::nextInt).limit(length).toArray();
    }

    /**
     * Perform the reference long to long[] conversion.
     * This may change between release versions.
     * The reference implementation is to create a long[] using a SplitMix64 generator.
     *
     * @param v Value
     * @param length Array length
     * @return the result
     */
    private static long[] long2longArray(long v, int length) {
        return LongStream.generate(new SplitMix64(v)::nextLong).limit(length).toArray();
    }

    /**
     * Gets the lengths for the byte[] seeds to convert.
     *
     * @return the lengths
     */
    static IntStream getByteLengths() {
        return IntStream.rangeClosed(0, Long.BYTES * 2);
    }

    /**
     * Gets the int seeds to convert.
     *
     * @return the int seeds
     */
    static IntStream getIntSeeds() {
        return IntStream.of(0, -1, 1267831682, 236786348, -52364);
    }

    /**
     * Gets the long seeds to convert.
     *
     * @return the long seeds
     */
    static LongStream getLongSeeds() {
        return LongStream.of(0, -1, 237848224324L, 6678328688668L, -2783792379423L);
    }

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

    // The following tests define the conversion contract for NativeSeedType.
    //
    // - Native seed types are passed through with no change
    // - long to int conversion uses hi ^ lo
    // - int to long conversion expands the bits.
    //   Creating a long using a SplitMix64 is the reference implementation.
    // - long to long[] conversion seeds a RNG then expands.
    //   Filling using a SplitMix64 is the reference implementation.
    // - long to int[] conversion seeds a RNG then expands.
    //   Filling using a SplitMix64 is the reference implementation.
    // - Primitive expansion should produce equivalent output bits
    //   for all larger output seed types,
    //   i.e. int -> long == int -> int[0]+int[1] == int -> long[0]
    // - int[] to int conversion uses ^ of all the bits
    // - long[] to long conversion uses ^ of all the bits
    // - Array-to-array conversions are little-endian.
    // - Arrays are converted with no zero fill to expand the length.
    // - Array conversion may be full length (F),
    //   or truncated (T) to the required bytes for the native type.
    //
    // Conversions are tested to perform an equivalent to the following operations.
    //
    // Int
    // int    -> int
    // long   -> int
    // int[]  -> int
    // long[] -> F int[] -> int
    // byte[] -> F int[] -> int
    //
    // Long
    // int    -> long
    // long   -> long
    // int[]  -> F long[] -> long
    // long[] -> long
    // byte[] -> F long[] -> int
    //
    // int[]
    // int    -> int[]
    // long   -> int[]
    // int[]  -> int[]
    // long[] -> T int[]
    // byte[] -> T int[]
    //
    // int[]
    // int    -> long[]
    // long   -> long[]
    // int[]  -> T long[]
    // long[] -> long[]
    // byte[] -> T long[]
    //
    // Notes:
    // 1. The actual implementation may be optimised to avoid redundant steps.
    // 2. Primitive type native seed use all bits from an array (F).
    // 3. Array type native seeds use only the initial n bytes from an array (T) required
    //    to satisfy the native seed length n
    // 4. Expansion of primitive seeds to a different native type should be consistent.
    //    Seeding an integer to create an int[] should match the byte output of
    //    using the same integer (or long) to create a long[] of equivalent length

    @ParameterizedTest
    @MethodSource(value = {"getIntSeeds"})
    void testPrimitiveSeedExpansion(int seed) {
        for (int i = 1; i < 3; i++) {
            final long l1 = (Long) NativeSeedType.LONG.convert(seed, i);
            final int[] ia1 = (int[]) NativeSeedType.INT_ARRAY.convert(seed, i * 2);
            final long[] la1 = (long[]) NativeSeedType.LONG_ARRAY.convert(seed, i);
            final int[] ia2 = (int[]) NativeSeedType.INT_ARRAY.convert(Long.valueOf(seed), i * 2);
            final long[] la2 = (long[]) NativeSeedType.LONG_ARRAY.convert(Long.valueOf(seed), i);
            Assertions.assertEquals(i, la1.length);
            Assertions.assertEquals(l1, la1[0], "int -> long != int -> long[0]");
            Assertions.assertArrayEquals(ia1, ia2, "int -> int[] != long -> int[]");
            Assertions.assertArrayEquals(la1, la2, "int -> long[] != long -> long[]");
            final ByteBuffer bb = ByteBuffer.allocate(i * Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
            Arrays.stream(ia1).forEach(bb::putInt);
            bb.flip();
            for (int j = 0; j < i; j++) {
                Assertions.assertEquals(bb.getLong(), la1[j]);
            }
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntSeeds"})
    void testIntNativeSeedWithInt(int seed) {
        // Native type
        final Integer s = Integer.valueOf(seed);
        for (int i = 0; i < 3; i++) {
            Assertions.assertSame(s, NativeSeedType.INT.convert(s, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntSeeds"})
    void testIntNativeSeedWithLong(long seed) {
        // Primitive conversion: Note: >>> takes precendence over ^
        final Integer l = (int) (seed ^ seed >>> 32);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(l, (Integer) NativeSeedType.INT.convert(seed, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getByteLengths"})
    void testIntNativeSeedWithArrays(int bytes) {
        final byte[] byteSeed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(byteSeed);

        // Get the bytes as each array type
        final int longSize = SeedUtils.longSizeFromByteSize(bytes);
        final int intSize = SeedUtils.intSizeFromByteSize(bytes);
        final ByteBuffer bb = ByteBuffer.wrap(
                Arrays.copyOf(byteSeed, longSize * Long.BYTES))
                .order(ByteOrder.LITTLE_ENDIAN);
        final long[] longSeed = new long[longSize];
        for (int i = 0; i < longSeed.length; i++) {
            longSeed[i] = bb.getLong();
        }
        bb.clear();
        final int[] intSeed = new int[intSize];
        int expected = 0;
        for (int i = 0; i < intSeed.length; i++) {
            intSeed[i] = bb.getInt();
            expected ^= intSeed[i];
        }

        // Length parameter is ignored and full bytes are always used
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(expected, NativeSeedType.INT.convert(byteSeed, i));
            Assertions.assertEquals(expected, NativeSeedType.INT.convert(intSeed, i));
            Assertions.assertEquals(expected, NativeSeedType.INT.convert(longSeed, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntSeeds"})
    void testLongNativeSeedWithInt(int seed) {
        // Primitive conversion.
        final Long l = int2long(seed);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(l, (Long) NativeSeedType.LONG.convert(seed, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getLongSeeds"})
    void testLongNativeSeedWithLong(long seed) {
        // Native type
        final Long s = Long.valueOf(seed);
        for (int i = 0; i < 3; i++) {
            Assertions.assertSame(s, NativeSeedType.LONG.convert(s, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getByteLengths"})
    void testLongNativeSeedWithArrays(int bytes) {
        final byte[] byteSeed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(byteSeed);

        // Get the bytes as each array type
        final int longSize = SeedUtils.longSizeFromByteSize(bytes);
        final int intSize = SeedUtils.intSizeFromByteSize(bytes);
        final ByteBuffer bb = ByteBuffer.wrap(
                Arrays.copyOf(byteSeed, longSize * Long.BYTES))
                .order(ByteOrder.LITTLE_ENDIAN);
        final long[] longSeed = new long[longSize];
        long expected = 0;
        for (int i = 0; i < longSeed.length; i++) {
            longSeed[i] = bb.getLong();
            expected ^= longSeed[i];
        }
        bb.clear();
        final int[] intSeed = new int[intSize];
        for (int i = 0; i < intSeed.length; i++) {
            intSeed[i] = bb.getInt();
        }

        // Length parameter is ignored and full bytes are always used
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(expected, NativeSeedType.LONG.convert(byteSeed, i));
            Assertions.assertEquals(expected, NativeSeedType.LONG.convert(intSeed, i));
            Assertions.assertEquals(expected, NativeSeedType.LONG.convert(longSeed, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntSeeds"})
    void testIntArrayNativeSeedWithInt(int seed) {
        // Full-length conversion
        for (int i = 0; i < 3; i++) {
            // Note: int seed is expanded using the same method as a long seed
            Assertions.assertArrayEquals(long2intArray(seed, i),
                (int[]) NativeSeedType.INT_ARRAY.convert(seed, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getLongSeeds"})
    void testIntArrayNativeSeedWithLong(long seed) {
        // Full-length conversion
        for (int i = 0; i < 3; i++) {
            Assertions.assertArrayEquals(long2intArray(seed, i),
                (int[]) NativeSeedType.INT_ARRAY.convert(seed, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getByteLengths"})
    void testIntArrayNativeSeedWithArrays(int bytes) {
        final byte[] byteSeed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(byteSeed);

        // Get the bytes as each array type
        final int longSize = SeedUtils.longSizeFromByteSize(bytes);
        final int intSize = SeedUtils.intSizeFromByteSize(bytes);
        final ByteBuffer bb = ByteBuffer.wrap(
                Arrays.copyOf(byteSeed, longSize * Long.BYTES))
                .order(ByteOrder.LITTLE_ENDIAN);
        final long[] longSeed = new long[longSize];
        for (int i = 0; i < longSeed.length; i++) {
            longSeed[i] = bb.getLong();
        }
        bb.clear();
        final int[] intSeed = new int[intSize];
        for (int i = 0; i < intSeed.length; i++) {
            intSeed[i] = bb.getInt();
        }

        // Native type
        Assertions.assertSame(intSeed, NativeSeedType.INT_ARRAY.convert(intSeed, 0));
        Assertions.assertSame(intSeed, NativeSeedType.INT_ARRAY.convert(intSeed, intSize));
        Assertions.assertSame(intSeed, NativeSeedType.INT_ARRAY.convert(intSeed, intSize * 2));

        // Full-length conversion
        Assertions.assertArrayEquals(intSeed, (int[]) NativeSeedType.INT_ARRAY.convert(byteSeed, intSize));
        Assertions.assertArrayEquals(intSeed, (int[]) NativeSeedType.INT_ARRAY.convert(longSeed, intSize));
        // Truncation
        Assertions.assertArrayEquals(new int[0], (int[]) NativeSeedType.INT_ARRAY.convert(byteSeed, 0));
        Assertions.assertArrayEquals(new int[0], (int[]) NativeSeedType.INT_ARRAY.convert(longSeed, 0));
        if (intSize != 0) {
            Assertions.assertArrayEquals(Arrays.copyOf(intSeed, 1), (int[]) NativeSeedType.INT_ARRAY.convert(byteSeed, 1));
            Assertions.assertArrayEquals(Arrays.copyOf(intSeed, 1), (int[]) NativeSeedType.INT_ARRAY.convert(longSeed, 1));
        }
        // No zero-fill (only use the bytes in the input seed)
        Assertions.assertArrayEquals(intSeed, (int[]) NativeSeedType.INT_ARRAY.convert(byteSeed, intSize * 2));
        Assertions.assertArrayEquals(Arrays.copyOf(intSeed, longSize * 2), (int[]) NativeSeedType.INT_ARRAY.convert(longSeed, intSize * 2));
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntSeeds"})
    void testLongArrayNativeSeedWithInt(int seed) {
        // Full-length conversion
        for (int i = 0; i < 3; i++) {
            // Note: int seed is expanded using the same method as a long seed
            Assertions.assertArrayEquals(long2longArray(seed, i),
                (long[]) NativeSeedType.LONG_ARRAY.convert(seed, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getLongSeeds"})
    void testLongArrayNativeSeedWithLong(long seed) {
        // Full-length conversion
        for (int i = 0; i < 3; i++) {
            Assertions.assertArrayEquals(long2longArray(seed, i),
                (long[]) NativeSeedType.LONG_ARRAY.convert(seed, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getByteLengths"})
    void testLongArrayNativeSeedWithArrays(int bytes) {
        final byte[] byteSeed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(byteSeed);

        // Get the bytes as each array type
        final int longSize = SeedUtils.longSizeFromByteSize(bytes);
        final int intSize = SeedUtils.intSizeFromByteSize(bytes);
        final ByteBuffer bb = ByteBuffer.wrap(
                Arrays.copyOf(byteSeed, longSize * Long.BYTES))
                .order(ByteOrder.LITTLE_ENDIAN);
        final long[] longSeed = new long[longSize];
        for (int i = 0; i < longSeed.length; i++) {
            longSeed[i] = bb.getLong();
        }
        bb.clear();
        final int[] intSeed = new int[intSize];
        for (int i = 0; i < intSeed.length; i++) {
            intSeed[i] = bb.getInt();
        }

        // Native type
        Assertions.assertSame(longSeed, NativeSeedType.LONG_ARRAY.convert(longSeed, 0));
        Assertions.assertSame(longSeed, NativeSeedType.LONG_ARRAY.convert(longSeed, longSize));
        Assertions.assertSame(longSeed, NativeSeedType.LONG_ARRAY.convert(longSeed, longSize * 2));

        // Full-length conversion
        Assertions.assertArrayEquals(longSeed, (long[]) NativeSeedType.LONG_ARRAY.convert(byteSeed, longSize));
        Assertions.assertArrayEquals(longSeed, (long[]) NativeSeedType.LONG_ARRAY.convert(intSeed, longSize));
        // Truncation
        Assertions.assertArrayEquals(new long[0], (long[]) NativeSeedType.LONG_ARRAY.convert(byteSeed, 0));
        Assertions.assertArrayEquals(new long[0], (long[]) NativeSeedType.LONG_ARRAY.convert(intSeed, 0));
        if (longSize != 0) {
            Assertions.assertArrayEquals(Arrays.copyOf(longSeed, 1), (long[]) NativeSeedType.LONG_ARRAY.convert(byteSeed, 1));
            Assertions.assertArrayEquals(Arrays.copyOf(longSeed, 1), (long[]) NativeSeedType.LONG_ARRAY.convert(intSeed, 1));
        }
        // No zero-fill (only use the bytes in the input seed)
        Assertions.assertArrayEquals(longSeed, (long[]) NativeSeedType.LONG_ARRAY.convert(byteSeed, longSize * 2));
        Assertions.assertArrayEquals(longSeed, (long[]) NativeSeedType.LONG_ARRAY.convert(intSeed, longSize * 2));
    }
}
