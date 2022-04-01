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

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source32.IntProvider;
import org.apache.commons.rng.core.source64.LongProvider;
import org.apache.commons.rng.core.source64.RandomLongSource;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.core.util.NumberFactory;

/**
 * Tests for {@link SeedFactory}.
 */
class SeedFactoryTest {
    @Test
    void testCreateLong() {
        final Map<Long, Integer> values = new HashMap<>();

        final int n = 100000;
        for (int i = 0; i < n; i++) {
            final long v = SeedFactory.createLong();

            Integer count = values.get(v);
            if (count == null) {
                count = 0;
            }
            values.put(v, count + 1);
        }

        // Check that all seeds are different.
        assertDifferentValues(values);
    }

    @Test
    void testCreateLongArray() {
        final Map<Long, Integer> values = new HashMap<>();

        final int n = 100000;
        final long[] array = SeedFactory.createLongArray(n);
        Assertions.assertEquals(n, array.length);

        for (long v : array) {
            Integer count = values.get(v);
            if (count == null) {
                count = 0;
            }
            values.put(v, count + 1);
        }

        // Check that all seeds are different.
        assertDifferentValues(values);
    }

    @Test
    void testCreateIntArray() {
        final Map<Long, Integer> values = new HashMap<>();

        for (int i = 0; i < 50000; i++) {
            final int[] a = SeedFactory.createIntArray(2);
            final long v = NumberFactory.makeLong(a[0], a[1]);
            Integer count = values.get(v);
            if (count == null) {
                count = 0;
            }
            values.put(v, count + 1);
        }

        // Check that all pairs in are different.
        assertDifferentValues(values);
    }

    /**
     * Asserts that all the keys in given {@code map} have their
     * value equal to 1.
     *
     * @param map Map to counts.
     */
    private static <T> void assertDifferentValues(Map<T, Integer> map) {
        final StringBuilder sb = new StringBuilder();

        int duplicates = 0;
        for (Map.Entry<T, Integer> entry : map.entrySet()) {
            final int count = entry.getValue();
            if (count <= 0) {
                throw new IllegalStateException();
            }

            if (count > 1) {
                duplicates += count - 1;
                sb.append(entry.getKey() + ": " + count + "\n");
            }
        }

        if (duplicates > 0) {
            Assertions.fail(duplicates + " duplicates\n" + sb);
        }
    }

    @Test
    void testCreateIntArrayWithZeroSize() {
        Assertions.assertArrayEquals(new int[0], SeedFactory.createIntArray(0));
    }

    @Test
    void testCreateIntArrayWithCompleteBlockSize() {
        // Block size is 8 for int
        assertCreateIntArray(8);
    }

    @Test
    void testCreateIntArrayWithIncompleteBlockSize() {
        // Block size is 8 for int
        assertCreateIntArray(8 + 1);
    }

    /**
     * Checks that the int array values can be placed into 2 bins with
     * approximately equal number of counts.
     * The test uses the expectation from a fixed-step "random walk".
     *
     * @param n The size of the array.
     */
    private static void assertCreateIntArray(int n) {
        final int[] array = SeedFactory.createIntArray(n);
        Assertions.assertEquals(n, array.length, "Incorrect array length");
        // The bit count should be 50%.
        int bitCount = 0;
        for (final int i : array) {
            bitCount += Integer.bitCount(i);
        }
        final int numberOfBits = n * Integer.SIZE;
        assertMonobit(bitCount, numberOfBits);
    }

    @Test
    void testCreateLongArrayWithZeroSize() {
        Assertions.assertArrayEquals(new long[0], SeedFactory.createLongArray(0));
    }

    @Test
    void testCreateLongArrayWithCompleteBlockSize() {
        // Block size is 4 for long
        assertCreateLongArray(4);
    }

    @Test
    void testCreateLongArrayWithIncompleteBlockSize() {
        // Block size is 4 for long
        assertCreateLongArray(4 + 1);
    }

    /**
     * Checks that the long array values can be placed into 2 bins with
     * approximately equal number of counts.
     * The test uses the expectation from a fixed-step "random walk".
     *
     * @param n The size of the array.
     */
    private static void assertCreateLongArray(int n) {
        final long[] array = SeedFactory.createLongArray(n);
        Assertions.assertEquals(n, array.length, "Incorrect array length");
        // The bit count should be 50%.
        int bitCount = 0;
        for (final long i : array) {
            bitCount += Long.bitCount(i);
        }
        final int numberOfBits = n * Long.SIZE;
        assertMonobit(bitCount, numberOfBits);
    }

