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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.SplitMix64;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test for {@link ZigguratSampler}.
 */
class ZigguratSamplerTest {
    /**
     * Test the exponential constructor with a bad mean.
     */
    @Test
    void testExponentialConstructorThrowsWithZeroMean() {
        final RestorableUniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final double mean = 0;
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ZigguratSampler.Exponential.of(rng, mean));
    }

    /**
     * Test the exponential SharedStateSampler implementation.
     */
    @Test
    void testExponentialSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final ZigguratSampler.Exponential sampler1 = ZigguratSampler.Exponential.of(rng1);
        final ZigguratSampler.Exponential sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the exponential SharedStateSampler implementation with a mean.
     */
    @Test
    void testExponentialSharedStateSamplerWithMean() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final double mean = 1.23;
        final ZigguratSampler.Exponential sampler1 = ZigguratSampler.Exponential.of(rng1, mean);
        final ZigguratSampler.Exponential sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the Gaussian SharedStateSampler implementation.
     */
    @Test
    void testGaussianSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final ZigguratSampler.NormalizedGaussian sampler1 = ZigguratSampler.NormalizedGaussian.of(rng1);
        final ZigguratSampler.NormalizedGaussian sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the recursion in the exponential distribution.
     */
    @Test
    void testExponentialRecursion() {
        // The exponential distribution will enter the edge of the ziggurat if the RNG
        // outputs -1 (all bits). This performs alias sampling using a long value.
        // The tail will be selected if the next output is -1.
        // Thus two -1 values enter recursion where a new exponential sample is added
        // to the tail value:
        final double tailValue = 7.569274694148063;

        // Alias sampling assigns the ziggurat layer using the lower 8 bits.
        // The rest determine if the layer or the alias are used. We do not control this
        // and leave it to a seeded RNG to select different layers.

        // A value of zero will create a sample of zero (42 is the seed)
        Assertions.assertEquals(0.0, expSample(42, 0));
        Assertions.assertEquals(tailValue, expSample(42, -1, -1, 0));

        // Use different seeds to test different layers from the edge of the ziggurat.
        for (final long seed : new long[] {42, -2136612838, 2340923842L, -1263746817818681L}) {
            // Base value
            final double x0 = expSample(seed);
            // Edge value
            final double x1 = expSample(seed, -1);
            // Recursion
            // Note the order of additions is important as the final sample is added to the
            // tail value multiple times.
            Assertions.assertEquals(x0 + tailValue, expSample(seed, -1, -1));
            Assertions.assertEquals(x1 + tailValue, expSample(seed, -1, -1, -1));
            // Double recursion
            Assertions.assertEquals(x0 + tailValue + tailValue, expSample(seed, -1, -1, -1, -1));
            Assertions.assertEquals(x1 + tailValue + tailValue, expSample(seed, -1, -1, -1, -1, -1));
        }
    }

    /**
     * Create an exponential sample from the sequence of longs, then revert to a seed RNG.
     *
     * @param seed the seed
     * @param longs the longs
     * @return the sample
     */
    private static double expSample(long seed, final long... longs) {
        final SplitMix64 rng = new SplitMix64(seed) {
            private int i;
            @Override
            public long next() {
                if (i == longs.length) {
                    // Revert to seeded RNG
                    return super.next();
                }
                return longs[i++];
            }
        };
        return ZigguratSampler.Exponential.of(rng).sample();
    }

    /**
     * Test Gaussian samples using a large number of bins based on uniformly spaced quantiles.
     * Added for RNG-159.
     */
    @Test
    void testGaussianSamplesWithQuantiles() {
        final int bins = 2000;
        final NormalDistribution dist = new NormalDistribution(null, 0.0, 1.0);
        final double[] quantiles = new double[bins];
        for (int i = 0; i < bins; i++) {
            quantiles[i] = dist.inverseCumulativeProbability((i + 1.0) / bins);
        }
        testSamples(quantiles, false);
    }

    /**
     * Test Gaussian samples using a large number of bins uniformly spaced in a range.
     * Added for RNG-159.
     */
    @Test
    void testGaussianSamplesWithUniformValues() {
        final int bins = 2000;
        final double[] values = new double[bins];
        final double minx = -8;
        final double maxx = 8;
        for (int i = 0; i < bins; i++) {
            values[i] = minx + (maxx - minx) * (i + 1.0) / bins;
        }
        // Ensure upper bound is the support limit
        values[bins - 1] = Double.POSITIVE_INFINITY;
        testSamples(values, false);
    }

    /**
     * Test exponential samples using a large number of bins based on uniformly spaced quantiles.
     */
    @Test
    void testExponentialSamplesWithQuantiles() {
        final int bins = 2000;
        final ExponentialDistribution dist = new ExponentialDistribution(null, 1.0);
        final double[] quantiles = new double[bins];
        for (int i = 0; i < bins; i++) {
            quantiles[i] = dist.inverseCumulativeProbability((i + 1.0) / bins);
        }
        testSamples(quantiles, true);
    }

    /**
     * Test exponential samples using a large number of bins uniformly spaced in a range.
     */
    @Test
    void testExponentialSamplesWithUniformValues() {
        final int bins = 2000;
        final double[] values = new double[bins];
        final double minx = 0;
        // Enter the tail of the distribution
        final double maxx = 12;
        for (int i = 0; i < bins; i++) {
            values[i] = minx + (maxx - minx) * (i + 1.0) / bins;
        }
        // Ensure upper bound is the support limit
        values[bins - 1] = Double.POSITIVE_INFINITY;
        testSamples(values, true);
    }

    /**
     * Test samples using the provided bins. Values correspond to the bin upper limit. It
     * is assumed the values span most of the distribution. Additional tests are performed
     * using a region of the distribution sampled using the edge of the ziggurat.
     *
     * @param values Bin upper limits
     * @param exponential Set the true to use an exponential sampler
     */
    private static void testSamples(double[] values,
                                    boolean exponential) {
        final int bins = values.length;

        final int samples = 10000000;
        final long[] observed = new long[bins];
        final RestorableUniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(0xabcdefL);
        final ContinuousSampler sampler = exponential ?
            ZigguratSampler.Exponential.of(rng) : ZigguratSampler.NormalizedGaussian.of(rng);
        for (int i = 0; i < samples; i++) {
            final double x = sampler.sample();
            final int index = findIndex(values, x);
            observed[index]++;
        }

        // Compute expected
        final AbstractRealDistribution dist = exponential ?
            new ExponentialDistribution(null, 1.0) : new NormalDistribution(null, 0.0, 1.0);
        final double[] expected = new double[bins];
        double x0 = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < bins; i++) {
            final double x1 = values[i];
            expected[i] = dist.probability(x0, x1);
            x0 = x1;
        }

        final double significanceLevel = 0.001;

        final double lowerBound = dist.getSupportLowerBound();

        final ChiSquareTest chiSquareTest = new ChiSquareTest();
        // Pass if we cannot reject null hypothesis that the distributions are the same.
        final double chi2 = chiSquareTest.chiSquareTest(expected, observed);
        Assertions.assertFalse(chi2 < 0.001,
            () -> String.format("(%s <= x < %s) Chi-square p-value = %s",
                                lowerBound, values[bins - 1], chi2));

        // Test bins sampled from the edge of the ziggurat. This is always around zero.
        for (final double range : new double[] {0.5, 0.25, 0.1, 0.05}) {
            final int min = findIndex(values, -range);
            final int max = findIndex(values, range);
            final long[] observed2 = Arrays.copyOfRange(observed, min, max + 1);
            final double[] expected2 = Arrays.copyOfRange(expected, min, max + 1);
            final double chi2b = chiSquareTest.chiSquareTest(expected2, observed2);
            Assertions.assertFalse(chi2b < significanceLevel,
                () -> String.format("(%s <= x < %s) Chi-square p-value = %s",
                                    min == 0 ? lowerBound : values[min - 1], values[max], chi2b));
        }
    }

    /**
     * Find the index of the value in the data such that:
     * <pre>
     * data[index - 1] <= x < data[index]
     * </pre>
     *
     * <p>This is a specialised binary search that assumes the bounds of the data are the
     * extremes of the support, and the upper support is infinite. Thus an index cannot
     * be returned as equal to the data length.
     *
     * @param data the data
     * @param x the value
     * @return the index
     */
    private static int findIndex(double[] data, double x) {
        int low = 0;
        int high = data.length - 1;

        // Bracket so that low is just above the value x
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final double midVal = data[mid];

            if (x < midVal) {
                // Reduce search range
                high = mid - 1;
            } else {
                // Set data[low] above the value
                low = mid + 1;
            }
        }
        // Verify the index is correct
        Assertions.assertTrue(x < data[low]);
        if (low != 0) {
            Assertions.assertTrue(x >= data[low - 1]);
        }
        return low;
    }
}
