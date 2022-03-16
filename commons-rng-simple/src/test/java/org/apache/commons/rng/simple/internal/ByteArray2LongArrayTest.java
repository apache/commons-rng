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

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import org.apache.commons.rng.core.util.NumberFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link ByteArray2LongArray} converter.
 */
class ByteArray2LongArrayTest {
    /**
     * Gets the lengths for the byte[] seeds to convert.
     *
     * @return the lengths
     */
    static IntStream getLengths() {
        return IntStream.rangeClosed(0, Long.BYTES * 2);
    }

    /**
     * Gets the expected output length.
     *
     * @param bytes Number of bytes
     * @return the output length
     */
    private static int getOutputLength(int bytes) {
        return (int) Math.ceil((double) bytes / Long.BYTES);
    }

    @ParameterizedTest
    @MethodSource(value = {"getLengths"})
    void testSeedSizeIsMultipleOfIntSize(int bytes) {
        final byte[] seed = new byte[bytes];

        // This calls convert without a length
        final long[] out = new ByteArray2LongArray().convert(seed);
        Assertions.assertEquals(getOutputLength(bytes), out.length);
    }

    @ParameterizedTest
    @MethodSource(value = {"getLengths"})
    void testSeedConversion(int bytes) {
        assertSeedConversion(bytes, getOutputLength(bytes));
    }

    @ParameterizedTest
    @MethodSource(value = {"getLengths"})
    void testFilledSeedConversion(int bytes) {
        assertSeedConversion(bytes, bytes / 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"getLengths"})
    void testTruncatedSeedConversion(int bytes) {
        assertSeedConversion(bytes, bytes * 3);
    }

    private static void assertSeedConversion(int bytes, int outLength) {
        final byte[] seed = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(seed);
        final byte[] filledSeed = Arrays.copyOf(seed, outLength * Long.BYTES);
        final long[] expected = NumberFactory.makeLongArray(filledSeed);

        // This calls convert with a length
        final long[] out = new ByteArray2LongArray().convert(seed, outLength);
        Assertions.assertArrayEquals(expected, out);
    }
}