    /**
     * Assert that the number of 1 bits is approximately 50%. This is based upon a
     * fixed-step "random walk" of +1/-1 from zero.
     *
     * <p>The test is equivalent to the NIST Monobit test with a fixed p-value of 0.0001.
     * The number of bits is recommended to be above 100.</p>
     *
     * @see <A
     * href="https://csrc.nist.gov/publications/detail/sp/800-22/rev-1a/final">Bassham, et
     * al (2010) NIST SP 800-22: A Statistical Test Suite for Random and Pseudorandom
     * Number Generators for Cryptographic Applications. Section 2.1.</a>
     *
     * @param bitCount The bit count.
     * @param numberOfBits Number of bits.
     */
    private static void assertMonobit(int bitCount, int numberOfBits) {
        // Convert the bit count into a number of +1/-1 steps.
        final double sum = 2.0 * bitCount - numberOfBits;
        // The reference distribution is Normal with a standard deviation of sqrt(n).
        // Check the absolute position is not too far from the mean of 0 with a fixed
        // p-value of 0.0001 taken from a 2-tailed Normal distribution. Computation of
        // the p-value requires the complimentary error function.
        // The p-value is set to be equal to a 0.01 with 1 allowed re-run.
        // (Re-runs are not configured for this test.)
        final double absSum = Math.abs(sum);
        final double max = Math.sqrt(numberOfBits) * 3.891;
        Assertions.assertTrue(absSum <= max,
            () -> "Walked too far astray: " + absSum +
                  " > " + max +
                  " (test will fail randomly about 1 in 10,000 times)");
    }

    @Test
    void testCreateByteArrayWithSizeZero() {
        assertCreateByteArray(new byte[0]);
    }

    @Test
    void testCreateByteArrayIgnoresNonZeroPositions() {
        final byte position = 123;
        int n = 3;
        for (int i = 0; i < n; i++) {
            final byte[] expected = new byte[n];
            expected[i] = position;
            assertCreateByteArray(expected);
        }
    }

    /**
     * Assert that the SeedFactory uses the bytes exactly as generated by the
     * {@link UniformRandomProvider#nextBytes(byte[])} method (assuming they are not all zero).
     *
     * @param expected the expected
     */
    private static void assertCreateByteArray(final byte[] expected) {
        final UniformRandomProvider rng = new IntProvider() {
            @Override
            public int next() {
                Assertions.fail("This method should not be used");
                return 0;
            }

            @Override
            public void nextBytes(byte[] bytes) {
                System.arraycopy(expected, 0, bytes, 0, Math.min(expected.length, bytes.length));
            }
        };

        final byte[] seed = SeedFactory.createByteArray(rng, expected.length, 0, expected.length);
        Assertions.assertArrayEquals(expected, seed);
    }

    @Test
    void testCreateByteArrayWithAllZeroBytesUpdatesFromTo() {
        final UniformRandomProvider rng = new IntProvider() {
            @Override
            public int next() {
                // Deliberately produce zero
                return 0;
            }
        };
        // Test the method only replaces the target sub-range.
        // Test all sub-ranges.
        final int size = 4;
        for (int start = 0; start < size; start++) {
            final int from = start;
            for (int end = start; end < size; end++) {
                final int to = end;
                final byte[] seed = SeedFactory.createByteArray(rng, 4, from, to);

                // Validate
                for (int i = 0; i < from; i++) {
                    final int index = i;
                    Assertions.assertEquals(0, seed[i],
                        () -> String.format("[%d, %d) zero at position %d should not be modified",
                            from, to, index));
                }
                if (to > from) {
                    byte allBits = 0;
                    for (int i = from; i < to; i++) {
                        allBits |= seed[i];
                    }
                    Assertions.assertNotEquals(0, allBits,
                        () -> String.format("[%d, %d) should not be all zero", from, to));
                }
                for (int i = to; i < size; i++) {
                    final int index = i;
                    Assertions.assertEquals(0, seed[i],
                        () -> String.format("[%d, %d) zero at position %d should not be modified",
                            from, to, index));
                }
            }
        }
    }

