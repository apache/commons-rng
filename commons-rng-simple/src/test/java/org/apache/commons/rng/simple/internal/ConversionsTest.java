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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.core.util.NumberFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link Conversions}.
 */
class ConversionsTest {
    /**
     * Gets the lengths for the byte[] seeds to convert.
     *
     * @return the lengths
     */
    static IntStream getByteLengths() {
        return IntStream.rangeClosed(0, Long.BYTES * 2);
    }

    /**
     * Gets the lengths for the int[] seeds to convert.
     *
     * @return the lengths
     */
    static IntStream getIntLengths() {
        return IntStream.rangeClosed(0, (Long.BYTES / Integer.BYTES) * 2);
    }

    /**
     * Gets the lengths for the long[] seeds to convert.
     *
     * @return the lengths
     */
    static IntStream getLongLengths() {
        return IntStream.rangeClosed(0, 2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, Integer.MAX_VALUE})
    void testIntSizeFromByteSize(int size) {
        Assertions.assertEquals((int) Math.ceil((double) size / Integer.BYTES), Conversions.intSizeFromByteSize(size));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, Integer.MAX_VALUE})
    void testLongSizeFromByteSize(int size) {
        Assertions.assertEquals((int) Math.ceil((double) size / Long.BYTES), Conversions.longSizeFromByteSize(size));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, Integer.MAX_VALUE})
    void testIntSizeFromLongSize(int size) {
        Assertions.assertEquals((int) Math.min(size * 2L, Integer.MAX_VALUE), Conversions.intSizeFromLongSize(size));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, Integer.MAX_VALUE})
    void testLongSizeFromIntSize(int size) {
        Assertions.assertEquals((int) Math.ceil((double) size / 2), Conversions.longSizeFromIntSize(size));
    }

    @RepeatedTest(value = 5)
    void testInt2Long() {
        final int v = ThreadLocalRandom.current().nextInt();
        Assertions.assertEquals(new SplitMix64(v).nextLong(), Conversions.int2Long(v));
    }

    @RepeatedTest(value = 5)
    void testInt2IntArray() {
        final int v = ThreadLocalRandom.current().nextInt();
        getIntLengths().forEach(len -> {
            Assertions.assertArrayEquals(Conversions.long2IntArray(v, len),
                                         Conversions.int2IntArray(v, len));
        });
    }

    @RepeatedTest(value = 5)
    void testInt2LongArray() {
        final int v = ThreadLocalRandom.current().nextInt();
        getIntLengths().forEach(len -> {
            final long[] a = Conversions.int2LongArray(v, len);
            Assertions.assertArrayEquals(Conversions.long2LongArray(v, len), a);
            if (len != 0) {
                // Special case of expansion to length 1
                // Expandion is done by mixing
                Assertions.assertEquals(Conversions.int2Long(v), a[0]);
            }
        });
    }

    @RepeatedTest(value = 5)
    void testLong2Int() {
        final long v = ThreadLocalRandom.current().nextLong();
        Assertions.assertEquals(NumberFactory.makeInt(v), Conversions.long2Int(v));
    }

    @RepeatedTest(value = 5)
    void testLong2IntArray() {
        final long v = ThreadLocalRandom.current().nextLong();
        getIntLengths().forEach(len -> {
            final int longs = Conversions.longSizeFromIntSize(len);
            // Little-endian conversion
            final ByteBuffer bb = ByteBuffer.allocate(longs * Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
            LongStream.generate(new SplitMix64(v)::nextLong).limit(longs).forEach(bb::putLong);
            bb.clear();
            final int[] expected = new int[len];
            for (int i = 0; i < len; i++) {
                expected[i] = bb.getInt();
            }
            Assertions.assertArrayEquals(expected,
                Conversions.long2IntArray(v, len));

            // Note:
            // long -> int[] position[0] != long -> int
            // Reduction is done by folding upper and lower using xor
        });
    }

    @RepeatedTest(value = 5)
    void testLong2LongArray() {
        final long v = ThreadLocalRandom.current().nextLong();
        getIntLengths().forEach(len -> {
            Assertions.assertArrayEquals(LongStream.generate(new SplitMix64(v)::nextLong).limit(len).toArray(),
                Conversions.long2LongArray(v, len));
        });
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntLengths"})
    void testIntArray2Int(int ints) {
        final int[] seed = ThreadLocalRandom.current().ints(ints).toArray();
        // xor all the bytes
        int expected = 0;
        for (final int i : seed) {
            expected ^= i;
        }
        Assertions.assertEquals(expected, Conversions.intArray2Int(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntLengths"})
    void testIntArray2Long(int ints) {
        final int[] seed = ThreadLocalRandom.current().ints(ints).toArray();

        // int[] -> long[] -> long
        // Concatenate all ints in little-endian order to bytes
        final int outLength = Conversions.longSizeFromIntSize(ints);
        final int[] filledSeed = Arrays.copyOf(seed, outLength * 2);
        final ByteBuffer bb = ByteBuffer.allocate(filledSeed.length * Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        Arrays.stream(filledSeed).forEach(bb::putInt);
        // xor all the bytes read as longs
        long expected = 0;
        bb.flip();
        for (int i = outLength; i-- != 0;) {
            final long l = bb.getLong();
            expected ^= l;
        }

        Assertions.assertEquals(expected, Conversions.intArray2Long(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntLengths"})
    void testIntArray2LongComposed(int ints) {
        final int[] seed = ThreadLocalRandom.current().ints(ints).toArray();
        final long expected = new LongArray2Long().convert(new IntArray2LongArray().convert(seed));
        Assertions.assertEquals(expected, Conversions.intArray2Long(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntLengths"})
    void testIntArray2LongArray(int ints) {
        final int[] seed = ThreadLocalRandom.current().ints(ints).toArray();

        // Concatenate all bytes in little-endian order to bytes
        final int outLength = Conversions.longSizeFromIntSize(ints);
        final ByteBuffer bb = ByteBuffer.allocate(outLength * Long.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        Arrays.stream(seed).forEach(bb::putInt);
        bb.clear();
        final long[] expected = new long[outLength];
        for (int i = 0; i < outLength; i++) {
            expected[i] = bb.getLong();
        }

        Assertions.assertArrayEquals(expected, Conversions.intArray2LongArray(seed, outLength));
        // Zero fill
        Assertions.assertArrayEquals(Arrays.copyOf(expected, outLength * 2),
            Conversions.intArray2LongArray(seed, outLength * 2));
        // Truncation
        for (int i = 0; i < outLength; i++) {
            Assertions.assertArrayEquals(Arrays.copyOf(expected, i), Conversions.intArray2LongArray(seed, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getLongLengths"})
    void testLongArray2Int(long longs) {
        final long[] seed = ThreadLocalRandom.current().longs(longs).toArray();
        // xor all the bytes
        long expected = 0;
        for (final long i : seed) {
            expected ^= i;
        }
        Assertions.assertEquals((int) (expected ^ expected >>> 32), Conversions.longArray2Int(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getLongLengths"})
    void testLongArray2Long(long longs) {
        final long[] seed = ThreadLocalRandom.current().longs(longs).toArray();
        // xor all the bytes
        long expected = 0;
        for (final long i : seed) {
            expected ^= i;
        }
        Assertions.assertEquals(expected, Conversions.longArray2Long(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getLongLengths"})
    void testLongArray2IntArray(int longs) {
        final long[] seed = ThreadLocalRandom.current().longs(longs).toArray();

        // Concatenate all bytes in little-endian order to bytes
        final int outLength = Conversions.intSizeFromLongSize(longs);
        final ByteBuffer bb = ByteBuffer.allocate(longs * Long.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        Arrays.stream(seed).forEach(bb::putLong);
        bb.clear();
        final int[] expected = new int[outLength];
        for (int i = 0; i < outLength; i++) {
            expected[i] = bb.getInt();
        }

        Assertions.assertArrayEquals(expected, Conversions.longArray2IntArray(seed, outLength));
        // Zero fill
        Assertions.assertArrayEquals(Arrays.copyOf(expected, outLength * 2),
            Conversions.longArray2IntArray(seed, outLength * 2));
        // Truncation
        for (int i = 0; i < outLength; i++) {
            Assertions.assertArrayEquals(Arrays.copyOf(expected, i), Conversions.longArray2IntArray(seed, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getByteLengths"})
    void testByteArray2Int(int bytes) {
        final byte[] seed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(seed);

        // byte[] -> int[] -> int
        // Concatenate all bytes in little-endian order to bytes
        final int outLength = Conversions.intSizeFromByteSize(bytes);
        final byte[] filledSeed = Arrays.copyOf(seed, outLength * Integer.BYTES);
        final ByteBuffer bb = ByteBuffer.wrap(filledSeed)
                .order(ByteOrder.LITTLE_ENDIAN);
        // xor all the bytes read as ints
        int expected = 0;
        for (int i = outLength; i-- != 0;) {
            final long l = bb.getInt();
            expected ^= l;
        }

        Assertions.assertEquals(expected, Conversions.byteArray2Int(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getByteLengths"})
    void testByteArray2IntComposed(int bytes) {
        final byte[] seed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(seed);
        final int expected = new IntArray2Int().convert(new ByteArray2IntArray().convert(seed));
        Assertions.assertEquals(expected, Conversions.byteArray2Int(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getByteLengths"})
    void testByteArray2IntArray(int bytes) {
        final byte[] seed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(seed);

        // Concatenate all bytes in little-endian order to bytes
        final int outLength = Conversions.intSizeFromByteSize(bytes);
        final byte[] filledSeed = Arrays.copyOf(seed, outLength * Integer.BYTES);
        final ByteBuffer bb = ByteBuffer.wrap(filledSeed)
                .order(ByteOrder.LITTLE_ENDIAN);
        final int[] expected = new int[outLength];
        for (int i = 0; i < outLength; i++) {
            expected[i] = bb.getInt();
        }

        Assertions.assertArrayEquals(expected, Conversions.byteArray2IntArray(seed, outLength));
        // Zero fill
        Assertions.assertArrayEquals(Arrays.copyOf(expected, outLength * 2),
            Conversions.byteArray2IntArray(seed, outLength * 2));
        // Truncation
        for (int i = 0; i < outLength; i++) {
            Assertions.assertArrayEquals(Arrays.copyOf(expected, i), Conversions.byteArray2IntArray(seed, i));
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"getByteLengths"})
    void testByteArray2Long(int bytes) {
        final byte[] seed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(seed);

        // byte[] -> long[] -> long
        // Concatenate all bytes in little-endian order to bytes
        final int outLength = Conversions.longSizeFromByteSize(bytes);
        final byte[] filledSeed = Arrays.copyOf(seed, outLength * Long.BYTES);
        final ByteBuffer bb = ByteBuffer.wrap(filledSeed)
                .order(ByteOrder.LITTLE_ENDIAN);
        // xor all the bytes read as longs
        long expected = 0;
        for (int i = outLength; i-- != 0;) {
            final long l = bb.getLong();
            expected ^= l;
        }

        Assertions.assertEquals(expected, Conversions.byteArray2Long(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getByteLengths"})
    void testByteArray2LongComposed(int bytes) {
        final byte[] seed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(seed);
        final long expected = new LongArray2Long().convert(new ByteArray2LongArray().convert(seed));
        Assertions.assertEquals(expected, Conversions.byteArray2Long(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getByteLengths"})
    void testByteArray2LongArray(int bytes) {
        final byte[] seed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(seed);

        // Concatenate all bytes in little-endian order to bytes
        final int outLength = Conversions.longSizeFromByteSize(bytes);
        final byte[] filledSeed = Arrays.copyOf(seed, outLength * Long.BYTES);
        final ByteBuffer bb = ByteBuffer.wrap(filledSeed)
                .order(ByteOrder.LITTLE_ENDIAN);
        final long[] expected = new long[outLength];
        for (int i = 0; i < outLength; i++) {
            expected[i] = bb.getLong();
        }

        Assertions.assertArrayEquals(expected, Conversions.byteArray2LongArray(seed, outLength));
        // Zero fill
        Assertions.assertArrayEquals(Arrays.copyOf(expected, outLength * 2),
            Conversions.byteArray2LongArray(seed, outLength * 2));
        // Truncation
        for (int i = 0; i < outLength; i++) {
            Assertions.assertArrayEquals(Arrays.copyOf(expected, i), Conversions.byteArray2LongArray(seed, i));
        }
    }
}
