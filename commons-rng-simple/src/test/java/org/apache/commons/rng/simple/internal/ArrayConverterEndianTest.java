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
 * Tests the endian conversion of all array converters.
 */
class ArrayConverterEndianTest {
    /**
     * Gets the lengths for the byte[] seeds.
     *
     * @return the lengths
     */
    static IntStream getLengths() {
        return IntStream.rangeClosed(0, 16);
    }

    @ParameterizedTest
    @MethodSource(value = {"getLengths"})
    void testLittleEndian(int bytes) {
        final byte[] seedBytes = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(seedBytes);

        // Reference implementation using a ByteBuffer
        final ByteBuffer bb = ByteBuffer.wrap(
            Arrays.copyOf(seedBytes, Conversions.longSizeFromByteSize(bytes) * Long.BYTES))
            .order(ByteOrder.LITTLE_ENDIAN);

        // byte[] -> int[]
        final int[] expectedInt = new int[Conversions.intSizeFromByteSize(bytes)];
        for (int i = 0; i < expectedInt.length; i++) {
            expectedInt[i] = bb.getInt();
        }
        Assertions.assertArrayEquals(expectedInt, new ByteArray2IntArray().convert(seedBytes));
        Assertions.assertArrayEquals(expectedInt, (int[]) NativeSeedType.INT_ARRAY.convert(seedBytes, expectedInt.length));

        // byte[] -> long[]
        bb.clear();
        final long[] expectedLong = new long[Conversions.longSizeFromByteSize(bytes)];
        for (int i = 0; i < expectedLong.length; i++) {
            expectedLong[i] = bb.getLong();
        }
        Assertions.assertArrayEquals(expectedLong, new ByteArray2LongArray().convert(seedBytes));
        Assertions.assertArrayEquals(expectedLong, (long[]) NativeSeedType.LONG_ARRAY.convert(seedBytes, expectedLong.length));

        // int[] -> long[]
        Assertions.assertArrayEquals(expectedLong, new IntArray2LongArray().convert(expectedInt));
        Assertions.assertArrayEquals(expectedLong, (long[]) NativeSeedType.LONG_ARRAY.convert(expectedInt, expectedLong.length));

        // long[] -> int[]
        Assertions.assertArrayEquals(expectedInt, new LongArray2IntArray().convert(expectedLong, expectedInt.length));
        Assertions.assertArrayEquals(expectedInt, (int[]) NativeSeedType.INT_ARRAY.convert(expectedLong, expectedInt.length));
    }
}
