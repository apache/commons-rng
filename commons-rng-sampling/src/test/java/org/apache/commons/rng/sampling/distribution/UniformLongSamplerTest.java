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
package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.LongProvider;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Locale;

/**
 * Test for the {@link UniformLongSampler}. The tests hit edge cases for the sampler
 * and demonstrates uniformity of output when the underlying RNG output is uniform.
 *
 * <p>Note: No statistical tests for uniformity are performed on the output. The tests
 * are constructed on the premise that the underlying sampling methods correctly
 * use the random bits from {@link UniformRandomProvider}. Correctness
 * for a small range is tested against {@link UniformRandomProvider#nextLong(long)}
 * and correctness for a large range is tested that the {@link UniformRandomProvider#nextLong()}
 * is within the range limits. Power of two ranges are tested against a bit shift
 * of a random long.
 */
class UniformLongSamplerTest {
    /**
     * Test the constructor with a bad range.
     */
    @Test
    void testConstructorThrowsWithLowerAboveUpper() {
        final long upper = 55;
        final long lower = upper + 1;
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> UniformLongSampler.of(rng, lower, upper));
    }

    @Test
    void testSamplesWithRangeOf1() {
        final long upper = 99;
        final long lower = upper;
        final UniformRandomProvider rng = RandomAssert.createRNG();
        final UniformLongSampler sampler = UniformLongSampler.of(rng, lower, upper);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(lower, sampler.sample());
        }
    }

    /**
     * Test samples with a full long range.
     * The output should be the same as the long values produced from a RNG.
     */
    @Test
    void testSamplesWithFullRange() {
        final long upper = Long.MAX_VALUE;
        final long lower = Long.MIN_VALUE;
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformLongSampler sampler = UniformLongSampler.of(rng2, lower, upper);
        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(rng1.nextLong(), sampler.sample());
        }
    }

    /**
     * Test samples with a non-power of 2 range.
     * The output should be the same as the long values produced from a RNG
     * based on o.a.c.rng.core.BaseProvider as the rejection algorithm is
     * the same.
     */
    @ParameterizedTest
    @ValueSource(longs = {234293789329234L, 145, 69987, 12673586268L, 234785389445435L, Long.MAX_VALUE})
    void testSamplesWithSmallNonPowerOf2Range(long upper) {
        for (final long lower : new long[] {-13, 0, 13}) {
            final long n = upper - lower + 1;
            // Skip overflow ranges
            if (n < 0) {
                continue;
            }

            Assertions.assertNotEquals(0, n & (n - 1), "Power of 2 is invalid here");

            // Largest multiple of the upper bound.
            // floor(2^63 / range) * range
            // Computed as 2^63 - 2 % 63 with unsigned integers.
            final long m = Long.MIN_VALUE - Long.remainderUnsigned(Long.MIN_VALUE, n);

            // Check the method used in the sampler
            Assertions.assertEquals(m, (Long.MIN_VALUE / n) * -n);

            // Use an RNG that forces the rejection path on the first few samples
            // This occurs when the positive value is above the limit set by the
            // largest multiple of upper that does not overflow.
            final UniformRandomProvider rng1 = createRngWithFullBitsOnFirstCall(m);
            final UniformRandomProvider rng2 = createRngWithFullBitsOnFirstCall(m);
            final UniformLongSampler sampler = UniformLongSampler.of(rng2, lower, upper);
            for (int i = 0; i < 100; i++) {
                Assertions.assertEquals(lower + rng1.nextLong(n), sampler.sample());
            }
        }
    }

    /**
     * Creates a RNG which will return full bits for the first sample, then bits
     * too high for the configured limit for a few iterations.
     *
     * @param m Upper limit for a sample
     * @return the uniform random provider
     */
    private static UniformRandomProvider createRngWithFullBitsOnFirstCall(long m) {
        return new SplitMix64(0L) {
            private int i;
            @Override
            public long next() {
                int j = i++;
                if (j == 0) {
                    // Full bits
                    return -1L;
                } else if (j < 6) {
                    // A value just above or below the limit.
                    // Assumes m is positive and the sampler uses >>> 1 to extract
                    // a positive value.
                    return (m + 3 - j) << 1;
                }
                return super.next();
            }
        };
    }

    /**
     * Test samples with a power of 2 range.
     * This tests the minimum and maximum output should be the range limits.
     */
    @Test
    void testSamplesWithPowerOf2Range() {
        final UniformRandomProvider rngZeroBits = new LongProvider() {
            @Override
            public long next() {
                // No bits
                return 0L;
            }
        };
        final UniformRandomProvider rngAllBits = new LongProvider() {
            @Override
            public long next() {
                // All bits
                return -1L;
            }
        };

        final long lower = -3;
        UniformLongSampler sampler;
        // The upper range for a positive long is 2^63-1. So the max positive power of
        // 2 is 2^62. However the sampler should handle a bit shift of 63 to create a range
        // of Long.MIN_VALUE as this is a power of 2 as an unsigned long (2^63).
        for (int i = 0; i < 64; i++) {
            final long range = 1L << i;
            final long upper = lower + range - 1;
            sampler = UniformLongSampler.of(rngZeroBits, lower, upper);
            Assertions.assertEquals(lower, sampler.sample(), "Zero bits sample");
            sampler = UniformLongSampler.of(rngAllBits, lower, upper);
            Assertions.assertEquals(upper, sampler.sample(), "All bits sample");
        }
    }

    /**
     * Test samples with a power of 2 range.
     * This tests the output is created using a bit shift.
     */
    @Test
    void testSamplesWithPowerOf2RangeIsBitShift() {
        final long lower = 0;
        UniformLongSampler sampler;
        // Power of 2 sampler used for a bit shift of 1 to 63.
        for (int i = 1; i <= 63; i++) {
            // Upper is inclusive so subtract 1
            final long upper = (1L << i) - 1;
            final int shift = 64 - i;
            final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
            final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
            sampler = UniformLongSampler.of(rng2, lower, upper);
            for (int j = 0; j < 10; j++) {
                Assertions.assertEquals(rng1.nextLong() >>> shift, sampler.sample());
            }
        }
    }

    /**
     * Test samples with a large non-power of 2 range.
     * This tests the large range algorithm uses a rejection method.
     */
    @Test
    void testSamplesWithLargeNonPowerOf2RangeIsRejectionMethod() {
        // Create a range bigger than 2^63
        final long upper = Long.MAX_VALUE / 2 + 1;
        final long lower = Long.MIN_VALUE / 2 - 1;
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformLongSampler sampler = UniformLongSampler.of(rng2, lower, upper);
        for (int i = 0; i < 10; i++) {
            // Get the expected value by the rejection method
            long expected;
            do {
                expected = rng1.nextLong();
            } while (expected < lower || expected > upper);
            Assertions.assertEquals(expected, sampler.sample());
        }
    }

    @Test
    void testOffsetSamplesWithNonPowerOf2Range() {
        assertOffsetSamples(257);
    }

    @Test
    void testOffsetSamplesWithPowerOf2Range() {
        assertOffsetSamples(256);
    }

    @Test
    void testOffsetSamplesWithRangeOf1() {
        assertOffsetSamples(1);
    }

    private static void assertOffsetSamples(long range) {
        final Long seed = RandomSource.createLong();
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(seed);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(seed);
        final UniformRandomProvider rng3 = RandomSource.SPLIT_MIX_64.create(seed);

        // Since the upper limit is inclusive
        range = range - 1;
        final long offsetLo = -13;
        final long offsetHi = 42;
        final UniformLongSampler sampler = UniformLongSampler.of(rng1, 0, range);
        final UniformLongSampler samplerLo = UniformLongSampler.of(rng2, offsetLo, offsetLo + range);
        final UniformLongSampler samplerHi = UniformLongSampler.of(rng3, offsetHi, offsetHi + range);
        for (int i = 0; i < 10; i++) {
            final long sample1 = sampler.sample();
            final long sample2 = samplerLo.sample();
            final long sample3 = samplerHi.sample();
            Assertions.assertEquals(sample1 + offsetLo, sample2, "Incorrect negative offset sample");
            Assertions.assertEquals(sample1 + offsetHi, sample3, "Incorrect positive offset sample");
        }
    }

    /**
     * Test the sample uniformity when using a small range that is a power of 2.
     */
    @Test
    void testSampleUniformityWithPowerOf2Range() {
        // Test using a RNG that outputs a counter of integers.
        // The n most significant bits will be represented uniformly over a
        // sequence that is a 2^n long.
        final UniformRandomProvider rng = new LongProvider() {
            private long bits = 0;

            @Override
            public long next() {
                // We reverse the bits because the most significant bits are used
                return Long.reverse(bits++);
            }
        };

        // n = upper range exclusive
        final int n = 32; // power of 2
        final int[] histogram = new int[n];

        final long lower = 0;
        final long upper = n - 1;

        final UniformLongSampler sampler = UniformLongSampler.of(rng, lower, upper);

        final int expected = 2;
        for (int i = expected * n; i-- > 0;) {
            histogram[(int) sampler.sample()]++;
        }

        // This should be even across the entire range
        for (int value : histogram) {
            Assertions.assertEquals(expected, value);
        }
    }

    @Test
    void testSharedStateSamplerWithSmallRange() {
        assertSharedStateSampler(5, 67);
    }

    @Test
    void testSharedStateSamplerWithLargeRange() {
        // Set the range so rejection below or above the threshold occurs with approximately
        // p=0.25 for each bound.
        assertSharedStateSampler(Long.MIN_VALUE / 2 - 1, Long.MAX_VALUE / 2 + 1);
    }

    @Test
    void testSharedStateSamplerWithPowerOf2Range() {
        assertSharedStateSampler(0, (1L << 45) - 1);
    }

    @Test
    void testSharedStateSamplerWithRangeOf1() {
        assertSharedStateSampler(968757657572323L, 968757657572323L);
    }

    /**
     * Test the SharedStateSampler implementation returns the same sequence as the source sampler
     * when using an identical RNG.
     *
     * @param lower Lower.
     * @param upper Upper.
     */
    private static void assertSharedStateSampler(long lower, long upper) {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformLongSampler sampler1 = UniformLongSampler.of(rng1, lower, upper);
        final UniformLongSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    @Test
    void testToStringWithSmallRange() {
        assertToString(5, 67);
    }

    @Test
    void testToStringWithLargeRange() {
        assertToString(-99999999, Long.MAX_VALUE);
    }

    @Test
    void testToStringWithPowerOf2Range() {
        // Note the range is upper - lower + 1
        assertToString(0, 31);
    }

    @Test
    void testToStringWithRangeOf1() {
        assertToString(9, 9);
    }

    /**
     * Test the toString method contains the term "uniform". This is true of all samplers
     * even for a fixed sample from a range of 1.
     *
     * @param lower Lower.
     * @param upper Upper.
     */
    private static void assertToString(long lower, long upper) {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformLongSampler sampler = UniformLongSampler.of(rng, lower, upper);
        Assertions.assertTrue(sampler.toString().toLowerCase(Locale.US).contains("uniform"));
    }
}