    @Test
    void testEnsureNonZeroIntArrayIgnoresEmptySeed() {
        final int[] seed = new int[0];
        SeedFactory.ensureNonZero(seed, 0, 0);
        // Note: Nothing to assert.
        // This tests an ArrayIndexOutOfBoundsException does not occur.
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 1",
        "1, -1, 1",
        "1, 0, 2",
        "1, 1, 2",
    })
    void testEnsureNonZeroIntArrayThrowsWithInvalidRange(int n, int from, int to) {
        final int[] seed = new int[n];
        Assertions.assertThrows(IndexOutOfBoundsException.class,
            () -> SeedFactory.ensureNonZero(seed, from, to));
    }


    @Test
    void testEnsureNonZeroIntArrayWithAllZeroBytesUpdatesFromTo() {
        // Test the method only replaces the target sub-range.
        // Test all sub-ranges.
        final int size = 4;
        final int[] seed = new int[size];
        for (int start = 0; start < size; start++) {
            final int from = start;
            for (int end = start; end < size; end++) {
                final int to = end;
                Arrays.fill(seed, 0);
                SeedFactory.ensureNonZero(seed, from, to);

                // Validate
                for (int i = 0; i < from; i++) {
                    final int index = i;
                    Assertions.assertEquals(0, seed[i],
                        () -> String.format("[%d, %d) zero at position %d should not be modified",
                            from, to, index));
                }
                if (to > from) {
                    int allBits = 0;
                    for (int i = from; i < to; i++) {
                        allBits |= seed[i];
                    }
                    Assertions.assertNotEquals(0, allBits,
                        () -> String.format("[%d, %d) should not be all zero", from, to));
                }
                for (int i = to; i < size; i++) {
                    final int index = i;
                    Assertions.assertEquals(0, seed[i],
                        () -> String.format("[%d, %d) zero at position %d should not be modified",
                            from, to, index));
                }
            }
        }
    }

    @Test
    void testEnsureNonZeroLongArrayIgnoresEmptySeed() {
        final long[] seed = new long[0];
        SeedFactory.ensureNonZero(seed, 0, 0);
        // Note: Nothing to assert.
        // This tests an ArrayIndexOutOfBoundsException does not occur.
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 1",
        "1, -1, 1",
        "1, 0, 2",
        "1, 1, 2",
    })
    void testEnsureNonZeroLongArrayThrowsWithInvalidRange(int n, int from, int to) {
        final long[] seed = new long[n];
        Assertions.assertThrows(IndexOutOfBoundsException.class,
            () -> SeedFactory.ensureNonZero(seed, from, to));
    }


    @Test
    void testEnsureNonZeroLongArrayWithAllZeroBytesUpdatesFromTo() {
        // Test the method only replaces the target sub-range.
        // Test all sub-ranges.
        final int size = 4;
        final long[] seed = new long[size];
        for (int start = 0; start < size; start++) {
            final int from = start;
            for (int end = start; end < size; end++) {
                final int to = end;
                Arrays.fill(seed, 0);
                SeedFactory.ensureNonZero(seed, from, to);

                // Validate
                for (int i = 0; i < from; i++) {
                    final int index = i;
                    Assertions.assertEquals(0, seed[i],
                        () -> String.format("[%d, %d) zero at position %d should not be modified",
                            from, to, index));
                }
                if (to > from) {
                    long allBits = 0;
                    for (int i = from; i < to; i++) {
                        allBits |= seed[i];
                    }
                    Assertions.assertNotEquals(0, allBits,
                        () -> String.format("[%d, %d) should not be all zero", from, to));
                }
                for (int i = to; i < size; i++) {
                    final int index = i;
                    Assertions.assertEquals(0, seed[i],
                        () -> String.format("[%d, %d) zero at position %d should not be modified",
                            from, to, index));
                }
            }
        }
    }

    @Test
    void testEnsureNonZeroByteArrayIgnoresEmptySeed() {
        final byte[] seed = new byte[0];
        final UniformRandomProvider rng = new SplitMix64(123);
        SeedFactory.ensureNonZero(seed, 0, 0, rng);
        // Note: Nothing to assert.
        // This tests an ArrayIndexOutOfBoundsException does not occur.
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 1",
        "1, -1, 1",
        "1, 0, 2",
        "1, 1, 2",
    })
    void testEnsureNonZeroByteArrayThrowsWithInvalidRange(int n, int from, int to) {
        final byte[] seed = new byte[n];
        final UniformRandomProvider rng = new SplitMix64(123);
        Assertions.assertThrows(IndexOutOfBoundsException.class,
            () -> SeedFactory.ensureNonZero(seed, from, to, rng));
    }

    /**
     * Test the fixed values returned for the zero byte array source of randomness
     * have specific properties.
     */
    @Test
    void testZeroByteArraySourceOfRandomness() {
        Assertions.assertEquals(0, MixFunctions.stafford13(-MixFunctions.GOLDEN_RATIO_64 + MixFunctions.GOLDEN_RATIO_64));
        Assertions.assertEquals(0, MixFunctions.stafford13((1463436497261722119L << 1) + MixFunctions.GOLDEN_RATIO_64) & (-1L >>> 56));
        Assertions.assertEquals(0, MixFunctions.stafford13((4949471497809997598L << 1) + MixFunctions.GOLDEN_RATIO_64) & (-1L >>> 48));
        // Note:
        // Finding a value x where MixFunctions.stafford13((x << 1) + MixFunctions.GOLDEN_RATIO_64)
        // is zero for the least significant 7 bytes used inversion of the mix function.
        // See the MixFunctionsTest for a routine to perform the unmixing.
        Assertions.assertEquals(0, MixFunctions.stafford13((953042962641938212L << 1) + MixFunctions.GOLDEN_RATIO_64) & (-1L >>> 8));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -MixFunctions.GOLDEN_RATIO_64, 1463436497261722119L, 4949471497809997598L, 953042962641938212L})
    void testEnsureNonZeroByteArrayWithAllZeroBytesUpdatesFromTo(long next) {
        // Use a fixed source of randomness to demonstrate the method is robust.
        // This should only be used when the sub-range is non-zero length.
        int[] calls = {0};
        final UniformRandomProvider rng = new LongProvider() {
            @Override
            public long next() {
                calls[0]++;
                return next;
            }
        };

        // Test the method only replaces the target sub-range.
        // Test all sub-ranges up to size.
        final int size = 2 * Long.BYTES;
        final byte[] seed = new byte[size];
        for (int start = 0; start < size; start++) {
            final int from = start;
            for (int end = start; end < size; end++) {
                final int to = end;
                Arrays.fill(seed, (byte) 0);
                final int before = calls[0];
                SeedFactory.ensureNonZero(seed, from, to, rng);

                // Validate
                for (int i = 0; i < from; i++) {
                    final int index = i;
                    Assertions.assertEquals(0, seed[i],
                        () -> String.format("[%d, %d) zero at position %d should not be modified",
                            from, to, index));
                }
                if (to > from) {
                    byte allBits = 0;
                    for (int i = from; i < to; i++) {
                        allBits |= seed[i];
                    }
                    Assertions.assertNotEquals(0, allBits,
                        () -> String.format("[%d, %d) should not be all zero", from, to));
                    // Check the source was used to seed the sequence
                    Assertions.assertNotEquals(before, calls[0],
                        () -> String.format("[%d, %d) should use the random source", from, to));
                } else {
                    // Check the source was not used
                    Assertions.assertEquals(before, calls[0],
                        () -> String.format("[%d, %d) should not use the random source", from, to));
                }
                for (int i = to; i < size; i++) {
                    final int index = i;
                    Assertions.assertEquals(0, seed[i],
                        () -> String.format("[%d, %d) zero at position %d should not be modified",
                            from, to, index));
                }
            }
        }
    }

    @Test
    void testEnsureNonZeroValue() {
        final long expected = 345;
        RandomLongSource source = new RandomLongSource() {
            @Override
            public long next() {
                return expected;
            }
        };
        Assertions.assertEquals(expected, SeedFactory.ensureNonZero(source, 0),
            "Zero should be replaced using the random source");
        for (final long nonZero : new long[] {Long.MIN_VALUE, -1, 1, 9876654321L, Long.MAX_VALUE}) {
            Assertions.assertEquals(nonZero, SeedFactory.ensureNonZero(source, nonZero),
                "Non-zero should be unmodified");
        }
    }
}
