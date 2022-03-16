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
 * Tests for the {@link IntArray2LongArray} converter.
 */
class IntArray2LongArrayTest {
    /**
     * Gets the lengths for the int[] seeds to convert.
     *
     * @return the lengths
     */
    static IntStream getLengths() {
        return IntStream.rangeClosed(0, (Long.BYTES / Integer.BYTES) * 2);
    }

    /**
     * Gets the expected output length.
     *
     * @param ints Number of ints
     * @return the output length
     */
    private static int getOutputLength(int ints) {
        return (int) Math.ceil((double) ints / 2);
    }

    @ParameterizedTest
    @MethodSource(value = {"getLengths"})
    void testSeedSizeIsMultipleOfIntSize(int ints) {
        final int[] seed = new int[ints];

        // This calls convert without a length
        final long[] out = new IntArray2LongArray().convert(seed);
        Assertions.assertEquals(getOutputLength(ints), out.length);
    }

    @ParameterizedTest
    @MethodSource(value = {"getLengths"})
    void testSeedConversion(int ints) {
        assertSeedConversion(ints, getOutputLength(ints));
    }

    @ParameterizedTest
    @MethodSource(value = {"getLengths"})
    void testFilledSeedConversion(int ints) {
        assertSeedConversion(ints, ints / 3);
    }

    @ParameterizedTest
    @MethodSource(value = {"getLengths"})
    void testTruncatedSeedConversion(int ints) {
        assertSeedConversion(ints, ints * 3);
    }

    private static void assertSeedConversion(int ints, int outLength) {
        final int[] seed = ThreadLocalRandom.current().ints(ints).toArray();

        // Convert to little-endian int array
        final int[] filledSeed = Arrays.copyOf(seed, outLength * 2);
        final long[] expected = new long[filledSeed.length / 2];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = NumberFactory.makeLong(filledSeed[2 * i + 1], filledSeed[2 * i]);
        }

        // This calls convert with a length
        final long[] out = new IntArray2LongArray().convert(seed, outLength);
        Assertions.assertArrayEquals(expected, out);
    }

    @ParameterizedTest
    @MethodSource(value = {"getLengths"})
    void testIntArrayToLongArrayToIntArray(int ints) {
        final int[] expected = ThreadLocalRandom.current().ints(ints).toArray();
        final int[] out = new LongArray2IntArray().convert(new IntArray2LongArray().convert(expected), ints);
        Assertions.assertArrayEquals(expected, out);
    }
}
