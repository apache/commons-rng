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
import org.apache.commons.rng.core.source32.IntProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.Test;

import java.util.Locale;

/**
 * Test for the {@link DiscreteUniformSampler}. The tests hit edge cases for the sampler
 * and demonstrates uniformity of output when the underlying RNG output is uniform.
 */
public class DiscreteUniformSamplerTest {
    /**
     * Test the constructor with a bad range.
     */
    @Test
    public void testConstructorThrowsWithLowerAboveUpper() {
        final int upper = 55;
        final int lower = upper + 1;
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> DiscreteUniformSampler.of(rng, lower, upper));
    }

    @Test
    public void testSamplesWithRangeOf1() {
        final int upper = 99;
        final int lower = upper;
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create();
        final SharedStateDiscreteSampler sampler = DiscreteUniformSampler.of(rng, lower, upper);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(lower, sampler.sample());
        }
    }

    /**
     * Test samples with a full integer range.
     * The output should be the same as the int values produced from a RNG.
     */
    @Test
    public void testSamplesWithFullRange() {
        final int upper = Integer.MAX_VALUE;
        final int lower = Integer.MIN_VALUE;
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final SharedStateDiscreteSampler sampler = DiscreteUniformSampler.of(rng2, lower, upper);
        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(rng1.nextInt(), sampler.sample());
        }
    }

    /**
     * Test samples with a non-power of 2 range.
     * The output should be the same as the long values produced from a RNG
     * based on o.a.c.rng.core.BaseProvider as the rejection algorithm is
     * the same.
     */
    @Test
    public void testSamplesWithSmallNonPowerOf2Range() {
        final int upper = 257;
        for (final int lower : new int[] {-13, 0, 13}) {
            final int n = upper - lower + 1;
            final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
            final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
            final SharedStateDiscreteSampler sampler = DiscreteUniformSampler.of(rng2, lower, upper);
            for (int i = 0; i < 10; i++) {
                Assertions.assertEquals(lower + rng1.nextInt(n), sampler.sample());
            }
        }
    }

    /**
     * Test samples with a power of 2 range.
     * This tests the minimum and maximum output should be the range limits.
     */
    @Test
    public void testSamplesWithPowerOf2Range() {
        final UniformRandomProvider rngZeroBits = new IntProvider() {
            @Override
            public int next() {
                // No bits
                return 0;
            }
        };
        final UniformRandomProvider rngAllBits = new IntProvider() {
            @Override
            public int next() {
                // All bits
                return -1;
            }
        };

        final int lower = -3;
        DiscreteUniformSampler sampler;
        // The upper range for a positive integer is 2^31-1. So the max positive power of
        // 2 is 2^30. However the sampler should handle a bit shift of 31 to create a range
        // of Integer.MIN_VALUE as this is a power of 2 as an unsigned int (2^31).
        for (int i = 0; i < 32; i++) {
            final int range = 1 << i;
            final int upper = lower + range - 1;
            sampler = new DiscreteUniformSampler(rngZeroBits, lower, upper);
            Assertions.assertEquals(lower, sampler.sample(), "Zero bits sample");
            sampler = new DiscreteUniformSampler(rngAllBits, lower, upper);
            Assertions.assertEquals(upper, sampler.sample(), "All bits sample");
        }
    }

    /**
     * Test samples with a power of 2 range.
     * This tests the output is created using a bit shift.
     */
    @Test
    public void testSamplesWithPowerOf2RangeIsBitShift() {
        final int lower = 0;
        SharedStateDiscreteSampler sampler;
        // Power of 2 sampler used for a bit shift of 1 to 31.
        for (int i = 1; i <= 31; i++) {
            // Upper is inclusive so subtract 1
            final int upper = (1 << i) - 1;
            final int shift = 32 - i;
            final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
            final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
            sampler = DiscreteUniformSampler.of(rng2, lower, upper);
            for (int j = 0; j < 10; j++) {
                Assertions.assertEquals(rng1.nextInt() >>> shift, sampler.sample());
            }
        }
    }

    /**
     * Test samples with a large non-power of 2 range.
     * This tests the large range algorithm uses a rejection method.
     */
    @Test
    public void testSamplesWithLargeNonPowerOf2RangeIsRejectionMethod() {
        // Create a range bigger than 2^63
        final int upper = Integer.MAX_VALUE / 2 + 1;
        final int lower = Integer.MIN_VALUE / 2 - 1;
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final SharedStateDiscreteSampler sampler = DiscreteUniformSampler.of(rng2, lower, upper);
        for (int i = 0; i < 10; i++) {
            // Get the expected value by the rejection method
            long expected;
            do {
                expected = rng1.nextInt();
            } while (expected < lower || expected > upper);
            Assertions.assertEquals(expected, sampler.sample());
        }
    }

    @Test
    public void testOffsetSamplesWithNonPowerOf2Range() {
        assertOffsetSamples(257);
    }

    @Test
    public void testOffsetSamplesWithPowerOf2Range() {
        assertOffsetSamples(256);
    }

    @Test
    public void testOffsetSamplesWithRangeOf1() {
        assertOffsetSamples(1);
    }

    private static void assertOffsetSamples(int range) {
        final Long seed = RandomSource.createLong();
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(seed);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(seed);
        final UniformRandomProvider rng3 = RandomSource.SPLIT_MIX_64.create(seed);

        // Since the upper limit is inclusive
        range = range - 1;
        final int offsetLo = -13;
        final int offsetHi = 42;
        final SharedStateDiscreteSampler sampler = DiscreteUniformSampler.of(rng1, 0, range);
        final SharedStateDiscreteSampler samplerLo = DiscreteUniformSampler.of(rng2, offsetLo, offsetLo + range);
        final SharedStateDiscreteSampler samplerHi = DiscreteUniformSampler.of(rng3, offsetHi, offsetHi + range);
        for (int i = 0; i < 10; i++) {
            final int sample1 = sampler.sample();
            final int sample2 = samplerLo.sample();
            final int sample3 = samplerHi.sample();
            Assertions.assertEquals(sample1 + offsetLo, sample2, "Incorrect negative offset sample");
            Assertions.assertEquals(sample1 + offsetHi, sample3, "Incorrect positive offset sample");
        }
    }

    /**
     * Test the sample uniformity when using a small range that is not a power of 2.
     */
    @Test
    public void testSampleUniformityWithNonPowerOf2Range() {
        // Test using a RNG that outputs an evenly spaced set of integers.
        // Create a Weyl sequence using George Marsaglia’s increment from:
        // Marsaglia, G (July 2003). "Xorshift RNGs". Journal of Statistical Software. 8 (14).
        // https://en.wikipedia.org/wiki/Weyl_sequence
        final UniformRandomProvider rng = new IntProvider() {
            private final int increment = 362437;
            // Start at the highest positive number
            private final int start = Integer.MIN_VALUE - increment;

            private int bits = start;

            @Override
            public int next() {
                // Output until the first wrap. The entire sequence will be uniform.
                // Note this is not the full period of the sequence.
                // Expect ((1L << 32) / increment) numbers = 11850
                int result = bits += increment;
                if (result < start) {
                    return result;
                }
                throw new IllegalStateException("end of sequence");
            }
        };

        // n = upper range exclusive
        final int n = 37; // prime
        final int[] histogram = new int[n];

        final int lower = 0;
        final int upper = n - 1;

        final SharedStateDiscreteSampler sampler = DiscreteUniformSampler.of(rng, lower, upper);

        try {
            while (true) {
                histogram[sampler.sample()]++;
            }
        } catch (IllegalStateException ex) {
            // Expected end of sequence
        }

        // The sequence will result in either x or (x+1) samples in each bin (i.e. uniform).
        int min = histogram[0];
        int max = histogram[0];
        for (int value : histogram) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        Assertions.assertTrue(max - min <= 1, "Not uniform, max = " + max + ", min=" + min);
    }

    /**
     * Test the sample uniformity when using a small range that is a power of 2.
     */
    @Test
    public void testSampleUniformityWithPowerOf2Range() {
        // Test using a RNG that outputs a counter of integers.
        // The n most significant bits will be represented uniformly over a
        // sequence that is a 2^n long.
        final UniformRandomProvider rng = new IntProvider() {
            private int bits = 0;

            @Override
            public int next() {
                // We reverse the bits because the most significant bits are used
                return Integer.reverse(bits++);
            }
        };

        // n = upper range exclusive
        final int n = 32; // power of 2
        final int[] histogram = new int[n];

        final int lower = 0;
        final int upper = n - 1;

        final SharedStateDiscreteSampler sampler = DiscreteUniformSampler.of(rng, lower, upper);

        final int expected = 2;
        for (int i = expected * n; i-- > 0;) {
            histogram[sampler.sample()]++;
        }

        // This should be even across the entire range
        for (int value : histogram) {
            Assertions.assertEquals(expected, value);
        }
    }

    /**
     * Test the sample rejection when using a range that is not a power of 2. The rejection
     * algorithm of Lemire (2019) splits the entire 32-bit range into intervals of size 2^32/n. It
     * will reject the lowest value in each interval that is over sampled. This test uses 0
     * as the first value from the RNG and tests it is rejected.
     */
    @Test
    public void testSampleRejectionWithNonPowerOf2Range() {
        // Test using a RNG that returns a sequence.
        // The first value of zero should produce a sample that is rejected.
        final int[] value = new int[1];
        final UniformRandomProvider rng = new IntProvider() {
            @Override
            public int next() {
                return value[0]++;
            }
        };

        // n = upper range exclusive.
        // Use a prime number to select the rejection algorithm.
        final int n = 37;
        final int lower = 0;
        final int upper = n - 1;

        final SharedStateDiscreteSampler sampler = DiscreteUniformSampler.of(rng, lower, upper);

        final int sample = sampler.sample();

        Assertions.assertEquals(0, sample, "Sample is incorrect");
        Assertions.assertEquals(2, value[0], "Sample should be produced from 2nd value");
    }

    @Test
    public void testSharedStateSamplerWithSmallRange() {
        testSharedStateSampler(5, 67);
    }

    @Test
    public void testSharedStateSamplerWithLargeRange() {
        // Set the range so rejection below or above the threshold occurs with approximately p=0.25
        testSharedStateSampler(Integer.MIN_VALUE / 2 - 1, Integer.MAX_VALUE / 2 + 1);
    }

    @Test
    public void testSharedStateSamplerWithPowerOf2Range() {
        testSharedStateSampler(0, 31);
    }

    @Test
    public void testSharedStateSamplerWithRangeOf1() {
        testSharedStateSampler(9, 9);
    }

    /**
     * Test the SharedStateSampler implementation.
     *
     * @param lower Lower.
     * @param upper Upper.
     */
    private static void testSharedStateSampler(int lower, int upper) {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        // Use instance constructor not factory constructor to exercise 1.X public API
        final SharedStateDiscreteSampler sampler1 =
            new DiscreteUniformSampler(rng1, lower, upper);
        final SharedStateDiscreteSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    @Test
    public void testToStringWithSmallRange() {
        assertToString(5, 67);
    }

    @Test
    public void testToStringWithLargeRange() {
        assertToString(-99999999, Integer.MAX_VALUE);
    }

    @Test
    public void testToStringWithPowerOf2Range() {
        // Note the range is upper - lower + 1
        assertToString(0, 31);
    }

    @Test
    public void testToStringWithRangeOf1() {
        assertToString(9, 9);
    }

    /**
     * Test the toString method contains the term "uniform". This is true of all samplers
     * even for a fixed sample from a range of 1.
     *
     * @param lower Lower.
     * @param upper Upper.
     */
    private static void assertToString(int lower, int upper) {
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final DiscreteUniformSampler sampler = new DiscreteUniformSampler(rng, lower, upper);
        Assertions.assertTrue(sampler.toString().toLowerCase(Locale.US).contains("uniform"));
    }
}
