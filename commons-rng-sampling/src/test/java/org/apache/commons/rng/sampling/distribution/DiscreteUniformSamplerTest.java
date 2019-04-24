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
import org.junit.Assert;
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
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithLowerAboveUpper() {
        final int upper = 55;
        final int lower = upper + 1;
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        DiscreteUniformSampler.of(rng, lower, upper);
    }

    @Test
    public void testSamplesWithRangeOf1() {
        final int upper = 99;
        final int lower = upper;
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64);
        final SharedStateDiscreteSampler sampler = DiscreteUniformSampler.of(rng, lower, upper);
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(lower, sampler.sample());
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
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final SharedStateDiscreteSampler sampler = DiscreteUniformSampler.of(rng2, lower, upper);
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(rng1.nextInt(), sampler.sample());
        }
    }

    @Test
    public void testSamplesWithPowerOf2Range() {
        final UniformRandomProvider rngZeroBits = new IntProvider() {
            @Override
            public int next() {
                return 0;
            }
        };
        final UniformRandomProvider rngAllBits = new IntProvider() {
            @Override
            public int next() {
                return 0xffffffff;
            }
        };

        final int lower = -3;
        DiscreteUniformSampler sampler;
        // The upper range for a positive integer is 2^31-1. So the max positive power of
        // 2 is 2^30. However the sampler should handle a bit shift of 31 to create a range
        // of Integer.MIN_VALUE (0x80000000) as this is a power of 2 as an unsigned int (2^31).
        for (int i = 0; i < 32; i++) {
            final int range = 1 << i;
            final int upper = lower + range - 1;
            sampler = new DiscreteUniformSampler(rngZeroBits, lower, upper);
            Assert.assertEquals("Zero bits sample", lower, sampler.sample());
            sampler = new DiscreteUniformSampler(rngAllBits, lower, upper);
            Assert.assertEquals("All bits sample", upper, sampler.sample());
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
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, seed);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, seed);
        final UniformRandomProvider rng3 = RandomSource.create(RandomSource.SPLIT_MIX_64, seed);

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
            Assert.assertEquals("Incorrect negative offset sample", sample1 + offsetLo, sample2);
            Assert.assertEquals("Incorrect positive offset sample", sample1 + offsetHi, sample3);
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
        Assert.assertTrue("Not uniform, max = " + max + ", min=" + min, max - min <= 1);
    }

    /**
     * Test the sample uniformity when using a small range that is a power of 2.
     */
    @Test
    public void testSampleUniformityWithPowerOf2Range() {
        // Test using a RNG that outputs a counter of integers.
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
            Assert.assertEquals(expected, value);
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

        Assert.assertEquals("Sample is incorrect", 0, sample);
        Assert.assertEquals("Sample should be produced from 2nd value", 2, value[0]);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSamplerWithSmallRange() {
        testSharedStateSampler(5, 67);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSamplerWithLargeRange() {
        // Set the range so rejection below or above the threshold occurs with approximately p=0.25
        testSharedStateSampler(Integer.MIN_VALUE / 2 - 1, Integer.MAX_VALUE / 2 + 1);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSamplerWithPowerOf2Range() {
        testSharedStateSampler(0, 31);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
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
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
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
     * Test the toString method. This is added to ensure coverage as the factory constructor
     * used in other tests does not create an instance of the wrapper class.
     *
     * @param lower Lower.
     * @param upper Upper.
     */
    private static void assertToString(int lower, int upper) {
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final DiscreteUniformSampler sampler =
            new DiscreteUniformSampler(rng, lower, upper);
        Assert.assertTrue(sampler.toString().toLowerCase(Locale.US).contains("uniform"));
    }
}
