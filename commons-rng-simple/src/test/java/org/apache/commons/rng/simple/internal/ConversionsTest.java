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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

    @ParameterizedTest
    @MethodSource(value = {"getIntLengths"})
    void testByteArray2Int(int bytes) {
        final byte[] seed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(seed);

        // byte[] -> int[] -> int
        // Concatenate all bytes in little-endian order to bytes
        final int outLength = SeedUtils.intSizeFromByteSize(bytes);
        final byte[] filledSeed = Arrays.copyOf(seed, outLength * Integer.BYTES);
        final ByteBuffer bb = ByteBuffer.wrap(filledSeed)
                .order(ByteOrder.LITTLE_ENDIAN);
        // xor all the bytes read as ints
        int expected = 0;
        for (int i = outLength; i-- != 0;) {
            long l = bb.getInt();
            expected ^= l;
        }

        Assertions.assertEquals(expected, Conversions.byteArray2Int(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntLengths"})
    void testByteArray2IntComposed(int bytes) {
        final byte[] seed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(seed);
        final int expected = new IntArray2Int().convert(new ByteArray2IntArray().convert(seed));
        Assertions.assertEquals(expected, Conversions.byteArray2Int(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntLengths"})
    void testByteArray2Long(int bytes) {
        final byte[] seed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(seed);

        // byte[] -> long[] -> long
        // Concatenate all bytes in little-endian order to bytes
        final int outLength = SeedUtils.longSizeFromByteSize(bytes);
        final byte[] filledSeed = Arrays.copyOf(seed, outLength * Long.BYTES);
        final ByteBuffer bb = ByteBuffer.wrap(filledSeed)
                .order(ByteOrder.LITTLE_ENDIAN);
        // xor all the bytes read as longs
        long expected = 0;
        for (int i = outLength; i-- != 0;) {
            long l = bb.getLong();
            expected ^= l;
        }

        Assertions.assertEquals(expected, Conversions.byteArray2Long(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntLengths"})
    void testByteArray2LongComposed(int bytes) {
        final byte[] seed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(seed);
        final long expected = new LongArray2Long().convert(new ByteArray2LongArray().convert(seed));
        Assertions.assertEquals(expected, Conversions.byteArray2Long(seed));
    }

    @ParameterizedTest
    @MethodSource(value = {"getIntLengths"})
    void testIntArray2Long(int ints) {
        final int[] seed = ThreadLocalRandom.current().ints(ints).toArray();

        // int[] -> long[] -> long
        // Concatenate all ints in little-endian order to bytes
        final int outLength = SeedUtils.longSizeFromIntSize(ints);
        final int[] filledSeed = Arrays.copyOf(seed, outLength * 2);
        final ByteBuffer bb = ByteBuffer.allocate(filledSeed.length * Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        Arrays.stream(filledSeed).forEach(bb::putInt);
        // xor all the bytes read as longs
        long expected = 0;
        bb.flip();
        for (int i = outLength; i-- != 0;) {
            long l = bb.getLong();
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
}
