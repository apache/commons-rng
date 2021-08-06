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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import java.util.Arrays;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test for {@link ZigguratSampler}.
 */
public class ZigguratSamplerTest {
    /**
     * Test the exponential constructor with a bad mean.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testExponentialConstructorThrowsWithZeroMean() {
        final RestorableUniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(0L);
        final double mean = 0;
        ZigguratSampler.Exponential.of(rng, mean);
    }

    /**
     * Test the exponential SharedStateSampler implementation.
     */
    @Test
    public void testExponentialSharedStateSampler() {
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
    public void testExponentialSharedStateSamplerWithMean() {
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
    public void testGaussianSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.SPLIT_MIX_64.create(0L);
        final UniformRandomProvider rng2 = RandomSource.SPLIT_MIX_64.create(0L);
        final ZigguratSampler.NormalizedGaussian sampler1 = ZigguratSampler.NormalizedGaussian.of(rng1);
        final ZigguratSampler.NormalizedGaussian sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test Gaussian samples using a large number of bins based on uniformly spaced quantiles.
     * Added for RNG-159.
     */
    @Ignore("See RNG-159")
    @Test
    public void testGaussianSamplesWithQuantiles() {
        final int bins = 2000;
        final NormalDistribution dist = new NormalDistribution(null, 0.0, 1.0);
        final double[] quantiles = new double[bins];
        for (int i = 0; i < bins; i++) {
            quantiles[i] = dist.inverseCumulativeProbability((i + 1.0) / bins);
        }

        final int samples = 10000000;
        final long[] observed = new long[bins];
        final RestorableUniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(0xabcdefL);
        final ZigguratSampler.NormalizedGaussian sampler = ZigguratSampler.NormalizedGaussian.of(rng);
        for (int i = 0; i < samples; i++) {
            final double x = sampler.sample();
            final int index = findIndex(quantiles, x);
            observed[index]++;
        }
        final double[] expected = new double[bins];
        Arrays.fill(expected, 1.0 / bins);

        final ChiSquareTest chiSquareTest = new ChiSquareTest();
        // Pass if we cannot reject null hypothesis that the distributions are the same.
        double chi2 = chiSquareTest.chiSquareTest(expected, observed);
        Assert.assertFalse("Chi-square p-value = " + chi2, chi2 < 0.001);

        // Test around the mean
        for (final double range : new double[] {0.5, 0.25, 0.1, 0.05}) {
            final int min = findIndex(quantiles, -range);
            final int max = findIndex(quantiles, range);
            final long[] observed2 = Arrays.copyOfRange(observed, min, max + 1);
            final double[] expected2 = Arrays.copyOfRange(expected, min, max + 1);
            chi2 = chiSquareTest.chiSquareTest(expected2, observed2);
            Assert.assertFalse(String.format("(%s <= x < %s) Chi-square p-value = %s",
                    -range, range, chi2), chi2 < 0.001);
        }
    }

    /**
     * Test Gaussian samples using a large number of bins uniformly spaced in a range.
     * Added for RNG-159.
     */
    @Ignore("See RNG-159")
    @Test
    public void testGaussianSamplesWithUniformValues() {
        final int bins = 2000;
        final double[] values = new double[bins];
        final double minx = -8;
        final double maxx = 8;
        for (int i = 0; i < bins; i++) {
            values[i] = minx + (maxx - minx) * (i + 1.0) / bins;
        }

        final int samples = 10000000;
        final long[] observed = new long[bins];
        final RestorableUniformRandomProvider rng = RandomSource.XO_SHI_RO_128_PP.create(0xabcdefL);
        final ZigguratSampler.NormalizedGaussian sampler = ZigguratSampler.NormalizedGaussian.of(rng);
        for (int i = 0; i < samples; i++) {
            final double x = sampler.sample();
            final int index = findIndex(values, x);
            observed[index]++;
        }

        // Compute expected
        final NormalDistribution dist = new NormalDistribution(null, 0.0, 1.0);
        final double[] expected = new double[bins];
        double x0 = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < bins; i++) {
            final double x1 = values[i];
            expected[i] = dist.probability(x0, x1);
            x0 = x1;
        }

        final ChiSquareTest chiSquareTest = new ChiSquareTest();
        // Pass if we cannot reject null hypothesis that the distributions are the same.
        double chi2 = chiSquareTest.chiSquareTest(expected, observed);
        Assert.assertFalse("Chi-square p-value = " + chi2, chi2 < 0.001);

        // Test around the mean
        for (final double range : new double[] {0.5, 0.25, 0.1, 0.05}) {
            final int min = findIndex(values, -range);
            final int max = findIndex(values, range);
            final long[] observed2 = Arrays.copyOfRange(observed, min, max + 1);
            final double[] expected2 = Arrays.copyOfRange(expected, min, max + 1);
            chi2 = chiSquareTest.chiSquareTest(expected2, observed2);
            Assert.assertFalse(String.format("(%s <= x < %s) Chi-square p-value = %s",
                -range, range, chi2), chi2 < 0.001);
        }
    }

    /**
     * Find the index of the value in the data such that:
     * <pre>
     * data[index - 1] <= x < data[index]
     * </pre>
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
        Assert.assertTrue(x < data[low]);
        if (low != 0) {
            Assert.assertTrue(x >= data[low - 1]);
        }
        return low;
    }
}
