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
package org.apache.commons.rng;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for default method implementations in {@link UniformRandomProvider}.
 *
 * <p>This class verifies all exception conditions for the range methods.
 * Single value generation methods are asserted using a test of uniformity
 * from multiple samples.
 *
 * <p>Stream methods are tested {@link UniformRandomProviderStreamTest}.
 */
class UniformRandomProviderTest {
    /** Sample size for statistical uniformity tests. */
    private static final int SAMPLE_SIZE = 1000;
    /** Sample size for statistical uniformity tests as a BigDecimal. */
    private static final BigDecimal SAMPLE_SIZE_BD = BigDecimal.valueOf(SAMPLE_SIZE);
    /** Relative error used to verify the sum of expected frequencies matches the sample size. */
    private static final double RELATIVE_ERROR = Math.ulp(1.0) * 2;

    /**
     * Dummy class for checking the behavior of the UniformRandomProvider.
     */
    private static class DummyGenerator implements UniformRandomProvider {
        /** An instance. */
        static final DummyGenerator INSTANCE = new DummyGenerator();

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("The nextLong method should not be invoked");
        }
    }

    static int[] invalidNextIntBound() {
        return new int[] {0, -1, Integer.MIN_VALUE};
    }

    static Stream<Arguments> invalidNextIntOriginBound() {
        return Stream.of(
            Arguments.of(1, 1),
            Arguments.of(2, 1),
            Arguments.of(-1, -1),
            Arguments.of(-1, -2)
        );
    }

    static long[] invalidNextLongBound() {
        return new long[] {0, -1, Long.MIN_VALUE};
    }

    static Stream<Arguments> invalidNextLongOriginBound() {
        return Stream.of(
            Arguments.of(1L, 1L),
            Arguments.of(2L, 1L),
            Arguments.of(-1L, -1L),
            Arguments.of(-1L, -2L)
        );
    }

    static float[] invalidNextFloatBound() {
        return new float[] {0, -1, -Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
    }

    static Stream<Arguments> invalidNextFloatOriginBound() {
        return Stream.of(
            Arguments.of(1f, 1f),
            Arguments.of(2f, 1f),
            Arguments.of(-1f, -1f),
            Arguments.of(-1f, -2f),
            Arguments.of(Float.NEGATIVE_INFINITY, 0f),
            Arguments.of(0f, Float.POSITIVE_INFINITY),
            Arguments.of(0f, Float.NaN),
            Arguments.of(Float.NaN, 1f)
        );
    }

    static double[] invalidNextDoubleBound() {
        return new double[] {0, -1, -Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
    }

    static Stream<Arguments> invalidNextDoubleOriginBound() {
        return Stream.of(
            Arguments.of(1, 1),
            Arguments.of(2, 1),
            Arguments.of(-1, -1),
            Arguments.of(-1, -2),
            Arguments.of(Double.NEGATIVE_INFINITY, 0),
            Arguments.of(0, Double.POSITIVE_INFINITY),
            Arguments.of(0, Double.NaN),
            Arguments.of(Double.NaN, 1)
        );
    }

    /**
     * Creates a functional random generator by implementing the
     * {@link UniformRandomProvider#nextLong} method with a high quality source of randomness.
     *
     * @param seed Seed for the generator.
     * @return the random generator
     */
    private static UniformRandomProvider createRNG(long seed) {
        // The algorithm for SplittableRandom with the default increment passes:
        // - Test U01 BigCrush
        // - PractRand with at least 2^42 bytes (4 TiB) of output
        return new SplittableRandom(seed)::nextLong;
    }

    @Test
    void testNextBytesThrows() {
        final UniformRandomProvider rng = DummyGenerator.INSTANCE;
        Assertions.assertThrows(NullPointerException.class, () -> rng.nextBytes(null));
        Assertions.assertThrows(NullPointerException.class, () -> rng.nextBytes(null, 0, 1));
        // Invalid range
        final int length = 10;
        final byte[] bytes = new byte[length];
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> rng.nextBytes(bytes, -1, 1), "start < 0");
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> rng.nextBytes(bytes, length, 1), "start >= length");
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> rng.nextBytes(bytes, 0, -1), "len < 0");
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> rng.nextBytes(bytes, 5, 10), "start + len > length");
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> rng.nextBytes(bytes, 5, Integer.MAX_VALUE), "start + len > length, taking into account integer overflow");
    }

    @ParameterizedTest
    @MethodSource(value = {"invalidNextIntBound"})
    void testNextIntBoundThrows(int bound) {
        final UniformRandomProvider rng = DummyGenerator.INSTANCE;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.nextInt(bound));
    }

    @ParameterizedTest
    @MethodSource(value = {"invalidNextIntOriginBound"})
    void testNextIntOriginBoundThrows(int origin, int bound) {
        final UniformRandomProvider rng = DummyGenerator.INSTANCE;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.nextInt(origin, bound));
    }

    @ParameterizedTest
    @MethodSource(value = {"invalidNextLongBound"})
    void testNextLongBoundThrows(long bound) {
        final UniformRandomProvider rng = DummyGenerator.INSTANCE;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.nextLong(bound));
    }

    @ParameterizedTest
    @MethodSource(value = {"invalidNextLongOriginBound"})
    void testNextLongOriginBoundThrows(long origin, long bound) {
        final UniformRandomProvider rng = DummyGenerator.INSTANCE;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.nextLong(origin, bound));
    }

    @ParameterizedTest
    @MethodSource(value = {"invalidNextFloatBound"})
    void testNextFloatBoundThrows(float bound) {
        final UniformRandomProvider rng = DummyGenerator.INSTANCE;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.nextFloat(bound));
    }

    @ParameterizedTest
    @MethodSource(value = {"invalidNextFloatOriginBound"})
    void testNextFloatOriginBoundThrows(float origin, float bound) {
        final UniformRandomProvider rng = DummyGenerator.INSTANCE;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.nextFloat(origin, bound));
    }

    @ParameterizedTest
    @MethodSource(value = {"invalidNextDoubleBound"})
    void testNextDoubleBoundThrows(double bound) {
        final UniformRandomProvider rng = DummyGenerator.INSTANCE;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.nextDouble(bound));
    }

    @ParameterizedTest
    @MethodSource(value = {"invalidNextDoubleOriginBound"})
    void testNextDoubleOriginBoundThrows(double origin, double bound) {
        final UniformRandomProvider rng = DummyGenerator.INSTANCE;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.nextDouble(origin, bound));
    }

    @Test
    void testNextFloatExtremes() {
        final UniformRandomProvider rng = new DummyGenerator() {
            private int i;
            private int[] values = {0, -1, 1 << 8};
            @Override
            public int nextInt() {
                return values[i++];
            }
        };
        Assertions.assertEquals(0, rng.nextFloat(), "Expected zero bits to return 0");
        Assertions.assertEquals(Math.nextDown(1f), rng.nextFloat(), "Expected all bits to return ~1");
        // Assumes the value is created from the high 24 bits of nextInt
        Assertions.assertEquals(1f - Math.nextDown(1f), rng.nextFloat(), "Expected 1 bit (shifted) to return ~0");
    }

    @ParameterizedTest
    @ValueSource(floats = {Float.MIN_NORMAL, Float.MIN_NORMAL / 2})
    void testNextFloatBoundRounding(float bound) {
        // This method will be used
        final UniformRandomProvider rng = new DummyGenerator() {
            @Override
            public float nextFloat() {
                return Math.nextDown(1.0f);
            }
        };
        Assertions.assertEquals(bound, rng.nextFloat() * bound, "Expected result to be rounded up");
        Assertions.assertEquals(Math.nextDown(bound), rng.nextFloat(bound), "Result was not correctly rounded down");
    }

    @Test
    void testNextFloatOriginBoundInfiniteRange() {
        // This method will be used
        final UniformRandomProvider rng = new DummyGenerator() {
            private int i;
            private float[] values = {0, 0.25f, 0.5f, 0.75f, 1};
            @Override
            public float nextFloat() {
                return values[i++];
            }
        };
        final float x = Float.MAX_VALUE;
        Assertions.assertEquals(-x, rng.nextFloat(-x, x));
        Assertions.assertEquals(-x / 2, rng.nextFloat(-x, x), Math.ulp(x / 2));
        Assertions.assertEquals(0, rng.nextFloat(-x, x));
        Assertions.assertEquals(x / 2, rng.nextFloat(-x, x), Math.ulp(x / 2));
        Assertions.assertEquals(Math.nextDown(x), rng.nextFloat(-x, x));
    }

    @Test
    void testNextFloatOriginBoundRounding() {
        // This method will be used
        final float v = Math.nextDown(1.0f);
        final UniformRandomProvider rng = new DummyGenerator() {
            @Override
            public float nextFloat() {
                return v;
            }
        };
        float origin;
        float bound;

        // Simple case
        origin = 3.5f;
        bound = 4.5f;
        Assertions.assertEquals(bound, origin + v * (bound - origin), "Expected result to be rounded up");
        Assertions.assertEquals(Math.nextDown(bound), rng.nextFloat(origin, bound), "Result was not correctly rounded down");

        // Alternate formula:
        // requires sub-normal result for 'origin * v' to force rounding
        origin = -Float.MIN_NORMAL / 2;
        bound = Float.MIN_NORMAL / 2;
        Assertions.assertEquals(bound, origin * (1 - v) + v * bound, "Expected result to be rounded up");
        Assertions.assertEquals(Math.nextDown(bound), rng.nextFloat(origin, bound), "Result was not correctly rounded down");
    }

    @Test
    void testNextDoubleExtremes() {
        final UniformRandomProvider rng = new DummyGenerator() {
            private int i;
            private long[] values = {0, -1, 1L << 11};
            @Override
            public long nextLong() {
                return values[i++];
            }
        };
        Assertions.assertEquals(0, rng.nextDouble(), "Expected zero bits to return 0");
        Assertions.assertEquals(Math.nextDown(1.0), rng.nextDouble(), "Expected all bits to return ~1");
        // Assumes the value is created from the high 53 bits of nextInt
        Assertions.assertEquals(1.0 - Math.nextDown(1.0), rng.nextDouble(), "Expected 1 bit (shifted) to return ~0");
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.MIN_NORMAL, Double.MIN_NORMAL / 2})
    void testNextDoubleBoundRounding(double bound) {
        // This method will be used
        final UniformRandomProvider rng = new DummyGenerator() {
            @Override
            public double nextDouble() {
                return Math.nextDown(1.0);
            }
        };
        Assertions.assertEquals(bound, rng.nextDouble() * bound, "Expected result to be rounded up");
        Assertions.assertEquals(Math.nextDown(bound), rng.nextDouble(bound), "Result was not correctly rounded down");
    }

    @Test
    void testNextDoubleOriginBoundInfiniteRange() {
        // This method will be used
        final UniformRandomProvider rng = new DummyGenerator() {
            private int i;
            private double[] values = {0, 0.25, 0.5, 0.75, 1};
            @Override
            public double nextDouble() {
                return values[i++];
            }
        };
        final double x = Double.MAX_VALUE;
        Assertions.assertEquals(-x, rng.nextDouble(-x, x));
        Assertions.assertEquals(-x / 2, rng.nextDouble(-x, x), Math.ulp(x / 2));
        Assertions.assertEquals(0, rng.nextDouble(-x, x));
        Assertions.assertEquals(x / 2, rng.nextDouble(-x, x), Math.ulp(x / 2));
        Assertions.assertEquals(Math.nextDown(x), rng.nextDouble(-x, x));
    }

    @Test
    void testNextDoubleOriginBoundRounding() {
        // This method will be used
        final double v = Math.nextDown(1.0);
        final UniformRandomProvider rng = new DummyGenerator() {
            @Override
            public double nextDouble() {
                return v;
            }
        };
        double origin;
        double bound;

        // Simple case
        origin = 3.5;
        bound = 4.5;
        Assertions.assertEquals(bound, origin + v * (bound - origin), "Expected result to be rounded up");
        Assertions.assertEquals(Math.nextDown(bound), rng.nextDouble(origin, bound), "Result was not correctly rounded down");

        // Alternate formula:
        // requires sub-normal result for 'origin * v' to force rounding
        origin = -Double.MIN_NORMAL / 2;
        bound = Double.MIN_NORMAL / 2;
        Assertions.assertEquals(bound, origin * (1 - v) + v * bound, "Expected result to be rounded up");
        Assertions.assertEquals(Math.nextDown(bound), rng.nextDouble(origin, bound), "Result was not correctly rounded down");
    }

    @Test
    void testNextBooleanIsIntSignBit() {
        final int size = 10;
        final int[] values = ThreadLocalRandom.current().ints(size).toArray();
        final UniformRandomProvider rng = new DummyGenerator() {
            private int i;
            @Override
            public int nextInt() {
                return values[i++];
            }
        };
        for (int i = 0; i < size; i++) {
            Assertions.assertEquals(values[i] < 0, rng.nextBoolean());
        }
    }

    // Statistical tests for uniform distribution

    // Monobit tests

    @ParameterizedTest
    @ValueSource(longs = {263784628482L, -2563472, -2367482842368L})
    void testNextIntMonobit(long seed) {
        final UniformRandomProvider rng = createRNG(seed);
        int bitCount = 0;
        final int n = 100;
        for (int i = 0; i < n; i++) {
            bitCount += Integer.bitCount(rng.nextInt());
        }
        final int numberOfBits = n * Integer.SIZE;
        assertMonobit(bitCount, numberOfBits);
    }

    @ParameterizedTest
    @ValueSource(longs = {263784628L, 253674, -23568426834L})
    void testNextDoubleMonobit(long seed) {
        final UniformRandomProvider rng = createRNG(seed);
        int bitCount = 0;
        final int n = 100;
        for (int i = 0; i < n; i++) {
            bitCount += Long.bitCount((long) (rng.nextDouble() * (1L << 53)));
        }
        final int numberOfBits = n * 53;
        assertMonobit(bitCount, numberOfBits);
    }

    @ParameterizedTest
    @ValueSource(longs = {265342L, -234232, -672384648284L})
    void testNextBooleanMonobit(long seed) {
        final UniformRandomProvider rng = createRNG(seed);
        int bitCount = 0;
        final int n = 1000;
        for (int i = 0; i < n; i++) {
            if (rng.nextBoolean()) {
                bitCount++;
            }
        }
        final int numberOfBits = n;
        assertMonobit(bitCount, numberOfBits);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1526731, 263846, 4545})
    void testNextFloatMonobit(long seed) {
        final UniformRandomProvider rng = createRNG(seed);
        int bitCount = 0;
        final int n = 100;
        for (int i = 0; i < n; i++) {
            bitCount += Integer.bitCount((int) (rng.nextFloat() * (1 << 24)));
        }
        final int numberOfBits = n * 24;
        assertMonobit(bitCount, numberOfBits);
    }

    @ParameterizedTest
    @CsvSource({
        "-2638468223894, 16",
        "235647674, 17",
        "-928738475, 23",
    })
    void testNextBytesMonobit(long seed, int range) {
        final UniformRandomProvider rng = createRNG(seed);
        final byte[] bytes = new byte[range];
        int bitCount = 0;
        final int n = 100;
        for (int i = 0; i < n; i++) {
            rng.nextBytes(bytes);
            for (final byte b1 : bytes) {
                bitCount += Integer.bitCount(b1 & 0xff);
            }
        }
        final int numberOfBits = n * Byte.SIZE * range;
        assertMonobit(bitCount, numberOfBits);
    }

    /**
     * Assert that the number of 1 bits is approximately 50%. This is based upon a
     * fixed-step "random walk" of +1/-1 from zero.
     *
     * <p>The test is equivalent to the NIST Monobit test with a fixed p-value of
     * 0.01. The number of bits is recommended to be above 100.</p>
     *
     * @see <A
     * href="https://csrc.nist.gov/publications/detail/sp/800-22/rev-1a/final">Bassham,
     * et al (2010) NIST SP 800-22: A Statistical Test Suite for Random and
     * Pseudorandom Number Generators for Cryptographic Applications. Section
     * 2.1.</a>
     *
     * @param bitCount The bit count.
     * @param numberOfBits Number of bits.
     */
    private static void assertMonobit(int bitCount, int numberOfBits) {
        // Convert the bit count into a number of +1/-1 steps.
        final double sum = 2.0 * bitCount - numberOfBits;
        // The reference distribution is Normal with a standard deviation of sqrt(n).
        // Check the absolute position is not too far from the mean of 0 with a fixed
        // p-value of 0.01 taken from a 2-tailed Normal distribution. Computation of
        // the p-value requires the complimentary error function.
        final double absSum = Math.abs(sum);
        final double max = Math.sqrt(numberOfBits) * 2.5758293035489004;
        Assertions.assertTrue(absSum <= max,
            () -> "Walked too far astray: " + absSum + " > " + max +
                  " (test will fail randomly about 1 in 100 times)");
    }

    // Uniformity tests

    @ParameterizedTest
    @CsvSource({
        "263746283, 23, 0, 23",
        "-126536861889, 16, 0, 16",
        "617868181124, 1234, 567, 89",
        "-56788, 512, 0, 233",
        "6787535424, 512, 233, 279",
    })
    void testNextBytesUniform(long seed,
                              int length, int start, int size) {
        final UniformRandomProvider rng = createRNG(seed);
        final byte[] buffer = new byte[length];

        final Runnable nextMethod = start == 0 && size == length ?
                () -> rng.nextBytes(buffer) :
                () -> rng.nextBytes(buffer, start, size);

        final int last = start + size;
        Assertions.assertTrue(isUniformNextBytes(buffer, start, last, nextMethod),
                              "Expected uniform bytes");

        // The parts of the buffer where no values are put should be zero.
        for (int i = 0; i < start; i++) {
            Assertions.assertEquals(0, buffer[i], () -> "Filled < start: " + start);
        }
        for (int i = last; i < length; i++) {
            Assertions.assertEquals(0, buffer[i], () -> "Filled >= last: " + last);
        }
    }

    /**
     * Checks that the generator values can be placed into 256 bins with
     * approximately equal number of counts.
     * Test allows to select only part of the buffer for performing the
     * statistics.
     *
     * @param buffer Buffer to be filled.
     * @param first First element (included) of {@code buffer} range for
     * which statistics must be taken into account.
     * @param last Last element (excluded) of {@code buffer} range for
     * which statistics must be taken into account.
     * @param nextMethod Method that fills the given {@code buffer}.
     * @return {@code true} if the distribution is uniform.
     */
    private static boolean isUniformNextBytes(byte[] buffer,
                                              int first,
                                              int last,
                                              Runnable nextMethod) {
        final int sampleSize = 10000;

        // Number of possible values (do not change).
        final int byteRange = 256;
        // Chi-square critical value with 255 degrees of freedom
        // and 1% significance level.
        final double chi2CriticalValue = 310.45738821990585;

        // Bins.
        final long[] observed = new long[byteRange];
        final double[] expected = new double[byteRange];

        Arrays.fill(expected, sampleSize * (last - first) / (double) byteRange);

        for (int k = 0; k < sampleSize; k++) {
            nextMethod.run();

            for (int i = first; i < last; i++) {
                // Convert byte to an index in [0, 255]
                ++observed[buffer[i] & 0xff];
            }
        }

        // Compute chi-square.
        double chi2 = 0;
        for (int k = 0; k < byteRange; k++) {
            final double diff = observed[k] - expected[k];
            chi2 += diff * diff / expected[k];
        }

        // Statistics check.
        return chi2 <= chi2CriticalValue;
    }

    // Add range tests

    @ParameterizedTest
    @CsvSource({
        // No lower bound
        "2673846826, 0, 10",
        "-23658268, 0, 12",
        "263478624, 0, 31",
        "1278332, 0, 32",
        "99734765, 0, 2016128993",
        "-63485384, 0, 1834691456",
        "3876457638, 0, 869657561",
        "-126784782, 0, 1570504788",
        "2637846, 0, 2147483647",
        // Small range
        "2634682, 567576, 567586",
        "-56757798989, -1000, -100",
        "-97324785, -54656, 12",
        "23423235, -526783468, 257",
        // Large range
        "-2634682, -1073741824, 1073741824",
        "6786868132, -1263842626, 1237846372",
        "-263846723, -368268, 2147483647",
        "7352352, -2147483648, 61523457",
    })
    void testNextIntUniform(long seed, int origin, int bound) {
        final UniformRandomProvider rng = createRNG(seed);
        final LongSupplier nextMethod = origin == 0 ?
                () -> rng.nextInt(bound) :
                () -> rng.nextInt(origin, bound);
        checkNextInRange("nextInt", origin, bound, nextMethod);
    }

    @ParameterizedTest
    @CsvSource({
        // No lower bound
        "2673846826, 0, 11",
        "-23658268, 0, 19",
        "263478624, 0, 31",
        "1278332, 0, 32",
        "99734765, 0, 2326378468282368421",
        "-63485384, 0, 4872347624242243222",
        "3876457638, 0, 6263784682638866843",
        "-126784782, 0, 7256684297832668332",
        "2637846, 0, 9223372036854775807",
        // Small range
        "2634682, 567576, 567586",
        "-56757798989, -1000, -100",
        "-97324785, -54656, 12",
        "23423235, -526783468, 257",
        // Large range
        "-2634682, -4611686018427387904, 4611686018427387904",
        "6786868132, -4962836478223688590, 6723648246224929947",
        "-263846723, -368268, 9223372036854775807",
        "7352352, -9223372036854775808, 61523457",
    })
    void testNextLongUniform(long seed, long origin, long bound) {
        final UniformRandomProvider rng = createRNG(seed);
        final LongSupplier nextMethod = origin == 0 ?
                () -> rng.nextLong(bound) :
                () -> rng.nextLong(origin, bound);
        checkNextInRange("nextLong", origin, bound, nextMethod);
    }

    @ParameterizedTest
    @CsvSource({
        // Note: If the range limits are integers above 2^24 (16777216) it is not possible
        // to represent all the values with a float. This has no effect on sampling into bins
        // but should be avoided when generating integers for use in production code.

        // No lower bound.
        "2673846826, 0, 11",
        "-23658268, 0, 19",
        "263478624, 0, 31",
        "1278332, 0, 32",
        "99734765, 0, 1234",
        "-63485384, 0, 578",
        "3876457638, 0, 10000",
        "-126784782, 0, 2983423",
        "2637846, 0, 16777216",
        // Range
        "2634682, 567576, 567586",
        "-56757798989, -1000, -100",
        "-97324785, -54656, 12",
        "23423235, -526783468, 257",
        "-2634682, -688689797, -516827",
        "6786868132, -67, 67",
        "-263846723, -5678, 42",
        "7352352, 678687, 61523457",
    })
    void testNextFloatUniform(long seed, float origin, float bound) {
        Assertions.assertEquals((long) origin, origin, "origin");
        Assertions.assertEquals((long) bound, bound, "bound");
        final UniformRandomProvider rng = createRNG(seed);
        // Note casting as long will round towards zero.
        // If the upper bound is negative then this can create a domain error so use floor.
        final LongSupplier nextMethod = origin == 0 ?
                () -> (long) rng.nextFloat(bound) :
                () -> (long) Math.floor(rng.nextFloat(origin, bound));
        checkNextInRange("nextFloat", (long) origin, (long) bound, nextMethod);
    }


    @ParameterizedTest
    @CsvSource({
        // Note: If the range limits are integers above 2^53 (9007199254740992) it is not possible
        // to represent all the values with a double. This has no effect on sampling into bins
        // but should be avoided when generating integers for use in production code.

        // No lower bound.
        "2673846826, 0, 11",
        "-23658268, 0, 19",
        "263478624, 0, 31",
        "1278332, 0, 32",
        "99734765, 0, 1234",
        "-63485384, 0, 578",
        "3876457638, 0, 10000",
        "-126784782, 0, 2983423",
        "2637846, 0, 9007199254740992",
        // Range
        "2634682, 567576, 567586",
        "-56757798989, -1000, -100",
        "-97324785, -54656, 12",
        "23423235, -526783468, 257",
        "-2634682, -688689797, -516827",
        "6786868132, -67, 67",
        "-263846723, -5678, 42",
        "7352352, 678687, 61523457",
    })
    void testNextDoubleUniform(long seed, double origin, double bound) {
        Assertions.assertEquals((long) origin, origin, "origin");
        Assertions.assertEquals((long) bound, bound, "bound");
        final UniformRandomProvider rng = createRNG(seed);
        // Note casting as long will round towards zero.
        // If the upper bound is negative then this can create a domain error so use floor.
        final LongSupplier nextMethod = origin == 0 ?
                () -> (long) rng.nextDouble(bound) :
                () -> (long) Math.floor(rng.nextDouble(origin, bound));
        checkNextInRange("nextDouble", (long) origin, (long) bound, nextMethod);
    }

    /**
     * Tests uniformity of the distribution produced by the given
     * {@code nextMethod}.
     * It performs a chi-square test of homogeneity of the observed
     * distribution with the expected uniform distribution.
     * Repeat tests are performed at the 1% level and the total number of failed
     * tests is tested at the 0.5% significance level.
     *
     * @param method Generator method.
     * @param origin Lower bound (inclusive).
     * @param bound Upper bound (exclusive).
     * @param nextMethod method to call.
     */
    private static void checkNextInRange(String method,
                                         long origin,
                                         long bound,
                                         LongSupplier nextMethod) {
        // Do not change
        // (statistical test assumes that 500 repeats are made with dof = 9).
        final int numTests = 500;
        final int numBins = 10; // dof = numBins - 1

        // Set up bins.
        final long[] binUpperBounds = new long[numBins];
        // Range may be above a positive long: step = (bound - origin) / bins
        final BigDecimal range = BigDecimal.valueOf(bound)
                .subtract(BigDecimal.valueOf(origin));
        final double step = range.divide(BigDecimal.TEN).doubleValue();
        for (int k = 1; k < numBins; k++) {
            binUpperBounds[k - 1] = origin + (long) (k * step);
        }
        // Final bound
        binUpperBounds[numBins - 1] = bound;

        // Create expected frequencies
        final double[] expected = new double[numBins];
        long previousUpperBound = origin;
        final double scale = SAMPLE_SIZE_BD.divide(range, MathContext.DECIMAL128).doubleValue();
        double sum = 0;
        for (int k = 0; k < numBins; k++) {
            final long binWidth = binUpperBounds[k] - previousUpperBound;
            expected[k] = scale * binWidth;
            sum += expected[k];
            previousUpperBound = binUpperBounds[k];
        }
        Assertions.assertEquals(SAMPLE_SIZE, sum, SAMPLE_SIZE * RELATIVE_ERROR, "Invalid expected frequencies");

        final int[] observed = new int[numBins];
        // Chi-square critical value with 9 degrees of freedom
        // and 1% significance level.
        final double chi2CriticalValue = 21.665994333461924;

        // For storing chi2 larger than the critical value.
        final List<Double> failedStat = new ArrayList<>();
        try {
            final int lastDecileIndex = numBins - 1;
            for (int i = 0; i < numTests; i++) {
                Arrays.fill(observed, 0);
                SAMPLE: for (int j = 0; j < SAMPLE_SIZE; j++) {
                    final long value = nextMethod.getAsLong();
                    if (value < origin) {
                        Assertions.fail(String.format("Sample %d not within bound [%d, %d)",
                                                      value, origin, bound));
                    }

                    for (int k = 0; k < lastDecileIndex; k++) {
                        if (value < binUpperBounds[k]) {
                            ++observed[k];
                            continue SAMPLE;
                        }
                    }
                    if (value >= bound) {
                        Assertions.fail(String.format("Sample %d not within bound [%d, %d)",
                                                      value, origin, bound));
                    }
                    ++observed[lastDecileIndex];
                }

                // Compute chi-square.
                double chi2 = 0;
                for (int k = 0; k < numBins; k++) {
                    final double diff = observed[k] - expected[k];
                    chi2 += diff * diff / expected[k];
                }

                // Statistics check.
                if (chi2 > chi2CriticalValue) {
                    failedStat.add(chi2);
                }
            }
        } catch (Exception e) {
            // Should never happen.
            throw new RuntimeException("Unexpected", e);
        }

        // The expected number of failed tests can be modelled as a Binomial distribution
        // B(n, p) with n=500, p=0.01 (500 tests with a 1% significance level).
        // The cumulative probability of the number of failed tests (X) is:
        // x     P(X>x)
        // 10    0.0132
        // 11    0.00521
        // 12    0.00190

        if (failedStat.size() > 11) { // Test will fail with 0.5% probability
            Assertions.fail(String.format(
                "%s: Too many failures for n = %d, sample size = %d " +
                "(%d out of %d tests failed, chi2 > %.3f=%s)",
                method, bound, SAMPLE_SIZE, failedStat.size(), numTests, chi2CriticalValue,
                failedStat.stream().map(d -> String.format("%.3f", d))
                          .collect(Collectors.joining(", ", "[", "]"))));
        }
    }
}
