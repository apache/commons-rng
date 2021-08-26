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

    // Note:
    // The sampler samples within the box regions of the ziggurat > 98.5% of the time.
    // The rest of the time a sample is taken from an overhang region or the tail. The proportion
    // of samples from each of the overhangs and tail is related to the volume of those
    // regions. The sampling within a region must fit the PDF curve of the region.
    // We must test two items: (1) if the sampling is biased to certain overhangs;
    // and (2) if the sampling is biased within a single overhang.
    //
    // The following tests check if the overall shape of the PDF is correct. Then small
    // regions are sampled that target specific parts of the ziggurat: the overhangs and the tail.
    // For the exponential distribution all overhangs are concave so any region can be tested.
    // For the normal distribution the overhangs switch from convex to concave at the inflection
    // point which is x=1. The test targets regions specifically above and below the inflection
    // point.
    //
    // Testing the distribution within an overhang requires a histogram with widths smaller
    // than the width differences in the ziggurat.
    //
    // For the normal distribution the widths at each end and around the inflection point are:
    // [3.6360066255009458, 3.431550493837111, 3.3044597575834205, 3.2104230299359244]
    // [1.0113527773194309, 1.003307168714496, 0.9951964183341382, 0.9870178156137862]
    // [0.3881084509554052, 0.34703847379449715, 0.29172225078072095, 0.0]
    //
    // For the exponential:
    // [7.569274694148063, 6.822872544335941, 6.376422694249821, 6.05490013642656]
    // [0.18840300957360784, 0.15921172977030051, 0.12250380599214447, 0.0]
    //
    // Two tests are performed:
    // (1) The bins are spaced using the quantiles of the distribution.
    // The histogram should be uniform in frequency across all bins.
    // (2) The bins are spaced uniformly. The frequencies should match the CDF of the function
    // for that region. In this test the number of bins is chosen to ensure that there are multiple
    // bins covering even the smallest gaps between ziggurat layers. Thus the bins will partition
    // samples within a single overhang layer.
    //
    // Note:
    // These tests could be improved as not all minor errors in the samplers are detected.
    // The test does detect all the bugs that were eliminated from the algorithm during
    // development if they are reintroduced.
    //
    // Bugs that cannot be detected:
    // 1. Changing the J_INFLECTION point in the Gaussian sampler to the extremes of 1 or 253
    //    does not fail. Thus the test cannot detect incorrect triangle sampling for
    //    convex/concave regions. A sample that is a uniform lower-left triangle sample
    //    for any concave region is missed by the test.
    // 2. Changing the E_MAX threshold in the exponential sampler to 0 does not fail.
    //    Thus the test cannot detect the difference between a uniform lower-left triangle
    //    sample and a uniform convex region sample.
    // This may require more samples in the histogram or a different test strategy. An example
    // would be to force the RNG to sample outside the ziggurat boxes. The expected counts would
    // have to be constructed using prior knowledge of the ziggurat box sizes. Thus the CDF can
    // be computed for each overhang minus the ziggurat region underneath. No distribution
    // exists to support this in Commons Math. A custom implementation should support computing
    // the CDF of the ziggurat boxes from x0 to x1.

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
        testSamples(quantiles, false,
            // Test positive and negative ranges to check symmetry.
            // Smallest layer is 0.292 (convex region)
            new double[] {0, 0.2},
            new double[] {-0.35, -0.1},
            // Around the mean
            new double[] {-0.1, 0.1},
            new double[] {-0.4, 0.6},
            // Inflection point at x=1
            new double[] {-1.1, -0.9},
            // A concave region
            new double[] {2.1, 2.5},
            // Tail = 3.64
            new double[] {2.5, 8});
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
        // Bin width = 16 / 2000 = 0.008
        for (int i = 0; i < bins; i++) {
            values[i] = minx + (maxx - minx) * (i + 1.0) / bins;
        }
        // Ensure upper bound is the support limit
        values[bins - 1] = Double.POSITIVE_INFINITY;
        testSamples(values, false,
            // Test positive and negative ranges to check symmetry.
            // Smallest layer is 0.292 (convex region)
            new double[] {0, 0.2},
            new double[] {-0.35, -0.1},
            // Around the mean
            new double[] {-0.1, 0.1},
            new double[] {-0.4, 0.6},
            // Inflection point at x=1
            new double[] {-1.01, -0.99},
            new double[] {0.98, 1.03},
            // A concave region
            new double[] {1.03, 1.05},
            // Tail = 3.64
            new double[] {3.6, 3.8},
            new double[] {3.7, 8});
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
        testSamples(quantiles, true,
            // Smallest layer is 0.122
            new double[] {0, 0.1},
            new double[] {0.05, 0.15},
            // Around the mean
            new double[] {0.9, 1.1},
            // Tail = 7.57
            new double[] {1.5, 12});
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
        // Bin width = 12 / 2000 = 0.006
        for (int i = 0; i < bins; i++) {
            values[i] = minx + (maxx - minx) * (i + 1.0) / bins;
        }
        // Ensure upper bound is the support limit
        values[bins - 1] = Double.POSITIVE_INFINITY;

        testSamples(values, true,
            // Smallest layer is 0.122
            new double[] {0, 0.1},
            new double[] {0.05, 0.15},
            // Around the mean
            new double[] {0.9, 1.1},
            // Tail = 7.57
            new double[] {7.5, 7.7},
            new double[] {7.7, 12});
    }

    /**
     * Test samples using the provided bins. Values correspond to the bin upper limit. It
     * is assumed the values span most of the distribution. Additional tests are performed
     * using a region of the distribution sampled.
     *
     * @param values Bin upper limits
     * @param exponential Set the true to use an exponential sampler
     * @param ranges Ranges of the distribution to test
     */
    private static void testSamples(double[] values,
                                    boolean exponential,
                                    double[]... ranges) {
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
        final double pValue = chiSquareTest.chiSquareTest(expected, observed);
        Assertions.assertFalse(pValue < 0.001,
            () -> String.format("(%s <= x < %s) Chi-square p-value = %s",
                                lowerBound, values[bins - 1], pValue));

        // Test regions of the ziggurat.
        for (final double[] range : ranges) {
            final int min = findIndex(values, range[0]);
            final int max = findIndex(values, range[1]);
            // Must have a range of 2
            if (max - min + 1 < 2) {
                // This will probably occur if the quantiles test uses too small a range
                // for the tail. The tail is so far into the CDF that a single bin is
                // often used to represent it.
                Assertions.fail("Invalid range: " + Arrays.toString(range));
            }
            final long[] observed2 = Arrays.copyOfRange(observed, min, max + 1);
            final double[] expected2 = Arrays.copyOfRange(expected, min, max + 1);
            final double pValueB = chiSquareTest.chiSquareTest(expected2, observed2);
            Assertions.assertFalse(pValueB < significanceLevel,
                () -> String.format("(%s <= x < %s) Chi-square p-value = %s",
                                    min == 0 ? lowerBound : values[min - 1], values[max], pValueB));
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
